"""Cards Realm provider.

Cards Realm publishes "Deck Tech & Sideboard Guide" articles for nearly
every non-Commander format. Their article body uses ``<section
class='forum_comment' id='forum_comment'>`` as a wrapper (so the cleaner
must keep ``comment`` out of the blacklist).

URL discovery uses their real article-search endpoint, which accepts
``keyword``, ``game=5`` (MTG), and a ``page`` parameter:

    https://cardsrealm.com/en-us/articles/search/?keyword=<q>&game=5&page=1

Article URLs returned by the search use ``mtg.cardsrealm.com`` as the host
once you follow the redirect.
"""

from __future__ import annotations

import logging
import re
import urllib.parse
from typing import Iterable

import httpx

from app.knowledge.primers.sources.base import Candidate, Provider, slugify

log = logging.getLogger(__name__)

_SEARCH_URL = "https://cardsrealm.com/en-us/articles/search/?keyword={q}&game=5&page=1"
_ARTICLE_HREF_RE = re.compile(r"^(https?://mtg\.cardsrealm\.com)?/en-us/articles/([a-z0-9][a-z0-9-]{10,})/?$")
_SUPPORTED = frozenset({
    "modern", "pioneer", "standard", "legacy", "vintage", "pauper",
    "historic", "premodern",
})


class CardsRealmProvider(Provider):
    publisher = "Cards Realm"
    supports = _SUPPORTED

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        fmt_l = (fmt or "").lower()
        if fmt_l not in self.supports:
            return []
        # Cards Realm slugs are typically <format>-<archetype>-deck-tech-...;
        # but adding the format keyword to the search REDUCES recall (their
        # search seems phrase-like). Search by archetype only and filter to
        # the right format by slug below.
        query = archetype
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
        for a in soup.find_all("a", href=True):
            href = a["href"]
            m = _ARTICLE_HREF_RE.match(href)
            if not m:
                continue
            slug = m.group(2).lower()
            if slug in seen:
                continue
            # require format hint AND at least one archetype token in the slug
            if fmt not in slug:
                continue
            if arch_tokens and not any(t in slug for t in arch_tokens):
                continue
            # extra filter: skip pure metagame digests
            if "metagame" in slug or "banlist" in slug:
                continue
            seen.add(slug)
            full = (
                href
                if href.startswith("http")
                else "https://mtg.cardsrealm.com" + href
            )
            title = a.get_text(" ", strip=True)
            yield Candidate(url=full, title=title)
