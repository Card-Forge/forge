"""Deterministic off-meta deck-style classifier.

Computes a score in [0, 1] for each broad strategy (aggro / control / combo /
midrange / tempo) from a list of observed opponent plays. Used by the
recognition prompt as additional structured evidence so the LLM can fall back
to an "Off-meta <Strategy>" label when no curated archetype is a good fit.

The heuristics are intentionally simple and explainable; the LLM still makes
the final call, but it sees these scores plus the rules they were derived
from. Extend the card-class sets below as new format-defining cards appear.
"""

from __future__ import annotations

from collections import defaultdict

# --- Known card classes -----------------------------------------------------
# These lists are not exhaustive — they cover the most format-defining cards
# in modern Constructed formats. Add more as needed; the classifier degrades
# gracefully when it doesn't recognize a card name.

BOARD_WIPES: frozenset[str] = frozenset(
    {
        "Wrath of God",
        "Damnation",
        "Day of Judgment",
        "Supreme Verdict",
        "Sweltering Suns",
        "Anger of the Gods",
        "Pyroclasm",
        "Earthquake",
        "Toxic Deluge",
        "Settle the Wreckage",
        "Cyclonic Rift",
        "Crux of Fate",
        "Languish",
        "Hour of Devastation",
        "Plague Wind",
        "Decree of Pain",
        "Doomskar",
        "Farewell",
        "March of Otherworldly Light",
        "Brotherhood's End",
        "Temporary Lockdown",
        "Sunfall",
        "Fumigate",
        "Kaya's Wrath",
        "Shatter the Sky",
        "Time Wipe",
        "End the Festivities",
        "Slagstorm",
    }
)

COUNTERSPELLS: frozenset[str] = frozenset(
    {
        "Counterspell",
        "Force of Negation",
        "Force of Will",
        "Spell Pierce",
        "Mana Leak",
        "Negate",
        "Dispel",
        "Daze",
        "Spell Snare",
        "Cryptic Command",
        "Stubborn Denial",
        "Make Disappear",
        "Mana Tithe",
        "Remand",
        "Logic Knot",
        "Archmage's Charm",
        "Drown in the Loch",
        "Memory Lapse",
    }
)

CHEAP_CARD_DRAW: frozenset[str] = frozenset(
    {
        "Brainstorm",
        "Ponder",
        "Preordain",
        "Opt",
        "Consider",
        "Faithless Looting",
        "Thought Scour",
        "Serum Visions",
        "Sleight of Hand",
        "Mishra's Bauble",
        "Otherworldly Gaze",
        "Expressive Iteration",
        "Picklock Prankster",
    }
)

BIG_CARD_DRAW: frozenset[str] = frozenset(
    {
        "Sphinx's Revelation",
        "Treasure Cruise",
        "Dig Through Time",
        "Chart a Course",
        "Glimpse the Cosmos",
        "Memory Deluge",
        "Notorious Throng",
        "Dragon's Rage Channeler",  # filters/draws
        "Wheel of Fortune",
        "Time Spiral",
        "Ancestral Recall",
    }
)

COMBO_PIECES: frozenset[str] = frozenset(
    {
        # Modern combos
        "Goryo's Vengeance",
        "Through the Breach",
        "Scapeshift",
        "Splinter Twin",
        "Kiki-Jiki, Mirror Breaker",
        "Devoted Druid",
        "Saheeli Rai",
        "Felidar Guardian",
        # Storm
        "Grapeshot",
        "Past in Flames",
        "Manamorphose",
        "Pyretic Ritual",
        "Desperate Ritual",
        "Ruby Medallion",
        # Belcher
        "Goblin Charbelcher",
        # Amulet Titan
        "Amulet of Vigor",
        "Primeval Titan",
        # Neobrand / Eldritch Evolution
        "Eldritch Evolution",
        "Neoform",
        # Living End / cascade
        "Living End",
        "Violent Outburst",
        "Shardless Agent",
        # Tron
        "Karn Liberated",
        "Wurmcoil Engine",
        # Hardened Scales / Affinity-ish combo cards
        "Hardened Scales",
        "Arcbound Ravager",
    }
)

# Removal spells — often used by midrange/control, not aggro
TARGETED_REMOVAL: frozenset[str] = frozenset(
    {
        "Lightning Bolt",
        "Path to Exile",
        "Swords to Plowshares",
        "Fatal Push",
        "Doom Blade",
        "Go for the Throat",
        "Prismatic Ending",
        "Solitude",
        "Fury",
        "Endurance",
        "Unholy Heat",
        "Drown in the Loch",
    }
)

# Planeswalker names are handled via the types field — no list needed.

OFF_META_STRATEGIES = ("aggro", "control", "combo", "midrange", "tempo")

OFF_META_NAMES: tuple[str, ...] = tuple("Off-meta " + s.capitalize() for s in OFF_META_STRATEGIES)


