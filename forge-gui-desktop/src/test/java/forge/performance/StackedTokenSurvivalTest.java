package forge.performance;

import forge.ai.AITest;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.LobbyPlayerForTests;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Roadmap item 1b: stacks must survive a burst of zone additions instead of
 * being collapsed by every view refresh. TokenEffectBase suppresses the view
 * update during a batch; this exercises the suppression/merge mechanics that
 * make that safe.
 */
public class StackedTokenSurvivalTest extends AITest {

    private Card makeToken(Game game, Player p1) {
        Card token = new Card(game.nextCardId(), p1.getGame());
        token.setName("Soldier");
        token.setOwner(p1);
        token.setController(p1, 0);
        token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        token.setBasePower(1);
        token.setBaseToughness(1);
        token.setGamePieceType(GamePieceType.TOKEN);
        return token;
    }

    @Test
    public void testStacksSurviveBatchWhileViewUpdateSuppressed() {
        List<RegisteredPlayer> players = new java.util.ArrayList<>();
        RegisteredPlayer p1Spec = new RegisteredPlayer(new Deck("Player 1"));
        p1Spec.setPlayer(new LobbyPlayerForTests("Player 1", new PlayerActions()));
        players.add(p1Spec);
        RegisteredPlayer p2Spec = new RegisteredPlayer(new Deck("Player 2"));
        p2Spec.setPlayer(new LobbyPlayerForTests("Player 2", new PlayerActions()));
        players.add(p2Spec);

        GameRules rules = new GameRules(GameType.Commander);
        Match match = new Match(rules, players, "Stack Survival Test Match");
        Game game = match.createGame();
        game.setAge(GameStage.Play);

        Player controller = game.getPlayers().get(0);
        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) controller.getZone(ZoneType.Battlefield);

        // TokenEffectBase wraps the whole burst with setSuppressViewUpdate(true).
        boolean restore = battlefield.setSuppressViewUpdate(true);
        try {
            for (int i = 0; i < 5; i++) {
                Card token = makeToken(game, controller);
                battlefield.add(token);
                assertTrue("token can be stacked", battlefield.tryStackToken(token));
            }
            // With the view refresh suppressed, each add() leaves prior stacks intact,
            // so all five coalesce into a single stack rather than collapsing to
            // individual cards on every add.
            assertEquals("batch must coalesce into one stack", 1, battlefield.getStackedTokens().size());
            assertEquals("one stack holds all five", 5, battlefield.getStackedTokens().get(0).getQuantity());
        } finally {
            battlefield.setSuppressViewUpdate(restore);
        }

        // First real read materializes the batch into distinct cards.
        assertEquals("read materializes all cards", 5, battlefield.getCards().size());
        assertEquals("stacks are gone once materialized", 0, battlefield.getStackedTokens().size());
    }
}