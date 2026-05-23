"""The opponent_strategist LangGraph node.

Runs after ``game_advisor`` once an opponent archetype has been recognized.
Where recognition only labels the deck, the strategist reasons about it like a
Pro Tour player: given the archetype's structured profile (role buckets, combo
pairs, kill priority, predicted lines), what is the opponent most likely
holding, what will they do next turn, and which permanents should the AI remove
first?

It makes one LLM call seeded with a deterministic Bayesian prior, then
post-validates the model's card citations against the profile/observed plays so
it cannot invent cards. If no profile exists for the archetype, or the call
fails, the node leaves the deterministic ``opponent_hand`` from game_advisor in
place — it only ever enriches, never regresses.
"""

from __future__ import annotations

import asyncio
import logging

from app.config import CONFIG
from app.knowledge import loader, mana_profile
from app.llm_client import LLMError, generate_json
from app.schema import GraphState

log = logging.getLogger(__name__)

_STRATEGIST_SYSTEM_PROMPT = (
    "You are a Pro Tour-level Magic: The Gathering player reasoning about your "
    "opponent. Given their recognized archetype, its card-role profile, and the "
    "public game state, infer what they are holding, predict their next turn, "
    "and rank which of their permanents are most dangerous. Only cite cards that "
    "appear in the provided profile or have already been observed. Always answer "
    "with a single JSON object and nothing else."
)

_ACTION_SYSTEM_PROMPT = (
    "You are a Pro Tour-level Magic: The Gathering pilot choosing the AI's next "
    "legal action. Use only the provided legal actions. Prefer casting spells "
    "before activating abilities in normal development turns, but override that "
    "when the deck guide and board state make an ability more important. Always "
    "answer with a single JSON object and nothing else."
)

# Decisions worth the extra LLM latency. Routine priority passes at low
# confidence skip the strategist and fall back to the deterministic inference.
_ALWAYS_RUN_DECISIONS = {"mulligan", "combat", "critical"}
_MIN_CONFIDENCE = 0.5


def _norm(s: str) -> str:
    return (s or "").strip().lower()


def _revealed_cards(state: GraphState) -> set[str]:
    seen: set[str] = set()
    for o in state.get("observations", []) or []:
        seen.add(_norm(o.get("card", "")))
    seen.update(_norm(c) for c in (state.get("opponent_board", []) or []))
    seen.update(_norm(c) for c in (state.get("opponent_graveyard", []) or []))
    seen.discard("")
    return seen


def _bayesian_prior(
    profile: dict,
    archetype: str,
    fmt: str,
    revealed: set[str],
    opp_hand_size: int,
) -> dict[str, float]:
    """Per-bucket prior probability the opponent still holds a card of that role.

    remaining = max(0, target_count - revealed_in_bucket); the prior is each
    bucket's share of total remaining, scaled by how full their hand is.
    """
    buckets = profile.get("buckets") or {}
    remaining: dict[str, float] = {}
    for bucket in loader.PROFILE_BUCKETS:
        payload = buckets.get(bucket)
        if not isinstance(payload, dict):
            continue
        target = float(payload.get("target_count", 0) or 0)
        if target <= 0:
            continue
        revealed_in_bucket = sum(
            1 for c in revealed if bucket in loader.card_buckets(c, fmt, archetype)
        )
        remaining[bucket] = max(0.0, target - revealed_in_bucket)
    total = sum(remaining.values())
    if total <= 0:
        return {}
    hand_factor = max(0.0, min(1.0, opp_hand_size / 7.0)) if opp_hand_size else 0.5
    return {b: round((r / total) * hand_factor, 3) for b, r in remaining.items()}


