#!/usr/bin/env python3
"""Aggregate self-play run metrics from the runner's per-seat JSONL.

The Java self-play runner emits one JSON object per *sidecar seat per game*
(goldfish = one record/game; mirror = two), with at least these fields:

    {"archetype": "Ruby Storm", "opponent": "60 Islands",
     "pilot_mode": "solve", "won": true, "win_turn": 3}

This script groups those records by (archetype, opponent, pilot_mode) and
reports the turns-to-win distribution and win rate per group — the headline
metric for the whole goldfishing effort.

    python -m scripts.selfplay_report runs/*.jsonl
    python -m scripts.selfplay_report runs/ --json
    python -m scripts.selfplay_report runs/ --by archetype
"""

from __future__ import annotations

import argparse
import glob
import json
import pathlib
import statistics
import sys
from collections import defaultdict

_GROUP_KEYS = {
    "archetype": ("archetype",),
    "opponent": ("archetype", "opponent"),
    "mode": ("archetype", "opponent", "pilot_mode"),
}


def _iter_records(paths: list[str]):
    files: list[pathlib.Path] = []
    for p in paths:
        path = pathlib.Path(p)
        if path.is_dir():
            files.extend(sorted(path.glob("*.jsonl")))
        else:
            files.extend(pathlib.Path(g) for g in glob.glob(p))
    for f in files:
        try:
            text = f.read_text(encoding="utf-8")
        except OSError as exc:
            print(f"warning: cannot read {f}: {exc}", file=sys.stderr)
            continue
        for line in text.splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                yield json.loads(line)
            except ValueError:
                print(f"warning: skipping bad JSON line in {f}", file=sys.stderr)


def _summarize(records: list[dict]) -> dict:
    n = len(records)
    win_turns = [
        r["win_turn"]
        for r in records
        if r.get("won") and isinstance(r.get("win_turn"), (int, float))
    ]
    n_wins = len(win_turns)
    return {
        "n_games": n,
        "n_wins": n_wins,
        "win_rate": round(n_wins / n, 3) if n else 0.0,
        "fastest_win": min(win_turns) if win_turns else None,
        "mean_win_turn": round(statistics.fmean(win_turns), 2) if win_turns else None,
        "median_win_turn": round(statistics.median(win_turns), 2) if win_turns else None,
        "slowest_win": max(win_turns) if win_turns else None,
    }


def main() -> int:
    ap = argparse.ArgumentParser(description="Report self-play turns-to-win metrics.")
    ap.add_argument("paths", nargs="+", help="JSONL files, globs, or directories")
    ap.add_argument("--by", default="mode", choices=sorted(_GROUP_KEYS),
                    help="Grouping granularity (default: mode)")
    ap.add_argument("--json", action="store_true", help="Emit JSON instead of a table")
    args = ap.parse_args()

    keys = _GROUP_KEYS[args.by]
    groups: dict[tuple, list[dict]] = defaultdict(list)
    for rec in _iter_records(args.paths):
        groups[tuple(str(rec.get(k, "")) for k in keys)].append(rec)

    if not groups:
        print("No records found.", file=sys.stderr)
        return 1

    rows = []
    for gkey in sorted(groups):
        summary = _summarize(groups[gkey])
        rows.append({**dict(zip(keys, gkey)), **summary})

    if args.json:
        print(json.dumps(rows, indent=2))
        return 0

    label_w = max(len(" / ".join(str(r[k]) for k in keys)) for r in rows)
    label_w = max(label_w, len(" / ".join(keys)))
    header = f"{' / '.join(keys):<{label_w}}  games  wins  win%  fastest  mean  median"
    print(header)
    print("-" * len(header))
    for r in rows:
        label = " / ".join(str(r[k]) for k in keys)
        print(f"{label:<{label_w}}  {r['n_games']:>5}  {r['n_wins']:>4}  "
              f"{r['win_rate'] * 100:>4.0f}  {str(r['fastest_win'] or '-'):>7}  "
              f"{str(r['mean_win_turn'] or '-'):>4}  {str(r['median_win_turn'] or '-'):>6}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
