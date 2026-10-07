package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ComputerUtil;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameCheckpoint;
import forge.game.UndoHistory;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
        System.out.println(checkpoint);

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

    @Test
    public void captureAndRestoreStayCheapOnALargeBoard() {
        Game game = initAndCreateThreePlayerGame();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        String[] permanents = {"Runeclaw Bear", "Hill Giant", "Serra Angel", "Llanowar Elves", "Sol Ring", "Forest", "Island"};
        for (Player p : game.getPlayers()) {
            for (int i = 0; i < 40; i++) {
                addCard(permanents[i % permanents.length], p);
            }
            for (int i = 0; i < 7; i++) {
                addCardToZone("Lightning Bolt", p, ZoneType.Hand);
            }
            for (int i = 0; i < 60; i++) {
                addCardToZone("Giant Growth", p, ZoneType.Library);
            }
        }
        game.getAction().checkStateEffects(true);

        GameCheckpoint.capture(game); // warm the per-class field cache
        long best = Long.MAX_VALUE;
        GameCheckpoint checkpoint = null;
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            checkpoint = GameCheckpoint.capture(game);
            best = Math.min(best, System.nanoTime() - start);
        }
        long start = System.nanoTime();
        checkpoint.restore();
        long restoreNanos = System.nanoTime() - start;
        System.out.printf("Large board (%d cards): %s, best capture %.1f ms, restore %.1f ms%n",
                game.getCardsInGame().size(), checkpoint, best / 1e6, restoreNanos / 1e6);
        AssertJUnit.assertTrue("capture too slow: " + best / 1e6 + " ms", best < 1_000_000_000L);
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

    @Test
    public void nothingIsStashedWithTheExperimentalRestoreOff() {
        Game game = newGame();
        game.stashGameState();
        AssertJUnit.assertNull(game.getStashedState());
        AssertJUnit.assertFalse(game.restoreGameState());
    }

    /** A priority point takes over the state the game loop stashed for the same decision. */
    @Test
    public void undoHistoryUsesTheStashedStateForAPriorityPoint() {
        Game game = newGame();
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        game.getUndoHistory().setEnabled(true);
        Player p = game.getPlayers().get(1);
        Card bear = addCard("Runeclaw Bear", p);
        addCard("Forest", p);
        Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        game.stashGameState();
        GameCheckpoint stashed = game.getStashedState();
        UndoHistory.Point point = game.getUndoHistory().recordPriority(p);
        AssertJUnit.assertSame(stashed, point.getCheckpoint());
        point.setOutcome("played Giant Growth");

        castAndResolve(p, growth, bear);
        game.stashGameState(); // the next decision's stash must not disturb the point
        AssertJUnit.assertTrue(game.getUndoHistory().requestUndo(p, point));
        AssertJUnit.assertSame(point, game.getUndoHistory().applyPendingUndo());

        AssertJUnit.assertEquals(ZoneType.Hand, game.findById(growth.getId()).getZone().getZoneType());
        AssertJUnit.assertEquals(2, game.findById(bear.getId()).getNetPower());
        AssertJUnit.assertEquals(1, untappedLands(p));
    }

    /** GameSnapshot is still what copies a game for the AI when the experimental restore is on. */
    @Test
    public void gameSnapshotStillCopiesAGameForSimulation() {
        Game game = newGame();
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        Player p = game.getPlayers().get(1);
        Card bear = addCard("Runeclaw Bear", p);
        addCardToZone("Giant Growth", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        GameCopier copier = new GameCopier(game);
        Game copy = copier.makeCopy();

        AssertJUnit.assertNotSame(game, copy);
        AssertJUnit.assertEquals(game.getCardsInGame().size(), copy.getCardsInGame().size());
        Card bearCopy = (Card) copier.find(bear);
        AssertJUnit.assertNotSame(bear, bearCopy);
        AssertJUnit.assertEquals(bear.getId(), bearCopy.getId());
        AssertJUnit.assertEquals(ZoneType.Battlefield, bearCopy.getZone().getZoneType());
    }

    /** The stack's own undo (taking back a mana ability) still works on a restored game. */
    @Test
    public void manaAbilityCanStillBeUndoneAfterARestore() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Card bear = addCard("Runeclaw Bear", p);
        Card forest = addCard("Forest", p);
        Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility tapForMana = forest.getManaAbilities().get(0);
        tapForMana.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, tapForMana, null));
        AssertJUnit.assertEquals(1, game.getStack().getUndoStackSize());
        AssertJUnit.assertFalse(p.getManaPool().isEmpty());

        GameCheckpoint checkpoint = GameCheckpoint.capture(game);
        castAndResolve(p, growth, bear);
        AssertJUnit.assertTrue(p.getManaPool().isEmpty());

        checkpoint.restore();

        AssertJUnit.assertEquals(1, game.getStack().getUndoStackSize());
        AssertJUnit.assertTrue(game.getStack().canUndo(p));
        AssertJUnit.assertTrue(game.getStack().undo());
        AssertJUnit.assertTrue(p.getManaPool().isEmpty());
        AssertJUnit.assertFalse(game.findById(forest.getId()).isTapped());
        AssertJUnit.assertEquals(0, game.getStack().getUndoStackSize());
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

        Set<String> expected = Set.of(
                // left alone on purpose
                "com.google.common.eventbus.EventBus", "forge.ai.PlayerControllerAi", "forge.game.GameLog",
                "forge.game.GameRules", "forge.game.Match", "org.apache.commons.lang3.time.StopWatch",
                // immutable
                "forge.card.mana.ManaCost", "forge.item.PaperCard", "forge.card.ColorSet", "java.lang.Object");
        Set<String> unexpected = new TreeSet<>(GameCheckpoint.auditUnwalkedTypes(game).keySet());
        unexpected.removeAll(expected);
        AssertJUnit.assertTrue("not walked, check they hold no mutable game state: " + unexpected, unexpected.isEmpty());
    }

    @Test
    public void undoesAResponseToASpellOnTheStack() {
        Game game = newGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card bear = addCard("Runeclaw Bear", p);
        addCards("Forest", 1, p);
        Card growth = addCardToZone("Giant Growth", p, ZoneType.Hand);
        addCards("Mountain", 1, opp);
        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        // Opponent bolts the bear; the human gets priority with it on the stack.
        cast(opp, bolt, bear);
        AssertJUnit.assertEquals(1, game.getStack().size());
        GameCheckpoint checkpoint = GameCheckpoint.capture(game);

        // The misplay: respond with Giant Growth, then everything resolves.
        castAndResolve(p, growth, bear);
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals(ZoneType.Battlefield, game.findById(bear.getId()).getZone().getZoneType());
        AssertJUnit.assertEquals(3, game.findById(bear.getId()).getDamage());

        checkpoint.restore();

        List<String> problems = new ArrayList<>();
        check(problems, "stack size", 1, game.getStack().size());
        check(problems, "bolt on stack", bolt.getId(), game.getStack().peekAbility().getHostCard().getId());
        check(problems, "bolt target", bear.getId(),
                ((Card) game.getStack().peekAbility().getTargets().getFirstTargetedCard()).getId());
        check(problems, "growth in hand", ZoneType.Hand, game.findById(growth.getId()).getZone().getZoneType());
        check(problems, "bear damage", 0, game.findById(bear.getId()).getDamage());
        check(problems, "bear power", 2, game.findById(bear.getId()).getNetPower());
        check(problems, "forest untapped", 1, untappedLands(p));
        assertNoProblems(problems);

        // Pass instead this time: the restored Bolt resolves and kills the bear.
        GameSimulator.resolveStack(game, p);
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals(ZoneType.Graveyard, game.findById(bear.getId()).getZone().getZoneType());
    }
}
