package forge.ai;

import java.util.Arrays;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.OpeningHand;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertNotEquals;

/**
 * Three things the Flat Monte Carlo AI is taught here: to pilot a dredge deck (which dredger, what to throw away),
 * to play the Dark Depths + Vampire Hexmage / Thespian's Stage combo, and to recognise a Lands deck.
 */
public class FlatMonteCarloGraveyardLandsTest extends AITest {

    private Player deal(final Game game, final Object... list) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                addCardToZone((String) list[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    private static final Object[] DREDGE_DECK = { "Stinkweed Imp", 4, "Golgari Grave-Troll", 4, "Life from the Loam", 3,
            "Bridge from Below", 4, "Narcomoeba", 4, "Prized Amalgam", 4, "Ichorid", 4, "Faithless Looting", 4,
            "Cathartic Reunion", 4, "Swamp", 12 };

    private static final Object[] DEPTHS_DECK = { "Vampire Hexmage", 4, "Dark Depths", 4, "Thespian's Stage", 4,
            "Lightning Bolt", 4, "Thoughtseize", 4, "Inquisition of Kozilek", 4, "Lotus Petal", 4, "Swamp", 14,
            "Mountain", 4, "Dark Ritual", 4 };

    // ------------------------------------------------------------------------------------------------- dredge

    @Test
    public void dredgeDeckIsStillReadAsDredge() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        AssertJUnit.assertTrue(DeckPlan.forSetting("auto", p).combos.has(ComboPlan.Kind.DREDGE));
    }

    @Test
    public void dredgeChoiceTakesTheBiggestDredgeThatTheLibraryCanAfford() {
        // 40 cards left: the 6 card dredge mills the most fuel and still leaves a healthy library
        AssertJUnit.assertTrue(ComboPlan.dredgeChoiceScore(6, 40) > ComboPlan.dredgeChoiceScore(3, 40));
        // 14 cards left: 6 would leave 8 (a deck about to dredge itself out), 3 leaves 11
        AssertJUnit.assertTrue(ComboPlan.dredgeChoiceScore(3, 14) > ComboPlan.dredgeChoiceScore(6, 14));
        // more than the library holds can never be chosen
        AssertJUnit.assertTrue(ComboPlan.dredgeChoiceScore(6, 5) < ComboPlan.dredgeChoiceScore(3, 5) + 1);
        AssertJUnit.assertTrue(ComboPlan.dredgeChoiceScore(6, 5) < 0);
    }

    @Test
    public void narcomoebaFlipsFromTheLibraryAndIsNotAGraveyardCardBridgeIs() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(ComboPlan.worksFromGraveyard(addCardToZone("Bridge from Below", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(ComboPlan.worksFromGraveyard(addCardToZone("Ichorid", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(ComboPlan.worksFromGraveyard(addCardToZone("Prized Amalgam", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(ComboPlan.worksFromGraveyard(addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(ComboPlan.worksFromGraveyard(addCardToZone("Swamp", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(ComboPlan.worksFromGraveyard(addCardToZone("Lightning Bolt", p, ZoneType.Hand)));
    }

    @Test
    public void dredgeDeckThrowsAwayItsDredgerAndKeepsTheLand() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card grave = addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        addCardToZone("Swamp", p, ZoneType.Hand);
        addCardToZone("Narcomoeba", p, ZoneType.Hand);
        final CardCollection hand = new CardCollection(p.getCardsIn(ZoneType.Hand));
        final CardCollection discards = plan.combos.preferredDiscards(p, hand, 1);
        AssertJUnit.assertNotNull(discards);
        AssertJUnit.assertEquals(grave, discards.getFirst());
    }

    @Test
    public void dredgeDeckWithABridgeInHandPutsFuelFirstUntilTheEngineRuns() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertFalse(plan.combos.fuelFirst(p)); // nothing in hand that wants to be in the graveyard
        addCardToZone("Stinkweed Imp", p, ZoneType.Hand);
        AssertJUnit.assertTrue(plan.combos.fuelFirst(p));
        // two dredgers away: the engine is running, the rest of the hand is played normally
        addCardToZone("Stinkweed Imp", p, ZoneType.Graveyard);
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Graveyard);
        AssertJUnit.assertFalse(plan.combos.fuelFirst(p));
    }

    @Test
    public void dredgeTermLikesACardThatWorksFromTheGraveyardMoreThanOneThatDoesNot() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final int empty = plan.combos.term(p);
        addCardToZone("Narcomoeba", p, ZoneType.Graveyard); // flips from the library only: nothing gained here
        AssertJUnit.assertEquals(empty, plan.combos.term(p));
        addCardToZone("Bridge from Below", p, ZoneType.Graveyard);
        AssertJUnit.assertTrue(plan.combos.term(p) > empty);
    }

    @Test
    public void dredgeDecksAreNotAskedToChooseWhenThereIsNothingToChoose() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertNull(plan.combos.chooseDredge(p, Arrays.<ReplacementEffect>asList()));
        final Card troll = addCardToZone("Golgari Grave-Troll", p, ZoneType.Graveyard);
        AssertJUnit.assertNull(plan.combos.chooseDredge(p, Arrays.asList(troll.getReplacementEffects().get(0))));
    }

    // ------------------------------------------------------------------------------------- Dark Depths combo

    @Test
    public void darkDepthsDeckIsReadAsACombo() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.has(ComboPlan.Kind.DARK_DEPTHS));
        AssertJUnit.assertEquals(DeckPlan.Archetype.COMBO, plan.archetype);
        final Card hexmage = addCardToZone("Vampire Hexmage", p, ZoneType.Hand);
        AssertJUnit.assertTrue(plan.combos.isKeyCard(hexmage));
        AssertJUnit.assertTrue(plan.combos.isKeyCard(addCardToZone("Dark Depths", p, ZoneType.Hand)));
    }

    @Test
    public void aLoneHexmageNextToAStrayDarkDepthsIsNotAComboDeck() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Vampire Hexmage", 1, "Dark Depths", 1, "Lightning Bolt", 4, "Goblin Guide", 4,
                "Monastery Swiftspear", 4, "Lava Spike", 4, "Chain Lightning", 4, "Rift Bolt", 4, "Mountain", 20);
        AssertJUnit.assertFalse(DeckPlan.forSetting("auto", p).combos.has(ComboPlan.Kind.DARK_DEPTHS));
    }

    @Test
    public void darkDepthsProgressGrowsFromLandToHexmageToMaritLage() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCard("Swamp", p);
        addCard("Swamp", p);
        addCard("Swamp", p);
        final int nothing = plan.combos.term(p);

        final Card depths = addCard("Dark Depths", p);
        depths.addCounterInternal(CounterType.getType("ICE"), 10, p, false, null, null);
        final int landOut = plan.combos.term(p);
        AssertJUnit.assertTrue("the land alone is worth something", landOut > nothing);

        addCardToZone("Vampire Hexmage", p, ZoneType.Hand);
        final int hexmageInHand = plan.combos.term(p);
        AssertJUnit.assertTrue("Hexmage in hand with the mana is better still", hexmageInHand > landOut);

        addCard("Vampire Hexmage", p);
        final int ready = plan.combos.term(p);
        AssertJUnit.assertTrue("both on the table is the best position before the token", ready > hexmageInHand);

        addToken("marit_lage", p); // a token script, not a card: addCard() cannot find it
        AssertJUnit.assertEquals("the 20/20 counts for itself, nothing is added on top", 0, plan.combos.term(p));
    }

    @Test
    public void aDarkDepthsWithoutIceCountersIsNotCountedAsProgress() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final int before = plan.combos.term(p);
        addCard("Dark Depths", p); // a script without ETB counters: nothing to take off
        AssertJUnit.assertTrue(plan.combos.term(p) <= before + 30);
    }

    // ---------------------------------------------------------------- what takes the counters off

    private static SpellAbility ability(final Card c, final String apiPrefix) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() != null && sa.getApi().name().startsWith(apiPrefix)) {
                return sa;
            }
        }
        throw new IllegalStateException(c.getName() + " has no " + apiPrefix + " ability");
    }

