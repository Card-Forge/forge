"""Tests for the opponent_strategist node and the archetype profile loader."""

from __future__ import annotations

import pytest

from app.knowledge import loader
from app.nodes import opponent_strategist as strat


def _base_state(**overrides) -> dict:
    state = {
        "game_id": "g1",
        "format": "Modern",
        "resolved_format": "modern",
        "turn": 3,
        "archetype": "Ruby Storm",
        "confidence": 0.97,
        "decision_type": "critical",
        "observations": [{"turn": 2, "event": "spell", "card": "Ruby Medallion"}],
        "opponent_board": ["Ruby Medallion"],
        "opponent_graveyard": ["Pyretic Ritual"],
        "opp_hand_size": 5,
        "opponent_hand": [
            {"category": "combo_piece", "example_cards": ["Grapeshot"],
             "probability": 0.3, "reasoning": "old"}
        ],
        "target_priorities": [],
    }
    state.update(overrides)
    return state


def test_profile_loads_and_reverse_index():
    profile = loader.load_archetype_profile("Ruby Storm", "modern")
    assert profile and profile["name"] == "Ruby Storm"
    # Many-to-many membership: Ral is in several buckets.
    buckets = loader.card_buckets("Ral, Monsoon Mage", "modern", "Ruby Storm")
    assert "mana_reducers" in buckets and "win_conditions" in buckets


def test_missing_profile_returns_none():
    assert loader.load_archetype_profile("Totally Fake Deck", "modern") is None


def test_bayesian_prior_subtracts_revealed():
    profile = loader.load_archetype_profile("Ruby Storm", "modern")
    revealed = {"ruby medallion", "pyretic ritual"}
    prior = strat._bayesian_prior(profile, "Ruby Storm", "modern", revealed, 5)
    assert prior  # non-empty
    # Probabilities are non-negative and bounded.
    assert all(0.0 <= v <= 1.0 for v in prior.values())


@pytest.mark.asyncio
async def test_strategist_filters_hallucinated_cards(monkeypatch):
    async def fake_generate_json(prompt, **kw):
        return {
            "bucket_probabilities": {
                "rituals": {"prob": 0.8, "top_cards": ["Desperate Ritual", "Made Up Card"]},
                "not_a_real_bucket": {"prob": 0.9, "top_cards": ["X"]},
            },
            "predicted_opp_line": {"primary_play": "Grapeshot kill",
                                   "supporting_plays": [], "mana_required": "RR",
                                   "reasoning": "storm"},
            "threat_priorities": [
                {"name": "Ruby Medallion", "score": 0.9, "reason": "engine"},
                {"name": "Not On Board", "score": 0.2, "reason": "n/a"},
            ],
            "beatdown": {"who_is_beatdown": "opponent", "reasoning": "faster clock"},
        }

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)
    out = await strat.opponent_strategist_node(_base_state())

    cats = {g["category"] for g in out["opponent_hand"]}
    assert "rituals" in cats and "not_a_real_bucket" not in cats
    ritual_cards = next(g for g in out["opponent_hand"] if g["category"] == "rituals")["example_cards"]
    assert "Desperate Ritual" in ritual_cards and "Made Up Card" not in ritual_cards
    # Threats are filtered to permanents actually on the opponent board.
    assert out["target_priorities"][0]["targets"] == ["Ruby Medallion"]
    assert out["predicted_opp_line"]["primary_play"] == "Grapeshot kill"
    assert out["beatdown_assessment"]["who_is_beatdown"] == "opponent"


@pytest.mark.asyncio
async def test_no_profile_keeps_existing_inference(monkeypatch):
    called = {"n": 0}

    async def fake_generate_json(prompt, **kw):
        called["n"] += 1
        return {}

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)
    state = _base_state(archetype="Some Off-Meta Brew")
    out = await strat.opponent_strategist_node(state)
    assert called["n"] == 0  # no LLM call without a profile
    assert out["opponent_hand"] == state["opponent_hand"]  # unchanged


@pytest.mark.asyncio
async def test_llm_failure_falls_back(monkeypatch):
    from app.llm_client import LLMError

    async def boom(prompt, **kw):
        raise LLMError("backend down")

    monkeypatch.setattr(strat, "generate_json", boom)
    state = _base_state()
    out = await strat.opponent_strategist_node(state)
    # Deterministic inference from game_advisor is preserved on failure.
    assert out["opponent_hand"] == state["opponent_hand"]


@pytest.mark.asyncio
async def test_low_confidence_priority_skips(monkeypatch):
    called = {"n": 0}

    async def fake_generate_json(prompt, **kw):
        called["n"] += 1
        return {}

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)
    state = _base_state(confidence=0.3, decision_type="priority")
    await strat.opponent_strategist_node(state)
    assert called["n"] == 0  # routine low-confidence decision: no extra call