def classify_style(observations: list[dict]) -> dict[str, float]:
    """Return a 0-1 score per strategy. Empty input → all zeros."""
    scores = {s: 0.0 for s in OFF_META_STRATEGIES}
    if not observations:
        return scores

    creatures_by_turn: dict[int, int] = defaultdict(int)
    spells_by_turn: dict[int, int] = defaultdict(int)
    cheap_creatures = 0
    high_cmc_spells = 0
    planeswalkers = 0
    counters_cast = 0
    wipes_cast = 0
    cheap_draw_cast = 0
    big_draw_cast = 0
    combo_pieces = 0
    removal_cast = 0
    colors: set[str] = set()
    max_turn = 0
    first_spell_turn: int | None = None

    for o in observations:
        turn = int(o.get("turn", 0) or 0)
        max_turn = max(max_turn, turn)
        name = str(o.get("card", "") or "")
        cmc = int(o.get("cmc", 0) or 0)
        event = str(o.get("event", "") or "")
        types = o.get("types", []) or []

        for c in o.get("colors", []) or []:
            colors.add(c)

        if event != "spell":
            continue

        spells_by_turn[turn] += 1
        if first_spell_turn is None or turn < first_spell_turn:
            first_spell_turn = turn
        if "Creature" in types:
            creatures_by_turn[turn] += 1
            if cmc <= 2:
                cheap_creatures += 1
        if "Planeswalker" in types:
            planeswalkers += 1
        if cmc >= 4:
            high_cmc_spells += 1
        if name in COUNTERSPELLS:
            counters_cast += 1
        if name in BOARD_WIPES:
            wipes_cast += 1
        if name in CHEAP_CARD_DRAW:
            cheap_draw_cast += 1
        if name in BIG_CARD_DRAW:
            big_draw_cast += 1
        if name in COMBO_PIECES:
            combo_pieces += 1
        if name in TARGETED_REMOVAL:
            removal_cast += 1

    # --- Aggro -----------------------------------------------------------
    # Cheap creatures on turns 1-2 are the strongest signal.
    early_creatures = creatures_by_turn.get(1, 0) + creatures_by_turn.get(2, 0)
    scores["aggro"] += min(0.45, early_creatures * 0.25)
    scores["aggro"] += min(0.25, max(0, cheap_creatures - early_creatures) * 0.1)
    if "R" in colors and max_turn <= 4:
        scores["aggro"] += 0.1
    # Aggro decks rarely run big spells; penalize if we see a lot.
    if high_cmc_spells >= 2:
        scores["aggro"] = max(0.0, scores["aggro"] - 0.2)

    # --- Control ---------------------------------------------------------
    scores["control"] += min(0.4, counters_cast * 0.2)
    scores["control"] += min(0.5, wipes_cast * 0.3)
    scores["control"] += min(0.3, big_draw_cast * 0.2)
    if first_spell_turn is not None and first_spell_turn >= 3:
        scores["control"] += 0.15
    if "U" in colors:
        scores["control"] += 0.1
    # Passes-with-mana-up proxy: turns where the opponent cast no spells.
    if max_turn >= 3:
        passes = sum(1 for t in range(1, max_turn + 1) if spells_by_turn.get(t, 0) == 0)
        # First-turn pass alone isn't strong evidence, so require 2+.
        scores["control"] += min(0.2, max(0, passes - 1) * 0.1)

    # --- Combo -----------------------------------------------------------
    scores["combo"] += min(0.7, combo_pieces * 0.35)
    # Heavy cheap-draw selection is a combo/storm tell.
    scores["combo"] += min(0.25, cheap_draw_cast * 0.1)
    # No creatures + lots of spells = combo-ish.
    total_creatures = sum(creatures_by_turn.values())
    total_spells = sum(spells_by_turn.values())
    if total_spells >= 4 and total_creatures == 0:
        scores["combo"] += 0.15

    # --- Midrange -------------------------------------------------------
    scores["midrange"] += min(0.4, planeswalkers * 0.3)
    scores["midrange"] += min(0.3, high_cmc_spells * 0.12)
    scores["midrange"] += min(0.25, removal_cast * 0.1)
    if "B" in colors and "G" in colors:
        scores["midrange"] += 0.2
    # Mid-CMC creatures (3-4 CMC) without early aggression
    mid_cmc_creatures = total_creatures - cheap_creatures
    if mid_cmc_creatures >= 1 and early_creatures == 0:
        scores["midrange"] += 0.1

    # --- Tempo ----------------------------------------------------------
    # Cheap creatures + counterspells/bounce is the Delver pattern.
    if cheap_creatures >= 1 and counters_cast >= 1:
        scores["tempo"] += 0.5
    if "U" in colors and ("R" in colors or "W" in colors) and cheap_creatures >= 1:
        scores["tempo"] += 0.15
    if cheap_creatures >= 1 and removal_cast >= 1 and wipes_cast == 0:
        scores["tempo"] += 0.1

    # Clamp to [0, 1].
    return {k: max(0.0, min(1.0, v)) for k, v in scores.items()}


def top_off_meta_label(scores: dict[str, float]) -> str | None:
    """Return the most-likely 'Off-meta <Strategy>' label, or None if no
    strategy scores above a minimum threshold."""
    if not scores:
        return None
    best = max(scores.items(), key=lambda kv: kv[1])
    if best[1] < 0.35:
        return None
    return "Off-meta " + best[0].capitalize()
