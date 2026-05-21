"""Tests for primer source providers (URL discovery + candidate extraction)."""

from app.knowledge.primers.sources.cards_realm import CardsRealmProvider
from app.knowledge.primers.sources.draftsim import DraftsimProvider
from app.knowledge.primers.sources.hareruya import HareruyaProvider
from app.knowledge.primers.sources.mtg_arena_zone import MTGArenaZoneProvider


_CARDS_REALM_SEARCH = """
<html><body>
  <a href="/en-us/articles/modern-boros-energy-deck-tech-sideboard-guide">
    Modern: Boros Energy - Deck Tech &amp; Sideboard Guide
  </a>
  <a href="https://mtg.cardsrealm.com/en-us/articles/standard-mono-green-landfall">
    Standard: Mono-Green Landfall
  </a>
  <a href="/en-us/articles/some-unrelated-piece">
    Some unrelated article
  </a>
  <a href="/en-us/articles/metagame-first-impressions-of-the-may-banlist-modern">
    Metagame: First Impressions
  </a>
</body></html>
"""


def test_cards_realm_filters_by_format_and_archetype():
    provider = CardsRealmProvider()
    candidates = list(
        provider._extract_candidates(_CARDS_REALM_SEARCH, "Boros Energy", "modern")
    )
    urls = [c.url for c in candidates]
    assert any("modern-boros-energy" in u for u in urls)
    # standard article should be filtered out (wrong format token)
    assert not any("mono-green-landfall" in u for u in urls)
    assert not any("unrelated-piece" in u for u in urls)
    # metagame digests should be filtered out even if they mention modern
    assert not any("metagame-first-impressions" in u for u in urls)


_HARERUYA_SEARCH = """
<html><body>
  <a href="/en/article/12345/">Esper Goryo's Modern Deck Guide</a>
  <a href="/en/article/67890/">Legacy Reanimator Update</a>
</body></html>
"""


def test_hareruya_filters_by_format_in_title():
    provider = HareruyaProvider()
    cands = list(provider._extract_candidates(_HARERUYA_SEARCH, "Goryo", "modern"))
    assert len(cands) == 1
    assert "12345" in cands[0].url


_MTGAZ_INDEX = """
<html><body>
  <a href="https://mtgazone.com/abzan-verdant-ritual-deep-dive/">
    Abzan Verdant Ritual Deep Dive - Deck Guide
  </a>
  <a href="https://mtgazone.com/format/historic/">Historic format index</a>
  <a href="https://mtgazone.com/some-news-post/">Some news post</a>
</body></html>
"""


def test_mtg_arena_zone_filters_to_guide_articles():
    provider = MTGArenaZoneProvider()
    cands = list(provider._extract_candidates(_MTGAZ_INDEX, "Abzan Verdant Ritual"))
    assert len(cands) == 1
    assert "abzan-verdant-ritual" in cands[0].url


def test_draftsim_returns_candidate_urls():
    provider = DraftsimProvider()
    cands = provider.search("Boros Energy", "modern")
    assert cands
    assert all(c.url.startswith("https://draftsim.com/") for c in cands)


def test_provider_supports_format_filtering():
    cr = CardsRealmProvider()
    assert cr.search("Anything", "commander") == []  # not supported
    har = HareruyaProvider()
    assert har.search("Anything", "vintage") == []
