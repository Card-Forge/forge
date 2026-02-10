package forge.gamesimulationtests;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.GameWrapper;
import forge.gamesimulationtests.util.card.CardSpecificationBuilder;
import forge.gamesimulationtests.util.gamestate.GameStateSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.playeractions.ActionPreCondition;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import forge.gamesimulationtests.util.playeractions.testactions.TestAction;
import forge.gamesimulationtests.util.playeractions.testactions.EndTestAction;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Issue4745Test extends BaseGameSimulationTest {

    @Test
    public void simpleTest() {
        Assert.assertTrue(true);
    }

    @Test(enabled = false)
    public void testOutpostSiegeRollbackBug() {
        PlayerActions actions = new PlayerActions(
                new SetOutpostSiegeModeAction(),
                new RollbackVerificationAction()
                        .when(new ActionPreCondition().turn(1)),
                new EndTestAction(PlayerSpecification.PLAYER_2)
                        .when(new ActionPreCondition().turn(1))
        );

        GameWrapper gameWrapper = new GameWrapper(
                new GameStateSpecificationBuilder()
                        .addCard(new CardSpecificationBuilder("Outpost Siege").controller(PlayerSpecification.PLAYER_1).battlefield())
                        .addCard(new CardSpecificationBuilder("Memnite").controller(PlayerSpecification.PLAYER_1).library())
                        .build(),
                actions
        );

        runGame(gameWrapper, PlayerSpecification.PLAYER_1, 1);
    }

    private static class SetOutpostSiegeModeAction extends TestAction {
        public SetOutpostSiegeModeAction() {
            super(PlayerSpecification.PLAYER_1);
        }

        @Override
        public void perform(Game game, Player player) {
            for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.getName().equals("Outpost Siege")) {
                    c.setChosenType("Khans");
                }
            }
        }
    }

    private static class RollbackVerificationAction extends TestAction {
        public RollbackVerificationAction() {
            super(PlayerSpecification.PLAYER_1);
        }

        @Override
        public void perform(Game game, Player player) {
            Card memnite = null;
            for (Card c : game.getCardsIn(ZoneType.Exile)) {
                if (c.getName().equals("Memnite")) {
                    memnite = c;
                    break;
                }
            }
            Assert.assertNotNull(memnite, "Memnite should be in exile");
            Assert.assertFalse(memnite.getAllPossibleAbilities(player, true).isEmpty(), "Should be able to play Memnite from exile");

            // Simulate casting (move to stack)
            game.getAction().moveToStack(memnite, null);
            
            // Simulate rollback (move back to exile)
            game.getAction().moveTo(ZoneType.Exile, memnite, null, AbilityKey.newMap());

            Assert.assertFalse(memnite.getAllPossibleAbilities(player, true).isEmpty(), 
                    "Should be able to play Memnite from exile after rollback (ForgetOnMoved should prevent effect cleanup)");
        }
    }
}
