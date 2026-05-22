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

from app.knowledge import forge_cards

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
    with httpx.Client(
        timeout=timeout, headers={"User-Agent": _USER_AGENT}, follow_redirects=True
    ) as client:
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

        results.append(
            {
                "name": name,
                "meta_share": _parse_share(tile),
                "colors": _parse_colors(tile),
                "signature_cards": _parse_signature_cards(tile),
            }
        )

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


# ---------------------------------------------------------------------------
# Decklist enrichment for the piloting-guide builder (scripts/build_piloting_guides.py).
# Best-effort and fully fail-soft: any failure returns an empty result so the
# builder degrades to signature-card context rather than aborting.
# ---------------------------------------------------------------------------
_SITE = "https://www.mtggoldfish.com"


def archetype_links(slug: str, *, timeout: float = 20.0) -> dict[str, str]:
    """Map archetype name -> MTGGoldfish archetype page URL for a format."""
    from bs4 import BeautifulSoup

    url = _URL_TEMPLATE.format(slug=slug)
    try:
        with httpx.Client(
            timeout=timeout, headers={"User-Agent": _USER_AGENT}, follow_redirects=True
        ) as client:
            resp = client.get(url)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        log.warning("scraper: archetype_links(%s) failed: %s", slug, exc)
        return {}

    links: dict[str, str] = {}
    soup = BeautifulSoup(resp.text, "html.parser")
    for tile in soup.select(".archetype-tile"):
        a = tile.select_one(".archetype-tile-title a")
        href = a.get("href") if a else None
        if a and href:
            links[a.get_text(strip=True)] = href if href.startswith("http") else _SITE + href
    return links


def fetch_archetype_decklist(page_url: str, *, timeout: float = 20.0) -> list[str]:
    """Fetch a representative decklist (card names) from an archetype page.

    Returns ``[]`` on any failure — the builder then relies on signature cards.
    """
    from bs4 import BeautifulSoup

    try:
        with httpx.Client(
            timeout=timeout, headers={"User-Agent": _USER_AGENT}, follow_redirects=True
        ) as client:
            resp = client.get(page_url)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        log.warning("scraper: fetch_archetype_decklist(%s) failed: %s", page_url, exc)
        return []

    soup = BeautifulSoup(resp.text, "html.parser")
    cards: list[str] = []
    seen: set[str] = set()
    for el in soup.select(".deck-col-card a, table.deck-view-deck-table a"):
        name = el.get_text(strip=True)
        if name and name.lower() not in seen:
            seen.add(name.lower())
            cards.append(name)
    return cards


def fetch_primer_text(page_url: str) -> str:
    """Placeholder hook for primer prose.

    MTGGoldfish archetype pages do not carry structured primers and free-text
    primer articles have patchy, unreliable coverage, so this returns an empty
    string today. The builder treats primer text as optional enrichment; wire a
    real source in here later without changing the builder.
    """
    return ""


# ---------------------------------------------------------------------------
# MTGGoldfish -> Forge .dck pipeline (scripts/import_goldfish_decks.py).
#
# An archetype page links to a representative tournament deck; that deck has a
# numeric id and a /deck/download/<id> endpoint that returns a plain-text
# decklist. We parse that text and emit a Forge .dck so the self-play / gauntlet
# harness can pilot real metagame decks instead of stand-ins. Network helpers
# are best-effort and fail soft (return None/empty); the pure parse + convert
# helpers are offline and unit-tested.
# ---------------------------------------------------------------------------
_DOWNLOAD_TEMPLATE = _SITE + "/deck/download/{deck_id}"
_DECK_ID_RE = re.compile(r"/deck/(?:download|arena_download)/(\d+)")
_DECK_PAGE_ID_RE = re.compile(r"/deck/(\d+)")

# A decklist line: "<qty> <name>" with optional Arena-style "(SET) 123" tail.
_CARD_LINE_RE = re.compile(r"^(\d+)\s+(.+?)(?:\s+\([A-Za-z0-9]{2,6}\)(?:\s+\S+)?)?$")
# Section headers used by MTGGoldfish's Arena-format export.
_MAIN_HEADERS = {"deck", "maindeck", "main", "commander"}
_SIDE_HEADERS = {"sideboard"}
_SKIP_HEADERS = {"companion", "maybeboard", "about"}


def archetype_deck_id(page_url: str, *, timeout: float = 20.0) -> str | None:
    """Return the representative deck id linked from an archetype page, or None."""
    try:
        with httpx.Client(
            timeout=timeout, headers={"User-Agent": _USER_AGENT}, follow_redirects=True
        ) as client:
            resp = client.get(page_url)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        log.warning("scraper: archetype_deck_id(%s) failed: %s", page_url, exc)
        return None

    html = resp.text
    # Prefer an explicit download link; fall back to the canonical deck link.
    match = _DECK_ID_RE.search(html) or _DECK_PAGE_ID_RE.search(html)
    return match.group(1) if match else None


