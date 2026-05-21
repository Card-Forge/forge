"""Find candidate gameplay videos for an archetype.

Discovery is purely local — it reads pre-built channel video lists from the
on-disk cache (built by ``scripts/build_youtube_cache.py``). The YouTube
Data API is only consulted by the cache builder, not on the hot path.

For ``kind: "creator"`` channels we match the archetype name against the
video title (with format hint as a secondary filter).

For ``kind: "tournament"`` channels (Play MTG) we parse each video's title +
description for the two archetypes featured.
"""

from __future__ import annotations

import datetime as dt
import json
import logging
import pathlib
from dataclasses import dataclass, field
from typing import Optional

from app.knowledge.primers.youtube import cache
from app.knowledge.primers.youtube.match_parse import (
    FeatureMatch,
    detect_format,
    parse_tournament_video,
)

log = logging.getLogger(__name__)

_CHANNELS_PATH = pathlib.Path(__file__).parent / "channels.json"


@dataclass(slots=True)
class VideoCandidate:
    video_id: str
    title: str
    channel_id: str
    channel_name: str
    channel_kind: str
    upload_date: str  # ISO 8601
    view_count: int
    url: str
    description: str = ""
    feature_match: Optional[FeatureMatch] = None  # tournament channels only
    # which archetype side this candidate is being indexed for (tournament)
    archetype_side: str = ""
    # which archetype we ranked this for (the OPPONENT side, for the matchup advice)
    opponent_side: str = ""


def load_channels() -> list[dict]:
    if not _CHANNELS_PATH.exists():
        return []
    payload = json.loads(_CHANNELS_PATH.read_text(encoding="utf-8"))
    return payload.get("channels", [])


def find_videos(
    archetype: str,
    fmt: str,
    *,
    since: Optional[dt.date] = None,
    max_results: int = 3,
    known_archetypes: list[str] | None = None,
) -> list[VideoCandidate]:
    """Return ranked VideoCandidates for an archetype.

    Filters by:
      * channel covers ``fmt``
      * video upload_date >= ``since`` (if provided)
      * title/description matches ``archetype`` (creator) or feature-match
        metadata names ``archetype`` (tournament)

    Ranks by (channel_priority asc, view_count desc, upload_date desc).
    """
    out: list[tuple[int, int, str, VideoCandidate]] = []
    arch_norm = archetype.lower()
    arch_tokens = [t for t in arch_norm.replace("'", "").replace("/", " ").split() if len(t) >= 3]
    for ch in load_channels():
        if not ch.get("channel_id"):
            continue
        if fmt not in ch.get("formats", []):
            continue
        payload = cache.load_channel_videos_any(ch["channel_id"])
        if not payload:
            log.debug("youtube: no cache for %s", ch.get("display_name"))
            continue
        for v in payload.get("videos", []):
            if not _upload_date_ok(v.get("upload_date"), since):
                continue
            if ch.get("kind") == "tournament":
                cand = _match_tournament(v, ch, archetype, fmt, known_archetypes)
            else:
                cand = _match_creator(v, ch, archetype, fmt, arch_tokens)
            if cand is None:
                continue
            sort_key = (
                int(ch.get("priority", 99)),
                -int(v.get("view_count") or 0),
                v.get("upload_date") or "",
            )
            out.append((sort_key[0], sort_key[1], sort_key[2], cand))

    out.sort(key=lambda t: (t[0], t[1], t[2]), reverse=False)
    # Note: view_count is negated so larger views sort first; date is string-
    # comparable (ISO 8601). To get desc-date for the final tiebreaker, reverse:
    out.sort(key=lambda t: (t[0], t[1], -_date_to_int(t[2])))
    return [c for _, _, _, c in out[:max_results]]


def _date_to_int(d: str) -> int:
    try:
        return int(dt.date.fromisoformat(d[:10]).toordinal())
    except (ValueError, AttributeError):
        return 0


def _upload_date_ok(upload_date: str | None, since: dt.date | None) -> bool:
    if since is None:
        return True
    if not upload_date:
        return False
    try:
        d = dt.date.fromisoformat(upload_date[:10])
    except ValueError:
        return False
    return d >= since


