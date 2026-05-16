"""Graph + knowledge tests. Network calls (LLM, Scryfall) are stubbed out."""

import pytest

from app.graph import get_graph
from app.knowledge import format_detect, loader, metagame
from app.nodes import deck_recognition


@pytest.fixture(autouse=True)
def _offline():
    """Keep graph tests offline: pre-seed the per-game format cache for the
    sample game so the node never makes a network call to detect the format."""
    deck_recognition._resolved_format.clear()
    deck_recognition._resolved_format["t"] = "modern"
    yield
    deck_recognition._resolved_format.clear()


@pytest.fixture
def sample_state():
    return {
        "game_id": "t",
        "format": "Constructed",
        "turn": 3,
        "observations": [
            {
                "turn": 1,
                "event": "land",
                "card": "Steam Vents",
                "cmc": 0,
                "colors": ["U", "R"],
                "types": ["Land"],
            },
            {
                "turn": 2,
                "event": "spell",
                "card": "Monastery Swiftspear",
                "cmc": 1,
                "colors": ["R"],
                "types": ["Creature"],
            },
        ],
        "deck_cards": ["Lightning Bolt", "Ragavan, Nimble Pilferer"],
        "alternatives": [],
    }


@pytest.mark.asyncio
async def test_graph_returns_archetype(monkeypatch, sample_state):
    async def fake_generate_json(prompt, system=None):
        return {
            "archetype": "Boros Energy",
            "confidence": 0.8,
            "reasoning": "Aggressive red cards.",
            "alternatives": [],
        }

    monkeypatch.setattr(deck_recognition, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(sample_state)
    assert result["archetype"] == "Boros Energy"
    assert result["resolved_format"] == "modern"
    assert 0.0 <= result["confidence"] <= 1.0


@pytest.mark.asyncio
async def test_confidence_is_clamped(monkeypatch, sample_state):
    async def fake_generate_json(prompt, system=None):
        return {"archetype": "Boros Energy", "confidence": 5, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(deck_recognition, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(sample_state)
    assert result["confidence"] == 1.0


@pytest.mark.asyncio
async def test_unknown_archetype_confidence_capped(monkeypatch, sample_state):
    async def fake_generate_json(prompt, system=None):
        return {
            "archetype": "Totally Made Up Brew",
            "confidence": 0.9,
            "reasoning": "",
            "alternatives": [],
        }

    monkeypatch.setattr(deck_recognition, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(sample_state)
    assert result["confidence"] <= 0.4


@pytest.mark.asyncio
async def test_metagame_reaches_the_prompt(monkeypatch, sample_state):
    """Scraped metagame archetypes + shares should be in the LLM prompt."""
    captured = {}

    async def fake_generate_json(prompt, system=None):
        captured["prompt"] = prompt
        return {"archetype": "Boros Energy", "confidence": 0.7, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(deck_recognition, "generate_json", fake_generate_json)
    await get_graph().ainvoke(sample_state)
    assert "%" in captured["prompt"]
    assert "metagame" in captured["prompt"].lower()


def test_metagame_data_files_load():
    for slug in ("modern", "standard", "legacy", "commander"):
        archetypes = metagame.get_metagame(slug)
        assert archetypes, f"no metagame data for {slug}"
        assert all("name" in a for a in archetypes)


def test_resolve_meta_format():
    assert metagame.resolve_meta_format("Modern") == "modern"
    assert metagame.resolve_meta_format("Commander") == "commander"
    # generic Forge "Constructed" is ambiguous -> None (triggers detection)
    assert metagame.resolve_meta_format("Constructed") is None


@pytest.mark.asyncio
async def test_format_detect_narrowest(monkeypatch):
    """A card pool legal only in Legacy should resolve to 'legacy'."""
    format_detect._legalities_cache.update(
        {
            "Brainstorm": {
                "standard": "not_legal",
                "pioneer": "not_legal",
                "modern": "not_legal",
                "legacy": "legal",
                "vintage": "legal",
            },
            "Ponder": {
                "standard": "not_legal",
                "pioneer": "not_legal",
                "modern": "not_legal",
                "legacy": "legal",
                "vintage": "legal",
            },
        }
    )

    async def no_fetch(names):
        return None

    monkeypatch.setattr(format_detect, "_fetch_legalities", no_fetch)
    assert await format_detect.infer_format(["Brainstorm", "Ponder"]) == "legacy"


def test_merge_keeps_unmatched_curated():
    live = [
        {
            "name": "Boros Energy",
            "meta_share": 18.2,
            "colors": ["W", "R"],
            "signature_cards": ["Ragavan, Nimble Pilferer"],
        }
    ]
    curated = loader.get_archetypes("modern")
    merged = loader.merge_with_curated(live, curated)
    names = {a["name"] for a in merged}
    assert "Boros Energy" in names
    assert len(merged) >= len(curated)
