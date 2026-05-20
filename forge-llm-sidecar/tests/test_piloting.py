"""Tests for the piloting-guidance layer (schema, loader, own-archetype id)."""

import pytest
from pydantic import ValidationError

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
