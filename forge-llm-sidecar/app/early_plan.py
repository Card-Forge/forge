"""Deterministic opening-hand and rolling early-game planning helpers."""

from __future__ import annotations

from collections import Counter
from math import comb

from app import advice
from app.schema import GraphState

_COLOR_WORDS = {
    "W": ("white", "plains"),
    "U": ("blue", "island"),
    "B": ("black", "swamp"),
    "R": ("red", "mountain"),
    "G": ("green", "forest"),
}

_LAND_COLORS = {
    "plains": ["W"],
    "island": ["U"],
    "swamp": ["B"],
    "mountain": ["R"],
    "forest": ["G"],
    "hallowed fountain": ["W", "U"],
    "watery grave": ["U", "B"],
    "blood crypt": ["B", "R"],
    "stomping ground": ["R", "G"],
    "temple garden": ["G", "W"],
    "godless shrine": ["W", "B"],
    "steam vents": ["U", "R"],
    "overgrown tomb": ["B", "G"],
    "sacred foundry": ["R", "W"],
    "breeding pool": ["G", "U"],
    "raucous theater": ["B", "R"],
    "thundering falls": ["U", "R"],
    "commercial district": ["R", "G"],
    "lush portico": ["G", "W"],
    "meticulous archive": ["W", "U"],
    "undercity sewers": ["U", "B"],
    "underground mortuary": ["B", "G"],
    "elegant parlor": ["R", "W"],
    "hedge maze": ["G", "U"],
    "shadowy backstreet": ["W", "B"],
}

_SURVEIL_LANDS = {
    "raucous theater",
    "thundering falls",
    "commercial district",
    "lush portico",
    "meticulous archive",
    "undercity sewers",
    "underground mortuary",
    "elegant parlor",
    "hedge maze",
    "shadowy backstreet",
}

_FETCH_HINTS = ("fetch", "mesa", "strand", "delta", "catacombs", "foothills", "tarn", "rainforest", "flats", "marsh", "heath")


def _norm(name: str) -> str:
    return (name or "").strip().lower()


def _is_land(name: str) -> bool:
    return advice._is_land_name(name) or _norm(name) in _LAND_COLORS or _is_fetch(name)


def _is_fetch(name: str) -> bool:
    n = _norm(name)
    return any(h in n for h in _FETCH_HINTS)


def _enters_tapped(name: str) -> bool:
    n = _norm(name)
    return n in _SURVEIL_LANDS or "triome" in n or "temple of " in n or "tapped" in n


def _land_colors(name: str) -> list[str]:
    n = _norm(name)
    if n in _LAND_COLORS:
        return _LAND_COLORS[n]
    out: list[str] = []
    for code, words in _COLOR_WORDS.items():
        if any(w in n for w in words):
            out.append(code)
    return out or (["W", "U", "B", "R", "G"] if _is_fetch(name) else ["C"])


def _spell_colors(name: str, guide: dict) -> list[str]:
    n = _norm(name)
    out: list[str] = []
    # Infer from common card names/guide role text. This is intentionally
    # conservative; Forge still does real legality/payability checks.
    for code, words in _COLOR_WORDS.items():
        if any(w in n for w in words):
            out.append(code)
    if "bolt" in n or "ritual" in n or "grapeshot" in n or "ragavan" in n:
        out.append("R")
    if "counter" in n or "consider" in n or "memory" in n:
        out.append("U")
    if "thoughtseize" in n or "fatal push" in n:
        out.append("B")
    if "prismatic ending" in n or "solitude" in n:
        out.append("W")
    if "veil of summer" in n or "green sun" in n:
        out.append("G")
    guide_text = " ".join(
        str(x.get("notes", "")) + " " + str(x.get("role", ""))
        for x in (guide.get("key_cards") or [])
        if _norm(x.get("name", "")) == n
    ).lower()
    for code, words in _COLOR_WORDS.items():
        if any(w in guide_text for w in words):
            out.append(code)
    return list(dict.fromkeys(out))