def _format_profile_block(
    profile: dict, prior: dict[str, float], revealed: set[str], fmt: str, archetype: str
) -> str:
    buckets = profile.get("buckets") or {}
    lines: list[str] = []
    for bucket in loader.PROFILE_BUCKETS:
        payload = buckets.get(bucket)
        if not isinstance(payload, dict):
            continue
        target = payload.get("target_count", 0) or 0
        cards = list(payload.get("cards") or [])
        for pair in payload.get("pairs") or []:
            cards.extend(pair)
        if not target and not cards:
            continue
        seen_n = sum(1 for c in revealed if bucket in loader.card_buckets(c, fmt, archetype))
        remaining = max(0, int(target) - seen_n)
        card_str = ", ".join(dict.fromkeys(cards)) or "(none listed)"
        p = prior.get(bucket)
        p_str = f" prior={p}" if p is not None else ""
        lines.append(
            f"- {bucket}: target {target}, seen {seen_n}, "
            f"remaining {remaining}{p_str} -> {card_str}"
        )
    combos = (buckets.get("combo_pieces") or {}).get("pairs") or []
    combo_str = "; ".join(" + ".join(pair) for pair in combos) if combos else "(none)"
    dual = (
        "; ".join(
            f"{d.get('card')} = {'/'.join(d.get('roles', []))}"
            for d in (profile.get("dual_role_cards") or [])
        )
        or "(none)"
    )
    pred = (
        "; ".join(
            f"if [{p.get('trigger')}] -> {p.get('line')}"
            for p in (profile.get("predicted_lines") or [])
        )
        or "(none)"
    )
    return (
        f"Archetype macro plan: {profile.get('macro_plan', '')}\n"
        f"Win-turn window: {profile.get('win_turn_window', [])}\n"
        f"Role buckets (many-to-many; a card can be in several):\n" + "\n".join(lines) + "\n"
        f"Key combos: {combo_str}\n"
        f"Multi-role cards: {dual}\n"
        f"Kill priority (most dangerous first): "
        f"{', '.join(profile.get('kill_priority', [])) or '(none)'}\n"
        f"Their interaction to play around: "
        f"{', '.join(profile.get('interaction_to_disrupt', [])) or '(none)'}\n"
        f"Known lines: {pred}"
    )


def _format_observations(state: GraphState, fmt: str, archetype: str) -> str:
    obs = state.get("observations", []) or []
    if not obs:
        return "(no plays observed yet)"
    lines = []
    for o in obs:
        name = o.get("card", "?")
        roles = loader.card_buckets(name, fmt, archetype)
        role_str = f" [{'/'.join(roles)}]" if roles else ""
        lines.append(f"- turn {o.get('turn', '?')}: {o.get('event', '?')} {name}{role_str}")
    return "\n".join(lines)


_COLOR_ORDER = "WUBRGC"


def _format_mana_pool(state: GraphState) -> str:
    """Describe the opponent's open mana as per-source color options plus the
    set of reachable colors, so the model can rule out plays they can't pay for.

    Each untapped source taps for ONE of its listed colors; we deliberately do
    not pre-enumerate the (exponential) set of color combinations and leave that
    reasoning to the model.
    """
    sources = state.get("opp_untapped_sources") or []
    avail = int(state.get("opp_mana_available") or 0)
    spent = int(state.get("opp_mana_spent_this_turn") or 0)
    if not sources:
        return (
            f"Opponent open mana: {avail} untapped source(s); "
            f"{spent} already committed this turn (color breakdown unavailable)."
        )
    per_source = ", ".join("[" + "/".join(s) + "]" if s else "[?]" for s in sources)
    reachable = sorted(
        {c for s in sources for c in s},
        key=lambda c: _COLOR_ORDER.index(c) if c in _COLOR_ORDER else 99,
    )
    return (
        f"Opponent open mana: {avail} untapped source(s), {spent} committed this turn.\n"
        f"Untapped sources (each taps for ONE of its listed colors): {per_source}\n"
        f"Reachable colors: {'/'.join(reachable) or '?'} — use the total count AND "
        f"these color options to rule out lines the opponent cannot actually pay for."
    )


