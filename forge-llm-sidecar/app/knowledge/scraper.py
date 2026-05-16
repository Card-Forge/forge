"""MTGGoldfish metagame scraper.

This module performs the actual network scrape and HTML parsing. It is used by
the ``scripts/scrape_metagame.py`` CLI (run weekly by a GitHub Action) — it is
NOT imported on the request path. The sidecar serves pre-scraped JSON files at
runtime (see :mod:`app.knowledge.metagame`).

The parser (:func:`parse_metagame_page`) is isolated so it can be adjusted if
MTGGoldfish changes its markup.
"""
from __future__ import annotations

import logging
import re

import httpx

log = logging.getLogger(__name__)

# MTGGoldfish format slugs to scrape.
FORMAT_SLUGS = ["standard", "pioneer", "modern", "legacy", "vintage", "pauper", "commander"]

_URL_TEMPLATE = "https://www.mtggoldfish.com/metagame/{slug}/full"
_USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(compatible; ForgeLLMSidecar/0.1; +https://github.com/Card-Forge/forge)"
)

# mana-symbol icon class -> single-letter color code
_COLOR_ICONS = {"ms-w": "W", "ms-u": "U", "ms-b": "B", "ms-r": "R", "ms-g": "G"}


def fetch_format(slug: str, *, timeout: float = 20.0) -> list[dict]:
    """Scrape one format's metagame page. Raises httpx.HTTPError on failure."""
    url = _URL_TEMPLATE.format(slug=slug)
    with httpx.Client(timeout=timeout, headers={"User-Agent": _USER_AGENT},
                      follow_redirects=True) as client:
        resp = client.get(url)
        resp.raise_for_status()
    archetypes = parse_metagame_page(resp.text)
    log.info("scraper: %s -> %d archetypes", slug, len(archetypes))
    return archetypes


def parse_metagame_page(html: str) -> list[dict]:
    """Parse an MTGGoldfish metagame page into a list of archetype dicts.

    Each archetype: ``{name, meta_share, colors, signature_cards}``.
    Returns ``[]`` if the markup is unrecognised.
    """
    from bs4 import BeautifulSoup

    soup = BeautifulSoup(html, "html.parser")
    results: list[dict] = []
    seen: set[str] = set()

    for tile in soup.select(".archetype-tile"):
        name_el = tile.select_one(".archetype-tile-title a")
        if not name_el:
            continue
        name = name_el.get_text(strip=True)
        key = name.lower()
        if not name or key in seen:
            continue
        seen.add(key)

        results.append({
            "name": name,
            "meta_share": _parse_share(tile),
            "colors": _parse_colors(tile),
            "signature_cards": _parse_signature_cards(tile),
        })

    return results


def _parse_share(tile) -> float | None:
    el = tile.select_one(".metagame-percentage .archetype-tile-statistic-value")
    if not el:
        return None
    match = re.search(r"([\d.]+)\s*%", el.get_text())
    if not match:
        return None
    try:
        return float(match.group(1))
    except ValueError:
        return None


def _parse_colors(tile) -> list[str]:
    colors: list[str] = []
    for icon in tile.select(".manacost i"):
        for cls in icon.get("class", []):
            code = _COLOR_ICONS.get(cls)
            if code and code not in colors:
                colors.append(code)
    return colors


def _parse_signature_cards(tile) -> list[str]:
    return [li.get_text(strip=True) for li in tile.select("ul li") if li.get_text(strip=True)]
