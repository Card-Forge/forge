"""Fetch and coalesce YouTube auto-caption transcripts.

Uses ``youtube-transcript-api`` (no API key required, just video ID). The
library returns raw caption segments; we coalesce them into ~45-second
chunks because isolated 2-second chunks lose context.

Transcripts are cached permanently — captions don't change.

Rate limiting:
  * Enforces a min interval between fetches (default 3.5s, override with
    ``YOUTUBE_TRANSCRIPT_MIN_INTERVAL``).
  * On IpBlocked, applies exponential backoff (60s → 300s → 900s, capped).
  * After 3 consecutive IpBlocked errors, fetches stop returning None
    instead of retrying — the caller can resume later from the cache.
"""

from __future__ import annotations

import logging
import os
import random
import threading
import time
from dataclasses import dataclass
from typing import Optional

from app.knowledge.primers.youtube import cache

log = logging.getLogger(__name__)

# Throttle: minimum seconds between consecutive transcript fetches.
_MIN_FETCH_INTERVAL = float(os.environ.get("YOUTUBE_TRANSCRIPT_MIN_INTERVAL", "3.5"))
# IpBlocked backoff schedule: each entry is the sleep BEFORE the next attempt.
_IP_BLOCK_BACKOFF = [60.0, 300.0, 900.0]
# After this many consecutive IpBlocked failures, stop returning real fetches.
_IP_BLOCK_HARD_STOP = 3

# Module-level rate-limit state. Locked so multiple threads (if any) don't race.
_state_lock = threading.Lock()
_last_fetch_time: float = 0.0
_consecutive_ip_blocks: int = 0


def _throttle() -> None:
    """Sleep so consecutive fetches are at least _MIN_FETCH_INTERVAL apart."""
    global _last_fetch_time
    with _state_lock:
        now = time.monotonic()
        wait = _MIN_FETCH_INTERVAL - (now - _last_fetch_time)
        if wait > 0:
            # Add a small jitter so we don't synchronize on the boundary.
            wait += random.uniform(0, 0.5)
            time.sleep(wait)
        _last_fetch_time = time.monotonic()


def _is_ip_blocked(exc: Exception) -> bool:
    name = type(exc).__name__
    return "IpBlocked" in name or "IPBlocked" in name


def _register_ip_block() -> int:
    global _consecutive_ip_blocks
    with _state_lock:
        _consecutive_ip_blocks += 1
        return _consecutive_ip_blocks


def _register_ok() -> None:
    global _consecutive_ip_blocks
    with _state_lock:
        _consecutive_ip_blocks = 0


def ip_block_hard_stop() -> bool:
    """True when we've hit too many consecutive IpBlocked errors."""
    with _state_lock:
        return _consecutive_ip_blocks >= _IP_BLOCK_HARD_STOP


@dataclass(slots=True)
class TranscriptChunk:
    start_sec: float
    end_sec: float
    text: str


def _try_import():
    """Return ``(api_class, exception_types)`` or ``(None, ())`` if missing.

    Supports both the legacy v0.x API (classmethod ``get_transcript``) and
    the v1.x API (instance method ``fetch``). The wrapper hides that.
    """
    try:
        from youtube_transcript_api import YouTubeTranscriptApi  # type: ignore
        try:  # v1.x exception module path
            from youtube_transcript_api._errors import (  # type: ignore
                CouldNotRetrieveTranscript,
                NoTranscriptFound,
                TranscriptsDisabled,
                VideoUnavailable,
            )
            excs = (
                CouldNotRetrieveTranscript,
                NoTranscriptFound,
                TranscriptsDisabled,
                VideoUnavailable,
            )
        except ImportError:
            from youtube_transcript_api import (  # type: ignore
                NoTranscriptFound,
                TranscriptsDisabled,
                VideoUnavailable,
            )
            excs = (NoTranscriptFound, TranscriptsDisabled, VideoUnavailable)
        return YouTubeTranscriptApi, excs
    except ImportError:
        log.warning(
            "youtube-transcript-api not installed; install with "
            "`pip install youtube-transcript-api`"
        )
        return None, ()


