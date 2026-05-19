#!/usr/bin/env python3
"""Generate piloting guides for metagame archetypes with an LLM.

Runs OFFLINE — never on the sidecar request path. For each archetype in
``app/knowledge/metagame_data/<format>.json`` it gathers context (colors,
signature cards, a representative decklist when reachable), asks the builder LLM
to write a piloting guide, validates it against :class:`PilotingGuide`, and
writes ``app/knowledge/piloting/<format>/<slug>.json``.

    python scripts/build_piloting_guides.py                 # all formats
    python scripts/build_piloting_guides.py modern          # one format
    python scripts/build_piloting_guides.py modern --archetype "Boros Energy"
    python scripts/build_piloting_guides.py modern --force  # regenerate existing

Generic fallback guides (``piloting/generic/*.json``) are hand-authored and are
NOT touched by this script. Generated files are committed and human-reviewable.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import logging
import pathlib
import sys
import time

# Allow running as a plain script (no package install needed).
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import builder_llm, scraper  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402
from app.knowledge.piloting_schema import PILOTING_SCHEMA_VERSION, PilotingGuide  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("build_piloting_guides")

ROOT = pathlib.Path(__file__).resolve().parent.parent
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"

_SYSTEM = (
    "You are an expert Magic: The Gathering coach writing a concise, accurate "
    "piloting guide for one deck archetype. Answer with a single JSON object "
    "and nothing else."
)


def _build_prompt(arch: dict, fmt: str, decklist: list[str]) -> str:
    colors = "/".join(arch.get("colors", [])) or "unknown"
    signature = ", ".join(arch.get("signature_cards", [])) or "(none listed)"
    deck_note = (
        f"Representative decklist:\n{', '.join(decklist[:80])}\n\n"
        if decklist
        else "No decklist available; rely on your knowledge of the archetype.\n\n"
    )
    return (
        f"Write a piloting guide for the {fmt} archetype \"{arch.get('name')}\".\n"
        f"Colors: {colors}. Signature cards: {signature}.\n\n"
        f"{deck_note}"
        "Respond with a single JSON object with exactly these keys:\n"
        '  "archetype": string,\n'
        f'  "format": "{fmt}",\n'
        '  "strategy_type": one of "aggro","tempo","midrange","control","combo","ramp",\n'
        '  "overview": string (2-4 sentences on the game plan),\n'
        '  "win_conditions": array of strings,\n'
        '  "mulligan": {"keep_criteria": [string], "mulligan_criteria": [string],\n'
        '    "examples": [{"hand": [string], "decision": "keep"|"mulligan", "reason": string}]},\n'
        '  "game_plan": {"early_game": [string], "mid_game": [string], "late_game": [string]},\n'
        '  "key_cards": [{"name": string, "role": string, "notes": string}],\n'
        '  "sequencing_tips": array of strings,\n'
        '  "matchups": [{"opponent_archetype": string, "advice": string, "watch_for": [string]}],\n'
        '  "common_threats": array of strings.\n'
        "Be specific and practical. Do not include any text outside the JSON object."
    )


def build_one(arch: dict, fmt: str, links: dict[str, str], *, force: bool) -> bool:
    name = arch.get("name") or ""
    slug = slugify(name)
    if not slug:
        log.warning("skipping archetype with no usable name: %r", arch)
        return False

    out_dir = PILOTING_DIR / fmt
    out_path = out_dir / f"{slug}.json"
    if out_path.exists() and not force:
        log.info("skip %s (exists; use --force to regenerate)", out_path)
        return False

    decklist: list[str] = []
    page_url = links.get(name)
    if page_url:
        decklist = scraper.fetch_archetype_decklist(page_url)

    try:
        raw = builder_llm.generate_guide_json(_build_prompt(arch, fmt, decklist), system=_SYSTEM)
    except builder_llm.BuilderLLMError as exc:
        log.error("LLM failed for %s: %s", name, exc)
        return False

    raw.setdefault("archetype", name)
    raw.setdefault("format", fmt)
    raw["metadata"] = {
        "source": "build_piloting_guides.py" + (" + mtggoldfish" if decklist else ""),
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "model": builder_llm.MODEL_NAME,
        "schema_version": PILOTING_SCHEMA_VERSION,
    }

    try:
        guide = PilotingGuide.model_validate(raw)
    except Exception as exc:  # noqa: BLE001 - report and move on
        log.error("guide for %s failed schema validation: %s", name, exc)
        return False

    out_dir.mkdir(parents=True, exist_ok=True)
    out_path.write_text(guide.model_dump_json(indent=2) + "\n", encoding="utf-8")
    log.info("wrote %s", out_path)
    return True


def build_format(fmt: str, *, archetype: str | None, force: bool) -> int:
    meta_path = METAGAME_DIR / f"{fmt}.json"
    if not meta_path.exists():
        log.error("no metagame data for '%s' (%s)", fmt, meta_path)
        return 0

    archetypes = json.loads(meta_path.read_text(encoding="utf-8")).get("archetypes", [])
    if archetype:
        archetypes = [a for a in archetypes if a.get("name") == archetype]
        if not archetypes:
            log.error("archetype %r not found in %s", archetype, fmt)
            return 0

    links = scraper.archetype_links(fmt)
    ok = 0
    for i, arch in enumerate(archetypes):
        if build_one(arch, fmt, links, force=force):
            ok += 1
        if i < len(archetypes) - 1:
            time.sleep(2)  # be polite to the LLM server and the source site
    return ok


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Generate piloting guides with an LLM.")
    parser.add_argument("formats", nargs="*", help="format slugs (default: all metagame files)")
    parser.add_argument("--archetype", help="only this archetype (use with one format)")
    parser.add_argument("--force", action="store_true", help="regenerate guides that already exist")
    args = parser.parse_args(argv[1:])

    formats = args.formats or sorted(p.stem for p in METAGAME_DIR.glob("*.json"))
    if args.archetype and len(formats) != 1:
        parser.error("--archetype requires exactly one format")

    total = 0
    for fmt in formats:
        total += build_format(fmt, archetype=args.archetype, force=args.force)
    log.info("done: %d guide(s) written", total)
    return 0 if total else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
