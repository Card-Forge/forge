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


# Common non-creature permanent types we can identify by name fragments.
# Used so ATTACK isn't recommended when own_board only has lands, planeswalkers
# or artifacts/enchantments. We bias toward false-negative (assume creature)
# because we'd rather under-attack than over-attack.
_PLANESWALKER_NAME_PREFIXES = (
    "jace,", "liliana,", "chandra,", "garruk,", "nissa,", "teferi,", "ajani,",
    "elspeth,", "sorin,", "vraska,", "narset,", "kaya,", "tamiyo,", "kasmina,",
    "ugin,", "karn,", "domri,", "ral,", "saheeli,", "wrenn", "the wandering",
    "oko,", "tibalt,", "ashiok,", "tezzeret,", "venser,", "kiora,", "samut,",
)


def _is_likely_noncreature_nonland(card_name: str) -> bool:
    """Best-effort: return True for permanents that are clearly NOT creatures.

    We only catch obvious cases (planeswalkers by ", <profession>" pattern,
    Sol Ring / Mana Vault style mana rocks). Anything we can't classify is
    assumed creature so the AI doesn't pass when it could swing.
    """
    n = _norm(card_name)
    if not n:
        return True
    for p in _PLANESWALKER_NAME_PREFIXES:
        if n.startswith(p):
            return True
    # Mana rocks / signets / talismans / commonly-seen non-creature artifacts.
    noncreature_substrings = (
        "signet", "talisman of", "sol ring", "arcane signet", "fellwar stone",
        "mind stone", "thought vessel", "chromatic ", "wayfarer's bauble",
    )
    return any(s in n for s in noncreature_substrings)


def count_possible_attackers(board: list[str]) -> int:
    """Count permanents that might be creatures we could attack with.

    Lands and obvious non-creatures are excluded; everything else is assumed
    creature-shaped. This is a heuristic — without card-type data from Forge we
    cannot tell creature from artifact-creature etc. — so we err toward false
    negatives (assume creature) for unknown names.
    """
    if not board:
        return 0
    return sum(
        1
        for c in board
        if c and not _is_land_name(c) and not _is_likely_noncreature_nonland(c)
    )


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


def estimate_opponent_hand_size(state: dict) -> int:
    """Cards in the human opponent's hand right now.

    Prefers the explicit `opp_hand_size` field (Forge sends this) and falls
    back to a heuristic for legacy clients that omit it.
    """
    explicit = state.get("opp_hand_size")
    if isinstance(explicit, int) and explicit > 0:
        return explicit
    # Fallback heuristic for older clients.
    turn = int(state.get("turn") or 0)
    observations = state.get("observations") or []
    plays_seen = sum(
        1
        for o in observations
        if isinstance(o, dict) and (o.get("event") in ("spell", "land", "permanent"))
    )
    estimate = 7 + max(0, turn) - plays_seen
    return max(0, min(15, estimate))


# --- tempo signals ----------------------------------------------------------

def opponent_lands_dropped(state: dict) -> int:
    """Count of lands the opponent has played, from observations.

    Each unique land observed at event="land" counts as one drop. Lands that
    later went to the graveyard still count — they were dropped at some point.
    """
    observations = state.get("observations") or []
    return sum(
        1
        for o in observations
        if isinstance(o, dict) and o.get("event") == "land"
    )


def opponent_missed_drops(state: dict) -> int:
    """How many turns the opponent has missed a land drop.

    On the play, by turn N a player should have N land drops. Cap at the
    current turn so this is a strict non-negative integer.
    """
    turn = int(state.get("turn") or 0)
    if turn <= 0:
        return 0
    dropped = opponent_lands_dropped(state)
    return max(0, turn - dropped)


_COLOR_LETTERS = ("W", "U", "B", "R", "G")


