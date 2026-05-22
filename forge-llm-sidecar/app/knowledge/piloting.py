"""Loads piloting guides and identifies the AI's own archetype.

Piloting guides live as JSON under ``piloting/<format>/<slug>.json`` with
hand-authored fallbacks under ``piloting/generic/<strategy>.json``. Rotated-
out archetypes live under ``piloting/<format>/_archive/<slug>.json`` and are
still loaded — they just get an ``out_of_meta`` stale flag attached.

Every file is validated against
:class:`~app.knowledge.piloting_schema.PilotingGuide` on load; an invalid
file is logged and skipped. The loader auto-upgrades v1 payloads in memory
(no on-disk migration) and computes :class:`StalenessFlags` against
``banlist_events.json`` at load time.

``identify_own_archetype`` matches a decklist to a known archetype by
signature-card overlap — a deterministic, no-LLM lookup the game_advisor node
runs once per game.
"""

from __future__ import annotations

import datetime as dt
import functools
import json
import logging
import re
from pathlib import Path

from pydantic import ValidationError

from app.knowledge import loader, metagame
from app.knowledge.piloting_schema import (
    PilotingGuide,
    StalenessFlags,
    StrategyType,
    upgrade_v1_payload,
)

log = logging.getLogger(__name__)

_PILOTING_DIR = Path(__file__).parent / "piloting"
_GENERIC_DIR = _PILOTING_DIR / "generic"
_BANLIST_PATH = Path(__file__).parent / "banlist_events.json"
_ARCHIVE_DIR_NAME = "_archive"
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
def _load_banlist_events() -> list[dict]:
    try:
        payload = json.loads(_BANLIST_PATH.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("piloting: failed to read banlist events: %s", exc)
        return []
    events = payload.get("events", [])
    return events if isinstance(events, list) else []


def _compute_stale_flags(guide: PilotingGuide, *, archived: bool = False) -> StalenessFlags:
    """Cross-reference guide content against the banlist event log."""
    flags = StalenessFlags(out_of_meta=archived)
    generated_at = guide.metadata.generated_at or ""
    try:
        # Accept date or full ISO timestamp.
        generated_dt = dt.datetime.fromisoformat(generated_at.replace("Z", "+00:00"))
    except ValueError:
        generated_dt = None
    fmt = (guide.format or "").lower()

    referenced_tokens = _guide_card_tokens(guide)
    banned_after: set[str] = set()
    unbanned_after: set[str] = set()
    rotation_event = False

    for event in _load_banlist_events():
        if (event.get("format") or "").lower() != fmt:
            continue
        event_date_str = event.get("date") or ""
        try:
            event_dt = dt.datetime.fromisoformat(event_date_str).replace(
                tzinfo=dt.timezone.utc
            )
        except ValueError:
            continue
        if generated_dt is not None and event_dt <= generated_dt:
            continue
        banned_after.update(event.get("banned", []))
        unbanned_after.update(event.get("unbanned", []))
        if event.get("type") == "reset" or len(event.get("banned", [])) + len(event.get("unbanned", [])) >= 3:
            rotation_event = True

    flags.banned_cards_referenced = sorted(
        card for card in banned_after if _card_referenced(card, referenced_tokens)
    )
    # Unbanned-cards-missing is informational only — guide pre-dates the unban
    # and may overlook the now-legal staple. We do NOT scan the guide; we just
    # surface the list so the runtime prompt can mention it.
    flags.unbanned_cards_missing = sorted(unbanned_after) if unbanned_after else []
    flags.format_rotation_event = rotation_event
    if generated_dt is not None:
        flags.age_days = max(
            0,
            (dt.datetime.now(dt.timezone.utc) - generated_dt.astimezone(dt.timezone.utc)).days,
        )
    return flags


def _guide_card_tokens(guide: PilotingGuide) -> set[str]:
    """Collect every card-like string referenced anywhere in the guide."""
    tokens: set[str] = set()
    for kc in guide.key_cards:
        if kc.name:
            tokens.add(kc.name.lower())
    for wc in guide.win_conditions:
        tokens.add(wc.lower())
    for m in guide.matchups:
        for w in m.watch_for:
            tokens.add(w.lower())
    for t in guide.common_threats:
        tokens.add(t.lower())
    tokens.add((guide.overview or "").lower())
    return tokens


def _card_referenced(card: str, tokens: set[str]) -> bool:
    needle = card.lower()
    for tok in tokens:
        if not tok:
            continue
        if needle in tok:
            return True
    return False


@functools.cache
def _load_guide_raw(path_str: str, archived: bool = False) -> PilotingGuide | None:
    path = Path(path_str)
    if not path.exists():
        return None
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("piloting: failed to read %s: %s", path, exc)
        return None
    upgrade_v1_payload(payload)
    try:
        guide = PilotingGuide.model_validate(payload)
    except ValidationError as exc:
        log.warning("piloting: failed to validate %s: %s", path, exc)
        return None
    guide.stale_flags = _compute_stale_flags(guide, archived=archived)
    return guide


def _load_guide(path_str: str) -> PilotingGuide | None:
    return _load_guide_raw(path_str, False)


def _load_archived_guide(path_str: str) -> PilotingGuide | None:
    return _load_guide_raw(path_str, True)


def get_piloting_guide(
    archetype: str,
    game_format: str,
    strategy_type: StrategyType | str | None = None,
) -> PilotingGuide | None:
    """Return the piloting guide for an archetype.

    Fallback chain:
      ``<format>/<slug>`` -> ``<format>/_archive/<slug>`` -> ``generic/<strategy>`` -> ``generic/midrange``
    Returns ``None`` only if even the midrange generic guide is missing or
    invalid. Archived guides are still returned with their ``stale_flags
    .out_of_meta`` set to True.
    """
    fmt = (game_format or "").strip().lower()
    slug = slugify(archetype)
    if fmt and slug:
        guide = _load_guide(str(_PILOTING_DIR / fmt / f"{slug}.json"))
        if guide is not None:
            return guide
        guide = _load_archived_guide(
            str(_PILOTING_DIR / fmt / _ARCHIVE_DIR_NAME / f"{slug}.json")
        )
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


def available_guides() -> dict[str, dict]:
    """Map each piloting subdirectory to ``{"live": [...], "archive": [...]}``.

    Used by the ``/piloting`` debug endpoint. Generic guides only have a
    ``live`` list.
    """
    out: dict[str, dict] = {}
    if not _PILOTING_DIR.exists():
        return out
    for sub in sorted(p for p in _PILOTING_DIR.iterdir() if p.is_dir()):
        live = sorted(f.stem for f in sub.glob("*.json"))
        archive_dir = sub / _ARCHIVE_DIR_NAME
        archive = (
            sorted(f.stem for f in archive_dir.glob("*.json"))
            if archive_dir.is_dir()
            else []
        )
        out[sub.name] = {"live": live, "archive": archive}
    return out


def combo_profiles_version() -> str:
    """Aggregate cache-busting token over guides that define combo profiles."""
    paths: list[Path] = []
    try:
        for path in _PILOTING_DIR.rglob("*.json"):
            try:
                payload = json.loads(path.read_text(encoding="utf-8"))
            except (OSError, ValueError):
                continue
            if isinstance(payload, dict) and payload.get("combo_profile"):
                paths.append(path)
    except OSError:
        return "0:0"
    if not paths:
        return "0:0"
    newest = max(int(p.stat().st_mtime) for p in paths)
    return f"{len(paths)}:{newest}"
