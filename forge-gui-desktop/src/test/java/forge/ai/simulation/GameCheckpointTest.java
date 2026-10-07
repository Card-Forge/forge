package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;

import forge.ai.ComputerUtil;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameCheckpoint;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.item.PaperCard;
import forge.trackable.TrackableProperty;

public class GameCheckpointTest extends SimulationTest {

    private void cast(Player p, Card card, Object target) {
        SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        boolean ok = ComputerUtil.handlePlayingSpellAbility(p, sa, playing -> {
            if (target instanceof Card c) {
                playing.getTargets().add(c);
            } else if (target instanceof Player tp) {
                playing.getTargets().add(tp);
            }
        });
        AssertJUnit.assertTrue("could not cast " + card, ok);
    }

    private void castAndResolve(Player p, Card card, Object target) {
        cast(p, card, target);
        GameSimulator.resolveStack(p.getGame(), p.getWeakestOpponent());
    }

    private static void check(List<String> problems, String what, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            problems.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void assertNoProblems(List<String> problems) {
        if (!problems.isEmpty()) {
            throw new AssertionError("Not restored:\n  " + String.join("\n  ", problems));
        }
    }

    private static int untappedLands(Player p) {
        return (int) p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isLand() && !c.isTapped()).count();
    }

    private Game newGame() {
        Game game = initAndCreateGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        return game;
    }

    @Test
    public void restoresCardPlayerAndZoneStateAfterSpellsResolve() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card bear = addCard("Runeclaw Bear", p);
        bear.setSickness(false);
        Card giant = addCard("Hill Giant", opp);
        addCards("Mountain", 2, p);
        addCards("Forest", 1, p);
        addCards("Plains", 2, p);
        Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        Card shock = addCardToZone("Shock", p, ZoneType.Hand);
        Card alarm = addCardToZone("Raise the Alarm", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        int handBefore = p.getCardsIn(ZoneType.Hand).size();
        int battlefieldBefore = game.getCardsIn(ZoneType.Battlefield).size();
        int oppLifeBefore = opp.getLife();
        int bearPowerBefore = bear.getNetPower();

        GameCheckpoint checkpoint = GameCheckpoint.capture(game);

        castAndResolve(p, growth, bear);
        castAndResolve(p, shock, giant);
        castAndResolve(p, alarm, null);
        bear.addCounterInternal(CounterEnumType.P1P1, 1, p, false, null, null);
        opp.setLife(oppLifeBefore - 3, null);

        AssertJUnit.assertTrue(bear.getNetPower() > bearPowerBefore);
        AssertJUnit.assertEquals(2, giant.getDamage());
        AssertJUnit.assertEquals(battlefieldBefore + 2, game.getCardsIn(ZoneType.Battlefield).size());

        checkpoint.restore();

        List<String> problems = new ArrayList<>();
        Card bearNow = game.findById(bear.getId());
        Card giantNow = game.findById(giant.getId());
        check(problems, "bear power", bearPowerBefore, bearNow.getNetPower());
        check(problems, "bear +1/+1 counters", 0, bearNow.getCounters(CounterEnumType.P1P1));
        check(problems, "giant damage", 0, giantNow.getDamage());
        check(problems, "giant zone", ZoneType.Battlefield, giantNow.getZone().getZoneType());
        check(problems, "battlefield size (tokens gone)", battlefieldBefore, game.getCardsIn(ZoneType.Battlefield).size());
        check(problems, "hand size", handBefore, p.getCardsIn(ZoneType.Hand).size());
        check(problems, "cards in hand are back in the hand zone", true,
                p.getCardsIn(ZoneType.Hand).stream().allMatch(c -> c.getZone() == p.getZone(ZoneType.Hand)));
        check(problems, "opp life", oppLifeBefore, opp.getLife());
        check(problems, "untapped lands", 5, untappedLands(p));
        check(problems, "graveyard empty", 0, p.getCardsIn(ZoneType.Graveyard).size());
        check(problems, "spells cast this turn", 0, p.getSpellsCastThisTurn());
        check(problems, "stack empty", true, game.getStack().isEmpty());
        check(problems, "mana pool empty", true, p.getManaPool().isEmpty());
        assertNoProblems(problems);

        // The restored game has to keep playing: cast the same spell again.
        castAndResolve(p, game.findById(shock.getId()), giantNow);
        AssertJUnit.assertEquals(2, giantNow.getDamage());
        AssertJUnit.assertEquals(1, p.getSpellsCastThisTurn());
    }

