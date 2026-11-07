package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.LoopRunner;
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
}
