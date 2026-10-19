package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.card.CardDb.CardArtPreference;
import forge.card.CardEdition.BorderColor;
import forge.card.CardEdition.Type;
import forge.deck.CardPool;
import forge.deck.generation.DeckGenPool;
import forge.gui.download.CdnUuidCache;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Covers every {@link CardArtPreference} (including the Black Border ones) and the
 * "prefer language for unique cards" / "card art language" behaviour of the card database.
 *
 * <p>
 * The assertions are written as invariants checked against an oracle computed from the edition
 * data (border, type, release date) instead of hardcoded edition codes, so they keep working
 * when new sets reprint the cards used here.
 * </p>
 */
public class CardArtPreferenceTest extends CardMockTestCase {

    private static final String[] CARD_NAMES = {
            "Shivan Dragon", "Lightning Dragon", "Hymn to Tourach", "Counterspell", "Llanowar Elves",
            "Lightning Bolt", "Serra Angel", "Sol Ring", "Dark Ritual", "Wrath of God"
    };

    private static final CardArtPreference[] BLACK_BORDER_PREFERENCES = {
            CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS,
            CardArtPreference.ORIGINAL_ART_BLACK_BORDER_CORE_EXPANSIONS_REPRINT_ONLY
    };

    private static final EnumSet<Type> ALLOWED_TYPES = EnumSet.of(Type.CORE, Type.EXPANSION, Type.REPRINT);

    private StaticData data;
    private CardDb cardDb;
    private CardArtPreference originalPreference;

    @BeforeMethod
    public void setupDb() {
        data = FModel.getMagicDb();
        cardDb = data.getCommonCards();
        originalPreference = cardDb.getCardArtPreference();
    }

    /** The database is shared by the whole test JVM: always put its settings back. */
    @AfterMethod(alwaysRun = true)
    public void restoreDb() {
        if (cardDb == null) {
            return;
        }
        cardDb.setPreferredLanguageAvailability(null);
        if (originalPreference != null) {
            cardDb.setCardArtPreference(originalPreference.latestFirst, originalPreference.filterSets,
                    originalPreference.blackBorderOnly);
        }
    }

    // helpers

    private CardEdition edition(PaperCard card) {
        return data.getEditions().get(card.getEdition());
    }

    private boolean isBlack(CardEdition ed) {
        return ed.getBorderColor() == BorderColor.BLACK;
    }

    /** Sort key used by the oracle: lower is better. {rank, date}; for latest art the date is negated. */
    private long[] key(CardArtPreference pref, CardEdition ed) {
        long date = ed.getDate().getTime();
        return new long[] { pref.getEditionRank(ed), pref.latestFirst ? -date : date };
    }

    private int compareKeys(long[] a, long[] b) {
        int c = Long.compare(a[0], b[0]);
        return c != 0 ? c : Long.compare(a[1], b[1]);
    }

    /** Prints the preference would allow, according to the edition data only. */
    private List<PaperCard> acceptedPrints(String cardName, CardArtPreference pref) {
        return cardDb.getAllCardsNoAlt(cardName).stream()
                .filter(pc -> edition(pc) != null)
                .filter(pc -> pref.accept(edition(pc)))
                .collect(Collectors.toList());
    }

    private PaperCard otherPrint(PaperCard not, List<PaperCard> prints) {
        return prints.stream()
                .filter(pc -> edition(pc) != null)
                .filter(pc -> !pc.getRarity().equals(CardRarity.Special))
                .filter(pc -> !(pc.getEdition().equals(not.getEdition())
                        && pc.getCollectorNumber().equals(not.getCollectorNumber())))
                .reduce((first, second) -> second) // last print: furthest from the default pick
                .orElseThrow(() -> new SkipException("card has a single print"));
    }

    private BiPredicate<String, String> onlyPrint(PaperCard target) {
        String setCode = edition(target).getScryfallCode();
        String collectorNumber = target.getCollectorNumber();
        return (set, cn) -> set.equalsIgnoreCase(setCode) && cn.equals(collectorNumber);
    }

    private void usePreference(CardArtPreference pref) {
        cardDb.setCardArtPreference(pref.latestFirst, pref.filterSets, pref.blackBorderOnly);
    }

    // preference model

