"""MTG Arena Zone provider.

Strongest source for Historic deep-dives. Their format-index pages list
recent guides as anchor tags with titles like "Abzan Verdant Ritual - Deck
Guide". We hit those index pages, find the candidate links, and fetch the
article.
"""

from __future__ import annotations

import logging
from typing import Iterable

import httpx

from app.knowledge.primers.sources.base import Candidate, Provider, slugify

log = logging.getLogger(__name__)

_FORMAT_INDEX = {
    "historic": "https://mtgazone.com/format/historic/",
    "standard": "https://mtgazone.com/format/standard/",
    "explorer": "https://mtgazone.com/format/explorer/",
    "timeless": "https://mtgazone.com/format/timeless/",
}


class MTGArenaZoneProvider(Provider):
    publisher = "MTG Arena Zone"
    supports = frozenset(_FORMAT_INDEX.keys())

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        index_url = _FORMAT_INDEX.get((fmt or "").lower())
        if not index_url:
            return []
        try:
            with httpx.Client(
                timeout=self.timeout,
                headers={"User-Agent": self.user_agent},
                follow_redirects=True,
            ) as client:
                resp = client.get(index_url)
        except httpx.HTTPError as exc:
            log.debug("mtg_arena_zone: search failed: %s", exc)
            return []
        if resp.status_code != 200:
            return []
        return list(self._extract_candidates(resp.text, archetype))

    def _extract_candidates(self, html: str, archetype: str) -> Iterable[Candidate]:
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(html, "html.parser")
        seen: set[str] = set()
        arch_tokens = [t for t in slugify(archetype).split("-") if len(t) >= 3]
        for a in soup.find_all("a", href=True):
            href = a["href"]
            if "mtgazone.com" not in href or "/format/" in href:
                continue
            if href in seen:
                continue
            seen.add(href)
            title_text = a.get_text(" ", strip=True).lower()
            if not any(kw in title_text for kw in ("guide", "deck tech", "deep dive", "primer")):
                continue
            if arch_tokens and not any(t in title_text for t in arch_tokens):
                continue
            yield Candidate(url=href, title=title_text)
