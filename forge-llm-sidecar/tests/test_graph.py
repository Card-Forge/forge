"""Graph + knowledge tests. Network calls (LLM, Scryfall) are stubbed out."""

import pytest
from pydantic import ValidationError

from app.graph import get_graph
from app.knowledge import format_detect, loader, metagame
from app.nodes import game_advisor
from app.schema import ActionScore, RecognitionRequest


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


# ── ActionScore & personality tests ─────────────────────────────────────────


class TestActionScore:
    """Unit tests for the ActionScore model."""

    def test_schema_accepts_valid_action(self):
        action = ActionScore(
            action_type="PLAY_SPELL",
            target="Lightning Bolt",
            percentage=85.0,
            reasoning="Best removal available.",
        )
        assert action.action_type == "PLAY_SPELL"
        assert action.target == "Lightning Bolt"
        assert action.percentage == 85.0

    def test_schema_defaults(self):
        action = ActionScore()
        assert action.action_type == ""
        assert action.target is None
        assert action.targets is None
        assert action.percentage == 0.0
        assert action.reasoning == ""

    def test_schema_allows_list_targets(self):
        action = ActionScore(
            action_type="ATTACK",
            target="all_available",
            targets=["Goblin Guide", "Monastery Swiftspear"],
            percentage=90.0,
        )
        assert action.targets == ["Goblin Guide", "Monastery Swiftspear"]

    def test_schema_rejects_bad_percentage_type(self):
        with pytest.raises(ValidationError):
            ActionScore(action_type="PLAY_SPELL", percentage="not-a-number")


class TestPersonalityInRequest:
    """Tests for personality field in RecognitionRequest."""

    def test_personality_defaults_empty(self):
        req = RecognitionRequest(game_id="g1", format="Modern")
        assert req.personality == {}

    def test_personality_accepts_traits(self):
        req = RecognitionRequest(
            game_id="g1",
            format="Modern",
            personality={"play_aggro": True, "mulligan_threshold": 5},
        )
        assert req.personality["play_aggro"] is True
        assert req.personality["mulligan_threshold"] == 5


class TestActionsInGraphOutput:
    """Tests that the graph returns structured actions in its output."""

    @pytest.mark.asyncio
    async def test_graph_returns_actions(self, monkeypatch, sample_state):
        async def fake_generate_json(prompt, system=None):
            return {
                "archetype": "Boros Energy",
                "confidence": 0.8,
                "reasoning": "Red cards.",
                "alternatives": [],
            }

        monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
        result = await get_graph().ainvoke(sample_state)
        assert "actions" in result
        assert isinstance(result["actions"], list)
        assert len(result["actions"]) > 0
        # Validate action structure
        for action in result["actions"]:
            assert "action_type" in action
            assert "percentage" in action
            assert 0.0 <= action["percentage"] <= 100.0

    @pytest.mark.asyncio
    async def test_graph_returns_mulligan_action_turn_0(self, monkeypatch):
        state = {
            "game_id": "mull-test",
            "format": "Constructed",
            "turn": 0,
            "observations": [],
            "deck_cards": ["Lightning Bolt", "Mountain"],
            "hand": ["Mountain", "Lightning Bolt", "Mountain", "Fireball"],
            "own_board": [],
            "alternatives": [],
        }
        game_advisor._resolved_format["mull-test"] = "modern"

        async def fake_generate_json(prompt, system=None):
            return {
                "archetype": "Unknown",
                "confidence": 0.0,
                "reasoning": "No plays observed yet.",
                "alternatives": [],
            }

        monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
        result = await get_graph().ainvoke(state)
        assert "actions" in result
        actions = result["actions"]
        types = {a.get("action_type") for a in actions}
        assert "MULLIGAN" in types
        # MULLIGAN should be the only action on turn 0
        assert len(actions) == 1
        assert actions[0]["action_type"] == "MULLIGAN"

    @pytest.mark.asyncio
    async def test_personality_influences_aggro_action_percentages(self, monkeypatch):
        """Aggro personality should boost ATTACK and PLAY_SPELL percentages."""
        # No opponent board / observations / graveyard so the role-derived
        # combat overrides are withheld (`has_opp_state == False`) and the
        # personality-driven base percentages drive ATTACK/BLOCK/PASS.
        aggro_state = {
            "game_id": "aggro-test",
            "format": "Constructed",
            "turn": 5,
            "observations": [],
            "deck_cards": ["Ragavan, Nimble Pilferer", "Lightning Bolt", "Mountain"],
            "hand": ["Lightning Bolt"],
            "own_board": ["Mountain", "Mountain", "Ragavan, Nimble Pilferer"],
            "opponent_board": [],
            "alternatives": [],
            "personality": {"play_aggro": True},
        }
        non_aggro_state = {
            "game_id": "control-test",
            "format": "Constructed",
            "turn": 5,
            "observations": [],
            "deck_cards": ["Counterspell", "Dig Through Time", "Island"],
            "hand": ["Counterspell"],
            "own_board": ["Island", "Island", "Snapcaster Mage"],
            "opponent_board": [],
            "alternatives": [],
            "personality": {},
        }
        game_advisor._resolved_format["aggro-test"] = "modern"
        game_advisor._resolved_format["control-test"] = "modern"

        async def fake_generate_json(prompt, system=None):
            return {
                "archetype": "Unknown",
                "confidence": 0.5,
                "reasoning": "Generic test.",
                "alternatives": [],
            }

        monkeypatch.setattr(game_advisor, "generate_json", fake_generate_json)
        aggro_result = await get_graph().ainvoke(aggro_state)
        non_aggro_result = await get_graph().ainvoke(non_aggro_state)

        aggro_actions = {a["action_type"]: a for a in aggro_result["actions"]}
        non_aggro_actions = {a["action_type"]: a for a in non_aggro_result["actions"]}

        # Aggro should have higher ATTACK percentage than non-aggro
        assert aggro_actions["ATTACK"]["percentage"] > non_aggro_actions["ATTACK"]["percentage"]
        # Aggro should have higher PLAY_SPELL percentage than non-aggro
        assert (
            aggro_actions["PLAY_SPELL"]["percentage"]
            > non_aggro_actions["PLAY_SPELL"]["percentage"]
        )
        # Aggro should have much lower PASS percentage than non-aggro
        assert aggro_actions["PASS"]["percentage"] < non_aggro_actions["PASS"]["percentage"]

    def test_actions_visible_in_dashboard_via_store(self):
        """Verify actions are included in the store recording format."""
        from app.store import RequestStore

        store = RequestStore()
        store.record(
            {
                "game_id": "g1",
                "actions": [
                    {
                        "action_type": "PLAY_SPELL",
                        "percentage": 85.0,
                        "reasoning": "Test",
                        "target": None,
                        "targets": None,
                    },
                    {
                        "action_type": "ATTACK",
                        "percentage": 60.0,
                        "reasoning": "Test",
                        "target": "all_available",
                        "targets": None,
                    },
                ],
            }
        )
        entry = store.last_entry
        assert entry is not None
        assert "actions" in entry
        assert len(entry["actions"]) == 2
        assert entry["actions"][0]["action_type"] == "PLAY_SPELL"
        assert entry["actions"][0]["percentage"] == 85.0
