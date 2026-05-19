"""Loads piloting guides and identifies the AI's own archetype.

Piloting guides live as JSON under ``piloting/<format>/<slug>.json`` with
hand-authored fallbacks under ``piloting/generic/<strategy>.json``. Every file
is validated against :class:`~app.knowledge.piloting_schema.PilotingGuide` on
load; an invalid file is logged and skipped (fail-soft, like the metagame and
archetype loaders).

``identify_own_archetype`` matches a decklist to a known archetype by
signature-card overlap — a deterministic, no-LLM lookup the game_advisor node
runs once per game.
"""

from __future__ import annotations

import functools
import logging
from pathlib import Path

from pydantic import ValidationError

from app.knowledge import loader, metagame
from app.knowledge.piloting_schema import PilotingGuide, StrategyType

log = logging.getLogger(__name__)

_PILOTING_DIR = Path(__file__).parent / "piloting"
_GENERIC_DIR = _PILOTING_DIR / "generic"
_FALLBACK_STRATEGY = StrategyType.MIDRANGE


def slugify(name: str) -> str:
    """Turn an archetype name into a filename slug (``Boros Energy`` -> ``boros-energy``)."""
    out: list[str] = []
    prev_dash = True  # avoids a leading dash
    for ch in (name or "").lower():
        if ch.isalnum():
            out.append(ch)
            prev_dash = False
        elif not prev_dash:
            out.append("-")
            prev_dash = True
    return "".join(out).strip("-")


@functools.cache
def _load_guide(path_str: str) -> PilotingGuide | None:
    path = Path(path_str)
    if not path.exists():
        return None
    try:
        return PilotingGuide.model_validate_json(path.read_text(encoding="utf-8"))
    except (ValidationError, OSError, ValueError) as exc:
        log.warning("piloting: failed to load guide %s: %s", path, exc)
        return None


def get_piloting_guide(
    archetype: str,
    game_format: str,
    strategy_type: StrategyType | str | None = None,
) -> PilotingGuide | None:
    """Return the piloting guide for an archetype.

    Fallback chain: ``<format>/<archetype-slug>`` -> ``generic/<strategy_type>``
    -> ``generic/midrange``. Returns ``None`` only if even the midrange generic
    guide is missing or invalid.
    """
    fmt = (game_format or "").strip().lower()
    slug = slugify(archetype)
    if fmt and slug:
        guide = _load_guide(str(_PILOTING_DIR / fmt / f"{slug}.json"))
        if guide is not None:
            return guide

    strat = _coerce_strategy(strategy_type)
    guide = _load_guide(str(_GENERIC_DIR / f"{strat.value}.json"))
    if guide is not None:
        return guide
    if strat is not _FALLBACK_STRATEGY:
        return _load_guide(str(_GENERIC_DIR / f"{_FALLBACK_STRATEGY.value}.json"))
    return None


def _coerce_strategy(value: StrategyType | str | None) -> StrategyType:
    if isinstance(value, StrategyType):
        return value
    try:
        return StrategyType((value or "").strip().lower())
    except ValueError:
        return _FALLBACK_STRATEGY


def identify_own_archetype(
    deck_cards: list[str], game_format: str
) -> tuple[str | None, StrategyType]:
    """Best-effort match of a decklist to a known archetype.

    Scores each candidate archetype by how many of its signature cards appear in
    ``deck_cards``. Deterministic and offline — no LLM call. Returns the matched
    archetype name (or ``None`` if nothing overlaps) and a strategy type to pick
    a fallback guide.
    """
    if not deck_cards:
        return None, _FALLBACK_STRATEGY

    owned = {loader._norm(c) for c in deck_cards}
    slug = metagame.resolve_meta_format(game_format) or game_format
    curated = loader.get_archetypes(slug) or loader.get_archetypes(game_format)
    live = metagame.get_metagame(slug)
    candidates = loader.merge_with_curated(live, curated) if live else curated

    best: dict | None = None
    best_score = 0
    for arch in candidates:
        signature = arch.get("signature_cards") or []
        score = sum(1 for c in signature if loader._norm(c) in owned)
        if score > best_score or (
            score == best_score and score > 0 and _share(arch) > _share(best or {})
        ):
            best, best_score = arch, score

    if not best or best_score == 0:
        return None, _FALLBACK_STRATEGY
    return best.get("name"), _coerce_strategy(best.get("strategy_type"))


def _share(arch: dict) -> float:
    return arch.get("meta_share") or 0.0


def available_guides() -> dict[str, list[str]]:
    """Map each piloting subdirectory to its guide slugs — for the debug endpoint."""
    out: dict[str, list[str]] = {}
    if not _PILOTING_DIR.exists():
        return out
    for sub in sorted(p for p in _PILOTING_DIR.iterdir() if p.is_dir()):
        out[sub.name] = sorted(f.stem for f in sub.glob("*.json"))
    return out
