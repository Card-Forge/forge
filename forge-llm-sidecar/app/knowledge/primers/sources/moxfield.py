"""Moxfield provider.

Falls back when editorial coverage is thin. Moxfield decks may have a
"primer" tab exposed at ``moxfield.com/decks/<id>/primer``. We use their
public search API to find decks tagged with the archetype name and filter for
ones whose page advertises a primer.

Moxfield's UI is React-rendered, so the cleaned HTML body is often light;
we accept that and rely on the LLM to extract whatever prose is present.
"""

from __future__ import annotations

import logging
import urllib.parse
from typing import Iterable

import httpx

from app.knowledge.primers.sources.base import Candidate, Provider, slugify

log = logging.getLogger(__name__)

_SEARCH_URL = (
    "https://api2.moxfield.com/v2/decks/search"
    "?q={q}&pageNumber=1&pageSize=10&sortType=updated&sortDirection=Descending"
    "&board=mainboard&hasPrimer=true&showIllegal=true"
)
_FORMAT_MAP = {
    "modern": "modern",
    "pioneer": "pioneer",
    "standard": "standard",
    "legacy": "legacy",
    "vintage": "vintage",
    "pauper": "pauper",
    "historic": "historic",
    "premodern": "premodern",
}


class MoxfieldProvider(Provider):
    publisher = "Moxfield"
    supports = frozenset(_FORMAT_MAP.keys())

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        fmt_l = (fmt or "").lower()
        if fmt_l not in self.supports:
            return []
        # Moxfield search lets us filter by format token in the query.
        query = f"{archetype} {fmt_l}"
        url = _SEARCH_URL.format(q=urllib.parse.quote_plus(query))
        try:
            with httpx.Client(
                timeout=self.timeout,
                headers={
                    "User-Agent": self.user_agent,
                    "Accept": "application/json",
                },
                follow_redirects=True,
            ) as client:
                resp = client.get(url)
        except httpx.HTTPError as exc:
            log.debug("moxfield: search failed: %s", exc)
            return []
        if resp.status_code != 200:
            return []
        try:
            payload = resp.json()
        except ValueError:
            return []
        return list(self._extract_candidates(payload, archetype))

    def _extract_candidates(self, payload: dict, archetype: str) -> Iterable[Candidate]:
        decks = payload.get("data") or payload.get("decks") or []
        arch_tokens = [t for t in slugify(archetype).split("-") if t]
        for d in decks[:20]:
            public_id = d.get("publicId") or d.get("id")
            if not public_id:
                continue
            name = (d.get("name") or "").lower()
            if arch_tokens and not any(t in name for t in arch_tokens):
                continue
            url = f"https://moxfield.com/decks/{public_id}/primer"
            yield Candidate(
                url=url,
                title=d.get("name", ""),
                publish_date=d.get("lastUpdatedAtUtc", "") or d.get("createdAtUtc", ""),
                author=(d.get("createdByUser") or {}).get("userName", ""),
            )
