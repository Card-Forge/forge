#!/usr/bin/env python3
"""Pre-bake per-deck mana profiles for known decklists.

Runs OFFLINE — never on the sidecar request path. Reads Forge ``.dck`` files
(or plain "N CardName" decklists), generates a manabase profile per list via the
builder LLM, and writes it to the committed ``mana_profiles/<format>/<hash>.json``
directory. Profiles are keyed by a hash of the main-deck card list, so a
pre-baked profile only hits in game when the AI plays a byte-identical list
(e.g. fixed self-play decks). Everything else is generated lazily at runtime by
``app.knowledge.mana_profile.get_or_schedule``.

    python scripts/build_mana_profiles.py selfplay/*.dck --format legacy
    python scripts/build_mana_profiles.py path/to/decks/ --format modern --force

Uses BUILDER_LLM_* env vars when set (falling back to the runtime LLM_* values),
matching the other offline builders.
"""

from __future__ import annotations

import argparse
import asyncio
import glob
import logging
import os
import pathlib
import sys

# Builder LLM overrides: prefer BUILDER_LLM_* but fall back to runtime values.
# Set BEFORE importing app.config so CONFIG picks them up.
for _key in ("LLM_BASE_URL", "MODEL_NAME", "LLM_API_KEY", "LLM_TIMEOUT"):
    _bk = "BUILDER_" + _key
    if os.environ.get(_bk):
        os.environ[_key] = os.environ[_bk]

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import mana_profile  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("build_mana_profiles")


def _parse_decklist(path: pathlib.Path) -> tuple[str, list[str]]:
    """Return (deck_name, expanded_card_names) from a .dck or plain list.

    Sideboard sections are ignored — the profile describes the main deck the AI
    actually plays game one. (Post-sideboard games hash differently and get a
    lazy profile at runtime.)"""
    name = path.stem
    cards: list[str] = []
    section = "main"
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line:
            continue
        low = line.lower()
        if low.startswith("[metadata]") or low.startswith("name="):
            if low.startswith("name="):
                name = line.split("=", 1)[1].strip() or name
            continue
        if line.startswith("["):
            section = "main" if "main" in low else "other"
            continue
        if section != "main":
            continue
        # "N CardName" (Forge .dck) or "Nx CardName" (common export form).
        parts = line.split(None, 1)
        if len(parts) == 2 and parts[0].rstrip("x").isdigit():
            qty = int(parts[0].rstrip("x"))
            card = parts[1].strip()
            cards.extend([card] * max(1, qty))
        else:
            cards.append(line)
    return name, cards


def _iter_paths(inputs: list[str]) -> list[pathlib.Path]:
    out: list[pathlib.Path] = []
    for token in inputs:
        for match in glob.glob(token):
            p = pathlib.Path(match)
            if p.is_dir():
                out.extend(sorted(p.glob("*.dck")))
            elif p.is_file():
                out.append(p)
    return out


async def _main_async(args: argparse.Namespace) -> int:
    paths = _iter_paths(args.inputs)
    if not paths:
        log.error("no decklists matched: %s", args.inputs)
        return 1
    built = skipped = failed = 0
    for path in paths:
        name, cards = _parse_decklist(path)
        if not cards:
            log.warning("%s: no cards parsed; skipping", path)
            continue
        h = mana_profile.deck_hash(cards)
        if not args.force and mana_profile.get_cached(cards, args.format) is not None:
            log.info("%s (%s): cached; skipping (use --force to rebuild)", name, h)
            skipped += 1
            continue
        if not mana_profile.has_analyzable_manabase(cards):
            log.info("%s (%s): no fetch/utility lands; skipping", name, h)
            skipped += 1
            continue
        profile = await mana_profile.build(cards, name, args.format, model=args.model)
        if profile is None:
            log.error("%s (%s): build failed", name, h)
            failed += 1
            continue
        dest = mana_profile.save_committed(profile)
        log.info("%s (%s): wrote %s", name, h, dest)
        built += 1
    log.info("done: %d built, %d skipped, %d failed", built, skipped, failed)
    return 0 if failed == 0 else 2


def main() -> int:
    ap = argparse.ArgumentParser(description="Pre-bake per-deck mana profiles.")
    ap.add_argument("inputs", nargs="+", help="decklist files, globs, or directories")
    ap.add_argument("--format", default="", help="format slug (e.g. legacy, modern)")
    ap.add_argument("--model", default=None, help="override builder model name")
    ap.add_argument("--force", action="store_true", help="rebuild existing profiles")
    args = ap.parse_args()
    return asyncio.run(_main_async(args))


if __name__ == "__main__":
    raise SystemExit(main())
