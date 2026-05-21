#!/usr/bin/env python3
"""List archetype names for a format (from metagame data + curated archetypes).

Handy for driving generate_archetype_profile.py in batch. Marks which already
have a profile.

    python -m scripts.list_meta_archetypes modern
    python -m scripts.list_meta_archetypes standard --missing-only
"""

from __future__ import annotations

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import loader, metagame  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402


def main() -> int:
    ap = argparse.ArgumentParser(description="List archetypes for a format.")
    ap.add_argument("format", help="Format slug, e.g. modern / standard")
    ap.add_argument("--missing-only", action="store_true", help="Only show archetypes lacking a profile")
    args = ap.parse_args()

    fmt = args.format.strip().lower()
    names = {a.get("name", "") for a in metagame.get_metagame(fmt)}
    names |= {a.get("name", "") for a in loader.get_archetypes(fmt)}
    names.discard("")

    for name in sorted(names):
        has_profile = loader.load_archetype_profile(name, fmt) is not None
        if args.missing_only and has_profile:
            continue
        mark = "have" if has_profile else "MISSING"
        print(f"[{mark}] {name}  ->  {fmt}/{slugify(name)}.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
