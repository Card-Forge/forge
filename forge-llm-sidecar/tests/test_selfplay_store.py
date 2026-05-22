"""Tests for the SQLite self-play results store and /api/selfplay/trends."""

import pytest
from httpx import ASGITransport, AsyncClient

from app import selfplay_store


def _games(won_turns):
    """Build per-seat records: won_turns is a list of win_turn ints (None = loss)."""
    out = []
    for wt in won_turns:
        if wt is None:
            out.append(
                {
                    "archetype": "Ruby Storm",
                    "opponent": "60 Islands",
                    "pilot_mode": "solve",
                    "won": False,
                    "turns": 12,
                }
            )
        else:
            out.append(
                {
                    "archetype": "Ruby Storm",
                    "opponent": "60 Islands",
                    "pilot_mode": "solve",
                    "won": True,
                    "win_turn": wt,
                    "turns": wt,
                }
            )
    return out


class TestStore:
    def test_insert_round_trips_games(self, tmp_path):
        db = tmp_path / "results.db"
        conn = selfplay_store.connect(db)
        run_id = selfplay_store.insert_run(
            conn,
            records=_games([4, 5, None]),
            format="modern",
            config="goldfish",
            learnings_version="0:0:0",
            created_at="2026-01-01T00:00:00+00:00",
        )
        assert run_id == 1
        rows = conn.execute("SELECT COUNT(*) AS n FROM games WHERE run_id=?", (run_id,)).fetchone()
        assert rows["n"] == 3

    def test_trend_is_time_ordered(self, tmp_path):
        conn = selfplay_store.connect(tmp_path / "r.db")
        selfplay_store.insert_run(
            conn,
            records=_games([6, 6]),
            format="modern",
            learnings_version="v1",
            created_at="2026-01-01T00:00:00+00:00",
        )
        selfplay_store.insert_run(
            conn,
            records=_games([4, 4]),
            format="modern",
            learnings_version="v2",
            created_at="2026-02-01T00:00:00+00:00",
        )
        groups = selfplay_store.archetype_trend(conn, "Ruby Storm")
        assert len(groups) == 1
        series = groups[0]["runs"]
        assert [r["learnings_version"] for r in series] == ["v1", "v2"]
        assert series[0]["mean_win_turn"] == 6.0
        assert series[1]["mean_win_turn"] == 4.0

    def test_baseline_vs_latest_deltas(self, tmp_path):
        conn = selfplay_store.connect(tmp_path / "r.db")
        # baseline: 50% win, mean turn 6; latest: 100% win, mean turn 4
        selfplay_store.insert_run(
            conn,
            records=_games([6, None]),
            created_at="2026-01-01T00:00:00+00:00",
        )
        selfplay_store.insert_run(
            conn,
            records=_games([4, 4]),
            created_at="2026-02-01T00:00:00+00:00",
        )
        rows = selfplay_store.baseline_vs_latest(conn)
        assert len(rows) == 1
        row = rows[0]
        assert row["baseline"]["win_rate"] == 0.5
        assert row["latest"]["win_rate"] == 1.0
        assert row["delta_win_rate"] == 0.5
        assert row["delta_mean_win_turn"] == -2.0  # faster is improvement
        assert row["n_runs"] == 2

    def test_label_baseline_pin_overrides_earliest(self, tmp_path):
        conn = selfplay_store.connect(tmp_path / "r.db")
        # earliest run is NOT the pinned baseline
        selfplay_store.insert_run(
            conn,
            records=_games([8, 8]),
            created_at="2026-01-01T00:00:00+00:00",
        )
        selfplay_store.insert_run(
            conn,
            records=_games([6, 6]),
            label="baseline",
            created_at="2026-02-01T00:00:00+00:00",
        )
        selfplay_store.insert_run(
            conn,
            records=_games([4, 4]),
            created_at="2026-03-01T00:00:00+00:00",
        )
        row = selfplay_store.baseline_vs_latest(conn)[0]
        assert row["baseline"]["mean_win_turn"] == 6.0  # the pinned one, not 8.0
        assert row["latest"]["mean_win_turn"] == 4.0


@pytest.mark.asyncio
async def test_trends_endpoint_empty_without_db(monkeypatch, tmp_path):
    monkeypatch.setenv("FORGE_SELFPLAY_DB", str(tmp_path / "missing.db"))
    from app.main import app

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get("/api/selfplay/trends")
        assert resp.status_code == 200
        assert resp.json() == {"groups": []}


@pytest.mark.asyncio
async def test_record_endpoint_persists_and_shows_in_trends(monkeypatch, tmp_path):
    monkeypatch.setenv("FORGE_SELFPLAY_DB", str(tmp_path / "results.db"))
    from app.main import app

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/selfplay/record",
            json={
                "records": _games([4, 5]),
                "format": "modern",
                "config": "goldfish",
                "label": "baseline",
                "source_file": "runs/ruby.jsonl",
            },
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["run_id"] == 1
        assert data["n_games"] == 2
        assert data["n_wins"] == 2

        trends = await client.get("/api/selfplay/trends")
        groups = trends.json()["groups"]
        assert len(groups) == 1
        assert groups[0]["baseline"]["label"] == "baseline"


@pytest.mark.asyncio
async def test_trends_endpoint_returns_seeded_groups(monkeypatch, tmp_path):
    db = tmp_path / "results.db"
    monkeypatch.setenv("FORGE_SELFPLAY_DB", str(db))
    conn = selfplay_store.connect(db)
    selfplay_store.insert_run(conn, records=_games([5, 5]), created_at="2026-01-01T00:00:00+00:00")
    conn.close()

    from app.main import app

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get("/api/selfplay/trends")
        assert resp.status_code == 200
        groups = resp.json()["groups"]
        assert len(groups) == 1
        assert groups[0]["archetype"] == "Ruby Storm"
