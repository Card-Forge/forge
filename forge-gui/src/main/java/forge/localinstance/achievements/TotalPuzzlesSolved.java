package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class TotalPuzzlesSolved extends ProgressiveAchievement {
    public TotalPuzzlesSolved(int bronze0, int silver0, int gold0, int mythic0) {
        super("TotalPuzzlesSolved", Localizer.getInstance().getMessage("lblTotalPuzzlesSolved"), null,
            Localizer.getInstance().getMessage("lblSolveNPuzzles", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblSolveNPuzzles", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblSolveNPuzzles", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblSolveNPuzzles", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected boolean eval(Player player, Game game) {
        return game.getMatch().isMatchOver() && game.getMatch().isWonBy(player.getLobbyPlayer());
    }

    @Override
    protected String getNoun() {
        return Localizer.getInstance().getMessage("lblPuzzle");
    }
}

