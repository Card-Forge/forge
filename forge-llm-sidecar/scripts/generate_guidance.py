#!/usr/bin/env python3
"""Generate piloting guidance for all archetypes across all formats.

Reads metagame data from ``app/knowledge/metagame_data/`` and calls the local
LLM to generate structured piloting guidance for each archetype.

Output: ``guidance/<format>.json`` — one file per format with guidance for
every archetype in the current metagame.

Usage::

    python scripts/generate_guidance.py                    # all formats
    python scripts/generate_guidance.py modern standard    # one or two formats

Progress is written to ``guidance/progress.json`` after each archetype so the
run can be resumed after interruption.
"""

from __future__ import annotations

import asyncio
import json
import logging
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import metagame  # noqa: E402
from app.llm_client import LLMError, generate_json  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("generate_guidance")

GUIDANCE_DIR = pathlib.Path(__file__).resolve().parent.parent / "guidance"
PROGRESS_FILE = GUIDANCE_DIR / "progress.json"

# ---------------------------------------------------------------------------
# System prompt — tells the model who it is and how to format output
# ---------------------------------------------------------------------------
_SYSTEM_PROMPT = (
    "You are an expert Magic: The Gathering player and deck builder. "
    "You write clear, actionable piloting guidance for a specific deck archetype "
    "in a specific format. Always answer with a single JSON object and nothing else."
)

# ---------------------------------------------------------------------------
# User prompt template — filled in with archetype data
# ---------------------------------------------------------------------------
_USER_PROMPT_TEMPLATE = """You are writing piloting guidance for a Magic: The Gathering deck.

## Archetype: {name}
## Format: {format}
## Colors: {colors}
## Metagame share: {meta_share}%
## Strategy (from metagame): {strategy}
## Signature cards: {signature_cards}

Write detailed piloting guidance covering these aspects. Base your advice on real
MTG knowledge of this archetype's typical game plan and common matchups.

1. **philosophy** (1-2 sentences): The overarching strategic identity. What is this
   deck trying to do, and what does it win against?

2. **opening_strategy**:
   - mulligan_criteria: What hands to keep / mulligan? Be specific about cards and
     mana base requirements.
   - early_game_plays: What are the typical turn-1 and turn-2 plays? Include land
     sequencing advice.
   - sideboard_plan: What cards to bring in / out against different opponent types
     (aggro, control, combo, mirror).

3. **midgame_priorities**:
   - resource_management: How to manage mana, cards in hand, and graveyard.
   - board_control: When to commit threats, when to hold back.
   - threat_tracking: What to watch for in the opponent's plays — key tells.

4. **endgame_tactics**:
   - finishers: What wins the game? How to get there?
   - when_to_commit: How to recognize when you should go all-in.
   - when_to_concede: What signals to fold (bad matchups, dead hands, no comeback).

5. **threat_responses**:
   - against_countermagic: How to overcome or work around counterspells.
   - against_ramp: How to deal with out-accelerating opponents.
   - against_aggro: How to stabilize against aggressive decks.
   - against_control: How to pressure or beat control decks.
   - vs_mirror: How to win the mirror match.

6. **sideboard_advice**: Post-sideboard strategy adjustments. What general changes
   should the pilot make after sideboard?

7. **notes**: Any additional tips, common mistakes to avoid, or unique interactions.

Respond with a JSON object with exactly these keys:
  "archetype" (string), "format" (string), "colors" (array of strings),
  "philosophy" (string), "opening_strategy" (object with "mulligan_criteria",
  "early_game_plays", "sideboard_plan"),
  "midgame_priorities" (object with "resource_management", "board_control",
  "threat_tracking"),
  "endgame_tactics" (object with "finishers", "when_to_commit", "when_to_concede"),
  "threat_responses" (object with "against_countermagic", "against_ramp",
  "against_aggro", "against_control", "vs_mirror"),
  "sideboard_advice" (string), "notes" (string).
"""


def _build_prompt(archetype: dict, archetypes_in_format: list[dict]) -> str:
    """Build the user prompt for a single archetype."""
    name = archetype.get("name", "Unknown")
    fmt = archetype.get("format", archetype.get("source", "Unknown"))
    colors = ", ".join(archetype.get("colors", [])) or "Unknown"
    meta_share = archetype.get("meta_share")
    meta_share_str = f"{meta_share:.1f}" if meta_share is not None else "N/A"
    strategy = archetype.get("strategy", archetype.get("note", "")) or ""
    sig = ", ".join(archetype.get("signature_cards", [])[:5]) or "(none listed)"

    # If strategy wasn't in the metagame data, try to infer from context
    if not strategy:
        strategy = (
            f"Top archetypes in {fmt}: "
            + ", ".join(a.get("name", "") for a in archetypes_in_format[:5])
        )

    return _USER_PROMPT_TEMPLATE.format(
        name=name,
        format=fmt,
        colors=colors,
        meta_share=meta_share_str,
        strategy=strategy,
        signature_cards=sig,
    )


