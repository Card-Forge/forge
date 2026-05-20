"""Board-aware deterministic advice helpers for ``game_advisor``.

Each helper consumes the GraphState that the recognition node already has
populated, plus the AI's own piloting guide and the candidate archetypes for
the format. They produce structured advice that the Java side consumes
through ``SidecarInfluence``.

No LLM calls. Everything here is deterministic so it adds no latency on top
of the existing ``/recognize`` call.

The single overarching concept here is Mike Flores's "Who's the Beatdown?":
role assignment is matchup- and state-dependent and can flip during a game.
``assess_role`` makes that decision once and the other helpers consult it so
the advice stays internally consistent.
"""

from __future__ import annotations

from typing import Any

# --- card -> category mapping ------------------------------------------------
# Small, deterministic, hand-curated. Names not listed default to "threat" if
# they appear among an archetype's signature_cards, or "filler" otherwise.
# Match is case-insensitive and exact-name.
_CATEGORY_BY_NAME: dict[str, str] = {
    # counterspells
    "counterspell": "counterspell",
    "force of will": "counterspell",
    "force of negation": "counterspell",
    "mana leak": "counterspell",
    "remand": "counterspell",
    "spell pierce": "counterspell",
    "spell snare": "counterspell",
    "dispel": "counterspell",
    "negate": "counterspell",
    "cryptic command": "counterspell",
    "make disappear": "counterspell",
    "subtlety": "counterspell",
    # spot removal
    "lightning bolt": "removal",
    "fatal push": "removal",
    "path to exile": "removal",
    "swords to plowshares": "removal",
    "unholy heat": "removal",
    "go for the throat": "removal",
    "abrupt decay": "removal",
    "prismatic ending": "removal",
    "leyline binding": "removal",
    "doom blade": "removal",
    "cut down": "removal",
    "bloodchief's thirst": "removal",
    # wraths / board sweepers
    "wrath of god": "wrath",
    "damnation": "wrath",
    "supreme verdict": "wrath",
    "sunfall": "wrath",
    "farewell": "wrath",
    "toxic deluge": "wrath",
    "sweltering suns": "wrath",
    "anger of the gods": "wrath",
    "languish": "wrath",
    "pyroclasm": "wrath",
    # card draw / selection
    "brainstorm": "card_draw",
    "ponder": "card_draw",
    "preordain": "card_draw",
    "consider": "card_draw",
    "expressive iteration": "card_draw",
    "memory deluge": "card_draw",
    "sphinx's revelation": "card_draw",
    "treasure cruise": "card_draw",
    "dig through time": "card_draw",
    # known combo pieces (sample)
    "splinter twin": "combo_piece",
    "goryo's vengeance": "combo_piece",
    "through the breach": "combo_piece",
    "scapeshift": "combo_piece",
    "grapeshot": "combo_piece",
    "manamorphose": "combo_piece",
    "devoted druid": "combo_piece",
    "vizier of remedies": "combo_piece",
    "living end": "combo_piece",
    "primeval titan": "combo_piece",
    "amulet of vigor": "combo_piece",
}


def _norm(name: str) -> str:
    return (name or "").strip().lower()


def categorize(card_name: str, archetype_signature: set[str] | None = None) -> str:
    """Map a card name to a coarse category (counterspell/removal/wrath/etc.).

    Falls back to ``threat`` for cards present in the archetype's signature
    list, and ``filler`` otherwise.
    """
    n = _norm(card_name)
    if n in _CATEGORY_BY_NAME:
        return _CATEGORY_BY_NAME[n]
    if archetype_signature and n in {_norm(s) for s in archetype_signature}:
        return "threat"
    return "filler"


# --- role assessment (Who's the Beatdown?) -----------------------------------

_BEATDOWN_NATURAL = {"aggro", "tempo", "burn"}
_CONTROL_NATURAL = {"control", "ramp"}


