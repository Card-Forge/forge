package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.player.Player;
import forge.util.Localizer;

public class MatchWinStreak extends StreakAchievement {
    public MatchWinStreak(int bronze0, int silver0, int gold0, int mythic0) {
        super("MatchWinStreak",Localizer.getInstance().getMessage("lblMatchWinStreak"), null,
            Localizer.getInstance().getMessage("lblWinNMatchesInARow", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblWinNMatchesInARow", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblWinNMatchesInARow", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblWinNMatchesInARow", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected Boolean eval(Player player, Game game) {
        if (game.getMatch().isMatchOver()) {
            return game.getMatch().isWonBy(player.getLobbyPlayer());
        }
        return null;
    }
}
