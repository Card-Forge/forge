package forge.headless;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.Player;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * Agent that always chooses option 0 (pass priority / do nothing).
 * Useful for testing scenarios where a player should be passive.
 */
public class PlayerControllerZero extends PlayerControllerTUI {

    public PlayerControllerZero(Game game, Player p, LobbyPlayer lp, boolean askMana, boolean numericChoices) {
        // Use a BufferedReader that always returns "0"
        super(game, p, lp, askMana, numericChoices, createZeroReader());
    }

    private static BufferedReader createZeroReader() {
        // This reader will be called repeatedly, so we create an infinite stream of "0\n"
        return new BufferedReader(new StringReader(generateInfiniteZeros()));
    }

    private static String generateInfiniteZeros() {
        // Generate a very long string of "0\n" repeated many times
        // This should be enough for any reasonable game
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("0\n");
        }
        return sb.toString();
    }

    @Override
    public boolean isAI() {
        return true; // Act like an AI for game logic purposes
    }
}