def _natural_role(strategy_type: str | None) -> str:
    if not strategy_type:
        return "contested"
    s = str(strategy_type).lower()
    if s in _BEATDOWN_NATURAL:
        return "beatdown"
    if s in _CONTROL_NATURAL:
        return "control"
    return "contested"  # midrange, combo — context dependent


def _ai_human_life(life_totals: dict[str, Any]) -> tuple[int, int]:
    """Return ``(ai_life, human_life)`` from a loose life_totals dict.

    The Java side has not standardized the keys yet, so we accept several
    common shapes: ``{"ai": 20, "human": 20}``, ``{"you": 20, "opponent": 20}``,
    or raw player IDs ``{"1": 20, "2": 20}``. Falls back to (20, 20) when
    nothing matches.
    """
    if not isinstance(life_totals, dict) or not life_totals:
        return (20, 20)
    lt = {str(k).lower(): int(v) for k, v in life_totals.items() if isinstance(v, (int, float))}
    if "ai" in lt and "human" in lt:
        return lt["ai"], lt["human"]
    if "you" in lt and "opponent" in lt:
        return lt["you"], lt["opponent"]
    # fall back to first two values in insertion order
    values = list(lt.values())
    if len(values) >= 2:
        return values[0], values[1]
    if len(values) == 1:
        return values[0], 20
    return 20, 20


def _board_pressure(board: list[str]) -> float:
    """Crude board pressure: count of *non-land* permanents.

    Without a card db we cannot read P/T, so the count of threats on board
    is the closest proxy. Lands are intentionally excluded so that two
    players who have only played lands so far register as evenly contested.
    """
    if not board:
        return 0.0
    return float(sum(1 for c in board if c and not _is_land_name(c)))


def _card_advantage(hand: list[str], board: list[str], graveyard: list[str]) -> float:
    """A coarse card-advantage proxy: live cards minus expended ones."""
    return float(len(hand or []) + len(board or [])) - 0.25 * float(len(graveyard or []))


def assess_role(
    state: dict,
    ai_strategy: str | None,
    opp_strategy: str | None,
) -> dict:
    """Return a serialized ``RoleAssessment`` (dict) for the current state.

    Implements the role-flip rule that is the crux of "Who's the Beatdown?":
    the natural role from each deck's archetype is overridden when the live
    board state contradicts it.
    """
    ai_hand = state.get("hand", []) or []
    ai_board = state.get("own_board", []) or []
    ai_gy = state.get("your_graveyard", []) or []
    opp_board = state.get("opponent_board", []) or []
    opp_gy = state.get("opponent_graveyard", []) or []
    ai_life, human_life = _ai_human_life(state.get("life_totals", {}))

    ai_natural = _natural_role(ai_strategy)
    opp_natural = _natural_role(opp_strategy)

    ai_pressure = _board_pressure(ai_board)
    opp_pressure = _board_pressure(opp_board)
    # AI sees its own hand exactly; we don't see opponent's hand. Treat
    # opponent hand size as unknown (default 4 mid-game) for the proxy.
    opp_hand_size_proxy = max(0, 7 - state.get("turn", 0))
    ai_ca = _card_advantage(ai_hand, ai_board, ai_gy)
    opp_ca = _card_advantage([""] * opp_hand_size_proxy, opp_board, opp_gy)

    pressure_delta = ai_pressure - opp_pressure  # +ve means AI ahead on board
    ca_delta = ai_ca - opp_ca                    # +ve means AI ahead on cards
    life_delta = ai_life - human_life            # +ve means AI ahead on life

    # Flip rule: aggro side that is behind on BOTH board and cards must
    # transition into the control role and stabilize. Control side that is
    # ahead on board AND cards AND has a finite clock takes over the
    # beatdown role.
    ai_role = ai_natural
    role_flipped = False
    if ai_natural == "beatdown" and pressure_delta < 0 and ca_delta < 0:
        ai_role = "control"
        role_flipped = True
    elif ai_natural == "control" and pressure_delta > 1 and ca_delta > 0 and human_life <= 12:
        ai_role = "beatdown"
        role_flipped = True
    elif ai_natural == "contested":
        # Midrange / combo: settle the role from the board.
        if pressure_delta >= 1 or ca_delta >= 1:
            ai_role = "beatdown"
        elif pressure_delta <= -1 or ca_delta <= -1:
            ai_role = "control"

    # Mirror the same decision to flag the opponent's role symmetrically.
    opp_role = "control" if ai_role == "beatdown" else (
        "beatdown" if ai_role == "control" else opp_natural
    )

    # Winning side: weighted combination. Each component is normalized
    # roughly to ±1.
    score = (
        0.4 * max(-1.0, min(1.0, pressure_delta / 4.0))
        + 0.3 * max(-1.0, min(1.0, ca_delta / 4.0))
        + 0.3 * max(-1.0, min(1.0, life_delta / 10.0))
    )
    if score > 0.15:
        winning_side, margin = "ai", min(1.0, abs(score))
    elif score < -0.15:
        winning_side, margin = "human", min(1.0, abs(score))
    else:
        winning_side, margin = "even", abs(score)

    parts = [
        f"AI {ai_role} (natural {ai_natural}{', flipped' if role_flipped else ''})",
        f"life {ai_life}-{human_life}",
        f"board {int(ai_pressure)}-{int(opp_pressure)}",
        f"hand {len(ai_hand)} vs ~{opp_hand_size_proxy}",
    ]
    return {
        "ai_role": ai_role,
        "opponent_role": opp_role,
        "winning_side": winning_side,
        "margin": round(margin, 3),
        "role_flipped": role_flipped,
        "reasoning": "; ".join(parts),
    }


