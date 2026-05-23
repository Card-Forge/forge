"""Tests for the per-deck mana profile and the strategist's mana planner.

Fully offline: the LLM is stubbed and the background profile build is disabled.
"""

from __future__ import annotations

import pytest

from app.knowledge import mana_profile
from app.nodes import opponent_strategist as strat


def test_deck_hash_is_order_independent_and_case_folded():
    a = mana_profile.deck_hash(["Scalding Tarn", "Island", "Mountain"])
    b = mana_profile.deck_hash(["mountain", "scalding tarn", "ISLAND"])
    assert a == b
    # A different list hashes differently (post-sideboard configs get fresh profiles).
    assert a != mana_profile.deck_hash(["Scalding Tarn", "Island", "Island"])


def test_has_analyzable_manabase():
    assert not mana_profile.has_analyzable_manabase(["Island", "Mountain", "Lightning Bolt"])
    assert mana_profile.has_analyzable_manabase(["Scalding Tarn", "Island"])
    assert mana_profile.has_analyzable_manabase(["Otawara, Soaring City", "Island"])


def test_remaining_library_subtracts_visible_zones():
    state = {
        "deck_cards": ["Island", "Island", "Scalding Tarn", "Scalding Tarn", "Lightning Bolt"],
        "hand": ["Scalding Tarn", "Lightning Bolt"],
        "own_board": ["Island"],
        "your_graveyard": [],
        "your_exile": [],
    }
    remaining = strat._remaining_library(state)
    # Started with 2 Island/2 Tarn/1 Bolt; saw 1 Island, 1 Tarn, 1 Bolt -> 1 Island, 1 Tarn left.
    assert sorted(remaining) == ["island", "scalding tarn"]


def test_coerce_mana_plan_drops_phantom_cards():
    state = {
        "deck_cards": ["Scalding Tarn", "Scalding Tarn", "Island", "Mountain", "Lightning Bolt"],
        "hand": ["Scalding Tarn"],
        "own_board": [],
        "your_graveyard": [],
        "your_exile": [],
    }
    raw = {
        "crack_fetch": "now",
        "fetch_target": "Island",  # in remaining library -> kept
        "fetch_alternatives": ["Mountain", "Bayou"],  # Bayou not in deck -> dropped
        "enter_untapped": True,
        "land_to_play": "Scalding Tarn",  # in hand -> kept
        "land_alternatives": ["Tropical Island"],  # not in hand -> dropped
        "color_needs": ["U", "R", "Z"],  # Z invalid -> dropped
        "hold_utility_lands": ["Otawara, Soaring City"],
        "reasoning": "fetch an Island for blue",
    }
    plan = strat._coerce_mana_plan(raw, state)
    assert plan is not None
    assert plan["crack_fetch"] == "now"
    assert plan["fetch_target"] == "Island"
    assert plan["fetch_alternatives"] == ["Mountain"]
    assert plan["land_to_play"] == "Scalding Tarn"
    assert plan["land_alternatives"] == []
    assert plan["color_needs"] == ["U", "R"]


def test_coerce_mana_plan_none_when_nothing_actionable():
    state = {"deck_cards": ["Island"], "hand": [], "own_board": []}
    raw = {"crack_fetch": "auto", "fetch_target": "Bayou", "color_needs": []}
    assert strat._coerce_mana_plan(raw, state) is None


@pytest.mark.asyncio
async def test_mana_planner_returns_validated_plan(monkeypatch):
    # No background profile build; just exercise the planner call + validation.
    monkeypatch.setattr(mana_profile, "get_or_schedule", lambda *a, **k: None)

    async def fake_generate_json(prompt, system=None, model=None, temperature=0.2):
        return {
            "crack_fetch": "now",
            "fetch_target": "Mountain",
            "fetch_alternatives": ["Goblin Guide"],  # not a land in library -> dropped
            "enter_untapped": True,
            "land_to_play": "",
            "color_needs": ["R"],
            "reasoning": "need red now",
        }

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)

    state = {
        "turn": 2,
        "deck_cards": ["Scalding Tarn", "Scalding Tarn", "Mountain", "Mountain", "Lightning Bolt"],
        "hand": ["Lightning Bolt"],
        "own_board": ["Scalding Tarn"],
        "your_graveyard": [],
        "your_exile": [],
        "resolved_format": "legacy",
    }
    out = await strat._run_mana_planner(state)
    assert "mana_plan" in out
    plan = out["mana_plan"]
    assert plan["crack_fetch"] == "now"
    assert plan["fetch_target"] == "Mountain"
    assert plan["fetch_alternatives"] == []  # phantom dropped
    assert plan["color_needs"] == ["R"]


@pytest.mark.asyncio
async def test_mana_planner_skips_basics_only_decks(monkeypatch):
    called = False

    async def fake_generate_json(*a, **k):
        nonlocal called
        called = True
        return {}

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)
    state = {"turn": 2, "deck_cards": ["Island"] * 40, "hand": [], "own_board": []}
    out = await strat._run_mana_planner(state)
    assert out == {}
    assert called is False  # no LLM call for a basics-only manabase
