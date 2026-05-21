"""Tests for the enrichment merge logic (no network)."""

from unittest.mock import patch

import pytest

from app.knowledge.piloting_schema import (
    EvidenceKind,
    KeyCard,
    MatchupNote,
    PilotingGuide,
)
from app.knowledge.primers.youtube import enricher
from app.knowledge.primers.youtube.discover import VideoCandidate
from app.knowledge.primers.youtube.match_parse import FeatureMatch


def _make_guide() -> PilotingGuide:
    return PilotingGuide.model_validate(
        {
            "archetype": "Boros Energy",
            "format": "modern",
            "strategy_type": "aggro",
            "overview": "Fast aggressive deck.",
            "sequencing_tips": ["Hold removal for Phlage"],
            "matchups": [
                {
                    "opponent_archetype": "Living End",
                    "advice": "Bring graveyard hate",
                    "watch_for": ["Force of Negation"],
                }
            ],
            "key_cards": [
                {"name": "Ragavan, Nimble Pilferer", "role": "pressure", "notes": ""}
            ],
        }
    )


def _candidate(channel_kind: str = "creator", opponent: str = "") -> VideoCandidate:
    return VideoCandidate(
        video_id="abc123",
        title="Boros Energy modern grinding",
        channel_id="UC-test",
        channel_name="AspiringSpike",
        channel_kind=channel_kind,
        upload_date="2026-05-19",
        view_count=12345,
        url="https://www.youtube.com/watch?v=abc123",
        description="grinding boros energy on ladder",
        archetype_side="Boros Energy",
        opponent_side=opponent,
    )


def test_merge_appends_new_sequencing_tip():
    guide = _make_guide()
    heuristics = {
        "sequencing_tips": [
            {
                "rule": "Lead with Guide of Souls to bait Solitude",
                "timestamp_range": "12:34-13:00",
                "confidence": 0.8,
            }
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 1
    assert any("Guide of Souls to bait Solitude" in t for t in guide.sequencing_tips)
    # evidence appended
    assert guide.sequencing_tips_evidence[-1].kind == EvidenceKind.GAMEPLAY_OBSERVED
    assert guide.sequencing_tips_evidence[-1].evidence_span == "12:34-13:00"
    assert guide.sequencing_tips_evidence[-1].source_index == 0  # first provenance entry


def test_merge_dedupes_near_duplicate_tips():
    guide = _make_guide()
    # Already has "Hold removal for Phlage"
    heuristics = {
        "sequencing_tips": [
            {"rule": "Hold removal for Phlage", "timestamp_range": "0:30-1:00", "confidence": 0.7}
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 0
    assert len(guide.sequencing_tips) == 1


def test_merge_extends_existing_matchup():
    guide = _make_guide()
    heuristics = {
        "matchup_advice": [
            {
                "opponent_archetype": "Living End",
                "advice": "Keep Tormod's Crypt up on turn 2",
                "timestamp_range": "5:00-5:30",
                "confidence": 0.8,
            }
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 1
    le = next(m for m in guide.matchups if m.opponent_archetype == "Living End")
    assert "Tormod's Crypt" in le.advice
    assert "(from gameplay)" in le.advice


def test_merge_creates_new_matchup_entry():
    guide = _make_guide()
    heuristics = {
        "matchup_advice": [
            {
                "opponent_archetype": "Amulet Titan",
                "advice": "Race them before turn-3 Titan",
                "timestamp_range": "8:00-8:30",
            }
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 1
    at = next(m for m in guide.matchups if m.opponent_archetype == "Amulet Titan")
    assert "Race them before turn-3 Titan" in at.advice


def test_merge_skips_self_matchups():
    guide = _make_guide()
    heuristics = {
        "matchup_advice": [
            {"opponent_archetype": "Boros Energy", "advice": "Mirror is about Phlage", "timestamp_range": "1:00-1:30"}
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    # Self-matchup should be skipped (or handled differently)
    assert added == 0


def test_merge_appends_mulligan_example():
    guide = _make_guide()
    heuristics = {
        "mulligan_observations": [
            {
                "hand_description": "2 lands, Ragavan, Phlage, removal, removal, blank",
                "decision": "keep",
                "reason": "fast clock with two-mana threat",
                "timestamp_range": "0:30-1:00",
            }
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 1
    assert guide.mulligan.examples[-1].decision == "keep"
    assert "Ragavan" in guide.mulligan.examples[-1].reason


def test_merge_appends_key_card_note():
    guide = _make_guide()
    heuristics = {
        "key_card_notes": [
            {
                "name": "Ragavan, Nimble Pilferer",
                "note": "Always play turn 1 even into removal threat",
                "timestamp_range": "0:15-0:30",
            }
        ]
    }
    added = enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    assert added == 1
    kc = next(c for c in guide.key_cards if c.name == "Ragavan, Nimble Pilferer")
    assert "Always play turn 1" in kc.notes
    assert "(from gameplay)" in kc.notes


def test_merge_records_provenance_with_url_and_archetype():
    guide = _make_guide()
    heuristics = {
        "sequencing_tips": [{"rule": "x", "timestamp_range": "0:00-0:10"}]
    }
    enricher._merge(guide, heuristics, _candidate(), "Boros Energy")
    p = guide.provenance[-1]
    assert p.publisher.startswith("YouTube/")
    assert p.source_url == "https://www.youtube.com/watch?v=abc123"
    assert p.extracted_for_archetype == "Boros Energy"
    assert "sequencing_tips" in p.used_for_fields


def test_enrich_is_noop_when_no_videos():
    guide = _make_guide()
    pre = len(guide.provenance)
    with patch.object(enricher.discover, "find_videos", return_value=[]):
        out = enricher.enrich(
            guide, archetype="Boros Energy", fmt="modern", signature_cards=[]
        )
    assert len(out.provenance) == pre
    assert len(out.sequencing_tips) == 1  # unchanged


def test_merge_dedupes_already_added_video():
    """Running the same merge twice should be idempotent: no new provenance,
    no doubled (from gameplay) markers."""
    guide = _make_guide()
    cand = _candidate()
    heuristics = {
        "matchup_advice": [
            {
                "opponent_archetype": "Amulet Titan",
                "advice": "Race them before turn-3 Titan",
                "timestamp_range": "8:00-8:30",
            }
        ]
    }
    n1 = enricher._merge(guide, heuristics, cand, "Boros Energy")
    pre_provs = len(guide.provenance)
    pre_advice = next(m.advice for m in guide.matchups if m.opponent_archetype == "Amulet Titan")

    n2 = enricher._merge(guide, heuristics, cand, "Boros Energy")
    assert n2 == 0  # second merge adds nothing
    assert len(guide.provenance) == pre_provs  # no new provenance
    # Matchup advice unchanged (no duplicate (from gameplay) marker)
    advice_after = next(m.advice for m in guide.matchups if m.opponent_archetype == "Amulet Titan")
    assert advice_after == pre_advice


def test_enrich_skips_videos_without_transcripts():
    guide = _make_guide()
    cand = _candidate()
    with patch.object(enricher.discover, "find_videos", return_value=[cand]):
        with patch.object(enricher.transcripts, "fetch_transcript", return_value=None):
            out = enricher.enrich(
                guide, archetype="Boros Energy", fmt="modern", signature_cards=[]
            )
    assert len(out.provenance) == 0
