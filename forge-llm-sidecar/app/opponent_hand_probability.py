"""Card-by-card probability helpers from known deck contents."""

from __future__ import annotations

from collections import Counter
from math import comb


def _norm(card: str) -> str:
    return " ".join((card or "").strip().lower().split())


def _display_names(cards: list[str]) -> dict[str, str]:
    names: dict[str, str] = {}
    for card in cards:
        key = _norm(card)
        if key and key not in names:
            names[key] = card
    return names


def _at_least_one_probability(population: int, successes: int, draws: int) -> float:
    if population <= 0 or successes <= 0 or draws <= 0:
        return 0.0
    draws = min(draws, population)
    successes = min(successes, population)
    if draws > population - successes:
        return 1.0
    return 1.0 - (comb(population - successes, draws) / comb(population, draws))


def opponent_card_probabilities(state: dict, *, limit: int = 20) -> list[dict]:
    """Estimate cards in the opponent's hand from decklist minus known cards.

    The population is the opponent's main deck after subtracting firm knowledge:
    battlefield, graveyard, exile, and cards seen from the opponent's hand.
    Observed spells/lands are also subtracted because once observed they are no
    longer in the library, even if they later moved to a hidden zone.
    """
    deck = [str(c) for c in (state.get("opponent_deck_cards") or []) if str(c).strip()]
    if not deck:
        return []

    deck_counts = Counter(_norm(c) for c in deck)
    names = _display_names(deck)

    public_cards: list[str] = []
    for key in ("opponent_board", "opponent_graveyard", "opponent_exile", "opponent_seen_hand"):
        if key != "opponent_seen_hand":
            public_cards.extend(str(c) for c in (state.get(key) or []) if str(c).strip())
    seen_hand_counts = Counter(
        _norm(str(c)) for c in (state.get("opponent_seen_hand") or []) if str(c).strip()
    )
    observed_cards: list[str] = []
    for obs in state.get("observations") or []:
        if isinstance(obs, dict):
            card = str(obs.get("card") or "").strip()
            if card:
                observed_cards.append(card)

    public_counts = Counter(_norm(c) for c in public_cards)
    observed_counts = Counter(_norm(c) for c in observed_cards)
    known_counts = Counter()
    for card in set(public_counts) | set(observed_counts) | set(seen_hand_counts):
        # Public zones are additive because those cards are simultaneously
        # visible. Observations/seen-hand can refer to a card now visible in a
        # public zone, so use max rather than summing and double-counting.
        known_counts[card] = max(
            public_counts.get(card, 0),
            observed_counts.get(card, 0),
            seen_hand_counts.get(card, 0),
        )
    remaining = {
        card: max(0, count - known_counts.get(card, 0))
        for card, count in deck_counts.items()
    }

    population = sum(remaining.values())
    hand_size = int(state.get("opp_hand_size") or 0)
    if hand_size <= 0:
        return []
    if state.get("opp_library_size"):
        # Prefer the real public hidden-zone total when available. This keeps
        # probabilities stable when the decklist includes sideboard drift or
        # when tokens/temporary cards confuse known-zone subtraction.
        population = max(population, hand_size + int(state.get("opp_library_size") or 0))

    out: list[dict] = []
    for card, copies in remaining.items():
        if copies <= 0:
            continue
        probability = _at_least_one_probability(population, copies, hand_size)
        if probability <= 0:
            continue
        removed = min(deck_counts[card], known_counts.get(card, 0))
        out.append(
            {
                "card": names.get(card, card),
                "remaining_copies": copies,
                "known_removed": removed,
                "probability": round(probability, 4),
                "reasoning": (
                    f"{copies} possible copy/copies among {population} unknown "
                    f"deck+hand cards; opponent has {hand_size} card(s) in hand."
                ),
            }
        )

    out.sort(key=lambda row: (-row["probability"], row["card"]))
    return out[:limit]


def ai_draw_probabilities(state: dict, *, limit: int = 20) -> list[dict]:
    """Estimate next-draw odds for the AI's own deck.

    Forge sends the AI's own main deck as ``deck_cards``. We subtract cards
    known outside the library: hand, battlefield, graveyard, exile, and observed
    own-zone state. The returned probability is the chance the next draw is that
    card name.
    """
    deck = [str(c) for c in (state.get("deck_cards") or []) if str(c).strip()]
    if not deck:
        return []

    deck_counts = Counter(_norm(c) for c in deck)
    names = _display_names(deck)
    known_cards: list[str] = []
    for key in ("hand", "own_board", "your_graveyard", "your_exile"):
        known_cards.extend(str(c) for c in (state.get(key) or []) if str(c).strip())
    known_counts = Counter(_norm(c) for c in known_cards)
    remaining = {
        card: max(0, count - known_counts.get(card, 0))
        for card, count in deck_counts.items()
    }

    population = sum(remaining.values())
    if state.get("ai_library_size"):
        population = max(1, int(state.get("ai_library_size") or population))
    if population <= 0:
        return []

    out: list[dict] = []
    for card, copies in remaining.items():
        if copies <= 0:
            continue
        probability = copies / population
        removed = min(deck_counts[card], known_counts.get(card, 0))
        out.append(
            {
                "card": names.get(card, card),
                "remaining_copies": copies,
                "known_removed": removed,
                "probability": round(probability, 4),
                "reasoning": (
                    f"{copies} possible copy/copies among {population} modeled "
                    "library cards for the next draw."
                ),
            }
        )

    out.sort(key=lambda row: (-row["probability"], row["card"]))
    return out[:limit]
