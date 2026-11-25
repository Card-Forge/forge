package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ActivationGuard;
import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sacrifice outlets only with fodder, and a trick for several creatures once the blocks are down.
 */
public class FlatMonteCarloMainEffectTest extends AITest {
    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCardToZone(name, owner, ZoneType.Battlefield);
        c.setSickness(false);
        return c;
    }

    private SpellAbility activated(final Card c) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(ai, true)) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no ability on " + c);
    }

    @Test
    public void anOutletDoesNotEatAGoodCreature() {
        setUp();
        final Card bombardment = addCardToZone("Goblin Bombardment", ai, ZoneType.Battlefield);
        ready("Serra Angel", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals("valuable-sacrifice", ActivationGuard.problem(ai, activated(bombardment)));
    }

    @Test
    public void anOutletIsOfferedWithFodder() {
        setUp();
        final Card bombardment = addCardToZone("Goblin Bombardment", ai, ZoneType.Battlefield);
        final Card angel = ready("Serra Angel", ai);
        angel.setSVar("SacMe", "1");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNull(ActivationGuard.problem(ai, activated(bombardment)));
    }

    @Test
    public void aTrickForTwoCreaturesIsAimedAtTheTwoInTheFight() {
        setUp();
        final Card appeal = addCardToZone("Appeal to Eirdu", ai, ZoneType.Hand);
        final Card bears = ready("Grizzly Bears", ai);
        final Card ogre = ready("Hill Giant", ai);
        final Card idle = ready("Runeclaw Bear", ai);
        final Card wall1 = ready("Wall of Stone", opp);
        final Card wall2 = ready("Wall of Stone", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        final Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addAttacker(ogre, opp);
        combat.addBlocker(bears, wall1);
        combat.addBlocker(ogre, wall2);
        combat.setBlocked(bears, true);
        combat.setBlocked(ogre, true);
        game.getPhaseHandler().setCombat(combat);
        final SpellAbility sa = appeal.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.multiCombatTargets(ai, sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(bears));
        AssertJUnit.assertTrue(sa.getTargets().getTargetCards().contains(ogre));
        AssertJUnit.assertFalse("not a creature outside the fight", sa.getTargets().getTargetCards().contains(idle));
    }
}
