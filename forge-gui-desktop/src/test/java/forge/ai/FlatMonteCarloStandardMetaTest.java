package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The combo decks of the Standard metagame (main decks only, written out here): each one is read as the combo it is
 * built around, and holding the pieces together is worth more than holding part of them.
 */
public class FlatMonteCarloStandardMetaTest extends AITest {
    private static final String REANIMATOR_4C = "2 Ardyn, the Usurper\n1 Island\n4 Bringer of the Last Gift\n4 Overgrown Tomb\n"
            + "4 Cache Grab\n2 Overlord of the Balemurk\n3 Cavern of Souls\n3 Starting Town\n4 Formidable Speaker\n"
            + "4 Oblivious Bookworm\n4 Breeding Pool\n2 Rapid Rescue\n2 Deceit\n1 Forest\n1 Awaken the Honored Dead\n"
            + "2 Wastewood Verge\n2 Willowrush Verge\n2 Wistfulness\n3 Watery Grave\n2 Terror of the Peaks\n"
            + "4 Seasoned Cryomancer\n4 Superior Spider-Man\n";

    private static final String DIMIR_REANIMATOR = "3 Seasoned Cryomancer\n2 Uldaros Theorix\n3 Theorix Charm\n2 Restless Reef\n"
            + "1 Harvester of Misery\n4 Deceit\n1 Winternight Stories\n5 Swamp\n1 Island\n3 Bitter Triumph\n"
            + "2 Multiversal Passage\n2 Spell Snare\n4 Superior Spider-Man\n4 Shipwreck Marsh\n2 Undercity Sewers\n"
            + "3 The Theorist, Jace Beleren\n4 Requiting Hex\n2 Stock Up\n4 Watery Grave\n1 Strategic Betrayal\n"
            + "4 Gloomlake Verge\n3 Overlord of the Balemurk\n";

    private static final String BANT_AIRBENDING = "1 Craterhoof Behemoth\n1 Forest\n4 Interdimensional Web Watch\n"
            + "4 Aang, Swift Savior\n3 Starting Town\n4 Temple Garden\n2 Floodfarm Verge\n4 Llanowar Elves\n"
            + "3 Hallowed Fountain\n2 Aang, at the Crossroads\n4 Breeding Pool\n4 Bramble Familiar\n1 Island\n"
            + "3 Great Divide Guide\n2 Cavern of Souls\n2 Airbender Ascension\n3 Hushwood Verge\n"
            + "1 Shang-Chi, Master of Kung Fu\n4 Doc Aurlock, Grizzled Genius\n1 Plains\n1 Avatar's Wrath\n"
            + "2 Formidable Speaker\n4 Appa, Steadfast Guardian\n";

    private static final String TEMUR_BATTLECRIER = "4 Devastating Onslaught\n1 Willowrush Verge\n3 Michelangelo's Technique\n"
            + "2 Forest\n4 Interdimensional Web Watch\n1 Stormcarved Coast\n2 Abrade\n1 Dragonhawk, Fate's Tempest\n"
            + "4 Starting Town\n4 Temur Battlecrier\n4 Stomping Ground\n4 Llanowar Elves\n4 Outcaster Trailblazer\n"
            + "4 Breeding Pool\n1 Island\n2 Multiversal Passage\n2 Marang River Regent\n1 Spell Pierce\n"
            + "4 Heartwood Crafter\n2 Quantum Riddler\n1 Riverpyre Verge\n2 Botanical Sanctum\n1 Hex Magic\n"
            + "1 Formidable Speaker\n1 Lavaleaper\n";

    private static final String KONA_OMNISCIENCE = "4 Bounce Off\n4 Formidable Speaker\n4 Kona, Rescue Beastie\n"
            + "3 Marang River Regent\n2 North Wind Avatar\n4 Omniscience\n3 Opt\n2 Spell Pierce\n3 Spell Snare\n"
            + "4 Stock Up\n2 Winternight Stories\n4 Island\n4 Forest\n4 Breeding Pool\n4 Stormcarved Coast\n"
            + "3 Willowrush Verge\n";

