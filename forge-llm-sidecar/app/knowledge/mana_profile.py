"""Per-deck manabase profiles.

A mana profile is a one-time, LLM-generated analysis of a specific decklist's
mana requirements and land usage: color/pip needs over the curve, how to choose
fetch targets, default fetch crack timing, and per-land guidance including
utility lands (Otawara, Boseiju, ...). It is the deck-specific knowledge that
hand-authored archetype guides do not carry, and it stays current as the meta
turns over because it is derived from whatever decklist is actually in play.

Profiles are keyed by a hash of the (normalized, sorted) main-deck card list, so
a post-sideboard configuration produces a different hash and gets its own
profile automatically.

Two sources, mirroring the user's "Both" choice:
  * committed pre-baked profiles under ``mana_profiles/<format>/<hash>.json``
    (written by ``scripts/build_mana_profiles.py``), and
  * a runtime cache directory for lazily generated profiles (anything not
    pre-baked, e.g. ad-hoc or post-sideboard decks), persisted across restarts.

Generation never blocks the request path: :func:`get_or_schedule` returns a
cached profile if present, otherwise kicks off a background build (once per
hash) and returns ``None`` so the caller degrades to deterministic play until
the profile is ready.
"""

from __future__ import annotations

import asyncio
import hashlib
import json
import logging
import os
from pathlib import Path

from app.config import CONFIG
from app.llm_client import LLMError, generate_json
from app.schema import ManaProfile

log = logging.getLogger(__name__)

_COMMITTED_DIR = Path(__file__).parent / "mana_profiles"
_DEFAULT_CACHE_DIR = Path.home() / ".cache" / "forge-sidecar" / "mana_profiles"
_CACHE_DIR = Path(os.environ.get("MANA_PROFILE_CACHE_DIR", str(_DEFAULT_CACHE_DIR)))

_SCHEMA_VERSION = 1

# In-memory caches so a hot deck never touches disk twice.
_mem: dict[str, ManaProfile] = {}
# Hashes with a build in flight (or known-failed), so we never double-build.
_inflight: set[str] = set()

_SYSTEM_PROMPT = (
    "You are a Pro Tour-level Magic: The Gathering deckbuilder analyzing a "
    "decklist's manabase. You reason precisely about color requirements, fetch "
    "land targets, and utility lands. Always answer with a single JSON object "
    "and nothing else."
)


def _norm(name: str) -> str:
    return (name or "").strip().lower()


def deck_hash(deck_cards: list[str]) -> str:
    """Stable hash of a main-deck card list (order-independent, case-folded)."""
    norm = sorted(_norm(c) for c in deck_cards if c and c.strip())
    digest = hashlib.sha1("\n".join(norm).encode("utf-8")).hexdigest()
    return digest[:16]


def _committed_path(fmt: str, h: str) -> Path:
    return _COMMITTED_DIR / (fmt or "unknown") / f"{h}.json"


def _cache_path(fmt: str, h: str) -> Path:
    return _CACHE_DIR / (fmt or "unknown") / f"{h}.json"


def _load_from_disk(fmt: str, h: str) -> ManaProfile | None:
    for path in (_committed_path(fmt, h), _cache_path(fmt, h)):
        if not path.is_file():
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            return ManaProfile(**data)
        except (OSError, ValueError, TypeError) as exc:
            log.warning("mana_profile: could not read %s: %s", path, exc)
    return None


def _save_to_cache(profile: ManaProfile) -> None:
    path = _cache_path(profile.format, profile.deck_hash)
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(profile.model_dump(), indent=2), encoding="utf-8")
    except OSError as exc:
        log.warning("mana_profile: could not persist %s: %s", path, exc)


def save_committed(profile: ManaProfile) -> Path:
    """Persist a profile to the committed (repo) directory. Offline use only."""
    path = _committed_path(profile.format, profile.deck_hash)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(profile.model_dump(), indent=2), encoding="utf-8")
    return path


def _distinct_with_counts(deck_cards: list[str]) -> list[tuple[str, int]]:
    counts: dict[str, int] = {}
    display: dict[str, str] = {}
    for c in deck_cards:
        if not c or not c.strip():
            continue
        k = _norm(c)
        counts[k] = counts.get(k, 0) + 1
        display.setdefault(k, c.strip())
    return [(display[k], counts[k]) for k in counts]


def has_analyzable_manabase(deck_cards: list[str]) -> bool:
    """Cheap pre-filter: only spend an LLM call on decks whose lands have real
    decisions — fetches, duals/shocks, or utility/man-lands. A pile of basics
    needs no profile (the engine's stock land logic is already fine)."""
    fetch_tells = ("fetch", "search your library")
    util_tells = (
        "tarn",
        "delta",
        "mire",
        "foothills",
        "strand",
        "mesa",
        "heath",
        "catacombs",
        "rainforest",
        "flats",
        "fountain",
        "garden",
        "ground",
        "temple",
        "otawara",
        "boseiju",
        "eiganjo",
        "takenuma",
        "sokenzan",
        "den of the bugbear",
        "mutavault",
        "creeping tar pit",
        "celestial colonnade",
        "raceway",
        "verge",
        "shock",
        "horizon",
    )
    for name in deck_cards:
        n = _norm(name)
        if any(t in n for t in fetch_tells) or any(t in n for t in util_tells):
            return True
    return False