def opponent_color_screwed(state: dict, opp_record: dict | None) -> bool:
    """True when the opponent has visibly fewer colors than their deck needs.

    Compares the opponent's known mana sources (from Forge, or inferred from
    observed lands' color identity) to the colors of their inferred archetype.
    Only fires from turn 3 onwards — earlier it's just normal tempo.
    """
    turn = int(state.get("turn") or 0)
    if turn < 3 or not opp_record:
        return False
    deck_colors = {c.upper() for c in (opp_record.get("colors") or []) if c}
    deck_colors.discard("C")
    if len(deck_colors) <= 1:
        return False
    seen = set(c.upper() for c in (state.get("opponent_mana_colors_seen") or []))
    if not seen:
        # Infer from observed lands' colors.
        for o in state.get("observations") or []:
            if (o or {}).get("event") == "land":
                for c in o.get("colors") or []:
                    seen.add((c or "").upper())
    seen &= set(_COLOR_LETTERS)
    needed = deck_colors & set(_COLOR_LETTERS)
    return bool(needed - seen)


# --- graveyard utility ------------------------------------------------------

# Cards/keywords on cards that get value from being in the graveyard. The
# Java side doesn't send oracle text, so we maintain a curated index of names
# whose presence in the graveyard implies utility (flashback, escape, dredge,
# delve fuel, unearth, jump-start, reanimation targets, etc.).
_GRAVEYARD_ACTIVE_CARDS = {
    # delve fuel + payoff
    "treasure cruise", "dig through time", "tombstalker", "murktide regent",
    # flashback / jump-start staples
    "ancestral vision", "snapcaster mage", "lingering souls", "faithless looting",
    # escape
    "uro, titan of nature's wrath", "kroxa, titan of death's hunger",
    # reanimation targets
    "griselbrand", "iona, shield of emeria", "archon of cruelty",
    # cascade-into-graveyard payoffs
    "living end", "shardless agent",
    # delirium / madness
    "tarmogoyf", "death's shadow",
    # dredge
    "narcomoeba", "stinkweed imp", "golgari thug", "life from the loam",
    # crucible-style recursion
    "crucible of worlds", "ramunap excavator",
}

# Archetypes whose name implies the graveyard is a strategic resource.
_GRAVEYARD_ARCHETYPES = (
    "living end", "dredge", "reanimator", "murktide", "delirium", "goryo",
    "delve", "ranking", "phoenix", "uro", "escape",
)


def graveyard_utility(
    opponent_graveyard: list[str],
    archetype_name: str | None,
    opp_record: dict | None,
) -> tuple[float, list[str]]:
    """Score how much the opponent's graveyard matters to their deck.

    Returns ``(utility, signals)`` where utility is in [0,1] and signals is a
    short list of why we think the graveyard matters.
    """
    if not opponent_graveyard:
        return 0.0, []
    signals: list[str] = []
    utility = 0.0

    name_lower = (archetype_name or "").lower()
    arch_hit = next((kw for kw in _GRAVEYARD_ARCHETYPES if kw in name_lower), None)
    if arch_hit:
        utility += 0.5
        signals.append(f"archetype '{archetype_name}' uses the graveyard")

    signature = {_norm(s) for s in ((opp_record or {}).get("signature_cards") or [])}
    active_in_signature = signature & _GRAVEYARD_ACTIVE_CARDS
    if active_in_signature:
        utility += 0.2
        signals.append(
            f"{len(active_in_signature)} graveyard-active signature card(s)"
        )

    active_in_gy = [c for c in opponent_graveyard if _norm(c) in _GRAVEYARD_ACTIVE_CARDS]
    if active_in_gy:
        utility += min(0.4, 0.1 * len(active_in_gy))
        signals.append(
            f"{len(active_in_gy)} card(s) in graveyard with known utility: "
            + ", ".join(active_in_gy[:3])
        )

    # Sheer mass: delve/dredge benefit from a fat graveyard even if individual
    # cards aren't tagged.
    if arch_hit and len(opponent_graveyard) >= 5:
        utility += 0.15
        signals.append(f"{len(opponent_graveyard)} cards in graveyard fuels the plan")

    return max(0.0, min(1.0, utility)), signals


# --- clock with blocks ------------------------------------------------------

def _creatures_from_details(board_details: list[dict]) -> list[dict]:
    """Filter board details down to creatures with known P/T."""
    out = []
    for c in board_details or []:
        if not isinstance(c, dict):
            continue
        if c.get("is_creature") or "Creature" in (c.get("types") or []):
            p = c.get("power")
            t = c.get("toughness")
            if isinstance(p, int) and isinstance(t, int) and p >= 0 and t >= 0:
                out.append({"name": c.get("name", "?"), "power": p, "toughness": t,
                            "tapped": bool(c.get("tapped"))})
    return out


