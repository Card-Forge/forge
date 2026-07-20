package forge.ai.blocking;

import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Covers block requirements - lure and "must be blocked if able" - which nothing else
 * in the suite exercises.
 *
 * CombatUtil.mustBlockAnAttacker is only reached when an attacker places a requirement
 * on a blocker, so a board without such attackers runs straight past it. That makes the
 * method easy to change without any test noticing, which matters because it is a
 * tempting target for further optimisation: its result depends only on the blocker, so
 * caching it per blocker looks safe until you notice the result also depends on which
 * blocks have already been assigned.
 */
public class BlockRequirementTests extends SimulationTest {

    private static final String LURE_CREATURE = "Elvish Bard";        // all able to block do so
    private static final String MUST_BLOCK = "Goblin Fire Fiend";     // must be blocked if able
    private static final String VANILLA = "Grizzly Bears";

    /** Declares blockers for the AI against every creature the attacker controls. */
    private Combat attackWithAll(final Game game, final Player attacker, final Player ai) {
        final Combat combat = new Combat(attacker);
        for (final Card c : attacker.getCreaturesInPlay()) {
            combat.addAttacker(c, ai);
        }
        ((PlayerControllerAi) ai.getController()).getAi().declareBlockersFor(ai, combat);
        return combat;
    }

    @Test
    public void lureTakesEveryAbleBlocker() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);

        final Card lure = addCard(LURE_CREATURE, human);
        final Card other = addCard(VANILLA, human);
        for (int i = 0; i < 3; i++) {
            addCard(VANILLA, ai).setSickness(false);
        }

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, human);
        game.getAction().checkStateEffects(true);

        final Combat combat = attackWithAll(game, human, ai);

        assertEquals(combat.getBlockers(lure).size(), 3, "every able blocker must block the lure creature");
        assertEquals(combat.getBlockers(other).size(), 0,
                "no blocker may block another attacker while the lure is unsatisfied");
    }

    @Test
    public void mustBeBlockedIfAbleGetsABlocker() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);

        final Card mustBlock = addCard(MUST_BLOCK, human);
        for (int i = 0; i < 3; i++) {
            addCard(VANILLA, ai).setSickness(false);
        }

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, human);
        game.getAction().checkStateEffects(true);

        final Combat combat = attackWithAll(game, human, ai);

        assertTrue(combat.getBlockers(mustBlock).size() >= 1,
                "an attacker that must be blocked if able needs at least one blocker");
    }
}
