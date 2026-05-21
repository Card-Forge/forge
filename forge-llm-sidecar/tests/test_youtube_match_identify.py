"""Tests for transcript-based archetype identification via signature cards."""

from app.knowledge.primers.youtube.match_identify import (
    ArchetypeScore,
    _count_phrase,
    _count_word,
    identify_archetypes,
    reconcile_with_llm,
)


_BOROS_ENERGY = {
    "name": "Boros Energy",
    "signature_cards": [
        "Ragavan, Nimble Pilferer",
        "Phlage, Titan of Fire's Fury",
        "Guide of Souls",
        "Ocelot Pride",
        "Galvanic Discharge",
        "Static Prison",
    ],
}

_LIVING_END = {
    "name": "Living End",
    "signature_cards": [
        "Living End",
        "Force of Negation",
        "Subtlety",
        "Curator of Mysteries",
        "Architects of Will",
        "Striped Riverwinder",
    ],
}

_DOMAIN_ZOO = {
    "name": "Domain Zoo",
    "signature_cards": [
        "Scion of Draco",
        "Tribal Flames",
        "Territorial Kavu",
        "Leyline Binding",
        "Phlage, Titan of Fire's Fury",  # also in Boros
    ],
}


def test_word_count_uses_boundaries():
    text = "i play phlage. then phlage attacks. but not phlageology."
    # "phlage" appears 2 times as a whole word; "phlageology" should not count.
    assert _count_word(text, "phlage") == 2


def test_phrase_count_handles_whitespace():
    text = "force of negation here, force of  negation there"
    assert _count_phrase(text, "force of negation") == 2


def test_identifies_boros_in_transcript():
    transcript = (
        "okay so i'm gonna play ragavan turn one. ragavan attacks. "
        "next turn i have guide of souls and another guide of souls. "
        "phlage is in hand. phlage will give me reach later. "
        "galvanic discharge to clear the blocker. and ocelot pride for the kill."
    )
    scored = identify_archetypes(
        transcript,
        [_BOROS_ENERGY, _LIVING_END, _DOMAIN_ZOO],
    )
    names = [s.name for s in scored]
    assert "Boros Energy" in names
    boros = next(s for s in scored if s.name == "Boros Energy")
    assert boros.distinct_cards >= 4  # ragavan + guide + phlage + galvanic + ocelot
    assert boros.total_mentions >= 6


def test_identifies_feature_match_two_archetypes():
    transcript = (
        "boros player plays ragavan. then guide of souls. phlage incoming. "
        "ocelot pride attacks. galvanic discharge on the creature. "
        "opponent has force of negation in hand. they cycle architects of will. "
        "striped riverwinder cycled. subtlety to bounce. living end resolves. "
        "force of negation countered the response. subtlety again."
    )
    scored = identify_archetypes(
        transcript,
        [_BOROS_ENERGY, _LIVING_END, _DOMAIN_ZOO],
    )
    names = [s.name for s in scored]
    assert "Boros Energy" in names
    assert "Living End" in names


def test_rejects_low_signal_match():
    transcript = "just one mention of ragavan, nothing else"
    scored = identify_archetypes(
        transcript,
        [_BOROS_ENERGY, _LIVING_END],
    )
    # Below both thresholds: should be empty
    assert scored == []


def test_common_first_word_falls_back_to_full_name():
    # "Force" is a common first word, so we should NOT count it alone.
    # Without the full phrase "force of negation", we should get zero.
    transcript = "force this, force that, force everything, force five force six"
    scored = identify_archetypes(
        transcript,
        [_LIVING_END],
        min_distinct_cards=1,
        min_total_mentions=1,
    )
    # "force" alone doesn't count as "Force of Negation" — should be empty
    assert scored == []


def test_shared_signature_card_assigns_to_both():
    # "Phlage" is in both Boros Energy and Domain Zoo signatures. A transcript
    # with mostly Domain Zoo cards + Phlage should still credit Domain Zoo.
    transcript = (
        "domain zoo deck: scion of draco, tribal flames, territorial kavu, "
        "leyline binding, phlage, scion of draco again, tribal flames again, "
        "territorial kavu, leyline binding, phlage closing"
    )
    scored = identify_archetypes(
        transcript,
        [_BOROS_ENERGY, _DOMAIN_ZOO],
    )
    names = [s.name for s in scored]
    assert "Domain Zoo" in names
    # Domain Zoo should outscore Boros even though phlage is shared.
    if "Boros Energy" in names:
        zoo = next(s for s in scored if s.name == "Domain Zoo")
        boros = next(s for s in scored if s.name == "Boros Energy")
        assert zoo.confidence > boros.confidence


def test_empty_transcript_returns_empty():
    assert identify_archetypes("", [_BOROS_ENERGY]) == []


def test_no_signature_cards_skipped():
    assert (
        identify_archetypes(
            "lots of ragavan ragavan ragavan",
            [{"name": "NoSigs", "signature_cards": []}],
        )
        == []
    )


