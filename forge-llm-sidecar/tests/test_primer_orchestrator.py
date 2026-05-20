"""Tests for the multi-source primer orchestrator."""

from unittest.mock import patch

import pytest

from app.knowledge.primers import orchestrator
from app.knowledge.primers.sources.base import Candidate, Provider, RawPrimer


class _FakeProvider(Provider):
    """Test double — every instance shares the class-level scripted output."""

    publisher = "FakeProvider"
    supports = frozenset({"modern"})
    scripted_candidates: list[Candidate] = []
    scripted_primer: RawPrimer | None = None

    def search(self, archetype, fmt):
        return list(self.scripted_candidates)

    def fetch(self, candidate):
        return self.scripted_primer


class _NoOpProvider(Provider):
    publisher = "NoOpProvider"
    supports = frozenset({"modern"})

    def search(self, archetype, fmt):
        return []

    def fetch(self, candidate):
        return None


def _good_primer() -> RawPrimer:
    return RawPrimer(
        cleaned_text="x" * 800,
        candidate=Candidate(url="https://example.com/boros", title="Boros Energy"),
        publisher="FakeProvider",
        fetched_at="2026-05-20T00:00:00+00:00",
        http_status=200,
    )


def _good_extract() -> dict:
    return {
        "strategy_type": "aggro",
        "overview": "Boros Energy is fast.",
        "win_conditions": ["Phlage", "Ragavan"],
        "key_cards": [{"name": "Ragavan, Nimble Pilferer", "role": "pressure", "notes": ""}],
        "game_plan": {"early_game": ["play one-drops"], "mid_game": [], "late_game": []},
        "matchups": [],
    }


def test_orchestrator_returns_guide_on_successful_extract():
    _FakeProvider.scripted_candidates = [Candidate(url="https://example.com/boros")]
    _FakeProvider.scripted_primer = _good_primer()
    with patch.object(
        orchestrator.builder_llm, "extract_primer_fields", return_value=_good_extract()
    ):
        guide = orchestrator.build_primer(
            "Boros Energy",
            "modern",
            signature_cards=["Ragavan, Nimble Pilferer"],
            colors=["W", "R"],
            chain_override=[_FakeProvider],
        )
    assert guide is not None
    assert guide.archetype == "Boros Energy"
    assert guide.format == "modern"
    assert guide.provenance and guide.provenance[0].publisher == "FakeProvider"
    assert guide.decklist_hash  # hashed from signature cards


def test_orchestrator_falls_through_to_default_primer():
    with patch.object(
        orchestrator.default_primer, "synthesize"
    ) as fake_synth, patch.object(
        orchestrator.builder_llm, "extract_primer_fields"
    ) as fake_extract:
        fake_synth.return_value = (
            _good_extract(),
            {"publisher": "builder_llm_default", "author": "x", "source_url": "", "publish_date": "", "fetched_at": "now", "http_status": 0, "used_for_fields": ["*"]},
        )
        guide = orchestrator.build_primer(
            "Unknown Archetype",
            "modern",
            signature_cards=["Some Card"],
            colors=["U"],
            chain_override=[_NoOpProvider],
        )
        fake_extract.assert_not_called()
        fake_synth.assert_called_once()
    assert guide is not None
    assert guide.provenance[0].publisher == "builder_llm_default"


def test_orchestrator_skips_sparse_extract():
    _FakeProvider.scripted_candidates = [Candidate(url="https://example.com/sparse")]
    _FakeProvider.scripted_primer = _good_primer()
    sparse = {"overview": "", "key_cards": [], "matchups": [], "game_plan": {}}
    with patch.object(
        orchestrator.builder_llm, "extract_primer_fields", return_value=sparse
    ), patch.object(
        orchestrator.default_primer, "synthesize",
        return_value=(_good_extract(), {"publisher": "builder_llm_default", "author": "", "source_url": "", "publish_date": "", "fetched_at": "now", "http_status": 0, "used_for_fields": []}),
    ):
        guide = orchestrator.build_primer(
            "Test Deck",
            "modern",
            chain_override=[_FakeProvider],
            signature_cards=[],
            colors=[],
        )
    assert guide is not None
    # Fell through to default_primer.
    publishers = [p.publisher for p in guide.provenance]
    assert "builder_llm_default" in publishers


def test_hash_signature_stable():
    h1 = orchestrator._hash_signature(["Ragavan", "Phlage"])
    h2 = orchestrator._hash_signature(["phlage", "ragavan"])
    assert h1 == h2  # canonicalized (sorted + lowered)
