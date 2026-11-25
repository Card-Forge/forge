package forge.ai;

import java.util.ArrayList;
import java.util.List;

import forge.ai.simulation.DeckPlan;
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
    public void aGlimpseDeckWithoutCraterhoofIsAComboDeckNotAnAggroDeck() {
        // the catalog line is Glimpse + Craterhoof; a deck of elves without the second card was read as aggro
        elfDeckWith(5, "Glimpse of Nature", "Elvish Visionary", "Heritage Druid");
        addCards("Llanowar Elves", 12, ai);
        AssertJUnit.assertEquals(DeckPlan.Archetype.COMBO, DeckPlan.forSetting("auto", ai).archetype);
    }

    @Test(timeOut = 120000)
    public void grapeshotIsCastOnceTheStormCountIsLethal() {
        elfDeckWith(3, "Glimpse of Nature", "Grapeshot", "Elvish Visionary");
        addCards("Mountain", 2, ai);
        opp.setLife(1, null);
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull("one life left: Grapeshot kills by itself", step);
        AssertJUnit.assertEquals("Grapeshot", step.getHostCard().getName());
        AssertJUnit.assertTrue(step.getTargets().contains(opp));
    }

    @Test(timeOut = 120000)
    public void naturalOrderIsLethalWhenCraterhoofWouldKill() {
        elfDeckWith(3, "Natural Order");
        addCardToZone("Craterhoof Behemoth", ai, ZoneType.Library);
        addCards("Llanowar Elves", 3, ai);
        opp.setLife(12, null);
        AssertJUnit.assertTrue("Craterhoof with three elves and a haste attack kills from twelve",
                ElfLoop.orderLethal(ai, opp));
        opp.setLife(60, null);
        AssertJUnit.assertFalse("but not from sixty", ElfLoop.orderLethal(ai, opp));
    }

    private static forge.game.card.CardCollection cards(final Card... cs) {
        final forge.game.card.CardCollection out = new forge.game.card.CardCollection();
        for (final Card c : cs) {
            out.add(c);
        }
        return out;
    }

    /** The Aluren deck with Aluren and Soul Warden in play, {@code lands} Islands, and the hand. */
    private void alurenDeckWith(final int lands, final String... hand) {
        alurenDeckWithLibrary(lands, 20, hand);
    }

    private void alurenDeckWithLibrary(final int lands, final int librarySize, final String... hand) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        for (int i = 0; i < librarySize; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        addCards("Island", lands, ai);
        for (final String name : hand) {
            addCardToZone(name, ai, ZoneType.Hand);
        }
        addCardToZone("Aluren", ai, ZoneType.Battlefield);
        addCardToZone("Soul Warden", ai, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true); // the static ability that makes the creatures free
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
    }

    /** Every way to play the cards: the free casts that Aluren grants, the abilities in play, the mana of the lands. */
    private List<SpellAbility> alurenPlays() {
        final List<SpellAbility> plays = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            plays.addAll(c.getAllPossibleAbilities(ai, true));
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            plays.addAll(c.getAllPossibleAbilities(ai, true));
        }
        for (final SpellAbility sa : plays) {
            sa.setActivatingPlayer(ai);
        }
        return plays;
    }

    @Test(timeOut = 120000)
    public void theManaLoopTapsTheLandsIntoThePoolFirst() {
        alurenDeckWith(4, "Stroke of Genius", "Cloud of Faeries", "Cavern Harpy");
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.MANA,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull("Stroke, Cloud, Harpy and Aluren: the mana loop starts", step);
        AssertJUnit.assertTrue("the untapped lands are tapped for the pool: Cloud untaps tapped ones", step.isManaAbility());
    }

    @Test(timeOut = 120000)
    public void theCloudIsCastOnceTheLandsAreTapped() {
        alurenDeckWith(4, "Stroke of Genius", "Cloud of Faeries", "Cavern Harpy");
        for (final Card c : ai.getLandsInPlay()) {
            c.setTapped(true);
        }
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull("every land is tapped: Cloud of Faeries untaps two", step);
        AssertJUnit.assertEquals("Cloud of Faeries", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theHarpyIsCastWhenTheCloudIsOutAndItBouncesTheCloud() {
        alurenDeckWith(4, "Stroke of Genius", "Cavern Harpy");
        final Card cloud = addCardToZone("Cloud of Faeries", ai, ZoneType.Battlefield);
        for (final Card c : ai.getLandsInPlay()) {
            c.setTapped(true);
        }
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull("the Cloud is in play and the Harpy waits in the hand", step);
        AssertJUnit.assertEquals("Cavern Harpy", step.getHostCard().getName());
        final Card harpy = addCardToZone("Cavern Harpy", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("its trigger sends the Cloud back", cloud,
                forge.ai.simulation.AlurenPlan.harpyBounce(ai, cards(cloud, harpy)));
    }

    @Test(timeOut = 120000)
    public void withoutStrokeTheFamiliarDigsForIt() {
        alurenDeckWith(4, "Raven Familiar", "Cloud of Faeries", "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.CARDS,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Raven Familiar", step.getHostCard().getName());
        final Card familiar = addCardToZone("Raven Familiar", ai, ZoneType.Battlefield);
        final Card harpy = addCardToZone("Cavern Harpy", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("the Familiar is the one to cast again", familiar,
                forge.ai.simulation.AlurenPlan.harpyBounce(ai, cards(familiar, harpy)));
    }

    @Test(timeOut = 120000)
    public void theTutorsFetchTheMissingPieceOfTheKill() {
        alurenDeckWith(4, "Cavern Harpy");
        final Card stroke = addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        final Card cloud = addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        final forge.game.card.CardCollection pool = cards(cloud, stroke);
        final forge.game.card.CardCollection picks = forge.ai.simulation.AlurenPlan.picks(ai, pool, 1);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("Stroke of Genius first", stroke, picks.getFirst());
    }

    @Test(timeOut = 120000)
    public void livingWishIsCastFirstForTheMissingUntapper() {
        alurenDeckWith(4, "Living Wish", "Stroke of Genius", "Cavern Harpy");
        addCardToZone("Cloud of Faeries", ai, ZoneType.Sideboard);
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull("Stroke is here, the Cloud is not: Living Wish first", step);
        AssertJUnit.assertEquals("Living Wish", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theWishAndTheTutorsTakeTheMissingPiece() {
        alurenDeckWith(4, "Stroke of Genius", "Cavern Harpy");
        final Card familiar = addCardToZone("Raven Familiar", ai, ZoneType.Sideboard);
        final Card cloud = addCardToZone("Cloud of Faeries", ai, ZoneType.Sideboard);
        AssertJUnit.assertEquals("Cloud of Faeries", forge.ai.simulation.AlurenPlan.wanted(ai, cards(familiar, cloud)));
        final forge.game.card.CardCollection picks = forge.ai.simulation.AlurenPlan.picks(ai, cards(familiar, cloud), 1);
        AssertJUnit.assertEquals(cloud, picks.getFirst());
    }

    @Test(timeOut = 120000)
    public void acererakIsItsOwnLoopWithoutTheHarpy() {
        alurenDeckWith(3, "Acererak the Archlich");
        AssertJUnit.assertTrue(forge.ai.simulation.AlurenPlan.isAlurenDeck(ai));
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.ACERERAK,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull("Aluren is in play: Acererak is free and comes back every time", step);
        AssertJUnit.assertEquals("Acererak the Archlich", step.getHostCard().getName());
        AssertJUnit.assertEquals("Lost Mine of Phandelver", forge.ai.simulation.AlurenPlan.dungeon(ai));
    }

    @Test(timeOut = 120000)
    public void theSavageAndTheHarpyDrawEvenWithEightCardsInHand() {
        alurenDeckWith(4, "Cavern Harpy", "Forest", "Forest", "Forest", "Forest", "Forest", "Forest", "Forest");
        for (int i = 0; i < 15; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.CARDS, forge.ai.simulation.AlurenPlan.stage(ai, opp));
        AssertJUnit.assertTrue("over seven cards is no reason to stop the draws", forge.ai.simulation.AlurenPlan.drawLoop(ai));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Cavern Harpy", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theHarpyAloneWaitsForTheFamiliar() {
        alurenDeckWith(4, "Cavern Harpy", "Raven Familiar");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        final SpellAbility harpy = LoopRunner.castFromHand(ai, alurenPlays(), "Cavern Harpy");
        AssertJUnit.assertNotNull(harpy);
        AssertJUnit.assertTrue("the Harpy returns only itself: the Familiar goes first", LoopRunner.held(ai, harpy));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Raven Familiar", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void soulWardenAndHarpyDoNotLoopWithoutAWinCondition() {
        alurenDeckWith(4, "Cavern Harpy");
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.NONE,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility harpy = LoopRunner.castFromHand(ai, alurenPlays(), "Cavern Harpy");
        AssertJUnit.assertNotNull(harpy);
        AssertJUnit.assertTrue("life gain alone is not a win condition",
                LoopRunner.held(ai, harpy));
    }

    @Test(timeOut = 120000)
    public void twoSavagesStopBeforeTheLibraryReserveIsConsumed() {
        // Three Forests plus Stroke and Cloud: five cards. Two Savages would draw two.
        alurenDeckWithLibrary(4, 3, "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals(5, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.NONE,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility harpy = LoopRunner.castFromHand(ai, alurenPlays(), "Cavern Harpy");
        AssertJUnit.assertNotNull(harpy);
        AssertJUnit.assertTrue("do not cast Harpy when two Savage triggers exceed the reserve",
                LoopRunner.held(ai, harpy));
    }

    @Test(timeOut = 120000)
    public void twoSavagesCanDrawWhenTheLibraryHasOneSafeRound() {
        // Four Forests plus Stroke and Cloud: six cards. The round leaves four.
        alurenDeckWithLibrary(4, 4, "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals(6, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.CARDS,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Cavern Harpy", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void maggotCarrierIsPreferredOverTheStrokeBackup() {
        alurenDeckWith(4, "Cavern Harpy", "Maggot Carrier",
                "Stroke of Genius", "Cloud of Faeries");
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.DRAIN,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Maggot Carrier", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void soulWardenLifeLoopStopsAtTwenty() {
        alurenDeckWith(4, "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        ai.setLife(19, null);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.LIFE,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Cavern Harpy", step.getHostCard().getName());
        final Card harpy = addCardToZone("Cavern Harpy", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals(harpy, forge.ai.simulation.AlurenPlan.harpyBounce(ai, cards(harpy)));
        ai.setLife(20, null);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.NONE,
                forge.ai.simulation.AlurenPlan.stage(ai, opp));
    }

    @Test(timeOut = 120000)
    public void cabalTherapyUsesARevealedCounterspellBeforeAluren() {
        elfDeckWith(4, "Aluren", "Cavern Harpy", "Maggot Carrier", "Cabal Therapy");
        addCard("Swamp", ai);
        final Card revealed = addCardToZone("Counterspell", opp, ZoneType.Hand);
        AiCardMemory.rememberCard(ai, revealed, AiCardMemory.MemorySet.REVEALED_CARDS);
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Cabal Therapy", step.getHostCard().getName());
        AssertJUnit.assertEquals("Counterspell",
                NamedDiscard.chooseName(ai, step, "Card.nonLand"));
    }

    @Test(timeOut = 120000)
    public void withTheStrokeInTheGraveyardTheLoopStopsDrawingAndDrains() {
        // the one Stroke of Genius is gone: the library holds nothing that is the kill, the Carrier is the way left
        alurenDeckWith(4, "Raven Familiar", "Cavern Harpy", "Maggot Carrier");
        addCardToZone("Stroke of Genius", ai, ZoneType.Graveyard);
        for (int i = 0; i < 15; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        AssertJUnit.assertFalse(forge.ai.simulation.AlurenPlan.manaKillAlive(ai));
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.DRAIN, forge.ai.simulation.AlurenPlan.stage(ai, opp));
    }

    @Test(timeOut = 120000)
    public void withTheStrokeInTheLibraryTheLoopKeepsDrawing() {
        alurenDeckWith(4, "Raven Familiar", "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Library);
        for (int i = 0; i < 15; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        addCardToZone("Cloud of Faeries", ai, ZoneType.Library);
        AssertJUnit.assertTrue(forge.ai.simulation.AlurenPlan.manaKillAlive(ai));
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.CARDS, forge.ai.simulation.AlurenPlan.stage(ai, opp));
    }

    @Test(timeOut = 120000)
    public void livingWishFetchesTheCarrierWhenTheStrokeIsGone() {
        alurenDeckWith(4, "Living Wish", "Raven Familiar", "Cavern Harpy");
        addCardToZone("Stroke of Genius", ai, ZoneType.Graveyard);
        addCardToZone("Maggot Carrier", ai, ZoneType.Sideboard);
        AssertJUnit.assertEquals("Maggot Carrier", forge.ai.simulation.AlurenPlan.wanted(ai,
                ai.getCardsIn(ZoneType.Sideboard)));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Living Wish", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theHarpyIsNotCastForItsManaCostWithoutAluren() {
        elfDeckWith(0, "Cavern Harpy", "Aluren");
        addCards("Island", 3, ai);
        addCards("Swamp", 3, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility harpy = LoopRunner.castFromHand(ai, alurenPlays(), "Cavern Harpy");
        AssertJUnit.assertNotNull(harpy);
        AssertJUnit.assertTrue("no Aluren in play: the Harpy is a piece of the loop, not a 2/1 for three mana",
                LoopRunner.held(ai, harpy));
    }

    @Test(timeOut = 120000)
    public void theHarpyIsNotPaidBackToTheHandForNothing() {
        alurenDeckWith(4, "Forest");
        final Card harpy = addCardToZone("Cavern Harpy", ai, ZoneType.Battlefield);
        SpellAbility back = null;
        for (final SpellAbility sa : harpy.getAllPossibleAbilities(ai, true)) {
            if (sa.isAbility() && !sa.isManaAbility()) {
                back = sa;
            }
        }
        AssertJUnit.assertNotNull(back);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.NONE, forge.ai.simulation.AlurenPlan.stage(ai, opp));
        AssertJUnit.assertTrue("nothing to loop: a life is not paid to return the Harpy", LoopRunner.held(ai, back));
    }

    @Test(timeOut = 120000)
    public void noTherapyAgainstAnEmptyHand() {
        alurenDeckWith(4, "Cabal Therapy", "Maggot Carrier", "Cavern Harpy");
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertFalse("nothing to name in an empty hand", "Cabal Therapy".equals(step.getHostCard().getName()));
    }

    @Test(timeOut = 120000)
    public void theBoardThatKillsByAttackingIsLeftAlone() {
        alurenDeckWith(4, "Maggot Carrier", "Cavern Harpy");
        opp.setLife(2, null);
        final Card savage = addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        savage.setSickness(false);
        AssertJUnit.assertNull("two power ready, two life: attack, do not loop", LoopRunner.next(ai, alurenPlays(), s -> true));
    }

    @Test(timeOut = 120000)
    public void withStrokeAndCloudInTheHandTheKillIsNotPostponedForTheCarrier() {
        alurenDeckWith(4, "Stroke of Genius", "Cloud of Faeries", "Cavern Harpy");
        addCardToZone("Maggot Carrier", ai, ZoneType.Sideboard);
        addCardToZone("Living Wish", ai, ZoneType.Library);
        AssertJUnit.assertEquals(forge.ai.simulation.AlurenPlan.Stage.MANA, forge.ai.simulation.AlurenPlan.stage(ai, opp));
    }

    @Test(timeOut = 120000)
    public void aWholeTurnWithStrokeCloudAndHarpyDrawsTheOpponentsLibrary() {
        alurenDeckWith(4, "Stroke of Genius", "Cloud of Faeries", "Cavern Harpy");
        runTurn();
        AssertJUnit.assertEquals("the Cloud untaps two lands a round, the pool pays for Stroke of Genius\n" + trace
                + gameStateToString(game), 0, opp.getCardsIn(ZoneType.Library).size());
    }

    @Test(timeOut = 120000)
    public void aWholeTurnWithTheCarrierAndTheHarpyDrainsTheOpponent() {
        alurenDeckWith(4, "Maggot Carrier", "Cavern Harpy");
        opp.setLife(4, null);
        runTurn();
        AssertJUnit.assertTrue("each Carrier enter costs both players a life\n" + trace + gameStateToString(game),
                game.isGameOver() || opp.getLife() <= 0);
    }

    @Test(timeOut = 120000)
    public void theHarpyIsCastForManaWhenTheSavageDrawsForIt() {
        elfDeckWith(0, "Cavern Harpy");
        addCardToZone("Aluren", ai, ZoneType.Library);
        for (int i = 0; i < 15; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        addCards("Island", 2, ai);
        addCards("Swamp", 2, ai);
        addCardToZone("Wirewood Savage", ai, ZoneType.Battlefield);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility harpy = LoopRunner.castFromHand(ai, alurenPlays(), "Cavern Harpy");
        AssertJUnit.assertNotNull(harpy);
        AssertJUnit.assertFalse("the Savage draws a card for every Beast: the Harpy is not held", LoopRunner.held(ai, harpy));
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Cavern Harpy", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theCantripIsCastWhileAlurenWaitsForMana() {
        elfDeckWith(0, "Brainstorm", "Aluren", "Cavern Harpy", "Maggot Carrier");
        for (int i = 0; i < 15; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        addCards("Island", 2, ai); // Aluren costs three: it waits, the Brainstorm does not
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility step = LoopRunner.next(ai, alurenPlays(), s -> true);
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Brainstorm", step.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void aHeritageDruidDeckCastsItsEngineBeforeRegalForce() {
        elfDeckWith(2, "Regal Force", "Nettle Sentinel", "Heritage Druid", "Elvish Visionary");
        AssertJUnit.assertTrue(ElfLoop.isDruidDeck(ai));
        final SpellAbility force = LoopRunner.castFromHand(ai, playsOf(), "Regal Force");
        AssertJUnit.assertNotNull(force);
        AssertJUnit.assertTrue("a card more for every green creature cast first", LoopRunner.held(ai, force));
        final SpellAbility step = LoopRunner.next(ai, playsOf(), s -> true);
        AssertJUnit.assertNotNull(step);
        final String name = step.getHostCard().getName();
        AssertJUnit.assertTrue("the engine first, not " + name, name.equals("Nettle Sentinel") || name.equals("Heritage Druid"));
    }

    @Test(timeOut = 120000)
    public void regalForceIsNotHeldWhenNothingCheaperIsLeft() {
        elfDeckWith(7, "Regal Force");
        addCardToZone("Nettle Sentinel", ai, ZoneType.Library);
        addCardToZone("Heritage Druid", ai, ZoneType.Library);
        final SpellAbility force = LoopRunner.castFromHand(ai, playsOf(), "Regal Force");
        AssertJUnit.assertNotNull(force);
        AssertJUnit.assertFalse(LoopRunner.held(ai, force));
    }

    @Test(timeOut = 120000)
    public void theTutorsOfADruidDeckTakeTheMissingPieceOfTheEngine() {
        elfDeckWith(2, "Heritage Druid");
        addCardToZone("Nettle Sentinel", ai, ZoneType.Library);
        final Card visionary = addCardToZone("Elvish Visionary", ai, ZoneType.Library);
        final Card sentinel = ai.getCardsIn(ZoneType.Library).stream().filter(c -> c.getName().equals("Nettle Sentinel"))
                .findFirst().get();
        final forge.game.card.CardCollection picks = ElfLoop.picks(ai, cards(visionary, sentinel), 1);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("the Druid is in the hand: the Sentinel is the piece that is missing", sentinel, picks.getFirst());
    }

    @Test(timeOut = 120000)
    public void theDiscardKeepsThePiecesOfTheKill() {
        alurenDeckWith(4, "Stroke of Genius", "Cloud of Faeries", "Intuition", "Intuition", "Cavern Harpy");
        final forge.game.card.CardCollection hand = new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Hand));
        final forge.game.card.CardCollection rest = forge.ai.simulation.AlurenPlan.withoutKeys(ai, hand);
        for (final Card c : rest) {
            AssertJUnit.assertEquals("only the Intuitions may go", "Intuition", c.getName());
        }
        AssertJUnit.assertEquals(2, rest.size());
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

    @Test(timeOut = 120000)
    public void aLandfallPermanentThatIsPaidForGoesBeforeTheLand() {
        elfDeckWith(3, "Scute Swarm", "Forest");
        playsOf();
        final List<SpellAbility> first = forge.ai.simulation.LandfallGuard.payoffsFirst(ai);
        AssertJUnit.assertEquals(1, first.size());
        AssertJUnit.assertEquals("Scute Swarm", first.get(0).getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theLandIsNotHeldForALandfallPermanentItIsNeededToCast() {
        elfDeckWith(2, "Scute Swarm", "Forest");
        playsOf();
        AssertJUnit.assertTrue("two Forests do not pay {2}{G}: the land comes first",
                forge.ai.simulation.LandfallGuard.payoffsFirst(ai).isEmpty());
    }

    @Test(timeOut = 120000)
    public void aLandfallPumpOnASickCreatureIsNoReasonToCastItFirst() {
        elfDeckWith(3, "Steppe Lynx", "Forest");
        playsOf();
        AssertJUnit.assertTrue(forge.ai.simulation.LandfallGuard.payoffsFirst(ai).isEmpty());
    }

    @Test(timeOut = 120000)
    public void aFetchLandIsNotCrackedForAPumpThatCannotAttack() {
        elfDeckWith(2);
        final Card lynx = addCardToZone("Steppe Lynx", ai, ZoneType.Battlefield);
        lynx.setSickness(true);
        final Card heath = addCardToZone("Windswept Heath", ai, ZoneType.Battlefield);
        playsOf();
        SpellAbility crack = null;
        for (final SpellAbility sa : heath.getSpellAbilities()) {
            if (forge.ai.simulation.LandfallGuard.isFetch(sa)) {
                crack = sa;
            }
        }
        AssertJUnit.assertNotNull(crack);
        AssertJUnit.assertEquals("landfall-pump-useless", forge.ai.simulation.LandfallGuard.fetchProblem(ai, crack));
        addCardToZone("Scute Swarm", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("a token for every land needs no combat", forge.ai.simulation.LandfallGuard.fetchProblem(ai, crack));
    }

    @Test(timeOut = 120000)
    public void scapeshiftCountsAValakutStillInTheLibrary() {
        elfDeckWith(7, "Scapeshift");
        addCardToZone("Valakut, the Molten Pinnacle", ai, ZoneType.Library);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        AssertJUnit.assertEquals("seven lands: Valakut and six Mountains, three damage each",
                18, forge.ai.simulation.RampPlan.scapeshiftDamage(ai));
    }

    @Test(timeOut = 120000)
    public void scapeshiftWaitsWhileTheMountainsAreTooFew() {
        elfDeckWith(5, "Scapeshift");
        addCardToZone("Valakut, the Molten Pinnacle", ai, ZoneType.Battlefield);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        AssertJUnit.assertEquals(0, forge.ai.simulation.RampPlan.scapeshiftDamage(ai));
    }

    @Test(timeOut = 120000)
    public void theLandSearchTakesValakutThenAMountain() {
        elfDeckWith(2, "Scapeshift");
        final Card forest = addCardToZone("Forest", ai, ZoneType.Library);
        final Card mountain = addCardToZone("Mountain", ai, ZoneType.Library);
        final Card valakut = addCardToZone("Valakut, the Molten Pinnacle", ai, ZoneType.Library);
        final forge.game.card.CardCollection picks =
                forge.ai.simulation.RampPlan.picks(ai, cards(forest, mountain, valakut), 2);
        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals(valakut, picks.get(0));
        AssertJUnit.assertEquals(mountain, picks.get(1));
    }

    @Test(timeOut = 120000)
    public void theLandSearchFillsRemainingSlotsAfterItsValakutPriority() {
        elfDeckWith(2, "Scapeshift");
        addCardToZone("Valakut, the Molten Pinnacle", ai, ZoneType.Battlefield);
        final Card mountain = addCardToZone("Mountain", ai, ZoneType.Library);
        final Card utility = addCardToZone("Kessig Wolf Run", ai, ZoneType.Library);

        final forge.game.card.CardCollection picks =
                forge.ai.simulation.RampPlan.picks(ai, cards(mountain, utility), 2);

        AssertJUnit.assertNotNull(picks);
        AssertJUnit.assertEquals("the search fills both available slots", 2, picks.size());
        AssertJUnit.assertEquals("the useful Mountain keeps priority", mountain, picks.get(0));
        AssertJUnit.assertTrue("the remaining slot uses the available utility land", picks.contains(utility));
    }

    @Test(timeOut = 120000)
    public void kessigWolfRunWaitsOutsideTheCombat() {
        elfDeckWith(1);
        final Card run = addCardToZone("Kessig Wolf Run", ai, ZoneType.Battlefield);
        playsOf();
        SpellAbility pump = null;
        for (final SpellAbility sa : run.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Pump) {
                pump = sa;
            }
        }
        AssertJUnit.assertNotNull(pump);
        AssertJUnit.assertTrue(forge.ai.simulation.RampPlan.wolfRunWaits(ai, pump));
    }

    // ---- Khalni Heart Expedition: the ramp plan, step by step

    private SpellAbility rampStep() {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        return forge.ai.simulation.RampPlan.expeditionStep(ai,
                sa -> { sa.setActivatingPlayer(ai); return sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false); }, land -> false);
    }

    private void quest(final Card expedition, final int n) {
        expedition.addCounterInternal(CounterEnumType.QUEST, n, ai, false, null, null);
    }

    /** What a land that is played or cracked does to the table, by hand: the land goes, a basic comes, a quest counter. */
    private void crackedFor(final Card fetch, final Card expedition) {
        game.getAction().moveToGraveyard(fetch, null);
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        quest(expedition, 1);
    }

    private void playedFor(final Card land, final Card expedition) {
        ai.getZone(ZoneType.Hand).remove(land);
        ai.getZone(ZoneType.Battlefield).add(land);
        ai.addLandPlayedThisTurn();
        quest(expedition, 1);
    }

    @Test(timeOut = 120000)
    public void theExpeditionIsCastBeforeTheFetchLandIsCracked() {
        elfDeckWith(2, "Khalni Heart Expedition");
        addCardToZone("Wooded Foothills", ai, ZoneType.Battlefield);
        final SpellAbility step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Khalni Heart Expedition", step.getHostCard().getName());
        AssertJUnit.assertTrue(step.isSpell());
    }

    @Test(timeOut = 120000)
    public void theExpeditionBecomesPayableAfterTheFetchAndIsCastBeforeExplore() {
        elfDeckWith(1, "Khalni Heart Expedition", "Explore");
        final Card heath = addCardToZone("Windswept Heath", ai, ZoneType.Battlefield);
        final SpellAbility crack = rampStep();
        AssertJUnit.assertNotNull("one mana short: the fetch land that is in play brings the second", crack);
        AssertJUnit.assertEquals("Windswept Heath", crack.getHostCard().getName());
        game.getAction().moveToGraveyard(heath, null);
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        final SpellAbility next = rampStep();
        AssertJUnit.assertNotNull(next);
        AssertJUnit.assertEquals("the new land pays for the Expedition, not for Explore", "Khalni Heart Expedition", next.getHostCard().getName());
    }

    @Test(timeOut = 120000)
    public void theExpeditionKeepsTheLandDropAndUsesTheFetchLandInPlay() {
        elfDeckWith(1, "Khalni Heart Expedition", "Forest");
        addCardToZone("Wooded Foothills", ai, ZoneType.Battlefield);
        final SpellAbility step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("the crack, not the land drop that earns a counter after the Expedition", "Wooded Foothills", step.getHostCard().getName());
        AssertJUnit.assertFalse(step.isLandAbility());
    }

    @Test(timeOut = 120000)
    public void foothillsExploreHeathSequenceFeedsTheExpedition() {
        elfDeckWith(2, "Explore", "Wooded Foothills", "Windswept Heath");
        final Card expedition = addCardToZone("Khalni Heart Expedition", ai, ZoneType.Battlefield);
        // 1. the land drop is a fetch land: it enters, and it is cracked as well
        SpellAbility step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertTrue("a fetch land is the land drop", step.isLandAbility());
        final Card first = step.getHostCard();
        AssertJUnit.assertTrue(first.getName().equals("Wooded Foothills") || first.getName().equals("Windswept Heath"));
        playedFor(first, expedition);
        // 2. crack it
        step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals(first, step.getHostCard());
        AssertJUnit.assertFalse(step.isLandAbility());
        crackedFor(first, expedition);
        // 3. the land drop is spent and a land is still in the hand: Explore
        step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals("Explore", step.getHostCard().getName());
        ai.resetLandsPlayedThisTurn(); // Explore's extra land drop
        // 4. the other fetch land
        step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertTrue(step.isLandAbility());
        final Card second = step.getHostCard();
        AssertJUnit.assertNotSame(first, second);
        playedFor(second, expedition);
        AssertJUnit.assertEquals("three counters", 3, expedition.getCounters(CounterEnumType.QUEST));
        // 5. the Expedition is activated before the second fetch land is cracked
        step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals(expedition, step.getHostCard());
        AssertJUnit.assertFalse(step.isSpell());
    }

    @Test(timeOut = 120000)
    public void theExpeditionIsActivatedAtThreeQuestCounters() {
        elfDeckWith(2, "Explore");
        final Card expedition = addCardToZone("Khalni Heart Expedition", ai, ZoneType.Battlefield);
        quest(expedition, 3);
        addCardToZone("Windswept Heath", ai, ZoneType.Battlefield);
        final SpellAbility step = rampStep();
        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals(expedition, step.getHostCard());
        AssertJUnit.assertEquals(ApiType.ChangeZone, step.getApi());
    }

    @Test(timeOut = 120000)
    public void theExpeditionIsNoPlanWithoutBasicLandsToFetch() {
        elfDeckWith(2, "Khalni Heart Expedition");
        for (final Card c : new ArrayList<>(ai.getCardsIn(ZoneType.Library))) {
            game.getAction().moveToGraveyard(c, null);
        }
        AssertJUnit.assertNull("nothing to fetch: not cast first", rampStep());
        final Card expedition = addCardToZone("Khalni Heart Expedition", ai, ZoneType.Battlefield);
        quest(expedition, 3);
        AssertJUnit.assertNull("nothing to fetch: not activated first", rampStep());
    }

    @Test(timeOut = 120000)
    public void anExpeditionOneManaShortUsesTheBasicLandDropBeforeAFetchLand() {
        elfDeckWith(0, "Khalni Heart Expedition");
        addCardToZone("Mountain", ai, ZoneType.Battlefield);
        addCardToZone("Wooded Foothills", ai, ZoneType.Hand);
        addCardToZone("Forest", ai, ZoneType.Hand);

        final SpellAbility step = rampStep();

        AssertJUnit.assertNotNull(step);
        AssertJUnit.assertEquals(
                "Forest",
                step.getHostCard().getName());
        AssertJUnit.assertTrue(step.isLandAbility());
    }

    @Test(timeOut = 120000)
    public void aWholeTurnWithKikiJikiAndPestermiteKillsTheOpponent() {
        elfDeckWith(0, "Pestermite");
        addCards("Island", 3, ai);
        final Card kiki = addCardToZone("Kiki-Jiki, Mirror Breaker", ai, ZoneType.Battlefield);
        kiki.setSickness(false); // it has been there since the start of the turn
        opp.setLife(8, null);
        runTurn();
        AssertJUnit.assertTrue("Pestermite untaps Kiki-Jiki, who copies it again, and the hasty fliers attack\n" + trace
                + gameStateToString(game), game.isGameOver() || opp.getLife() <= 0);
    }

    @Test(timeOut = 120000)
    public void aWholeTurnWithStoneforgeMysticPutsBatterskullIntoPlay() {
        elfDeckWith(0, "Batterskull");
        addCards("Plains", 2, ai);
        final Card mystic = addCardToZone("Stoneforge Mystic", ai, ZoneType.Battlefield);
        mystic.setSickness(false); // it has been there since the start of the turn: it can tap
        runTurn();
        boolean inPlay = false;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            inPlay |= c.getName().equals("Batterskull");
        }
        AssertJUnit.assertTrue("{1}{W}, {T}: the Equipment from the hand comes in without its five mana\n" + trace
                + gameStateToString(game), inPlay);
    }
}
