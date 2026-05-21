#!/usr/bin/env python3
"""Refresh the on-disk YouTube channel-video cache.

Pulls each channel's most recent uploads via YouTube Data API v3 and writes
them to ``cache/youtube/channels/<channel_id>.json``. Burn ~80 units/channel
on a full refresh (~800 units for the default list — well under the 10k
free tier).

Usage:
    YOUTUBE_API_KEY=... python scripts/build_youtube_cache.py
    YOUTUBE_API_KEY=... python scripts/build_youtube_cache.py --channel @PlayMTG
    python scripts/build_youtube_cache.py --resolve-only  # just fill in channel_ids

Resolve-only mode is the bootstrap step: channels.json starts with empty
``channel_id`` fields; running this with --resolve-only populates them by
hitting the API once per handle and writing back to channels.json.
"""

from __future__ import annotations

import argparse
import json
import logging
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge.primers.youtube import api_client, cache  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("build_youtube_cache")

CHANNELS_PATH = (
    pathlib.Path(__file__).resolve().parent.parent
    / "app" / "knowledge" / "primers" / "youtube" / "channels.json"
)


def _load_channels() -> dict:
    return json.loads(CHANNELS_PATH.read_text(encoding="utf-8"))


def _save_channels(payload: dict) -> None:
    CHANNELS_PATH.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )


def resolve_channel_ids(channels: list[dict], *, filter_handle: str | None = None) -> int:
    """Look up channel IDs for any channels missing one. Mutates list in place."""
    updated = 0
    for ch in channels:
        if filter_handle and ch.get("handle") != filter_handle:
            continue
        if ch.get("channel_id"):
            continue
        handle = ch.get("handle")
        if not handle:
            log.warning("channel has no handle, skipping: %s", ch.get("display_name"))
            continue
        log.info("resolving %s ...", handle)
        info = api_client.resolve_channel(handle)
        if info is None:
            log.error("could not resolve %s", handle)
            continue
        ch["channel_id"] = info.channel_id
        ch["uploads_playlist_id"] = info.uploads_playlist_id
        log.info("  -> channel_id=%s uploads=%s", info.channel_id, info.uploads_playlist_id)
        updated += 1
        time.sleep(0.5)
    return updated


def refresh_channel(ch: dict, *, max_videos: int) -> bool:
    cid = ch.get("channel_id")
    uploads = ch.get("uploads_playlist_id")
    if not cid or not uploads:
        log.warning("channel %s missing channel_id/uploads_playlist_id; run --resolve-only first", ch.get("handle"))
        return False
    log.info("refreshing %s (last %d videos)...", ch.get("display_name"), max_videos)
    videos = api_client.fetch_uploads(uploads, max_videos=max_videos)
    if not videos:
        log.warning("no videos returned for %s", ch.get("display_name"))
        return False
    api_client.enrich_view_counts(videos)
    cache.save_channel_videos(cid, videos, channel_meta=ch)
    log.info("  -> %d videos cached", len(videos))
    return True


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Refresh YouTube channel cache.")
    parser.add_argument("--channel", help="only this channel (by handle, e.g. @PlayMTG)")
    parser.add_argument("--max-videos", type=int, default=200, help="per-channel cap")
    parser.add_argument("--resolve-only", action="store_true", help="only fill in channel_ids; do not refresh caches")
    args = parser.parse_args(argv[1:])

    payload = _load_channels()
    channels = payload.get("channels", [])

    updated = resolve_channel_ids(channels, filter_handle=args.channel)
    if updated:
        _save_channels(payload)
        log.info("wrote %d new channel_id(s) to %s", updated, CHANNELS_PATH)

    if args.resolve_only:
        return 0

    ok = 0
    for ch in channels:
        if args.channel and ch.get("handle") != args.channel:
            continue
        try:
            if refresh_channel(ch, max_videos=args.max_videos):
                ok += 1
        except api_client.YouTubeAPIError as exc:
            log.error("api error: %s", exc)
            return 1
        except Exception as exc:  # noqa: BLE001
            log.error("refresh failed for %s: %s", ch.get("display_name"), exc)
    log.info("done: %d channel(s) refreshed", ok)
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