def _build_prompt(deck_cards: list[str], archetype: str, fmt: str) -> str:
    distinct = _distinct_with_counts(deck_cards)
    deck_lines = "\n".join(f"  {n} x{c}" for n, c in sorted(distinct, key=lambda x: x[0]))
    return (
        f"Format: {fmt or 'unknown'}\n"
        f"Archetype (best guess): {archetype or 'Unknown'}\n\n"
        f"Decklist (main deck, with counts):\n{deck_lines}\n\n"
        "Analyze ONLY this deck's manabase. Determine:\n"
        "1. Its color identity and how demanding each color is over the curve "
        "(which colors are needed by which turn, double-pip requirements).\n"
        "2. For each LAND in the deck, how it should be used: its role (fetch / "
        "dual / shock / basic / fast / utility / creature_land / other), the "
        "colors it makes, whether it enters tapped, and timing guidance.\n"
        "3. For fetchlands, the priority order for choosing what to fetch "
        "(which colors/duals/basics matter, and whether to bias toward basics "
        "to play around Wasteland/Blood Moon).\n"
        "4. A default fetch crack timing: 'now' (crack as soon as you can use "
        "the mana), 'end_of_turn' (hold to deny information / enable a shuffle "
        "after Brainstorm/Top), or 'hold'.\n"
        "5. Utility-land policy (e.g. when to play Otawara/Boseiju as a land vs "
        "hold to Channel; sequencing between colorless utility lands).\n\n"
        "Only include real lands from the decklist in the 'lands' array. "
        "Respond with exactly these keys:\n"
        '  "primary_colors": [string],  (W/U/B/R/G/C)\n'
        '  "color_requirements": string,\n'
        '  "fetch_priority": string,\n'
        '  "crack_timing_default": "now" | "end_of_turn" | "hold",\n'
        '  "lands": [{"card": string, "role": string, "colors": [string], '
        '"play_timing": string, "enters_tapped": boolean, "notes": string}],\n'
        '  "utility_land_notes": string,\n'
        '  "reasoning": string'
    )


def _parse(raw: dict, h: str, archetype: str, fmt: str, model: str) -> ManaProfile:
    lands_raw = raw.get("lands") if isinstance(raw.get("lands"), list) else []
    lands = []
    for entry in lands_raw:
        if not isinstance(entry, dict) or not str(entry.get("card", "")).strip():
            continue
        lands.append(
            {
                "card": str(entry.get("card", "")).strip(),
                "role": str(entry.get("role", "other")).strip() or "other",
                "colors": [
                    str(c).strip().upper() for c in (entry.get("colors") or []) if str(c).strip()
                ],
                "play_timing": str(entry.get("play_timing", "")).strip(),
                "enters_tapped": bool(entry.get("enters_tapped", False)),
                "notes": str(entry.get("notes", "")).strip(),
            }
        )
    timing = str(raw.get("crack_timing_default", "now")).strip().lower()
    if timing not in ("now", "end_of_turn", "hold"):
        timing = "now"
    return ManaProfile(
        deck_hash=h,
        archetype=archetype or "Unknown",
        format=fmt or "",
        primary_colors=[
            str(c).strip().upper() for c in (raw.get("primary_colors") or []) if str(c).strip()
        ],
        color_requirements=str(raw.get("color_requirements", "")).strip(),
        fetch_priority=str(raw.get("fetch_priority", "")).strip(),
        crack_timing_default=timing,
        lands=lands,
        utility_land_notes=str(raw.get("utility_land_notes", "")).strip(),
        reasoning=str(raw.get("reasoning", "")).strip(),
        generated_by=model,
        schema_version=_SCHEMA_VERSION,
    )


async def build(
    deck_cards: list[str],
    archetype: str,
    fmt: str,
    *,
    model: str | None = None,
) -> ManaProfile | None:
    """Generate (and persist) a profile via one LLM call. Fail-soft to None.

    Used directly by the offline batch script; the runtime path goes through
    :func:`get_or_schedule`, which wraps this in a non-blocking background task.
    """
    h = deck_hash(deck_cards)
    use_model = model or CONFIG.model_name
    try:
        raw = await generate_json(
            _build_prompt(deck_cards, archetype, fmt),
            system=_SYSTEM_PROMPT,
            model=use_model,
            temperature=0.2,
        )
    except LLMError as exc:
        log.warning("mana_profile: build failed for %s (%s): %s", archetype, h, exc)
        return None
    profile = _parse(raw, h, archetype, fmt, use_model)
    _mem[h] = profile
    _save_to_cache(profile)
    log.info(
        "mana_profile: built %s (%s) lands=%d colors=%s",
        archetype,
        h,
        len(profile.lands),
        "/".join(profile.primary_colors),
    )
    return profile


def get_cached(deck_cards: list[str], fmt: str) -> ManaProfile | None:
    """Return an already-available profile (memory or disk), or None."""
    h = deck_hash(deck_cards)
    if h in _mem:
        return _mem[h]
    profile = _load_from_disk(fmt, h)
    if profile is not None:
        _mem[h] = profile
    return profile


def get_or_schedule(deck_cards: list[str], archetype: str, fmt: str) -> ManaProfile | None:
    """Non-blocking accessor for the request path.

    Returns the cached profile if present. Otherwise, if the deck has a manabase
    worth analyzing, schedules a one-time background build and returns None so
    the caller falls back to deterministic play this turn. Subsequent turns pick
    up the profile once the build completes.
    """
    cached = get_cached(deck_cards, fmt)
    if cached is not None:
        return cached
    h = deck_hash(deck_cards)
    if h in _inflight:
        return None
    if not has_analyzable_manabase(deck_cards):
        _inflight.add(h)  # remember: nothing to do for this deck
        return None
    try:
        loop = asyncio.get_running_loop()
    except RuntimeError:
        return None
    _inflight.add(h)

    async def _run() -> None:
        try:
            await build(deck_cards, archetype, fmt)
        finally:
            # Leave failed/empty hashes marked so we don't hammer the model; a
            # successful build is in _mem/disk and short-circuits before here.
            pass

    loop.create_task(_run())
    return None