def _parse_guidance(raw: dict) -> dict | None:
    """Validate and clean up LLM output. Returns None on failure."""
    required_keys = [
        "archetype", "format", "colors", "philosophy",
        "opening_strategy", "midgame_priorities", "endgame_tactics",
        "threat_responses", "sideboard_advice", "notes",
    ]
    for key in required_keys:
        if key not in raw:
            log.warning("  missing key: %s", key)
            return None

    opening = raw.get("opening_strategy", {})
    midgame = raw.get("midgame_priorities", {})
    endgame = raw.get("endgame_tactics", {})
    threats = raw.get("threat_responses", {})

    return {
        "archetype": str(raw["archetype"]).strip(),
        "format": str(raw["format"]).strip(),
        "colors": raw.get("colors", []),
        "philosophy": str(raw["philosophy"]).strip(),
        "opening_strategy": {
            "mulligan_criteria": str(opening.get("mulligan_criteria", "")).strip(),
            "early_game_plays": str(opening.get("early_game_plays", "")).strip(),
            "sideboard_plan": str(opening.get("sideboard_plan", "")).strip(),
        },
        "midgame_priorities": {
            "resource_management": str(midgame.get("resource_management", "")).strip(),
            "board_control": str(midgame.get("board_control", "")).strip(),
            "threat_tracking": str(midgame.get("threat_tracking", "")).strip(),
        },
        "endgame_tactics": {
            "finishers": str(endgame.get("finishers", "")).strip(),
            "when_to_commit": str(endgame.get("when_to_commit", "")).strip(),
            "when_to_concede": str(endgame.get("when_to_concede", "")).strip(),
        },
        "threat_responses": {
            "against_countermagic": str(threats.get("against_countermagic", "")).strip(),
            "against_ramp": str(threats.get("against_ramp", "")).strip(),
            "against_aggro": str(threats.get("against_aggro", "")).strip(),
            "against_control": str(threats.get("against_control", "")).strip(),
            "vs_mirror": str(threats.get("vs_mirror", "")).strip(),
        },
        "sideboard_advice": str(raw.get("sideboard_advice", "")).strip(),
        "notes": str(raw.get("notes", "")).strip(),
    }


# ---------------------------------------------------------------------------
# Progress tracking
# ---------------------------------------------------------------------------
def _load_progress() -> dict:
    if PROGRESS_FILE.exists():
        try:
            return json.loads(PROGRESS_FILE.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return {}
    return {}


def _save_progress(progress: dict) -> None:
    GUIDANCE_DIR.mkdir(parents=True, exist_ok=True)
    PROGRESS_FILE.write_text(
        json.dumps(progress, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _is_done(progress: dict, slug: str, name: str) -> bool:
    return progress.get(f"{slug}:{name}", {}).get("status") == "done"


# ---------------------------------------------------------------------------
# LLM call with retry
# ---------------------------------------------------------------------------
async def _generate_guidance(prompt: str) -> dict | None:
    for attempt in range(3):
        try:
            raw = await generate_json(prompt, system=_SYSTEM_PROMPT)
            parsed = _parse_guidance(raw)
            if parsed:
                return parsed
            log.warning("  parse failed on attempt %d", attempt + 1)
        except LLMError as exc:
            log.warning("  LLM error on attempt %d: %s", attempt + 1, exc)
        await asyncio.sleep(2 * (attempt + 1))
    return None


# ---------------------------------------------------------------------------
# Main generator
# ---------------------------------------------------------------------------
async def generate_for_format(slug: str, progress: dict, *, dry_run: bool = False) -> int:
    """Generate guidance for all archetypes in one format. Returns success count."""
    archetypes = metagame.get_metagame(slug)
    if not archetypes:
        log.warning("no archetypes for format '%s', skipping", slug)
        return 0

    log.info("%s: %d archetypes to process", slug.upper(), len(archetypes))

    ok = 0
    for i, arch in enumerate(archetypes):
        name = arch.get("name", f"unknown_{i}")
        key = f"{slug}:{name}"

        if key in progress:
            status = progress[key].get("status", "unknown")
            if status == "done":
                log.info("[%d/%d] %s — %s [skip]", i + 1, len(archetypes), slug.upper(), name)
                ok += 1
                continue
            elif status == "failed":
                log.info("[%d/%d] %s — %s [retry]", i + 1, len(archetypes), slug.upper(), name)

        progress[key] = {"status": "in_progress", "format": slug, "archetype": name}
        _save_progress(progress)

        prompt = _build_prompt(arch, archetypes)
        guidance = await _generate_guidance(prompt)

        if guidance and not dry_run:
            out_path = GUIDANCE_DIR / f"{slug}.json"
            # Load existing or create new
            if out_path.exists():
                try:
                    existing = json.loads(out_path.read_text(encoding="utf-8"))
                    if not isinstance(existing, list):
                        existing = []
                except (json.JSONDecodeError, OSError):
                    existing = []
            else:
                existing = []

            # Check if this archetype already exists in the file
            found = False
            for j, g in enumerate(existing):
                if g.get("archetype") == name:
                    existing[j] = guidance
                    found = True
                    break
            if not found:
                existing.append(guidance)

            out_path.write_text(
                json.dumps(existing, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )

        status = "done" if guidance else "failed"
        progress[key] = {
            "status": status,
            "format": slug,
            "archetype": name,
            "attempt": i,
        }
        _save_progress(progress)

        if status == "done":
            ok += 1
            log.info("[%d/%d] %s — %s [done] ✓", i + 1, len(archetypes), slug.upper(), name)
        else:
            log.error("[%d/%d] %s — %s [failed] ✗", i + 1, len(archetypes), slug.upper(), name)

        # Be gentle to the LLM server between calls
        await asyncio.sleep(0.5)

    return ok


async def main(argv: list[str]) -> int:
    """Generate guidance for one or more formats."""
    slugs = argv[1:] or metagame.available_slugs()
    if not slugs:
        log.error("no formats specified and none found. Usage: %s [format ...]", argv[0])
        return 1

    progress = _load_progress()
    total_ok = 0
    total_skipped = 0

    for slug in slugs:
        slug = slug.strip().lower()
        count = await generate_for_format(slug, progress)
        total_ok += count

    log.info("done: %d/%d archetypes generated guidance", total_ok, sum(
        1 for v in progress.values() if v.get("format") in slugs
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main(sys.argv)))