def fetch_deck_download(deck_id: str, *, timeout: float = 20.0) -> str:
    """Fetch the raw plain-text decklist for a deck id. Returns "" on failure."""
    url = _DOWNLOAD_TEMPLATE.format(deck_id=deck_id)
    try:
        with httpx.Client(
            timeout=timeout, headers={"User-Agent": _USER_AGENT}, follow_redirects=True
        ) as client:
            resp = client.get(url)
            resp.raise_for_status()
    except httpx.HTTPError as exc:
        log.warning("scraper: fetch_deck_download(%s) failed: %s", deck_id, exc)
        return ""
    return resp.text


def parse_goldfish_decklist(text: str) -> tuple[list[tuple[int, str]], list[tuple[int, str]]]:
    """Parse a downloaded decklist into ``(main, sideboard)`` ``(qty, name)`` lists.

    Handles both MTGGoldfish export shapes:

    * plain ``.txt`` — main deck, a blank line, then the sideboard;
    * Arena format — ``Deck`` / ``Sideboard`` header lines and ``(SET) 123``
      printing tails (the set codes are dropped so Forge picks a printing).
    """
    main: list[tuple[int, str]] = []
    side: list[tuple[int, str]] = []
    section = main
    saw_header = False
    blank_split = False  # plain format: first blank line moves us to the sideboard

    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            if not saw_header and not blank_split and main:
                section = side
                blank_split = True
            continue

        header = line.lower()
        if header in _MAIN_HEADERS:
            section, saw_header = main, True
            continue
        if header in _SIDE_HEADERS:
            section, saw_header = side, True
            continue
        if header in _SKIP_HEADERS:
            section, saw_header = None, True  # ignore companion/maybeboard cards
            continue

        m = _CARD_LINE_RE.match(line)
        if not m or section is None:
            continue
        section.append((int(m.group(1)), m.group(2).strip()))

    return main, side


def _resolve_cards(cards: list[tuple[int, str]]) -> tuple[list[tuple[int, str]], list[str]]:
    """Map scraped names to canonical Forge names; return ``(resolved, unresolved)``.

    Unmatched names are kept verbatim (Forge still tries them) and reported so the
    importer can flag cards its database doesn't recognize on the first pass.
    """
    resolved: list[tuple[int, str]] = []
    unresolved: list[str] = []
    for qty, card in cards:
        canonical, matched = forge_cards.resolve_or_keep(card)
        if not matched:
            unresolved.append(card)
        resolved.append((qty, canonical))
    return resolved, unresolved


def decklist_to_dck(
    name: str,
    main: list[tuple[int, str]],
    side: list[tuple[int, str]],
    *,
    resolve_names: bool = True,
) -> str:
    """Render parsed ``(qty, name)`` lists as Forge ``.dck`` text.

    Card names are emitted bare (no ``|SET``) so Forge resolves a default
    printing, which is the most robust mapping from MTGGoldfish names. With
    ``resolve_names`` (default), each name is first mapped to the exact string
    Forge's database uses — e.g. a Room card ``Roaring Furnace/Steaming Sauna``
    becomes ``Roaring Furnace // Steaming Sauna`` so Forge stops dropping it.
    Names Forge doesn't recognize are kept as-is and logged.
    """
    if resolve_names:
        main, main_unresolved = _resolve_cards(main)
        side, side_unresolved = _resolve_cards(side)
        unresolved = main_unresolved + side_unresolved
        if unresolved:
            log.warning(
                "scraper: %d card name(s) not found in Forge DB for %r (kept as-is): %s",
                len(unresolved),
                name,
                ", ".join(sorted(set(unresolved))),
            )
    lines = ["[metadata]", f"Name={name}", "[Main]"]
    lines += [f"{qty} {card}" for qty, card in main]
    if side:
        lines.append("[Sideboard]")
        lines += [f"{qty} {card}" for qty, card in side]
    return "\n".join(lines) + "\n"


def fetch_deck_as_dck(deck_id: str, name: str, *, timeout: float = 20.0) -> str | None:
    """Download deck ``deck_id`` and return Forge ``.dck`` text, or None if empty."""
    text = fetch_deck_download(deck_id, timeout=timeout)
    if not text.strip():
        return None
    main, side = parse_goldfish_decklist(text)
    if not main:
        log.warning("scraper: deck %s parsed to an empty main deck", deck_id)
        return None
    return decklist_to_dck(name, main, side)