    /** With the experimental restore on, cancelling a cast puts back the state stashed before the decision. */
    @Test
    public void cancellingACastRestoresTheStashedState() {
        Game game = newGame();
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        Player p = game.getPlayers().get(1);
        Card bear = addCard("Runeclaw Bear", p);
        addCard("Forest", p);
        Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        game.stashGameState();
        cast(p, growth, bear);
        bear.addCounterInternal(CounterEnumType.P1P1, 1, p, false, null, null);
        AssertJUnit.assertEquals(1, game.getStack().size());

        GameActionUtil.rollbackAbility(game.getStack().peekAbility(), null, 0, null, null);

        List<String> problems = new ArrayList<>();
        check(problems, "stack empty", true, game.getStack().isEmpty());
        check(problems, "growth in hand", ZoneType.Hand, game.findById(growth.getId()).getZone().getZoneType());
        check(problems, "forest untapped", 1, untappedLands(p));
        check(problems, "bear +1/+1 counters", 0, game.findById(bear.getId()).getCounters(CounterEnumType.P1P1));
        check(problems, "spells cast this turn", 0, p.getSpellsCastThisTurn());
        assertNoProblems(problems);
    }

    /**
     * A checkpoint only restores what it walks into. Anything else reachable from the game has to be
     * immutable or deliberately left alone; a new kind of holder showing up here needs a look.
     */
    @Test
    public void everythingNotWalkedIsImmutableOrLeftAloneOnPurpose() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Runeclaw Bear", p);
        addCard("Serra Angel", opp);
        addCards("Mountain", 2, p);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        addCardToZone("Raise the Alarm", p, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        castAndResolve(p, p.getCardsIn(ZoneType.Hand).get(0), opp);

        Set<Class<?>> expected = Set.of(
                // not game state
                EventBus.class, StopWatch.class,
                // immutable
                ManaCost.class, PaperCard.class, ColorSet.class);
        Set<Class<?>> unexpected = new HashSet<>(GameCheckpoint.auditUnwalkedTypes(game));
        unexpected.removeAll(expected);
        AssertJUnit.assertTrue("not walked, check they hold no mutable game state: " + unexpected, unexpected.isEmpty());
    }

    /** A network client is sent what a restore rewrote in the views, with the next delta. */
    @Test
    public void aRestoreReachesNetworkClientsWithTheNextDelta() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mountain", p);
        Card shock = addCardToZone("Shock", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        DeltaSyncManager client = new DeltaSyncManager();
        client.collectDeltas(game.getView());
        GameCheckpoint checkpoint = GameCheckpoint.capture(game);
        int oppLife = opp.getLife();

        castAndResolve(p, shock, opp);
        client.collectDeltas(game.getView()); // the client has seen the Shock resolve

        checkpoint.restore();
        DeltaPacket delta = client.collectDeltas(game.getView());

        Map<TrackableProperty, Object> oppDelta = delta.getObjectDeltas().get(DeltaPacket.makeDeltaKey(opp.getView()));
        AssertJUnit.assertNotNull("nothing sent for the opponent", oppDelta);
        AssertJUnit.assertEquals(oppLife, oppDelta.get(TrackableProperty.Life));
        Map<TrackableProperty, Object> ownDelta = delta.getObjectDeltas().get(DeltaPacket.makeDeltaKey(p.getView()));
        AssertJUnit.assertNotNull("nothing sent for the player", ownDelta);
        AssertJUnit.assertTrue("hand not sent: " + ownDelta.keySet(), ownDelta.containsKey(TrackableProperty.Hand));
        AssertJUnit.assertTrue("graveyard not sent: " + ownDelta.keySet(), ownDelta.containsKey(TrackableProperty.Graveyard));
        // the Shock that went to the graveyard was a new object; the one put back in hand is sent in full
        AssertJUnit.assertTrue("card back in hand not sent",
                delta.getNewObjects().containsKey(DeltaPacket.makeDeltaKey(game.findById(shock.getId()).getView())));
    }
}