def best_damage_through(
    attackers_details: list[dict],
    blockers_details: list[dict],
) -> int:
    """Greedy estimate of how much damage gets through after optimal blocks.

    Both sides have full P/T. We pair attackers with blockers greedily:
    blockers volunteer to chump or trade; the attacker side picks the order
    that maximizes damage through. This is a coarse upper-bound estimate but
    avoids the false "1 attacker = lethal in 20 turns" trap when there's a
    real board stall.
    """
    attackers = sorted(
        [a for a in attackers_details if not a.get("tapped")],
        key=lambda a: -a.get("power", 0),
    )
    blockers = sorted(
        [b for b in blockers_details if not b.get("tapped")],
        key=lambda b: -b.get("toughness", 0),
    )
    if not attackers:
        return 0
    available_blockers = list(blockers)
    damage = 0
    for atk in attackers:
        if not available_blockers:
            damage += atk["power"]
            continue
        # Best blocker = smallest one whose toughness >= attacker's power,
        # falling back to the largest blocker for a chump.
        survivors = [b for b in available_blockers if b["toughness"] > atk["power"]]
        if survivors:
            # Trade or eat the attacker; no damage through.
            blocker = min(survivors, key=lambda b: b["toughness"])
            available_blockers.remove(blocker)
            continue
        # Chump with the smallest blocker — attacker still survives.
        chump = min(available_blockers, key=lambda b: b["power"])
        available_blockers.remove(chump)
        # If the chump is bigger than attacker's toughness, it trades; no damage.
        if chump["power"] >= atk["toughness"]:
            continue
        damage += atk["power"]
    return damage