def _build_prompt(
    state: GraphState, profile: dict, prior: dict[str, float], revealed: set[str]
) -> str:
    archetype = state.get("archetype") or ""
    fmt = (state.get("resolved_format") or state.get("format") or "").lower()
    opp_hand_size = int(state.get("opp_hand_size") or 0)
    opp_board = ", ".join(state.get("opponent_board", []) or []) or "(empty)"
    opp_gy = ", ".join(state.get("opponent_graveyard", []) or []) or "(empty)"
    colors = "/".join(state.get("opponent_mana_colors_seen", []) or []) or "?"
    return (
        f"Opponent archetype: {archetype} (recognition confidence "
        f"{state.get('confidence', 0.0):.2f})\n"
        f"Turn: {state.get('turn', 0)}\n\n"
        f"{_format_profile_block(profile, prior, revealed, fmt, archetype)}\n\n"
        f"PUBLIC STATE\n"
        f"Opponent battlefield: {opp_board}\n"
        f"Opponent graveyard: {opp_gy}\n"
        f"Opponent mana colors seen: {colors}\n"
        f"{_format_mana_pool(state)}\n"
        f"Opponent cards in hand: {opp_hand_size}\n\n"
        f"Their observed plays (chronological, tagged with role):\n"
        f"{_format_observations(state, fmt, archetype)}\n\n"
        "TASK\n"
        "1. For each role bucket, estimate the probability (0-1) the opponent is "
        "holding at least one card of that role right now, starting from the "
        "provided prior and adjusting for what they've revealed, their mana, and "
        "their hand size. Name up to 3 concrete cards per bucket (from the "
        "profile/observed only).\n"
        "2. Predict their NEXT turn assuming they untap with one more mana than "
        "they have now. Pick the most fitting known line or describe one.\n"
        "3. Rank their permanents (and imminent threats) by how urgently the AI "
        "should remove/answer them, using the kill priority and the board.\n\n"
        "Respond with exactly these keys:\n"
        '  "bucket_probabilities": object mapping bucket name -> {"prob": number, '
        '"top_cards": [string]},\n'
        '  "predicted_opp_line": {"primary_play": string, "supporting_plays": '
        '[string], "mana_required": string, "reasoning": string},\n'
        '  "threat_priorities": [{"name": string, "score": number, "reason": '
        "string}] ordered most-dangerous-first"
    )


def _format_own_guide(guide: dict | None) -> str:
    if not guide:
        return "(no own-deck piloting guide available)"
    parts = [
        f"Own deck: {guide.get('archetype', '?')} ({guide.get('strategy_type', '?')})",
        f"Overview: {guide.get('overview', '')}",
    ]
    gp = guide.get("game_plan") or {}
    for key in ("early_game", "mid_game", "late_game"):
        vals = gp.get(key) or []
        if vals:
            parts.append(f"{key}: " + "; ".join(vals[:4]))
    if guide.get("key_cards"):
        parts.append(
            "Key cards: "
            + "; ".join(
                f"{c.get('name')} ({c.get('role')})"
                for c in (guide.get("key_cards") or [])[:8]
                if c.get("name")
            )
        )
    if guide.get("sequencing_tips"):
        parts.append("Sequencing: " + "; ".join((guide.get("sequencing_tips") or [])[:5]))
    if guide.get("learnings"):
        parts.append(
            "Self-play learnings: "
            + "; ".join(
                (f"when {x.get('trigger')}, " if x.get("trigger") else "")
                + (x.get("recommendation") or "")
                for x in (guide.get("learnings") or [])[:5]
            )
        )
    return "\n".join(p for p in parts if p)


def _format_legal_actions(actions: list[dict]) -> str:
    if not actions:
        return "(none)"
    lines = []
    for i, action in enumerate(actions[:40], start=1):
        typ = action.get("action_type", "")
        card = action.get("card", "")
        ability = action.get("ability", "") or "(default cast/play)"
        cost = action.get("cost", "") or "no explicit cost"
        zone = action.get("source_zone", "") or "unknown zone"
        produces = "/".join(action.get("produces") or [])
        produces_text = f"; produces {produces}" if produces else ""
        lines.append(f"{i}. {typ}: {card}; ability={ability}; cost={cost}; zone={zone}{produces_text}")
    return "\n".join(lines)


