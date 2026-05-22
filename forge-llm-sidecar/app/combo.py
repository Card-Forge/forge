"""Deterministic combo-readiness analysis and action adjustment helpers."""

from __future__ import annotations

from collections import defaultdict

from app.schema import GraphState


def _norm(name: str) -> str:
    return "".join(ch for ch in (name or "").lower() if ch.isalnum())


def _names_by_norm(cards: list[str]) -> dict[str, str]:
    return {_norm(c): c for c in cards if c}


def _available_mana_count(state: GraphState) -> int:
    mana = state.get("available_mana") or []
    return len(mana)


def _open_blue_mana(state: GraphState) -> bool:
    return any("U" in (source or []) for source in (state.get("opp_untapped_sources") or []))


def _opponent_posture(state: GraphState, combo_profile: dict) -> tuple[str, float, list[str]]:
    archetype = (state.get("archetype") or "").lower()
    role = state.get("role") or {}
    opp_hand = state.get("opponent_hand") or []
    open_blue = _open_blue_mana(state)
    visible_hate = [
        card
        for card in (state.get("opponent_board") or [])
        if _norm(card) in {_norm(h) for h in combo_profile.get("visible_hate_cards") or []}
    ]
    likely_countermagic = open_blue or any(
        (g.get("category") or "").lower() in {"counterspell", "counterspells", "protection"}
        and float(g.get("probability") or 0.0) >= 0.25
        for g in opp_hand
        if isinstance(g, dict)
    )
    if visible_hate:
        return "hate", 90.0, [f"visible hate: {', '.join(visible_hate)}"]
    if likely_countermagic or "control" in archetype:
        return "control", 85.0, ["opponent has likely countermagic/open blue mana"]
    if any(word in archetype for word in ("aggro", "burn", "prowess", "energy")):
        return "aggro", 65.0, ["opponent pressure lowers the required setup threshold"]
    if "combo" in archetype:
        return "fast_combo", 65.0, ["opposing combo deck pressures the AI to act sooner"]
    if role.get("clock_score", 0.0) < -0.35:
        return "under_pressure", 65.0, ["opponent clock is materially faster"]
    return "normal", 75.0, []


def analyze_combo_state(state: GraphState, combo_profile: dict) -> dict:
    """Classify zones into combo buckets and compute a readiness plan."""
    hand = state.get("hand") or []
    board = state.get("own_board") or []
    graveyard = state.get("your_graveyard") or []
    hand_by_norm = _names_by_norm(hand)
    board_norm = {_norm(c) for c in board}
    graveyard_norm = {_norm(c) for c in graveyard}
    category_cards = combo_profile.get("category_cards") or {}

    present: dict[str, list[str]] = defaultdict(list)
    in_hand: dict[str, list[str]] = defaultdict(list)
    on_board: dict[str, list[str]] = defaultdict(list)
    in_graveyard: dict[str, list[str]] = defaultdict(list)
    for category, cards in category_cards.items():
        for card in cards or []:
            key = _norm(card)
            if key in hand_by_norm:
                present[category].append(hand_by_norm[key])
                in_hand[category].append(hand_by_norm[key])
            if key in board_norm:
                present[category].append(card)
                on_board[category].append(card)
            if key in graveyard_norm:
                present[category].append(card)
                in_graveyard[category].append(card)

    required = combo_profile.get("required_setup_categories") or []
    missing = [cat for cat in required if not present.get(cat)]
    mana_count = _available_mana_count(state)
    posture, threshold, risk_notes = _opponent_posture(state, combo_profile)
    protection_available = bool(present.get("protection"))
    if posture in {"control", "hate"} and protection_available:
        threshold = max(70.0, threshold - 10.0)

    reducer_ready = bool(on_board.get("reducers") or on_board.get("mana_reducers"))
    ritual_count = len(present.get("rituals", []))
    draw_count = len(present.get("draw_engine", []) + present.get("draw/engine", []))
    recursion_ready = bool(present.get("recursion"))
    payoff_ready = bool(present.get("payoff"))
    graveyard_fuel = sum(
        1
        for category in ("rituals", "draw_engine", "draw/engine")
        for _ in in_graveyard.get(category, [])
    )
    past_ready = recursion_ready and graveyard_fuel >= 3 and ritual_count >= 1

    score = 0.0
    score += 22.0 if reducer_ready else 10.0 if present.get("reducers") else 0.0
    score += min(24.0, ritual_count * 8.0)
    score += min(18.0, draw_count * 6.0)
    score += 20.0 if payoff_ready else 0.0
    score += 18.0 if past_ready else 8.0 if recursion_ready else 0.0
    score += min(8.0, max(0, mana_count - 1) * 2.0)
    if posture == "hate":
        score -= 18.0
    elif posture == "control" and not protection_available:
        score -= 10.0
    elif posture in {"aggro", "fast_combo", "under_pressure"}:
        score += 8.0
    score = max(0.0, min(100.0, score))

    known_lines = combo_profile.get("known_lines") or []
    preferred_line = ""
    if past_ready:
        preferred_line = "Past in Flames reload"
    elif payoff_ready and present.get("tutor"):
        preferred_line = "Wish/payoff line"
    elif reducer_ready and ritual_count >= 2 and (draw_count or payoff_ready):
        preferred_line = "Normal storm turn"
    elif known_lines:
        preferred_line = known_lines[0].get("name", "")

    selected_line = next(
        (line for line in known_lines if line.get("name", "").lower() == preferred_line.lower()),
        known_lines[0] if known_lines else {},
    )
    needed_cards = list(dict.fromkeys(missing))
    go = score >= threshold and not (posture == "hate" and not protection_available)
    wait_reason = ""
    if not go:
        if needed_cards:
            wait_reason = "Missing combo setup: " + ", ".join(needed_cards)
        elif posture in {"control", "hate"} and not protection_available:
            wait_reason = "Wait for protection or a bait spell before committing into disruption."
        else:
            wait_reason = f"Readiness {score:.0f} is below the {threshold:.0f} threshold."

    risk = "; ".join(risk_notes) if risk_notes else "No major disruption signal detected."
    return {
        "line_name": preferred_line,
        "go_for_it_now": go,
        "readiness_score": round(score, 1),
        "missing_pieces": needed_cards,
        "needed_cards": needed_cards,
        "needed_mana": selected_line.get("mana_requirement", ""),
        "preferred_line": preferred_line,
        "wait_reason": wait_reason,
        "sequence": [str(s) for s in (selected_line.get("sequence") or [])],
        "protection_plan": (
            "Use available protection before payoff or bait with a lower-value engine spell."
            if protection_available
            else "No protection currently visible."
        ),
        "risk_assessment": risk,
        "action_adjustments": _deterministic_adjustments(
            state, combo_profile, go, preferred_line, needed_cards
        ),
        "bucket_state": {
            "present": dict(present),
            "in_hand": dict(in_hand),
            "on_board": dict(on_board),
            "in_graveyard": dict(in_graveyard),
            "opponent_posture": posture,
            "go_threshold": threshold,
        },
    }


