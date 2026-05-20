"""Hareruya provider.

Hareruya hosts pro-authored Modern and Pioneer deck guides under
``/en/article/...`` with a ``Contents`` block followed by deck/matchup/
conclusion sections. We search the site's article listing rather than
guessing URLs.
"""

from __future__ import annotations

import logging
import re
import urllib.parse
from typing import Iterable

import httpx

from app.knowledge.primers.sources.base import Candidate, Provider, slugify

log = logging.getLogger(__name__)

_SEARCH_URL = "https://article.hareruyamtg.com/en/article/?s={q}"
_ARTICLE_HREF_RE = re.compile(r"/en/article/\d+/")
_SUPPORTED = frozenset({"modern", "pioneer", "legacy", "standard"})


class HareruyaProvider(Provider):
    publisher = "Hareruya"
    supports = _SUPPORTED

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        fmt_l = (fmt or "").lower()
        if fmt_l not in self.supports:
            return []
        query = f"{archetype} {fmt_l} deck guide"
        url = _SEARCH_URL.format(q=urllib.parse.quote_plus(query))
        try:
            with httpx.Client(
                timeout=self.timeout,
                headers={"User-Agent": self.user_agent},
                follow_redirects=True,
            ) as client:
                resp = client.get(url)
        except httpx.HTTPError as exc:
            log.debug("hareruya: search failed: %s", exc)
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
        arch_tokens = [t for t in slugify(archetype).split("-") if len(t) >= 3]
        for a in soup.find_all("a", href=True):
            href = a["href"]
            if not _ARTICLE_HREF_RE.search(href):
                continue
            full = href if href.startswith("http") else "https://article.hareruyamtg.com" + href
            if full in seen:
                continue
            seen.add(full)
            title = a.get_text(" ", strip=True).lower()
            if fmt not in title:
                continue
            if arch_tokens and not any(t in title for t in arch_tokens):
                continue
            yield Candidate(url=full, title=title)
