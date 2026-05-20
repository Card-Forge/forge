"""Draftsim provider.

Wraps the legacy :mod:`app.knowledge.draftsim` URL patterns. Used as a
last-resort source because Draftsim's templates are inconsistent across
archetypes — by routing through the orchestrator's cleaned-HTML +
LLM-extraction path we sidestep the heading-based parser that was the
original failure mode.
"""

from __future__ import annotations

from app.knowledge.draftsim import candidate_urls as _legacy_candidate_urls
from app.knowledge.primers.sources.base import Candidate, Provider


class DraftsimProvider(Provider):
    publisher = "Draftsim"
    supports = frozenset({"modern", "pioneer", "standard", "legacy", "vintage", "pauper"})

    def search(self, archetype: str, fmt: str) -> list[Candidate]:
        fmt_l = (fmt or "").lower()
        if fmt_l not in self.supports:
            return []
        return [Candidate(url=u) for u in _legacy_candidate_urls(archetype, fmt_l)]