# --- hand valuation ----------------------------------------------------------


def _role_for_card_in_guide(card_name: str, guide: dict) -> str | None:
    n = _norm(card_name)
    for kc in (guide.get("key_cards") or []):
        if _norm(kc.get("name")) == n:
            return (kc.get("role") or "").lower() or "key_card"
    return None


def _is_land_name(card_name: str) -> bool:
    # Without a card db this is a best-effort heuristic on common basic and
    # dual-land names. Names not matched here are not treated as lands.
    n = _norm(card_name)
    basics = {"plains", "island", "swamp", "mountain", "forest", "wastes"}
    if n in basics:
        return True
    land_substrings = (
        " land", "shock", "fetch", "fountain", "spire", "ground", "shore",
        "tarn", "delta", "foundry", "harbor", "verge", "vents", "garden",
        "temple", "hideaway",
    )
    return any(s in n for s in land_substrings)


def score_hand(
    hand: list[str],
    guide: dict,
    phase_bucket: str,
    role: dict,
) -> list[dict]:
    """Per-card value scores for cards currently in the AI's hand.

    Values are 0-100. The piloting guide's ``key_cards`` provide role tags;
    cards not in the guide get a "filler" tag and a modest baseline value.
    Role-flipped scoring inverts the threat/answer preference.
    """
    if not hand:
        return []
    ai_role = (role or {}).get("ai_role", "contested")
    is_beatdown = ai_role == "beatdown"

    results: list[dict] = []
    for card in hand:
        if not card:
            continue
        guide_role = _role_for_card_in_guide(card, guide)
        if _is_land_name(card):
            tag = "land"
            base = 55.0
            reason = "land — develops mana"
        elif guide_role:
            tag = guide_role
            base = 80.0
            reason = f"key card in the piloting guide ({guide_role})"
        else:
            tag = "filler"
            base = 35.0
            reason = "not a key card in the guide"

        # Role modulation. Beatdown wants threats / win_cons; control wants
        # answers / card_draw.
        if tag in ("threat", "win_con") and is_beatdown:
            base += 12.0
            reason += "; boosted by beatdown role"
        elif tag in ("answer", "card_draw") and not is_beatdown:
            base += 12.0
            reason += "; boosted by control role"
        elif tag in ("threat", "win_con") and not is_beatdown:
            base -= 5.0
        elif tag in ("answer", "card_draw") and is_beatdown:
            base -= 5.0

        # Phase modulation — win_cons rise as the game goes long; ramp loses
        # value late.
        if tag == "win_con":
            base += {"early_game": -5.0, "mid_game": 5.0, "late_game": 15.0}.get(phase_bucket, 0.0)
        if tag == "ramp":
            base += {"early_game": 10.0, "mid_game": 0.0, "late_game": -10.0}.get(phase_bucket, 0.0)

        value = max(0.0, min(100.0, base))
        results.append(
            {
                "card": card,
                "value": round(value, 1),
                "role": tag,
                "reasoning": reason,
            }
        )
    # Stable sort: highest value first so the dashboard renders nicely.
    results.sort(key=lambda r: -r["value"])
    return results