def test_shared_only_matches_get_capped_confidence():
    """Bant Nadu / Azorius Control share Force of Negation + Solitude + Subtlety.

    A Bant Nadu video should NOT credit Azorius Control as a high-confidence
    match just because the shared cards are mentioned. Azorius Control has
    no UNIQUE matched card so its confidence is capped.
    """
    azorius_ctl = {
        "name": "Azorius Control",
        "signature_cards": [
            "Force of Negation", "Solitude", "Subtlety",
            "Supreme Verdict", "Teferi, Time Raveler",
        ],
    }
    bant_nadu = {
        "name": "Bant Nadu",
        "signature_cards": [
            "Nadu, Winged Wisdom", "Force of Negation", "Solitude", "Subtlety",
            "Shuko", "Springheart Nantuko",
        ],
    }
    # Transcript mentions all shared cards + Nadu's unique card multiple times.
    transcript = (
        "force of negation force of negation solitude solitude subtlety subtlety "
        "nadu nadu nadu winged wisdom shuko springheart nantuko"
    )
    scored = identify_archetypes(transcript, [azorius_ctl, bant_nadu])
    names = [s.name for s in scored]
    # Bant Nadu must outrank Azorius Control because it has unique matches.
    if "Azorius Control" in names and "Bant Nadu" in names:
        bant = next(s for s in scored if s.name == "Bant Nadu")
        az = next(s for s in scored if s.name == "Azorius Control")
        assert bant.confidence > az.confidence
        assert az.confidence <= 0.4  # capped
        assert bant.unique_distinct >= 1
    else:
        # Azorius should have been filtered out below threshold
        assert "Bant Nadu" in names
        assert "Azorius Control" not in names


def test_reconcile_llm_replaces_sig_top_when_disagreement():
    """LLM names two archetypes and neither is the sig top → REPLACE the sig top."""
    sig_scores = [
        ArchetypeScore("Azorius Control", distinct_cards=3, total_mentions=8,
                       unique_distinct=1, confidence=0.6),
        ArchetypeScore("Affinity", distinct_cards=5, total_mentions=15,
                       unique_distinct=2, confidence=0.95),
    ]
    llm = {
        "player_a": {"archetype_guess": "Affinity", "confidence": 0.9},
        "player_b": {"archetype_guess": "Izzet Prowess", "confidence": 0.85},
    }
    known = {"Affinity", "Izzet Prowess", "Azorius Control"}
    final, audit = reconcile_with_llm(sig_scores, llm, known)
    names = {s.name for s in final}
    assert "Affinity" in names
    assert "Izzet Prowess" in names
    assert "Azorius Control" not in names
    assert any(a["action"] == "llm_rejected_sig_top" for a in audit)
    assert any(a["action"] == "llm_added" and a["name"] == "Izzet Prowess" for a in audit)


def test_reconcile_llm_adds_missing_archetype():
    """LLM names an archetype not in sig_scores but it IS in metagame → add it."""
    sig_scores = [
        ArchetypeScore("Living End", distinct_cards=4, total_mentions=10,
                       unique_distinct=1, confidence=0.85),
    ]
    llm = {
        "player_a": {"archetype_guess": "Living End", "confidence": 0.9},
        "player_b": {"archetype_guess": "Goryo's Vengeance", "confidence": 0.8},
    }
    known = {"Living End", "Goryo's Vengeance"}
    final, audit = reconcile_with_llm(sig_scores, llm, known)
    names = {s.name for s in final}
    assert names == {"Living End", "Goryo's Vengeance"}


def test_reconcile_llm_ignores_unknown_verdict():
    """LLM says 'unknown' → keep sig scores untouched."""
    sig_scores = [
        ArchetypeScore("Living End", distinct_cards=4, total_mentions=10,
                       unique_distinct=1, confidence=0.85),
    ]
    llm = {
        "player_a": {"archetype_guess": "unknown"},
        "player_b": {"archetype_guess": "Some Niche Brew Not In Metagame"},
    }
    known = {"Living End", "Goryo's Vengeance"}
    final, audit = reconcile_with_llm(sig_scores, llm, known)
    assert [s.name for s in final] == ["Living End"]
    # one audit entry for the not-in-metagame name
    assert any(a["action"] == "llm_named_unknown" for a in audit)


def test_pure_shared_archetype_filtered_out():
    """If an archetype's ONLY matched cards are shared, it falls below threshold."""
    azorius_ctl = {
        "name": "Azorius Control",
        "signature_cards": [
            "Force of Negation", "Solitude", "Subtlety", "Supreme Verdict",
        ],
    }
    bant_nadu = {
        "name": "Bant Nadu",
        "signature_cards": [
            "Nadu, Winged Wisdom", "Force of Negation", "Solitude", "Subtlety",
        ],
    }
    transcript = (
        "force of negation force of negation solitude solitude solitude "
        "subtlety subtlety nadu nadu nadu nadu winged wisdom"
    )
    scored = identify_archetypes(transcript, [azorius_ctl, bant_nadu])
    names = [s.name for s in scored]
    assert "Bant Nadu" in names
    # Azorius Control: 3 shared cards matched, 0 unique → capped at 0.4 → filtered.
    assert "Azorius Control" not in names
