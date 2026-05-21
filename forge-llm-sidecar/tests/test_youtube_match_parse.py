"""Tests for Play MTG–style tournament VOD title/description parsing."""

from app.knowledge.primers.youtube.match_parse import (
    detect_format,
    parse_tournament_video,
)


def test_parse_basic_paren_form():
    title = "Reid Duke vs Andrea Mengucci | Round 12 | Pro Tour Edge of Eternities"
    desc = (
        "Modern format feature match. Reid Duke (Boros Energy) faces off "
        "against Andrea Mengucci (Living End) in round 12."
    )
    m = parse_tournament_video(title, desc, known_archetypes=["Boros Energy", "Living End"])
    assert m is not None
    assert m.archetype_a == "Boros Energy"
    assert m.archetype_b == "Living End"
    assert m.player_a == "Reid Duke"
    assert m.player_b == "Andrea Mengucci"
    assert m.format == "modern"
    assert "Pro Tour" in m.tournament


def test_parse_vs_in_title():
    title = "Boros Energy vs Living End | Modern PT Quarterfinal"
    desc = "Two top decks battle in the Modern format."
    m = parse_tournament_video(title, desc, known_archetypes=["Boros Energy", "Living End"])
    assert m is not None
    assert m.archetype_a == "Boros Energy"
    assert m.archetype_b == "Living End"
    assert m.format == "modern"


def test_parse_returns_none_when_no_archetypes():
    title = "Tournament Highlights | Pro Tour"
    desc = "A great weekend of Magic."
    m = parse_tournament_video(title, desc)
    assert m is None


def test_filter_excludes_round_markers():
    # "Round 5" is in parens but should not be treated as an archetype.
    title = "Modern PT Round 5 | Feature Match"
    desc = "Reid Duke (Round 5) vs ... wait that's a typo."
    m = parse_tournament_video(title, desc)
    # No second archetype found
    assert m is None


def test_detect_format_recognizes_words():
    assert detect_format("Modern PT Top 8", "") == "modern"
    assert detect_format("", "A great Legacy match!") == "legacy"
    assert detect_format("Highlights", "") == ""


def test_known_archetypes_promote_text_matches():
    title = "Two Top Modern Decks Square Off"
    desc = "An exciting match featuring Domain Zoo against Amulet Titan."
    m = parse_tournament_video(
        title, desc, known_archetypes=["Domain Zoo", "Amulet Titan", "Boros Energy"]
    )
    assert m is not None
    sides = {m.archetype_a, m.archetype_b}
    assert sides == {"Domain Zoo", "Amulet Titan"}
