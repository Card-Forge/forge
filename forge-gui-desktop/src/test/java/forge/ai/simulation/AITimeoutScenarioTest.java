package forge.ai.simulation;

import java.lang.reflect.Field;
import java.util.Map;

import forge.game.zone.ZoneType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.ai.AiAttackController;
import forge.ai.AiController;
import forge.ai.ComputerUtilCombat;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.AITimeoutTracker;

/**
 * Scenario tests for the cooperative AI deadline (PR #12265).
 *
 * HOW THE DEADLINE IS FORCED: instead of racing a real 1-2 second timer (flaky on CI), each test opens an
 * "already expired" session for the AI player with {@link #expireSession(Player)}. AiController nests its own
 * begin()/arm() inside that session, so it inherits the expired deadline and the first AITimeoutTracker.check()
 * throws Expired - exactly what happens when the real timer runs out, only deterministic. Each scenario also has a
 * CONTROL run with a normal deadline so a wrong test setup shows up as a failing control, not as a false alarm.
 *
 */
public class AITimeoutScenarioTest extends SimulationTest {
    private static final long EXPIRED_NANOS = 1_000_000L; // 1 ms: gone before the AI starts
    private int sessionPlayerId = -1;

    @AfterMethod
    public void closeSession() {
        if (sessionPlayerId >= 0) {
            AITimeoutTracker.end(sessionPlayerId);
            sessionPlayerId = -1;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 1. must-attack creature + timeout
    // ---------------------------------------------------------------------------------------------

    @Test
    public void mustAttackCreatureAttacksNormally_control() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Card juggernaut = addCard("Juggernaut", ai);
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Combat combat = newCombat(game, ai);
        aiOf(ai).declareAttackers(ai, combat);

        Assert.assertTrue(combat.getAttackers().contains(juggernaut), "control: Juggernaut must attack");
    }

    @Test
    public void mustAttackCreatureStillAttacksWhenDeadlineAlreadyPassed() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Card juggernaut = addCard("Juggernaut", ai);
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Combat combat = newCombat(game, ai);
        expireSession(ai);
        final long t0 = System.nanoTime();
        aiOf(ai).declareAttackers(ai, combat);
        final long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;

        Assert.assertTrue(combat.getAttackers().contains(juggernaut),
                "timeout must fall back to the legal/mandatory attack declaration");
        Assert.assertTrue(elapsedMs < 1000, "AI must stop at the deadline, took " + elapsedMs + " ms");
    }

    @Test
    public void lateWorkersNeverTouchCombatAfterDeadline() throws Exception {
        // many must-attackers -> the parallel must-attack check has real work; after the deadline the combat must stay
        // stable (no ConcurrentModification / attackers appearing afterwards)
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        for (int i = 0; i < 8; i++) {
            addCard("Juggernaut", ai);
        }
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Combat combat = newCombat(game, ai);
        expireSession(ai);
        aiOf(ai).declareAttackers(ai, combat);
        final int atReturn = combat.getAttackers().size();
        Thread.sleep(300); // give zombie workers time to misbehave
        Assert.assertEquals(combat.getAttackers().size(), atReturn, "combat changed after declareAttackers returned");
        Assert.assertEquals(atReturn, 8, "all mandatory attackers must be declared");
    }

    // ---------------------------------------------------------------------------------------------
    // 2. lure / must-block when the deadline fires mid-block
    // ---------------------------------------------------------------------------------------------

