package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class TotalGameWins extends ProgressiveAchievement {
    public TotalGameWins(int bronze0, int silver0, int gold0, int mythic0) {
        super("TotalGameWins", "Total Game Wins", null,
            String.format("Win %d games", bronze0), bronze0,
            String.format("Win %d games", silver0), silver0,
            String.format("Win %d games", gold0), gold0,
            String.format("Win %d games", mythic0), mythic0);
    }

    @Override
    protected boolean eval(Player player, Game game) {
        return player.getOutcome().hasWon();
    }

    @Override
    protected String getNoun() {
        return "Game";
    }
}
