from app.knowledge import guidance_migration as gm


def test_merge_legacy_into_existing_guide_adds_runtime_fields():
    guide = {
        "archetype": "Sample Tempo",
        "format": "standard",
        "strategy_type": "tempo",
        "overview": "Existing overview.",
        "win_conditions": ["Attack with threats"],
        "mulligan": {"keep_criteria": [], "mulligan_criteria": [], "examples": []},
        "game_plan": {"early_game": [], "mid_game": [], "late_game": []},
        "key_cards": [{"name": "Slickshot Show-Off", "role": "threat", "notes": "Fast clock"}],
        "sequencing_tips": [],
        "matchups": [],
        "common_threats": [],
    }
    legacy = {
        "archetype": "Sample Tempo",
        "opening_strategy": {
            "mulligan_criteria": "Keep two-land hands with a cheap threat. Mulligan no-land hands.",
            "early_game_plays": "Deploy a threat before holding interaction.",
        },
        "midgame_priorities": {"threat_tracking": "Watch for sweepers."},
        "threat_responses": {"against_control": "Pressure them before they can stabilize."},
        "notes": "Cast cantrips before pump spells.",
    }

    merged = gm.merge_legacy_into_guide(guide, legacy, "standard")

    assert "two-land hands" in merged["mulligan"]["keep_criteria"][0]
    assert "Deploy a threat" in merged["game_plan"]["early_game"][0]
    assert merged["matchups"][0]["opponent_archetype"] == "Control"
    assert merged["provenance"][0]["publisher"] == "legacy_guidance"


def test_build_archetype_profile_assigns_expected_buckets():
    guide = {
        "archetype": "Sample Tempo",
        "format": "standard",
        "strategy_type": "tempo",
        "overview": "Tempo deck with burn removal and card draw.",
        "win_conditions": ["Win with Slickshot Show-Off"],
        "mulligan": {"keep_criteria": [], "mulligan_criteria": [], "examples": []},
        "game_plan": {"early_game": ["Play Steam Vents"], "mid_game": [], "late_game": []},
        "key_cards": [
            {"name": "Slickshot Show-Off", "role": "threat", "notes": "Primary win condition"},
            {"name": "Lightning Strike", "role": "removal", "notes": "Burn spell"},
            {"name": "Steam Vents", "role": "land", "notes": "Mana fixing"},
        ],
        "sequencing_tips": [],
        "matchups": [],
        "common_threats": [],
    }

    profile = gm.build_archetype_profile(guide, {"colors": ["U", "R"], "signature_cards": []})

    assert "Slickshot Show-Off" in profile["buckets"]["threats"]["cards"]
    assert "Lightning Strike" in profile["buckets"]["removal"]["cards"]
    assert "Steam Vents" in profile["buckets"]["lands"]["cards"]
    assert profile["buckets"]["interaction_density"] > 0
