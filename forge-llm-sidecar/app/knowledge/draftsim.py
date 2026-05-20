"""Draftsim primer fetcher and section parser.

Draftsim publishes static-HTML deck primers under predictable URLs and uses
a consistent set of ``H2`` headings:

* ``Payoffs`` — key payoff cards
* ``Ramp Pieces`` (where applicable)
* ``Interaction``
* ``Removal``
* ``Win Condition``
* ``Mana Base``
* ``Strategy`` — game plan
* ``Tips and Interactions`` — sequencing notes
* ``Sideboard`` — with one ``H3`` per opponent archetype (matchup advice)
* ``How to Beat`` (where applicable)

This module fetches a primer page and parses these sections into a flat
``{section_name: text}`` dict the builder LLM uses to produce a piloting
guide. It is intentionally tolerant: missing sections return empty strings,
the LLM still has the rest.

This module is OFFLINE-only — used by ``scripts/build_piloting_guides.py``,
never on the request path.
"""

from __future__ import annotations

import logging
import re
from typing import Iterable

import httpx

log = logging.getLogger(__name__)

_USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(compatible; ForgeLLMSidecar/0.1; +https://github.com/Card-Forge/forge)"
)

_FORMAT_ALIASES = {
    "modern": "modern",
    "pioneer": "pioneer",
    "standard": "standard",
    "legacy": "legacy",
    "vintage": "vintage",
    "pauper": "pauper",
    "commander": "commander",
}


def _archetype_slug(name: str) -> str:
    """``Boros Energy`` -> ``boros-energy``; tolerates apostrophes, commas."""
    s = name.lower()
    s = re.sub(r"[‘’']", "", s)  # drop curly + straight apostrophes
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")


def candidate_urls(archetype: str, fmt: str) -> list[str]:
    """Possible Draftsim primer URLs for a given archetype + format.

    Draftsim uses two patterns and a few small variants per archetype, so we
    return all plausible ones; the caller fetches in order until one returns
    200 with non-trivial content.
    """
    slug = _archetype_slug(archetype)
    fmt_slug = _FORMAT_ALIASES.get((fmt or "").lower(), fmt or "modern").lower()
    return [
        f"https://draftsim.com/mtg-{slug}-{fmt_slug}-deck/",
        f"https://draftsim.com/{slug}-{fmt_slug}/",
        f"https://draftsim.com/mtg-{slug}-deck/",
        f"https://draftsim.com/{slug}/",
    ]


def fetch_primer_html(archetype: str, fmt: str, *, timeout: float = 20.0) -> tuple[str, str] | None:
    """Return ``(url, html)`` for the first URL that loads, else None."""
    for url in candidate_urls(archetype, fmt):
        try:
            with httpx.Client(
                timeout=timeout,
                headers={"User-Agent": _USER_AGENT},
                follow_redirects=True,
            ) as client:
                resp = client.get(url)
            if resp.status_code == 200 and len(resp.text) > 5000:
                log.info("draftsim: fetched %s (%d bytes)", url, len(resp.text))
                return url, resp.text
            log.debug("draftsim: %s -> %s", url, resp.status_code)
        except httpx.HTTPError as exc:
            log.debug("draftsim: %s -> %s", url, exc)
    return None


# Heading keywords we care about — matched substring-insensitively against the
# Draftsim H2 / H3 text. Each maps to a normalized section name.
_SECTION_MAP: list[tuple[tuple[str, ...], str]] = [
    (("payoff",), "payoffs"),
    (("ramp",), "ramp"),
    (("interaction",), "interaction"),
    (("removal",), "removal"),
    (("win condition", "win con"), "win_condition"),
    (("mana base", "manabase"), "mana_base"),
    (("strategy",), "strategy"),
    (("tips", "interactions"), "tips"),
    (("sideboard",), "sideboard"),
    (("how to beat", "playing against"), "how_to_beat"),
    (("mulligan",), "mulligan"),
]


def _normalize_heading(heading: str) -> str | None:
    h = heading.strip().lower()
    if not h:
        return None
    for keywords, normalized in _SECTION_MAP:
        if all(k in h for k in keywords) or any(k in h for k in keywords):
            return normalized
    return None