def _match_creator(
    v: dict, ch: dict, archetype: str, fmt: str, arch_tokens: list[str]
) -> Optional[VideoCandidate]:
    title_lower = (v.get("title") or "").lower()
    desc_lower = (v.get("description") or "").lower()
    haystack = title_lower + " \n " + desc_lower
    fmt_in_meta = (
        fmt in title_lower
        or fmt in desc_lower
        or detect_format(v.get("title", ""), v.get("description", "")) == fmt
    )
    if not fmt_in_meta:
        return None
    if not arch_tokens:
        return None
    # Stricter matching:
    #   - multi-token names ("Living End", "Boros Energy"): require the FULL
    #     phrase as a substring; ANY-token matching produced false positives
    #     on words like "end" / "burn" / "blink".
    #   - single-token names ("Affinity", "Belcher"): require the token as a
    #     whole word.
    arch_lower = archetype.lower().strip()
    if len(arch_tokens) >= 2:
        # full-phrase match (case-insensitive, with apostrophes normalized)
        phrase = arch_lower.replace("'", "")
        title_clean = title_lower.replace("'", "")
        desc_clean = desc_lower.replace("'", "")
        if phrase not in title_clean and phrase not in desc_clean:
            return None
    else:
        import re as _re

        if not _re.search(r"\b" + _re.escape(arch_tokens[0]) + r"\b", haystack):
            return None
    return VideoCandidate(
        video_id=v["video_id"],
        title=v.get("title", ""),
        channel_id=ch["channel_id"],
        channel_name=ch.get("display_name", ""),
        channel_kind=ch.get("kind", "creator"),
        upload_date=v.get("upload_date", ""),
        view_count=int(v.get("view_count") or 0),
        url=f"https://www.youtube.com/watch?v={v['video_id']}",
        description=v.get("description", ""),
        archetype_side=archetype,
    )


def _match_tournament(
    v: dict,
    ch: dict,
    archetype: str,
    fmt: str,
    known_archetypes: list[str] | None,
) -> Optional[VideoCandidate]:
    arch_low = archetype.lower()

    # Path A: title/description-based parsing (works when archetypes are
    # explicitly named in metadata — uncommon on Play MTG).
    match = parse_tournament_video(
        v.get("title", ""), v.get("description", ""), known_archetypes=known_archetypes
    )
    if match is not None:
        if match.format and match.format != fmt:
            return None
        sides = {
            match.archetype_a.lower(): match.archetype_a,
            match.archetype_b.lower(): match.archetype_b,
        }
        if arch_low in sides:
            opponent = (
                match.archetype_b
                if arch_low == match.archetype_a.lower()
                else match.archetype_a
            )
            return _build_tournament_candidate(
                v, ch, sides[arch_low], opponent, feature_match=match
            )

    # Path B: pre-identified archetypes from transcript signature-card
    # matching (see scripts/identify_tournament_archetypes.py).
    identified = v.get("identified_archetypes") or []
    if identified:
        # Honour the format the identifier used.
        if v.get("identified_format") and v["identified_format"] != fmt:
            return None
        # Match by case-insensitive name.
        names = {(a.get("name") or "").lower(): a.get("name") for a in identified if a.get("name")}
        if arch_low not in names:
            return None
        # The opponent is the OTHER highest-confidence archetype in the list.
        # If there's only one identified archetype, treat opponent as "(unknown)".
        opponent_name = ""
        for a in identified:
            n = a.get("name") or ""
            if n and n.lower() != arch_low:
                opponent_name = n
                break
        return _build_tournament_candidate(
            v, ch, names[arch_low], opponent_name, feature_match=None
        )

    return None


def _build_tournament_candidate(
    v: dict, ch: dict, side: str, opponent: str, *, feature_match
) -> VideoCandidate:
    return VideoCandidate(
        video_id=v["video_id"],
        title=v.get("title", ""),
        channel_id=ch["channel_id"],
        channel_name=ch.get("display_name", ""),
        channel_kind=ch.get("kind", "tournament"),
        upload_date=v.get("upload_date", ""),
        view_count=int(v.get("view_count") or 0),
        url=f"https://www.youtube.com/watch?v={v['video_id']}",
        description=v.get("description", ""),
        feature_match=feature_match,
        archetype_side=side,
        opponent_side=opponent,
    )
