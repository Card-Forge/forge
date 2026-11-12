package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
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

public class StakeEffectTest extends AITest {

    private RegisteredPlayer loser;
    private RegisteredPlayer winner;

    /** Plays one game of a Commander match in which both players stake their commander and 5 of their nonbasic cards, and one player concedes. */
    private Match concedeFirstGame(int gamesPerMatch) {
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add("Grizzly Bears", 1);
        deck.getMain().add("Forest", 20);
        deck.getMain().add("Llanowar Elves", 20);
        final List<String> script = Arrays.asList(
                "DB$ Stake | Defined$ ValidCommand Card.YouOwn+IsCommander",
                "DB$ Stake | Defined$ ValidAll Card.YouOwn+nonBasic+inZoneHand,Card.YouOwn+nonBasic+inZoneLibrary | Amount$ 5");
        loser = RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("loser", null));
        winner = RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("winner", null));
        loser.addStartGameScript(script);
        winner.addStartGameScript(script);
        final GameRules rules = new GameRules(GameType.Commander);
        rules.setGamesPerMatch(gamesPerMatch);
        final Match match = new Match(rules, Arrays.asList(loser, winner), "Test");
        final Game game = match.createGame();

        match.startGame(game, () -> {
            for (final Player p : game.getPlayers()) {
                if (p.getRegisteredPlayer() == loser) {
                    p.concede();
                }
            }
            game.getAction().checkGameOverCondition();
        });
        return match;
    }

    private static void assertLostStakes(List<PaperCard> lost) {
        AssertJUnit.assertEquals("the commander is lost", 1, lost.stream().filter(c -> c.getName().equals("Grizzly Bears")).count());
        AssertJUnit.assertEquals("exactly 5 nonbasic cards are lost", 5, lost.stream().filter(c -> c.getName().equals("Llanowar Elves")).count());
        AssertJUnit.assertEquals("no basic lands are lost", 6, lost.size());
    }

    @Test
    public void matchLoserLosesStakedCards() {
        final Match match = concedeFirstGame(1);

        assertLostStakes(match.getAnteResult(loser).lostCards);
        AssertJUnit.assertTrue("the winner loses nothing", match.getAnteResult(winner).lostCards.isEmpty());
    }

    @Test
    public void quittingUnfinishedMatchLosesStakedCards() {
        final Match match = concedeFirstGame(3);
        AssertJUnit.assertTrue("losing one game of three loses nothing yet", match.getAnteResult(loser).lostCards.isEmpty());

        match.forfeitStakes(loser);

        assertLostStakes(match.getAnteResult(loser).lostCards);
        AssertJUnit.assertTrue("the other player loses nothing", match.getAnteResult(winner).lostCards.isEmpty());
    }
}
