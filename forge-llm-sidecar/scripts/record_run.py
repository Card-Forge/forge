#!/usr/bin/env python3
"""Ingest a self-play JSONL batch into the persistent results store.

The Java runner keeps emitting per-seat JSONL as the raw artifact; this script
records one runner invocation into ``selfplay/results.db``, capturing the
metadata the JSONL lacks: wall-clock time, format, config, the current
``learnings_version()`` token, and the code revision. That turns ad-hoc run
files into a queryable history (see ``scripts/selfplay_trends.py``).

    python -m scripts.record_run runs/ruby.jsonl --format modern --config goldfish
    python -m scripts.record_run runs/baseline.jsonl --format standard --label baseline
"""

from __future__ import annotations

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app import selfplay_store  # noqa: E402
from scripts.selfplay_report import _iter_records  # noqa: E402


def main() -> int:
    ap = argparse.ArgumentParser(description="Record a self-play run into the results DB.")
    ap.add_argument("paths", nargs="+", help="JSONL files, globs, or directories")
    ap.add_argument("--format", default="", help="Game format, e.g. modern (JSONL omits it)")
    ap.add_argument("--config", default="", help='Runner config: "goldfish" or "mirror"')
    ap.add_argument("--label", default="", help='Optional note; use "baseline" to pin a baseline')
    ap.add_argument(
        "--db", default=None, help="DB path (default: FORGE_SELFPLAY_DB or selfplay/results.db)"
    )
    args = ap.parse_args()

    records = list(_iter_records(args.paths))
    if not records:
        print("No records found to ingest.", file=sys.stderr)
        return 1

    conn = selfplay_store.connect(args.db)
    try:
        run_id = selfplay_store.insert_run(
            conn,
            records=records,
            config=args.config,
            format=args.format,
            learnings_version=selfplay_store.current_learnings_version(),
            git_sha=selfplay_store.current_git_sha(),
            source_file=" ".join(args.paths),
            label=args.label,
        )
    finally:
        conn.close()

    n_wins = sum(1 for r in records if r.get("won"))
    label = f" [{args.label}]" if args.label else ""
    print(
        f"Recorded run #{run_id}{label}: {len(records)} games, {n_wins} wins "
        f"({args.format or '?'} / {args.config or '?'}) -> {selfplay_store.db_path(args.db)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