def _build_action_prompt(state: GraphState) -> str:
    own_board = ", ".join(state.get("own_board", []) or []) or "(empty)"
    opp_board = ", ".join(state.get("opponent_board", []) or []) or "(empty)"
    hand = ", ".join(state.get("hand", []) or []) or "(empty)"
    gy = ", ".join(state.get("your_graveyard", []) or []) or "(empty)"
    opp_hand = state.get("opponent_hand") or []
    opp_read = "; ".join(
        f"{g.get('category')} {int(float(g.get('probability') or 0) * 100)}%"
        for g in opp_hand[:6]
        if isinstance(g, dict)
    ) or "(none)"
    return (
        f"Turn: {state.get('turn', 0)}\n"
        f"Phase: {state.get('phase', '') or '?'}\n"
        f"Life totals: {state.get('life_totals') or {}}\n"
        f"AI hand: {hand}\n"
        f"AI battlefield: {own_board}\n"
        f"AI graveyard: {gy}\n"
        f"Opponent archetype/read: {state.get('archetype', 'Unknown')} "
        f"({float(state.get('confidence') or 0.0):.2f})\n"
        f"Opponent battlefield: {opp_board}\n"
        f"Opponent inferred hand buckets: {opp_read}\n\n"
        f"OWN DECK GUIDANCE\n{_format_own_guide(state.get('piloting_guide'))}\n\n"
        f"CURRENT LEGAL ACTIONS FROM FORGE\n{_format_legal_actions(state.get('legal_actions') or [])}\n\n"
        "Choose the most important next action from CURRENT LEGAL ACTIONS. "
        "In general, prefer PLAY_SPELL before ACTIVATE_ABILITY before PLAY_LAND, "
        "but use the deck guide, phase, board, mana plan, and opponent read to "
        "override that default. If activating an ability is best, return the "
        "same card and ability text exactly as listed.\n"
        "Respond with exactly this JSON shape:\n"
        '{ "actions": ['
        '{"action_type":"PLAY_SPELL|PLAY_LAND|ACTIVATE_ABILITY|PASS",'
        '"target":"card name or empty", "ability":"exact ability text or empty",'
        '"percentage":0-100, "reasoning":"one sentence"}'
        "] }"
    )


def _legal_action_keys(state: GraphState) -> set[tuple[str, str, str]]:
    keys: set[tuple[str, str, str]] = set()
    for action in state.get("legal_actions") or []:
        typ = str(action.get("action_type") or "").upper()
        card = _norm(str(action.get("card") or ""))
        ability = _norm(str(action.get("ability") or ""))
        if typ and card:
            keys.add((typ, card, ability))
    return keys


def _coerce_action_plan(raw: dict, state: GraphState) -> list[dict]:
    if not isinstance(raw, dict):
        return []
    legal = _legal_action_keys(state)
    out: list[dict] = []
    for item in raw.get("actions") or []:
        if not isinstance(item, dict):
            continue
        typ = str(item.get("action_type") or "").upper()
        target = str(item.get("target") or "").strip()
        ability = str(item.get("ability") or "").strip()
        if typ == "PASS":
            pass
        elif (typ, _norm(target), _norm(ability)) not in legal:
            if not ability and any(k[0] == typ and k[1] == _norm(target) for k in legal):
                pass
            else:
                continue
        try:
            pct = float(item.get("percentage") or 0.0)
        except (TypeError, ValueError):
            pct = 0.0
        out.append(
            {
                "action_type": typ,
                "target": target,
                "ability": ability,
                "targets": None,
                "percentage": max(0.0, min(100.0, pct)),
                "reasoning": str(item.get("reasoning") or "").strip(),
            }
        )
    return out[:5]


def _merge_strategist_actions(existing: list[dict], planned: list[dict]) -> list[dict]:
    if not planned:
        return existing or []
    return planned + [
        a for a in (existing or [])
        if not any(
            (a.get("action_type") or "").upper() == (p.get("action_type") or "").upper()
            and _norm(a.get("target") or "") == _norm(p.get("target") or "")
            and _norm(a.get("ability") or "") == _norm(p.get("ability") or "")
            for p in planned
        )
    ]


async def _run_action_planner(state: GraphState) -> GraphState:
    """Ask the strategist model to choose among Forge-provided legal actions,
    including activated abilities. The result is validated against the legal
    action list before it can influence the Java engine."""
    if (
        int(state.get("turn", 0) or 0) <= 0
        or int(state.get("sidecar_influence", 50) or 0) <= 0
        or not (state.get("legal_actions") or [])
    ):
        return {}
    try:
        log.info(
            "llm_call node=opponent_strategist purpose=action_plan turn=%s legal=%s",
            state.get("turn", 0),
            len(state.get("legal_actions") or []),
        )
        raw = await generate_json(
            _build_action_prompt(state),
            system=_ACTION_SYSTEM_PROMPT,
            model=CONFIG.strategist_model_name,
            temperature=0.15,
        )
    except LLMError as exc:
        log.warning("opponent_strategist: action planner failed (%s); keeping existing actions", exc)
        return {}
    planned = _coerce_action_plan(raw, state)
    if not planned:
        return {}
    return {"actions": _merge_strategist_actions(state.get("actions") or [], planned)}