def _draw_probability(successes: int, population: int, looks: int) -> float:
    if population <= 0 or successes <= 0 or looks <= 0:
        return 0.0
    successes = min(successes, population)
    looks = min(looks, population)
    misses = population - successes
    if misses < looks:
        return 1.0
    return 1.0 - (comb(misses, looks) / comb(population, looks))


def _legal_names(state: GraphState, action_type: str) -> set[str]:
    return {
        str(a.get("card") or a.get("target") or "")
        for a in (state.get("legal_actions") or [])
        if a.get("action_type") == action_type
    }


def _guide_matches(hand: list[str], guide: dict) -> tuple[list[str], list[str]]:
    hand_text = " ".join(hand).lower()
    mull = guide.get("mulligan") or {}
    keeps = [c for c in mull.get("keep_criteria", []) if any(tok in hand_text for tok in c.lower().split() if len(tok) > 3)]
    ships = [c for c in mull.get("mulligan_criteria", []) if any(tok in hand_text for tok in c.lower().split() if len(tok) > 3)]
    return keeps[:3], ships[:3]


def build_early_game_plan(state: GraphState) -> dict:
    hand = [str(c) for c in (state.get("hand") or []) if c]
    deck = [str(c) for c in (state.get("deck_cards") or []) if c]
    guide = state.get("piloting_guide") or {}
    turn = int(state.get("turn") or 0)
    influence = max(0, min(100, int(state.get("sidecar_influence") or 50)))
    cards_to_return = max(0, int(state.get("cards_to_return") or 0))
    lands = [c for c in hand if _is_land(c)]
    spells = [c for c in hand if not _is_land(c)]
    legal_lands = _legal_names(state, "PLAY_LAND")
    legal_spells = _legal_names(state, "PLAY_SPELL")

    keep_matches, ship_matches = _guide_matches(hand, guide)
    land_count = len(lands)
    low_curve = len(spells)
    key_names = {_norm(k.get("name", "")) for k in (guide.get("key_cards") or [])}
    key_hits = [c for c in hand if _norm(c) in key_names]
    score = 45.0
    score += 16.0 if 2 <= land_count <= 4 else -24.0 if land_count == 0 or land_count >= 6 else -8.0
    score += min(18.0, low_curve * 4.0)
    score += min(18.0, len(key_hits) * 8.0)
    score += len(keep_matches) * 7.0
    score -= len(ship_matches) * 8.0
    if cards_to_return > 0:
        score -= cards_to_return * 4.0
    score = max(0.0, min(100.0, score))
    decision = "keep" if score >= 52.0 else "mulligan"
    confidence = abs(score - 50.0) / 50.0

    known = Counter(hand + (state.get("own_board") or []) + (state.get("your_graveyard") or []))
    remaining = list(deck)
    for card, count in known.items():
        for _ in range(count):
            try:
                remaining.remove(card)
            except ValueError:
                break
    remaining_lands = sum(1 for c in remaining if _is_land(c))
    draw_looks = 1 + sum(1 for c in spells if any(tok in _norm(c) for tok in ("consider", "bauble", "wrenn", "resolve", "impulse", "draw")))
    land_draw_prob = _draw_probability(remaining_lands, len(remaining), draw_looks)

    needed_colors = []
    for card in spells:
        for color in _spell_colors(card, guide):
            if color not in needed_colors:
                needed_colors.append(color)

    land_sequence = _sequence_lands(lands, needed_colors)
    planned_turns = _planned_turns(state, land_sequence, spells, legal_spells)
    bottom_cards = _bottom_cards(hand, lands, spells, key_hits, cards_to_return)
    fetch_plan = _fetch_plan(lands, needed_colors)
    win_turn = _estimate_win_turn(guide, state, decision)
    loss_turn = _estimate_loss_turn(state)
    actions = _plan_actions(state, decision, score, land_sequence, planned_turns, influence)

    return {
        "decision": decision,
        "confidence": round(confidence, 2),
        "influence_weight": influence,
        "keep_reason": "; ".join(keep_matches) or f"{land_count} land(s), {low_curve} spell(s), {len(key_hits)} key card(s).",
        "mulligan_reason": "; ".join(ship_matches) or ("Mana or curve is weak." if decision == "mulligan" else ""),
        "bottom_cards": bottom_cards,
        "land_sequence": land_sequence,
        "fetch_plan": fetch_plan,
        "planned_turns": planned_turns,
        "draw_assumptions": [
            f"{draw_looks} estimated look(s) before the next key decision.",
            f"Chance to draw a land in those looks: {land_draw_prob * 100:.0f}%.",
        ],
        "probability_notes": f"Remaining deck estimate: {remaining_lands}/{len(remaining) or 1} lands.",
        "contingencies": _contingencies(state),
        "estimated_win_turn": win_turn,
        "estimated_loss_turn": loss_turn,
        "reasoning": "Guide criteria plus deterministic mana/curve/draw analysis.",
        "last_updated_turn": turn,
        "action_adjustments": actions,
    }


