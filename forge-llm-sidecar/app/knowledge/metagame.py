"""Runtime metagame loader.

Loads the pre-scraped metagame JSON files from ``metagame_data/`` — these are
produced weekly by the ``update-metagame`` GitHub Action (see
``scripts/scrape_metagame.py``) and committed to the repo. The sidecar performs
NO network scraping on the request path.

Each ``metagame_data/<slug>.json`` holds the current archetype breakdown for a
format (name, meta share %, colors, signature cards).
"""

from __future__ import annotations

import functools
import json
import logging
from pathlib import Path

log = logging.getLogger(__name__)

_DATA_DIR = Path(__file__).parent / "metagame_data"

# Forge format name (lower-cased) -> metagame slug. "constructed" is generic in
# Forge; it resolves to None here so callers fall back to format detection.
_FORMAT_MAP: dict[str, str | None] = {
    "constructed": None,
    "standard": "standard",
    "pioneer": "pioneer",
    "modern": "modern",
    "legacy": "legacy",
    "vintage": "vintage",
    "pauper": "pauper",
    "commander": "commander",
    "brawl": "standard",
    "historicbrawl": "standard",
}


def resolve_meta_format(game_format: str) -> str | None:
    """Map a Forge format name to a metagame slug, or None if ambiguous."""
    key = (game_format or "").strip().lower()
    if key in _FORMAT_MAP:
        return _FORMAT_MAP[key]
    return key if (_DATA_DIR / f"{key}.json").exists() else None


@functools.cache
def _load(slug: str) -> dict:
    path = _DATA_DIR / f"{slug}.json"
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        log.warning("metagame: failed to load %s: %s", path, exc)
        return {}


def get_metagame(slug: str | None) -> list[dict]:
    """Return the archetype breakdown for a metagame slug (``[]`` if unknown)."""
    if not slug:
        return []
    return _load(slug).get("archetypes", [])


def metagame_info(slug: str | None) -> dict:
    """Return metadata (updated_at, count, ...) for a slug — for /metagame."""
    if not slug:
        return {}
    data = _load(slug)
    return {k: v for k, v in data.items() if k != "archetypes"}


def available_slugs() -> list[str]:
    return sorted(p.stem for p in _DATA_DIR.glob("*.json")) if _DATA_DIR.exists() else []
