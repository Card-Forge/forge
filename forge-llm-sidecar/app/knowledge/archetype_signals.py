"""Per-archetype strategic signals (graveyard utility, wrath density, etc.).

The data lives in ``archetype_signals.json`` so it's reviewable, editable,
and could later be enriched by the offline primer pipeline (Draftsim parse →
per-archetype scores). Lookup falls back to substring patterns, then to a
neutral default.

These signals are pure data — no LLM call at lookup time.
"""

from __future__ import annotations

import functools
import json
import logging
from pathlib import Path

log = logging.getLogger(__name__)

_PATH = Path(__file__).parent / "archetype_signals.json"

_NEUTRAL: dict[str, float | bool] = {
    "graveyard_utility": 0.15,
    "counterspell_density": 0.1,
    "wrath_density": 0.1,
    "spotremoval_density": 0.3,
    "is_token_deck": False,
    "is_combo": False,
}


@functools.cache
def _load() -> dict:
    try:
        return json.loads(_PATH.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("archetype_signals: failed to load %s: %s", _PATH, exc)
        return {"by_name": {}, "by_pattern": [], "off_meta_defaults": _NEUTRAL}


def signals_for(archetype_name: str | None) -> dict:
    """Return the strategic signal dict for an archetype.

    Lookup order:
    1. Exact by_name entry.
    2. Substring patterns in name order.
    3. Off-meta defaults when name starts with "Off-meta".
    4. Neutral fallback.
    """
    if not archetype_name:
        return dict(_NEUTRAL)

    data = _load()
    by_name = data.get("by_name") or {}
    if archetype_name in by_name:
        return {**_NEUTRAL, **by_name[archetype_name]}

    # Case-insensitive exact match fallback.
    lower = archetype_name.strip().lower()
    for k, v in by_name.items():
        if k.lower() == lower:
            return {**_NEUTRAL, **v}

    # Substring patterns. Each pattern carries one or more match strings;
    # later patterns can stack on top of earlier matches.
    out = dict(_NEUTRAL)
    matched = False
    for pat in data.get("by_pattern") or []:
        match_list = pat.get("match")
        if isinstance(match_list, str):
            match_list = [match_list]
        if not match_list:
            continue
        if any(m.lower() in lower for m in match_list):
            matched = True
            for k, v in pat.items():
                if k != "match":
                    out[k] = v

    if archetype_name.lower().startswith("off-meta"):
        out.update(data.get("off_meta_defaults") or {})
        matched = True

    if not matched:
        return dict(_NEUTRAL)
    return out
