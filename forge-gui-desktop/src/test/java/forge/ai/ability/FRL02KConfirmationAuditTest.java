package forge.ai.ability;

import java.util.List;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/** Focused, test-only evidence for the FRL-02K trigger boundary audit. */
public class FRL02KConfirmationAuditTest extends AITest {

    private static final class CountingController extends PlayerControllerAi {
        private final boolean triggerDecision;
        private int confirmTriggerCalls;

        private CountingController(final Game game, final Player player, final boolean triggerDecision) {
            super(game, player, new LobbyPlayerAi(player.getName() + "-frl02k-audit", null));
            this.triggerDecision = triggerDecision;
        }

        @Override
        public boolean confirmTrigger(final WrappedAbility wrapper) {
            confirmTriggerCalls++;
            return triggerDecision;
        }

        @Override
        public void orderAndPlaySimultaneousSa(final List<SpellAbility> activePlayerSAs) {
            for (final SpellAbility sa : activePlayerSAs) {
                sa.resolve();
            }
        }
    }

    @Test
    public void mandatoryTriggerResolvesWithoutConfirmTrigger() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final CountingController controller = installController(game, player, true);
        final int battlefieldBefore = game.getCardsIn(ZoneType.Battlefield).size();
        addCard("Bitterblossom", player);

        runUpkeepTrigger(game, player);

        AssertJUnit.assertEquals("a mandatory trigger must not call confirmTrigger", 0,
                controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("the mandatory trigger effect still resolves", battlefieldBefore + 2,
                game.getCardsIn(ZoneType.Battlefield).size());
        AssertJUnit.assertEquals("Bitterblossom's mandatory trigger resolves", 19, player.getLife());
    }

    @Test
    public void optionalNoCostTriggerMapsTrueToEffectAndFalseToNoEffect() {
        final TriggerRunResult accepted = runOptionalAngel(true);
        AssertJUnit.assertEquals("optional no-cost trigger asks exactly once", 1,
                accepted.controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("true allows the token effect", accepted.battlefieldBefore + 2,
                accepted.game.getCardsIn(ZoneType.Battlefield).size());

        final TriggerRunResult declined = runOptionalAngel(false);
        AssertJUnit.assertEquals("optional no-cost trigger asks exactly once", 1,
                declined.controller.confirmTriggerCalls);
        AssertJUnit.assertEquals("false prevents the token effect", declined.battlefieldBefore + 1,
                declined.game.getCardsIn(ZoneType.Battlefield).size());
    }

    private TriggerRunResult runOptionalAngel(final boolean decision) {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(1);
        final CountingController controller = installController(game, player, decision);
        final int battlefieldBefore = game.getCardsIn(ZoneType.Battlefield).size();
        addCard("Luminous Angel", player);
        runUpkeepTrigger(game, player);
        return new TriggerRunResult(game, controller, battlefieldBefore);
    }

    private CountingController installController(final Game game, final Player player, final boolean decision) {
        final CountingController controller = new CountingController(game, player, decision);
        player.dangerouslySetController(controller);
        return controller;
    }

    private void runUpkeepTrigger(final Game game, final Player player) {
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, player);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(player), false);
        AssertJUnit.assertTrue("fixture must create a simultaneous trigger entry",
                game.getStack().hasSimultaneousStackEntries());
        game.getStack().addAllTriggeredAbilitiesToStack();
    }

    private static final class TriggerRunResult {
        private final Game game;
        private final CountingController controller;
        private final int battlefieldBefore;

        private TriggerRunResult(final Game game, final CountingController controller, final int battlefieldBefore) {
            this.game = game;
            this.controller = controller;
            this.battlefieldBefore = battlefieldBefore;
        }
    }
}
