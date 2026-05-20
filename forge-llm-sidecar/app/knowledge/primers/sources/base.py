"""Base classes for primer source providers.

A :class:`Provider` resolves an ``(archetype, format)`` pair to one or more
candidate URLs, fetches the page, cleans the HTML to plain text, and returns a
:class:`RawPrimer` for the orchestrator. The orchestrator then hands the
cleaned text to the builder LLM for structured extraction — providers do NOT
parse strategy fields themselves.
"""

from __future__ import annotations

import abc
import dataclasses
import datetime as dt
import logging
from typing import Optional

import httpx

log = logging.getLogger(__name__)

DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(compatible; ForgeLLMSidecar/0.2; +https://github.com/Card-Forge/forge)"
)


@dataclasses.dataclass(slots=True)
class Candidate:
    """A potential primer URL for an archetype."""

    url: str
    title: str = ""
    publish_date: str = ""
    author: str = ""


@dataclasses.dataclass(slots=True)
class RawPrimer:
    """A fetched primer ready for LLM extraction."""

    cleaned_text: str
    candidate: Candidate
    publisher: str
    fetched_at: str
    http_status: int

    def is_usable(self, min_chars: int = 600) -> bool:
        return bool(self.cleaned_text) and len(self.cleaned_text) >= min_chars


class Provider(abc.ABC):
    """A primer source plugin."""

    #: human-readable publisher name (goes into Provenance.publisher)
    publisher: str = ""
    #: formats this provider can sensibly cover
    supports: frozenset[str] = frozenset()

    def __init__(self, *, timeout: float = 20.0, user_agent: str = DEFAULT_USER_AGENT) -> None:
        self.timeout = timeout
        self.user_agent = user_agent

    @abc.abstractmethod
    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        """Return plausible primer URLs for the archetype, best-first."""

    def fetch(self, candidate: Candidate) -> Optional[RawPrimer]:
        """Fetch the candidate URL and return a RawPrimer or None.

        Subclasses can override to do custom cleaning, but the default
        implementation handles the common case: GET, run through
        :func:`clean_for_extraction`, return.
        """
        from app.knowledge.primers.clean_html import clean_for_extraction

        try:
            with httpx.Client(
                timeout=self.timeout,
                headers={"User-Agent": self.user_agent},
                follow_redirects=True,
            ) as client:
                resp = client.get(candidate.url)
        except httpx.HTTPError as exc:
            log.debug("%s: %s -> %s", self.publisher, candidate.url, exc)
            return None
        if resp.status_code != 200 or len(resp.text) < 2000:
            log.debug(
                "%s: %s -> status=%s len=%d (skipped)",
                self.publisher,
                candidate.url,
                resp.status_code,
                len(resp.text),
            )
            return None
        cleaned = clean_for_extraction(resp.text)
        return RawPrimer(
            cleaned_text=cleaned,
            candidate=candidate,
            publisher=self.publisher,
            fetched_at=dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
            http_status=resp.status_code,
        )


def slugify(name: str) -> str:
    """Common slug normalizer (``Boros Energy`` -> ``boros-energy``)."""
    import re

    s = (name or "").lower()
    s = re.sub(r"[‘’']", "", s)
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")
