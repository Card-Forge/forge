#!/usr/bin/env python3
"""Scrape the current metagame and write JSON files into the repo.

Run weekly by the ``update-metagame`` GitHub Action; can also be run by hand:

    python scripts/scrape_metagame.py            # all formats
    python scripts/scrape_metagame.py modern     # one format

Output: ``app/knowledge/metagame_data/<format>.json``. The sidecar loads these
committed files at startup — it never scrapes at request time.
"""
from __future__ import annotations

import datetime as dt
import json
import logging
import pathlib
import sys
import time

# Allow running as a plain script (no package install needed).
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import scraper  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("scrape_metagame")

OUTPUT_DIR = pathlib.Path(__file__).resolve().parent.parent / "app" / "knowledge" / "metagame_data"


def scrape_one(slug: str) -> bool:
    try:
        archetypes = scraper.fetch_format(slug)
    except Exception as exc:  # noqa: BLE001 - best effort per format
        log.error("failed to scrape %s: %s", slug, exc)
        return False
    if not archetypes:
        log.error("no archetypes parsed for %s (markup changed?)", slug)
        return False

    payload = {
        "format": slug,
        "source": "mtggoldfish",
        "updated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "archetype_count": len(archetypes),
        "archetypes": archetypes,
    }
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUTPUT_DIR / f"{slug}.json"
    out.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    log.info("wrote %s (%d archetypes)", out, len(archetypes))
    return True


def main(argv: list[str]) -> int:
    slugs = argv[1:] or scraper.FORMAT_SLUGS
    ok = 0
    for i, slug in enumerate(slugs):
        if scrape_one(slug):
            ok += 1
        if i < len(slugs) - 1:
            time.sleep(2)  # be polite to the source
    log.info("done: %d/%d formats scraped", ok, len(slugs))
    # Succeed if at least one format scraped, so a single bad page does not
    # fail the whole Action.
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
