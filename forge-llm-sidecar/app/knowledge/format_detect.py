"""Scryfall-based format detection.

Given a list of card names (ideally the AI's own complete deck, which Forge
knows exactly), determine the narrowest Constructed format in which every card
is playable. Used when Forge only reports a generic "Constructed" game type.

Fully fail-soft: any network/parse error returns ``None`` and callers fall back
to a default format. Per-card legalities are cached for the process lifetime,
so repeated cards across calls cost nothing.
"""

from __future__ import annotations

import asyncio
import logging

import httpx

from app.config import CONFIG

log = logging.getLogger(__name__)

_SCRYFALL_COLLECTION = "https://api.scryfall.com/cards/collection"
_HEADERS = {"User-Agent": "ForgeLLMSidecar/0.1", "Accept": "application/json"}
_BATCH = 75  # Scryfall collection endpoint limit

# Narrowest -> widest. A card legal in 'standard' is also legal in everything
# wider, so the first format all cards share is the best guess.
_CHAIN = ["standard", "pioneer", "modern", "legacy", "vintage"]
_PLAYABLE = {"legal", "restricted"}

# card name -> Scryfall legalities dict (empty dict = looked up, not found)
_legalities_cache: dict[str, dict] = {}


def _chunks(items: list[str], size: int):
    for i in range(0, len(items), size):
        yield items[i : i + size]


async def _fetch_legalities(names: list[str]) -> None:
    todo = [n for n in names if n not in _legalities_cache]
    if not todo:
        return
    async with httpx.AsyncClient(timeout=15.0, headers=_HEADERS) as client:
        for batch in _chunks(todo, _BATCH):
            identifiers = [{"name": n} for n in batch]
            resp = await client.post(_SCRYFALL_COLLECTION, json={"identifiers": identifiers})
            resp.raise_for_status()
            data = resp.json()
            for card in data.get("data", []):
                name = card.get("name")
                if name:
                    _legalities_cache[name] = card.get("legalities", {})
            # Record misses so we don't keep retrying them.
            for miss in data.get("not_found", []):
                name = miss.get("name")
                if name:
                    _legalities_cache.setdefault(name, {})
            await asyncio.sleep(0.1)  # be polite to Scryfall


async def infer_format(card_names: list[str]) -> str | None:
    """Infer the narrowest Constructed format for a set of cards.

    Returns a format slug, or ``None`` if detection is disabled or fails.
    """
    if not CONFIG.format_detect_enable:
        return None
    names = sorted({n.strip() for n in card_names if n and n.strip()})
    if not names:
        return None

    try:
        await _fetch_legalities(names)
    except (httpx.HTTPError, ValueError) as exc:
        log.warning("format_detect: Scryfall lookup failed: %s", exc)
        return None

    legalities = [_legalities_cache.get(n, {}) for n in names]
    resolved = [lg for lg in legalities if lg]  # ignore cards Scryfall didn't know
    if not resolved:
        return None

    for fmt in _CHAIN:
        if all(lg.get(fmt) in _PLAYABLE for lg in resolved):
            log.info("format_detect: inferred '%s' from %d cards", fmt, len(resolved))
            return fmt

    # Nothing fits the chain cleanly (odd cards / silver-border): widest guess.
    return "vintage"
