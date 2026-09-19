package forge.deck;

import forge.StaticData;
import forge.ai.AITest;
import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.io.DeckSerializer;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Rules coverage for the Pauper Commander (PDH) deck format: the banlist, the commons-only 99, the
 * uncommon-commander gate, deck conformance and the format's life total. Extends {@link AITest} for
 * the one-time card database load.
 */
public class PauperCommanderTest extends AITest {
    private static final DeckFormat PDH = DeckFormat.PauperCommander;

    private static PaperCard card(final String name) {
        PaperCard c = StaticData.instance().getCommonCards().getCard(name);
        assertNotNull(c, name + " is missing from the card database");
        return c;
    }

    @Test
    public void bannedCardsAreIllegal() {
        // the two named PDH bans, cards Wizards removed from all constructed formats, and Sol Ring,
        // which Forge only calls "common" through the 30th Anniversary proxy set
        for (String name : new String[] { "Mystic Remora", "Rhystic Study", "Chaos Orb", "Shahrazad",
                "Contract from Below", "Invoke Prejudice", "Pradesh Gypsies", "Stone-Throwing Devils",
                "Crusade", "Jeweled Bird", "Sol Ring" }) {
            assertFalse(PDH.isLegalCard(card(name)), name + " must not be legal in the 99");
        }
    }

    @Test
    public void cardsWithACommonPrintingAnywhereAreLegal() {
        // legality follows the card, not the printing chosen for the deck: these all have rare or
        // mythic printings but were printed at common somewhere, so they stay legal
        for (String name : new String[] { "Counterspell", "Brainstorm", "Abrade", "Lightning Bolt" }) {
            PaperCard c = card(name);
            assertTrue(StaticData.instance().getCommonCards().wasPrintedAtRarity(CardRarity.Common).test(c),
                    name + " is expected to have a common printing");
            assertTrue(PDH.isLegalCard(c), name + " must be legal in the 99");
        }
        // genuinely printed at common in a Commander product, so not a "fake common"
        assertTrue(PDH.isLegalCard(card("Arcane Signet")), "Arcane Signet must be legal in the 99");
    }

    @Test
    public void landsAreLegal() {
        for (String name : new String[] { "Plains", "Island", "Swamp", "Mountain", "Forest", "Wastes" }) {
            assertTrue(PDH.isLegalCard(card(name)), name + " must be legal in the 99");
        }
    }

    @Test
    public void cardsNeverPrintedAtCommonAreIllegal() {
        for (String name : new String[] { "Swiftfoot Boots", "Lightning Greaves", "Palladium Myr",
                "Cyclonic Rift", "Demonic Tutor", "Swords to Plowshares" }) {
            PaperCard c = card(name);
            assertFalse(StaticData.instance().getCommonCards().wasPrintedAtRarity(CardRarity.Common).test(c),
                    name + " is not expected to have a common printing");
            assertFalse(PDH.isLegalCard(c), name + " must not be legal in the 99");
        }
    }

    @Test
    public void commandersMustBeUncommonCreatures() {
        // uncommon creatures qualify, and need not be legendary
        assertTrue(PDH.isLegalCommander(card("Rayblade Trooper")));
        assertTrue(PDH.isLegalCommander(card("Slickshot Lockpicker")));
        // wrong rarity
        assertFalse(PDH.isLegalCommander(card("Llanowar Elves")), "a common creature is not a PDH commander");
        // wrong type
        assertFalse(PDH.isLegalCommander(card("Arcane Signet")), "an artifact is not a PDH commander");
        // rarity above uncommon
        assertFalse(PDH.isLegalCommander(card("Atraxa, Praetors' Voice")),
                "a mythic legend is not a PDH commander");
        // banned names cannot come in through the command zone either
        assertFalse(PDH.isLegalCommander(card("Mystic Remora")), "a banned card is not a PDH commander");
    }

    @Test
    public void deckConformanceRequiresACommanderAndNinetyNine() {
        assertNull(PDH.getDeckConformanceProblem(deck(99)), "1 commander + 99 must conform");
        assertNotNull(PDH.getDeckConformanceProblem(deck(90)), "a 90 card deck must be rejected");

        Deck noCommander = new Deck("no commander");
        for (int i = 0; i < 100; i++) {
            noCommander.getOrCreate(DeckSection.Main).add(card("Plains"));
        }
        assertNotNull(PDH.getDeckConformanceProblem(noCommander),
                "a deck without a commander must be rejected");
    }