# --- card-specific actions ---------------------------------------------------


def card_specific_actions(
    hand_values: list[dict],
    role: dict,
    phase_bucket: str,
) -> list[dict]:
    """Concrete card-name ActionScores that the Java side can match on.

    Picks the highest-value land (if any) for PLAY_LAND and the top three
    non-land cards for PLAY_SPELL. Percentages flow from the hand value plus
    a role-derived boost so the Java side gets a meaningful signal even when
    two cards are equally valuable.
    """
    if not hand_values:
        return []
    ai_role = (role or {}).get("ai_role", "contested")
    is_beatdown = ai_role == "beatdown"

    lands = [hv for hv in hand_values if hv.get("role") == "land"]
    spells = [hv for hv in hand_values if hv.get("role") != "land"]

    actions: list[dict] = []
    if lands:
        best_land = lands[0]
        actions.append(
            {
                "action_type": "PLAY_LAND",
                "target": best_land["card"],
                "targets": [hv["card"] for hv in lands[:3]],
                "percentage": min(95.0, 60.0 + best_land["value"] * 0.3),
                "reasoning": "Highest-value land in hand from board-aware scoring.",
            }
        )

    for hv in spells[:3]:
        boost = 10.0 if (is_beatdown and hv.get("role") in ("threat", "win_con")) else 0.0
        boost += 10.0 if (not is_beatdown and hv.get("role") in ("answer", "card_draw")) else 0.0
        actions.append(
            {
                "action_type": "PLAY_SPELL",
                "target": hv["card"],
                "targets": None,
                "percentage": min(95.0, hv["value"] + boost),
                "reasoning": f"Hand value {hv['value']:.0f} as {hv.get('role', 'card')}.",
            }
        )

    # Role-derived combat / pass percentages override the static baselines.
    if is_beatdown:
        actions.append(
            {
                "action_type": "ATTACK",
                "target": "all_available",
                "targets": None,
                "percentage": 75.0,
                "reasoning": "Beatdown role — race the opponent.",
            }
        )
        actions.append(
            {
                "action_type": "BLOCK",
                "target": "",
                "targets": None,
                "percentage": 15.0,
                "reasoning": "Beatdown role — take damage to push for lethal.",
            }
        )
        actions.append(
            {
                "action_type": "PASS",
                "target": "",
                "targets": None,
                "percentage": 10.0,
                "reasoning": "Beatdown role — pass only when no play exists.",
            }
        )
    elif ai_role == "control":
        actions.append(
            {
                "action_type": "ATTACK",
                "target": "",
                "targets": None,
                "percentage": 25.0,
                "reasoning": "Control role — attack only with safe damage.",
            }
        )
        actions.append(
            {
                "action_type": "BLOCK",
                "target": "",
                "targets": None,
                "percentage": 65.0,
                "reasoning": "Control role — protect life total.",
            }
        )
        actions.append(
            {
                "action_type": "PASS",
                "target": "",
                "targets": None,
                "percentage": 45.0,
                "reasoning": "Control role — hold up mana for interaction.",
            }
        )
    return actions


