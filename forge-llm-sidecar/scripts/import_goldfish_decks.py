#!/usr/bin/env python3
"""Import real metagame decks from MTGGoldfish into Forge ``.dck`` files.

Runs OFFLINE relative to the sidecar request path — this is a one-shot/manual
harness builder, not used at serve time. It reuses the metagame scraper's
existing archetype->URL map (:func:`app.knowledge.scraper.archetype_links`),
follows each archetype page to its representative deck, downloads the plain-text
decklist from ``/deck/download/<id>`` and converts it to a Forge ``.dck`` for the
self-play / gauntlet harness (see ``forge-gui-desktop`` ``SelfPlayRunner``).

    python scripts/import_goldfish_decks.py modern                     # whole format
    python scripts/import_goldfish_decks.py modern --archetype "Boros Energy"
    python scripts/import_goldfish_decks.py modern --limit 8           # top N tiles
    python scripts/import_goldfish_decks.py modern --out selfplay/decks --force

Output: ``<out>/<format>/<archetype-slug>.dck`` (default out is ``selfplay/decks``).
Existing files are skipped unless ``--force`` is given. Best-effort per
archetype: a failed download is logged and skipped rather than aborting.
"""

from __future__ import annotations

import argparse
import logging
import pathlib
import sys
import time

# Allow running as a plain script (no package install needed).
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import scraper  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("import_goldfish_decks")

ROOT = pathlib.Path(__file__).resolve().parent.parent
DEFAULT_OUT = ROOT / "selfplay" / "decks"


def import_format(
    slug: str,
    *,
    out_dir: pathlib.Path,
    only: str | None,
    limit: int | None,
    force: bool,
    sleep: float,
) -> int:
    links = scraper.archetype_links(slug)
    if not links:
        log.error("no archetype links for %s (markup changed or network down?)", slug)
        return 0

    items = list(links.items())
    if only:
        want = only.lower()
        items = [(n, u) for n, u in items if want in n.lower()]
        if not items:
            log.error("no archetype matching %r in %s", only, slug)
            return 0
    if limit:
        items = items[:limit]

    dest = out_dir / slug
    dest.mkdir(parents=True, exist_ok=True)
    written = 0

    for i, (name, page_url) in enumerate(items):
        out_path = dest / f"{slugify(name)}.dck"
        if out_path.exists() and not force:
            log.info("skip %s (exists; use --force)", out_path.name)
            continue

        deck_id = scraper.archetype_deck_id(page_url)
        if not deck_id:
            log.warning("no deck id for %s (%s)", name, page_url)
            continue

        dck = scraper.fetch_deck_as_dck(deck_id, name)
        if not dck:
            log.warning("no decklist for %s (deck %s)", name, deck_id)
        else:
            out_path.write_text(dck, encoding="utf-8")
            written += 1
            log.info("wrote %s (deck %s)", out_path, deck_id)

        if sleep and i < len(items) - 1:
            time.sleep(sleep)  # be polite to the source

    log.info("%s: wrote %d/%d decks into %s", slug, written, len(items), dest)
    return written


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("format", help="MTGGoldfish format slug, e.g. modern")
    parser.add_argument("--archetype", help="only import archetypes matching this substring")
    parser.add_argument("--limit", type=int, help="cap the number of archetypes imported")
    parser.add_argument("--out", default=str(DEFAULT_OUT), help="output directory root")
    parser.add_argument("--force", action="store_true", help="overwrite existing .dck files")
    parser.add_argument("--sleep", type=float, default=2.0, help="seconds between requests")
    args = parser.parse_args(argv[1:])

    written = import_format(
        args.format,
        out_dir=pathlib.Path(args.out),
        only=args.archetype,
        limit=args.limit,
        force=args.force,
        sleep=args.sleep,
    )
    return 0 if written else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