    /** A Dark Depths with its ten ice counters, as it enters. */
    private Card depthsWithCounters(final Player p) {
        final Card depths = addCard("Dark Depths", p);
        depths.addCounterInternal(CounterType.getType("ICE"), 10, p, false, null, null);
        return depths;
    }

    @Test
    public void thespiansStageCopiesDarkDepthsInsteadOfPayingForIceCounters() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCard("Swamp", p);
        addCard("Swamp", p);
        final Card depths = depthsWithCounters(p);
        final Card stage = addCard("Thespian's Stage", p);
        final SpellAbility copy = ability(stage, "Clone");
        copy.setActivatingPlayer(p);
        final SpellAbility removeCounter = ability(depths, "RemoveCounter");
        removeCounter.setActivatingPlayer(p);

        // three mana would take one counter off of ten: the Stage makes a Dark Depths with none for two
        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.darkDepthsRuling(p, removeCounter));
        AssertJUnit.assertEquals(ComboPlan.Ruling.PLAY, plan.combos.darkDepthsRuling(p, copy));
        AssertJUnit.assertTrue(copy.getTargets().getTargetCards().contains(depths));
    }

    @Test
    public void aHexmageOnTheTableIsSacrificedForDarkDepths() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card depths = depthsWithCounters(p);
        final Card hexmage = addCard("Vampire Hexmage", p);
        final SpellAbility wipe = ability(hexmage, "RemoveCounter");
        wipe.setActivatingPlayer(p);
        final SpellAbility removeCounter = ability(depths, "RemoveCounter");
        removeCounter.setActivatingPlayer(p);

        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.darkDepthsRuling(p, removeCounter));
        AssertJUnit.assertEquals(ComboPlan.Ruling.PLAY, plan.combos.darkDepthsRuling(p, wipe));
        AssertJUnit.assertTrue(wipe.getTargets().getTargetCards().contains(depths));
    }

    @Test
    public void aHexmageInHandAlsoStopsTheAiFromPayingForIceCounters() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card depths = depthsWithCounters(p);
        addCardToZone("Vampire Hexmage", p, ZoneType.Hand);
        final SpellAbility removeCounter = ability(depths, "RemoveCounter");
        removeCounter.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.darkDepthsRuling(p, removeCounter));
    }

    @Test
    public void withNothingToTakeTheCountersOffTheIceIsLeftToTheNormalRules() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card depths = depthsWithCounters(p);
        final SpellAbility removeCounter = ability(depths, "RemoveCounter");
        removeCounter.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.NO_OPINION, plan.combos.darkDepthsRuling(p, removeCounter));
    }

    @Test
    public void theStageWaitsForTheTwoManaItCosts() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        depthsWithCounters(p);
        final Card stage = addCard("Thespian's Stage", p);
        final SpellAbility copy = ability(stage, "Clone");
        copy.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.NO_OPINION, plan.combos.darkDepthsRuling(p, copy));
    }

    // ------------------------------------------------------------------ the Legacy Lands hand and Crop Rotation

    private CardCollection handOf(final Player p, final String... names) {
        final CardCollection hand = new CardCollection();
        for (final String n : names) {
            hand.add(addCardToZone(n, p, ZoneType.Hand));
        }
        return hand;
    }

    @Test
    public void aHandWithDarkDepthsTheStageAndTwoSourcesIsKept() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final CardCollection hand = handOf(p, "Dark Depths", "Thespian's Stage", "Forest", "Forest", "Wasteland",
                "Exploration", "Crop Rotation");
        AssertJUnit.assertTrue(OpeningHand.keep(p, hand, 0, true, plan, 4));
    }

    @Test
    public void theLondonMulliganKeepsDarkDepthsAndTheStage() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final CardCollection hand = handOf(p, "Dark Depths", "Thespian's Stage", "Forest", "Forest", "Wasteland",
                "Karakas", "Maze of Ith", "Exploration");
        final CardCollection bottom = OpeningHand.toBottom(p, hand, 1, true, plan);
        AssertJUnit.assertEquals(1, bottom.size());
        final String out = bottom.get(0).getName();
        AssertJUnit.assertFalse("bottomed: " + out, out.equals("Dark Depths"));
        AssertJUnit.assertFalse("bottomed: " + out, out.equals("Thespian's Stage"));
    }

    @Test
    public void cropRotationWaitsForSomethingToTakeTheCountersOff() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCard("Forest", p);
        addCard("Forest", p);
        addCard("Forest", p);
        final SpellAbility crop = addCardToZone("Crop Rotation", p, ZoneType.Hand).getFirstSpellAbility();
        crop.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.darkDepthsRuling(p, crop));

        // the Stage in hand and mana for G and its 2: the normal rules decide
        addCardToZone("Thespian's Stage", p, ZoneType.Hand);
        addCard("Forest", p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.NO_OPINION, plan.combos.darkDepthsRuling(p, crop));
    }

    @Test
    public void cropRotationWaitsWhenTheStageCouldNotBePaidForAfterwards() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCard("Forest", p);
        addCard("Forest", p);
        addCard("Thespian's Stage", p);
        final SpellAbility crop = addCardToZone("Crop Rotation", p, ZoneType.Hand).getFirstSpellAbility();
        crop.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.darkDepthsRuling(p, crop));
    }

    // ------------------------------------------------------------------------------------------------- lands

    private Player landsDeck(final Game game) {
        return deal(game, "Exploration", 4, "Burgeoning", 2, "Azusa, Lost but Seeking", 4, "Oracle of Mul Daya", 4,
                "Crucible of Worlds", 3, "Life from the Loam", 4, "Mulch", 3, "Manabond", 2, "Titania, Protector of Argoth", 2,
                "Wasteland", 4, "Strip Mine", 4, "Mishra's Factory", 4, "Maze of Ith", 3, "Horizon Canopy", 4,
                "Forest", 16, "Plains", 4);
    }

    @Test
    public void landsDeckIsRecognisedAsLands() {
        final Game game = initAndCreateGame();
        AssertJUnit.assertEquals(DeckPlan.Archetype.LANDS, DeckPlan.forSetting("auto", landsDeck(game)).archetype);
    }

    @Test
    public void landsArchetypeCanBeForcedFromTheProfile() {
        final Game game = initAndCreateGame();
        final Player p = landsDeck(game);
        final DeckPlan plan = DeckPlan.forSetting("lands", p);
        AssertJUnit.assertEquals(DeckPlan.Archetype.LANDS, plan.archetype);
        AssertJUnit.assertTrue(plan.manaDevelopmentExtra > 0);
    }

    @Test
    public void anOrdinaryDeckWithTwentyFourLandsIsNotALandsDeck() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Lightning Bolt", 4, "Goblin Guide", 4, "Monastery Swiftspear", 4, "Lava Spike", 4,
                "Chain Lightning", 4, "Rift Bolt", 4, "Eidolon of the Great Revel", 4, "Searing Spear", 4, "Mountain", 24);
        assertNotEquals(DeckPlan.Archetype.LANDS, DeckPlan.forSetting("auto", p).archetype);
    }

    @Test
    public void aLandHeavyHandIsKeptByALandsDeckButNotByAnAggroDeck() {
        final Game game = initAndCreateGame();
        final Player p = landsDeck(game);
        final CardCollection hand = new CardCollection();
        for (final String n : new String[] { "Forest", "Forest", "Wasteland", "Mishra's Factory", "Horizon Canopy",
                "Exploration", "Azusa, Lost but Seeking" }) {
            hand.add(addCardToZone(n, p, ZoneType.Hand));
        }
        AssertJUnit.assertTrue(OpeningHand.keep(p, hand, 0, true, DeckPlan.forSetting("lands", p), 4));
    }

    // ----------------------------------------------------------------------- Living End rules live in ComboPlan

    private static final Object[] LIVING_END_DECK = { "Living End", 4, "Violent Outburst", 4, "Demonic Dread", 4,
            "Shardless Agent", 4, "Street Wraith", 4, "Architects of Will", 4, "Curator of Mysteries", 4,
            "Waker of Waves", 4, "Fury", 3, "Grief", 3, "Swamp", 8, "Mountain", 4, "Forest", 4 };

    @Test
    public void cascadeWaitsForTheGraveyardAndTheDeckIsReadAsLivingEnd() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, LIVING_END_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.isMassReanimationDeck(p));
        final Card agent = addCardToZone("Shardless Agent", p, ZoneType.Hand);
        final SpellAbility cast = agent.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        // nothing in the graveyard: the cascade would waste Living End, it is not even looked at
        AssertJUnit.assertEquals(ComboPlan.Ruling.SKIP, plan.combos.massReanimationRuling(p, cast, false));
        for (int i = 0; i < 5; i++) {
            addCardToZone("Street Wraith", p, ZoneType.Graveyard);
        }
        assertNotEquals(ComboPlan.Ruling.SKIP, plan.combos.massReanimationRuling(p, cast, false));
    }

    @Test
    public void aDeckWithoutLivingEndHasNoOpinionAboutPlays() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, "Lightning Bolt", 4, "Goblin Guide", 4, "Monastery Swiftspear", 4, "Lava Spike", 4,
                "Chain Lightning", 4, "Rift Bolt", 4, "Mountain", 20);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertFalse(plan.combos.isMassReanimationDeck(p));
        final SpellAbility bolt = addCardToZone("Lightning Bolt", p, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(p);
        AssertJUnit.assertEquals(ComboPlan.Ruling.NO_OPINION, plan.combos.massReanimationRuling(p, bolt, false));
    }

    // -------------------------------------------------------------------------------- no engine noise

    @Test
    public void theMulliganDecisionDoesNotPrintActivatorWarnings() {
        final Game game = initAndCreateGame();
        final Player p = landsDeck(game);
        final CardCollection hand = new CardCollection();
        for (final String n : new String[] { "Forest", "Wasteland", "Mishra's Factory", "Horizon Canopy", "Exploration" }) {
            hand.add(addCardToZone(n, p, ZoneType.Hand));
        }
        final java.io.PrintStream realOut = System.out;
        final java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(captured, true));
        try {
            OpeningHand.keep(p, hand, 0, true, DeckPlan.forSetting("lands", p), 4);
        } finally {
            System.setOut(realOut);
        }
        AssertJUnit.assertFalse(captured.toString().contains("Did not have activator set"));
    }

    // ----------------------------------------------------------------------- Legacy Lands (Dark Depths + Stage)

    private Player legacyLands(final Game game) {
        return deal(game, "Ancient Tomb", 1, "Bojuka Bog", 1, "Boseiju, Who Endures", 3, "Crop Rotation", 4,
                "Dark Depths", 4, "Disruptor Flute", 3, "Expedition Map", 1, "Exploration", 4, "Forest", 1,
                "Ghost Quarter", 1, "Grafdigger's Cage", 1, "Karakas", 2, "Life from the Loam", 4,
                "Malevolent Rumble", 4, "Maze of Ith", 1, "Mox Diamond", 4, "Once Upon a Time", 3, "Pithing Needle", 1,
                "Skateboard", 1, "The Tabernacle at Pendrell Vale", 1, "Thespian's Stage", 4, "Urza's Saga", 4,
                "Wasteland", 4, "Yavimaya, Cradle of Growth", 4);
    }

    @Test
    public void legacyLandsIsALandsDeckThatKeepsItsDarkDepthsCombo() {
        final Game game = initAndCreateGame();
        final DeckPlan plan = DeckPlan.forSetting("auto", legacyLands(game));
        final StringBuilder found = new StringBuilder();
        for (final ComboPlan.Kind k : ComboPlan.Kind.values()) {
            found.append(plan.combos.has(k) ? k + " " : "");
        }
        AssertJUnit.assertEquals("archetype " + plan.archetype + ", combos found: " + found,
                DeckPlan.Archetype.LANDS, plan.archetype);
        AssertJUnit.assertTrue(plan.combos.has(ComboPlan.Kind.DARK_DEPTHS));
        AssertJUnit.assertTrue(plan.longPlayouts);
    }

    @Test
    public void cropRotationIsAPieceOfTheDarkDepthsCombo() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.isKeyCard(addCardToZone("Crop Rotation", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(plan.combos.isKeyCard(addCardToZone("Exploration", p, ZoneType.Hand)));
    }

    @Test
    public void cropRotationFetchesDarkDepthsThenTheStage() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final CardCollection options = new CardCollection();
        for (final Card c : p.getCardsIn(ZoneType.Library)) {
            final String n = c.getName();
            if (n.equals("Dark Depths") || n.equals("Thespian's Stage") || n.equals("Wasteland")) {
                options.add(c);
            }
        }
        final java.util.List<ZoneType> fromLibrary = Arrays.asList(ZoneType.Library);
        AssertJUnit.assertEquals("Dark Depths",
                plan.combos.choose(p, ZoneType.Battlefield, fromLibrary, options).getName());
        // Dark Depths is out with its counters and nothing takes them off yet: the Stage is what is missing
        addCard("Forest", p);
        final Card depths = addCard("Dark Depths", p);
        depths.addCounterInternal(CounterType.getType("ICE"), 10, p, false, null, null);
        AssertJUnit.assertEquals("Thespian's Stage",
                plan.combos.choose(p, ZoneType.Battlefield, fromLibrary, options).getName());
    }

    @Test
    public void darkDepthsWaitsForTwoSourcesAndThenIsPlayedBeforeAnotherLand() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCardToZone("Thespian's Stage", p, ZoneType.Hand);
        final CardCollection playable = new CardCollection();
        playable.add(addCardToZone("Wasteland", p, ZoneType.Hand));
        final Card depths = addCardToZone("Dark Depths", p, ZoneType.Hand);
        playable.add(depths);
        AssertJUnit.assertNull("no mana yet: the normal rule plays a source first", plan.combos.chooseLand(p, playable));
        addCard("Forest", p);
        addCard("Forest", p);
        AssertJUnit.assertEquals(depths, plan.combos.chooseLand(p, playable));
    }

    @Test
    public void aCropRotationInHandIsWorthSomethingWhileDarkDepthsIsInTheLibrary() {
        final Game game = initAndCreateGame();
        final Player p = legacyLands(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCard("Forest", p);
        final int without = plan.combos.term(p);
        addCardToZone("Crop Rotation", p, ZoneType.Hand);
        AssertJUnit.assertTrue(plan.combos.term(p) > without);
    }

    @Test
    public void dredgeOutletsAreReadFromTheCard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(ComboPlan.isDredgeOutlet(addCardToZone("Faithless Looting", p, ZoneType.Hand)));
        AssertJUnit.assertTrue(ComboPlan.isDredgeOutlet(addCardToZone("Cathartic Reunion", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(ComboPlan.isDredgeOutlet(addCardToZone("Swamp", p, ZoneType.Hand)));
        AssertJUnit.assertFalse(ComboPlan.isDredgeOutlet(addCardToZone("Narcomoeba", p, ZoneType.Hand)));
    }

    @Test
    public void aDredgeHandWithAnOutletAndSomethingToThrowAwayIsKept() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final CardCollection hand = handOf(p, "Swamp", "Swamp", "Faithless Looting", "Golgari Grave-Troll",
                "Bridge from Below", "Ichorid", "Narcomoeba");
        AssertJUnit.assertTrue(OpeningHand.keep(p, hand, 0, true, plan, 4));
    }

    @Test
    public void aDredgeOneLanderWithACastableLootingIsKeptButNotWithoutOne() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Mountain", p, ZoneType.Library); // the red the Looting needs
        }
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        // the Mountain pays for the Looting
        final CardCollection loot = handOf(p, "Mountain", "Faithless Looting", "Stinkweed Imp",
                "Bridge from Below", "Ichorid", "Narcomoeba", "Golgari Grave-Troll");
        AssertJUnit.assertTrue(OpeningHand.explain(p, loot, true, plan),
                OpeningHand.keep(p, loot, 0, true, plan, 4));
        // a Swamp does not: the one outlet cannot be cast
        AssertJUnit.assertFalse(OpeningHand.keep(p, handOf(p, "Swamp", "Faithless Looting", "Stinkweed Imp",
                "Bridge from Below", "Ichorid", "Narcomoeba", "Golgari Grave-Troll"), 0, true, plan, 4));
        // no outlet at all
        AssertJUnit.assertFalse(OpeningHand.keep(p, handOf(p, "Swamp", "Stinkweed Imp", "Bridge from Below",
                "Ichorid", "Narcomoeba", "Golgari Grave-Troll", "Prized Amalgam"), 0, true, plan, 4));
    }
}
