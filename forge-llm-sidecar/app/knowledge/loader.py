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
_PROFILE_DIR = Path(__file__).parent / "archetype_profiles"
_DEFAULT_KEY = "_default"

# The role buckets a profile may define. A single card can appear in several
# buckets (membership is many-to-many). ``interaction_density`` is a scalar
# rather than a card list and is handled separately.
PROFILE_BUCKETS: tuple[str, ...] = (
    "mana_reducers",
    "rituals",
    "card_advantage",
    "dig_draw",
    "tutors_wildcards",
    "win_conditions",
    "threats",
    "removal",
    "wrath",
    "counterspells",
    "protection",
    "discard_outlets",
    "graveyard_enablers",
    "reanimation_targets",
    "recursion",
    "engines",
    "payoff_cards",
    "combo_pieces",
    "hate_pieces",
    "mana_fixing",
    "lands",
    "planeswalker_threats",
)


@functools.cache
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
    return sorted(p.stem for p in _ARCHETYPE_DIR.glob("*.json") if p.stem != _DEFAULT_KEY)


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


# --- opponent archetype profiles --------------------------------------------
# Structured per-archetype knowledge (role buckets, combos, kill priority,
# predicted lines) consumed by the opponent_strategist node for hand inference
# and next-turn prediction. Keyed by ``<format>/<slug>.json`` to match the
# piloting-guide naming convention.


def _slugify(name: str) -> str:
    out: list[str] = []
    prev_dash = True
    for ch in (name or "").lower():
        if ch.isalnum():
            out.append(ch)
            prev_dash = False
        elif not prev_dash:
            out.append("-")
            prev_dash = True
    return "".join(out).strip("-")


@functools.cache
def _load_profile_file(fmt: str, slug: str) -> dict | None:
    path = _PROFILE_DIR / fmt / f"{slug}.json"
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError) as exc:
        log.warning("Failed to load archetype profile %s: %s", path, exc)
        return None


def load_archetype_profile(name: str, game_format: str) -> dict | None:
    """Return the structured profile for an archetype, or ``None`` if absent.

    Off-meta / unknown archetypes have no profile; callers degrade gracefully.
    """
    fmt = (game_format or "").strip().lower()
    slug = _slugify(name)
    if not fmt or not slug:
        return None
    return _load_profile_file(fmt, slug)


@functools.cache
def _reverse_index_for(fmt: str, slug: str) -> dict[str, tuple[str, ...]]:
    """Map a normalized card name -> the buckets it belongs to, for one profile."""
    profile = _load_profile_file(fmt, slug)
    if not profile:
        return {}
    index: dict[str, list[str]] = {}
    for bucket, payload in (profile.get("buckets") or {}).items():
        if bucket not in PROFILE_BUCKETS:
            continue
        cards = list(payload.get("cards") or [])
        for pair in payload.get("pairs") or []:  # combo_pieces uses pairs[]
            cards.extend(pair)
        for card in cards:
            key = _norm(card)
            if not key:
                continue
            buckets = index.setdefault(key, [])
            if bucket not in buckets:
                buckets.append(bucket)
    return {k: tuple(v) for k, v in index.items()}


def card_buckets(name: str, game_format: str, archetype: str) -> tuple[str, ...]:
    """Return the role buckets a card occupies within an archetype's profile."""
    return _reverse_index_for(
        (game_format or "").strip().lower(), _slugify(archetype)
    ).get(_norm(name), ())


def profile_version(name: str, game_format: str) -> str:
    """A cache-busting token that changes when the profile file is edited."""
    fmt = (game_format or "").strip().lower()
    slug = _slugify(name)
    path = _PROFILE_DIR / fmt / f"{slug}.json"
    try:
        return f"{slug}:{int(path.stat().st_mtime)}"
    except OSError:
        return f"{slug}:0"


def all_profiles_version() -> str:
    """Aggregate version token over every profile file (newest mtime + count).

    Included in the recognize cache key so editing any profile invalidates
    cached responses.
    """
    try:
        paths = list(_PROFILE_DIR.rglob("*.json"))
    except OSError:
        return "0:0"
    if not paths:
        return "0:0"
    newest = max(int(p.stat().st_mtime) for p in paths)
    return f"{len(paths)}:{newest}"
