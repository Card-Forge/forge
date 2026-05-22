"""Tests for canonical Forge card-name resolution (offline, fake cardsfolder)."""

import pytest

from app.knowledge import forge_cards

_SPLIT_CARD = """Name:Roaring Furnace
ManaCost:1 R
Types:Enchantment Room
AlternateMode:Split
ALTERNATE
Name:Steaming Sauna
ManaCost:3 U
Types:Enchantment Room
"""

_MDFC_CARD = """Name:Valki, God of Lies
ManaCost:1 B
AlternateMode:Modal
ALTERNATE
Name:Tibalt, Cosmic Impostor
ManaCost:5 B R
"""

_PLAIN_CARD = """Name:Llanowar Elves
ManaCost:G
Types:Creature Elf Druid
"""

_ACCENT_CARD = """Name:Lim-Dûl's Vault
ManaCost:U B
Types:Instant
"""


@pytest.fixture
def fake_db(tmp_path, monkeypatch):
    folder = tmp_path / "cardsfolder"
    (folder / "r").mkdir(parents=True)
    (folder / "v").mkdir()
    (folder / "l").mkdir()
    (folder / "r" / "roaring_furnace_steaming_sauna.txt").write_text(_SPLIT_CARD)
    (folder / "v" / "valki.txt").write_text(_MDFC_CARD)
    (folder / "l" / "llanowar_elves.txt").write_text(_PLAIN_CARD)
    (folder / "l" / "lim_duls_vault.txt").write_text(_ACCENT_CARD)
    monkeypatch.setenv("FORGE_CARDSFOLDER", str(folder))
    forge_cards.build_index.cache_clear()
    yield folder
    forge_cards.build_index.cache_clear()


class TestNormalize:
    def test_separator_insensitive(self):
        assert forge_cards.normalize("A // B") == forge_cards.normalize("A/B") == "a b"

    def test_accent_and_punctuation_folded(self):
        assert forge_cards.normalize("Lim-Dûl's Vault") == "lim dul s vault"


class TestResolve:
    def test_split_bare_slash_resolves_to_combined(self, fake_db):
        assert forge_cards.resolve("Roaring Furnace/Steaming Sauna") == (
            "Roaring Furnace // Steaming Sauna"
        )

    def test_split_already_combined_resolves(self, fake_db):
        assert forge_cards.resolve("Roaring Furnace // Steaming Sauna") == (
            "Roaring Furnace // Steaming Sauna"
        )

    def test_split_front_face_resolves_to_combined(self, fake_db):
        assert forge_cards.resolve("Roaring Furnace") == "Roaring Furnace // Steaming Sauna"

    def test_mdfc_uses_front_face_only(self, fake_db):
        # Modal (not Split) -> Forge names by the front face alone.
        assert forge_cards.resolve("Valki, God of Lies // Tibalt, Cosmic Impostor") == (
            "Valki, God of Lies"
        )

    def test_plain_card_resolves_to_itself(self, fake_db):
        assert forge_cards.resolve("Llanowar Elves") == "Llanowar Elves"

    def test_accented_name_matches_ascii_input(self, fake_db):
        assert forge_cards.resolve("Lim-Dul's Vault") == "Lim-Dûl's Vault"

    def test_unknown_card_is_none(self, fake_db):
        assert forge_cards.resolve("Totally Made Up Card") is None

    def test_resolve_or_keep_keeps_unmatched(self, fake_db):
        assert forge_cards.resolve_or_keep("Totally Made Up Card") == (
            "Totally Made Up Card",
            False,
        )
        assert forge_cards.resolve_or_keep("Llanowar Elves") == ("Llanowar Elves", True)


def test_missing_cardsfolder_fails_soft(tmp_path, monkeypatch):
    monkeypatch.setenv("FORGE_CARDSFOLDER", str(tmp_path / "nonexistent"))
    forge_cards.build_index.cache_clear()
    assert forge_cards.build_index() == {}
    assert forge_cards.resolve("Llanowar Elves") is None
    assert forge_cards.resolve_or_keep("Llanowar Elves") == ("Llanowar Elves", False)
    forge_cards.build_index.cache_clear()