def _sequence_lands(lands: list[str], needed_colors: list[str]) -> list[str]:
    def score(land: str) -> tuple[int, int, str]:
        colors = _land_colors(land)
        overlap = sum(1 for c in needed_colors if c in colors)
        tapped = 1 if _enters_tapped(land) else 0
        fetch = 1 if _is_fetch(land) else 0
        return (-tapped, overlap + fetch, land)

    tapped = [l for l in lands if _enters_tapped(l)]
    untapped = [l for l in lands if not _enters_tapped(l)]
    ordered = sorted(lands, key=score, reverse=True)
    if tapped and len(lands) >= 2:
        first_tapped = sorted(tapped, key=lambda l: len(set(_land_colors(l)) & set(needed_colors)), reverse=True)[0]
        if first_tapped in ordered:
            ordered.remove(first_tapped)
        ordered.insert(0, first_tapped)
    return ordered[:4]


def _planned_turns(state: GraphState, land_sequence: list[str], spells: list[str], legal_spells: set[str]) -> list[dict]:
    turn = int(state.get("turn") or 0)
    if turn <= 0:
        start, end = 1, 4
    else:
        start, end = turn + 1, turn + 4
    out: list[dict] = []
    spell_idx = 0
    for t in range(start, end + 1):
        land = land_sequence[min(t - start, len(land_sequence) - 1)] if land_sequence else ""
        turn_spells: list[str] = []
        if spell_idx < len(spells):
            candidate = spells[spell_idx]
            if not legal_spells or candidate in legal_spells or t > turn:
                turn_spells.append(candidate)
                spell_idx += 1
        if spell_idx < len(spells) and t >= start + 1:
            turn_spells.append(spells[spell_idx])
            spell_idx += 1
        out.append(
            {
                "turn": t,
                "land": land,
                "spells": turn_spells,
                "mana_rationale": _land_rationale(land),
                "draw_assumption": "Assumes normal draw step plus any planned cantrip/impulse draw.",
                "opponent_adjustment": "Replan if opponent presents removal, countermagic, or a faster clock.",
            }
        )
    return out


def _land_rationale(land: str) -> str:
    if not land:
        return "No land currently planned."
    if _is_fetch(land):
        return "Fetch based on the colors needed by the current hand first, deck needs second."
    if _enters_tapped(land):
        return "Tapped/surveil land sequenced early to reduce tempo loss."
    return "Untapped land supports casting planned spells on curve."


def _bottom_cards(hand: list[str], lands: list[str], spells: list[str], key_hits: list[str], n: int) -> list[str]:
    if n <= 0:
        return []
    protected = set(key_hits)
    candidates = []
    if len(lands) > 3:
        candidates.extend(lands[3:])
    candidates.extend([s for s in reversed(spells) if s not in protected])
    candidates.extend([c for c in hand if c not in candidates and c not in protected])
    return candidates[:n]


def _fetch_plan(lands: list[str], needed_colors: list[str]) -> list[str]:
    plans = []
    for land in lands:
        if not _is_fetch(land):
            continue
        if needed_colors:
            plans.append(f"{land}: fetch {'/'.join(needed_colors[:2])} source for hand first.")
        else:
            plans.append(f"{land}: fetch the color pair most represented in remaining deck spells.")
    return plans


