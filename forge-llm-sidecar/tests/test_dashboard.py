"""Tests for the in-memory request store and dashboard endpoints."""

import pytest
from httpx import ASGITransport, AsyncClient

from app.store import RequestStore


class TestRequestStore:
    """Unit tests for the RequestStore class."""

    def test_initial_state(self):
        store = RequestStore()
        assert store.total_requests == 0
        assert store.history == []
        assert store.last_entry is None
        assert store.uptime_seconds >= 0

    def test_record_single_entry(self):
        store = RequestStore()
        store.record({"game_id": "g1", "archetype": "Boros Energy", "confidence": 0.8})
        assert store.total_requests == 1
        assert len(store.history) == 1
        assert store.history[0]["game_id"] == "g1"
        assert store.last_entry["game_id"] == "g1"

    def test_record_multiple_entries(self):
        store = RequestStore()
        for i in range(5):
            store.record({"game_id": f"g{i}", "archetype": f"Deck {i}", "confidence": 0.5})
        assert store.total_requests == 5
        assert len(store.history) == 5

    def test_max_history_limit(self):
        store = RequestStore()
        for i in range(30):
            store.record({"game_id": f"g{i}", "archetype": f"Deck {i}", "confidence": 0.5})
        assert store.total_requests == 30
        assert len(store.history) == 20
        assert store.history[0]["game_id"] == "g10"
        assert store.history[-1]["game_id"] == "g29"

    def test_timestamp_present(self):
        store = RequestStore()
        store.record({"game_id": "g1"})
        assert "timestamp" in store.history[0]
        assert store.history[0]["timestamp"] > 0

    def test_history_returns_copy(self):
        store = RequestStore()
        store.record({"game_id": "g1"})
        hist = store.history
        hist.clear()
        assert len(store.history) == 1

    def test_last_entry_none_when_empty(self):
        store = RequestStore()
        assert store.last_entry is None

    def test_last_entry_returns_latest(self):
        store = RequestStore()
        store.record({"game_id": "g1"})
        store.record({"game_id": "g2"})
        assert store.last_entry["game_id"] == "g2"


class TestStatsEndpoint:
    """Integration tests for /api/stats via FastAPI test client."""

    @pytest.fixture
    def app(self):
        from app.main import app
        return app

    @pytest.mark.asyncio
    async def test_stats_returns_uptime_and_counts(self, app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.get("/api/stats")
            assert resp.status_code == 200
            data = resp.json()
            assert "uptime_seconds" in data
            assert "total_requests" in data
            assert "history" in data
            assert data["total_requests"] == 0
            assert data["history"] == []

    @pytest.mark.asyncio
    async def test_stats_counts_increments_after_recognize(self, app, monkeypatch):
        """After a /recognize call, stats should reflect the new request."""
        from app.nodes import game_advisor

        async def fake_generate_json(prompt, system=None):
            return {
                "archetype": "Boros Energy",
                "confidence": 0.8,
                "reasoning": "Test reasoning.",
                "alternatives": [],
                "recommended_play": "Test play.",
                "play_reasoning": "Test.",
                "play_alternatives": [],
            }

        monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
        game_advisor._resolved_format.clear()
        game_advisor._own_archetype.clear()
        game_advisor._resolved_format["test-game"] = "modern"

        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            await client.post("/recognize", json={
                "client": "test",
                "game_id": "test-game",
                "format": "Constructed",
                "turn": 3,
                "observations": [
                    {"turn": 1, "event": "land", "card": "Mountain", "cmc": 0, "colors": ["R"], "types": ["Land"]}
                ],
                "deck_cards": ["Ragavan, Nimble Pilferer", "Lightning Bolt"],
            })

            resp = await client.get("/api/stats")
            data = resp.json()
            assert data["total_requests"] >= 1
            assert len(data["history"]) >= 1
            assert data["history"][-1]["archetype"] == "Boros Energy"

    @pytest.mark.asyncio
    async def test_health_still_works(self, app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.get("/health")
            assert resp.status_code == 200
            data = resp.json()
            assert data["status"] == "ok"


class TestDashboardRoot:
    """Tests for the dashboard root route."""

    @pytest.mark.asyncio
    async def test_root_returns_html(self, app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.get("/")
            assert resp.status_code == 200
            assert "text/html" in resp.headers.get("content-type", "")
            assert "Forge LLM Sidecar" in resp.text

    @pytest.mark.asyncio
    async def test_static_files_accessible(self, app):
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            resp = await client.get("/static/dashboard.html")
            assert resp.status_code == 200
            assert "Dashboard" in resp.text
