"""Thin wrapper around the YouTube Data API v3.

Used ONLY by ``scripts/build_youtube_cache.py`` — never on the enrichment
hot path. The hot path reads pre-built caches and never burns quota.

Quota math (free tier = 10k units/day):
  * channels.list(by handle):     1 unit per handle
  * playlistItems.list(uploads):  1 unit per 50-video page
  * videos.list(view counts):     1 unit per 50-video batch

For ~10 channels × 4 pages each + 4 videos.list batches per channel ≈ 80
units/channel/day = 800 units total. Well under the cap.
"""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from typing import Iterable, Optional

log = logging.getLogger(__name__)

YOUTUBE_API_KEY = os.environ.get("YOUTUBE_API_KEY", "")


class YouTubeAPIError(RuntimeError):
    pass


@dataclass(slots=True)
class ChannelInfo:
    channel_id: str
    uploads_playlist_id: str
    title: str


def _client():
    try:
        from googleapiclient.discovery import build  # type: ignore
    except ImportError as exc:
        raise YouTubeAPIError(
            "google-api-python-client not installed (uv add google-api-python-client)"
        ) from exc
    if not YOUTUBE_API_KEY:
        raise YouTubeAPIError(
            "YOUTUBE_API_KEY not set in environment"
        )
    return build("youtube", "v3", developerKey=YOUTUBE_API_KEY, cache_discovery=False)


def resolve_channel(handle_or_id: str) -> Optional[ChannelInfo]:
    """Resolve an ``@handle`` or raw channel ID to ChannelInfo."""
    yt = _client()
    handle = handle_or_id.lstrip("@")
    # forHandle works for @handles; fall back to id= for raw channel IDs.
    try:
        resp = (
            yt.channels()
            .list(part="snippet,contentDetails", forHandle=handle)
            .execute()
        )
    except Exception as exc:  # noqa: BLE001
        log.warning("channels.list forHandle=%s failed: %s", handle, exc)
        resp = None

    if not resp or not resp.get("items"):
        try:
            resp = (
                yt.channels()
                .list(part="snippet,contentDetails", id=handle_or_id)
                .execute()
            )
        except Exception as exc:  # noqa: BLE001
            log.warning("channels.list id=%s failed: %s", handle_or_id, exc)
            return None

    items = (resp or {}).get("items") or []
    if not items:
        return None
    item = items[0]
    return ChannelInfo(
        channel_id=item["id"],
        uploads_playlist_id=item["contentDetails"]["relatedPlaylists"]["uploads"],
        title=item["snippet"]["title"],
    )


def fetch_uploads(uploads_playlist_id: str, *, max_videos: int = 200) -> list[dict]:
    """Page through uploads playlist; return up to ``max_videos`` videos with metadata."""
    yt = _client()
    out: list[dict] = []
    page_token: Optional[str] = None
    while len(out) < max_videos:
        page_size = min(50, max_videos - len(out))
        resp = (
            yt.playlistItems()
            .list(
                part="snippet,contentDetails",
                playlistId=uploads_playlist_id,
                maxResults=page_size,
                pageToken=page_token,
            )
            .execute()
        )
        for item in resp.get("items", []):
            snippet = item.get("snippet", {})
            content = item.get("contentDetails", {})
            video_id = content.get("videoId") or snippet.get("resourceId", {}).get("videoId")
            if not video_id:
                continue
            out.append(
                {
                    "video_id": video_id,
                    "title": snippet.get("title", ""),
                    "description": snippet.get("description", ""),
                    "upload_date": (
                        content.get("videoPublishedAt") or snippet.get("publishedAt") or ""
                    ),
                    "view_count": 0,  # filled in by enrich_view_counts
                }
            )
        page_token = resp.get("nextPageToken")
        if not page_token:
            break
    return out


def enrich_view_counts(videos: list[dict]) -> None:
    """Mutates ``videos`` in place, filling each entry's ``view_count``."""
    if not videos:
        return
    yt = _client()
    for batch in _chunks([v["video_id"] for v in videos], 50):
        resp = (
            yt.videos()
            .list(part="statistics", id=",".join(batch))
            .execute()
        )
        stats = {item["id"]: item.get("statistics", {}) for item in resp.get("items", [])}
        for v in videos:
            s = stats.get(v["video_id"])
            if not s:
                continue
            try:
                v["view_count"] = int(s.get("viewCount") or 0)
            except (TypeError, ValueError):
                v["view_count"] = 0


def _chunks(seq: list, n: int) -> Iterable[list]:
    for i in range(0, len(seq), n):
        yield seq[i : i + n]
