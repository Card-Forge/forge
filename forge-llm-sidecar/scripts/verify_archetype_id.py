#!/usr/bin/env python3
"""Verify archetype identifications by LLM-reading the transcript.

Sends a cached transcript to the builder LLM and asks it to enumerate the
cards mentioned per player, then guess the archetype each player is on.
Useful for catching false positives from the signature-card-matching
identifier — especially when one archetype's signatures overlap heavily
with another's.

Usage:
    python scripts/verify_archetype_id.py modern --archetype "Azorius Control"
    python scripts/verify_archetype_id.py modern --video-id 4NygNBb7BGA
    python scripts/verify_archetype_id.py modern --limit 5
"""

from __future__ import annotations

import argparse
import json
import logging
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import builder_llm  # noqa: E402
from app.knowledge.primers.youtube import cache, transcripts  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("verify_archetype_id")

ROOT = pathlib.Path(__file__).resolve().parent.parent
CHANNELS_PATH = ROOT / "app" / "knowledge" / "primers" / "youtube" / "channels.json"
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"


_SYSTEM = (
    "You are reading an MTG match transcript and reconstructing which decks "
    "the two players were on. Auto-captions mangle card names — cross-"
    "reference against the supplied list of known archetype signature cards. "
    "Output ONLY a JSON object. Be specific: if a player's deck cannot be "
    "identified from the transcript, say 'unknown', do not guess wildly."
)


def _prompt(transcript_text: str, format: str, candidate_archetypes: list[dict]) -> str:
    sig_block = "\n".join(
        f"- {a['name']}: " + ", ".join((a.get("signature_cards") or [])[:10])
        for a in candidate_archetypes[:30]
    )
    return (
        f"Format: {format}\n\n"
        f"Known {format} archetypes and their signature cards:\n"
        f"{sig_block}\n\n"
        "Transcript (timestamped):\n"
        "---\n"
        f"{transcript_text}\n"
        "---\n\n"
        "Identify the two decks being played. List ALL distinct card names "
        "mentioned for each player (correct caption garbles using the "
        "signature list above). Pick the archetype name that best matches "
        "each player's card pool from the list above, or say 'unknown' if "
        "no listed archetype clearly fits.\n\n"
        "Return JSON EXACTLY in this shape:\n"
        "{\n"
        '  "player_a": {\n'
        '    "name": "player name from commentary",\n'
        '    "cards_mentioned": ["card1", "card2", ...],\n'
        '    "archetype_guess": "name from the list above OR \\"unknown\\"",\n'
        '    "reasoning": "1-2 sentences why"\n'
        "  },\n"
        '  "player_b": {\n'
        '    "name": "...",\n'
        '    "cards_mentioned": [...],\n'
        '    "archetype_guess": "...",\n'
        '    "reasoning": "..."\n'
        "  }\n"
        "}\n"
    )


def _load_metagame(fmt: str) -> list[dict]:
    p = METAGAME_DIR / f"{fmt}.json"
    if not p.exists():
        return []
    return json.loads(p.read_text(encoding="utf-8")).get("archetypes", [])


def _load_tournament_channels() -> list[dict]:
    payload = json.loads(CHANNELS_PATH.read_text(encoding="utf-8"))
    return [c for c in payload.get("channels", []) if c.get("kind") == "tournament"]


def _gather_candidates(fmt: str, archetype_filter: str | None, video_id_filter: str | None) -> list[dict]:
    candidates: list[dict] = []
    for ch in _load_tournament_channels():
        cid = ch.get("channel_id")
        if not cid:
            continue
        payload = cache.load_channel_videos_any(cid)
        if not payload:
            continue
        for v in payload.get("videos", []):
            if v.get("identified_format") != fmt:
                continue
            ids = v.get("identified_archetypes") or []
            if video_id_filter and v.get("video_id") != video_id_filter:
                continue
            if archetype_filter:
                names = [a.get("name", "").lower() for a in ids]
                if archetype_filter.lower() not in names:
                    continue
            candidates.append({"video": v, "channel": ch})
    return candidates


def verify_one(v: dict, ch: dict, fmt: str, metagame: list[dict]) -> dict:
    video_id = v["video_id"]
    title = v.get("title", "")
    cached = cache.load_transcript(video_id)
    if not cached:
        return {"video_id": video_id, "title": title, "error": "no cached transcript"}
    chunks = [
        transcripts.TranscriptChunk(c["start_sec"], c["end_sec"], c["text"])
        for c in cached
    ]
    rendered = transcripts.render_for_prompt(chunks, max_chars=14000)
    try:
        result = builder_llm.generate_guide_json(
            _prompt(rendered, fmt, metagame),
            system=_SYSTEM,
        )
    except builder_llm.BuilderLLMError as exc:
        return {"video_id": video_id, "title": title, "error": f"LLM: {exc}"}
    sig_ids = [a.get("name") for a in (v.get("identified_archetypes") or [])]
    return {
        "video_id": video_id,
        "title": title,
        "url": f"https://www.youtube.com/watch?v={video_id}",
        "signature_card_guess": sig_ids,
        "llm_verdict": result,
    }


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description="LLM-verify archetype identifications.")
    p.add_argument("format", help="format slug (modern, pioneer, ...)")
    p.add_argument("--archetype", help="only videos where this archetype was identified")
    p.add_argument("--video-id", help="only this video id")
    p.add_argument("--limit", type=int, default=5, help="max videos to verify (default 5)")
    args = p.parse_args(argv[1:])

    metagame = _load_metagame(args.format)
    if not metagame:
        log.error("no metagame data for %s", args.format)
        return 1
    cands = _gather_candidates(args.format, args.archetype, args.video_id)
    if not cands:
        log.error("no candidate videos found")
        return 1
    log.info("verifying up to %d of %d candidates", args.limit, len(cands))
    results = []
    for i, c in enumerate(cands[: args.limit]):
        log.info("(%d/%d) %s ...", i + 1, min(args.limit, len(cands)), c["video"].get("title", "")[:70])
        r = verify_one(c["video"], c["channel"], args.format, metagame)
        results.append(r)

    print(json.dumps(results, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
