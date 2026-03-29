package forge.gamesimulationtests;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationBuilder;
import forge.gamesimulationtests.util.gamestate.GameStateSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.playeractions.ActionPreCondition;
import forge.gamesimulationtests.util.playeractions.DeclareAttackersAction;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import forge.gamesimulationtests.util.playeractions.testactions.EndTestAction;
import forge.gamesimulationtests.util.playeractions.testactions.TestAction;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LureTest extends BaseGameSimulationTest {

    @Test
    public void testLureForcesBlocks() {
        CardSpecification grizzlyBears = new CardSpecificationBuilder("Grizzly Bears").controller(PlayerSpecification.PLAYER_1).battlefield().build();
        CardSpecification lure = new CardSpecificationBuilder("Lure").controller(PlayerSpecification.PLAYER_1).battlefield().build();
        CardSpecification memnite = new CardSpecificationBuilder("Memnite").controller(PlayerSpecification.PLAYER_2).battlefield().build();
        CardSpecification ornithopter = new CardSpecificationBuilder("Ornithopter").controller(PlayerSpecification.PLAYER_2).battlefield().build();

        PlayerActions actions = new PlayerActions(
                new AttachLureAction().when(new ActionPreCondition().phase(PhaseType.MAIN1)),
                new DeclareAttackersAction(PlayerSpecification.PLAYER_1).attack(grizzlyBears),
                new CheckLureBlocksAction().when(new ActionPreCondition().phase(PhaseType.COMBAT_DECLARE_BLOCKERS)),
                new EndTestAction(PlayerSpecification.PLAYER_1)
        );

        GameWrapper gameWrapper = new GameWrapper(
                new GameStateSpecificationBuilder()
                        .addCard(grizzlyBears)
                        .addCard(lure)
                        .addCard(memnite)
                        .addCard(ornithopter)
                        .build(),
                actions
        );

        gameWrapper.runGame();
    }

    private static class AttachLureAction extends TestAction {
        public AttachLureAction() {
            super(PlayerSpecification.PLAYER_1);
        }

        @Override
        public void perform(Game game, Player player) {
             Card bear = game.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Grizzly Bears")).findFirst().orElse(null);
             Card lure = game.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Lure")).findFirst().orElse(null);
             
             // If Lure went to graveyard (SBA due to no target), move it back
             if (lure == null) {
                 lure = game.getCardsIn(ZoneType.Graveyard).stream().filter(c -> c.getName().equals("Lure")).findFirst().orElse(null);
                 if (lure != null) {
                     game.getAction().moveTo(ZoneType.Battlefield, lure, null, null);
                 }
             }

             if (bear != null && lure != null && !bear.getEnchantedBy().contains(lure)) {
                 // Workaround: In test environment, Lure might lose Aura type if not loaded correctly or if moved from GY
                 if (!lure.isAura()) {
                     lure.addType("Aura");
                 }
                 lure.attachToEntity(bear, null);
             }
        }
    }

    private static class CheckLureBlocksAction extends TestAction {
        public CheckLureBlocksAction() {
            super(PlayerSpecification.PLAYER_2);
        }

        @Override
        public void perform(Game game, Player player) {
             Combat combat = game.getCombat();
             Assert.assertNotNull(combat, "Combat should be active");
             
             // 1. Verify no blocks declared yet -> Validation fails
             String validationResult = CombatUtil.validateBlocks(combat, player);
             Assert.assertNotNull(validationResult, "Validation should fail because no blocks are declared yet");
             Assert.assertTrue(validationResult.contains("must block"), "Validation message should mention 'must block', got: " + validationResult);
             
             // 2. Declare valid blocks (All must block)
             Card bear = game.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Grizzly Bears")).findFirst().orElse(null);
             Card memnite = game.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Memnite")).findFirst().orElse(null);
             Card ornithopter = game.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Ornithopter")).findFirst().orElse(null);
             
             if (bear != null && memnite != null && ornithopter != null) {
                 combat.addBlocker(bear, memnite);
                 combat.addBlocker(bear, ornithopter);
                 
                 // 3. Verify valid blocks -> Validation passes
                 validationResult = CombatUtil.validateBlocks(combat, player);
                 Assert.assertNull(validationResult, "Validation should pass with all creatures blocking, but got: " + validationResult);
             }
             
             // Concede to allow test to finish with Player 1 win
             player.concede();
        }
    }
}
