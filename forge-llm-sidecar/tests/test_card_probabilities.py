"""Tests for card-by-card deck probability helpers."""

from app.opponent_hand_probability import ai_draw_probabilities, opponent_card_probabilities


def test_opponent_card_probabilities_use_remaining_deck_and_hand_size():
    state = {
        "opponent_deck_cards": ["Bolt", "Bolt", "Bolt", "Island", "Island"],
        "opponent_board": ["Island"],
        "opponent_graveyard": ["Bolt"],
        "opponent_exile": [],
        "opponent_seen_hand": [],
        "observations": [],
        "opp_hand_size": 2,
        "opp_library_size": 1,
    }
    rows = opponent_card_probabilities(state)
    bolt = next(r for r in rows if r["card"] == "Bolt")
    assert bolt["remaining_copies"] == 2
    assert bolt["known_removed"] == 1
    assert bolt["probability"] == 1.0


def test_opponent_probabilities_do_not_double_count_observed_board_card():
    state = {
        "opponent_deck_cards": ["Ruby Medallion", "Ruby Medallion", "Mountain"],
        "opponent_board": ["Ruby Medallion"],
        "opponent_graveyard": [],
        "opponent_exile": [],
        "opponent_seen_hand": [],
        "observations": [{"event": "spell", "card": "Ruby Medallion"}],
        "opp_hand_size": 1,
        "opp_library_size": 1,
    }
    ruby = next(r for r in opponent_card_probabilities(state) if r["card"] == "Ruby Medallion")
    assert ruby["remaining_copies"] == 1


def test_ai_draw_probabilities_are_next_draw_odds():
    state = {
        "deck_cards": ["Opt", "Opt", "Island", "Island"],
        "hand": ["Opt"],
        "own_board": ["Island"],
        "your_graveyard": [],
        "your_exile": [],
        "ai_library_size": 2,
    }
    rows = ai_draw_probabilities(state)
    assert {r["card"]: r["probability"] for r in rows} == {"Island": 0.5, "Opt": 0.5}
