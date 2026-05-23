"""Tests for timeout-aware self-play reflection."""

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app
from app import main as main_module


@pytest.mark.asyncio
async def test_reflect_suppresses_all_timed_out_games(monkeypatch):
    async def boom(*args, **kwargs):
        raise AssertionError("reflection LLM should be suppressed for all-timeout batches")

    monkeypatch.setattr(main_module, "generate_json", boom)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/selfplay/reflect",
            json={
                "format": "standard",
                "archetype": "Izzet Spells",
                "context": "mirror",
                "stage": True,
                "games": [{"won": False, "timed_out": True, "win_turn": None}],
            },
        )

    assert resp.status_code == 200
    data = resp.json()
    assert data["n_games"] == 1
    assert data["n_wins"] == 0
    assert data["lessons"] == []
    assert data["staged_path"] is None


@pytest.mark.asyncio
async def test_reflect_suppresses_no_win_batches(monkeypatch):
    async def boom(*args, **kwargs):
        raise AssertionError("reflection LLM should be suppressed without wins")

    monkeypatch.setattr(main_module, "generate_json", boom)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/selfplay/reflect",
            json={
                "format": "standard",
                "archetype": "Selesnya Landfall",
                "context": "mirror",
                "stage": True,
                "games": [{"won": False, "timed_out": False, "actions": ["lost on turn 7"]}],
            },
        )

    assert resp.status_code == 200
    data = resp.json()
    assert data["n_games"] == 1
    assert data["n_wins"] == 0
    assert data["lessons"] == []
    assert data["staged_path"] is None


@pytest.mark.asyncio
async def test_reflect_calls_llm_for_winning_batches(monkeypatch):
    prompts = []

    async def fake_generate_json(prompt, system=None):
        prompts.append(prompt)
        return {
            "lessons": [
                {
                    "trigger": "Opening hand has pressure and lands",
                    "recommendation": "Curve out instead of holding threats.",
                }
            ]
        }

    monkeypatch.setattr(main_module, "generate_json", fake_generate_json)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.post(
            "/selfplay/reflect",
            json={
                "format": "standard",
                "archetype": "Selesnya Landfall",
                "context": "mirror",
                "stage": False,
                "games": [{"won": True, "timed_out": False, "win_turn": 5}],
            },
        )

    assert resp.status_code == 200
    data = resp.json()
    assert data["n_wins"] == 1
    assert len(data["lessons"]) == 1
    assert "Timeouts: 0" in prompts[0]
