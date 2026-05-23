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

import json
import logging

from app.config import CONFIG
from app.knowledge import loader
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


def _format_profile_block(profile: dict, prior: dict[str, float], revealed: set[str], fmt: str, archetype: str) -> str:
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
        lines.append(f"- {bucket}: target {target}, seen {seen_n}, remaining {remaining}{p_str} -> {card_str}")
    combos = (buckets.get("combo_pieces") or {}).get("pairs") or []
    combo_str = "; ".join(" + ".join(pair) for pair in combos) if combos else "(none)"
    dual = "; ".join(
        f"{d.get('card')} = {'/'.join(d.get('roles', []))}"
        for d in (profile.get("dual_role_cards") or [])
    ) or "(none)"
    pred = "; ".join(
        f"if [{p.get('trigger')}] -> {p.get('line')}" for p in (profile.get("predicted_lines") or [])
    ) or "(none)"
    return (
        f"Archetype macro plan: {profile.get('macro_plan', '')}\n"
        f"Win-turn window: {profile.get('win_turn_window', [])}\n"
        f"Role buckets (many-to-many; a card can be in several):\n" + "\n".join(lines) + "\n"
        f"Key combos: {combo_str}\n"
        f"Multi-role cards: {dual}\n"
        f"Kill priority (most dangerous first): {', '.join(profile.get('kill_priority', [])) or '(none)'}\n"
        f"Their interaction to play around: {', '.join(profile.get('interaction_to_disrupt', [])) or '(none)'}\n"
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


def _build_prompt(state: GraphState, profile: dict, prior: dict[str, float], revealed: set[str]) -> str:
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


def _valid_card_pool(profile: dict, revealed: set[str], fmt: str, archetype: str) -> set[str]:
    pool = set(revealed)
    for bucket, payload in (profile.get("buckets") or {}).items():
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


async def opponent_strategist_node(state: GraphState) -> GraphState:
    archetype = state.get("archetype") or ""
    fmt = (state.get("resolved_format") or state.get("format") or "").lower()
    if not archetype or archetype.lower().startswith("off-meta") or archetype == "Unknown":
        return state

    profile = loader.load_archetype_profile(archetype, fmt)
    if not profile:
        return state  # graceful: keep deterministic opponent_hand

    confidence = state.get("confidence") or 0.0
    decision = _norm(state.get("decision_type") or "")
    if confidence < _MIN_CONFIDENCE and decision not in _ALWAYS_RUN_DECISIONS:
        return state

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
        log.warning("opponent_strategist: LLM call failed (%s); keeping deterministic inference", exc)
        # Beatdown is board-score driven, so emit it even when the LLM is down.
        return {**state, "beatdown_assessment": _beatdown_from_board(state)}

    pool = _valid_card_pool(profile, revealed, fmt, archetype)

    # --- opponent hand: profile-bucket guesses replace signature-only guesses
    opponent_hand: list[dict] = []
    bucket_probs = result.get("bucket_probabilities")
    if isinstance(bucket_probs, dict):
        for bucket, payload in bucket_probs.items():
            if bucket not in loader.PROFILE_BUCKETS or not isinstance(payload, dict):
                continue
            cards = [
                c for c in (payload.get("top_cards") or [])
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

    out: GraphState = {**state}
    if opponent_hand:
        out["opponent_hand"] = opponent_hand
    out["target_priorities"] = target_priorities
    if predicted_opp_line:
        out["predicted_opp_line"] = predicted_opp_line
    if beatdown_assessment:
        out["beatdown_assessment"] = beatdown_assessment
    return out
