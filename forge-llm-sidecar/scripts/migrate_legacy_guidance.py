#!/usr/bin/env python3
"""Migrate legacy ``guidance/*.json`` into runtime guide/profile files.

This is an offline one-way bridge. Runtime code continues to read only
``app/knowledge/piloting``, ``app/knowledge/archetype_profiles``, and
``app/knowledge/learnings``.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import guidance_migration as gm  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402


def _targets(fmt: str, archetype: str | None) -> list[str]:
    legacy = gm.load_legacy_guidance(fmt)
    if archetype:
        slug = slugify(archetype)
        return [slug] if slug in legacy else []
    return sorted(legacy)


def migrate_format(
    fmt: str,
    *,
    archetype: str | None,
    write_guides: bool,
    write_profiles: bool,
    create_missing: bool,
    dry_run: bool,
) -> int:
    legacy_by_slug = gm.load_legacy_guidance(fmt)
    meta_by_slug = gm.load_metagame(fmt)
    count = 0
    for slug in _targets(fmt, archetype):
        legacy = legacy_by_slug.get(slug)
        if not legacy:
            continue
        guide_path = gm.PILOTING_DIR / fmt / f"{slug}.json"
        if guide_path.exists():
            guide = json.loads(guide_path.read_text(encoding="utf-8"))
            merged = gm.merge_legacy_into_guide(guide, legacy, fmt)
        elif create_missing:
            merged = gm.guide_from_legacy(fmt, legacy, meta_by_slug.get(slug))
        else:
            print(f"skip {fmt}/{slug}: no runtime piloting guide")
            continue

        if write_guides:
            print(f"{'would write' if dry_run else 'write'} {guide_path}")
            if not dry_run:
                guide_path.parent.mkdir(parents=True, exist_ok=True)
                guide_path.write_text(json.dumps(merged, indent=2) + "\n", encoding="utf-8")

        if write_profiles:
            profile = gm.build_archetype_profile(merged, meta_by_slug.get(slug), legacy)
            profile_path = gm.PROFILE_DIR / fmt / f"{slug}.json"
            print(f"{'would write' if dry_run else 'write'} {profile_path}")
            if not dry_run:
                gm.write_profile(profile, fmt, slug)

        count += 1
    return count


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Migrate legacy guidance into runtime files.")
    parser.add_argument("formats", nargs="*", default=["standard"], help="format slugs")
    parser.add_argument("--archetype", help="only this archetype; requires one format")
    parser.add_argument("--no-guides", action="store_true", help="do not update piloting guides")
    parser.add_argument("--no-profiles", action="store_true", help="do not write archetype profiles")
    parser.add_argument("--create-missing", action="store_true", help="create piloting guides if absent")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv[1:])

    if args.archetype and len(args.formats) != 1:
        parser.error("--archetype requires exactly one format")
    total = 0
    for fmt in args.formats:
        total += migrate_format(
            fmt,
            archetype=args.archetype,
            write_guides=not args.no_guides,
            write_profiles=not args.no_profiles,
            create_missing=args.create_missing,
            dry_run=args.dry_run,
        )
    print(f"done: {total} archetype(s)")
    return 0 if total else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
