"""Scenario tests for legal actions that originate outside the AI hand.

These are sidecar-level game-state scenarios: Forge is responsible for
enumerating only legal actions, and the strategist must be able to choose the
right out-of-hand action from that list without hallucinating a card or zone.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from app.nodes import opponent_strategist as strat


KNOWLEDGE_DIR = Path(__file__).resolve().parents[1] / "app" / "knowledge"


WEIRD_MECHANIC_REPRESENTATIVES = {
    "Adventure": "Beanstalk Giant",
    "Companion": "Lurrus of the Dream-Den",
    "Escape": "Cling to Dust",
    "Flashback": "Past in Flames",
    "Foretell": "Behold the Multiverse",
    "Harmonize": "Winternight Stories",
    "OutsideGameWish": "Karn, the Great Creator",
    "Plot": "Slickshot Show-Off",
    "Rebound": "Ephemerate",
    "Retrace": "Throes of Chaos",
    "Suspend": "Crashing Footfalls",
    "Unearth": "Cityscape Leveler",
    "Warp": "Quantum Riddler",
}


def _load_imported_cards() -> dict[str, dict]:
    cache = json.loads((KNOWLEDGE_DIR / "card_cache" / "cards.json").read_text())["cards"]
    by_name = {card["name"].lower(): card for card in cache.values()}
    imported: dict[str, dict] = {}
    for deck_file in (KNOWLEDGE_DIR / "decklists").rglob("*.json"):
        deck = json.loads(deck_file.read_text())
        for section in ("mainboard", "sideboard", "commander"):
            for item in deck.get(section, []) or []:
                name = item.get("name") if isinstance(item, dict) else None
                card = by_name.get((name or "").lower())
                if card:
                    imported[card["name"]] = card
    return imported


def _detected_weird_mechanics(cards: dict[str, dict]) -> set[str]:
    found: set[str] = set()
    for card in cards.values():
        keywords = set(card.get("keywords") or [])
        oracle = card.get("oracle_text", "")
        type_line = card.get("type_line", "")
        if "Adventure" in type_line:
            found.add("Adventure")
        if "Companion" in keywords:
            found.add("Companion")
        if "Escape" in keywords:
            found.add("Escape")
        if "Flashback" in keywords:
            found.add("Flashback")
        if "Foretell" in keywords:
            found.add("Foretell")
        if "Harmonize" in keywords:
            found.add("Harmonize")
        if "Plot" in keywords:
            found.add("Plot")
        if "Rebound" in keywords:
            found.add("Rebound")
        if "Retrace" in keywords:
            found.add("Retrace")
        if "Suspend" in keywords:
            found.add("Suspend")
        if "Unearth" in keywords:
            found.add("Unearth")
        if "Warp" in keywords:
            found.add("Warp")
        if "outside the game" in oracle.lower():
            found.add("OutsideGameWish")
    return found


def _state_for(action: dict, *, guide: dict | None = None) -> dict:
    return {
        "game_id": "out-of-hand-test",
        "turn": 5,
        "phase": "MAIN1",
        "sidecar_influence": 100,
        "archetype": "Mono Red Aggro",
        "confidence": 0.74,
        "hand": ["Mountain"],
        "own_board": ["Mountain", "Mountain", "Mountain", "Forest"],
        "opponent_board": ["Goblin Guide"],
        "your_graveyard": [],
        "life_totals": {"ai": 8, "human": 11},
        "opponent_hand": [{"category": "removal", "probability": 0.15}],
        "piloting_guide": guide
        or {
            "archetype": "Value Midrange",
            "strategy_type": "midrange",
            "overview": "Convert mana into board presence and cards.",
            "game_plan": {
                "mid_game": [
                    "Use non-hand resources before passing with unused mana.",
                    "Prioritize cards that create immediate material advantage.",
                ]
            },
            "key_cards": [{"name": action["card"], "role": "resource advantage"}],
        },
        "legal_actions": [
            action,
            {
                "action_type": "PLAY_LAND",
                "card": "Mountain",
                "ability": "Play Mountain.",
                "source_zone": "Hand",
            },
            {
                "action_type": "PASS",
                "card": "",
                "ability": "",
                "source_zone": "",
            },
        ],
        "actions": [
            {
                "action_type": "PLAY_LAND",
                "target": "Mountain",
                "percentage": 45,
                "reasoning": "Fallback land drop.",
            }
        ],
    }


def test_imported_deck_weird_mechanics_have_representative_scenarios():
    imported_cards = _load_imported_cards()
    found = _detected_weird_mechanics(imported_cards)
    missing = found - set(WEIRD_MECHANIC_REPRESENTATIVES)
    assert missing == set()
    for mechanic in found:
        assert WEIRD_MECHANIC_REPRESENTATIVES[mechanic] in imported_cards


@pytest.mark.asyncio
@pytest.mark.parametrize(
    ("label", "source_deck", "action"),
    [
        (
            "companion command-zone move to hand",
            "vintage/Dimir Lurrus Control",
            {
                "action_type": "ACTIVATE_ABILITY",
                "card": "Lurrus of the Dream-Den",
                "ability": "Companion - Put Lurrus of the Dream-Den in to your hand",
                "cost": "{3}",
                "source_zone": "Command",
            },
        ),
        (
            "adventure creature cast from exile",
            "generated adventure coverage",
            {
                "action_type": "PLAY_SPELL",
                "card": "Beanstalk Giant",
                "ability": "Cast Beanstalk Giant from exile after Fertile Footsteps resolved.",
                "cost": "{6}{G}",
                "source_zone": "Exile",
            },
        ),
        (
            "warp card recast from exile",
            "modern/4/5c Omnath",
            {
                "action_type": "PLAY_SPELL",
                "card": "Quantum Riddler",
                "ability": "Cast Quantum Riddler from exile after it was warped.",
                "cost": "{3}{U}{U}",
                "source_zone": "Exile",
            },
        ),
        (
            "flashback spell cast from graveyard",
            "modern/Ruby Storm",
            {
                "action_type": "PLAY_SPELL",
                "card": "Past in Flames",
                "ability": "Cast Past in Flames from your graveyard using flashback.",
                "cost": "{4}{R}",
                "source_zone": "Graveyard",
            },
        ),
        (
            "escape spell cast from graveyard",
            "modern/Dimir Midrange",
            {
                "action_type": "PLAY_SPELL",
                "card": "Cling to Dust",
                "ability": "Escape - Cast Cling to Dust from your graveyard.",
                "cost": "{3}{B}, Exile five other cards from your graveyard",
                "source_zone": "Graveyard",
            },
        ),
        (
            "escape creature cast from graveyard",
            "legacy/Beanstalk Control (Yorion)",
            {
                "action_type": "PLAY_SPELL",
                "card": "Uro, Titan of Nature's Wrath",
                "ability": "Escape - Cast Uro, Titan of Nature's Wrath from your graveyard.",
                "cost": "{G}{G}{U}{U}, Exile five other cards from your graveyard",
                "source_zone": "Graveyard",
            },
        ),
        (
            "unearth ability from graveyard",
            "modern/Eldrazi Tron",
            {
                "action_type": "ACTIVATE_ABILITY",
                "card": "Cityscape Leveler",
                "ability": "Unearth Cityscape Leveler from your graveyard.",
                "cost": "{8}",
                "source_zone": "Graveyard",
            },
        ),
        (
            "retrace spell cast from graveyard",
            "legacy/Creative Technique",
            {
                "action_type": "PLAY_SPELL",
                "card": "Throes of Chaos",
                "ability": "Cast Throes of Chaos from your graveyard using retrace.",
                "cost": "{3}{R}, Discard a land card",
                "source_zone": "Graveyard",
            },
        ),
        (
            "plotted spell cast from exile",
            "pioneer/Izzet Prowess",
            {
                "action_type": "PLAY_SPELL",
                "card": "Slickshot Show-Off",
                "ability": "Cast Slickshot Show-Off from exile after it was plotted.",
                "cost": "{0}",
                "source_zone": "Exile",
            },
        ),
        (
            "foretold spell cast from exile",
            "pauper/Dimir Control",
            {
                "action_type": "PLAY_SPELL",
                "card": "Behold the Multiverse",
                "ability": "Cast Behold the Multiverse from exile after it was foretold.",
                "cost": "{1}{U}",
                "source_zone": "Exile",
            },
        ),
        (
            "suspend spell cast from exile",
            "modern/Crashing Footfalls",
            {
                "action_type": "PLAY_SPELL",
                "card": "Crashing Footfalls",
                "ability": "Cast Crashing Footfalls from exile after the last time counter was removed.",
                "cost": "{0}",
                "source_zone": "Exile",
            },
        ),
        (
            "harmonize spell cast from graveyard",
            "standard/Temur Control",
            {
                "action_type": "PLAY_SPELL",
                "card": "Winternight Stories",
                "ability": "Cast Winternight Stories from your graveyard using harmonize.",
                "cost": "{4}{U}",
                "source_zone": "Graveyard",
            },
        ),
        (
            "rebound spell cast from exile",
            "modern/Azorius GenericBlink",
            {
                "action_type": "PLAY_SPELL",
                "card": "Ephemerate",
                "ability": "Cast Ephemerate from exile due to rebound.",
                "cost": "{0}",
                "source_zone": "Exile",
            },
        ),
        (
            "Karn wish ability gets sideboard card",
            "modern/Eldrazi Tron",
            {
                "action_type": "ACTIVATE_ABILITY",
                "card": "Karn, the Great Creator",
                "ability": "You may reveal an artifact card you own from outside the game or choose a face-up artifact card you own in exile, reveal that card, and put it into your hand.",
                "cost": "-2 loyalty",
                "source_zone": "Battlefield",
            },
        ),
    ],
)
async def test_strategist_can_recommend_out_of_hand_legal_action_highest(
    monkeypatch, label, source_deck, action
):
    async def fake_generate_json(prompt, **_kwargs):
        assert action["card"] in prompt, label
        assert action["source_zone"] in prompt, label
        assert action["ability"] in prompt, label
        return {
            "actions": [
                {
                    "action_type": action["action_type"],
                    "target": action["card"],
                    "ability": action["ability"],
                    "percentage": 94,
                    "reasoning": f"{label} from {source_deck} is the highest-impact legal action.",
                },
                {
                    "action_type": "PLAY_LAND",
                    "target": "Mountain",
                    "ability": "Play Mountain.",
                    "percentage": 35,
                    "reasoning": "Less important than using the special-zone card.",
                },
            ]
        }

    monkeypatch.setattr(strat, "generate_json", fake_generate_json)

    out = await strat._run_action_planner(_state_for(action))

    assert out["actions"][0]["action_type"] == action["action_type"]
    assert out["actions"][0]["target"] == action["card"]
    assert out["actions"][0]["ability"] == action["ability"]
    assert out["actions"][0]["percentage"] == 94


def test_action_planner_rejects_wrong_zone_or_unlisted_out_of_hand_card():
    state = _state_for(
        {
            "action_type": "PLAY_SPELL",
            "card": "Beanstalk Giant",
            "ability": "Cast Beanstalk Giant from exile after Fertile Footsteps resolved.",
            "cost": "{6}{G}",
            "source_zone": "Exile",
        }
    )

    out = strat._coerce_action_plan(
        {
            "actions": [
                {
                    "action_type": "PLAY_SPELL",
                    "target": "Beanstalk Giant",
                    "ability": "Cast Fertile Footsteps from graveyard.",
                    "percentage": 99,
                    "reasoning": "Not in Forge legal actions.",
                },
                {
                    "action_type": "PLAY_SPELL",
                    "target": "Quantum Riddler",
                    "ability": "Cast Quantum Riddler from exile after it was warped.",
                    "percentage": 98,
                    "reasoning": "Also absent from this scenario.",
                },
            ]
        },
        state,
    )

    assert out == []
