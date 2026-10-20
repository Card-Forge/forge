package forge.ai.simulation;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.ai.AiBlockController;
import forge.ai.AiController;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.AITimeoutTracker;

/**
 * Tests for the review assessment: partial (illegal) blocks after a timeout, and no half-restored state on the game thread.
 * Same technique as AITimeoutScenarioTest: an already-expired session makes the first check() throw, deterministically.
 */
public class AITimeoutBlockValidityTest extends SimulationTest {
    private static final long EXPIRED_NANOS = 1_000_000L;
    private int sessionPlayerId = -1;

    @AfterMethod
    public void closeSession() {
        if (sessionPlayerId >= 0) {
            AITimeoutTracker.end(sessionPlayerId);
            sessionPlayerId = -1;
        }
    }

    /** Direct: a single blocker left on a menace attacker (what makeRequiredBlocks & co. can produce) is removed. */
    @Test
    public void completeRequiredBlocksDropsSingleBlockOnMenaceAttacker() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        final Card brute = addCard("Boggart Brute", attacker); // 3/2 menace
        final Card bear = addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, brute);
        combat.addBlocker(brute, bear); // illegal on its own: menace needs two blockers

        new AiBlockController(ai, false).completeRequiredBlocks(combat);

        Assert.assertTrue(combat.getBlockers(brute).isEmpty(), "single block on a menace attacker must be removed");
        assertNoIllegalBlocks(combat);
    }

    /** Direct: a legal double block on a menace attacker is left alone. */
    @Test
    public void completeRequiredBlocksKeepsLegalDoubleBlock() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        final Card brute = addCard("Boggart Brute", attacker);
        final Card bear1 = addCard("Grizzly Bears", ai);
        final Card bear2 = addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, brute);
        combat.addBlocker(brute, bear1);
        combat.addBlocker(brute, bear2);

        new AiBlockController(ai, false).completeRequiredBlocks(combat);

        Assert.assertEquals(combat.getBlockers(brute).size(), 2, "a legal gang block must survive the validity pass");
    }

    /** End to end: expired deadline, lethal menace attacker, only one potential blocker -> never a single block. */
    @Test
    public void timeoutNeverLeavesSingleBlockOnMenaceAttacker() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        ai.setLife(3, null);
        final Card brute = addCard("Boggart Brute", attacker);
        addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, brute);

        expireSession(ai);
        aiOf(ai).declareBlockersFor(ai, combat);

        assertNoIllegalBlocks(combat);
        Assert.assertNotEquals(combat.getBlockers(brute).size(), 1, "a lone blocker on a menace attacker is illegal");
    }

    /** End to end: with two potential blockers a double block is legal; whatever the AI does must satisfy the invariant. */
    @Test
    public void timeoutWithTwoBlockersKeepsBlocksLegal() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player ai = aiPlayer(game, 0);
        ai.setLife(3, null);
        final Card brute = addCard("Boggart Brute", attacker);
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        final Combat combat = attack(game, attacker, ai, brute);

        expireSession(ai);
        aiOf(ai).declareBlockersFor(ai, combat);

        assertNoIllegalBlocks(combat);
    }

    /**
     * Spell choice with an expired deadline must not throw into the game thread and must leave no hypothetical castSA
     * behind on any real card (the eval thread is armed, the game thread is not).
     */
    @Test
    public void spellChoiceAfterDeadlineLeavesNoHypotheticalCastSa() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        expireSession(ai);
        aiOf(ai).chooseSpellAbilityToPlay(); // must return (pass priority), not throw

        for (final Card c : game.getCardsInGame()) {
            Assert.assertNull(c.getCastSA(), c + " kept a hypothetical castSA after the AI timed out");
        }
    }

    /**
     * getPredictedCombat() assigns its field before declareAttackers runs. If the deadline cuts the prediction short, the
     * partial (empty) combat must not stay cached: the next call has to compute the real prediction again.
     */
    @Test
    public void cutShortPredictedCombatIsNotCached() {
        final Game game = initAndCreateGame();
        final Player ai = aiPlayer(game, 0);
        final Card bear = addCard("Grizzly Bears", ai); // opponent has no creatures: it will attack
        startCombatStep(game, ai, PhaseType.COMBAT_DECLARE_ATTACKERS);

        final Object limit = AITimeoutTracker.arm(System.nanoTime() - 1, false); // this thread's deadline already passed
        try {
            aiOf(ai).getPredictedCombat();
            Assert.fail("expected the prediction to be cut short by the expired deadline");
        } catch (AITimeoutTracker.Expired expected) {
            // ok
        } finally {
            AITimeoutTracker.disarm(limit);
        }

        final Combat predicted = aiOf(ai).getPredictedCombat(); // now unarmed: must be a complete prediction
        Assert.assertTrue(predicted.getAttackers().contains(bear), "a partial prediction was cached");
    }

    /** Every attacker that has blockers must be blockable with exactly that many (same predicate as master's validity loop). */
    private static void assertNoIllegalBlocks(final Combat combat) {
        for (final Card attacker : combat.getAttackers()) {
            final int blockers = combat.getBlockers(attacker).size();
            Assert.assertTrue(blockers == 0 || CombatUtil.canAttackerBeBlockedWithAmount(attacker, blockers, combat),
                    attacker + " is blocked by an illegal number of creatures: " + blockers);
        }
    }

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

    private static AiController aiOf(final Player p) {
        return ((PlayerControllerAi) p.getController()).getAi();
    }

    private static void startCombatStep(final Game game, final Player active, final PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, active);
        for (final Card c : active.getCardsIn(ZoneType.Battlefield)) {
            c.setSickness(false);
        }
        game.getAction().checkStateEffects(true);
    }

    private static Combat attack(final Game game, final Player attacker, final Player defender, final Card... attackers) {
        startCombatStep(game, attacker, PhaseType.COMBAT_DECLARE_BLOCKERS);
        final Combat combat = new Combat(attacker);
        for (final Card c : attackers) {
            combat.addAttacker(c, (GameEntity) defender);
        }
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }
}