def _valid_card_pool(profile: dict, revealed: set[str], fmt: str, archetype: str) -> set[str]:
    pool = set(revealed)
    for _bucket, payload in (profile.get("buckets") or {}).items():
        if not isinstance(payload, dict):
            continue
        for c in payload.get("cards") or []:
            pool.add(_norm(c))
        for pair in payload.get("pairs") or []:
            pool.update(_norm(c) for c in pair)
    for d in profile.get("dual_role_cards") or []:
        pool.add(_norm(d.get("card", "")))
    for name in profile.get("kill_priority") or []:
        pool.add(_norm(name))
    pool.discard("")
    return pool


def _clamp(x, lo=0.0, hi=1.0) -> float:
    try:
        return max(lo, min(hi, float(x)))
    except (TypeError, ValueError):
        return 0.0


def _beatdown_from_board(state: GraphState) -> dict:
    """Decide the beatdown deterministically from the board score.

    The beatdown is whoever is ahead on board and should be racing. We trust
    the upstream board_score (AI's perspective, [-1, +1]) rather than a
    free-form LLM guess: if the AI is ahead on board it is the beatdown,
    otherwise the human opponent is. An even/empty board defaults to the
    opponent so the AI assumes the control role until it pulls ahead.
    """
    role = state.get("role") or {}
    board_score = role.get("board_score")
    try:
        board_score = float(board_score)
    except (TypeError, ValueError):
        board_score = 0.0
    if board_score > 0.0:
        return {
            "who_is_beatdown": "ai",
            "reasoning": (
                f"AI is ahead on board (board score {board_score:+.2f}); the side "
                "ahead on board is the beatdown and should race to close the game."
            ),
        }
    return {
        "who_is_beatdown": "opponent",
        "reasoning": (
            f"Opponent is ahead or even on board (board score {board_score:+.2f}); "
            "the AI is not winning the board, so the human is the beatdown and the "
            "AI should take the control role."
        ),
    }


