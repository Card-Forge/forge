"""Offline tests for the decklist-to-buckets pipeline."""

import json

from app.knowledge import card_buckets


def test_parse_dck_reads_main_and_sideboard(tmp_path):
    path = tmp_path / "izzet-spells.dck"
    path.write_text(
        """
[metadata]
Name=Izzet Spells
[Main]
4 Opt
2 Spell Pierce
4 Riverpyre Verge
[Sideboard]
1 Negate
2 Ghost Vacuum
""".strip()
        + "\n",
        encoding="utf-8",
    )

    deck = card_buckets.parse_dck(path, fmt="standard")

    assert deck.archetype == "Izzet Spells"
    assert deck.format == "standard"
    assert [(e.qty, e.name) for e in deck.mainboard] == [
        (4, "Opt"),
        (2, "Spell Pierce"),
        (4, "Riverpyre Verge"),
    ]
    assert [(e.qty, e.name) for e in deck.sideboard] == [(1, "Negate"), (2, "Ghost Vacuum")]


def test_classify_card_uses_metadata_and_combo_profile_hints():
    guide = {
        "combo_profile": {
            "category_cards": {
                "reducers": ["Ral, Monsoon Mage"],
                "rituals": ["Desperate Ritual"],
                "payoff": ["Grapeshot"],
            }
        },
        "key_cards": [],
    }

    ral = card_buckets.classify_card(
        "Ral, Monsoon Mage",
        {"type_line": "Legendary Creature // Planeswalker", "oracle_text": "Instant and sorcery spells you cast cost {1} less."},
        guide=guide,
    )
    ritual = card_buckets.classify_card(
        "Desperate Ritual",
        {"type_line": "Instant", "oracle_text": "Add {R}{R}{R}."},
        guide=guide,
    )
    grapeshot = card_buckets.classify_card(
        "Grapeshot",
        {"type_line": "Sorcery", "oracle_text": "Grapeshot deals 1 damage to any target. Storm"},
        guide=guide,
    )

    assert {"mana_reducers", "engines", "combo_pieces", "planeswalker_threats"} <= ral
    assert {"rituals", "combo_pieces"} <= ritual
    assert {"win_conditions", "payoff_cards", "combo_pieces", "removal"} <= grapeshot


def test_build_profile_from_decklist_buckets_every_card(monkeypatch, tmp_path):
    deck = card_buckets.NormalizedDecklist(
        archetype="Ruby Storm",
        format="modern",
        source_file="ruby.dck",
        mainboard=[
            card_buckets.DeckEntry(4, "Ral, Monsoon Mage"),
            card_buckets.DeckEntry(4, "Desperate Ritual"),
            card_buckets.DeckEntry(2, "Grapeshot"),
            card_buckets.DeckEntry(18, "Mountain"),
        ],
        sideboard=[card_buckets.DeckEntry(2, "Defense Grid")],
    )
    guide = {
        "archetype": "Ruby Storm",
        "format": "modern",
        "strategy_type": "combo",
        "overview": "Storm deck.",
        "win_conditions": ["Grapeshot"],
        "mulligan": {"keep_criteria": [], "mulligan_criteria": [], "examples": []},
        "game_plan": {"early_game": [], "mid_game": [], "late_game": []},
        "key_cards": [],
        "sequencing_tips": [],
        "matchups": [],
        "common_threats": [],
        "combo_profile": {
            "category_cards": {
                "reducers": ["Ral, Monsoon Mage"],
                "rituals": ["Desperate Ritual"],
                "payoff": ["Grapeshot"],
                "protection": ["Defense Grid"],
            },
            "known_lines": [],
        },
    }
    cache = {
        card_buckets._norm("Ral, Monsoon Mage"): {
            "type_line": "Legendary Creature // Planeswalker",
            "oracle_text": "Instant and sorcery spells you cast cost {1} less.",
        },
        card_buckets._norm("Desperate Ritual"): {
            "type_line": "Instant",
            "oracle_text": "Add {R}{R}{R}.",
        },
        card_buckets._norm("Grapeshot"): {
            "type_line": "Sorcery",
            "oracle_text": "Grapeshot deals 1 damage to any target. Storm",
        },
        card_buckets._norm("Mountain"): {
            "type_line": "Basic Land - Mountain",
            "oracle_text": "Add {R}.",
            "produced_mana": ["R"],
        },
        card_buckets._norm("Defense Grid"): {
            "type_line": "Artifact",
            "oracle_text": "Each spell costs {3} more to cast except during its controller's turn.",
        },
    }

    monkeypatch.setattr(card_buckets, "_guide_payload", lambda _deck: guide)
    monkeypatch.setattr(card_buckets, "_existing_profile", lambda _deck: None)
    monkeypatch.setattr(card_buckets.guidance_migration, "load_metagame", lambda _fmt: {})

    profile, coverage = card_buckets.build_profile_from_decklist(deck, card_cache=cache)

    assert coverage["coverage"] == 1.0
    assert coverage["unbucketed_cards"] == []
    assert "Ral, Monsoon Mage" in profile["buckets"]["mana_reducers"]["cards"]
    assert "Desperate Ritual" in profile["buckets"]["rituals"]["cards"]
    assert "Grapeshot" in profile["buckets"]["win_conditions"]["cards"]
    assert "Mountain" in profile["buckets"]["lands"]["cards"]
    assert "Defense Grid" in profile["buckets"]["protection"]["cards"]
    assert "Ral, Monsoon Mage" in profile["kill_priority"]


def test_write_outputs(tmp_path):
    deck = card_buckets.NormalizedDecklist(
        archetype="Sample Deck",
        format="standard",
        source_file="sample.dck",
        mainboard=[card_buckets.DeckEntry(4, "Island")],
    )
    deck_path = card_buckets.write_decklist_json(deck, out_dir=tmp_path / "decklists")
    assert json.loads(deck_path.read_text(encoding="utf-8"))["mainboard"][0]["name"] == "Island"

    report = card_buckets.write_coverage_report(
        [
            {
                "format": "standard",
                "archetype": "Sample Deck",
                "source_file": "sample.dck",
                "mainboard_cards": 4,
                "sideboard_cards": 0,
                "unique_cards": 1,
                "bucketed_unique_cards": 1,
                "coverage": 1.0,
                "unbucketed_cards": [],
                "multi_role_cards": ["Island"],
            }
        ],
        fmt="standard",
        out_dir=tmp_path / "reports",
    )
    assert "Sample Deck" in report.read_text(encoding="utf-8")
