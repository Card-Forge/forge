#!/usr/bin/env python3
"""Report self-play performance over time from the persistent results store.

Where ``selfplay_report.py`` aggregates a pile of JSONL files in one shot, this
reads ``selfplay/results.db`` (populated by ``scripts/record_run.py``) and adds
the *time* dimension: each deck's **baseline** vs its **latest** run, the win-%
and turns-to-win deltas, and — per run — the ``learnings_version`` token so
learnings changes line up against performance.

    python -m scripts.selfplay_trends                          # baseline-vs-latest, all decks
    python -m scripts.selfplay_trends --archetype "Ruby Storm" # full per-run series for one deck
    python -m scripts.selfplay_trends --json
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))

from app import selfplay_store  # noqa: E402


def _fmt(v: object, default: str = "-") -> str:
    return default if v is None else str(v)


def _fmt_delta(v: float | None) -> str:
    if v is None:
        return "-"
    return f"+{v}" if v > 0 else str(v)


def _print_overview(rows: list[dict]) -> None:
    if not rows:
        print("No runs recorded yet. Ingest one with `python -m scripts.record_run ...`.")
        return
    label_w = max(max(len(r["label"]) for r in rows), len("deck / opponent / mode"))
    header = (
        f"{'deck / opponent / mode':<{label_w}}  runs  base_win%  last_win%  Δwin%  "
        f"base_turn  last_turn  Δturn"
    )
    print(header)
    print("-" * len(header))
    for r in rows:
        b, latest = r["baseline"], r["latest"]
        dwin = r["delta_win_rate"]
        print(
            f"{r['label']:<{label_w}}  {r['n_runs']:>4}  "
            f"{b['win_rate'] * 100:>8.0f}  {latest['win_rate'] * 100:>8.0f}  "
            f"{_fmt_delta(round(dwin * 100, 1) if dwin is not None else None):>5}  "
            f"{_fmt(b['mean_win_turn']):>9}  {_fmt(latest['mean_win_turn']):>9}  "
            f"{_fmt_delta(r['delta_mean_win_turn']):>6}"
        )


def _print_series(groups: list[dict], archetype: str) -> None:
    matched = [g for g in groups if g["archetype"] == archetype]
    if not matched:
        print(f"No runs recorded for archetype {archetype!r}.")
        return
    for g in matched:
        print(f"\n{g['label']}")
        header = "  created_at                       runs/games  win%  mean_turn  learnings_version"
        print(header)
        print("  " + "-" * (len(header) - 2))
        for run in g["runs"]:
            pin = "  *baseline" if (run.get("label") or "").lower() == "baseline" else ""
            print(
                f"  {run['created_at']:<32}  {run['n_games']:>10}  "
                f"{run['win_rate'] * 100:>4.0f}  {_fmt(run['mean_win_turn']):>9}  "
                f"{_fmt(run['learnings_version']):<18}{pin}"
            )


def main() -> int:
    ap = argparse.ArgumentParser(description="Report self-play trends over time.")
    ap.add_argument("--archetype", default=None, help="Show the full per-run series for one deck")
    ap.add_argument(
        "--db", default=None, help="DB path (default: FORGE_SELFPLAY_DB or selfplay/results.db)"
    )
    ap.add_argument("--json", action="store_true", help="Emit JSON instead of a table")
    args = ap.parse_args()

    if not selfplay_store.db_exists(args.db):
        if args.json:
            print(json.dumps({"groups": []}, indent=2))
        else:
            print("No results DB yet. Ingest a run with `python -m scripts.record_run ...`.")
        return 0

    conn = selfplay_store.connect(args.db)
    try:
        if args.archetype:
            groups = selfplay_store.archetype_trend(conn, args.archetype)
            if args.json:
                print(json.dumps({"groups": groups}, indent=2))
            else:
                _print_series(groups, args.archetype)
        else:
            rows = selfplay_store.baseline_vs_latest(conn)
            if args.json:
                print(json.dumps({"groups": rows}, indent=2))
            else:
                _print_overview(rows)
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
