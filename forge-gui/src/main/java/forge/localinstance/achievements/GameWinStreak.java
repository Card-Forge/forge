package forge.localinstance.achievements;

import forge.game.IGame;
import forge.game.player.Player;
import forge.util.Localizer;

public class GameWinStreak extends StreakAchievement {
    public GameWinStreak(int bronze0, int silver0, int gold0, int mythic0) {
        super("GameWinStreak", 
            Localizer.getInstance().getMessage("lblGameWinStreak"), null,
            Localizer.getInstance().getMessage("lblWinNGamesInARow", String.valueOf(bronze0)), bronze0,
            Localizer.getInstance().getMessage("lblWinNGamesInARow", String.valueOf(silver0)), silver0,
            Localizer.getInstance().getMessage("lblWinNGamesInARow", String.valueOf(gold0)), gold0,
            Localizer.getInstance().getMessage("lblWinNGamesInARow", String.valueOf(mythic0)), mythic0
        );
    }

    @Override
    protected Boolean eval(Player player, IGame game) {
        return player.getOutcome().hasWon();
    }
}
