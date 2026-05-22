"""Loads and stages self-play "lessons" — the auto-staged learnings layer.

Lessons live as JSON lists under ``learnings/<format>/<slug>.json``. They are
produced by the ``/selfplay/reflect`` endpoint (distilled from goldfishing /
mirror game logs), auto-staged here, and injected — capped and context-gated —
alongside the curated piloting guide. A human later promotes vetted lessons
into the curated guides via ``scripts/promote_learning.py``; this layer is the
machine-writable staging area kept separate so it can never silently degrade
the hand-authored guides.

Each entry is validated against :class:`~app.schema.Lesson`; invalid entries
are logged and skipped. Injection selection enforces a confidence/sample-count
threshold and a hard character budget so learnings can never bloat the prompt
(the latency budget the whole system is built around).
"""

from __future__ import annotations

import json
import logging
from pathlib import Path

from pydantic import ValidationError

from app.knowledge.piloting import slugify
from app.schema import Lesson

log = logging.getLogger(__name__)

_LEARNINGS_DIR = Path(__file__).parent / "learnings"

# Injection guardrails. A lesson must clear both thresholds to be eligible, and
# the rendered block is capped to ~500 tokens (~4 chars/token).
_MIN_CONFIDENCE = 0.5
_MIN_GAMES = 5
_MAX_INJECT_CHARS = 2000


def _store_path(game_format: str, archetype: str) -> Path:
    fmt = (game_format or "").strip().lower()
    return _LEARNINGS_DIR / fmt / f"{slugify(archetype)}.json"


def get_learnings(game_format: str, archetype: str) -> list[Lesson]:
    """Return all staged lessons for an archetype, or empty if none/invalid."""
    path = _store_path(game_format, archetype)
    if not path.exists():
        return []
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("learnings: failed to read %s: %s", path, exc)
        return []
    if not isinstance(payload, list):
        log.warning("learnings: %s is not a JSON list — skipping", path)
        return []
    out: list[Lesson] = []
    for entry in payload:
        try:
            out.append(Lesson.model_validate(entry))
        except ValidationError as exc:
            log.warning("learnings: invalid lesson in %s: %s", path, exc)
    return out


def select_for_injection(
    lessons: list[Lesson],
    *,
    observed_interaction: bool,
    max_chars: int = _MAX_INJECT_CHARS,
) -> list[Lesson]:
    """Pick the lessons eligible to inject into the current game state.

    Gating rules:
    - ``no_interaction`` lessons inject ONLY when the opponent has shown no
      interaction this game. This is the anti-poisoning gate: a goldfish-derived
      "race, don't hedge" lesson never reaches a game where interaction is live.
    - Each lesson must clear the confidence and sample-count thresholds.
    - Highest-confidence first, accumulated until the character budget is hit.
    """
    eligible = [
        lsn
        for lsn in lessons
        if lsn.confidence >= _MIN_CONFIDENCE
        and lsn.evidence.n_games >= _MIN_GAMES
        and _context_matches(lsn.context, observed_interaction)
    ]
    eligible.sort(key=lambda lsn: lsn.confidence, reverse=True)
    selected: list[Lesson] = []
    used = 0
    for lsn in eligible:
        cost = len(_render_one(lsn))
        if used + cost > max_chars:
            break
        selected.append(lsn)
        used += cost
    return selected


def _context_matches(context: str, observed_interaction: bool) -> bool:
    ctx = (context or "").strip().lower()
    if ctx == "no_interaction":
        return not observed_interaction
    # Other contexts (e.g. "vs_control") are not gated on the interaction
    # signal here — they are matched by archetype lookup upstream.
    return True


def _render_one(lesson: Lesson) -> str:
    trigger = f"when {lesson.trigger}, " if lesson.trigger else ""
    return f"- {trigger}{lesson.recommendation}"


def render_learnings(lessons: list[Lesson]) -> str:
    """Render selected lessons as a compact prompt block, or empty string."""
    if not lessons:
        return ""
    lines = ["Self-play learnings (conditional — context-gated):"]
    lines.extend(_render_one(lsn) for lsn in lessons)
    return "\n".join(lines)


def fastest_line(lessons: list[Lesson]) -> Lesson | None:
    """Highest-confidence ``no_interaction`` lesson — the unobstructed-clock
    baseline the turns-ahead planner anchors to. Returns None if none qualify."""
    candidates = [
        lsn
        for lsn in lessons
        if (lsn.context or "").strip().lower() == "no_interaction"
        and lsn.confidence >= _MIN_CONFIDENCE
        and lsn.evidence.n_games >= _MIN_GAMES
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda lsn: lsn.confidence)


def append_lessons(game_format: str, archetype: str, lessons: list[Lesson]) -> Path:
    """Auto-stage lessons to the store, appending to any existing file.

    Used by /selfplay/reflect. Creates the per-format directory as needed.
    """
    path = _store_path(game_format, archetype)
    path.parent.mkdir(parents=True, exist_ok=True)
    existing = get_learnings(game_format, archetype)
    combined = existing + list(lessons)
    path.write_text(
        json.dumps([lsn.model_dump() for lsn in combined], indent=2),
        encoding="utf-8",
    )
    return path


def replace_lessons(game_format: str, archetype: str, lessons: list[Lesson]) -> Path:
    """Overwrite the store for an archetype with ``lessons`` (deletes the file
    when the list is empty). Used by the promotion tool after a lesson is
    promoted into a curated guide or dropped."""
    path = _store_path(game_format, archetype)
    if not lessons:
        if path.exists():
            path.unlink()
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps([lsn.model_dump() for lsn in lessons], indent=2),
        encoding="utf-8",
    )
    return path


def all_staged() -> list[tuple[str, str, list[Lesson]]]:
    """Every staged file as ``(format, slug, lessons)`` for the promotion tool."""
    out: list[tuple[str, str, list[Lesson]]] = []
    if not _LEARNINGS_DIR.exists():
        return out
    for path in sorted(_LEARNINGS_DIR.rglob("*.json")):
        fmt = path.parent.name
        slug = path.stem
        lessons = get_learnings(fmt, slug)
        if lessons:
            out.append((fmt, slug, lessons))
    return out


def learnings_version() -> str:
    """Cache-busting token over all learnings files (count:newest-mtime)."""
    try:
        paths = list(_LEARNINGS_DIR.rglob("*.json"))
    except OSError:
        return "0:0"
    if not paths:
        return "0:0"
    stats = [p.stat() for p in paths]
    newest = max(int(s.st_mtime) for s in stats)
    # Total size is folded in so an append within the same wall-clock second
    # (mtime is whole-second) still busts the cache.
    total = sum(s.st_size for s in stats)
    return f"{len(paths)}:{newest}:{total}"