def _call_fetch(api_cls, video_id: str) -> list[dict]:
    """Bridge legacy + new youtube-transcript-api versions.

    Returns a list of ``{"text", "start", "duration"}`` dicts.
    """
    langs = ["en", "en-US", "en-GB"]
    # v1.x: instance method fetch() -> FetchedTranscript with to_raw_data()
    if hasattr(api_cls, "fetch") and not hasattr(api_cls, "get_transcript"):
        return api_cls().fetch(video_id, languages=langs).to_raw_data()
    # legacy: classmethod get_transcript()
    return api_cls.get_transcript(video_id, languages=langs)


def fetch_transcript(
    video_id: str, *, chunk_seconds: float = 45.0
) -> Optional[list[TranscriptChunk]]:
    """Return coalesced transcript chunks, or None if unavailable.

    Uses the on-disk cache first. On a miss, calls
    ``youtube-transcript-api`` for English captions (auto-generated is fine).
    Caches failures too so we don't retry videos that have no captions.
    """
    cached = cache.load_transcript(video_id)
    if cached is not None:
        if not cached:
            return None  # cached miss
        return [TranscriptChunk(c["start_sec"], c["end_sec"], c["text"]) for c in cached]

    if ip_block_hard_stop():
        log.info("transcript: skipping %s — hit IpBlocked hard stop", video_id)
        return None

    api, exc_types = _try_import()
    if api is None:
        return None

    raw = None
    for attempt in range(len(_IP_BLOCK_BACKOFF) + 1):
        _throttle()
        try:
            raw = _call_fetch(api, video_id)
            _register_ok()
            break
        except exc_types as exc:
            if _is_ip_blocked(exc):
                count = _register_ip_block()
                if attempt < len(_IP_BLOCK_BACKOFF):
                    backoff = _IP_BLOCK_BACKOFF[attempt]
                    log.warning(
                        "transcript: %s IpBlocked (#%d) — sleeping %.0fs before retry",
                        video_id, count, backoff,
                    )
                    time.sleep(backoff)
                    continue
                log.warning("transcript: %s IpBlocked after %d attempts — giving up", video_id, attempt + 1)
                # DO NOT cache an IpBlocked as permanently unavailable —
                # captions probably exist, we're just blocked.
                return None
            # Non-IP-block expected error: captions disabled, video gone, etc.
            log.info("transcript: %s -> %s", video_id, type(exc).__name__)
            cache.mark_transcript_unavailable(video_id, type(exc).__name__)
            return None
        except Exception as exc:  # noqa: BLE001 — network/parsing failures
            log.warning("transcript: %s -> %s", video_id, exc)
            return None
    if raw is None:
        return None

    chunks = _coalesce(raw, chunk_seconds=chunk_seconds)
    cache.save_transcript(
        video_id,
        [{"start_sec": c.start_sec, "end_sec": c.end_sec, "text": c.text} for c in chunks],
    )
    return chunks


def _coalesce(raw: list[dict], *, chunk_seconds: float) -> list[TranscriptChunk]:
    """Combine 2-5 second caption segments into ~45-second context windows."""
    out: list[TranscriptChunk] = []
    if not raw:
        return out
    buf_start: float = float(raw[0].get("start", 0.0))
    buf_end: float = buf_start
    buf_text: list[str] = []
    for seg in raw:
        start = float(seg.get("start", 0.0))
        dur = float(seg.get("duration", 0.0))
        text = (seg.get("text") or "").replace("\n", " ").strip()
        if not text:
            continue
        end = start + dur
        if (end - buf_start) >= chunk_seconds and buf_text:
            out.append(TranscriptChunk(buf_start, buf_end, " ".join(buf_text)))
            buf_start = start
            buf_text = []
        buf_text.append(text)
        buf_end = end
    if buf_text:
        out.append(TranscriptChunk(buf_start, buf_end, " ".join(buf_text)))
    return out


def render_for_prompt(chunks: list[TranscriptChunk], *, max_chars: int = 18000) -> str:
    """Serialize chunks as timestamped lines for the extractor LLM."""
    lines: list[str] = []
    total = 0
    for c in chunks:
        line = f"[{_fmt_ts(c.start_sec)}-{_fmt_ts(c.end_sec)}] {c.text}"
        if total + len(line) + 1 > max_chars:
            lines.append("[...transcript truncated for length...]")
            break
        lines.append(line)
        total += len(line) + 1
    return "\n".join(lines)


def _fmt_ts(seconds: float) -> str:
    m, s = divmod(int(seconds), 60)
    h, m = divmod(m, 60)
    if h:
        return f"{h:d}:{m:02d}:{s:02d}"
    return f"{m:d}:{s:02d}"