def _deterministic_adjustments(
    state: GraphState,
    combo_profile: dict,
    go: bool,
    preferred_line: str,
    missing: list[str],
) -> list[dict]:
    hand = state.get("hand") or []
    hand_norm = _names_by_norm(hand)
    category_cards = combo_profile.get("category_cards") or {}

    def first_in_hand(category: str) -> str:
        for card in category_cards.get(category, []) or []:
            match = hand_norm.get(_norm(card))
            if match:
                return match
        return ""

    adjustments: list[dict] = []
    if go:
        priority_categories = ["reducers", "rituals", "draw_engine", "recursion", "tutor", "payoff"]
        pct = 96.0
        for category in priority_categories:
            card = first_in_hand(category)
            if not card:
                continue
            adjustments.append(
                {
                    "action_type": "PLAY_SPELL",
                    "target": card,
                    "targets": None,
                    "percentage": pct,
                    "reasoning": f"Combo line '{preferred_line}' is ready; this card starts or continues the line.",
                }
            )
            pct = max(86.0, pct - 3.0)
        adjustments.append(
            {
                "action_type": "PASS",
                "target": "",
                "targets": None,
                "percentage": 8.0,
                "reasoning": "Combo readiness is high; passing gives up a likely line.",
            }
        )
        return adjustments

    setup_categories = ["reducers", "draw_engine", "protection", "tutor", "payoff"]
    pct = 82.0
    for category in setup_categories:
        card = first_in_hand(category)
        if not card:
            continue
        adjustments.append(
            {
                "action_type": "PLAY_SPELL",
                "target": card,
                "targets": None,
                "percentage": pct,
                "reasoning": "Combo setup action; avoid spending rituals until a full line is available.",
            }
        )
        pct = max(68.0, pct - 4.0)
    for land in hand:
        if land and any(token in land.lower() for token in ("mountain", "island", "spires", "vents", "fountain", "foundry", "canal")):
            adjustments.append(
                {
                    "action_type": "PLAY_LAND",
                    "target": land,
                    "targets": None,
                    "percentage": 78.0,
                    "reasoning": "Develop mana while assembling the combo.",
                }
            )
            break
    if missing:
        adjustments.append(
            {
                "action_type": "PASS",
                "target": "",
                "targets": None,
                "percentage": 28.0,
                "reasoning": "Wait unless a setup spell is available; missing " + ", ".join(missing) + ".",
            }
        )
    return adjustments


def merge_combo_adjustments(actions: list[dict], adjustments: list[dict]) -> list[dict]:
    """Append combo adjustments while replacing lower same-target recommendations."""
    if not adjustments:
        return actions
    out = list(actions or [])
    for adj in adjustments:
        if not isinstance(adj, dict):
            continue
        adj_type = adj.get("action_type")
        adj_target = adj.get("target")
        try:
            adj_pct = float(adj.get("percentage") or 0.0)
        except (TypeError, ValueError):
            adj_pct = 0.0
        replaced = False
        for idx, existing in enumerate(out):
            if existing.get("action_type") == adj_type and existing.get("target") == adj_target:
                try:
                    old_pct = float(existing.get("percentage") or 0.0)
                except (TypeError, ValueError):
                    old_pct = 0.0
                if adj_pct >= old_pct:
                    out[idx] = adj
                replaced = True
                break
        if not replaced:
            out.append(adj)
    return out