def parse_sections(html: str) -> dict[str, object]:
    """Parse a Draftsim primer's HTML into a section dict.

    Returns::

        {
          "intro": "first paragraph(s) under H1",
          "payoffs": "...", "ramp": "...", ...
          "matchups": [{"name": "Boros Energy", "advice": "..."}],
        }

    Sections that aren't found come back as empty strings (or empty lists).
    """
    from bs4 import BeautifulSoup

    soup = BeautifulSoup(html, "html.parser")

    # Strip noisy widgets so they don't bleed into section text.
    for sel in ("script", "style", "nav", "aside", "form", "footer", "header"):
        for el in soup.find_all(sel):
            el.decompose()

    h2s = soup.find_all("h2")
    sections: dict[str, list[str]] = {}
    matchups: list[dict] = []
    current_section: str | None = "intro"
    in_sideboard = False

    # Walk siblings of the article element.
    article = soup.find("article") or soup.body or soup
    for el in article.descendants:
        name = getattr(el, "name", None)
        if name == "h2":
            heading = el.get_text(strip=True)
            normalized = _normalize_heading(heading)
            current_section = normalized
            in_sideboard = normalized == "sideboard"
            continue
        if name == "h3" and in_sideboard:
            # Sideboard subheadings = matchup names.
            mname = el.get_text(strip=True)
            if mname and not mname.lower().startswith("what cards"):
                matchups.append({"name": mname, "advice": ""})
            continue
        if name == "p":
            text = el.get_text(" ", strip=True)
            if not text:
                continue
            if in_sideboard and matchups:
                # Attach to the latest matchup.
                matchups[-1]["advice"] = (
                    matchups[-1]["advice"] + " " + text
                ).strip()
            elif current_section:
                sections.setdefault(current_section, []).append(text)

    # Collapse paragraph lists into single strings.
    flat: dict[str, object] = {k: " ".join(v).strip() for k, v in sections.items()}
    # Trim matchups to ~600 chars each so prompts stay focused.
    for m in matchups:
        if len(m["advice"]) > 600:
            m["advice"] = m["advice"][:597] + "..."
    flat["matchups"] = matchups
    return flat


def primer_for(archetype: str, fmt: str) -> tuple[str | None, dict[str, object] | None]:
    """Convenience wrapper: returns ``(source_url, sections)`` or ``(None, None)``."""
    pair = fetch_primer_html(archetype, fmt)
    if pair is None:
        return None, None
    url, html = pair
    return url, parse_sections(html)


def primer_to_prompt_text(sections: dict[str, object]) -> str:
    """Render a parsed Draftsim primer as compact prompt context.

    Used by the builder script to give the LLM real strategy text instead of
    just the deck name + signature cards.
    """
    if not sections:
        return ""
    parts: list[str] = []

    def _emit(label: str, key: str, limit: int = 800) -> None:
        val = sections.get(key)
        if isinstance(val, str) and val:
            v = val.strip()
            if len(v) > limit:
                v = v[: limit - 3] + "..."
            parts.append(f"{label}: {v}")

    _emit("Overview", "intro", 600)
    _emit("Payoffs", "payoffs", 400)
    _emit("Ramp pieces", "ramp", 300)
    _emit("Interaction", "interaction", 300)
    _emit("Removal", "removal", 300)
    _emit("Win condition", "win_condition", 300)
    _emit("Mana base", "mana_base", 250)
    _emit("Strategy / Game plan", "strategy", 1000)
    _emit("Sequencing tips", "tips", 700)
    _emit("How to beat", "how_to_beat", 400)
    _emit("Mulligan notes", "mulligan", 300)

    matchups: Iterable[dict] = sections.get("matchups") or []  # type: ignore[assignment]
    if matchups:
        lines = ["Matchup notes (from Sideboard guide):"]
        for m in matchups:
            advice = (m.get("advice") or "").strip()
            if advice:
                lines.append(f"  vs {m.get('name', '?')}: {advice[:280]}")
        parts.append("\n".join(lines))

    return "\n\n".join(parts)
