package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;

public class TotalMatchWins extends ProgressiveAchievement {
    public TotalMatchWins(int bronze0, int silver0, int gold0, int mythic0) {
        super("TotalMatchWins", "Total Match Wins", null,
            String.format("Win %d matches", bronze0), bronze0,
            String.format("Win %d matches", silver0), silver0,
            String.format("Win %d matches", gold0), gold0,
            String.format("Win %d matches", mythic0), mythic0);
    }

    @Override
    protected boolean eval(Player player, Game game) {
        return game.getMatch().isMatchOver() && game.getMatch().isWonBy(player.getLobbyPlayer());
    }

    @Override
    protected String getNoun() {
        return "Match";
    }
}
