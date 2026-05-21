#!/usr/bin/env python3
"""Clean up duplicate enrichment artifacts in existing piloting guides.

The early enricher didn't dedupe by (source_url, extracted_for_archetype),
so guides that were enriched twice ended up with:
  * duplicate Provenance entries pointing at the same YouTube URL
  * matchup ``advice`` strings with ``(from gameplay)`` markers twice
  * key_cards.notes with the same gameplay-derived sentence appended twice

This script removes those duplicates in place. Idempotent — running twice is
a no-op once the guides are clean.

Usage:
    python scripts/dedupe_enrichment.py              # all formats
    python scripts/dedupe_enrichment.py modern       # one format
"""

from __future__ import annotations

import argparse
import logging
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge.piloting_schema import PilotingGuide  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("dedupe_enrichment")

ROOT = pathlib.Path(__file__).resolve().parent.parent
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"


def _dedupe_text_segments(text: str, marker: str = "(from gameplay)") -> str:
    """Collapse repeated sentences ending in ``marker``.

    Splits on the marker, dedupes the segments before each marker. The first
    occurrence is kept, later occurrences of the same segment are dropped.
    """
    if marker not in text:
        return text
    # Split keeping the marker on the LEFT segment.
    pieces = text.split(marker)
    # Strip surrounding whitespace from each piece. Last piece is the trailing
    # tail (no marker).
    seen: set[str] = set()
    out_pieces: list[str] = []
    for i, p in enumerate(pieces[:-1]):
        cleaned = re.sub(r"\s+", " ", p).strip()
        if cleaned in seen:
            continue
        seen.add(cleaned)
        out_pieces.append(cleaned)
    tail = pieces[-1].strip()
    rebuilt = (" " + marker + " ").join(out_pieces)
    if out_pieces:
        rebuilt += " " + marker
    if tail:
        rebuilt = (rebuilt + " " + tail).strip()
    return re.sub(r"\s+", " ", rebuilt).strip()


def dedupe_guide(guide: PilotingGuide) -> dict:
    """Mutate ``guide`` in place. Returns a stats dict."""
    stats = {"provenance_removed": 0, "matchups_cleaned": 0, "key_cards_cleaned": 0}

    # 1. Provenance dedup by (source_url, extracted_for_archetype).
    seen_prov: set[tuple[str, str]] = set()
    kept = []
    for p in guide.provenance:
        key = (p.source_url, p.extracted_for_archetype)
        # Empty source_url + empty archetype = default_primer / non-Youtube; keep all
        if not p.source_url and not p.extracted_for_archetype:
            kept.append(p)
            continue
        if key in seen_prov:
            stats["provenance_removed"] += 1
            continue
        seen_prov.add(key)
        kept.append(p)
    guide.provenance = kept

    # 2. Matchup advice — collapse repeated "(from gameplay)" segments.
    for m in guide.matchups:
        cleaned = _dedupe_text_segments(m.advice)
        if cleaned != m.advice:
            stats["matchups_cleaned"] += 1
            m.advice = cleaned

    # 3. Key card notes — same treatment.
    for c in guide.key_cards:
        cleaned = _dedupe_text_segments(c.notes)
        if cleaned != c.notes:
            stats["key_cards_cleaned"] += 1
            c.notes = cleaned

    return stats


def process_file(path: pathlib.Path) -> bool:
    try:
        guide = PilotingGuide.model_validate_json(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        log.warning("skip %s: %s", path, exc)
        return False
    stats = dedupe_guide(guide)
    total = sum(stats.values())
    if total == 0:
        return False
    path.write_text(
        guide.model_dump_json(indent=2, exclude={"stale_flags"}) + "\n", encoding="utf-8"
    )
    log.info("cleaned %s: %s", path.relative_to(ROOT), stats)
    return True


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description="Dedupe enrichment artifacts in piloting guides.")
    p.add_argument("formats", nargs="*", help="format slugs (default: all)")
    args = p.parse_args(argv[1:])
    formats = args.formats or sorted(
        d.name for d in PILOTING_DIR.iterdir() if d.is_dir() and d.name != "generic"
    )
    cleaned = 0
    for fmt in formats:
        fdir = PILOTING_DIR / fmt
        if not fdir.is_dir():
            continue
        for path in sorted(fdir.glob("*.json")):
            if process_file(path):
                cleaned += 1
    log.info("done: %d guide(s) cleaned", cleaned)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
