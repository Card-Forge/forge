"""Graph + knowledge tests. Network calls (LLM, Scryfall) are stubbed out."""

import pytest

from app.graph import get_graph
from app.knowledge import format_detect, loader, metagame
from app.nodes import game_advisor


@pytest.fixture(autouse=True)
def _offline():
    """Keep graph tests offline: pre-seed the per-game format cache for the
    sample game so the node never makes a network call to detect the format."""
    game_advisor._resolved_format.clear()
    game_advisor._own_archetype.clear()
    game_advisor._resolved_format["t"] = "modern"
    yield
    game_advisor._resolved_format.clear()
    game_advisor._own_archetype.clear()


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
        "hand": ["Mountain", "Lightning Bolt"],
        "own_board": ["Mountain"],
        "alternatives": [],
    }


@pytest.mark.asyncio
async def test_graph_returns_archetype_and_piloting(monkeypatch, sample_state):
    calls = []

    async def fake_generate_json(prompt, system=None):
        calls.append((prompt, system))
        return {
            "archetype": "Boros Energy",
            "confidence": 0.8,
            "reasoning": "Aggressive red cards.",
            "alternatives": [],
        }

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(sample_state)
    assert result["archetype"] == "Boros Energy"
    assert result["resolved_format"] == "modern"
    assert 0.0 <= result["confidence"] <= 1.0
    assert len(calls) == 1
    # piloting outputs are present alongside recognition outputs, but local/guide-derived.
    assert result["recommended_play"]
    assert result["own_archetype"]  # identified deterministically from deck_cards


@pytest.mark.asyncio
async def test_own_archetype_identified_and_cached(monkeypatch, sample_state):
    async def fake_generate_json(prompt, system=None):
        return {"archetype": "Boros Energy", "confidence": 0.7, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    await get_graph().ainvoke(sample_state)
    # Ragavan is a Boros Energy signature card -> deterministic match, cached.
    assert "t" in game_advisor._own_archetype
    assert game_advisor._own_archetype["t"][0] == "Boros Energy"


@pytest.mark.asyncio
async def test_confidence_is_clamped(monkeypatch, sample_state):
    async def fake_generate_json(prompt, system=None):
        return {"archetype": "Boros Energy", "confidence": 5, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
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

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(sample_state)
    assert result["confidence"] <= 0.4


@pytest.mark.asyncio
async def test_llm_failure_is_fail_soft(monkeypatch, sample_state):
    async def boom(prompt, system=None):
        raise game_advisor.LLMError("server down")

    monkeypatch.setattr(game_advisor, "generate_json", boom)
    result = await get_graph().ainvoke(sample_state)
    assert result["archetype"] == "Unknown"
    assert result["confidence"] == 0.0
    assert result["recommended_play"] == ""


@pytest.mark.asyncio
async def test_metagame_reaches_the_prompt(monkeypatch, sample_state):
    """Scraped metagame archetypes + shares should be in the recognition prompt."""
    captured = {"recognition": "", "calls": 0}

    async def fake_generate_json(prompt, system=None):
        captured["calls"] += 1
        captured["recognition"] = prompt
        return {"archetype": "Boros Energy", "confidence": 0.7, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    await get_graph().ainvoke(sample_state)
    assert "%" in captured["recognition"]
    assert "metagame" in captured["recognition"].lower()
    assert "deck guidance" not in captured["recognition"].lower()
    assert captured["calls"] == 1


@pytest.mark.asyncio
async def test_prompt_keeps_recognition_and_piloting_separate(monkeypatch, sample_state):
    captured = {"recognition": "", "recognition_system": "", "calls": 0}

    async def fake_generate_json(prompt, system=None):
        captured["calls"] += 1
        captured["recognition"] = prompt
        captured["recognition_system"] = system
        return {"archetype": "Boros Energy", "confidence": 0.7, "reasoning": "", "alternatives": []}

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    await get_graph().ainvoke(sample_state)

    assert "identify only the human opponent" in captured["recognition_system"].lower()
    assert "use only the observed plays above" in captured["recognition"].lower()
    assert "deck guidance" not in captured["recognition"].lower()
    assert "your deck:" not in captured["recognition"].lower()
    assert "ai hand:" not in captured["recognition"].lower()
    assert "ai battlefield:" not in captured["recognition"].lower()
    assert captured["calls"] == 1


@pytest.mark.asyncio
async def test_recognition_prompt_excludes_ai_state_when_ai_is_dimir(monkeypatch):
    state = {
        "game_id": "standard-flip",
        "format": "Standard",
        "turn": 4,
        "observations": [
            {
                "turn": 1,
                "event": "land",
                "card": "Spirebluff Canal",
                "cmc": 0,
                "colors": ["U", "R"],
                "types": ["Land"],
            },
            {
                "turn": 2,
                "event": "spell",
                "card": "Artist's Talent",
                "cmc": 2,
                "colors": ["R"],
                "types": ["Enchantment"],
            },
            {
                "turn": 3,
                "event": "spell",
                "card": "Firebending Lesson",
                "cmc": 2,
                "colors": ["R"],
                "types": ["Sorcery"],
            },
        ],
        "deck_cards": ["Doomsday Excruciator", "Bitter Triumph", "Watery Grave"],
        "hand": ["Private AI Card"],
        "own_board": ["Private AI Permanent"],
        "opponent_board": ["Artist's Talent"],
        "alternatives": [],
    }
    captured = {"recognition": "", "calls": 0}

    async def fake_generate_json(prompt, system=None):
        captured["calls"] += 1
        captured["recognition"] = prompt
        return {
            "archetype": "Izzet Lessons",
            "confidence": 0.8,
            "reasoning": "Observed Izzet Lessons cards.",
            "alternatives": [],
        }

    monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
    result = await get_graph().ainvoke(state)

    assert result["archetype"] == "Izzet Lessons"
    assert "private ai card" not in captured["recognition"].lower()
    assert "private ai permanent" not in captured["recognition"].lower()
    assert "firebending lesson" in captured["recognition"].lower()
    assert captured["calls"] == 1


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
