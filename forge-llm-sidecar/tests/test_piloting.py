"""Tests for the piloting-guidance layer (schema, loader, own-archetype id)."""

import pytest
from pydantic import ValidationError

from app.combo import analyze_combo_state
from app.knowledge import piloting
from app.knowledge.piloting_schema import PilotingGuide, StrategyType

GENERIC_STRATEGIES = ["aggro", "tempo", "midrange", "control", "combo", "ramp"]


def _minimal_guide() -> dict:
    return {"archetype": "Test Deck", "strategy_type": "aggro"}


def test_schema_accepts_minimal_guide():
    guide = PilotingGuide.model_validate(_minimal_guide())
    assert guide.archetype == "Test Deck"
    assert guide.strategy_type is StrategyType.AGGRO
    assert guide.metadata.schema_version == 2
    assert guide.combo_profile is None


def test_schema_rejects_missing_required_field():
    with pytest.raises(ValidationError):
        PilotingGuide.model_validate({"archetype": "No Strategy"})


def test_schema_rejects_bad_strategy_type():
    bad = _minimal_guide()
    bad["strategy_type"] = "not-a-strategy"
    with pytest.raises(ValidationError):
        PilotingGuide.model_validate(bad)


@pytest.mark.parametrize("strategy", GENERIC_STRATEGIES)
def test_generic_guides_load_and_validate(strategy):
    guide = piloting.get_piloting_guide("Nonexistent Archetype", "modern", strategy)
    assert guide is not None
    assert guide.strategy_type.value == strategy


def test_fallback_to_midrange_for_unknown_strategy():
    guide = piloting.get_piloting_guide("Whatever", "modern", strategy_type=None)
    assert guide is not None
    assert guide.strategy_type is StrategyType.MIDRANGE


def test_slugify():
    assert piloting.slugify("Boros Energy") == "boros-energy"
    assert piloting.slugify("Goryo's Vengeance") == "goryo-s-vengeance"
    assert piloting.slugify("  4/5c Omnath  ") == "4-5c-omnath"


def test_identify_own_archetype_matches_signature_cards():
    # Ragavan is a Boros Energy signature card in the modern metagame data.
    name, strategy = piloting.identify_own_archetype(
        ["Ragavan, Nimble Pilferer", "Lightning Bolt", "Mountain"], "modern"
    )
    assert name == "Boros Energy"
    assert isinstance(strategy, StrategyType)


def test_identify_own_archetype_no_match():
    name, strategy = piloting.identify_own_archetype(["Island", "Plains"], "modern")
    assert name is None
    assert strategy is StrategyType.MIDRANGE


def test_identify_own_archetype_empty_deck():
    name, strategy = piloting.identify_own_archetype([], "modern")
    assert name is None
    assert strategy is StrategyType.MIDRANGE


def test_available_guides_lists_generic():
    guides = piloting.available_guides()
    assert "generic" in guides
    # v2: shape is {"live": [...], "archive": [...]}
    assert set(GENERIC_STRATEGIES).issubset(set(guides["generic"]["live"]))
    assert guides["generic"]["archive"] == []


def test_ruby_storm_combo_profile_loads():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    assert guide is not None
    assert guide.combo_profile is not None
    assert "reducers" in guide.combo_profile.required_setup_categories
    assert {line.name for line in guide.combo_profile.known_lines} >= {
        "Normal storm turn",
        "Past in Flames reload",
        "Wish/payoff line",
    }


def test_combo_analyzer_high_readiness_with_reducer_rituals_payoff():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    profile = guide.combo_profile.model_dump()
    state = {
        "hand": ["Desperate Ritual", "Pyretic Ritual", "Wrenn's Resolve", "Grapeshot"],
        "own_board": ["Ruby Medallion"],
        "your_graveyard": [],
        "available_mana": ["R", "R", "C"],
        "archetype": "Boros Energy",
        "opponent_board": [],
        "opponent_hand": [],
        "opp_untapped_sources": [],
    }
    plan = analyze_combo_state(state, profile)
    assert plan["go_for_it_now"] is True
    assert plan["readiness_score"] >= 75
    assert plan["preferred_line"] == "Normal storm turn"


def test_combo_analyzer_past_in_flames_reload_high_readiness():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    profile = guide.combo_profile.model_dump()
    state = {
        "hand": ["Past in Flames", "Grapeshot", "Pyretic Ritual"],
        "own_board": ["Ruby Medallion"],
        "your_graveyard": ["Desperate Ritual", "Manamorphose", "Wrenn's Resolve"],
        "available_mana": ["R", "R", "R", "C"],
        "archetype": "Boros Energy",
        "opponent_board": [],
        "opponent_hand": [],
        "opp_untapped_sources": [],
    }
    plan = analyze_combo_state(state, profile)
    assert plan["go_for_it_now"] is True
    assert plan["preferred_line"] == "Past in Flames reload"


def test_combo_analyzer_low_readiness_prefers_setup():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    profile = guide.combo_profile.model_dump()
    state = {
        "hand": ["Mountain", "Grapeshot", "Spell Pierce"],
        "own_board": [],
        "your_graveyard": [],
        "available_mana": ["R"],
        "archetype": "Boros Energy",
        "opponent_board": [],
        "opponent_hand": [],
        "opp_untapped_sources": [],
    }
    plan = analyze_combo_state(state, profile)
    assert plan["go_for_it_now"] is False
    assert plan["readiness_score"] < 50
    assert "reducers" in plan["missing_pieces"]


def test_combo_analyzer_control_open_blue_raises_risk():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    profile = guide.combo_profile.model_dump()
    state = {
        "hand": ["Desperate Ritual", "Pyretic Ritual", "Wrenn's Resolve", "Grapeshot"],
        "own_board": ["Ruby Medallion"],
        "your_graveyard": [],
        "available_mana": ["R", "R", "C"],
        "archetype": "Azorius Control",
        "opponent_board": [],
        "opponent_hand": [],
        "opp_untapped_sources": [["U"], ["W"]],
    }
    plan = analyze_combo_state(state, profile)
    assert "countermagic" in plan["risk_assessment"]
    assert plan["bucket_state"]["go_threshold"] >= 85


def test_combo_analyzer_aggro_lowers_threshold():
    guide = piloting.get_piloting_guide("Ruby Storm", "modern", "combo")
    profile = guide.combo_profile.model_dump()
    state = {
        "hand": ["Desperate Ritual", "Pyretic Ritual", "Wrenn's Resolve", "Grapeshot"],
        "own_board": ["Ruby Medallion"],
        "your_graveyard": [],
        "available_mana": ["R", "R"],
        "archetype": "Mono-Red Aggro",
        "opponent_board": ["Goblin Guide"],
        "opponent_hand": [],
        "opp_untapped_sources": [],
    }
    plan = analyze_combo_state(state, profile)
    assert plan["bucket_state"]["go_threshold"] == 65.0
