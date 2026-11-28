package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.mulligan.MulliganService;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Keeping and mulliganing opening hands by the deck's plan. */
public class OpeningHandTest extends AITest {
    private Player ai;

    private void setUp(final int islands, final int mountains) {
        final Game game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (int i = 0; i < islands; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        for (int i = 0; i < mountains; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        for (int i = 0; i < 20; i++) { // the spells left in the library
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
    }

    private CardCollection hand(final String... names) {
        final CardCollection hand = new CardCollection();
        for (final String n : names) {
            hand.add(addCardToZone(n, ai, ZoneType.Hand));
        }
        return hand;
    }

    private static final String[] SPELLS = {"Grizzly Bears", "Hill Giant", "Shock", "Air Elemental"};

    private boolean keeps(final DeckPlan plan, final boolean onPlay, final String... names) {
        return OpeningHand.keep(ai, hand(names), 0, onPlay, plan, 5);
    }

    private static String[] mix(final String[] a, final String... b) {
        final String[] r = new String[a.length + b.length];
        System.arraycopy(b, 0, r, 0, b.length);
        System.arraycopy(a, 0, r, b.length, a.length);
        return r;
    }

    /**
     * Runs the real mulligan service (London) for an AI that mulligans twice, then looks at the given hand.
     *
     * @return what the third question was: the cards in hand, how many it was told it still has to put back, and what
     *         the AI answers for the five cards given
     */
    private String thirdLookAfterTwoMulligans(final String... five) {
        setUp(12, 12);
        final Game game = ai.getGame();
        final Player other = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Island", other, ZoneType.Library);
        }
        final String[] third = new String[1];
        final PlayerControllerAi judge = new PlayerControllerAi(game, ai, ai.getLobbyPlayer()) {
            private int asked;

            @Override
            public boolean mulliganKeepHand(final Player first, final int cardsToReturn) {
                if (++asked < 3) {
                    return false;
                }
                if (third[0] == null) {
                    final int size = ai.getCardsIn(ZoneType.Hand).size();
                    for (final Card c : new CardCollection(ai.getCardsIn(ZoneType.Hand))) {
                        game.getAction().moveToLibrary(c, null);
                    }
                    hand(five);
                    third[0] = size + " cards, " + cardsToReturn + " to put back, keep="
                            + super.mulliganKeepHand(first, cardsToReturn);
                }
                return true;
            }
        };
        final PlayerControllerAi keeps = new PlayerControllerAi(game, other, other.getLobbyPlayer()) {
            @Override
            public boolean mulliganKeepHand(final Player first, final int cardsToReturn) {
                return true;
            }
        };
        // as a game does before the mulligans
        ai.drawCards(7);
        other.drawCards(7);
        other.runWithController(() -> ai.runWithController(() -> new MulliganService(ai).perform(), judge), keeps);
        return third[0];
    }

    @Test
    public void afterTwoLondonMulligansTheFiveCardsAreJudgedAsFive() {
        // the two cards were put back while the hand was drawn: there is nothing left to put back
        AssertJUnit.assertEquals("5 cards, 0 to put back, keep=false", thirdLookAfterTwoMulligans(
                "Mountain", "Mountain", "Dissipate", "Spectral Flight", "Invisible Stalker"));
    }

    @Test
    public void aGoodFiveCardHandIsStillKeptAfterTwoLondonMulligans() {
        AssertJUnit.assertEquals("5 cards, 0 to put back, keep=true", thirdLookAfterTwoMulligans(
                "Island", "Island", "Dissipate", "Spectral Flight", "Invisible Stalker"));
    }

    // Card-Forge/forge#4209: spells that cost no mana are no reason to keep a hand without lands

    private void assertNoKeepWithoutLands(final String... cards) {
        setUp(9, 8);
        final CardCollection hand = hand(cards);
        AssertJUnit.assertFalse("the default mulligan", OpeningHand.keep(ai, hand, 0, true,
                DeckPlan.of(DeckPlan.Archetype.AGGRO), 5));
        AssertJUnit.assertTrue("the classic mulligan", ComputerUtil.wantMulligan(ai, 0));
    }

    @Test
    public void freeCreaturesDoNotMakeUpForHavingNoLands() {
        assertNoKeepWithoutLands("Memnite", "Memnite", "Ornithopter", "Ornithopter", "Hill Giant", "Shock",
                "Air Elemental");
    }

    @Test
    public void aPhyrexianManaSpellDoesNotMakeUpForHavingNoLands() {
        assertNoKeepWithoutLands("Mutagenic Growth", "Mutagenic Growth", "Grizzly Bears", "Hill Giant", "Shock",
                "Air Elemental", "Grizzly Bears");
    }

    @Test
    public void aCardWithAPregameActionDoesNotMakeUpForHavingNoLands() {
        assertNoKeepWithoutLands("Leyline of Sanctity", "Memnite", "Grizzly Bears", "Hill Giant", "Shock",
                "Air Elemental", "Grizzly Bears");
    }

    // an artifact with a mana ability is only a source if it can be cast and used as it is

    /** Two of the artifact, no land: the classic mulligan must not count them as two lands. */
    private boolean classicSendsBackTwo(final String artifact) {
        setUp(9, 8);
        hand(artifact, artifact, "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental", "Grizzly Bears");
        return ComputerUtil.wantMulligan(ai, 0);
    }

    @Test
    public void moxDiamondsWithoutALandToDiscardAreNoLands() {
        AssertJUnit.assertTrue(classicSendsBackTwo("Mox Diamond"));
    }

    @Test
    public void moxOpalsWithoutMetalcraftAreNoLands() {
        AssertJUnit.assertTrue(classicSendsBackTwo("Mox Opal"));
    }

    @Test
    public void otherArtifactsThatNeedSomethingFirstAreNoLands() {
        final StringBuilder kept = new StringBuilder();
        for (final String name : new String[] {"Mox Amber", "Chrome Mox", "Lion's Eye Diamond", "Everflowing Chalice",
                "Mox Tantalite"}) {
            if (!classicSendsBackTwo(name)) {
                kept.append(name).append("; ");
            }
        }
        AssertJUnit.assertEquals("", kept.toString());
    }

    @Test
    public void freeArtifactsThatWorkAsTheyAreStillCount() {
        for (final String name : new String[] {"Mox Emerald", "Lotus Petal", "Mana Crypt"}) {
            AssertJUnit.assertFalse(name, classicSendsBackTwo(name));
        }
    }

    /**
     * The hard rule of the default mulligan, which comes before its score: whether two of the artifact make up for
     * there being only one land.
     */
    private boolean oneLandAndTwoPass(final String artifact) {
        setUp(9, 8);
        return OpeningHand.playable(
                hand("Island", artifact, artifact, "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental"), 7);
    }

    @Test
    public void moxDiamondsDoNotMakeUpForASingleLand() {
        AssertJUnit.assertFalse(oneLandAndTwoPass("Mox Diamond"));
    }

    @Test
    public void moxOpalsDoNotMakeUpForASingleLand() {
        AssertJUnit.assertFalse(oneLandAndTwoPass("Mox Opal"));
    }

    @Test
    public void manaArtifactsThatWorkStillMakeUpForASingleLand() {
        // the free ones: the single land has nothing to pay for
        for (final String name : new String[] {"Mox Emerald", "Lotus Petal"}) {
            AssertJUnit.assertTrue(name, oneLandAndTwoPass(name));
        }
    }

    @Test
    public void manaArtifactsThatCostTwoCannotBeCastWithASingleLand() {
        // a Fire Diamond or a Signet costs two: the one land casts neither, so they are no sources on their own
        for (final String name : new String[] {"Fire Diamond", "Azorius Signet"}) {
            AssertJUnit.assertFalse(name, oneLandAndTwoPass(name));
        }
    }

    @Test
    public void aFreeSourceHelpsToCastAnArtifactThatCostsTwo() {
        AssertJUnit.assertTrue(oneLandAndSources("Island", new String[] {"Mox Emerald", "Fire Diamond"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void aSignetNeedsManaOtherThanItsOwnToBeActivated() {
        // the Mox and the Island pay for casting the Signet and for its {1}: two sources; two Signets and the Island
        // alone cannot even cast the first one
        AssertJUnit.assertTrue(oneLandAndSources("Island", new String[] {"Mox Emerald", "Azorius Signet"},
                "Shock", "Shock", "Divination", "Lava Axe"));
        AssertJUnit.assertFalse(oneLandAndSources("Island", new String[] {"Azorius Signet", "Azorius Signet"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void manaFiltersAreNoAdditionalSources() {
        // a Mana Cylix pays {1} for one mana: it changes the colour of the Island's mana and adds none
        AssertJUnit.assertFalse(oneLandAndSources("Island", new String[] {"Mana Cylix", "Mana Cylix"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void restrictedManaWithNothingToSpendItOnIsNoSource() {
        // Creeping Peeper's mana pays for enchantments only, and the hand has none
        AssertJUnit.assertFalse(oneLandAndSources("Island", new String[] {"Mox Sapphire", "Creeping Peeper"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void restrictedManaThatCannotPayTheEligibleSpellIsNoSource() {
        // Pacifism is an enchantment, but it costs {1}{W} and nothing in the hand makes white mana
        AssertJUnit.assertFalse(oneLandAndSources("Island", new String[] {"Mox Sapphire", "Creeping Peeper"},
                "Pacifism", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void restrictedManaThatPaysAnEligibleSpellIsASource() {
        // Curiosity is an enchantment that costs {U}, which the Peeper's {U} pays
        AssertJUnit.assertTrue(oneLandAndSources("Island", new String[] {"Mox Sapphire", "Creeping Peeper"},
                "Curiosity", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void restrictedManaThatPaysOnlyOneColouredPartOfTheSpellIsNoSource() {
        // Mind Over Matter costs {2}{U}{U}{U}{U}: six mana, but only the Island's and the Peeper's are blue.
        // The Moxen and the Sol Ring are four sources, the Peeper is not a fifth.
        AssertJUnit.assertEquals(4, sourcesCounted("Island",
                new String[] {"Mox Ruby", "Mox Emerald", "Mox Jet", "Sol Ring", "Creeping Peeper"},
                "Mind Over Matter"));
    }

    @Test
    public void restrictedManaThatPaysTheWholeSpellIsASource() {
        // Island, two Mox Sapphires and the Peeper make four blue, and the Sol Ring the generic part
        AssertJUnit.assertEquals(4, sourcesCounted("Island",
                new String[] {"Mox Sapphire", "Mox Sapphire", "Sol Ring", "Creeping Peeper"},
                "Mind Over Matter"));
    }

    @Test
    public void comboManaIsOneManaOfOneOfTheColours() {
        // Urborg Elf makes one {B}, {G} or {U}: with the Forest and the Mox it cannot make the {R} of a Hill Giant
        // ({3}{R}), so the Springleaf Drum has no creature to tap. The Mox and the Elf are the two sources.
        AssertJUnit.assertEquals(2, sourcesCounted("Forest",
                new String[] {"Mox Emerald", "Urborg Elf", "Springleaf Drum"}, "Hill Giant"));
    }

    @Test
    public void restrictedLandManaDoesNotPayForAnArtifact() {
        // Ancient Ziggurat's mana is for creature spells: with the Mox it does not make the Fire Diamond's {2}
        AssertJUnit.assertEquals(1, sourcesCounted("Ancient Ziggurat", new String[] {"Mox Emerald", "Fire Diamond"},
                "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental"));
    }

    @Test
    public void restrictedLandManaPaysForACreatureThatIsASource() {
        // the Ziggurat's mana pays for Llanowar Elves ({G}), a creature
        AssertJUnit.assertEquals(1, sourcesCounted("Ancient Ziggurat", new String[] {"Llanowar Elves"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void sliverHivesColouredManaDoesNotPayForANonSliver() {
        // {C} is free to spend, but the Hive's {G} is for Sliver spells: only the Mox is a source, not the Mystic
        AssertJUnit.assertEquals(1, sourcesCounted("Sliver Hive", new String[] {"Mox Sapphire", "Elvish Mystic"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void sliverHiveAndAnElfItCannotCastDoNotMakeUpForASingleLand() {
        AssertJUnit.assertFalse(oneLandAndSources("Sliver Hive", new String[] {"Mox Sapphire", "Elvish Mystic"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void sliverHivesColorlessManaIsStillFreeToSpend() {
        // the Hive's {C} and the Mox pay the Fire Diamond's {2}
        AssertJUnit.assertEquals(2, sourcesCounted("Sliver Hive", new String[] {"Mox Sapphire", "Fire Diamond"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void sliverHiveIsTappedForOnlyOneOfItsAbilities() {
        // {C} and the Sliver-only {G} are alternatives: alone, the Hive cannot pay the {1}{G} of the Muscle Sliver,
        // so the Springleaf Drum it can cast has no creature to tap
        AssertJUnit.assertEquals(0, sourcesCounted("Sliver Hive",
                new String[] {"Muscle Sliver", "Springleaf Drum"}, "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void sliverHivesColouredManaPaysForASliver() {
        // the Hive's {G} and the Mox cast the Muscle Sliver, which the Springleaf Drum can then tap
        AssertJUnit.assertEquals(2, sourcesCounted("Sliver Hive",
                new String[] {"Mox Sapphire", "Muscle Sliver", "Springleaf Drum"}, "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void anotherCardsRestrictedManaDoesNotMakeARestrictedSourceUseful() {
        // the Ziggurat's mana can cast the Bears, but Creeping Peeper's own mana has no enchantment to pay for
        AssertJUnit.assertEquals(1, sourcesCounted("Ancient Ziggurat",
                new String[] {"Mox Sapphire", "Creeping Peeper"}, "Grizzly Bears", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void ancientTombsTwoManaCastsASignet() {
        // the Tomb casts the Signet, then there is mana for the Drum, which taps the Memnite: two sources
        final String[] sources = {"Azorius Signet", "Springleaf Drum", "Memnite"};
        AssertJUnit.assertEquals(2, sourcesCounted("Ancient Tomb", sources, "Grizzly Bears", "Hill Giant", "Shock"));
    }

    @Test
    public void ancientTombWithTwoCheapSourcesMakesUpForASingleLand() {
        AssertJUnit.assertTrue(oneLandAndSources("Ancient Tomb",
                new String[] {"Azorius Signet", "Springleaf Drum", "Memnite"}, "Grizzly Bears", "Hill Giant", "Shock"));
    }

    @Test
    public void landsThatAskForMoreThanATapDoNotCountAllTheirMana() {
        // none of them pays a Fire Diamond's {2} as the only land: Azorius Chancery returns itself, Lotus Vale and
        // Scorched Ruins need two lands to give up, and Phyrexian Tower needs a creature for its second ability
        setUp(9, 8);
        for (final String land : new String[] {"Azorius Chancery", "Lotus Vale", "Scorched Ruins", "Phyrexian Tower"}) {
            AssertJUnit.assertEquals(land, 0,
                    OpeningHand.cheapManaSources(hand(land, "Fire Diamond", "Shock", "Divination", "Lava Axe")));
        }
    }

    @Test
    public void cascadeBluffsMakesNoRedWithoutBlueOrRedToPayForIt() {
        // the Mox Emerald's {G} cannot pay {U/R}: no {R} for the Zhur-Taa Druid, so the Mox is the only source
        AssertJUnit.assertEquals(1, sourcesCounted("Cascade Bluffs", new String[] {"Mox Emerald", "Zhur-Taa Druid"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void cascadeBluffsAndADruidItCannotHelpCastDoNotMakeUpForASingleLand() {
        AssertJUnit.assertFalse(oneLandAndSources("Cascade Bluffs", new String[] {"Mox Emerald", "Zhur-Taa Druid"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void cascadeBluffsMakesRedWhenBluePaysForIt() {
        // the Sapphire pays the Bluffs, whose {R} and the Emerald's {G} cast the Druid
        AssertJUnit.assertEquals(3, sourcesCounted("Cascade Bluffs",
                new String[] {"Mox Sapphire", "Mox Emerald", "Zhur-Taa Druid"}, "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void skycloudExpanseMakesNothingByItself() {
        AssertJUnit.assertEquals(0, sourcesCounted("Skycloud Expanse", new String[] {"Fire Diamond"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void skycloudExpanseMakesTwoWhenAnotherSourcePaysForIt() {
        // the Mox pays the Expanse's {1}, and its {W}{U} casts the Fire Diamond
        AssertJUnit.assertEquals(2, sourcesCounted("Skycloud Expanse", new String[] {"Mox Emerald", "Fire Diamond"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void manaCreaturesTheLandCannotCastAreNoSources() {
        // an Island makes no green mana for the Elves
        AssertJUnit.assertFalse(oneLandAndSources("Island", new String[] {"Llanowar Elves", "Llanowar Elves"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    @Test
    public void manaCreaturesTheLandCanCastAreSources() {
        AssertJUnit.assertTrue(oneLandAndSources("Forest", new String[] {"Llanowar Elves", "Llanowar Elves"},
                "Shock", "Shock", "Divination", "Lava Axe"));
    }

    /** One land and the given sources of mana, around the other cards. */
    private boolean oneLandAndSources(final String land, final String[] sources, final String... others) {
        setUp(9, 8);
        final CardCollection cards = hand(mix(others, withLand(land, sources)));
        return OpeningHand.playable(cards, cards.size());
    }

    /** How many of the sources the hand of one land, the sources and the other cards counts. */
    private int sourcesCounted(final String land, final String[] sources, final String... others) {
        setUp(9, 8);
        return OpeningHand.cheapManaSources(hand(mix(others, withLand(land, sources))));
    }

    private static String[] withLand(final String land, final String[] sources) {
        final String[] fixed = new String[1 + sources.length];
        fixed[0] = land;
        System.arraycopy(sources, 0, fixed, 1, sources.length);
        return fixed;
    }

    /** One land and some Springleaf Drums, which tap an untapped creature besides themselves, around the given cards. */
    private boolean oneLandAndDrums(final String land, final int drums, final String... others) {
        final String[] sources = new String[drums];
        java.util.Arrays.fill(sources, "Springleaf Drum");
        return oneLandAndSources(land, sources, others);
    }

    private boolean oneLandAndTwoDrums(final String... others) {
        return oneLandAndDrums("Island", 2, others);
    }

    @Test
    public void springleafDrumsWithoutACreatureDoNotMakeUpForASingleLand() {
        AssertJUnit.assertFalse(oneLandAndTwoDrums("Shock", "Shock", "Shock", "Divination"));
    }

    @Test
    public void springleafDrumsWithACreatureEachMakeUpForASingleLand() {
        AssertJUnit.assertTrue(oneLandAndTwoDrums("Memnite", "Ornithopter", "Shock", "Lava Axe"));
    }

    @Test
    public void oneCreatureCannotPayForTwoSpringleafDrums() {
        AssertJUnit.assertFalse(oneLandAndTwoDrums("Memnite", "Shock", "Shock", "Divination"));
    }

    @Test
    public void springleafDrumsDoNotTapCreaturesThatTheLandsCannotCast() {
        // two Hill Giants (four mana each) with a single Forest are no creatures to tap yet
        AssertJUnit.assertFalse(oneLandAndDrums("Forest", 2, "Hill Giant", "Hill Giant", "Shock", "Shock"));
    }

    @Test
    public void aManaCreatureIsNotCountedAsBothASourceAndADrumsCreature() {
        // the Elves can make mana or be tapped for the Drums, not both: one source, not three
        AssertJUnit.assertFalse(oneLandAndDrums("Forest", 2, "Llanowar Elves", "Shock", "Shock", "Divination"));
    }

    @Test
    public void aManaCreatureAndAnotherCreatureAreTwoSourcesWithADrum() {
        // the Elves make mana on their own, and the Drum taps the Memnite
        AssertJUnit.assertTrue(oneLandAndDrums("Forest", 1, "Llanowar Elves", "Memnite", "Shock", "Divination"));
    }

    @Test
    public void aTempleOfTheFalseGodIsNoLandForTheFirstTurns() {
        setUp(9, 8);
        // one Mountain and the Temple: the Temple makes no mana before the fifth land, so this is a one-lander
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                mix(SPELLS, "Mountain", "Temple of the False God", "Grizzly Bears")));
    }

    @Test
    public void keepsThreeLandsAndFourSpells() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                mix(SPELLS, "Island", "Island", "Mountain")));
    }

    @Test
    public void sendsBackAHandWithoutLands() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental", "Grizzly Bears", "Hill Giant", "Shock"));
    }

    @Test
    public void sendsBackAOneLanderOnThePlay() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Grizzly Bears", "Hill Giant", "Shock", "Air Elemental", "Grizzly Bears", "Shock"));
    }

    @Test
    public void sendsBackAHandOfOnlyLands() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Island", "Island", "Mountain", "Mountain", "Mountain", "Island"));
    }

    @Test
    public void sendsBackLandsAndCreatureAurasWithoutCreature() {
        setUp(9, 8);
        final CardCollection hand = hand("Island", "Island", "Mountain", "Holy Strength", "Holy Strength", "Rancor", "Holy Strength");
        final DeckPlan plan = DeckPlan.of(DeckPlan.Archetype.MIDRANGE);

        AssertJUnit.assertTrue("the hand is only lands and creature Auras",
                OpeningHand.hasOnlyLandsAndCreatureAuras(hand));
        AssertJUnit.assertFalse("Auras without a creature are not castable opening plays",
                OpeningHand.castsSomething(hand, ai));
        AssertJUnit.assertFalse("the smart mulligan must reject the hand",
                OpeningHand.keep(ai, hand, 0, true, plan, 4));
        AssertJUnit.assertTrue("the classic mulligan must reject the same hand",
                ComputerUtil.wantMulligan(ai, 0));
    }

    @Test
    public void keepsCreatureAurasWhenARealCreatureIsInHand() {
        setUp(9, 8);
        final CardCollection hand = hand("Forest", "Forest", "Plains", "Gladecover Scout", "Rancor", "Holy Strength", "Hyena Umbra");
        final DeckPlan plan = DeckPlan.of(DeckPlan.Archetype.AGGRO);

        AssertJUnit.assertFalse(OpeningHand.hasOnlyLandsAndCreatureAuras(hand));
        AssertJUnit.assertTrue(OpeningHand.castsSomething(hand, ai));
        AssertJUnit.assertTrue("the creature gives the Auras a real target and the hand has a castable play",
                OpeningHand.keep(ai, hand, 0, true, plan, 4));
        AssertJUnit.assertFalse(ComputerUtil.wantMulligan(ai, 0));
    }

    @Test
    public void sendsBackSixLandsAndOneSpell() {
        setUp(9, 8);
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), false,
                "Island", "Island", "Island", "Mountain", "Mountain", "Mountain", "Shock"));
    }

    @Test
    public void anAggroDeckKeepsAHandOfTwoLandsAndCheapCreatures() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.AGGRO), false,
                "Mountain", "Mountain", "Grizzly Bears", "Grizzly Bears", "Shock", "Grizzly Bears", "Shock"));
    }

    @Test
    public void aControlDeckWantsMoreLandsThanItsHandOfTwoGives() {
        setUp(9, 8);
        // two lands and expensive spells, on the play: aggro-style luck is not enough for a deck that needs four
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.CONTROL), true,
                "Island", "Island", "Air Elemental", "Air Elemental", "Hill Giant", "Hill Giant", "Air Elemental"));
    }

    @Test
    public void aSmallHandIsKeptWhenItCanPlayTheGame() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true,
                "Island", "Mountain", "Grizzly Bears", "Shock", "Hill Giant"));
    }

    @Test
    public void aMulliganToFourIsAlwaysKept() {
        setUp(9, 8);
        AssertJUnit.assertTrue(keeps(DeckPlan.of(DeckPlan.Archetype.MIDRANGE), true, "Shock", "Shock", "Shock", "Shock"));
    }

    @Test
    public void putsBackTheLandsWhenFlooded() {
        setUp(9, 8);
        final CardCollection hand = hand("Island", "Island", "Island", "Mountain", "Mountain", "Grizzly Bears", "Shock");
        final CardCollection bottom = OpeningHand.toBottom(ai, hand, 2, true, DeckPlan.of(DeckPlan.Archetype.MIDRANGE));
        AssertJUnit.assertEquals(2, bottom.size());
        for (final Card c : bottom) {
            AssertJUnit.assertTrue(c + " should stay: only extra lands go back", c.isLand());
        }
    }

    @Test
    public void putsBackTheSpellItCannotCastWhenShortOnLands() {
        setUp(9, 8);
        // two Islands and no red source in hand: the Mountains are a minority in the library
        final CardCollection hand = hand("Island", "Island", "Island", "Grizzly Bears", "Air Elemental", "Shock", "Hill Giant");
        final CardCollection bottom = OpeningHand.toBottom(ai, hand, 1, true, DeckPlan.of(DeckPlan.Archetype.MIDRANGE));
        AssertJUnit.assertEquals(1, bottom.size());
        AssertJUnit.assertFalse("the lands are what the hand needs: " + bottom, bottom.get(0).isLand());
    }

    @Test
    public void landDropOddsAreExact() {
        // one land in two draws from 40 cards holding 17 lands
        AssertJUnit.assertEquals(1.0 - (23.0 / 40.0) * (22.0 / 39.0), OpeningHand.atLeast(1, 2, 40, 17), 1e-9);
        AssertJUnit.assertEquals(1.0, OpeningHand.atLeast(0, 3, 40, 17), 0.0);
        AssertJUnit.assertEquals(0.0, OpeningHand.atLeast(3, 2, 40, 17), 0.0);
    }

    @Test
    public void keyCardsFollowThePlan() {
        setUp(9, 8);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        final Card counter = addCardToZone("Counterspell", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(bolt, DeckPlan.Archetype.BURN));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(bears, DeckPlan.Archetype.AGGRO));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(bolt, DeckPlan.Archetype.AGGRO));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(elves, DeckPlan.Archetype.RAMP));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(bears, DeckPlan.Archetype.RAMP));
        AssertJUnit.assertTrue(OpeningHand.isKeyCard(counter, DeckPlan.Archetype.CONTROL));
        AssertJUnit.assertFalse(OpeningHand.isKeyCard(elves, DeckPlan.Archetype.CONTROL));
    }

    private boolean castsSomething(final CardCollection cards) {
        ai.getGame().getAction().checkStateEffects(true);
        return OpeningHand.castsSomething(cards, ai);
    }

    @Test
    public void oneDualLandCannotPayBothColoredPips() {
        setUp(0, 0);
        AssertJUnit.assertFalse("one Underground Sea only pays one of {U} or {B}",
                castsSomething(hand("Underground Sea", "Baleful Strix")));
    }

    @Test
    public void dualLandAndOffColorLandStillCannotPayBothColoredPips() {
        setUp(0, 0);
        AssertJUnit.assertFalse("the Mountain cannot cover the second blue/black pip",
                castsSomething(hand("Underground Sea", "Mountain", "Baleful Strix")));
    }

    @Test
    public void dualLandCanPayEitherColorWhenAnotherSourceCoversTheOther() {
        setUp(0, 0);
        AssertJUnit.assertTrue("the Sea can cover black while the Island covers blue",
                castsSomething(hand("Underground Sea", "Island", "Baleful Strix")));
    }

    @Test
    public void colorlessLandDoesNotBorrowTheLibraryColors() {
        setUp(0, 1); // a Mountain is left in the library, but the hand has no red source
        AssertJUnit.assertFalse("Wastes makes only {C} and must not pretend to find the library's Mountain",
                castsSomething(hand("Wastes", "Plains", "Lightning Bolt")));
    }

    @Test
    public void handWithOnlyColorlessAndOffColorLandsIsNotKept() {
        setUp(0, 1);
        // Wastes and Plains cast none of the red spells, whatever the library still holds
        AssertJUnit.assertFalse(keeps(DeckPlan.of(DeckPlan.Archetype.BURN), true,
                "Wastes", "Plains", "Lightning Bolt", "Shock", "Lightning Bolt", "Shock", "Lightning Bolt"));
    }

    @Test
    public void fetchLandStillGetsTheLibraryColors() {
        setUp(0, 1);
        AssertJUnit.assertTrue("Evolving Wilds can fetch the Mountain that is left in the library",
                castsSomething(hand("Evolving Wilds", "Plains", "Lightning Bolt")));
    }

    @Test
    public void twoFetchLandsCannotBothUseTheOnlyLibraryIsland() {
        setUp(1, 0);

        AssertJUnit.assertFalse(
                "two Evolving Wilds cannot both fetch the only Island for Counterspell",
                castsSomething(hand(
                        "Evolving Wilds",
                        "Evolving Wilds",
                        "Counterspell")));
    }

    @Test
    public void fetchLandCannotFindAnIneligibleNonbasicLand() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);

        AssertJUnit.assertFalse(
                "Evolving Wilds cannot fetch a nonbasic Tropical Island",
                castsSomething(hand(
                        "Evolving Wilds",
                        "Plains",
                        "Coral Merfolk")));
    }

    @Test
    public void twoFetchLandsCannotReuseTheSameDualLand() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);

        AssertJUnit.assertFalse(
                "one Tropical Island cannot pay both green and blue through two fetch lands",
                castsSomething(hand(
                        "Flooded Strand",
                        "Windswept Heath",
                        "Coiling Oracle")));
    }

    @Test
    public void twoFetchLandsCanFindTwoDifferentLibraryLands() {
        setUp(0, 0);
        addCardToZone("Tropical Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);

        AssertJUnit.assertTrue(
                "Flooded Strand finds the Tropical Island for blue, Windswept Heath the Forest for green",
                castsSomething(hand(
                        "Flooded Strand",
                        "Windswept Heath",
                        "Coiling Oracle")));
    }

    @Test
    public void twoFetchLandsCanUseTwoLibraryIslands() {
        setUp(2, 0);

        AssertJUnit.assertTrue("each Evolving Wilds can fetch its own Island for Counterspell",
                castsSomething(hand("Evolving Wilds", "Evolving Wilds", "Counterspell")));
    }

    @Test
    public void sliverHivesColouredManaDoesNotMakeShockCastable() {
        setUp(0, 0);
        AssertJUnit.assertFalse(castsSomething(hand("Sliver Hive", "Shock")));
    }

    @Test
    public void sliverHivesColouredManaMakesASliverCastable() {
        setUp(0, 0);
        AssertJUnit.assertTrue(castsSomething(hand("Sliver Hive", "Muscle Sliver")));
    }

    @Test
    public void ancientZigguratCastsCreaturesOnly() {
        setUp(0, 0);
        AssertJUnit.assertFalse(castsSomething(hand("Ancient Ziggurat", "Shock")));
        AssertJUnit.assertTrue(castsSomething(hand("Ancient Ziggurat", "Grizzly Bears")));
    }

    @Test
    public void cavernOfSoulsCanChooseTheTypeOfACreatureInHand() {
        setUp(0, 0);
        AssertJUnit.assertTrue(castsSomething(hand("Cavern of Souls", "Grizzly Bears")));
        AssertJUnit.assertFalse(castsSomething(hand("Cavern of Souls", "Shock")));
    }

    @Test
    public void cascadeBluffsNeedsBlueOrRedToMakeRed() {
        setUp(0, 0);
        AssertJUnit.assertFalse(castsSomething(hand("Cascade Bluffs", "Forest", "Shock")));
        AssertJUnit.assertTrue(castsSomething(hand("Cascade Bluffs", "Island", "Shock")));
    }

    @Test
    public void aFetchedLandCanPayForCascadeBluffs() {
        setUp(1, 0); // the Island that Evolving Wilds finds pays the Bluffs, which then make red
        AssertJUnit.assertTrue(castsSomething(hand("Cascade Bluffs", "Evolving Wilds", "Shock")));
    }

    @Test
    public void aDualLandMakesOneColourOfTheTwoPips() {
        setUp(0, 0);
        final Card charm = addCardToZone("Simic Charm", ai, ZoneType.Library); // {G}{U}
        AssertJUnit.assertFalse("one Tropical Island pays {G} or {U}, not both",
                OpeningHand.makesColorsOf(hand("Tropical Island"), ai, charm));
        AssertJUnit.assertTrue("a Forest beside it pays the other pip",
                OpeningHand.makesColorsOf(hand("Tropical Island", "Forest"), ai, charm));
    }
}