def turns_to_lethal(
    attackers_details: list[dict],
    blockers_details: list[dict],
    defender_life: int,
) -> int | None:
    """Approximate turns until lethal at current board state.

    Returns None if no damage gets through (board stall) or attackers can't
    keep dealing damage. Uses ``best_damage_through`` repeated each turn.
    """
    if defender_life <= 0:
        return 0
    per_turn = best_damage_through(attackers_details, blockers_details)
    if per_turn <= 0:
        return None
    # Simple constant-rate division; we don't simulate trades / removal.
    return max(1, (defender_life + per_turn - 1) // per_turn)


def _card_advantage(hand: list[str], board: list[str], graveyard: list[str]) -> float:
    """A coarse card-advantage proxy: live cards minus expended ones."""
    return float(len(hand or []) + len(board or [])) - 0.25 * float(len(graveyard or []))


def _clamp(v: float, lo: float = -1.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def _power_sum(creatures: list[dict]) -> int:
    return sum(int(c.get("power") or 0) for c in creatures)


def _hand_value_sum(hand_values: list[dict] | None) -> float:
    if not hand_values:
        return 0.0
    return sum(float(hv.get("value") or 0.0) for hv in hand_values) / 100.0


def assess_role(
    state: dict,
    ai_strategy: str | None,
    opp_strategy: str | None,
    *,
    opp_record: dict | None = None,
    archetype_name: str | None = None,
    hand_values: list[dict] | None = None,
) -> dict:
    """Return a serialized ``RoleAssessment`` (dict) for the current state.

    Implements the role-flip rule (Mike Flores's "Who's the Beatdown?") plus
    v5 dimension scores: per-axis scores for board / cards / clock / tempo /
    graveyard, each in [-1, +1] from the AI's perspective. The role-flip
    decision uses the dimension scores instead of the original ad-hoc deltas.
    """
    ai_hand = state.get("hand", []) or []
    ai_board = state.get("own_board", []) or []
    ai_gy = state.get("your_graveyard", []) or []
    opp_board = state.get("opponent_board", []) or []
    opp_gy = state.get("opponent_graveyard", []) or []
    ai_life, human_life = _ai_human_life(state.get("life_totals", {}))

    ai_natural = _natural_role(ai_strategy)
    opp_natural = _natural_role(opp_strategy)

    # Detailed creatures with P/T when available.
    ai_creatures = _creatures_from_details(state.get("own_board_details") or [])
    opp_creatures = _creatures_from_details(state.get("opponent_board_details") or [])

    # Counts always available — these are the fallback when P/T missing.
    ai_attackers = count_possible_attackers(ai_board) if not ai_creatures else len(ai_creatures)
    opp_attackers = count_possible_attackers(opp_board) if not opp_creatures else len(opp_creatures)
    ai_pressure = _board_pressure(ai_board)
    opp_pressure = _board_pressure(opp_board)
    opp_hand_size = estimate_opponent_hand_size(state)
    ai_hand_size = (
        state.get("ai_hand_size")
        if isinstance(state.get("ai_hand_size"), int) and state.get("ai_hand_size") > 0
        else len(ai_hand)
    )

    # ---- Dimension: Board -------------------------------------------------
    # When we have P/T, use power-sum delta (better signal). Otherwise count.
    if ai_creatures or opp_creatures:
        ai_power = _power_sum(ai_creatures)
        opp_power = _power_sum(opp_creatures)
        board_score = _clamp((ai_power - opp_power) / 6.0)
    else:
        board_score = _clamp((ai_pressure - opp_pressure) / 4.0)

    # ---- Dimension: Cards (hand size + hand value + graveyard discount) --
    # Compare raw hand counts plus per-card value if we already computed it.
    hand_count_delta = ai_hand_size - opp_hand_size
    hand_value_signal = _hand_value_sum(hand_values) - 0.5  # ~0 if average values
    cards_score = _clamp(
        0.6 * (hand_count_delta / 4.0)
        + 0.4 * hand_value_signal
        - 0.05 * (len(ai_gy) - len(opp_gy)) / 4.0
    )

    # ---- Dimension: Clock (with optimal blocks) --------------------------
    # AI's clock vs opponent: positive = AI's clock is faster.
    ai_clock = turns_to_lethal(ai_creatures, opp_creatures, human_life) if ai_creatures else None
    opp_clock = turns_to_lethal(opp_creatures, ai_creatures, ai_life) if opp_creatures else None
    if ai_clock is None and opp_clock is None:
        clock_score = 0.0
    elif ai_clock is None:
        clock_score = -1.0
    elif opp_clock is None:
        clock_score = 1.0
    else:
        # Faster clock wins. Map turn-difference [-5,5] -> [-1,1].
        clock_score = _clamp((opp_clock - ai_clock) / 5.0)

    # ---- Dimension: Tempo (land drops + color screw) ---------------------
    opp_drops = opponent_lands_dropped(state)
    opp_missed = opponent_missed_drops(state)
    opp_screwed = opponent_color_screwed(state, opp_record)
    # AI is winning tempo if opp missed drops or is color-screwed.
    tempo_score = _clamp(0.3 * opp_missed - (0.4 if opp_screwed else 0.0))

    # ---- Dimension: Graveyard utility ------------------------------------
    gy_util, gy_signals = graveyard_utility(opp_gy, archetype_name, opp_record)
    # Graveyard score is *negative* for the AI when the opp's graveyard is
    # useful to them, mild positive otherwise.
    graveyard_score = _clamp(0.2 - 1.0 * gy_util)

    # Life is folded into the clock dimension (clock = life / damage rate).
    life_delta = ai_life - human_life
    life_signal = _clamp(life_delta / 10.0)

    # ---- Role decision (uses dimension scores) ---------------------------
    # Flip rule: aggro side that is behind on BOTH board and cards becomes
    # control. Control side that is ahead on board AND cards AND has a finite
    # clock takes over beatdown.
    ai_role = ai_natural
    role_flipped = False
    if ai_natural == "beatdown" and board_score < 0 and cards_score < 0:
        ai_role = "control"
        role_flipped = True
    elif ai_natural == "control" and board_score > 0.25 and cards_score >= 0 and human_life <= 12:
        ai_role = "beatdown"
        role_flipped = True
    elif ai_natural == "contested":
        if board_score >= 0.25 or cards_score >= 0.25:
            ai_role = "beatdown"
        elif board_score <= -0.25 or cards_score <= -0.25:
            ai_role = "control"

    opp_role = "control" if ai_role == "beatdown" else (
        "beatdown" if ai_role == "control" else opp_natural
    )

    # ---- Winning side: weighted combination ------------------------------
    score = (
        0.30 * board_score
        + 0.25 * cards_score
        + 0.25 * clock_score
        + 0.10 * tempo_score
        + 0.10 * life_signal
    )
    if score > 0.15:
        winning_side, margin = "ai", min(1.0, abs(score))
    elif score < -0.15:
        winning_side, margin = "human", min(1.0, abs(score))
    else:
        winning_side, margin = "even", abs(score)

    # Reasoning summary
    parts = [
        f"AI {ai_role} (natural {ai_natural}{', flipped' if role_flipped else ''})",
        f"life {ai_life}-{human_life}",
        f"board {int(ai_pressure)}-{int(opp_pressure)}",
        f"hand {ai_hand_size} vs {opp_hand_size}",
    ]
    if ai_clock is not None:
        parts.append(f"AI clock {ai_clock}t")
    if opp_clock is not None:
        parts.append(f"opp clock {opp_clock}t")
    if opp_missed > 0:
        parts.append(f"opp missed {opp_missed} land(s)")
    if opp_screwed:
        parts.append("opp color-screwed")
    if gy_signals:
        parts.append("gy: " + "; ".join(gy_signals[:1]))

    return {
        "ai_role": ai_role,
        "opponent_role": opp_role,
        "winning_side": winning_side,
        "margin": round(margin, 3),
        "role_flipped": role_flipped,
        "opp_hand_size": opp_hand_size,
        "ai_attackers": ai_attackers,
        "opp_attackers": opp_attackers,
        "board_score": round(board_score, 3),
        "cards_score": round(cards_score, 3),
        "clock_score": round(clock_score, 3),
        "tempo_score": round(tempo_score, 3),
        "graveyard_score": round(graveyard_score, 3),
        "opp_lands_dropped": opp_drops,
        "opp_missed_drops": opp_missed,
        "opp_color_screwed": opp_screwed,
        "opp_graveyard_utility": round(gy_util, 3),
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


def _opp_hand_categories(opp_hand: list[dict] | None) -> dict[str, float]:
    """Map of category -> probability the opponent holds at least one."""
    if not opp_hand:
        return {}
    out: dict[str, float] = {}
    for entry in opp_hand:
        cat = (entry.get("category") or "").lower()
        prob = float(entry.get("probability") or 0.0)
        if cat:
            out[cat] = max(out.get(cat, 0.0), prob)
    return out


def score_hand(
    hand: list[str],
    guide: dict,
    phase_bucket: str,
    role: dict,
    *,
    opp_strategy: str | None = None,
    opp_board: list[str] | None = None,
    own_board: list[str] | None = None,
    opp_hand_inference: list[dict] | None = None,
) -> list[dict]:
    """Per-card value scores for cards currently in the AI's hand.

    Values are 0-100. The piloting guide's ``key_cards`` provide role tags;
    cards not in the guide get a "filler" tag and a modest baseline value.
    Role-flipped scoring inverts the threat/answer preference. Matchup
    context is applied on top so the same card is valued differently
    depending on what the opponent is doing.
    """
    if not hand:
        return []
    ai_role = (role or {}).get("ai_role", "contested")
    is_beatdown = ai_role == "beatdown"
    opp_strat = (opp_strategy or "").strip().lower()
    opp_board = opp_board or []
    own_board = own_board or []
    opp_creature_count = count_possible_attackers(opp_board)
    own_creature_count = count_possible_attackers(own_board)

    # Matchup signals that modulate categories.
    opp_is_aggro = opp_strat in {"aggro", "burn", "tempo"}
    opp_is_control = opp_strat in {"control"}
    opp_is_combo = opp_strat in {"combo"}
    opp_has_wide_board = opp_creature_count >= 3
    opp_has_one_big_threat = opp_creature_count == 1

    # What we think the opponent is holding — drives threat-aware penalties /
    # boosts. ``p_counter`` etc. are 0..1 probabilities.
    cats = _opp_hand_categories(opp_hand_inference)
    p_counter = cats.get("counterspell", 0.0)
    p_removal = cats.get("removal", 0.0)
    p_wrath = cats.get("wrath", 0.0)
    p_threat = cats.get("threat", 0.0)

    results: list[dict] = []
    for card in hand:
        if not card:
            continue
        guide_role = _role_for_card_in_guide(card, guide)
        category = categorize(card)  # counterspell/removal/wrath/card_draw/...
        if _is_land_name(card):
            tag = "land"
            base = 55.0
            reason = "land — develops mana"
        elif guide_role:
            tag = guide_role
            base = 80.0
            reason = f"key card in the piloting guide ({guide_role})"
        else:
            tag = category if category != "filler" else "filler"
            base = 50.0 if category != "filler" else 35.0
            reason = (
                f"recognized as {category}"
                if category != "filler"
                else "not a key card in the guide"
            )

        # Role modulation (Who's the Beatdown).
        if tag in ("threat", "win_con") and is_beatdown:
            base += 12.0
            reason += "; boosted by beatdown role"
        elif tag in ("answer", "card_draw", "removal", "counterspell", "wrath") and not is_beatdown:
            base += 12.0
            reason += "; boosted by control role"
        elif tag in ("threat", "win_con") and not is_beatdown:
            base -= 5.0
        elif tag in ("answer", "card_draw") and is_beatdown:
            base -= 5.0

        # Matchup modulation.
        if category == "wrath":
            if opp_has_wide_board:
                base += 25.0
                reason += f"; opp has {opp_creature_count} creatures — wrath shines"
            elif opp_creature_count == 0:
                base -= 20.0
                reason += "; opp has no board — wrath is dead"
            elif opp_is_combo or opp_is_control:
                base -= 10.0
                reason += "; opp deck has few creatures"
        elif category == "removal":
            if opp_has_one_big_threat:
                base += 10.0
                reason += "; one priority threat on opp board"
            elif opp_creature_count == 0:
                base -= 15.0
                reason += "; nothing relevant to point removal at"
            elif opp_is_control or opp_is_combo:
                base -= 8.0
                reason += "; opp deck plays few creatures"
        elif category == "counterspell":
            if opp_is_combo:
                base += 18.0
                reason += "; counters are gold vs combo"
            elif opp_is_control:
                base += 8.0
                reason += "; counter wars vs control"
            elif opp_is_aggro and phase_bucket == "early_game":
                base -= 10.0
                reason += "; aggro empties hand fast — counters need targets"
        elif category == "card_draw":
            if opp_is_aggro and own_creature_count == 0 and phase_bucket == "early_game":
                base -= 15.0
                reason += "; can't tap out for draw against an early aggro clock"
            elif phase_bucket == "late_game":
                base += 8.0
                reason += "; card draw seals attrition matches"
        elif category == "combo_piece":
            base += 10.0
            reason += "; combo piece — protect and assemble"

        # Threat-aware modulation: discount based on what opp likely holds.
        if tag in ("threat", "win_con"):
            if p_counter >= 0.4:
                base -= 8.0 * p_counter
                reason += f"; opp likely holds counterspells ({int(p_counter*100)}%)"
            if p_wrath >= 0.4 and own_creature_count >= 2:
                base -= 6.0 * p_wrath
                reason += f"; sweeper risk ({int(p_wrath*100)}%) with {own_creature_count} on board"
            if p_removal >= 0.5 and category != "filler":
                base -= 4.0 * p_removal
                reason += f"; spot removal likely ({int(p_removal*100)}%)"
        elif category in ("removal", "counterspell") and p_threat >= 0.5:
            base += 5.0 * p_threat
            reason += f"; opp likely has threats to answer ({int(p_threat*100)}%)"

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
    # Gate them on actual board state — attacking with no creatures is
    # impossible and shouldn't be the highest-scored action just because the
    # deck is naturally beatdown. Same for BLOCK without incoming attackers.
    ai_attackers = (role or {}).get("ai_attackers", 0)
    opp_attackers = (role or {}).get("opp_attackers", 0)
    # Dimension scores (-1..+1) influence weighting.
    clock_score = float((role or {}).get("clock_score", 0.0) or 0.0)
    tempo_score = float((role or {}).get("tempo_score", 0.0) or 0.0)
    cards_score = float((role or {}).get("cards_score", 0.0) or 0.0)
    # Bonuses: faster clock & tempo-positive scenes push ATTACK; card
    # disadvantage and bad clock push toward PASS/BLOCK.
    attack_bias = 10.0 * clock_score + 6.0 * tempo_score
    pass_bias = -8.0 * clock_score - 4.0 * cards_score

    # The hard-board gate always fires (zero attackers = ATTACK must be 0).
    # The role-derived combat *bias* only fires when role is decisive; in
    # contested matchups we leave combat to the personality-driven base so
    # personality preferences come through.
    if ai_attackers <= 0:
        actions.append(
            {
                "action_type": "ATTACK",
                "target": "",
                "targets": None,
                "percentage": 0.0,
                "reasoning": "No creatures on the AI's battlefield — cannot attack.",
            }
        )
    elif is_beatdown or ai_role == "control":
        base = 75.0 if is_beatdown else 25.0
        adj = max(0.0, min(95.0, base + attack_bias))
        actions.append(
            {
                "action_type": "ATTACK",
                "target": "all_available",
                "targets": None,
                "percentage": round(adj, 1),
                "reasoning": (
                    f"Beatdown role — race with {ai_attackers} attacker(s)."
                    if is_beatdown
                    else f"Control role — only swing if {ai_attackers} attacker(s) chips safely."
                ) + (f" Clock bias {attack_bias:+.0f}." if abs(attack_bias) >= 3 else ""),
            }
        )
    # else: role is contested with attackers available — leave ATTACK to base.

    if opp_attackers <= 0:
        actions.append(
            {
                "action_type": "BLOCK",
                "target": "",
                "targets": None,
                "percentage": 0.0,
                "reasoning": "Opponent has no attackers — nothing to block.",
            }
        )
    elif is_beatdown or ai_role == "control":
        actions.append(
            {
                "action_type": "BLOCK",
                "target": "",
                "targets": None,
                "percentage": 15.0 if is_beatdown else 65.0,
                "reasoning": (
                    "Beatdown role — take damage to keep pressing."
                    if is_beatdown
                    else "Control role — protect life total."
                ),
            }
        )

    if is_beatdown or ai_role == "control":
        base = 10.0 if is_beatdown else 45.0
        adj = max(0.0, min(95.0, base + pass_bias))
        actions.append(
            {
                "action_type": "PASS",
                "target": "",
                "targets": None,
                "percentage": round(adj, 1),
                "reasoning": (
                    "Beatdown role — pass only when no play exists."
                    if is_beatdown
                    else "Control role — hold up mana for interaction."
                ) + (f" Pressure bias {pass_bias:+.0f}." if abs(pass_bias) >= 3 else ""),
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
    *,
    opp_hand_size: int | None = None,
) -> list[dict]:
    """Bucketed guesses about what the human still has in hand.

    Probability is a product of:
    - archetype confidence (how sure we are about their deck),
    - share of unseen signatures still possibly in deck,
    - hand-size factor (more cards in hand = more likely to be holding one),
    - category share (more cards in a category = more likely category).
    """
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

    # Probability heuristic blends multiple signals. Hand-size factor is
    # the new piece: with 7 cards in hand it's almost certain they have at
    # least one signature card; with 0 cards there's nothing to infer.
    total = max(1, len(signatures))
    unseen_share = len(unseen) / total
    turn_factor = max(0.4, 1.0 - 0.05 * max(0, turn))
    if opp_hand_size is None:
        hand_size_factor = max(0.3, 1.0 - 0.07 * max(0, turn))  # rough fallback
        hand_descr = f"~ unknown cards on turn {turn}"
    else:
        # 0 cards -> 0; 1-2 cards -> small; 4 cards -> 0.7; 7 cards -> 1.0.
        hand_size_factor = max(0.0, min(1.0, opp_hand_size / 7.0))
        hand_descr = f"{opp_hand_size} cards in hand"

    out: list[dict] = []
    for cat, cards in by_cat.items():
        cat_share = len(cards) / total
        prob = (
            confidence
            * (0.5 * unseen_share + 0.5 * cat_share)
            * turn_factor
            * (0.4 + 0.6 * hand_size_factor)
        )
        # Floor only when the opponent actually has cards in hand.
        floor = 0.05 if (opp_hand_size or 1) > 0 else 0.0
        prob = max(0.0, min(1.0, prob + floor))
        out.append(
            {
                "category": cat,
                "example_cards": cards[:4],
                "probability": round(prob, 3),
                "reasoning": (
                    f"{len(cards)} of {total} signature cards unseen, "
                    f"{hand_descr} (turn {turn})."
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
