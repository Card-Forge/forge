"""SQLite-backed persistent store for self-play run results.

The Java ``SelfPlayRunner`` emits per-seat JSONL (one record per sidecar seat
per game). ``scripts/record_run.py`` ingests a JSONL batch into this store,
capturing metadata the JSONL lacks — wall-clock time, format, config, the
``learnings_version()`` token, and the code revision. This turns ad-hoc run
files into a queryable history so each deck has a **baseline** and its
win-rate / turns-to-win can be tracked **over time** against learnings changes.

Two tables:
  * ``runs``  — one ingested JSONL batch (one runner invocation) + metadata.
  * ``games`` — one seat-record (won / win_turn / turns) linked to its run.

Aggregates (win rate, turns-to-win distribution) are computed in SQL so this
store is the single source of truth, matching ``selfplay_report._summarize``:
a ``win_turn`` only counts when the seat actually ``won`` and the value is
numeric.

The DB path defaults to ``<sidecar>/selfplay/results.db`` and is overridable
via the ``FORGE_SELFPLAY_DB`` env var (mirrors ``app/config.py``).
"""

from __future__ import annotations

import os
import sqlite3
import statistics
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

_DEFAULT_DB = Path(__file__).resolve().parent.parent / "selfplay" / "results.db"

_SCHEMA = """
CREATE TABLE IF NOT EXISTS runs (
    run_id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL,
    config TEXT,
    format TEXT,
    learnings_version TEXT,
    git_sha TEXT,
    source_file TEXT,
    label TEXT
);
CREATE TABLE IF NOT EXISTS games (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id INTEGER NOT NULL REFERENCES runs(run_id),
    archetype TEXT,
    opponent TEXT,
    pilot_mode TEXT,
    won INTEGER,
    win_turn INTEGER,
    turns INTEGER
);
CREATE INDEX IF NOT EXISTS idx_games_run ON games(run_id);
"""


def current_git_sha() -> str:
    """Best-effort short git revision of this repo (empty string on failure)."""
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            capture_output=True,
            text=True,
            timeout=5,
            cwd=str(Path(__file__).resolve().parent.parent),
        )
        return out.stdout.strip() if out.returncode == 0 else ""
    except (OSError, subprocess.SubprocessError):
        return ""


def current_learnings_version() -> str:
    """The learnings cache-bust token, snapshotted so runs line up with learnings state."""
    try:
        from app.knowledge.learnings import learnings_version

        return learnings_version()
    except Exception:  # noqa: BLE001 — best-effort metadata, never block a record
        return ""


def db_path(path: str | os.PathLike[str] | None = None) -> Path:
    """Resolve the DB path: explicit arg > ``FORGE_SELFPLAY_DB`` > default."""
    if path is not None:
        return Path(path)
    env = os.environ.get("FORGE_SELFPLAY_DB", "").strip()
    return Path(env) if env else _DEFAULT_DB


def db_exists(path: str | os.PathLike[str] | None = None) -> bool:
    """True if the DB file is present (used by the fail-soft endpoint)."""
    return db_path(path).exists()


def connect(path: str | os.PathLike[str] | None = None) -> sqlite3.Connection:
    """Open (creating parent dirs + schema if needed) and return a connection."""
    resolved = db_path(path)
    resolved.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(resolved))
    conn.row_factory = sqlite3.Row
    conn.executescript(_SCHEMA)
    return conn


def insert_run(
    conn: sqlite3.Connection,
    *,
    records: list[dict[str, Any]],
    config: str = "",
    format: str = "",
    learnings_version: str = "",
    git_sha: str = "",
    source_file: str = "",
    label: str = "",
    created_at: str | None = None,
) -> int:
    """Write one run row + its game rows in a single transaction.

    ``records`` are the raw per-seat dicts parsed from the runner JSONL.
    Returns the new ``run_id``.
    """
    ts = created_at or datetime.now(timezone.utc).isoformat()
    with conn:  # transaction
        cur = conn.execute(
            "INSERT INTO runs "
            "(created_at, config, format, learnings_version, git_sha, source_file, label) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (ts, config, format, learnings_version, git_sha, source_file, label),
        )
        run_id = int(cur.lastrowid)
        conn.executemany(
            "INSERT INTO games "
            "(run_id, archetype, opponent, pilot_mode, won, win_turn, turns) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            [
                (
                    run_id,
                    str(r.get("archetype", "")),
                    str(r.get("opponent", "")),
                    str(r.get("pilot_mode", "")),
                    1 if r.get("won") else 0,
                    r.get("win_turn") if isinstance(r.get("win_turn"), (int, float)) else None,
                    r.get("turns") if isinstance(r.get("turns"), (int, float)) else None,
                )
                for r in records
            ],
        )
    return run_id