    @Test
    public void commanderStaysInTheCommandZone() {
        // the commander is an ordinary uncommon creature, so nothing may move it to the main deck
        Deck d = deck(99);
        assertEquals(d.getCommanders().size(), 1);
        assertEquals(d.getCommanders().get(0).getName(), "Rayblade Trooper");
    }

    @Test
    public void startingLifeIsThirty() {
        RegisteredPlayer pdh = RegisteredPlayer.forVariants(2, EnumSet.of(GameType.PauperCommander),
                deck(99), null, false, null, null);
        assertEquals(pdh.getStartingLife(), 30, "PDH players start at 30 life");

        // and PDH must not pick up Commander's +20
        RegisteredPlayer commander = RegisteredPlayer.forVariants(2, EnumSet.of(GameType.Commander),
                deck(99), null, false, null, null);
        assertEquals(commander.getStartingLife(), 40, "Commander is unaffected");
    }

    @Test
    public void generatedDecksAreLegal() {
        List<PaperCard> pool = commanderPool();
        assertFalse(pool.isEmpty(), "no legal PDH commanders found");

        byte[] colors = { MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK, MagicColor.RED, MagicColor.GREEN };
        List<PaperCard> picks = new ArrayList<>();
        pick(picks, pool, (byte) 0);
        for (byte color : colors) {
            pick(picks, pool, color);
        }
        for (int i = 0; i < colors.length; i++) {
            for (int j = i + 1; j < colors.length; j++) {
                pick(picks, pool, (byte) (colors[i] | colors[j]));
            }
        }

        for (PaperCard commander : picks) {
            for (boolean forAi : new boolean[] { true, false }) {
                Deck d = DeckgenUtil.generatePauperCommanderDeck(commander, forAi);
                assertNotNull(d, "no deck generated for " + commander.getName());
                assertNull(PDH.getDeckConformanceProblem(d),
                        "generated deck for " + commander.getName() + " (forAi=" + forAi + ") is not legal");
            }
        }
    }

    @Test
    public void shippedStarterDecksAreLegal() {
        File dir = new File(ForgeConstants.ASSETS_DIR + "pdh-decks");
        File[] decks = dir.listFiles((d, name) -> name.endsWith(".dck"));
        if (decks == null || decks.length == 0) {
            throw new SkipException("no PDH decks at " + dir);
        }
        for (File f : decks) {
            Deck d = DeckSerializer.fromFile(f);
            assertNotNull(d, f.getName() + " could not be loaded");
            assertNull(PDH.getDeckConformanceProblem(d), f.getName() + " is not a legal PDH deck");
            for (PaperCard commander : d.getCommanders()) {
                assertTrue(PDH.isLegalCommander(commander),
                        f.getName() + " has an illegal commander: " + commander.getName());
            }
        }
    }

    /** A legal deck: Rayblade Trooper plus the given number of Plains. */
    private static Deck deck(final int mainSize) {
        Deck d = new Deck("pdh test deck");
        d.getOrCreate(DeckSection.Commander).add(card("Rayblade Trooper"));
        for (int i = 0; i < mainSize; i++) {
            d.getOrCreate(DeckSection.Main).add(card("Plains"));
        }
        return d;
    }

    private static List<PaperCard> commanderPool() {
        Map<String, PaperCard> byName = new LinkedHashMap<>();
        for (PaperCard c : StaticData.instance().getCommonCards().getAllCards()) {
            if (c.isRebalanced() || c.getName().startsWith("A-")) {
                continue; // Alchemy rebalances aren't real printings
            }
            if (PDH.isLegalCommander(c)) {
                byName.putIfAbsent(c.getName(), c);
            }
        }
        List<PaperCard> pool = new ArrayList<>(byName.values());
        pool.sort(java.util.Comparator.comparing(PaperCard::getName));
        return pool;
    }

    /** Adds the first commander whose colour identity is exactly the given mask. */
    private static void pick(final List<PaperCard> picks, final List<PaperCard> pool, final byte mask) {
        for (PaperCard c : pool) {
            ColorSet ci = c.getRules().getColorIdentity();
            boolean matches = mask == 0 ? ci.isColorless() : ci.hasAllColors(mask) && ci.hasNoColorsExcept(mask);
            if (matches) {
                picks.add(c);
                return;
            }
        }
    }
}
