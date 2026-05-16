"""Loads and caches the curated archetype knowledge base.

There is no live metagame feed for an offline simulator, so these bundled JSON
files *are* the "current metagame" knowledge. They are keyed by Forge's
``GameType`` name (lower-cased); ``_default.json`` is the fallback.
"""
from __future__ import annotations

import functools
import json
import logging
from pathlib import Path

log = logging.getLogger(__name__)

_ARCHETYPE_DIR = Path(__file__).parent / "archetypes"
_DEFAULT_KEY = "_default"


@functools.lru_cache(maxsize=None)
def _load_file(key: str) -> list[dict]:
    path = _ARCHETYPE_DIR / f"{key}.json"
    if not path.exists():
        return []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return data.get("archetypes", [])
    except (json.JSONDecodeError, OSError) as exc:
        log.warning("Failed to load archetype file %s: %s", path, exc)
        return []


def get_archetypes(game_format: str) -> list[dict]:
    """Return the candidate archetypes for a format, falling back to default."""
    key = (game_format or "").strip().lower()
    archetypes = _load_file(key) if key else []
    if not archetypes:
        archetypes = _load_file(_DEFAULT_KEY)
    return archetypes


def available_formats() -> list[str]:
    return sorted(
        p.stem for p in _ARCHETYPE_DIR.glob("*.json") if p.stem != _DEFAULT_KEY
    )


def _norm(name: str) -> str:
    return "".join(ch for ch in (name or "").lower() if ch.isalnum())


def merge_with_curated(live: list[dict], curated: list[dict]) -> list[dict]:
    """Combine a metagame list with curated archetype details.

    The metagame list drives the candidate set and carries ``meta_share`` /
    colors / signature cards. Curated entries supply ``strategy`` and ``tells``
    (and fill colors / signature cards if the metagame entry lacks them) when
    an archetype name matches. Curated archetypes with no metagame match are
    appended so the model still has fallbacks.
    """
    curated_by_name = {_norm(a.get("name", "")): a for a in curated}
    merged: list[dict] = []
    used: set[str] = set()

    for entry in live:
        key = _norm(entry.get("name", ""))
        used.add(key)
        item: dict = dict(entry)  # name, meta_share, colors, signature_cards
        match = curated_by_name.get(key)
        if match:
            for field in ("colors", "signature_cards"):
                if not item.get(field):
                    item[field] = match.get(field, [])
            item["strategy"] = match.get("strategy", "")
            item["tells"] = match.get("tells", [])
        merged.append(item)

    for key, archetype in curated_by_name.items():
        if key not in used:
            merged.append({**archetype, "meta_share": None})

    return merged
