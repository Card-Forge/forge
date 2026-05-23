#!/usr/bin/env python3
"""Build strategist archetype profiles from real metagame decklists.

This is an offline pipeline. It parses Forge ``.dck`` files, writes normalized
decklist JSON, optionally refreshes a local Scryfall card cache, buckets every
listed card, updates ``app/knowledge/archetype_profiles``, and emits a coverage
report for review.

Examples:

    python scripts/build_card_buckets.py standard
    python scripts/build_card_buckets.py modern --archetype "Ruby Storm"
    python scripts/build_card_buckets.py standard --refresh-scryfall --write-profiles
"""

from __future__ import annotations

import argparse
import json
import logging
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import card_buckets  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_DECK_DIR = ROOT / "selfplay" / "decks"

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("build_card_buckets")


def _deck_paths(fmt: str, deck_root: pathlib.Path, archetype: str | None) -> list[pathlib.Path]:
    root = deck_root / fmt
    if not root.exists():
        return []
    paths = sorted(root.glob("*.dck"))
    if archetype:
        wanted = slugify(archetype)
        paths = [p for p in paths if p.stem == wanted or wanted in p.stem]
    return paths


def _build_format(
    fmt: str,
    *,
    deck_root: pathlib.Path,
    archetype: str | None,
    refresh_scryfall: bool,
    write_profiles: bool,
    write_decklists: bool,
    write_report: bool,
    dry_run: bool,
) -> int:
    paths = _deck_paths(fmt, deck_root, archetype)
    if not paths:
        log.error("no .dck files found for %s under %s", fmt, deck_root / fmt)
        return 0

    decks = [card_buckets.parse_dck(path, fmt=fmt) for path in paths]
    cache = card_buckets.load_card_cache()
    if refresh_scryfall:
        names = []
        for deck in decks:
            names.extend(deck.all_card_names())
        log.info("refreshing Scryfall metadata for %d unique requested names", len(set(names)))
        cache = card_buckets.fetch_scryfall_metadata(names, cache)
        if not dry_run:
            card_buckets.write_card_cache(cache)

    coverage_rows: list[dict] = []
    for deck in decks:
        if write_decklists:
            out = card_buckets.DECKLIST_DIR / deck.format / f"{slugify(deck.archetype)}.json"
            log.info("%s normalized decklist %s", "would write" if dry_run else "write", out)
            if not dry_run:
                card_buckets.write_decklist_json(deck)
        profile, coverage = card_buckets.build_profile_from_decklist(deck, card_cache=cache)
        coverage_rows.append(coverage)
        if write_profiles:
            out = card_buckets.PROFILE_DIR / deck.format / f"{slugify(deck.archetype)}.json"
            log.info(
                "%s profile %s (coverage %.0f%%)",
                "would write" if dry_run else "write",
                out,
                coverage["coverage"] * 100,
            )
            if not dry_run:
                card_buckets.write_profile(profile)
        else:
            log.info(
                "%s: coverage %.0f%% (%d/%d unique)",
                deck.archetype,
                coverage["coverage"] * 100,
                coverage["bucketed_unique_cards"],
                coverage["unique_cards"],
            )

    if write_report:
        path = card_buckets.REPORT_DIR / f"card_bucket_coverage_{fmt}.md"
        log.info("%s coverage report %s", "would write" if dry_run else "write", path)
        if not dry_run:
            card_buckets.write_coverage_report(coverage_rows, fmt=fmt)

    print(json.dumps({"format": fmt, "decks": len(decks), "coverage": coverage_rows}, indent=2))
    return len(decks)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("formats", nargs="+", help="format slugs, e.g. standard modern")
    parser.add_argument("--archetype", help="only this archetype; use with one format")
    parser.add_argument("--deck-root", default=str(DEFAULT_DECK_DIR), help="root containing <format>/*.dck")
    parser.add_argument("--refresh-scryfall", action="store_true", help="fetch missing card metadata into the local cache")
    parser.add_argument("--no-profiles", action="store_true", help="do not write generated archetype profiles")
    parser.add_argument("--no-decklists", action="store_true", help="do not write normalized decklist JSON")
    parser.add_argument("--no-report", action="store_true", help="do not write coverage markdown")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)

    if args.archetype and len(args.formats) != 1:
        parser.error("--archetype requires exactly one format")

    total = 0
    for fmt in args.formats:
        total += _build_format(
            fmt.strip().lower(),
            deck_root=pathlib.Path(args.deck_root),
            archetype=args.archetype,
            refresh_scryfall=args.refresh_scryfall,
            write_profiles=not args.no_profiles,
            write_decklists=not args.no_decklists,
            write_report=not args.no_report,
            dry_run=args.dry_run,
        )
    return 0 if total else 1


if __name__ == "__main__":
    raise SystemExit(main())
