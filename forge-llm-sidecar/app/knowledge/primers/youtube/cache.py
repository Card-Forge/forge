"""On-disk caches for YouTube data.

Two caches:

* ``channels/<channel_id>.json`` — last N videos for a channel. Rebuilt daily
  by ``scripts/build_youtube_cache.py``. TTL 24h, but staleness is non-blocking
  — if the cache is missing or old, the enricher just skips that channel.
* ``transcripts/<video_id>.json`` — fetched auto-caption transcript for a
  video. Permanent — transcripts don't change.

Cache is committed-ignored by adding to .gitignore.
"""

from __future__ import annotations

import datetime as dt
import json
import logging
import pathlib
from typing import Optional

log = logging.getLogger(__name__)

ROOT = pathlib.Path(__file__).resolve().parents[4]
CACHE_DIR = ROOT / "cache" / "youtube"
CHANNELS_DIR = CACHE_DIR / "channels"
TRANSCRIPTS_DIR = CACHE_DIR / "transcripts"

#: how long a channel-video-list cache is considered fresh
_CHANNEL_TTL_HOURS = 36


def _now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")


def channel_path(channel_id: str) -> pathlib.Path:
    return CHANNELS_DIR / f"{channel_id}.json"


def transcript_path(video_id: str) -> pathlib.Path:
    return TRANSCRIPTS_DIR / f"{video_id}.json"


def load_channel_videos(channel_id: str, *, max_age_hours: int = _CHANNEL_TTL_HOURS) -> Optional[dict]:
    """Return cached channel-video-list payload or None if missing/stale.

    ``None`` from a stale cache is non-fatal — the caller can still fall back
    to whatever is there with ``load_channel_videos_any``.
    """
    p = channel_path(channel_id)
    if not p.exists():
        return None
    try:
        payload = json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    fetched_at = payload.get("fetched_at", "")
    try:
        fetched_dt = dt.datetime.fromisoformat(fetched_at.replace("Z", "+00:00"))
    except ValueError:
        return None
    age = dt.datetime.now(dt.timezone.utc) - fetched_dt.astimezone(dt.timezone.utc)
    if age > dt.timedelta(hours=max_age_hours):
        log.info("youtube cache: %s is stale (%d hours)", channel_id, age.total_seconds() / 3600)
        return None
    return payload


def load_channel_videos_any(channel_id: str) -> Optional[dict]:
    """Like load_channel_videos but accepts arbitrarily stale caches."""
    p = channel_path(channel_id)
    if not p.exists():
        return None
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def save_channel_videos(channel_id: str, videos: list[dict], *, channel_meta: dict) -> None:
    CHANNELS_DIR.mkdir(parents=True, exist_ok=True)
    payload = {
        "channel_id": channel_id,
        "channel": channel_meta,
        "fetched_at": _now_iso(),
        "video_count": len(videos),
        "videos": videos,
    }
    channel_path(channel_id).write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )


def load_transcript(video_id: str) -> Optional[list[dict]]:
    p = transcript_path(video_id)
    if not p.exists():
        return None
    try:
        return json.loads(p.read_text(encoding="utf-8")).get("chunks")
    except (OSError, ValueError):
        return None


def save_transcript(video_id: str, chunks: list[dict]) -> None:
    TRANSCRIPTS_DIR.mkdir(parents=True, exist_ok=True)
    payload = {
        "video_id": video_id,
        "fetched_at": _now_iso(),
        "chunk_count": len(chunks),
        "chunks": chunks,
    }
    transcript_path(video_id).write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )


def mark_transcript_unavailable(video_id: str, reason: str) -> None:
    """Record that a video has no usable transcript so we don't retry."""
    TRANSCRIPTS_DIR.mkdir(parents=True, exist_ok=True)
    transcript_path(video_id).write_text(
        json.dumps(
            {"video_id": video_id, "fetched_at": _now_iso(), "chunks": [], "unavailable": reason},
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
