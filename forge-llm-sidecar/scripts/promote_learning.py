#!/usr/bin/env python3
"""Human-in-the-loop promotion of staged self-play lessons into curated guides.

Self-play reflection auto-stages lessons to the learnings layer
(``app/knowledge/learnings/<format>/<slug>.json``). They are injected at
runtime conditionally and capped, but they never touch the hand-authored
piloting guides until a human promotes them here — rewriting each to a
conditional form before it lands in ``piloting/<format>/<slug>.json``.

    # see everything staged
    python -m scripts.promote_learning list
    # inspect one archetype's staged lessons + the guide it would merge into
    python -m scripts.promote_learning show modern ruby-storm
    # promote lesson #0, rewriting it to a conditional sequencing tip
    python -m scripts.promote_learning promote modern ruby-storm 0 \
        --as "If the opponent has shown no interaction, chain rituals for a turn-3 kill." --yes
    # discard a staged lesson without promoting it
    python -m scripts.promote_learning drop modern ruby-storm 0 --yes

The human gate is the ``--as`` rewrite plus the confirmation prompt (skippable
with ``--yes`` for scripted use).
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app.knowledge import learnings as L  # noqa: E402
from app.knowledge.piloting import slugify  # noqa: E402

_PILOTING_DIR = pathlib.Path(__file__).resolve().parent.parent / "app" / "knowledge" / "piloting"
_PROMOTABLE_FIELDS = ("sequencing_tips", "common_threats", "win_conditions")


def _render(lesson) -> str:
    trigger = f"when {lesson.trigger}, " if lesson.trigger else ""
    return f"{trigger}{lesson.recommendation}"


def _guide_path(fmt: str, slug: str) -> pathlib.Path:
    return _PILOTING_DIR / fmt.lower() / f"{slugify(slug)}.json"


def cmd_list(args: argparse.Namespace) -> int:
    staged = L.all_staged()
    if args.format:
        staged = [s for s in staged if s[0] == args.format.lower()]
    if not staged:
        print("No staged learnings.")
        return 0
    for fmt, slug, lessons in staged:
        print(f"{fmt}/{slug}  ({len(lessons)} lesson(s))")
        for i, lsn in enumerate(lessons):
            print(f"  [{i}] conf={lsn.confidence:.2f} n={lsn.evidence.n_games} "
                  f"ctx={lsn.context}  {_render(lsn)}")
    return 0


def cmd_show(args: argparse.Namespace) -> int:
    lessons = L.get_learnings(args.format, args.archetype)
    if not lessons:
        print(f"No staged learnings for {args.format}/{slugify(args.archetype)}.")
        return 1
    print(f"Staged lessons for {args.format}/{slugify(args.archetype)}:")
    for i, lsn in enumerate(lessons):
        print(f"  [{i}] conf={lsn.confidence:.2f} n={lsn.evidence.n_games} "
              f"delta={lsn.evidence.turns_to_win_delta} ctx={lsn.context}")
        print(f"       {_render(lsn)}")
    gp = _guide_path(args.format, args.archetype)
    if gp.exists():
        guide = json.loads(gp.read_text(encoding="utf-8"))
        print(f"\nCurated guide: {gp}")
        print(f"  current {args.field}: {guide.get(args.field) or '(none)'}")
    else:
        print(f"\nNo curated guide at {gp} — promotion will create the field on a new file is not supported; create the guide first.")
    return 0


def _confirm(prompt: str, assume_yes: bool) -> bool:
    if assume_yes:
        return True
    if not sys.stdin.isatty():
        print("Refusing to promote without --yes in non-interactive mode.", file=sys.stderr)
        return False
    return input(f"{prompt} [y/N] ").strip().lower() in {"y", "yes"}


def cmd_promote(args: argparse.Namespace) -> int:
    lessons = L.get_learnings(args.format, args.archetype)
    if args.index < 0 or args.index >= len(lessons):
        print(f"Index {args.index} out of range (0..{len(lessons) - 1}).", file=sys.stderr)
        return 1
    lesson = lessons[args.index]
    text = args.as_text or _render(lesson)

    gp = _guide_path(args.format, args.archetype)
    if not gp.exists():
        print(f"Curated guide not found: {gp}", file=sys.stderr)
        return 1
    guide = json.loads(gp.read_text(encoding="utf-8"))
    field = args.field
    bucket = guide.get(field)
    if not isinstance(bucket, list):
        print(f"Guide field '{field}' is not a list — cannot promote into it.", file=sys.stderr)
        return 1

    print(f"Promote into {gp.name}:{field}")
    print(f"  staged : {_render(lesson)}")
    print(f"  as     : {text}")
    if not _confirm("Promote this lesson?", args.yes):
        print("Aborted.")
        return 1
    if text not in bucket:
        bucket.append(text)
        gp.write_text(json.dumps(guide, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"Promoted into {gp}")
    else:
        print("Already present in the guide; not duplicating.")

    remaining = lessons[: args.index] + lessons[args.index + 1 :]
    L.replace_lessons(args.format, args.archetype, remaining)
    print(f"Cleared staged lesson [{args.index}]; {len(remaining)} remain.")
    return 0


def cmd_drop(args: argparse.Namespace) -> int:
    lessons = L.get_learnings(args.format, args.archetype)
    if args.index < 0 or args.index >= len(lessons):
        print(f"Index {args.index} out of range (0..{len(lessons) - 1}).", file=sys.stderr)
        return 1
    if not _confirm(f"Drop staged lesson [{args.index}] ({_render(lessons[args.index])})?", args.yes):
        print("Aborted.")
        return 1
    remaining = lessons[: args.index] + lessons[args.index + 1 :]
    L.replace_lessons(args.format, args.archetype, remaining)
    print(f"Dropped; {len(remaining)} remain.")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description="Promote staged self-play lessons into curated guides.")
    sub = ap.add_subparsers(dest="cmd", required=True)

    p_list = sub.add_parser("list", help="List all staged learnings")
    p_list.add_argument("--format", default="", help="Filter to one format")
    p_list.set_defaults(func=cmd_list)

    p_show = sub.add_parser("show", help="Show one archetype's staged lessons + guide")
    p_show.add_argument("format")
    p_show.add_argument("archetype")
    p_show.add_argument("--field", default="sequencing_tips", choices=_PROMOTABLE_FIELDS)
    p_show.set_defaults(func=cmd_show)

    p_prom = sub.add_parser("promote", help="Promote a lesson into a curated guide")
    p_prom.add_argument("format")
    p_prom.add_argument("archetype")
    p_prom.add_argument("index", type=int)
    p_prom.add_argument("--as", dest="as_text", default="",
                        help="Conditional rewrite to store in the guide (defaults to the rendered lesson)")
    p_prom.add_argument("--field", default="sequencing_tips", choices=_PROMOTABLE_FIELDS)
    p_prom.add_argument("--yes", action="store_true", help="Skip the confirmation prompt")
    p_prom.set_defaults(func=cmd_promote)

    p_drop = sub.add_parser("drop", help="Discard a staged lesson without promoting")
    p_drop.add_argument("format")
    p_drop.add_argument("archetype")
    p_drop.add_argument("index", type=int)
    p_drop.add_argument("--yes", action="store_true", help="Skip the confirmation prompt")
    p_drop.set_defaults(func=cmd_drop)

    args = ap.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
