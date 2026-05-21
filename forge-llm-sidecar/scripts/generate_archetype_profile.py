#!/usr/bin/env python3
"""Generate an opponent archetype *profile* with the builder LLM.

Runs OFFLINE — never on the sidecar request path. Given an archetype name, a
handful of seed signature cards, and (optionally) a dump of commonly-played
cards, the LLM fills the structured role-bucket profile consumed by the
opponent_strategist node.

Output is always written as ``<slug>.draft.json`` for human review; rename it
to ``<slug>.json`` once verified.

    python -m scripts.generate_archetype_profile \\
        --name "Ruby Storm" --format modern \\
        --seed-signature "Grapeshot,Ral, Monsoon Mage,Ruby Medallion" \\
        --extra-cards-file ./ruby_storm_top80.txt

Profiles live in ``app/knowledge/archetype_profiles/<format>/<slug>.json`` and
are committed and human-reviewable. The buckets are intentionally many-to-many:
a single card may appear in several buckets.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.config import CONFIG  # noqa: E402
from app.knowledge import loader  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402
from app.llm_client import LLMError, generate_json  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("generate_archetype_profile")

ROOT = pathlib.Path(__file__).resolve().parent.parent
PROFILE_DIR = ROOT / "app" / "knowledge" / "archetype_profiles"

_SYSTEM = (
    "You are an expert Magic: The Gathering deck analyst building a structured "
    "profile of an archetype so an AI can reason about what an opponent on that "
    "deck is holding. Be accurate and concrete; only list real cards that the "
    "archetype actually plays. Always answer with a single JSON object."
)


def _build_prompt(name: str, fmt: str, seed: list[str], extra: list[str]) -> str:
    buckets = "\n".join(f"    - {b}" for b in loader.PROFILE_BUCKETS)
    seed_str = ", ".join(seed) or "(none provided)"
    extra_str = ", ".join(extra) if extra else "(none provided)"
    return (
        f"Archetype: {name}\nFormat: {fmt}\n"
        f"Known signature cards: {seed_str}\n"
        f"Other commonly-played cards in this deck (may include sideboard): {extra_str}\n\n"
        "Build a JSON profile with EXACTLY these top-level keys:\n"
        '  "schema_version": 1,\n'
        '  "name", "format", "colors" (array of WUBRG letters),\n'
        '  "strategy_type" (aggro|tempo|midrange|control|combo|ramp),\n'
        '  "expected_deck_total" (usually 60),\n'
        '  "macro_plan" (2-4 sentences on how the deck wins and what to watch),\n'
        '  "win_turn_window" ([min_turn, max_turn]),\n'
        '  "buckets": an object with these keys (every key present):\n'
        f"{buckets}\n"
        "    Each bucket is an object {\"target_count\": int (expected copies in a "
        "60-75 card deck+sideboard), \"cards\": [card names], \"notes\": optional "
        "string}. The special bucket \"combo_pieces\" may also include \"pairs\": "
        "[[cardA, cardB], ...]. The special bucket \"interaction_density\" is "
        "instead a single number 0-1.\n"
        "    IMPORTANT: buckets are many-to-many — put a card in EVERY bucket whose "
        "role it fills (e.g. a planeswalker that reduces costs and wins the game "
        "goes in mana_reducers, win_conditions, and planeswalker_threats). Account "
        "for as many of the deck's real cards as you can; most lands go in "
        "\"lands\".\n"
        '  "dual_role_cards": [{"card", "roles":[bucket names], "notes"}] for the '
        "most important multi-role cards,\n"
        '  "tells": [short strings] — what tips you off this is the deck,\n'
        '  "predicted_lines": [{"trigger", "line"}] — likely sequences,\n'
        '  "kill_priority": [card names] most dangerous first,\n'
        '  "interaction_to_disrupt": [card names] their answers to play around,\n'
        '  "last_updated": "YYYY-MM-DD".\n'
        "Respond with the JSON object only."
    )


def _validate(profile: dict) -> list[str]:
    """Return a list of human-readable problems (empty == looks good)."""
    problems: list[str] = []
    for key in ("name", "format", "colors", "strategy_type", "buckets", "macro_plan"):
        if key not in profile:
            problems.append(f"missing top-level key: {key}")
    buckets = profile.get("buckets") or {}
    missing = [b for b in loader.PROFILE_BUCKETS if b not in buckets]
    if missing:
        problems.append(f"missing buckets: {', '.join(missing)}")
    if "interaction_density" not in buckets:
        problems.append("missing buckets.interaction_density scalar")
    return problems


async def _generate(name: str, fmt: str, seed: list[str], extra: list[str]) -> dict:
    prompt = _build_prompt(name, fmt, seed, extra)
    return await generate_json(
        prompt, system=_SYSTEM, model=CONFIG.model_name, temperature=0.4
    )


def main() -> int:
    ap = argparse.ArgumentParser(description="Generate an archetype profile draft.")
    ap.add_argument("--name", required=True, help='Archetype name, e.g. "Ruby Storm"')
    ap.add_argument("--format", required=True, help="Format slug, e.g. modern / standard")
    ap.add_argument("--seed-signature", default="", help="Comma-separated signature cards")
    ap.add_argument("--extra-cards-file", default="", help="Path to a newline/comma list of common cards")
    ap.add_argument("--out", default="", help="Output path (defaults to <dir>/<slug>.draft.json)")
    args = ap.parse_args()

    fmt = args.format.strip().lower()
    seed = [s.strip() for s in args.seed_signature.split(",") if s.strip()]
    extra: list[str] = []
    if args.extra_cards_file:
        text = pathlib.Path(args.extra_cards_file).read_text(encoding="utf-8")
        for chunk in text.replace(",", "\n").splitlines():
            c = chunk.strip()
            if c:
                extra.append(c)

    try:
        profile = asyncio.run(_generate(args.name, fmt, seed, extra))
    except LLMError as exc:
        log.error("LLM call failed: %s", exc)
        return 1

    # Stamp identity fields so a sloppy model answer is still usable.
    profile.setdefault("schema_version", 1)
    profile["name"] = args.name
    profile["format"] = fmt

    problems = _validate(profile)
    if problems:
        log.warning("Profile has issues (review carefully): %s", "; ".join(problems))

    slug = slugify(args.name)
    out_path = (
        pathlib.Path(args.out)
        if args.out
        else PROFILE_DIR / fmt / f"{slug}.draft.json"
    )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(profile, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    log.info("Wrote draft profile: %s", out_path)
    log.info("Review it, then rename to %s/%s.json to activate.", fmt, slug)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
