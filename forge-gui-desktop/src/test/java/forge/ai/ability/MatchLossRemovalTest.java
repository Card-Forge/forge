package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class MatchLossRemovalTest extends AITest {

    private static final List<String> LOSE_ON_MATCH_LOSS = Arrays.asList(
            "DB$ RemoveFromMatch | Defined$ ValidCommand Card.YouOwn+IsCommander | RemoveFromInventory$ True | OnMatchLoss$ True",
            "DB$ RemoveFromMatch | Defined$ ValidAll Card.YouOwn+inZoneHand,Card.YouOwn+inZoneLibrary | RemoveFromInventory$ True | OnMatchLoss$ True | Amount$ 2");

    private RegisteredPlayer first;
    private RegisteredPlayer second;

    /** A Commander match in which each player's script puts their own commander and 2 random cards at stake. */
    private Match newMatch(int gamesPerMatch) {
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add("Grizzly Bears", 1);
        deck.getMain().add("Forest", 40);
        first = RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("first", null));
        second = RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("second", null));
        first.addStartGameScript(LOSE_ON_MATCH_LOSS);
        second.addStartGameScript(LOSE_ON_MATCH_LOSS);
        final GameRules rules = new GameRules(GameType.Commander);
        rules.setGamesPerMatch(gamesPerMatch);
        return new Match(rules, Arrays.asList(first, second), "Test");
    }

    private static void playGameConcededBy(Match match, RegisteredPlayer conceding) {
        final Game game = match.createGame();
        match.startGame(game, () -> {
            for (final Player p : game.getPlayers()) {
                if (p.getRegisteredPlayer() == conceding) {
                    p.concede();
                }
            }
            game.getAction().checkGameOverCondition();
        });
    }

    private static void playGameConcededAtPlayOrDrawBy(Match match, RegisteredPlayer conceding) {
        final Game game = match.createGame();
        for (final Player p : game.getPlayers()) {
            if (p.getRegisteredPlayer() != conceding) {
                continue;
            }
            p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
                @Override
                public Player chooseStartingPlayer(boolean isFirstGame) {
                    player.concede();
                    game.getAction().checkGameOverCondition();
                    return player;
                }
            });
        }
        match.startGame(game);
        AssertJUnit.assertEquals("the game ended before its first turn", 0, game.getPhaseHandler().getTurn());
    }

    private static void assertLostOnce(Match match, RegisteredPlayer loser, RegisteredPlayer winner) {
        final List<PaperCard> lost = match.getAnteResult(loser).lostCards;
        AssertJUnit.assertEquals("the loser loses their commander and 2 cards once", 3, lost.size());
        AssertJUnit.assertEquals("the commander is lost", 1, lost.stream().filter(c -> c.getName().equals("Grizzly Bears")).count());
        AssertJUnit.assertTrue("the winner loses nothing", match.getAnteResult(winner).lostCards.isEmpty());
    }

    @Test
    public void onlyMatchLoserLosesCards() {
        final Match match = newMatch(3);
        playGameConcededBy(match, first);
        playGameConcededBy(match, second);
        playGameConcededBy(match, second);

        assertLostOnce(match, second, first);
    }

    @Test
    public void quittingBetweenGamesLosesCards() {
        final Match match = newMatch(3);
        playGameConcededBy(match, first);
        match.concede(first);

        assertLostOnce(match, first, second);
    }

    @Test
    public void concedingAtGame2PlayOrDrawPromptLosesCards() {
        final Match match = newMatch(3);
        playGameConcededBy(match, first);
        // the loser of the last game is asked whether to play or draw
        playGameConcededAtPlayOrDrawBy(match, first);

        assertLostOnce(match, first, second);
    }

    @Test
    public void quittingAfterMatchIsOverLosesNothingMore() {
        final Match match = newMatch(1);
        playGameConcededBy(match, first);
        match.concede(first);
        match.concede(second);

        assertLostOnce(match, first, second);
    }

    @Test
    public void scriptRunTwiceLosesCardsOnce() {
        final Match match = newMatch(1);
        // as after a Karn Liberated restart
        first.addStartGameScript(LOSE_ON_MATCH_LOSS);
        playGameConcededBy(match, first);

        assertLostOnce(match, first, second);
    }
}