async def _run_opponent_inference(
    state: GraphState, profile: dict, fmt: str, archetype: str
) -> GraphState:
    """The original opponent-reading LLM call: hand inference, threats, next
    line. Gated on a recognized, profiled, confident-enough opponent. Returns
    only the keys it wants to update (merged by the node)."""
    confidence = state.get("confidence") or 0.0
    decision = _norm(state.get("decision_type") or "")
    if confidence < _MIN_CONFIDENCE and decision not in _ALWAYS_RUN_DECISIONS:
        # Not worth the call; keep deterministic inference, but still emit the
        # board-driven beatdown assessment.
        return {"beatdown_assessment": _beatdown_from_board(state)}

    revealed = _revealed_cards(state)
    opp_hand_size = int(state.get("opp_hand_size") or 0)
    prior = _bayesian_prior(profile, archetype, fmt, revealed, opp_hand_size)

    try:
        log.info(
            "llm_call node=opponent_strategist archetype=%s format=%s confidence=%.2f decision=%s",
            archetype,
            fmt,
            confidence,
            decision,
        )
        result = await generate_json(
            _build_prompt(state, profile, prior, revealed),
            system=_STRATEGIST_SYSTEM_PROMPT,
            model=CONFIG.strategist_model_name,
            temperature=0.3,
        )
        log.info(
            "llm_result node=opponent_strategist archetype=%s format=%s",
            archetype,
            fmt,
        )
    except LLMError as exc:
        log.warning(
            "opponent_strategist: LLM call failed (%s); keeping deterministic inference", exc
        )
        # Beatdown is board-score driven, so emit it even when the LLM is down.
        return {"beatdown_assessment": _beatdown_from_board(state)}

    pool = _valid_card_pool(profile, revealed, fmt, archetype)

    # --- opponent hand: profile-bucket guesses replace signature-only guesses
    opponent_hand: list[dict] = []
    bucket_probs = result.get("bucket_probabilities")
    if isinstance(bucket_probs, dict):
        for bucket, payload in bucket_probs.items():
            if bucket not in loader.PROFILE_BUCKETS or not isinstance(payload, dict):
                continue
            cards = [
                c
                for c in (payload.get("top_cards") or [])
                if isinstance(c, str) and _norm(c) in pool
            ][:3]
            prob = _clamp(payload.get("prob"))
            if prob <= 0.0 and not cards:
                continue
            opponent_hand.append(
                {
                    "category": bucket,
                    "example_cards": cards,
                    "probability": round(prob, 3),
                    "reasoning": f"{archetype} role inference (prior {prior.get(bucket, 0.0)}).",
                }
            )
        opponent_hand.sort(key=lambda r: -r["probability"])
        opponent_hand = opponent_hand[:8]

    # --- threat priorities: rank opponent permanents, archetype-aware
    opp_board_norm = {_norm(c) for c in (state.get("opponent_board", []) or [])}
    threat_entries = result.get("threat_priorities")
    ranked_targets: list[str] = []
    reasons: list[str] = []
    if isinstance(threat_entries, list):
        for entry in threat_entries:
            if not isinstance(entry, dict):
                continue
            name = entry.get("name", "")
            if not isinstance(name, str) or not name.strip():
                continue
            ranked_targets.append(name.strip())
            r = entry.get("reason")
            if isinstance(r, str) and r.strip():
                reasons.append(f"{name.strip()}: {r.strip()}")
    # Prefer permanents actually on the opponent board; otherwise keep order.
    on_board = [t for t in ranked_targets if _norm(t) in opp_board_norm]
    final_targets = on_board or ranked_targets
    target_priorities = state.get("target_priorities") or []
    if final_targets:
        target_priorities = [
            {
                "spell": "",
                "targets": final_targets[:6],
                "reasoning": "Archetype-aware threat order. " + " | ".join(reasons[:4]),
            }
        ]

    # --- predicted next turn (informational, surfaced to UI/logs)
    predicted = result.get("predicted_opp_line")
    predicted_opp_line = predicted if isinstance(predicted, dict) else None
    # Beatdown is decided deterministically from board score, not the LLM.
    beatdown_assessment = _beatdown_from_board(state)

    out: GraphState = {}
    if opponent_hand:
        out["opponent_hand"] = opponent_hand
    out["target_priorities"] = target_priorities
    if predicted_opp_line:
        out["predicted_opp_line"] = predicted_opp_line
    if beatdown_assessment:
        out["beatdown_assessment"] = beatdown_assessment
    return out


# --------------------------------------------------------------------------
# Mana planner: own-deck manabase decisions (fetch / land / utility lands).
# Runs every in-game action regardless of opponent recognition, since the AI
# must sequence its own mana whether or not it has read the opponent yet.
# --------------------------------------------------------------------------

_MANA_SYSTEM_PROMPT = (
    "You are a Pro Tour-level Magic: The Gathering player sequencing your own "
    "manabase. Decide whether to crack a fetchland and what to fetch, which land "
    "to play from hand, and how to use utility lands — using only cards that are "
    "actually in your hand or remaining in your library. Always answer with a "
    "single JSON object and nothing else."
)


def _remaining_library(state: GraphState) -> list[str]:
    """Cards still in the AI's library = deck minus the zones we can see.

    deck_cards preserves duplicates, so we subtract one occurrence per known
    card. The AI legitimately knows its own library contents (not order)."""
    from collections import Counter

    deck = Counter(_norm(c) for c in (state.get("deck_cards") or []) if c)
    for zone in ("hand", "own_board", "your_graveyard", "your_exile"):
        for c in state.get(zone) or []:
            n = _norm(c)
            if deck.get(n, 0) > 0:
                deck[n] -= 1
    out: list[str] = []
    for name, n in deck.items():
        out.extend([name] * n)
    return out


def _land_names(cards: list[str]) -> list[str]:
    from app.advice import _is_land_name

    seen: dict[str, int] = {}
    for c in cards:
        if c and _is_land_name(c):
            seen[c] = seen.get(c, 0) + 1
    return [f"{name} x{n}" if n > 1 else name for name, n in seen.items()]


