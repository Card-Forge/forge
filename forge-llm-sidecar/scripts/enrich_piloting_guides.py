#!/usr/bin/env python3
"""Enrich existing piloting guides with YouTube gameplay heuristics.

Walks ``app/knowledge/piloting/<fmt>/`` and for each guide:

  1. Loads the archetype's signature_cards from metagame_data/<fmt>.json
  2. Finds 2-3 recent gameplay videos via the cached channel list
  3. Pulls transcripts, runs LLM extraction
  4. Merges new sequencing tips, matchup advice, mulligan examples,
     key-card notes into the guide
  5. Writes back the augmented guide

Usage:
    python scripts/enrich_piloting_guides.py modern
    python scripts/enrich_piloting_guides.py modern --archetype "Boros Energy"
    python scripts/enrich_piloting_guides.py modern --since 2026-05-18

Requires:
    YOUTUBE_API_KEY     (only for cache builds, not enrichment itself)
    youtube-transcript-api installed (transcript fetching)
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import logging
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge.piloting_schema import PilotingGuide  # noqa: E402
from app.knowledge.primers.youtube import enricher  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("enrich_piloting_guides")

ROOT = pathlib.Path(__file__).resolve().parent.parent
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"


def _load_archetypes(fmt: str) -> list[dict]:
    p = METAGAME_DIR / f"{fmt}.json"
    if not p.exists():
        return []
    return json.loads(p.read_text(encoding="utf-8")).get("archetypes", [])


def _last_br_date(fmt: str) -> dt.date | None:
    p = ROOT / "app" / "knowledge" / "banlist_events.json"
    if not p.exists():
        return None
    events = json.loads(p.read_text(encoding="utf-8")).get("events", [])
    fmt_l = fmt.lower()
    dates = []
    for e in events:
        if (e.get("format") or "").lower() != fmt_l:
            continue
        try:
            dates.append(dt.date.fromisoformat(e["date"]))
        except (KeyError, ValueError):
            continue
    return max(dates) if dates else None


def enrich_one(fmt: str, arch: dict, *, since_override: dt.date | None = None) -> bool:
    name = arch.get("name") or ""
    if not name:
        return False
    from app.knowledge.piloting import slugify
    slug = slugify(name)
    path = PILOTING_DIR / fmt / f"{slug}.json"
    if not path.exists():
        log.warning("no guide to enrich at %s", path)
        return False
    try:
        guide = PilotingGuide.model_validate_json(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        log.error("failed to load %s: %s", path, exc)
        return False

    archetypes = _load_archetypes(fmt)
    known = [a.get("name", "") for a in archetypes if a.get("name")]
    since = since_override if since_override is not None else _last_br_date(fmt)

    pre_seq = len(guide.sequencing_tips)
    pre_mu = len(guide.matchups)
    pre_ex = len(guide.mulligan.examples)
    pre_prov = len(guide.provenance)

    guide = enricher.enrich(
        guide,
        archetype=name,
        fmt=fmt,
        signature_cards=arch.get("signature_cards") or [],
        known_archetypes=known,
        since=since,
    )

    added = (
        (len(guide.sequencing_tips) - pre_seq)
        + (len(guide.matchups) - pre_mu)
        + (len(guide.mulligan.examples) - pre_ex)
    )
    new_provs = len(guide.provenance) - pre_prov
    if added == 0 and new_provs == 0:
        log.info("no changes for %s/%s", fmt, name)
        return False

    path.write_text(
        guide.model_dump_json(indent=2, exclude={"stale_flags"}) + "\n", encoding="utf-8"
    )
    log.info(
        "enriched %s/%s: +%d items, +%d provenance entries",
        fmt,
        name,
        added,
        new_provs,
    )
    return True


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description="Enrich piloting guides with YouTube heuristics.")
    p.add_argument("format", help="format slug (modern, pioneer, ...)")
    p.add_argument("--archetype", help="only this archetype")
    p.add_argument("--since", help="ISO date YYYY-MM-DD — only consider videos after")
    args = p.parse_args(argv[1:])

    fmt = args.format
    archetypes = _load_archetypes(fmt)
    if args.archetype:
        archetypes = [a for a in archetypes if a.get("name") == args.archetype]
        if not archetypes:
            log.error("archetype %r not found in %s", args.archetype, fmt)
            return 1

    since_override: dt.date | None = None
    if args.since:
        try:
            since_override = dt.date.fromisoformat(args.since)
        except ValueError:
            log.error("invalid --since value %r (use YYYY-MM-DD)", args.since)
            return 1

    ok = 0
    for i, arch in enumerate(archetypes):
        try:
            if enrich_one(fmt, arch, since_override=since_override):
                ok += 1
        except Exception as exc:  # noqa: BLE001
            log.error("enrich failed for %s: %s", arch.get("name"), exc)
        if i < len(archetypes) - 1:
            time.sleep(1)
    log.info("done: %d guide(s) enriched", ok)
    return 0 if ok or not archetypes else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
