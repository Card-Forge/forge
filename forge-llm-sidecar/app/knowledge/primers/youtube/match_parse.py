"""Parse Play MTG–style tournament VOD titles + descriptions.

The Play MTG channel hosts hundreds of feature matches with consistent
metadata patterns. We extract:
  * The two player names
  * The two archetype names
  * The format the match was played in
  * The tournament name (kept for provenance)

A single VOD enriches BOTH archetypes — once for each side of the match.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from typing import Optional

log = logging.getLogger(__name__)


@dataclass(slots=True)
class FeatureMatch:
    archetype_a: str
    archetype_b: str
    player_a: str = ""
    player_b: str = ""
    format: str = ""
    tournament: str = ""


# Tournament -> format mapping. Hard-coded for the major recurring events; the
# parser falls back to scanning the title/description for the format word
# when the tournament isn't recognized.
_TOURNAMENT_FORMAT: dict[str, str] = {
    "pro tour": "",  # ambiguous; let title-scan resolve
    "modern horizons": "modern",
    "world championship": "",
    "magiccon": "",
    "magic world championship": "",
    "eternal weekend": "",  # may be legacy or vintage
    "regional championship": "",
}

_FORMAT_WORDS = (
    "modern",
    "pioneer",
    "standard",
    "legacy",
    "vintage",
    "pauper",
    "historic",
    "explorer",
    "timeless",
    "premodern",
)

# Common archetype keywords we look for in title/description after "(...)" or
# "[...]". Most Play MTG descriptions use parenthetical "(Boros Energy)" form;
# some titles use "Boros Energy vs Living End".
_PARENS_ARCH_RE = re.compile(r"[\(\[]([^\)\]]{3,40})[\)\]]")
_VS_TITLE_RE = re.compile(
    r"\b([A-Z][a-zA-Z'\- /]{2,30}?)\s+vs\.?\s+([A-Z][a-zA-Z'\- /]{2,30}?)\b",
    re.IGNORECASE,
)


def detect_format(title: str, description: str) -> str:
    """Best-effort format detection from a Play MTG video's metadata."""
    haystack = (title + " " + description).lower()
    for fmt in _FORMAT_WORDS:
        if re.search(r"\b" + fmt + r"\b", haystack):
            return fmt
    for tour, fmt in _TOURNAMENT_FORMAT.items():
        if tour in haystack and fmt:
            return fmt
    return ""


def parse_tournament_video(
    title: str,
    description: str,
    *,
    known_archetypes: list[str] | None = None,
) -> Optional[FeatureMatch]:
    """Pull a FeatureMatch out of a Play MTG video's title + description.

    Strategy:
      1. Look for parenthetical archetype names in the description first
         (most reliable: ``"Reid Duke (Boros Energy) vs Andrea Mengucci (Living End)"``).
      2. Fall back to ``<arch> vs <arch>`` in the title.
      3. Cross-reference candidate strings against ``known_archetypes`` if
         supplied (drops false positives like ``"Round 5"``).

    Returns None if we can't identify two distinct archetypes.
    """
    archetypes = _extract_archetypes(description, title, known_archetypes)
    if len(archetypes) < 2:
        return None
    arch_a, arch_b = archetypes[0], archetypes[1]
    player_a, player_b = _extract_players(description)
    fmt = detect_format(title, description)
    tournament = _extract_tournament(title, description)
    return FeatureMatch(
        archetype_a=arch_a,
        archetype_b=arch_b,
        player_a=player_a,
        player_b=player_b,
        format=fmt,
        tournament=tournament,
    )


def _extract_archetypes(
    description: str,
    title: str,
    known: list[str] | None,
) -> list[str]:
    candidates: list[str] = []
    # 1. parens/brackets
    for m in _PARENS_ARCH_RE.finditer(description + "\n" + title):
        text = m.group(1).strip()
        if _looks_like_archetype(text):
            candidates.append(text)
    # 2. "X vs Y" pattern in title
    if len(candidates) < 2:
        m = _VS_TITLE_RE.search(title)
        if m:
            for grp in (m.group(1), m.group(2)):
                grp = grp.strip()
                if _looks_like_archetype(grp):
                    candidates.append(grp)
    # 3. cross-reference against known archetypes if supplied
    if known:
        norm_known = {n.lower(): n for n in known}
        # Promote any candidate that exactly matches a known archetype.
        promoted = [norm_known[c.lower()] for c in candidates if c.lower() in norm_known]
        # Also scan the raw text for any known archetype names that we missed.
        haystack = description + "\n" + title
        for n_low, n in norm_known.items():
            if re.search(r"\b" + re.escape(n_low) + r"\b", haystack.lower()):
                if n not in promoted:
                    promoted.append(n)
        candidates = promoted + [c for c in candidates if c.lower() not in norm_known]
    # dedupe preserving order
    out: list[str] = []
    seen: set[str] = set()
    for c in candidates:
        key = c.lower()
        if key in seen:
            continue
        seen.add(key)
        out.append(c)
    return out


_FILTER_PHRASES = (
    "round",
    "match",
    "feature",
    "game ",
    "vs",
    "deck tech",
    "highlights",
    "subscribe",
    "live",
    "day ",
    "top 8",
    "top 4",
    "final",
    "semi",
    "quarter",
)


def _looks_like_archetype(text: str) -> bool:
    t = text.strip().lower()
    if len(t) < 3 or len(t) > 45:
        return False
    if any(t.startswith(p) or t == p.strip() for p in _FILTER_PHRASES):
        return False
    # archetype names are usually 1-4 words
    words = t.split()
    if not (1 <= len(words) <= 5):
        return False
    return True


def _extract_players(description: str) -> tuple[str, str]:
    # A name token: starts with uppercase, 1-3 words. Case-sensitive for the
    # NAME portion (so "match." won't capture as a name) but the connector
    # words are case-insensitive.
    name = r"(?:[A-Z][a-zA-Z'\-\.]{1,20}(?:\s+[A-Z][a-zA-Z'\-\.]{1,20}){0,2})"
    connectors = r"(?:[vV][sS]\.?|[Vv]ersus|[Ff]aces?\s+off\s+against|[Pp]lays?|[Tt]akes?\s+on|[Bb]attles?|[Mm]eets?)"
    pattern = re.compile(
        r"\b(" + name + r")\s*[\(\[]([^\)\]]+)[\)\]]\s*"
        + connectors
        + r"\s*(" + name + r")\s*[\(\[]"
    )
    m = pattern.search(description)
    if m:
        return m.group(1).strip(), m.group(3).strip()
    return "", ""


_TOURNAMENT_PATTERN = re.compile(
    r"(Pro Tour [^\|\n]+|World Championship[^\|\n]*|MagicCon[^\|\n]*|Regional Championship[^\|\n]*|Eternal Weekend[^\|\n]*)"
)


def _extract_tournament(title: str, description: str) -> str:
    m = _TOURNAMENT_PATTERN.search(title) or _TOURNAMENT_PATTERN.search(description)
    if m:
        return m.group(1).strip(" -|")
    return ""
