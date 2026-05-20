"""Cards Realm provider.

Cards Realm publishes "Deck Tech & Sideboard Guide" articles across every
non-Commander format we care about. The article body consistently exposes
``The Decklist`` and ``Sideboard Guide`` h2 blocks with per-matchup IN/OUT
subsections — exactly what the extractor LLM needs.

URL discovery: Cards Realm articles live under ``/en/article/<slug>/<id>``.
There is no clean URL we can guess from an archetype name, so we hit the
site's own search endpoint and filter by format.
"""

from __future__ import annotations

import logging
import re
import urllib.parse
from typing import Iterable

import httpx

from app.knowledge.primers.sources.base import Candidate, Provider, slugify

log = logging.getLogger(__name__)

_SEARCH_URL = "https://cards-realm.com/en/search?search={q}"
_ARTICLE_HREF_RE = re.compile(r"/en/article/([^/?#]+)")
_FORMAT_KEYWORDS = {
    "modern": ("modern",),
    "pioneer": ("pioneer",),
    "standard": ("standard",),
    "legacy": ("legacy",),
    "vintage": ("vintage",),
    "pauper": ("pauper",),
    "historic": ("historic",),
    "premodern": ("premodern", "pre-modern"),
}


class CardsRealmProvider(Provider):
    publisher = "Cards Realm"
    supports = frozenset(_FORMAT_KEYWORDS.keys())

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        fmt_l = (fmt or "").lower()
        if fmt_l not in self.supports:
            return []
        query = f"{archetype} {fmt_l} deck tech sideboard guide"
        url = _SEARCH_URL.format(q=urllib.parse.quote_plus(query))
        try:
            with httpx.Client(
                timeout=self.timeout,
                headers={"User-Agent": self.user_agent},
                follow_redirects=True,
            ) as client:
                resp = client.get(url)
        except httpx.HTTPError as exc:
            log.debug("cards_realm: search failed: %s", exc)
            return []
        if resp.status_code != 200:
            return []
        return list(self._extract_candidates(resp.text, archetype, fmt_l))

    def _extract_candidates(
        self, html: str, archetype: str, fmt: str
    ) -> Iterable[Candidate]:
        from bs4 import BeautifulSoup

        soup = BeautifulSoup(html, "html.parser")
        seen: set[str] = set()
        arch_tokens = [t for t in slugify(archetype).split("-") if t]
        keywords = _FORMAT_KEYWORDS.get(fmt, ())
        for a in soup.find_all("a", href=True):
            href = a["href"]
            m = _ARTICLE_HREF_RE.match(href)
            if not m:
                continue
            slug = m.group(1).lower()
            if slug in seen:
                continue
            seen.add(slug)
            # require format hint AND at least one archetype token in the slug
            if not any(k in slug for k in keywords):
                continue
            if arch_tokens and not any(t in slug for t in arch_tokens):
                continue
            title = a.get_text(" ", strip=True)
            full = href if href.startswith("http") else "https://cards-realm.com" + href
            yield Candidate(url=full, title=title)
