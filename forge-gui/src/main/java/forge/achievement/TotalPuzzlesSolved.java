package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class TotalPuzzlesSolved extends ProgressiveAchievement {
    public TotalPuzzlesSolved(int bronze0, int silver0, int gold0, int mythic0) {
        super("TotalPuzzlesSolved", "Total Puzzles Solved", null,
            String.format(bronze0 == 1 ? "Solve a puzzle" : "Solve %d puzzles", bronze0), bronze0,
            String.format("Solve %d puzzles", silver0), silver0,
            String.format("Solve %d puzzles", gold0), gold0,
            String.format("Solve %d puzzles", mythic0), mythic0);
    }

    @Override
    protected boolean eval(Player player, Game game) {
        return game.getMatch().isMatchOver() && game.getMatch().isWonBy(player.getLobbyPlayer());
    }

    @Override
    protected String getNoun() {
        return "Puzzle";
    }
}