def _format_mana_profile(profile: dict | None) -> str:
    if not profile:
        return "(no deck mana profile yet — reason from the decklist directly)"
    parts = [
        f"Primary colors: {'/'.join(profile.get('primary_colors') or []) or '?'}",
        f"Color requirements: {profile.get('color_requirements', '')}",
        f"Fetch priority: {profile.get('fetch_priority', '')}",
        f"Default crack timing: {profile.get('crack_timing_default', 'now')}",
    ]
    if profile.get("utility_land_notes"):
        parts.append(f"Utility lands: {profile['utility_land_notes']}")
    lands = profile.get("lands") or []
    if lands:
        ll = "; ".join(
            f"{land.get('card')} ({land.get('role')}"
            + (", tapped" if land.get("enters_tapped") else "")
            + (f": {land.get('play_timing')}" if land.get("play_timing") else "")
            + ")"
            for land in lands
            if isinstance(land, dict) and land.get("card")
        )
        parts.append("Per-land guidance: " + ll)
    return "\n".join(p for p in parts if p)


def _build_mana_prompt(state: GraphState, profile: dict | None) -> str:
    hand = state.get("hand") or []
    hand_lands = [c for c in hand if _land_names([c])]
    hand_spells = [c for c in hand if c and c not in hand_lands]
    own_board = state.get("own_board") or []
    board_lands = _land_names(own_board)
    remaining = _remaining_library(state)
    remaining_lands = _land_names(remaining)
    life = state.get("life_totals") or {}
    avail = state.get("available_mana") or []
    return (
        f"Turn: {state.get('turn', 0)}  Phase: {state.get('phase', '?')}\n"
        f"Your life totals (by seat/name): {life}\n"
        f"Mana you can make right now: {'/'.join(avail) or '(unknown)'}\n\n"
        f"DECK MANA PROFILE\n{_format_mana_profile(profile)}\n\n"
        f"CURRENT STATE\n"
        f"Lands you control: {', '.join(board_lands) or '(none)'}\n"
        f"Spells in hand: {', '.join(hand_spells) or '(none)'}\n"
        f"Lands in hand: {', '.join(hand_lands) or '(none)'}\n"
        f"Fetchable / remaining lands in your library: "
        f"{', '.join(remaining_lands) or '(none)'}\n\n"
        "TASK\n"
        "Plan this turn's mana. Consider what colors your hand needs THIS turn "
        "and over the next couple of turns, whether to crack a fetchland now (to "
        "use the mana) or hold it, what exact land to fetch (a real card from the "
        "remaining library), whether it should enter untapped (pay life on a "
        "shock when tempo matters and life is safe), which land to play from "
        "hand, and which utility lands to hold for later value.\n\n"
        "Respond with exactly these keys:\n"
        '  "crack_fetch": "now" | "end_of_turn" | "hold" | "auto",\n'
        '  "fetch_target": string (exact card name, or "" if not fetching),\n'
        '  "fetch_alternatives": [string] (ranked fallbacks, real library cards),\n'
        '  "enter_untapped": boolean,\n'
        '  "land_to_play": string (exact land from hand, or ""),\n'
        '  "land_alternatives": [string],\n'
        '  "color_needs": [string] (colors needed soon, priority order, W/U/B/R/G/C),\n'
        '  "hold_utility_lands": [string],\n'
        '  "reasoning": string'
    )


def _valid_library_pool(state: GraphState) -> set[str]:
    """Names the LLM may legally fetch/play: remaining library + current hand."""
    pool = {_norm(c) for c in _remaining_library(state)}
    pool.update(_norm(c) for c in (state.get("hand") or []))
    pool.discard("")
    return pool


