"""Tests for assess_role's strict board-score role rule.

The role (beatdown vs control) now tracks board_score directly rather than the
deck's natural Flores role: whoever is ahead on board is the beatdown.
"""

from __future__ import annotations

from app import advice


def _state(ai_creatures, opp_creatures, **overrides) -> dict:
    def details(cs):
        return [
            {"name": n, "power": p, "toughness": t, "is_creature": True,
             "types": ["Creature"]}
            for (n, p, t) in cs
        ]

    state = {
        "own_board": [c[0] for c in ai_creatures],
        "opponent_board": [c[0] for c in opp_creatures],
        "own_board_details": details(ai_creatures),
        "opponent_board_details": details(opp_creatures),
        "hand": [],
        "your_graveyard": [],
        "opponent_graveyard": [],
        "life_totals": {"ai": 20, "human": 20},
    }
    state.update(overrides)
    return state


def test_ai_ahead_on_board_is_beatdown_even_for_control_deck():
    # AI control deck crushing the board while human is at full life: the old
    # Flores rule kept this "control" (needed human_life <= 12 to flip). The
    # strict rule makes the side ahead on board the beatdown.
    state = _state([("Big", 6, 6)], [("Small", 1, 1)], life_totals={"ai": 20, "human": 20})
    role = advice.assess_role(state, "control", "aggro")
    assert role["board_score"] > 0
    assert role["ai_role"] == "beatdown"
    assert role["opponent_role"] == "control"
    assert role["role_flipped"] is True  # natural control overridden


def test_ai_behind_on_board_is_control_even_for_aggro_deck():
    state = _state([("Small", 1, 1)], [("Big", 6, 6)])
    role = advice.assess_role(state, "aggro", "control")
    assert role["board_score"] < 0
    assert role["ai_role"] == "control"
    assert role["opponent_role"] == "beatdown"
    assert role["role_flipped"] is True  # natural beatdown overridden


def test_even_board_defaults_to_control():
    # board_score not > 0 -> AI is not winning the board -> control.
    state = _state([("A", 2, 2)], [("B", 2, 2)])
    role = advice.assess_role(state, "aggro", "aggro")
    assert role["board_score"] == 0
    assert role["ai_role"] == "control"
    assert role["opponent_role"] == "beatdown"


def test_role_not_flipped_when_board_matches_natural_role():
    state = _state([("Big", 6, 6)], [("Small", 1, 1)])
    role = advice.assess_role(state, "aggro", "control")
    assert role["ai_role"] == "beatdown"
    assert role["role_flipped"] is False  # natural beatdown, board agrees
