"""Tests for YouTube candidate discovery (channel-list cache → ranked candidates)."""

import datetime as dt
import json
from pathlib import Path
from unittest.mock import patch

import pytest

from app.knowledge.primers.youtube import cache, discover


@pytest.fixture
def fake_cache(tmp_path, monkeypatch):
    cache_dir = tmp_path / "cache" / "youtube"
    (cache_dir / "channels").mkdir(parents=True)
    (cache_dir / "transcripts").mkdir(parents=True)
    monkeypatch.setattr(cache, "CACHE_DIR", cache_dir)
    monkeypatch.setattr(cache, "CHANNELS_DIR", cache_dir / "channels")
    monkeypatch.setattr(cache, "TRANSCRIPTS_DIR", cache_dir / "transcripts")
    return cache_dir


@pytest.fixture
def fake_channels(monkeypatch, tmp_path):
    cfg = {
        "channels": [
            {
                "channel_id": "UC-ASPIRINGSPIKE",
                "display_name": "AspiringSpike",
                "kind": "creator",
                "priority": 1,
                "formats": ["modern", "pioneer"],
            },
            {
                "channel_id": "UC-PLAYMTG",
                "display_name": "Play MTG",
                "kind": "tournament",
                "priority": 1,
                "formats": ["modern", "pioneer", "standard"],
            },
            {
                "channel_id": "UC-LSV",
                "display_name": "LSV",
                "kind": "creator",
                "priority": 3,
                "formats": ["modern"],
            },
        ]
    }
    chan_path = tmp_path / "channels.json"
    chan_path.write_text(json.dumps(cfg))
    monkeypatch.setattr(discover, "_CHANNELS_PATH", chan_path)
    return cfg


def _save_videos(channel_id: str, videos: list[dict], meta: dict | None = None):
    cache.save_channel_videos(channel_id, videos, channel_meta=meta or {"display_name": channel_id})


def test_find_videos_filters_by_format(fake_cache, fake_channels):
    _save_videos(
        "UC-ASPIRINGSPIKE",
        [
            {
                "video_id": "v1",
                "title": "Modern Boros Energy Ladder Climb",
                "description": "playing boros energy in modern",
                "upload_date": "2026-05-19",
                "view_count": 50000,
            },
            {
                "video_id": "v2",
                "title": "Standard Mono-Red — not modern",
                "description": "standard format",
                "upload_date": "2026-05-19",
                "view_count": 30000,
            },
        ],
    )
    cands = discover.find_videos("Boros Energy", "modern")
    urls = [c.url for c in cands]
    assert any("v1" in u for u in urls)
    assert not any("v2" in u for u in urls)


def test_find_videos_filters_by_since_date(fake_cache, fake_channels):
    _save_videos(
        "UC-ASPIRINGSPIKE",
        [
            {
                "video_id": "old",
                "title": "Boros Energy Modern - march 2025",
                "description": "modern boros energy",
                "upload_date": "2025-03-01",
                "view_count": 10000,
            },
            {
                "video_id": "new",
                "title": "Boros Energy Modern - post ban",
                "description": "modern boros energy",
                "upload_date": "2026-05-20",
                "view_count": 5000,
            },
        ],
    )
    cands = discover.find_videos("Boros Energy", "modern", since=dt.date(2026, 5, 18))
    urls = [c.url for c in cands]
    assert any("new" in u for u in urls)
    assert not any("old" in u for u in urls)


def test_find_videos_ranks_priority_then_views(fake_cache, fake_channels):
    _save_videos(
        "UC-ASPIRINGSPIKE",
        [{"video_id": "spike1", "title": "Boros Energy Modern", "description": "", "upload_date": "2026-05-19", "view_count": 1000}],
    )
    _save_videos(
        "UC-LSV",
        [{"video_id": "lsv1", "title": "Boros Energy Modern", "description": "", "upload_date": "2026-05-19", "view_count": 100000}],
    )
    cands = discover.find_videos("Boros Energy", "modern")
    # AspiringSpike has priority 1; LSV is 3 — Spike should win even with fewer views.
    assert cands[0].channel_name == "AspiringSpike"


def test_tournament_video_uses_identified_archetypes(fake_cache, fake_channels):
    """When title/desc doesn't name the decks, pre-identified archetypes
    (from transcript signature-card matching) should still surface the video."""
    _save_videos(
        "UC-PLAYMTG",
        [
            {
                "video_id": "pt-uniden",
                "title": "Semifinal | Larsen vs. Zhang | Modern | #PTSOS",
                "description": "Find decklists at magic.gg",  # archetype NOT in metadata
                "upload_date": "2026-05-15",
                "view_count": 80000,
                "identified_format": "modern",
                "identified_archetypes": [
                    {"name": "Boros Energy", "distinct_cards": 5, "total_mentions": 18, "confidence": 0.85},
                    {"name": "Living End", "distinct_cards": 4, "total_mentions": 12, "confidence": 0.72},
                ],
            }
        ],
    )
    cands = discover.find_videos("Boros Energy", "modern")
    assert len(cands) >= 1
    c = next(c for c in cands if c.video_id == "pt-uniden")
    assert c.archetype_side == "Boros Energy"
    assert c.opponent_side == "Living End"


def test_tournament_video_indexed_for_each_archetype(fake_cache, fake_channels):
    _save_videos(
        "UC-PLAYMTG",
        [
            {
                "video_id": "pt-feature",
                "title": "Reid Duke vs Andrea Mengucci | Pro Tour Modern",
                "description": "Reid Duke (Boros Energy) vs Andrea Mengucci (Living End) Modern",
                "upload_date": "2026-05-15",
                "view_count": 80000,
            }
        ],
    )
    known = ["Boros Energy", "Living End"]
    boros_cands = discover.find_videos(
        "Boros Energy", "modern", known_archetypes=known
    )
    living_cands = discover.find_videos(
        "Living End", "modern", known_archetypes=known
    )
    assert len(boros_cands) >= 1
    assert len(living_cands) >= 1
    bc = next(c for c in boros_cands if c.channel_kind == "tournament")
    assert bc.archetype_side == "Boros Energy"
    assert bc.opponent_side == "Living End"
    lc = next(c for c in living_cands if c.channel_kind == "tournament")
    assert lc.archetype_side == "Living End"
    assert lc.opponent_side == "Boros Energy"
