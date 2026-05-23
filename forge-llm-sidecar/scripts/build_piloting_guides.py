#!/usr/bin/env python3
"""Generate piloting guides for metagame archetypes.

Runs OFFLINE — never on the sidecar request path. Walks the
:mod:`app.knowledge.primers` provider chain (Cards Realm, Hareruya, MTG Arena
Zone, Moxfield, Draftsim) and uses the builder LLM to extract structured
fields from cleaned primer HTML. When every editorial source fails, the
orchestrator falls back to a synthesized default primer marked low-confidence.

    python scripts/build_piloting_guides.py                       # all formats
    python scripts/build_piloting_guides.py modern                # one format
    python scripts/build_piloting_guides.py modern --archetype "Boros Energy"
    python scripts/build_piloting_guides.py modern --force        # regenerate existing
    python scripts/build_piloting_guides.py modern --from-diff    # consume metagame diff
    python scripts/build_piloting_guides.py modern --rebuild-archived
    python scripts/build_piloting_guides.py modern --refresh-stale # rerun TTL-expired

Generic fallback guides (``piloting/generic/*.json``) are hand-authored and
NOT touched by this script. Generated files are committed and human-reviewable.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import logging
import pathlib
import shutil
import sys
import time

# Allow running as a plain script (no package install needed).
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import guidance_migration, primers  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
log = logging.getLogger("build_piloting_guides")

ROOT = pathlib.Path(__file__).resolve().parent.parent
METAGAME_DIR = ROOT / "app" / "knowledge" / "metagame_data"
PILOTING_DIR = ROOT / "app" / "knowledge" / "piloting"
ARCHIVE_DIR_NAME = "_archive"

# Per-format time-to-live (days). Used by --refresh-stale.
_TTL_DAYS = {
    "standard": 30,
    "historic": 30,
    "modern": 90,
    "pioneer": 90,
    "legacy": 90,
    "pauper": 90,
    "vintage": 180,
    "premodern": 365,
}


def _archive_path(fmt: str, slug: str) -> pathlib.Path:
    return PILOTING_DIR / fmt / ARCHIVE_DIR_NAME / f"{slug}.json"


def _live_path(fmt: str, slug: str) -> pathlib.Path:
    return PILOTING_DIR / fmt / f"{slug}.json"


def _move_to_archive(fmt: str, slug: str) -> bool:
    src = _live_path(fmt, slug)
    if not src.exists():
        return False
    dst = _archive_path(fmt, slug)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(src), str(dst))
    log.info("archived %s/%s -> %s", fmt, slug, dst)
    return True


def _restore_from_archive(fmt: str, slug: str) -> bool:
    src = _archive_path(fmt, slug)
    if not src.exists():
        return False
    dst = _live_path(fmt, slug)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(src), str(dst))
    log.info("restored %s/%s from archive", fmt, slug)
    return True


def _maybe_enrich(guide, arch: dict, fmt: str, *, enabled: bool, known_archetypes: list[dict]):
    """Optionally run YouTube enrichment over a freshly-built guide."""
    if not enabled or guide is None:
        return guide
    try:
        from app.knowledge.primers.youtube import enricher  # lazy import
    except ImportError:
        log.warning("youtube enrichment requested but package import failed")
        return guide
    known_names = [a.get("name", "") for a in known_archetypes if a.get("name")]
    return enricher.enrich(
        guide,
        archetype=arch.get("name", ""),
        fmt=fmt,
        signature_cards=arch.get("signature_cards") or [],
        known_archetypes=known_names,
    )


def build_one(
    arch: dict,
    fmt: str,
    *,
    force: bool,
    enrich_from_youtube: bool = False,
    all_archetypes: list[dict] | None = None,
    merge_legacy_guidance: bool = False,
    write_profile: bool = False,
) -> bool:
    name = arch.get("name") or ""
    slug = slugify(name)
    if not slug:
        log.warning("skipping archetype with no usable name: %r", arch)
        return False

    out_path = _live_path(fmt, slug)
    if out_path.exists() and not force:
        log.info("skip %s (exists; use --force to regenerate)", out_path)
        return False

    guide = primers.build_primer(
        name,
        fmt,
        signature_cards=arch.get("signature_cards") or [],
        colors=arch.get("colors") or [],
    )
    if guide is None:
        log.error("failed to build any guide for %s in %s", name, fmt)
        return False

    guide = _maybe_enrich(
        guide, arch, fmt, enabled=enrich_from_youtube, known_archetypes=all_archetypes or []
    )
    payload = guide.model_dump(exclude={"stale_flags"})
    legacy = guidance_migration.legacy_for(fmt, name)
    if merge_legacy_guidance and legacy:
        payload = guidance_migration.merge_legacy_into_guide(payload, legacy, fmt)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    log.info("wrote %s (source: %s)", out_path, guide.metadata.source or "default")
    if write_profile:
        profile = guidance_migration.build_archetype_profile(payload, arch, legacy)
        profile_path = guidance_migration.write_profile(profile, fmt, slug)
        log.info("wrote %s", profile_path)
    # Clear archive duplicate if any.
    arch_path = _archive_path(fmt, slug)
    if arch_path.exists():
        arch_path.unlink()
        log.info("removed stale archive %s", arch_path)
    return True


def _load_archetypes(fmt: str) -> list[dict]:
    meta_path = METAGAME_DIR / f"{fmt}.json"
    if not meta_path.exists():
        log.error("no metagame data for '%s' (%s)", fmt, meta_path)
        return []
    return json.loads(meta_path.read_text(encoding="utf-8")).get("archetypes", [])


def _load_diff(fmt: str) -> dict | None:
    diff_path = METAGAME_DIR / f"{fmt}.diff.json"
    if not diff_path.exists():
        return None
    try:
        return json.loads(diff_path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("failed to load diff for %s: %s", fmt, exc)
        return None


def _archetypes_by_name(archetypes: list[dict]) -> dict[str, dict]:
    return {(a.get("name") or "").lower(): a for a in archetypes}


def _is_stale(out_path: pathlib.Path, ttl_days: int) -> bool:
    if not out_path.exists():
        return True
    try:
        payload = json.loads(out_path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return True
    generated = (payload.get("metadata") or {}).get("generated_at", "")
    try:
        generated_dt = dt.datetime.fromisoformat(generated.replace("Z", "+00:00"))
    except ValueError:
        return True
    age = dt.datetime.now(dt.timezone.utc) - generated_dt.astimezone(dt.timezone.utc)
    return age.days >= ttl_days


def build_format(
    fmt: str,
    *,
    archetype: str | None,
    force: bool,
    from_diff: bool,
    rebuild_archived: bool,
    refresh_stale: bool,
    enrich_from_youtube: bool = False,
    merge_legacy_guidance: bool = False,
    write_profiles: bool = False,
) -> int:
    archetypes = _load_archetypes(fmt)
    if not archetypes:
        return 0
    by_name = _archetypes_by_name(archetypes)

    if archetype:
        target = by_name.get(archetype.lower())
        if not target:
            log.error("archetype %r not found in %s", archetype, fmt)
            return 0
        return 1 if build_one(
            target,
            fmt,
            force=force,
            enrich_from_youtube=enrich_from_youtube,
            all_archetypes=archetypes,
            merge_legacy_guidance=merge_legacy_guidance,
            write_profile=write_profiles,
        ) else 0

    queue: list[dict] = []

    if from_diff:
        diff = _load_diff(fmt)
        if diff is None:
            log.warning("no diff for %s — falling back to full rebuild", fmt)
        else:
            for name in diff.get("new_archetypes", []):
                if name.lower() in by_name:
                    queue.append(by_name[name.lower()])
            for name in diff.get("dropped_archetypes", []):
                _move_to_archive(fmt, slugify(name))
            for name in diff.get("returned_archetypes", []):
                _restore_from_archive(fmt, slugify(name))
            if refresh_stale:
                ttl = _TTL_DAYS.get(fmt, 90)
                for arch in archetypes:
                    slug = slugify(arch.get("name") or "")
                    if slug and _is_stale(_live_path(fmt, slug), ttl):
                        queue.append(arch)
            # de-dup
            seen: set[str] = set()
            queue = [a for a in queue if not (slugify(a.get("name") or "") in seen or seen.add(slugify(a.get("name") or "")))]
    elif rebuild_archived:
        archive_dir = PILOTING_DIR / fmt / ARCHIVE_DIR_NAME
        if archive_dir.is_dir():
            archived_slugs = {f.stem for f in archive_dir.glob("*.json")}
            for arch in archetypes:
                slug = slugify(arch.get("name") or "")
                if slug in archived_slugs:
                    queue.append(arch)
                    _restore_from_archive(fmt, slug)
    elif refresh_stale:
        ttl = _TTL_DAYS.get(fmt, 90)
        for arch in archetypes:
            slug = slugify(arch.get("name") or "")
            if slug and _is_stale(_live_path(fmt, slug), ttl):
                queue.append(arch)
    else:
        queue = list(archetypes)

    ok = 0
    for i, arch in enumerate(queue):
        if build_one(
            arch,
            fmt,
            force=force,
            enrich_from_youtube=enrich_from_youtube,
            all_archetypes=archetypes,
            merge_legacy_guidance=merge_legacy_guidance,
            write_profile=write_profiles,
        ):
            ok += 1
        if i < len(queue) - 1:
            time.sleep(2)
    return ok


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Generate piloting guides.")
    parser.add_argument("formats", nargs="*", help="format slugs (default: all metagame files)")
    parser.add_argument("--archetype", help="only this archetype (use with one format)")
    parser.add_argument("--force", action="store_true", help="regenerate guides that already exist")
    parser.add_argument(
        "--from-diff",
        action="store_true",
        help="consume metagame_data/<fmt>.diff.json (new -> build, dropped -> archive, returned -> restore)",
    )
    parser.add_argument(
        "--rebuild-archived",
        action="store_true",
        help="rebuild every archived guide for the format and restore it to live",
    )
    parser.add_argument(
        "--refresh-stale",
        action="store_true",
        help="rebuild guides older than the format TTL (standard/historic 30d, others longer)",
    )
    parser.add_argument(
        "--enrich-from-youtube",
        action="store_true",
        help="after building each guide, append gameplay heuristics extracted from cached YouTube videos",
    )
    parser.add_argument(
        "--merge-legacy-guidance",
        action="store_true",
        help="merge matching guidance/<fmt>.json entries into generated runtime guides",
    )
    parser.add_argument(
        "--write-profiles",
        action="store_true",
        help="also write app/knowledge/archetype_profiles/<fmt>/<slug>.json",
    )
    args = parser.parse_args(argv[1:])

    formats = args.formats or sorted(p.stem for p in METAGAME_DIR.glob("*.json") if not p.stem.endswith(".diff"))
    if args.archetype and len(formats) != 1:
        parser.error("--archetype requires exactly one format")

    total = 0
    for fmt in formats:
        total += build_format(
            fmt,
            archetype=args.archetype,
            force=args.force,
            from_diff=args.from_diff,
            rebuild_archived=args.rebuild_archived,
            refresh_stale=args.refresh_stale,
            enrich_from_youtube=args.enrich_from_youtube,
            merge_legacy_guidance=args.merge_legacy_guidance,
            write_profiles=args.write_profiles,
        )
    log.info("done: %d guide(s) written", total)
    return 0 if total else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