# --- opponent hand inference -------------------------------------------------


def infer_opponent_hand(
    archetype_name: str,
    candidate_archetypes: list[dict],
    observations: list[dict],
    opponent_board: list[str],
    opponent_graveyard: list[str],
    confidence: float,
    turn: int,
) -> list[dict]:
    """Bucketed guesses about what the human still has in hand."""
    if not archetype_name:
        return []
    record = None
    target_norm = _norm(archetype_name)
    for a in candidate_archetypes or []:
        if _norm(a.get("name")) == target_norm:
            record = a
            break
    if record is None:
        return []

    signatures = [str(s) for s in (record.get("signature_cards") or []) if s]
    if not signatures:
        return []

    seen = {_norm(o.get("card", "")) for o in (observations or [])}
    seen.update(_norm(c) for c in (opponent_board or []))
    seen.update(_norm(c) for c in (opponent_graveyard or []))

    signature_set = {_norm(s) for s in signatures}
    unseen = [s for s in signatures if _norm(s) not in seen]
    if not unseen:
        return []

    # Bucket by category.
    by_cat: dict[str, list[str]] = {}
    for card in unseen:
        cat = categorize(card, archetype_signature=signature_set)
        by_cat.setdefault(cat, []).append(card)

    # Probability heuristic: archetype confidence × share of signatures unseen
    # × turn-decay (the later the turn the more likely they've drawn copies).
    total = max(1, len(signatures))
    unseen_share = len(unseen) / total
    turn_factor = max(0.4, 1.0 - 0.05 * max(0, turn))

    out: list[dict] = []
    for cat, cards in by_cat.items():
        cat_share = len(cards) / total
        prob = confidence * (0.5 * unseen_share + 0.5 * cat_share) * turn_factor
        prob = max(0.0, min(1.0, prob + 0.1))  # small floor so non-zero
        out.append(
            {
                "category": cat,
                "example_cards": cards[:4],
                "probability": round(prob, 3),
                "reasoning": (
                    f"{len(cards)} of {total} signature cards in this category "
                    f"unseen on turn {turn}."
                ),
            }
        )
    out.sort(key=lambda r: -r["probability"])
    return out


# --- target priorities -------------------------------------------------------


def target_priorities(
    opponent_board: list[str],
    opp_archetype_record: dict | None,
    guide: dict,
    role: dict,
) -> list[dict]:
    """Rank opponent permanents by how badly the AI wants them removed.

    One generic entry plus, eventually, per-spell entries. For now the
    generic entry is the workhorse; per-spell ranking can be layered in
    later without changing the schema.
    """
    if not opponent_board:
        return []

    ai_role = (role or {}).get("ai_role", "contested")
    signatures = {
        _norm(s) for s in ((opp_archetype_record or {}).get("signature_cards") or [])
    }
    threats = {_norm(t) for t in (guide.get("common_threats") or [])}

    scored: list[tuple[float, str]] = []
    for card in opponent_board:
        n = _norm(card)
        score = 1.0
        reasons = []
        if n in signatures:
            score += 3.0
            reasons.append("archetype signature")
        if n in threats:
            score += 2.0
            reasons.append("matchup threat")
        # Role modulation: beatdown wants blockers gone; control wants the
        # clock gone.
        cat = categorize(card)
        if ai_role == "beatdown" and cat in ("threat", "filler"):
            score += 1.0
        if ai_role == "control" and cat == "threat":
            score += 1.5
        scored.append((score, card))

    scored.sort(key=lambda t: -t[0])
    ordered = [card for _score, card in scored]
    return [
        {
            "spell": "",
            "targets": ordered[:6],
            "reasoning": (
                f"{ai_role.title()} priorities: archetype signatures first, "
                "then matchup threats."
            ),
        }
    ]