def _estimate_win_turn(guide: dict, state: GraphState, decision: str) -> int | None:
    if decision != "keep":
        return None
    strategy = (guide.get("strategy_type") or "").lower()
    turn = int(state.get("turn") or 0)
    if strategy == "combo":
        return max(turn + 3, 4)
    if strategy == "aggro":
        return max(turn + 4, 5)
    if strategy == "control":
        return max(turn + 7, 8)
    return max(turn + 5, 6)


def _estimate_loss_turn(state: GraphState) -> int | None:
    role = state.get("role") or {}
    turn = int(state.get("turn") or 0)
    try:
        clock = float(role.get("clock_score") or 0.0)
    except (TypeError, ValueError):
        clock = 0.0
    if clock < -0.45:
        return max(turn + 2, 3)
    if state.get("opponent_board"):
        return max(turn + 4, 5)
    return None


def _contingencies(state: GraphState) -> list[str]:
    out = []
    if state.get("opp_untapped_sources"):
        out.append("Respect open opponent mana; prefer bait/protection before key plays.")
    if state.get("opponent_board"):
        out.append("Opponent board pressure may require interaction over planned development.")
    return out or ["Continue executing the planned curve unless opponent action changes priorities."]


def _plan_actions(
    state: GraphState,
    decision: str,
    score: float,
    land_sequence: list[str],
    planned_turns: list[dict],
    influence: int,
) -> list[dict]:
    pct = max(0.0, min(100.0, score))
    actions = [
        {
            "action_type": "MULLIGAN",
            "target": decision,
            "targets": None,
            "percentage": pct if decision == "keep" else 100.0 - pct,
            "reasoning": "Early-game planner keep/mulligan decision.",
        }
    ]
    current_turn = int(state.get("turn") or 0)
    current_plan = next((p for p in planned_turns if p.get("turn") == current_turn), None) or (planned_turns[0] if planned_turns else {})
    legal_lands = _legal_names(state, "PLAY_LAND")
    legal_spells = _legal_names(state, "PLAY_SPELL")
    land = current_plan.get("land") or (land_sequence[0] if land_sequence else "")
    if land and (not legal_lands or land in legal_lands):
        actions.append(
            {
                "action_type": "PLAY_LAND",
                "target": land,
                "targets": None,
                "percentage": min(100.0, 60.0 + influence * 0.4),
                "reasoning": current_plan.get("mana_rationale") or _land_rationale(land),
            }
        )
    for spell in current_plan.get("spells") or []:
        if spell and (not legal_spells or spell in legal_spells):
            actions.append(
                {
                    "action_type": "PLAY_SPELL",
                    "target": spell,
                    "targets": None,
                    "percentage": min(100.0, 58.0 + influence * 0.4),
                    "reasoning": "Current rolling early-game plan priority.",
                }
            )
    return actions


def filter_actions_to_legal(state: GraphState, actions: list[dict]) -> list[dict]:
    """Drop current play recommendations outside the adapter-provided legal set."""
    legal_by_type = {
        "PLAY_LAND": _legal_names(state, "PLAY_LAND"),
        "PLAY_SPELL": _legal_names(state, "PLAY_SPELL"),
        "ACTIVATE_ABILITY": _legal_names(state, "ACTIVATE_ABILITY"),
    }
    if not any(legal_by_type.values()):
        return actions
    out = []
    for action in actions:
        action_type = action.get("action_type")
        legal = legal_by_type.get(action_type)
        if legal is not None and legal:
            target = str(action.get("target") or "")
            if target and target not in legal:
                continue
        out.append(action)
    return out


def merge_plan_actions(actions: list[dict], plan_actions: list[dict]) -> list[dict]:
    out = [a for a in (plan_actions or []) if a.get("action_type") != "MULLIGAN"]
    out.extend(actions or [])
    for action in plan_actions:
        if action.get("action_type") == "MULLIGAN":
            out = [a for a in out if a.get("action_type") != "MULLIGAN"]
            out.append(action)
    return out
