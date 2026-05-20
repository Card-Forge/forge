"""Tests for staleness computation from banlist events."""

from app.knowledge import piloting
from app.knowledge.piloting_schema import PilotingGuide


def _make_guide(*, generated_at: str, key_card: str = "Phlage, Titan of Fire's Fury") -> PilotingGuide:
    return PilotingGuide.model_validate(
        {
            "archetype": "Boros Energy",
            "format": "modern",
            "strategy_type": "aggro",
            "overview": "Aggressive deck.",
            "win_conditions": [key_card],
            "key_cards": [{"name": key_card, "role": "win condition", "notes": ""}],
            "metadata": {
                "source": "test",
                "source_url": "",
                "generated_at": generated_at,
                "model": "test",
                "schema_version": 2,
            },
        }
    )


def test_pre_ban_modern_guide_flags_phlage():
    # Generated before the 2026-05-18 Modern ban.
    guide = _make_guide(generated_at="2026-04-15T00:00:00+00:00")
    flags = piloting._compute_stale_flags(guide)
    assert "Phlage, Titan of Fire's Fury" in flags.banned_cards_referenced
    assert "Umezawa's Jitte" in flags.unbanned_cards_missing
    assert flags.age_days >= 0


def test_post_ban_modern_guide_is_clean():
    guide = _make_guide(generated_at="2026-05-19T00:00:00+00:00")
    flags = piloting._compute_stale_flags(guide)
    assert flags.banned_cards_referenced == []
    assert flags.unbanned_cards_missing == []


def test_historic_reset_flags_rotation():
    guide = PilotingGuide.model_validate(
        {
            "archetype": "Eldrazi Tron",
            "format": "historic",
            "strategy_type": "midrange",
            "overview": "Big mana with Eldrazi Temple.",
            "key_cards": [
                {"name": "Eldrazi Temple", "role": "ramp", "notes": ""}
            ],
            "metadata": {"generated_at": "2026-01-01T00:00:00+00:00", "schema_version": 2},
        }
    )
    flags = piloting._compute_stale_flags(guide)
    assert "Eldrazi Temple" in flags.banned_cards_referenced
    assert flags.format_rotation_event is True


def test_no_banlist_match_for_unrelated_format():
    guide = _make_guide(generated_at="2026-01-01T00:00:00+00:00")
    # Patch the guide's format to one with no events.
    guide.format = "commander"
    flags = piloting._compute_stale_flags(guide)
    assert flags.banned_cards_referenced == []
    assert flags.format_rotation_event is False