    private static final String DIMIR_EXCRUCIATOR = "3 Seasoned Cryomancer\n1 Uldaros Theorix\n2 Theorix Charm\n"
            + "2 Doomsday Excruciator\n4 Restless Reef\n3 Winternight Stories\n4 Deceit\n3 Duress\n7 Swamp\n"
            + "4 Superior Spider-Man\n3 Bitter Triumph\n2 Cavern of Souls\n4 Shipwreck Marsh\n1 Undercity Sewers\n"
            + "1 Deadly Cover-Up\n3 Requiting Hex\n2 Day of Black Sun\n4 Watery Grave\n1 Strategic Betrayal\n"
            + "4 Gloomlake Verge\n2 Emeritus of Ideation\n";

    private Player deal(final String list) {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        for (final String line : list.split("\n")) {
            final int space = line.indexOf(' ');
            if (space < 0) {
                continue;
            }
            for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                addCardToZone(line.substring(space + 1).trim(), p, ZoneType.Library);
            }
        }
        return p;
    }

    private static ComboPlan plan(final Player p) {
        return DeckPlan.forSetting("auto", p).combos;
    }

    @Test
    public void fourColorReanimatorIsReadAsReanimator() {
        final ComboPlan combos = plan(deal(REANIMATOR_4C));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.REANIMATOR));
    }

    @Test
    public void dimirReanimatorWithTheorixIsReadAsReanimator() {
        final ComboPlan combos = plan(deal(DIMIR_REANIMATOR));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.REANIMATOR));
    }

    @Test
    public void aBringerInTheGraveyardWithSpiderManInHandIsWorthMoreThanSpiderManAlone() {
        final Player p = deal(REANIMATOR_4C);
        final ComboPlan combos = plan(p);
        addCardToZone("Superior Spider-Man", p, ZoneType.Hand);
        final int spiderOnly = combos.term(p);
        addCardToZone("Bringer of the Last Gift", p, ZoneType.Graveyard);
        final int withBringer = combos.term(p);
        AssertJUnit.assertTrue("bringer " + withBringer + " vs " + spiderOnly, withBringer > spiderOnly);
    }

    @Test
    public void bantAirbendingIsAFreeCastDeck() {
        final ComboPlan combos = plan(deal(BANT_AIRBENDING));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.FREE_CAST));
    }

    @Test
    public void docAurlockWithAangInHandIsWorthMoreThanAangAlone() {
        final Player p = deal(BANT_AIRBENDING);
        final ComboPlan combos = plan(p);
        addCardToZone("Aang, Swift Savior", p, ZoneType.Hand);
        final int aangOnly = combos.term(p);
        addCardToZone("Doc Aurlock, Grizzled Genius", p, ZoneType.Battlefield);
        final int withDoc = combos.term(p);
        AssertJUnit.assertTrue("doc " + withDoc + " vs " + aangOnly, withDoc > aangOnly);
    }

    @Test
    public void anAirbendIsAimedAtTheOtherCreatureOfTheLoopOnlyWithDocInPlay() {
        final Player p = deal(BANT_AIRBENDING);
        final ComboPlan combos = plan(p);
        final Card aang = addCard("Aang, Swift Savior", p);
        final Card appa = addCard("Appa, Steadfast Guardian", p);
        addCard("Llanowar Elves", p);
        AssertJUnit.assertTrue(combos.airbendLoopTargets(p, aang).isEmpty());
        addCard("Doc Aurlock, Grizzled Genius", p);
        final List<Card> targets = combos.airbendLoopTargets(p, aang);
        AssertJUnit.assertEquals(1, targets.size());
        AssertJUnit.assertSame(appa, targets.get(0));
    }

    @Test
    public void temurBattlecrierIsACreatureCopyDeck() {
        final ComboPlan combos = plan(deal(TEMUR_BATTLECRIER));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.CREATURE_COPY));
    }

    @Test
    public void onslaughtWithABigEnterTriggerIsWorthMoreThanOnslaughtAlone() {
        final Player p = deal(TEMUR_BATTLECRIER);
        final ComboPlan combos = plan(p);
        addCardToZone("Devastating Onslaught", p, ZoneType.Hand);
        final int alone = combos.term(p);
        addCardToZone("Marang River Regent", p, ZoneType.Hand);
        final int together = combos.term(p);
        AssertJUnit.assertTrue("together " + together + " vs " + alone, together > alone);
    }

    @Test
    public void konaOmnisciencePlaysOmniscienceAsAFreeCastEngine() {
        final ComboPlan combos = plan(deal(KONA_OMNISCIENCE));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.FREE_CAST));
    }

    /**
     * Cards of the Standard metagame marked for the heuristic AI to leave out: Springleaf Drum is {@code AI:RemoveDeck:All}
     * (the Momo decks), so it is only ever played through Flat MC's own discovery, never by the heuristics; Demolition
     * Field, Flamebraider, Doorkeeper Thrull, Lunar Convocation and Omniscience are {@code AI:RemoveDeck:Random}, which
     * only drops them from AI built decks. Omniscience stays a piece of the free cast combo, so it is always on offer.
     */
    @Test
    public void theRemoveDeckCardsOfTheMetaAreStillMarked() {
        final Player p = deal(KONA_OMNISCIENCE);
        AssertJUnit.assertTrue(ComputerUtilCard.isCardRemAIDeck(addCardToZone("Springleaf Drum", p, ZoneType.Hand)));
        for (final String name : new String[] { "Demolition Field", "Flamebraider", "Doorkeeper Thrull",
                "Lunar Convocation", "Omniscience" }) {
            AssertJUnit.assertTrue(name, ComputerUtilCard.isCardRemRandomDeck(addCardToZone(name, p, ZoneType.Hand)));
        }
        final ComboPlan combos = plan(p);
        AssertJUnit.assertTrue(combos.isKeyCard(addCardToZone("Omniscience", p, ZoneType.Hand)));
    }

    // ------------------------------------------------------------------------------- Dimir Excruciator

    private static void trimLibrary(final Player p, final int keep) {
        final CardCollection cards = new CardCollection(p.getCardsIn(ZoneType.Library));
        for (int i = keep; i < cards.size(); i++) {
            p.getZone(ZoneType.Library).remove(cards.get(i));
        }
    }

    @Test
    public void dimirExcruciatorIsALibraryCutDeck() {
        final ComboPlan combos = plan(deal(DIMIR_EXCRUCIATOR));
        AssertJUnit.assertTrue(combos.summary(), combos.has(ComboPlan.Kind.LIBRARY_CUT));
    }

    @Test
    public void theExcruciatorIsNotABigCreatureToReanimateForItsSize() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        // reanimating it is a different decision (the cut), never the "big creature" bonus of a reanimator deck
        addCardToZone("Doomsday Excruciator", p, ZoneType.Graveyard);
        addCardToZone("Superior Spider-Man", p, ZoneType.Hand);
        AssertJUnit.assertFalse(combos.summary(), combos.has(ComboPlan.Kind.REANIMATOR));
    }

    @Test
    public void theCutIsOnlySafeWithAFinisher() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        addCard("Swamp", p);
        AssertJUnit.assertFalse(combos.libraryCutSafe(p));
        addCard("Restless Reef", p);
        AssertJUnit.assertTrue(combos.libraryCutSafe(p));
    }

    @Test
    public void insatiableAvariceInHandIsAFinisherToo() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        AssertJUnit.assertFalse(combos.libraryCutSafe(p));
        addCardToZone("Insatiable Avarice", p, ZoneType.Hand);
        AssertJUnit.assertTrue(combos.libraryCutSafe(p));
    }

    @Test
    public void theGuardRefusesTheExcruciatorWithoutAFinisherAndAllowsItWithOne() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        final Card excruciator = addCardToZone("Doomsday Excruciator", p, ZoneType.Hand);
        final forge.game.spellability.SpellAbility cast = excruciator.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertEquals("library-cut-unsafe", ComboGuard.problem(p, cast, null, combos));
        addCard("Restless Reef", p);
        AssertJUnit.assertNull(ComboGuard.problem(p, cast, null, combos));
    }

    @Test
    public void spiderManIsRefusedWhenTheOnlyCreatureToCopyIsTheExcruciatorAndTheCutIsUnsafe() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        addCardToZone("Doomsday Excruciator", p, ZoneType.Graveyard);
        final Card spider = addCardToZone("Superior Spider-Man", p, ZoneType.Hand);
        final forge.game.spellability.SpellAbility cast = spider.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertEquals("library-cut-unsafe", ComboGuard.problem(p, cast, null, combos));
        // another creature to copy: the copy choice avoids the Excruciator, so the cast is fine
        addCardToZone("Seasoned Cryomancer", p, ZoneType.Graveyard);
        AssertJUnit.assertNull(ComboGuard.problem(p, cast, null, combos));
        // and with a finisher it is fine either way
        addCard("Restless Reef", p);
        AssertJUnit.assertNull(ComboGuard.problem(p, cast, null, combos));
    }

    @Test
    public void spiderManCopiesTheExcruciatorOnlyWhenTheCutIsSafe() {
        final Player p = deal(DIMIR_EXCRUCIATOR);
        final ComboPlan combos = plan(p);
        final Card excruciator = addCardToZone("Doomsday Excruciator", p, ZoneType.Graveyard);
        final Card cryomancer = addCardToZone("Seasoned Cryomancer", p, ZoneType.Graveyard);
        final List<Card> options = java.util.Arrays.asList(excruciator, cryomancer);
        AssertJUnit.assertSame("no finisher: the other creature", cryomancer, combos.cloneChoice(p, options));
        addCard("Restless Reef", p);
        AssertJUnit.assertSame("a finisher: the cut", excruciator, combos.cloneChoice(p, options));
        trimLibrary(p, 6);
        AssertJUnit.assertSame("already cut: not again", cryomancer, combos.cloneChoice(p, options));
    }

    @Test
    public void afterTheCutTheRaceDecidesTheValue() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (final String line : DIMIR_EXCRUCIATOR.split("\n")) {
            for (int n = Integer.parseInt(line.substring(0, line.indexOf(' '))); n > 0; n--) {
                addCardToZone(line.substring(line.indexOf(' ') + 1), p, ZoneType.Library);
            }
        }
        final ComboPlan combos = plan(p);
        trimLibrary(p, 6);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        final int noFinisher = combos.term(p);
        addCard("Restless Reef", p);
        final int withReef = combos.term(p);
        AssertJUnit.assertTrue("reef " + withReef + " vs " + noFinisher, withReef > noFinisher);
        for (final Card c : new CardCollection(opp.getCardsIn(ZoneType.Library))) {
            opp.getZone(ZoneType.Library).remove(c);
        }
        AssertJUnit.assertTrue("an empty opponent library is the win", combos.term(p) > withReef);
    }

    // ------------------------------------------------------------------------- the airbend loop and its cap

    @Test
    public void semblanceAnvilWithACreatureImprintedMakesTheRecastFreeToo() {
        final Player p = deal(BANT_AIRBENDING);
        final ComboPlan combos = plan(p);
        final Card aang = addCard("Aang, Swift Savior", p);
        final Card appa = addCard("Appa, Steadfast Guardian", p);
        final Card anvil = addCard("Semblance Anvil", p);
        AssertJUnit.assertTrue("nothing imprinted: no reduction", combos.airbendLoopTargets(p, aang).isEmpty());
        anvil.addImprintedCard(addCardToZone("Llanowar Elves", p, ZoneType.Exile));
        final List<Card> targets = combos.airbendLoopTargets(p, aang);
        AssertJUnit.assertEquals(1, targets.size());
        AssertJUnit.assertSame(appa, targets.get(0));
    }

    @Test
    public void otherAangsAndKalastriaHealerAreLoopCreatures() {
        final Player p = deal(BANT_AIRBENDING);
        final ComboPlan combos = plan(p);
        final Card aang = addCard("Aang, Airbending Master", p);
        addCard("Doc Aurlock, Grizzled Genius", p);
        final Card healer = addCard("Kalastria Healer", p);
        final List<Card> targets = combos.airbendLoopTargets(p, aang);
        AssertJUnit.assertTrue(targets.toString(), targets.contains(healer));
        AssertJUnit.assertFalse("never the creature that makes the trigger", targets.contains(aang));
    }

    @Test
    public void theLoopCapGrowsWithAPayoffInPlayAndTheOpponentsLife() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (final String line : BANT_AIRBENDING.split("\n")) {
            for (int n = Integer.parseInt(line.substring(0, line.indexOf(' '))); n > 0; n--) {
                addCardToZone(line.substring(line.indexOf(' ') + 1), p, ZoneType.Library);
            }
        }
        final ComboPlan combos = plan(p);
        AssertJUnit.assertEquals(12, combos.airbendLoopCap(p));
        addCard("Bontu's Monument", p);
        AssertJUnit.assertEquals(opp.getLife() + 10, combos.airbendLoopCap(p));
    }
}
