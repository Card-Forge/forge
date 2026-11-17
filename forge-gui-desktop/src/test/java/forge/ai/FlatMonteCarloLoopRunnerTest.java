package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ElfLoop;
import forge.ai.simulation.LoopRunner;
import forge.ai.simulation.PodPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The loops that LoopRunner goes round: Heliod, Sun-Crowned with Walking Ballista (lifelink first, then the pings) and
 * Mikaeus, the Unhallowed with Triskelion (the pings at the opponent, then at itself, and the undying brings the counters back). The steps alone, and the
 * whole turn against an opponent with twenty life.
 */
public class FlatMonteCarloLoopRunnerTest extends AITest {
    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        for (final String basic : new String[] { "Plains", "Swamp", "Island", "Mountain", "Forest" }) {
            addCards(basic, 5, ai);
        }
    }

    private Card withCounters(final String name, final int counters) {
        final Card c = addCardToZone(name, ai, ZoneType.Battlefield);
        c.addCounterInternal(CounterEnumType.P1P1, counters, ai, false, null, null);
        return c;
    }

    private List<SpellAbility> abilitiesOf(final Card... cards) {
        final List<SpellAbility> sas = new ArrayList<>();
        for (final Card c : cards) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                if (sa.isActivatedAbility()) {
                    sa.setActivatingPlayer(ai);
                    sas.add(sa);
                }
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        return sas;
    }

    /** What happened in the turn: the phase, the opponent's life and the stack whenever one of them changed. */
    private final StringBuilder trace = new StringBuilder();

    private void runTurn() {
        trace.setLength(0); // the test class is one instance for all its tests
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        String last = "";
        int steps = 0;
        for (; steps < 2000 && !game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                && game.getPhaseHandler().getPlayerTurn() == ai; steps++) {
            game.getPhaseHandler().mainLoopStep();
            final StringBuilder now = new StringBuilder();
            now.append(game.getPhaseHandler().getPhase()).append(" opp ").append(opp.getLife()).append(" stack ")
                    .append(game.getStack().size()).append(" [");
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if (!c.isLand()) {
                    now.append(c.getName()).append(' ').append(c.getCounters(CounterEnumType.P1P1)).append(c.hasKeyword("Undying") ? " undying" : "")
                            .append(c.hasKeyword("Lifelink") ? " lifelink" : "").append("; ");
                }
            }
            now.append(']');
            if (!now.toString().equals(last)) {
                last = now.toString();
                trace.append(steps).append(": ").append(last).append('\n');
            }
        }
        trace.append("after ").append(steps).append(" steps\n");
    }

    @Test(timeOut = 120000)
    public void heliodGivesTheBallistaLifelinkFirst() {
        setUp();
        final Card heliod = addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        final Card ballista = withCounters("Walking Ballista", 2);
        final SpellAbility step = LoopRunner.next(ai, abilitiesOf(heliod, ballista), s -> true);
        AssertJUnit.assertNotNull("Heliod and a Ballista with counters: the first step", step);
        AssertJUnit.assertEquals("Heliod gives the lifelink", heliod, step.getHostCard());
        AssertJUnit.assertEquals(ApiType.Pump, step.getApi());
        AssertJUnit.assertTrue("aimed at the Ballista", step.getTargets().contains(ballista));
    }

    @Test(timeOut = 120000)
    public void aLoneCounterIsGrownBeforeTheLoopStarts() {
        setUp();
        final Card heliod = addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        final Card ballista = withCounters("Walking Ballista", 1);
        final SpellAbility step = LoopRunner.next(ai, abilitiesOf(heliod, ballista), s -> true);
        AssertJUnit.assertNotNull("a Ballista with one counter: grow it, do not ping it dead", step);
        AssertJUnit.assertEquals(ballista, step.getHostCard());
        AssertJUnit.assertEquals(ApiType.PutCounter, step.getApi());
    }

    @Test(timeOut = 120000)
    public void theLoopWaitsForTheCounters() {
        setUp();
        final Card heliod = addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        final Card ballista = addCardToZone("Walking Ballista", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("a Ballista without counters is not a loop",
                LoopRunner.next(ai, abilitiesOf(heliod, ballista), s -> true));
    }

    @Test(timeOut = 120000)
    public void theTriskelionIsPingedAtTheOpponent() {
        setUp();
        final Card mikaeus = addCardToZone("Mikaeus, the Unhallowed", ai, ZoneType.Battlefield);
        final Card triskelion = withCounters("Triskelion", 3);
        game.getAction().checkStateEffects(true);
        final SpellAbility step = LoopRunner.next(ai, abilitiesOf(mikaeus, triskelion), s -> true);
        AssertJUnit.assertNotNull("Mikaeus and a Triskelion with counters: the ping (undying: "
                + triskelion.hasKeyword("Undying") + ")", step);
        AssertJUnit.assertEquals(triskelion, step.getHostCard());
        AssertJUnit.assertTrue("aimed at the opponent", step.getTargets().contains(opp));
    }

    @Test(timeOut = 120000)
    public void aLoopOfAnotherTurnIsNotRun() {
        setUp();
        final Card heliod = addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        final Card ballista = withCounters("Walking Ballista", 2);
        final List<SpellAbility> sas = abilitiesOf(heliod, ballista);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNull("only on the AI's own turn", LoopRunner.next(ai, sas, s -> true));
    }

    @Test(timeOut = 300000)
    public void heliodAndTheBallistaKillTheOpponentInATurn() {
        setUp();
        addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        withCounters("Walking Ballista", 2);
        runTurn();
        AssertJUnit.assertTrue("the opponent should be dead (life " + opp.getLife() + ")\n" + trace + gameStateToString(game),
                game.isGameOver() || opp.getLife() <= 0);
    }

    @Test(timeOut = 300000)
    public void mikaeusAndTheTriskelionKillTheOpponentInATurn() {
        setUp();
        addCardToZone("Mikaeus, the Unhallowed", ai, ZoneType.Battlefield);
        withCounters("Triskelion", 3);
        game.getAction().checkStateEffects(true);
        runTurn();
        AssertJUnit.assertTrue("the opponent should be dead (life " + opp.getLife() + ")\n" + trace + gameStateToString(game),
                game.isGameOver() || opp.getLife() <= 0);
    }

    @Test(timeOut = 120000)
    public void theBloomTenderIsTappedForManaFirst() {
        setUp();
        final Card tender = addCardToZone("Bloom Tender", ai, ZoneType.Battlefield);
        final Card freed = addCardToZone("Freed from the Real", ai, ZoneType.Battlefield);
        tender.setSickness(false); // it has been there since the start of the turn: it can tap for mana
        freed.attachToEntity(tender, null);
        final Card ballista = addCardToZone("Walking Ballista", ai, ZoneType.Hand);
        final List<SpellAbility> sas = abilitiesOf(tender, freed);
        ballista.getFirstSpellAbility().setActivatingPlayer(ai);
        sas.add(ballista.getFirstSpellAbility());
        final SpellAbility step = LoopRunner.next(ai, sas, s -> true);
        AssertJUnit.assertNotNull("Freed on the Tender, a Ballista in hand: the first step", step);
        AssertJUnit.assertEquals("the Tender is tapped for mana", tender, step.getHostCard());
        AssertJUnit.assertTrue(step.isManaAbility());
    }

    @Test(timeOut = 120000)
    public void withoutAnOutletTheManaLoopIsNotRun() {
        setUp();
        final Card tender = addCardToZone("Bloom Tender", ai, ZoneType.Battlefield);
        final Card freed = addCardToZone("Freed from the Real", ai, ZoneType.Battlefield);
        tender.setSickness(false); // it has been there since the start of the turn: it can tap for mana
        freed.attachToEntity(tender, null);
        AssertJUnit.assertNull("no Ballista to spend the mana on", LoopRunner.next(ai, abilitiesOf(tender, freed), s -> true));
    }

    @Test(timeOut = 300000)
    public void freedAndTheBloomTenderKillTheOpponentWithABallista() {
        setUp();
        final Card tender = addCardToZone("Bloom Tender", ai, ZoneType.Battlefield);
        final Card freed = addCardToZone("Freed from the Real", ai, ZoneType.Battlefield);
        tender.setSickness(false); // it has been there since the start of the turn: it can tap for mana
        freed.attachToEntity(tender, null);
        addCardToZone("Walking Ballista", ai, ZoneType.Hand);
        runTurn();
        AssertJUnit.assertTrue("the opponent should be dead (life " + opp.getLife() + ")\n" + trace + gameStateToString(game),
                game.isGameOver() || opp.getLife() <= 0);
    }

    @Test(timeOut = 300000)
    public void aBallistaIsNotCastForAnXOfNothing() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Plains", 1, ai); // {X}{X}: one land pays for an X of nothing, two for an X of one
        addCardToZone("Heliod, Sun-Crowned", ai, ZoneType.Battlefield);
        addCardToZone("Walking Ballista", ai, ZoneType.Hand);
        runTurn();
        boolean inHand = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            inHand |= c.getName().equals("Walking Ballista");
        }
        AssertJUnit.assertTrue("one land cannot pay for an X of one: the Ballista stays in hand\n" + trace + gameStateToString(game),
                inHand);
    }

    /** The spells of the hand and the mana abilities of the permanents: what the AI chooses from. */
    private List<SpellAbility> playsOf() {
        final List<SpellAbility> sas = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            sas.addAll(c.getSpellAbilities());
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            sas.addAll(c.getSpellAbilities());
        }
        for (final SpellAbility sa : sas) {
            sa.setActivatingPlayer(ai);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        return sas;
    }

    private void islandsAndTheTidePieces(final int islands) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Island", islands, ai);
        addCardToZone("High Tide", ai, ZoneType.Hand);
        addCardToZone("Palinchron", ai, ZoneType.Hand);
        addCardToZone("Stroke of Genius", ai, ZoneType.Hand);
    }

    private SpellAbility spellOf(final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                return c.getFirstSpellAbility();
            }
        }
        throw new IllegalStateException(name + " is not in the hand");
    }

    @Test(timeOut = 120000)
    public void highTideIsCastOnceTheIslandsAreThere() {
        islandsAndTheTidePieces(8);
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("eight Islands, High Tide, Palinchron and Stroke: the first step", step);
        AssertJUnit.assertEquals("High Tide first", "High Tide", step.getHostCard().getName());
        AssertJUnit.assertFalse("High Tide is not held when the Islands are there", LoopRunner.held(ai, spellOf("High Tide")));
    }

    @Test(timeOut = 120000)
    public void highTideAndStrokeAreHeldWithoutTheIslands() {
        islandsAndTheTidePieces(3);
        AssertJUnit.assertNull("three Islands are not enough to go round", LoopRunner.next(ai, playsOf(), s -> true));
        AssertJUnit.assertTrue("High Tide is kept for the loop", LoopRunner.held(ai, spellOf("High Tide")));
        AssertJUnit.assertTrue("Stroke of Genius too", LoopRunner.held(ai, spellOf("Stroke of Genius")));
    }

    @Test(timeOut = 120000)
    public void highTideAloneIsNotHeld() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        addCards("Island", 3, ai);
        addCardToZone("High Tide", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("without Palinchron and Stroke it is a card like another", LoopRunner.held(ai, spellOf("High Tide")));
    }

    @Test(timeOut = 600000)
    public void highTideAndPalinchronDrawTheOpponentsLibrary() {
        islandsAndTheTidePieces(10);
        runTurn();
        AssertJUnit.assertEquals("the library of the opponent should be drawn (Stroke of Genius in the graveyard: "
                        + ai.getCardsIn(ZoneType.Graveyard).size() + " cards there)\n" + trace + gameStateToString(game),
                0, opp.getCardsIn(ZoneType.Library).size());
    }

    @Test(timeOut = 120000)
    public void highTideWithTurnaboutIsCastWhenTheHandMakesEnoughMana() {
        islandsAndTheTurnaboutDeck(12);
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("twelve Islands, two Turnabouts and a Stroke are enough for a library of twenty", step);
        AssertJUnit.assertEquals("High Tide first", "High Tide", step.getHostCard().getName());
        AssertJUnit.assertFalse(LoopRunner.held(ai, spellOf("High Tide")) && LoopRunner.held(ai, spellOf("Turnabout")));
    }

    @Test(timeOut = 120000)
    public void highTideStrokeAndTurnaboutAreHeldWhenTheHandIsTooSmall() {
        islandsAndTheTurnaboutDeck(3);
        AssertJUnit.assertNull(LoopRunner.next(ai, playsOf(), s -> true));
        AssertJUnit.assertTrue("High Tide is kept", LoopRunner.held(ai, spellOf("High Tide")));
        AssertJUnit.assertTrue("Stroke is kept", LoopRunner.held(ai, spellOf("Stroke of Genius")));
        AssertJUnit.assertTrue("Turnabout is kept", LoopRunner.held(ai, spellOf("Turnabout")));
    }

    @Test(timeOut = 120000)
    public void theAiUntapsItsOwnLandsAndOnlyTapsAnOpponents() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        final Card mom = addCard("Mind Over Matter", ai);
        final SpellAbility untap = mom.getFirstSpellAbility();
        AssertJUnit.assertFalse("one's own land is untapped", ai.getController().chooseBinary(untap, "", 
                forge.game.player.PlayerController.BinaryChoiceType.TapOrUntap, Boolean.FALSE));
        AssertJUnit.assertTrue("an opponent's land is tapped", ai.getController().chooseBinary(untap, "",
                forge.game.player.PlayerController.BinaryChoiceType.TapOrUntap, Boolean.TRUE));
    }

    @Test(timeOut = 120000)
    public void theLoopKeepsItsKeyCardsWhenThrowingCardsAway() {
        islandsAndTheTurnaboutDeck(5);
        final Card bears = addCard("Grizzly Bears", ai);
        ai.getZone(ZoneType.Battlefield).remove(bears);
        ai.getZone(ZoneType.Hand).add(bears);
        addCardToZone("Arcane Denial", ai, ZoneType.Hand);
        final forge.game.card.CardCollection safe = LoopRunner.withoutTideKeys(ai,
                new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Hand)));
        AssertJUnit.assertEquals("only the Grizzly Bears may go: " + safe, 1, safe.size());
        AssertJUnit.assertEquals("Grizzly Bears", safe.getFirst().getName());
    }

    @Test(timeOut = 120000)
    public void aCounterspellInTheHandKeepsItsPriceBackFromTheStroke() {
        islandsAndTheTurnaboutDeck(5);
        AssertJUnit.assertEquals("no counterspell: nothing kept back", 0, LoopRunner.counterReserve(ai));
        addCardToZone("Arcane Denial", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("Arcane Denial costs two", 2, LoopRunner.counterReserve(ai));
        addCardToZone("Force of Will", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("Force of Will is free", 0, LoopRunner.counterReserve(ai));
    }

    private void islandsAndTheTurnaboutDeck(final int islands) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Island", islands, ai);
        addCardToZone("High Tide", ai, ZoneType.Hand);
        addCardToZone("Turnabout", ai, ZoneType.Hand);
        addCardToZone("Turnabout", ai, ZoneType.Hand);
        addCardToZone("Stroke of Genius", ai, ZoneType.Hand);
    }

    private void tideDeckWith(final int islands, final String... hand) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Plains", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Island", islands, ai);
        for (final String name : hand) {
            addCardToZone(name, ai, ZoneType.Hand);
        }
    }

    @Test(timeOut = 120000)
    public void highTideStartsWithBlueSunsZenithAndCandelabra() {
        tideDeckWith(10, "High Tide", "Blue Sun's Zenith", "Candelabra of Tawnos");
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("ten Islands, High Tide, Candelabra and a Zenith: start", step);
        AssertJUnit.assertEquals("High Tide first", "High Tide", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void moreHighTidesRaiseTheProjection() {
        tideDeckWith(6, "High Tide", "Blue Sun's Zenith", "Turnabout");
        final int one = LoopRunner.projectedTideMana(ai);
        addCardToZone("High Tide", ai, ZoneType.Hand);
        addCardToZone("High Tide", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("three High Tides make more than one: " + one + " -> " + LoopRunner.projectedTideMana(ai),
                LoopRunner.projectedTideMana(ai) > one);
    }

    @Test(timeOut = 120000)
    public void brainFreezeIsAFinisherAndIsKept() {
        tideDeckWith(3, "High Tide", "Brain Freeze", "Turnabout");
        AssertJUnit.assertTrue("no storm to mill the library with: Brain Freeze is kept", LoopRunner.held(ai, spellOf("Brain Freeze")));
    }

    @Test(timeOut = 120000)
    public void theZenithIsNotThrownAwayForMindOverMatter() {
        tideDeckWith(5, "High Tide", "Blue Sun's Zenith", "Mind Over Matter", "Island", "Force of Will");
        final forge.game.card.CardCollection safe = LoopRunner.withoutTideKeys(ai,
                new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Hand)));
        AssertJUnit.assertEquals("only the Island may go: " + safe, 1, safe.size());
        AssertJUnit.assertEquals("Island", safe.getFirst().getName());
    }

    @Test(timeOut = 120000)
    public void aTutorFetchesTheFinisherFirstThenTheMissingPieces() {
        tideDeckWith(8, "High Tide", "Turnabout");
        addCardToZone("Brain Freeze", ai, ZoneType.Library);
        addCardToZone("Mind Over Matter", ai, ZoneType.Library);
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        final forge.game.card.CardCollection pool = new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Library));
        final forge.game.card.CardCollection one = forge.ai.simulation.TidePlan.picks(ai, pool, 1);
        AssertJUnit.assertNotNull("a finisher is missing", one);
        AssertJUnit.assertEquals("the draw spell comes before Brain Freeze", "Stroke of Genius", one.getFirst().getName());
    }

    @Test(timeOut = 120000)
    public void aTutorFetchesTheUntapWhenTheFinisherIsInHand() {
        tideDeckWith(8, "High Tide", "Stroke of Genius");
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Candelabra of Tawnos", ai, ZoneType.Library);
        addCardToZone("Turnabout", ai, ZoneType.Library);
        final forge.game.card.CardCollection one = forge.ai.simulation.TidePlan.picks(ai,
                new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Library)), 1);
        AssertJUnit.assertNotNull(one);
        AssertJUnit.assertEquals("Turnabout", one.getFirst().getName());
    }

    @Test(timeOut = 120000)
    public void aTutorHasNoOpinionWhenTheLoopIsComplete() {
        tideDeckWith(8, "High Tide", "Stroke of Genius", "Turnabout");
        addCardToZone("Turnabout", ai, ZoneType.Library);
        AssertJUnit.assertNull(forge.ai.simulation.TidePlan.picks(ai,
                new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Library)), 1));
    }

    @Test(timeOut = 300000)
    public void theTideTurnIsPlayedOutInACopyBeforeItIsStarted() {
        tideDeckWith(12, "High Tide", "Stroke of Genius", "Turnabout", "Turnabout");
        playsOf(); // the main phase of the AI
        AssertJUnit.assertTrue("twelve Islands, two Turnabouts and a Stroke win against a library of twenty: "
                + forge.ai.simulation.TideSolver.lastNote, forge.ai.simulation.TideSolver.wins(ai));
    }

    @Test(timeOut = 120000)
    public void twoHighTidesAndTurnaboutBeforeTimeSpiralWhenThereIsNoFinisher() {
        tideDeckWith(7, "High Tide", "High Tide", "Turnabout", "Time Spiral", "Time Spiral");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library); // a finisher left to draw
        final SpellAbility first = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("seven Islands, two High Tides, Turnabout and Time Spiral: start", first);
        AssertJUnit.assertEquals("High Tide first", "High Tide", first.getHostCard().getName());
        AssertJUnit.assertTrue("Time Spiral is kept for after them", LoopRunner.held(ai, spellOf("Turnabout")) == false);
    }

    @Test(timeOut = 120000)
    public void splinterTwinIsNotPutOnSpellskite() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        addCardToZone("Spellskite", ai, ZoneType.Battlefield);
        final Card twin = addCardToZone("Splinter Twin", ai, ZoneType.Hand);
        addCards("Mountain", 4, ai);
        playsOf();
        final AiPlayDecision decision = ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(twin.getFirstSpellAbility());
        AssertJUnit.assertNotSame("nothing worth copying: Splinter Twin stays in hand", AiPlayDecision.WillPlay, decision);
    }

    @Test(timeOut = 120000)
    public void aSecondMindOverMatterOrCandelabraIsRedundant() {
        tideDeckWith(8, "High Tide", "Mind Over Matter", "Candelabra of Tawnos");
        addCardToZone("Mind Over Matter", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("duplicate-engine", LoopRunner.redundantEngine(ai, spellOf("Mind Over Matter")));
        AssertJUnit.assertNull("Candelabra is not in play yet", LoopRunner.redundantEngine(ai, spellOf("Candelabra of Tawnos")));
        addCardToZone("Candelabra of Tawnos", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("duplicate-engine", LoopRunner.redundantEngine(ai, spellOf("Candelabra of Tawnos")));
    }

    @Test(timeOut = 120000)
    public void theSetupPutsCandelabraInPlayWhenTheTurnCannotWinYet() {
        tideDeckWith(3, "High Tide", "Candelabra of Tawnos", "Stroke of Genius", "Turnabout");
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("three Islands cannot win, so the setup casts the engine", step);
        AssertJUnit.assertEquals("Candelabra of Tawnos", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theFinishersAreKeptWhenHighTideIsInEffectAndTheLoopCannotWin() {
        tideDeckWith(4, "High Tide", "Blue Sun's Zenith", "Brain Freeze", "Turnabout");
        final SpellAbility zenith = spellOf("Blue Sun's Zenith");
        final SpellAbility freeze = spellOf("Brain Freeze");
        AssertJUnit.assertFalse("High Tide is not in effect yet", LoopRunner.keptForTheLoop(ai, zenith));
        final SpellAbility tide = spellOf("High Tide");
        tide.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(tide);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Blue Sun's Zenith is not drawn at oneself", LoopRunner.keptForTheLoop(ai, zenith));
        AssertJUnit.assertTrue("Brain Freeze is not spent short of the library", LoopRunner.keptForTheLoop(ai, freeze));
        AssertJUnit.assertFalse("Turnabout is not a finisher", LoopRunner.keptForTheLoop(ai, spellOf("Turnabout")));
    }

    private void elfDeckWith(final int forests, final String... hand) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 20; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Forest", forests, ai);
        for (final String name : hand) {
            addCardToZone(name, ai, ZoneType.Hand);
        }
    }

    private void glimpseInEffect() {
        final SpellAbility glimpse = spellOf("Glimpse of Nature");
        glimpse.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(glimpse);
        game.getAction().checkStateEffects(true);
    }

    @Test(timeOut = 120000)
    public void theElfChainCastsTheCheapCreatureFirstAndKeepsCraterhoof() {
        elfDeckWith(5, "Glimpse of Nature", "Craterhoof Behemoth", "Elvish Visionary", "Heritage Druid");
        glimpseInEffect();
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("Glimpse is in effect: a creature is next", step);
        AssertJUnit.assertEquals("Heritage Druid", step.getHostCard().getName()); // the cheapest: one mana, a card
    }

    @Test(timeOut = 120000)
    public void noCreatureIsHeldWhenGlimpseIsNotInTheHand() {
        elfDeckWith(5, "Craterhoof Behemoth", "Elvish Visionary", "Heritage Druid");
        addCardToZone("Glimpse of Nature", ai, ZoneType.Library);
        AssertJUnit.assertFalse("nothing to wait for", ElfLoop.held(ai, spellOf("Elvish Visionary")));
    }

    @Test(timeOut = 120000)
    public void anElfTutorFetchesCraterhoofOnceGlimpseIsHere() {
        elfDeckWith(5, "Glimpse of Nature", "Elvish Visionary");
        final Card hoof = addCardToZone("Craterhoof Behemoth", ai, ZoneType.Library);
        final Card visionary = addCardToZone("Elvish Visionary", ai, ZoneType.Library);
        final forge.game.card.CardCollection pool = new forge.game.card.CardCollection();
        pool.add(visionary);
        pool.add(hoof);
        final forge.game.card.CardCollection picks = ElfLoop.picks(ai, pool, 1);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("Craterhoof Behemoth", picks.get(0).getName());
    }

    @Test(timeOut = 120000)
    public void anElfTutorFetchesGlimpseWhenItIsMissing() {
        elfDeckWith(5, "Elvish Visionary");
        final Card glimpse = addCardToZone("Glimpse of Nature", ai, ZoneType.Library);
        final Card hoof = addCardToZone("Craterhoof Behemoth", ai, ZoneType.Library);
        final forge.game.card.CardCollection pool = new forge.game.card.CardCollection();
        pool.add(hoof);
        pool.add(glimpse);
        final forge.game.card.CardCollection picks = ElfLoop.picks(ai, pool, 1);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("Glimpse of Nature", picks.get(0).getName());
    }

    @Test(timeOut = 120000)
    public void birthingPodFetchesTheCreatureThatPaysBackAndLeavesARung() {
        elfDeckWith(5, "Birthing Pod");
        addCardToZone("Birthing Pod", ai, ZoneType.Battlefield);
        final Card finks = addCardToZone("Kitchen Finks", ai, ZoneType.Library); // cmc 3, an enters trigger
        final Card hill = addCardToZone("Hill Giant", ai, ZoneType.Library);     // cmc 3, nothing
        addCardToZone("Restoration Angel", ai, ZoneType.Library);                 // cmc 4: the next rung
        final forge.game.card.CardCollection pool = new forge.game.card.CardCollection();
        pool.add(hill);
        pool.add(finks);
        final forge.game.card.CardCollection picks = PodPlan.picks(ai, pool, 1);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("Kitchen Finks", picks.get(0).getName());
    }

    @Test(timeOut = 120000)
    public void noPodOpinionWithoutBirthingPod() {
        elfDeckWith(5, "Elvish Visionary");
        final Card finks = addCardToZone("Kitchen Finks", ai, ZoneType.Library);
        final forge.game.card.CardCollection pool = new forge.game.card.CardCollection();
        pool.add(finks);
        AssertJUnit.assertNull(PodPlan.picks(ai, pool, 1));
    }

    private void affinityDeckWith(final String... hand) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < 18; i++) {
            addCardToZone("Ornithopter", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Plains", 4, ai);
        for (final String name : hand) {
            addCardToZone(name, ai, ZoneType.Hand);
        }
    }

    @Test(timeOut = 120000)
    public void affinityCastsTheFreeArtifactBeforeTheOneThatGetsCheaper() {
        affinityDeckWith("Frogmite", "Memnite", "Cranial Plating");
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Memnite", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void affinityKeepsCranialPlatingForLast() {
        affinityDeckWith("Cranial Plating", "Frogmite");
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Frogmite", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void aDeckWithoutEnoughArtifactsIsNotAnAffinityDeck() {
        elfDeckWith(4, "Frogmite", "Memnite");
        AssertJUnit.assertFalse(forge.ai.simulation.AffinityPlan.isAffinityDeck(ai));
    }

    @Test(timeOut = 120000)
    public void glimpseGoesFirstEvenWithoutCraterhoof() {
        elfDeckWith(5, "Glimpse of Nature", "Elvish Visionary", "Llanowar Elves");
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("two creatures in the hand: Glimpse first", step);
        AssertJUnit.assertEquals("Glimpse of Nature", step.getHostCard().getName());
        AssertJUnit.assertTrue("and the creatures wait", ElfLoop.held(ai, spellOf("Llanowar Elves")));
    }

    @Test(timeOut = 120000)
    public void glimpseDoesNotWaitForOneCreature() {
        elfDeckWith(5, "Glimpse of Nature", "Llanowar Elves");
        AssertJUnit.assertFalse("one creature is no reason to hold it", ElfLoop.held(ai, spellOf("Llanowar Elves")));
    }
}
