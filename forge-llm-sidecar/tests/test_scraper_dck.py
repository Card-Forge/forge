"""Tests for the MTGGoldfish -> Forge .dck conversion helpers.

Pure parsing/formatting only — no network. Guards the two decklist export
shapes (plain .txt and Arena format) the importer must accept.
"""

from app.knowledge.scraper import decklist_to_dck, parse_goldfish_decklist

_PLAIN = """4 Monastery Swiftspear
4 Soul-Scar Mage
20 Mountain

2 Abrade
1 Roiling Vortex
"""

_ARENA = """Deck
4 Monastery Swiftspear (DFT) 137
20 Mountain (MOM) 281

Sideboard
2 Abrade (HOU) 91

Companion
1 Jegantha, the Wellspring (IKO) 222
"""


def test_parse_plain_splits_on_blank_line():
    main, side = parse_goldfish_decklist(_PLAIN)
    assert main == [(4, "Monastery Swiftspear"), (4, "Soul-Scar Mage"), (20, "Mountain")]
    assert side == [(2, "Abrade"), (1, "Roiling Vortex")]


def test_parse_arena_uses_headers_and_strips_set_codes():
    main, side = parse_goldfish_decklist(_ARENA)
    assert main == [(4, "Monastery Swiftspear"), (20, "Mountain")]
    assert side == [(2, "Abrade")]  # companion is ignored


def test_decklist_to_dck_round_trips():
    main, side = parse_goldfish_decklist(_PLAIN)
    dck = decklist_to_dck("Mono-Red Aggro", main, side)
    assert dck.startswith("[metadata]\nName=Mono-Red Aggro\n[Main]\n")
    assert "4 Monastery Swiftspear\n" in dck
    assert "[Sideboard]\n2 Abrade\n" in dck
    assert dck.endswith("\n")


def test_decklist_to_dck_omits_empty_sideboard():
    dck = decklist_to_dck("All Main", [(60, "Island")], [])
    assert "[Sideboard]" not in dck