def _coerce_mana_plan(raw: dict, state: GraphState) -> dict | None:
    """Validate the LLM's mana plan against cards actually available. Drops any
    fetch/land target that isn't in the remaining library or hand so the engine
    never gets a phantom card. Returns None if nothing usable remains."""
    if not isinstance(raw, dict):
        return None
    pool = _valid_library_pool(state)
    hand_pool = {_norm(c) for c in (state.get("hand") or [])}

    def _keep(name: str, allowed: set[str]) -> str:
        return name if _norm(name) in allowed else ""

    crack = str(raw.get("crack_fetch", "auto")).strip().lower()
    if crack not in ("now", "end_of_turn", "hold", "auto"):
        crack = "auto"
    fetch_target = _keep(str(raw.get("fetch_target", "")).strip(), pool)
    fetch_alts = [
        a
        for a in (str(x).strip() for x in raw.get("fetch_alternatives", []) or [])
        if a and _norm(a) in pool
    ][:5]
    land_to_play = _keep(str(raw.get("land_to_play", "")).strip(), hand_pool)
    land_alts = [
        a
        for a in (str(x).strip() for x in raw.get("land_alternatives", []) or [])
        if a and _norm(a) in hand_pool
    ][:5]
    color_needs = [
        c.strip().upper()
        for c in (str(x) for x in raw.get("color_needs", []) or [])
        if c.strip().upper() in ("W", "U", "B", "R", "G", "C")
    ]
    hold_util = [str(x).strip() for x in (raw.get("hold_utility_lands") or []) if str(x).strip()][
        :6
    ]
    plan = {
        "crack_fetch": crack,
        "fetch_target": fetch_target,
        "fetch_alternatives": fetch_alts,
        "enter_untapped": bool(raw.get("enter_untapped", True)),
        "land_to_play": land_to_play,
        "land_alternatives": land_alts,
        "color_needs": color_needs,
        "hold_utility_lands": hold_util,
        "reasoning": str(raw.get("reasoning", "")).strip(),
    }
    # Nothing actionable -> let the engine use stock heuristics.
    if not (fetch_target or land_to_play or color_needs or crack in ("now", "end_of_turn", "hold")):
        return None
    return plan


async def _run_mana_planner(state: GraphState) -> GraphState:
    """One focused LLM call that plans the AI's manabase for this action. Runs
    every in-game turn; fail-soft to no plan (engine uses stock heuristics)."""
    if int(state.get("turn", 0) or 0) <= 0:
        return {}
    deck = state.get("deck_cards") or []
    if not mana_profile.has_analyzable_manabase(deck):
        return {}  # basics-only deck: stock land logic is fine
    fmt = (state.get("resolved_format") or state.get("format") or "").lower()
    profile_obj = mana_profile.get_or_schedule(deck, state.get("own_archetype") or "", fmt)
    profile = profile_obj.model_dump() if profile_obj is not None else None
    try:
        log.info(
            "llm_call node=opponent_strategist purpose=mana_plan turn=%s have_profile=%s",
            state.get("turn", 0),
            profile is not None,
        )
        raw = await generate_json(
            _build_mana_prompt(state, profile),
            system=_MANA_SYSTEM_PROMPT,
            model=CONFIG.strategist_model_name,
            temperature=0.2,
        )
    except LLMError as exc:
        log.warning(
            "opponent_strategist: mana planner failed (%s); engine uses stock heuristics", exc
        )
        return {}
    plan = _coerce_mana_plan(raw, state)
    return {"mana_plan": plan} if plan else {}


async def opponent_strategist_node(state: GraphState) -> GraphState:
    """Broadened strategist: opponent inference (when recognized) AND own-deck
    mana planning, as two focused LLM calls run concurrently so total latency is
    the slower of the two, not their sum. Either branch is independently
    fail-soft; the node only ever enriches state."""
    if state.get("recognition_complete") is False:
        log.info("opponent_strategist: skipped because recognition is not complete")
        return state

    archetype = state.get("archetype") or ""
    fmt = (state.get("resolved_format") or state.get("format") or "").lower()

    opp_profile = None
    if archetype and not archetype.lower().startswith("off-meta") and archetype != "Unknown":
        opp_profile = loader.load_archetype_profile(archetype, fmt)

    async def _opp() -> GraphState:
        if not opp_profile:
            return {}
        return await _run_opponent_inference(state, opp_profile, fmt, archetype)

    opp_update, mana_update, action_update = await asyncio.gather(
        _opp(), _run_mana_planner(state), _run_action_planner(state)
    )

    out: GraphState = {**state}
    out.update(opp_update or {})
    out.update(mana_update or {})
    out.update(action_update or {})
    return out