def _summarize_rows(rows: list[sqlite3.Row]) -> dict[str, Any]:
    """Aggregate a group's game rows — same shape as selfplay_report._summarize."""
    n = len(rows)
    win_turns = [
        r["win_turn"] for r in rows if r["won"] and isinstance(r["win_turn"], (int, float))
    ]
    n_wins = sum(1 for r in rows if r["won"])
    return {
        "n_games": n,
        "n_wins": n_wins,
        "win_rate": round(n_wins / n, 3) if n else 0.0,
        "fastest_win": min(win_turns) if win_turns else None,
        "mean_win_turn": round(statistics.fmean(win_turns), 2) if win_turns else None,
        "median_win_turn": round(statistics.median(win_turns), 2) if win_turns else None,
        "slowest_win": max(win_turns) if win_turns else None,
    }


def _group_label(archetype: str, opponent: str, pilot_mode: str) -> str:
    return f"{archetype} / {opponent} / {pilot_mode}"


def archetype_trend(conn: sqlite3.Connection, archetype: str | None = None) -> list[dict[str, Any]]:
    """Per ``(archetype, opponent, pilot_mode)`` group, the time-ordered run series.

    Each series entry carries the run's ``created_at`` and ``learnings_version``
    alongside its aggregates, so learnings changes line up against performance.
    """
    sql = (
        "SELECT g.*, r.created_at, r.learnings_version, r.git_sha, r.label, r.config "
        "FROM games g JOIN runs r ON g.run_id = r.run_id"
    )
    params: tuple[Any, ...] = ()
    if archetype:
        sql += " WHERE g.archetype = ?"
        params = (archetype,)
    sql += " ORDER BY r.created_at, r.run_id"
    rows = conn.execute(sql, params).fetchall()

    # group_key -> run_id -> {meta, rows}
    groups: dict[tuple[str, str, str], dict[int, dict[str, Any]]] = {}
    for row in rows:
        gkey = (row["archetype"], row["opponent"], row["pilot_mode"])
        per_run = groups.setdefault(gkey, {})
        bucket = per_run.setdefault(
            row["run_id"],
            {
                "run_id": row["run_id"],
                "created_at": row["created_at"],
                "learnings_version": row["learnings_version"],
                "git_sha": row["git_sha"],
                "label": row["label"],
                "config": row["config"],
                "rows": [],
            },
        )
        bucket["rows"].append(row)

    out: list[dict[str, Any]] = []
    for gkey in sorted(groups):
        runs_series = []
        for run in sorted(groups[gkey].values(), key=lambda b: (b["created_at"], b["run_id"])):
            entry = {
                "run_id": run["run_id"],
                "created_at": run["created_at"],
                "learnings_version": run["learnings_version"],
                "git_sha": run["git_sha"],
                "label": run["label"],
                "config": run["config"],
                **_summarize_rows(run["rows"]),
            }
            runs_series.append(entry)
        out.append(
            {
                "archetype": gkey[0],
                "opponent": gkey[1],
                "pilot_mode": gkey[2],
                "label": _group_label(*gkey),
                "runs": runs_series,
            }
        )
    return out


def baseline_vs_latest(conn: sqlite3.Connection) -> list[dict[str, Any]]:
    """Per group, the baseline run vs the latest run, with deltas.

    Baseline = the run flagged ``label='baseline'`` (earliest such), else the
    earliest run for the group. The headline view for the dashboard panel and
    the default CLI report.
    """
    out: list[dict[str, Any]] = []
    for group in archetype_trend(conn):
        series = group["runs"]
        if not series:
            continue
        pinned = [r for r in series if (r.get("label") or "").strip().lower() == "baseline"]
        baseline = pinned[0] if pinned else series[0]
        latest = series[-1]

        def _delta(key: str, base=baseline, last=latest) -> float | None:
            b, latest_v = base.get(key), last.get(key)
            if b is None or latest_v is None:
                return None
            return round(latest_v - b, 3)

        out.append(
            {
                "archetype": group["archetype"],
                "opponent": group["opponent"],
                "pilot_mode": group["pilot_mode"],
                "label": group["label"],
                "n_runs": len(series),
                "baseline": baseline,
                "latest": latest,
                "delta_win_rate": _delta("win_rate"),
                "delta_mean_win_turn": _delta("mean_win_turn"),
                "win_rate_series": [r["win_rate"] for r in series],
            }
        )
    return out
