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


def _load_previous_names(path: pathlib.Path) -> tuple[set[str], set[str]]:
    """Return ``(live_names, archived_slugs)`` from prior state.

    ``live_names`` come from the existing metagame file; ``archived_slugs``
    from the piloting/<fmt>/_archive directory.
    """
    live: set[str] = set()
    if path.exists():
        try:
            prior = json.loads(path.read_text(encoding="utf-8"))
            live = {a.get("name") for a in prior.get("archetypes", []) if a.get("name")}
        except (OSError, ValueError):
            pass
    archive_dir = (
        pathlib.Path(__file__).resolve().parent.parent
        / "app" / "knowledge" / "piloting" / path.stem / "_archive"
    )
    archived: set[str] = set()
    if archive_dir.is_dir():
        archived = {f.stem for f in archive_dir.glob("*.json")}
    return live, archived


def _slugify(name: str) -> str:
    import re
    s = (name or "").lower()
    s = re.sub(r"[‘’']", "", s)
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")


def scrape_one(slug: str) -> bool:
    try:
        archetypes = scraper.fetch_format(slug)
    except Exception as exc:  # noqa: BLE001 - best effort per format
        log.error("failed to scrape %s: %s", slug, exc)
        return False
    if not archetypes:
        log.error("no archetypes parsed for %s (markup changed?)", slug)
        return False

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUTPUT_DIR / f"{slug}.json"

    prev_live, prev_archived_slugs = _load_previous_names(out)
    new_names = {a.get("name") for a in archetypes if a.get("name")}

    payload = {
        "format": slug,
        "source": "mtggoldfish",
        "updated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "archetype_count": len(archetypes),
        "archetypes": archetypes,
    }
    out.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    log.info("wrote %s (%d archetypes)", out, len(archetypes))

    # Emit diff. Newly returning archetypes (currently archived but back in
    # the meta) are flagged separately so the builder can restore rather than
    # rebuild.
    if prev_live or prev_archived_slugs:
        diff = {
            "format": slug,
            "computed_at": payload["updated_at"],
            "new_archetypes": sorted(
                n for n in new_names
                if n not in prev_live and _slugify(n) not in prev_archived_slugs
            ),
            "returned_archetypes": sorted(
                n for n in new_names if _slugify(n) in prev_archived_slugs
            ),
            "dropped_archetypes": sorted(prev_live - new_names),
        }
        diff_path = OUTPUT_DIR / f"{slug}.diff.json"
        diff_path.write_text(
            json.dumps(diff, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
        log.info(
            "wrote %s (new=%d returned=%d dropped=%d)",
            diff_path,
            len(diff["new_archetypes"]),
            len(diff["returned_archetypes"]),
            len(diff["dropped_archetypes"]),
        )
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
