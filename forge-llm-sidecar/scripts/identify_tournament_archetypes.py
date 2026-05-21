#!/usr/bin/env python3
"""Annotate cached tournament-VOD videos with identified archetypes.

For each tournament-kind channel (e.g. Play MTG), iterates through cached
videos and identifies the featured archetypes from their transcripts via
signature-card matching. Writes the result back into the channel's cache
file as ``video.identified_archetypes``.

Self-contained: no LLM, no YouTube Data API usage. Transcript fetching is
done via youtube-transcript-api (also free, also cached).

Usage:
    python scripts/identify_tournament_archetypes.py
    python scripts/identify_tournament_archetypes.py --format modern
    python scripts/identify_tournament_archetypes.py --channel @Play_MTG
    python scripts/identify_tournament_archetypes.py --reidentify  # ignore prior results
"""

from __future__ import annotations

import argparse
import json
import logging
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge.primers.youtube import cache, match_identify, transcripts  # noqa: E402
from app.knowledge.primers.youtube.match_parse import detect_format  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("identify_tournament_archetypes")

ROOT = pathlib.Path(__file__).resolve().parent.parent
CHANNELS_PATH = ROOT / "app" / "knowledge" / "primers" / "youtube" / "channels.json"
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"


def _load_metagame(fmt: str) -> list[dict]:
    p = METAGAME_DIR / f"{fmt}.json"
    if not p.exists():
        return []
    return json.loads(p.read_text(encoding="utf-8")).get("archetypes", [])


def _process_video(
    v: dict,
    metagames: dict[str, list[dict]],
    *,
    reidentify: bool,
    only_format: str | None,
    cache_only: bool = False,
    verify_with_llm: bool = False,
) -> bool:
    """Return True if we wrote new identification data for this video.

    When ``cache_only`` is True, skip the video entirely if its transcript
    isn't already cached on disk (no network fetch, no IpBlocked backoff).
    Useful for quickly re-scoring with updated identification logic.
    """
    if not reidentify and "identified_archetypes" in v:
        return False
    fmt = detect_format(v.get("title", ""), v.get("description", ""))
    if only_format and fmt != only_format:
        return False
    if not fmt or fmt not in metagames:
        v["identified_archetypes"] = []
        v["identified_format"] = fmt or ""
        return True

    if cache_only:
        from app.knowledge.primers.youtube import cache as ytcache
        cached = ytcache.load_transcript(v["video_id"])
        if not cached:
            # Transcript not in cache. Don't fetch — just skip.
            return False
        chunks = [
            transcripts.TranscriptChunk(c["start_sec"], c["end_sec"], c["text"])
            for c in cached
        ]
    else:
        chunks = transcripts.fetch_transcript(v["video_id"])
        if not chunks:
            v["identified_archetypes"] = []
            v["identified_format"] = fmt
            return True

    transcript_text = " ".join(c.text for c in chunks)
    scores = match_identify.identify_archetypes(transcript_text, metagames[fmt])

    audit: list[dict] = []
    if verify_with_llm and scores:
        rendered = transcripts.render_for_prompt(chunks, max_chars=14000)
        verdict = match_identify.verify_with_llm(rendered, metagames[fmt], fmt=fmt)
        if verdict:
            known = {a.get("name", "") for a in metagames[fmt] if a.get("name")}
            scores, audit = match_identify.reconcile_with_llm(scores, verdict, known)

    v["identified_archetypes"] = [
        {
            "name": s.name,
            "distinct_cards": s.distinct_cards,
            "total_mentions": s.total_mentions,
            "unique_distinct": s.unique_distinct,
            "confidence": s.confidence,
        }
        for s in scores
    ]
    v["identified_format"] = fmt
    if audit:
        v["llm_audit"] = audit
    return True


def process_channel(channel_id: str, channel_meta: dict, *, reidentify: bool, only_format: str | None, cache_only: bool = False, verify_with_llm: bool = False) -> int:
    payload = cache.load_channel_videos_any(channel_id)
    if not payload:
        log.warning("no cache for channel_id=%s", channel_id)
        return 0
    videos = payload.get("videos", [])
    if not videos:
        return 0

    metagames: dict[str, list[dict]] = {}
    for fmt in channel_meta.get("formats", []):
        archetypes = _load_metagame(fmt)
        if archetypes:
            metagames[fmt] = archetypes

    processed = 0
    matched = 0
    for v in videos:
        try:
            if _process_video(v, metagames, reidentify=reidentify, only_format=only_format, cache_only=cache_only, verify_with_llm=verify_with_llm):
                processed += 1
                if v.get("identified_archetypes"):
                    matched += 1
        except Exception as exc:  # noqa: BLE001
            log.warning("identify failed for %s: %s", v.get("video_id"), exc)
        if processed % 25 == 0 and processed:
            log.info(
                "%s: processed %d/%d (matched=%d)",
                channel_meta.get("display_name"),
                processed,
                len(videos),
                matched,
            )
        # Polite pacing for transcript fetches.
        if processed and processed % 5 == 0:
            time.sleep(0.2)

    # Persist updated cache.
    cache.save_channel_videos(channel_id, videos, channel_meta=channel_meta)
    log.info(
        "%s: done — %d videos processed, %d with archetypes identified",
        channel_meta.get("display_name"),
        processed,
        matched,
    )
    return matched


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description="Identify archetypes in cached tournament VODs.")
    p.add_argument("--channel", help="only this channel (by handle, e.g. @Play_MTG)")
    p.add_argument("--format", help="only videos detected as this format")
    p.add_argument("--reidentify", action="store_true", help="re-process videos that already have identified_archetypes")
    p.add_argument("--cache-only", action="store_true", help="skip videos whose transcript isn't already cached (no network fetch)")
    p.add_argument("--verify-with-llm", action="store_true", help="after signature-card identification, run an LLM verification pass that can override the result when the LLM names a different archetype that exists in our metagame")
    args = p.parse_args(argv[1:])

    payload = json.loads(CHANNELS_PATH.read_text(encoding="utf-8"))
    channels = [c for c in payload.get("channels", []) if c.get("kind") == "tournament"]
    if args.channel:
        channels = [c for c in channels if c.get("handle") == args.channel]
        if not channels:
            log.error("no tournament channel matched %s", args.channel)
            return 1

    total = 0
    for ch in channels:
        cid = ch.get("channel_id")
        if not cid:
            continue
        total += process_channel(
            cid, ch,
            reidentify=args.reidentify,
            only_format=args.format,
            cache_only=args.cache_only,
            verify_with_llm=args.verify_with_llm,
        )
    log.info("done: %d videos identified across %d channel(s)", total, len(channels))
    return 0 if total or not channels else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