    @Test
    public void lureForcesBlocksNormally_control() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        final Card elf = addCard("Taunting Elf", attacker); // "All creatures able to block it do so"
        final Card bear1 = addCard("Grizzly Bears", ai);
        final Card bear2 = addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, elf);

        aiOf(ai).declareBlockersFor(ai, combat);

        Assert.assertTrue(combat.getBlockers(elf).contains(bear1) && combat.getBlockers(elf).contains(bear2),
                "control: both bears must block the lure creature");
    }

    @Test
    public void lureStillForcesBlocksWhenDeadlineAlreadyPassed() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        final Card elf = addCard("Taunting Elf", attacker);
        final Card bear1 = addCard("Grizzly Bears", ai);
        final Card bear2 = addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, elf);

        expireSession(ai);
        aiOf(ai).declareBlockersFor(ai, combat);

        Assert.assertTrue(combat.getBlockers(elf).contains(bear1) && combat.getBlockers(elf).contains(bear2),
                "completeRequiredBlocks must still honour the lure after a timeout");
    }

    // ---------------------------------------------------------------------------------------------
    // 3. lethal attack + timeout: the AI must at least chump instead of ending with no blocks
    // ---------------------------------------------------------------------------------------------

    @Test
    public void lethalAttackIsChumpedWhenDeadlineAlreadyPassed() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        ai.setLife(3, null);
        final Card a1 = addCard("Grizzly Bears", attacker);
        final Card a2 = addCard("Grizzly Bears", attacker);
        addCard("Grizzly Bears", ai); // one potential chump blocker
        final Combat combat = attack(game, attacker, ai, a1, a2);
        Assert.assertTrue(ComputerUtilCombat.lifeThatWouldRemain(ai, combat) <= 0, "setup: unblocked attack is lethal");

        expireSession(ai);
        aiOf(ai).declareBlockersFor(ai, combat);

        Assert.assertFalse(combat.getAllBlockers().isEmpty(), "AI must not be left without blocks while facing lethal");
        Assert.assertTrue(ComputerUtilCombat.lifeThatWouldRemain(ai, combat) > 0, "AI must survive the combat");
    }

    // ---------------------------------------------------------------------------------------------
    // 4. Propaganda-style attack cost + timeout
    // ---------------------------------------------------------------------------------------------

    @Test
    public void unpayableAttackCostIsRespected_control() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Player defender = game.getPlayers().get(1);
        addCard("Propaganda", defender); // creatures can't attack its controller unless their controller pays {2}
        final Card juggernaut = addCard("Juggernaut", ai); // wants to attack, AI has no mana
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Combat combat = newCombat(game, ai);
        aiOf(ai).declareAttackers(ai, combat);

        Assert.assertFalse(combat.getAttackers().contains(juggernaut), "control: can't pay Propaganda -> no attack");
    }

    @Test
    public void unpayableAttackCostIsRespectedWhenDeadlineAlreadyPassed() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Player defender = game.getPlayers().get(1);
        addCard("Propaganda", defender);
        final Card juggernaut = addCard("Juggernaut", ai);
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Combat combat = newCombat(game, ai);
        expireSession(ai);
        aiOf(ai).declareAttackers(ai, combat);

        Assert.assertFalse(combat.getAttackers().contains(juggernaut),
                "removeUnpayableAttackers must also run after a timeout");
    }

    // ---------------------------------------------------------------------------------------------
    // 5. predicting a HUMAN's attack must not light up that player's timer bar
    // ---------------------------------------------------------------------------------------------

    @Test
    public void predictingHumanAttackDoesNotTrackTimer() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Player human = humanPlayer(game, 1);
        addCard("Grizzly Bears", human);
        addCard("Grizzly Bears", ai);
        startCombatStep(game, human, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Map<?, ?> slots = trackerSlots();
        slots.remove(human.getId());
        slots.remove(ai.getId());

        // what AI code does when it predicts the opponent's attack (e.g. while evaluating a spell)
        new AiAttackController(human).declareAttackers(new Combat(human));
        Assert.assertFalse(slots.containsKey(human.getId()), "a human's predicted attack must not open a timer session");

        // control: a real AI attack does open (and close) a session
        new AiAttackController(ai).declareAttackers(newCombat(game, ai));
        Assert.assertTrue(slots.containsKey(ai.getId()), "control: AI attack tracks its timer");
        Assert.assertFalse(AITimeoutTracker.isActive(ai.getId()), "session must be closed again");
    }

    // ---------------------------------------------------------------------------------------------
    // helpers (ADAPT HERE if the harness differs)
    // ---------------------------------------------------------------------------------------------

    /** Opens a session for the player whose deadline has already passed; nested AI sessions inherit it. */
    private void expireSession(final Player p) {
        sessionPlayerId = p.getId();
        AITimeoutTracker.begin(sessionPlayerId, EXPIRED_NANOS);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Player aiPlayer(final Game game, final int idx) {
        final Player p = game.getPlayers().get(idx);
        p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()));
        return p;
    }

    /** AI-brained controller that reports itself as human, like a human-controlled seat in a real match. */
    private static Player humanPlayer(final Game game, final int idx) {
        final Player p = game.getPlayers().get(idx);
        p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
            @Override
            public boolean isAI() {
                return false;
            }
        });
        return p;
    }

    private static AiController aiOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private static void startCombatStep(final Game game, final Player active, final PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, active);
        for (final Card c : active.getCardsIn(ZoneType.Battlefield)) {
            c.setSickness(false); // addCard leaves creatures summoning-sick
        }
        game.getAction().checkStateEffects(true); // apply static abilities (Propaganda, must-attack, lure ...)
        for (final Card c : active.getCardsIn(ZoneType.Battlefield)) {
            Assert.assertFalse(c.isSick(), "setup: " + c + " is still summoning-sick");
        }
    }

    private static Combat newCombat(final Game game, final Player attacker) {
        final Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    /** Builds a combat where `attacker` attacks `defender` with the given creatures, in the declare blockers step. */
    private static Combat attack(final Game game, final Player attacker, final Player defender, final Card... attackers) {
        startCombatStep(game, attacker, PhaseType.COMBAT_DECLARE_BLOCKERS);
        final Combat combat = new Combat(attacker);
        for (final Card c : attackers) {
            combat.addAttacker(c, (GameEntity) defender);
        }
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    /** The tracker has no "was begin() called" accessor; a slot only exists once begin() ran for that player id. */
    private static Map<?, ?> trackerSlots() throws Exception {
        final Field f = AITimeoutTracker.class.getDeclaredField("SLOTS");
        f.setAccessible(true);
        return (Map<?, ?>) f.get(null);
    }
}