    @Test
    public void testPreferenceFactoryMapsEveryCombination() {
        assertEquals(CardArtPreference.of(true, false, false), CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(CardArtPreference.of(true, true, false), CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        assertEquals(CardArtPreference.of(false, false, false), CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        assertEquals(CardArtPreference.of(false, true, false), CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        assertEquals(CardArtPreference.of(false, false, true), CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS);
        assertEquals(CardArtPreference.of(false, true, true), CardArtPreference.ORIGINAL_ART_BLACK_BORDER_CORE_EXPANSIONS_REPRINT_ONLY);
        // Black Border only exists for Original Art: Latest Art ignores it.
        assertEquals(CardArtPreference.of(true, false, true), CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(CardArtPreference.of(true, true, true), CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);
    }

    @Test
    public void testAcceptFollowsBorderAndSetType() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            for (CardEdition ed : data.getEditions().getOrderedEditions()) {
                boolean expected = (!pref.blackBorderOnly || isBlack(ed))
                        && (!pref.filterSets || ALLOWED_TYPES.contains(ed.getType()));
                assertEquals(pref.accept(ed), expected, pref + " / " + ed.getCode());
            }
            assertFalse(pref.accept(null), pref + " must reject a null edition");
        }
    }

    @Test
    public void testEditionRanks() {
        boolean sawOriginal = false, sawOther = false, sawCompilation = false;
        for (CardEdition ed : data.getEditions().getOrderedEditions()) {
            for (CardArtPreference pref : CardArtPreference.values()) {
                int rank = pref.getEditionRank(ed);
                if (!pref.blackBorderOnly) {
                    assertEquals(rank, CardArtPreference.RANK_ORIGINAL, "only Black Border ranks editions: " + ed.getCode());
                    continue;
                }
                Type type = ed.getType();
                if (type == Type.CORE || type == Type.EXPANSION) {
                    assertEquals(rank, CardArtPreference.RANK_ORIGINAL, ed.getCode());
                    sawOriginal = true;
                } else if (EnumSet.of(Type.REPRINT, Type.PROMO, Type.COLLECTOR_EDITION, Type.ONLINE,
                        Type.BOXED_SET, Type.DUEL_DECK).contains(type)) {
                    assertEquals(rank, CardArtPreference.RANK_COMPILATION, ed.getCode());
                    sawCompilation = true;
                } else {
                    assertEquals(rank, CardArtPreference.RANK_OTHER, ed.getCode());
                    sawOther = true;
                }
            }
        }
        assertTrue(sawOriginal && sawOther && sawCompilation, "edition data should contain every rank");
        assertEquals(CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS.getEditionRank(null), CardArtPreference.RANK_ORIGINAL);
    }

    @Test
    public void testComparatorOrdersBorderThenRankThenDate() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            List<CardEdition> editions = new ArrayList<>();
            data.getEditions().getOrderedEditions().forEach(editions::add);
            for (CardEdition a : editions) {
                // Spot check against a handful of neighbours rather than the full O(n^2) matrix.
                for (CardEdition b : editions.subList(0, Math.min(editions.size(), 40))) {
                    // The set type filter of the Core/Expansion preferences comes before the border check.
                    if (pref.acceptsSetType(a) != pref.acceptsSetType(b)) {
                        assertEquals(pref.compare(a, b) < 0, pref.acceptsSetType(a), pref + " allowed set types come first");
                        continue;
                    }
                    int cmp = pref.compare(a, b);
                    assertEquals(Integer.signum(pref.compare(b, a)), -Integer.signum(cmp), pref + " must be antisymmetric");
                    if (pref.blackBorderOnly && isBlack(a) != isBlack(b)) {
                        assertEquals(cmp < 0, isBlack(a), "black borders come first");
                    }
                }
            }
        }
    }

    // getCardFromEditions

    @Test
    public void testEveryPreferencePicksTheBestAcceptedEdition() {
        int checked = 0;
        for (CardArtPreference pref : CardArtPreference.values()) {
            for (String name : CARD_NAMES) {
                List<PaperCard> accepted = acceptedPrints(name, pref);
                if (accepted.isEmpty()) {
                    continue;
                }
                PaperCard picked = cardDb.getCardFromEditions(name, pref);
                assertNotNull(picked, pref + " / " + name);
                CardEdition pickedEdition = edition(picked);
                assertTrue(pref.accept(pickedEdition), pref + " / " + name + " picked " + picked.getEdition()
                        + " which the preference does not allow");
                long[] best = accepted.stream().map(pc -> key(pref, edition(pc)))
                        .min(this::compareKeys).orElseThrow();
                assertEquals(compareKeys(key(pref, pickedEdition), best), 0,
                        pref + " / " + name + " picked " + picked.getEdition() + " instead of the best accepted edition");
                checked++;
            }
        }
        assertTrue(checked > CARD_NAMES.length, "the card list should exercise most preferences");
    }

    @Test
    public void testLatestArtPicksNewestAndOriginalArtPicksOldest() {
        for (String name : CARD_NAMES) {
            List<PaperCard> prints = acceptedPrints(name, CardArtPreference.LATEST_ART_ALL_EDITIONS);
            if (prints.size() < 2) {
                continue;
            }
            long newest = prints.stream().mapToLong(pc -> edition(pc).getDate().getTime()).max().orElseThrow();
            long oldest = prints.stream().mapToLong(pc -> edition(pc).getDate().getTime()).min().orElseThrow();
            assertEquals(edition(cardDb.getCardFromEditions(name, CardArtPreference.LATEST_ART_ALL_EDITIONS))
                    .getDate().getTime(), newest, name + " latest");
            assertEquals(edition(cardDb.getCardFromEditions(name, CardArtPreference.ORIGINAL_ART_ALL_EDITIONS))
                    .getDate().getTime(), oldest, name + " original");
        }
    }

    @Test
    public void testBlackBorderPreferencesNeverPickAWhiteBorderWhenABlackOneExists() {
        for (CardArtPreference pref : BLACK_BORDER_PREFERENCES) {
            for (String name : CARD_NAMES) {
                if (acceptedPrints(name, pref).isEmpty()) {
                    continue;
                }
                PaperCard picked = cardDb.getCardFromEditions(name, pref);
                assertNotNull(picked, name);
                assertTrue(isBlack(edition(picked)), pref + " / " + name + " picked " + picked.getEdition()
                        + " (" + edition(picked).getBorderColor() + ")");
            }
        }
    }

    @Test
    public void testBlackBorderRanksCoreAndExpansionBeforePromosAndCompilations() {
        CardArtPreference pref = CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS;
        int checked = 0;
        for (String name : CARD_NAMES) {
            List<PaperCard> black = acceptedPrints(name, pref);
            boolean hasOriginal = black.stream().anyMatch(pc ->
                    pref.getEditionRank(edition(pc)) == CardArtPreference.RANK_ORIGINAL);
            if (!hasOriginal) {
                continue;
            }
            PaperCard picked = cardDb.getCardFromEditions(name, pref);
            Type type = edition(picked).getType();
            assertTrue(type == Type.CORE || type == Type.EXPANSION,
                    name + " picked " + picked.getEdition() + " (" + type + ") although a Core/Expansion print exists");
            checked++;
        }
        assertTrue(checked > 0);
    }

    @Test
    public void testBlackBorderPicksTraditionalFrame() {
        CardArtPreference pref = CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS;
        for (String name : CARD_NAMES) {
            if (acceptedPrints(name, pref).isEmpty()) {
                continue;
            }
            PaperCard picked = cardDb.getCardFromEditions(name, pref);
            if (!cardDb.getTraditionalFramePrints(picked).isEmpty()) {
                assertTrue(cardDb.isTraditionalFramePrint(picked), name + " picked a special frame: "
                        + picked.getEdition() + " #" + picked.getCollectorNumber());
            }
        }
    }

    @Test
    public void testPreferenceDoesNotChangeTheCardIdentity() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            for (String name : CARD_NAMES) {
                PaperCard picked = cardDb.getCardFromEditions(name, pref);
                if (picked != null) {
                    assertEquals(picked.getName(), name);
                }
            }
        }
    }

    // unique card index + language

    @Test
    public void testUniqueIndexFollowsEveryPreference() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            usePreference(pref);
            assertEquals(cardDb.getCardArtPreference(), pref);
            for (String name : CARD_NAMES) {
                PaperCard unique = cardDb.getUniqueByName(name);
                assertNotNull(unique, pref + " / " + name);
                if (pref.blackBorderOnly && acceptedPrints(name, pref).stream().anyMatch(pc -> !pc.getRarity().equals(CardRarity.Special))) {
                    assertTrue(isBlack(edition(unique)), pref + " / " + name + " unique print " + unique.getEdition());
                }
            }
        }
    }

    @Test
    public void testPreferredLanguagePrintWinsTheUniqueIndex() {
        usePreference(CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        for (String name : new String[] { "Shivan Dragon", "Lightning Bolt", "Counterspell" }) {
            PaperCard baseline = cardDb.getUniqueByName(name);
            PaperCard target = otherPrint(baseline, cardDb.getAllCardsNoAlt(name));

            cardDb.setPreferredLanguageAvailability(onlyPrint(target));
            PaperCard unique = cardDb.getUniqueByName(name);
            assertEquals(unique.getEdition(), target.getEdition(), name + " should use the print available in the preferred language");
            assertEquals(unique.getCollectorNumber(), target.getCollectorNumber(), name);

            cardDb.setPreferredLanguageAvailability(null);
        }
    }

    @Test
    public void testEnglishFallbackWhenNoPrintHasThePreferredLanguage() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            usePreference(pref);
            cardDb.setPreferredLanguageAvailability(null);
            PaperCard[] baseline = Arrays.stream(CARD_NAMES).map(cardDb::getUniqueByName).toArray(PaperCard[]::new);

            // The preferred language exists for nothing: the normal (English) choice must be untouched.
            cardDb.setPreferredLanguageAvailability((set, cn) -> false);
            for (int i = 0; i < CARD_NAMES.length; i++) {
                PaperCard unique = cardDb.getUniqueByName(CARD_NAMES[i]);
                assertEquals(unique.getEdition(), baseline[i].getEdition(), pref + " / " + CARD_NAMES[i]);
                assertEquals(unique.getCollectorNumber(), baseline[i].getCollectorNumber(), pref + " / " + CARD_NAMES[i]);
            }
        }
    }

    @Test
    public void testClearingTheLanguagePreferenceRestoresTheDefaultChoice() {
        usePreference(CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        PaperCard baseline = cardDb.getUniqueByName("Shivan Dragon");
        PaperCard target = otherPrint(baseline, cardDb.getAllCardsNoAlt("Shivan Dragon"));

        cardDb.setPreferredLanguageAvailability(onlyPrint(target));
        assertEquals(cardDb.getUniqueByName("Shivan Dragon").getEdition(), target.getEdition());

        cardDb.setPreferredLanguageAvailability(null);
        PaperCard restored = cardDb.getUniqueByName("Shivan Dragon");
        assertEquals(restored.getEdition(), baseline.getEdition());
        assertEquals(restored.getCollectorNumber(), baseline.getCollectorNumber());
    }

    @Test
    public void testLanguageAppliesUnderEveryPreferenceButNeverBreaksBlackBorder() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            usePreference(pref);
            cardDb.setPreferredLanguageAvailability(null);
            for (String name : new String[] { "Shivan Dragon", "Lightning Bolt", "Serra Angel" }) {
                PaperCard baseline = cardDb.getUniqueByName(name);
                List<PaperCard> candidates = cardDb.getAllCardsNoAlt(name).stream()
                        .filter(pc -> edition(pc) != null)
                        .filter(pc -> !pc.getRarity().equals(CardRarity.Special))
                        .filter(pc -> !pref.blackBorderOnly || isBlack(edition(pc)))
                        .collect(Collectors.toList());
                if (candidates.size() < 2) {
                    continue;
                }
                PaperCard target = otherPrint(baseline, candidates);
                cardDb.setPreferredLanguageAvailability(onlyPrint(target));
                PaperCard unique = cardDb.getUniqueByName(name);
                assertEquals(unique.getEdition(), target.getEdition(), pref + " / " + name);
                assertEquals(unique.getCollectorNumber(), target.getCollectorNumber(), pref + " / " + name);
                cardDb.setPreferredLanguageAvailability(null);
            }
        }
    }

    @Test
    public void testLanguageNeverBeatsBlackBorderWithAWhiteBorderedTranslation() {
        for (CardArtPreference pref : BLACK_BORDER_PREFERENCES) {
            usePreference(pref);
            cardDb.setPreferredLanguageAvailability(null);
            int checked = 0;
            for (String name : CARD_NAMES) {
                List<PaperCard> prints = cardDb.getAllCardsNoAlt(name).stream()
                        .filter(pc -> edition(pc) != null)
                        .filter(pc -> !pc.getRarity().equals(CardRarity.Special))
                        .collect(Collectors.toList());
                boolean hasBlack = prints.stream().anyMatch(pc -> isBlack(edition(pc)));
                PaperCard white = prints.stream().filter(pc -> !isBlack(edition(pc))).findFirst().orElse(null);
                if (!hasBlack || white == null) {
                    continue;
                }
                // Only the white-bordered print exists in the preferred language.
                cardDb.setPreferredLanguageAvailability(onlyPrint(white));
                PaperCard unique = cardDb.getUniqueByName(name);
                assertTrue(isBlack(edition(unique)), pref + " / " + name + " switched to white-bordered "
                        + unique.getEdition() + " because of the language preference");
                cardDb.setPreferredLanguageAvailability(null);
                checked++;
            }
            assertTrue(checked > 0, "card list should contain a card with white and black prints");
        }
    }

    // pivot edition and generated decks

    private CardEdition editionOfType(Type type, int minCards, java.util.Set<String> excludeCodes) {
        CardEdition found = null;
        for (CardEdition ed : data.getEditions().getOrderedEditions()) {
            if (ed.getType() != type || excludeCodes.contains(ed.getCode()))
                continue;
            if (cardDb.getAllCards(ed).stream().map(PaperCard::getName).distinct().count() >= minCards)
                found = ed; // keep the newest
        }
        return found;
    }

    private void addDistinctCards(CardPool pool, CardEdition ed, int count) {
        cardDb.getAllCards(ed).stream()
                .filter(pc -> !pc.getRules().getType().isBasicLand())
                .collect(Collectors.toMap(PaperCard::getName, pc -> pc, (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream().limit(count).forEach(pc -> pool.add(pc, 1));
    }

    @Test
    public void testPivotEditionIgnoresPromosForCoreExpansionPreferences() {
        CardEdition promo = editionOfType(Type.PROMO, 4, java.util.Collections.emptySet());
        CardEdition core = editionOfType(Type.CORE, 2, java.util.Collections.emptySet());
        if (promo == null || core == null) {
            throw new SkipException("edition data has no suitable promo/core set");
        }
        for (CardArtPreference pref : new CardArtPreference[] {
                CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY,
                CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY,
                CardArtPreference.ORIGINAL_ART_BLACK_BORDER_CORE_EXPANSIONS_REPRINT_ONLY }) {
            usePreference(pref);
            CardPool pool = new CardPool();
            addDistinctCards(pool, promo, 4);   // the promo set has the most cards...
            addDistinctCards(pool, core, 2);
            CardEdition pivot = pool.getPivotCardEdition(pref.latestFirst);
            assertNotNull(pivot, pref.name());
            assertTrue(pref.acceptsSetType(pivot), pref + " pivot " + pivot.getCode() + " is a " + pivot.getType()
                    + " set, not an allowed Core/Expansion/Reprint set");
        }
    }

    @Test
    public void testPivotEditionStillUsesPromosWhenTheDeckHasNothingElse() {
        CardEdition promo = editionOfType(Type.PROMO, 3, java.util.Collections.emptySet());
        if (promo == null) {
            throw new SkipException("edition data has no promo set");
        }
        usePreference(CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        CardPool pool = new CardPool();
        addDistinctCards(pool, promo, 3);
        assertEquals(pool.getPivotCardEdition(true), promo);
    }

    @Test
    public void testGeneratedDeckPoolKeepsThePreferredPrintWhateverTheOrder() {
        for (CardArtPreference pref : CardArtPreference.values()) {
            usePreference(pref);
            for (String name : CARD_NAMES) {
                List<PaperCard> prints = cardDb.getAllCardsNoAlt(name).stream()
                        .filter(pc -> edition(pc) != null).collect(Collectors.toList());
                if (prints.size() < 2) {
                    continue;
                }
                DeckGenPool forward = new DeckGenPool(prints);
                List<PaperCard> reversed = new ArrayList<>(prints);
                java.util.Collections.reverse(reversed);
                DeckGenPool backward = new DeckGenPool(reversed);

                PaperCard a = forward.getCard(name);
                PaperCard b = backward.getCard(name);
                assertEquals(a.getEdition() + "#" + a.getCollectorNumber(), b.getEdition() + "#" + b.getCollectorNumber(),
                        pref + " / " + name + ": the kept print must not depend on the iteration order");
                CardEdition best = prints.stream().map(this::edition).min(pref).orElseThrow();
                assertEquals(pref.compare(edition(a), best), 0, pref + " / " + name + " kept " + a.getEdition());
            }
        }
    }

    // image language download fallback

    @Test
    public void testCardArtLanguageFallsBackToEnglishWhenNotAvailable() {
        // English / empty / already the edition language: no lookup, the default language is used.
        assertEquals(CdnUuidCache.resolvePreferredLangCode("en", "lea", "175", "en"), "en");
        assertEquals(CdnUuidCache.resolvePreferredLangCode("", "lea", "175", "en"), "en");
        assertEquals(CdnUuidCache.resolvePreferredLangCode(null, "lea", "175", "en"), "en");
        assertEquals(CdnUuidCache.resolvePreferredLangCode("es", "lea", "175", "es"), "es");
        // Spanish asked for a set that has no Spanish data cached: English is used.
        assertEquals(CdnUuidCache.resolvePreferredLangCode("es", "zzz-no-such-set", "1", "en"), "en");
        assertFalse(CdnUuidCache.isAvailableInLanguage("zzz-no-such-set", "1", "es"));
        assertFalse(CdnUuidCache.isAvailableInLanguage(null, "1", "es"));
    }
}
