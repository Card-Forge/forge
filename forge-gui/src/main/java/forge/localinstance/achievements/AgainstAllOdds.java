package forge.localinstance.achievements;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.util.Localizer;

public class AgainstAllOdds extends Achievement {
    public AgainstAllOdds() {
        super("Against all Odds", Localizer.getInstance().getMessage("lblAgainstAllOdds"),
            Localizer.getInstance().getMessage("lblWinGame"), 0,
            Localizer.getInstance().getMessage("lblAgainstIndividual", "3"), 1,
            Localizer.getInstance().getMessage("lblAgainstIndividual", "7"), 2,
            Localizer.getInstance().getMessage("lblAgainstTeam", "3"), 3,
            Localizer.getInstance().getMessage("lblAgainstTeam", "7"), 4
        );
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon() && game.getRegisteredPlayers().size() - player.getRegisteredOpponents().size() == 1) {
            int teamNum = 0;
            for (Player opp : player.getRegisteredOpponents()) {
                PlayerCollection otherOpps = player.getRegisteredOpponents();
                otherOpps.remove(opp);
                if (Iterables.all(otherOpps, PlayerPredicates.sameTeam(opp))) {
                    teamNum++;   
                } else if (Iterables.all(otherOpps, Predicates.not(PlayerPredicates.sameTeam(opp)))) {
                    teamNum--;
                }
            }
            if (teamNum == 7) {
                return 4;
            }
            if (teamNum >= 3) {
                return 3;
            }
            if (teamNum == -7) {
                return 2;
            }
            if (teamNum <= -3) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    protected String getNoun() {
        return null;
    }

    @Override
    public String getSubTitle(boolean includeTimestamp) {
        if (includeTimestamp) {
            String formattedTimestamp = getFormattedTimestamp();
            if (formattedTimestamp != null) {
                return "Earned " + formattedTimestamp;
            }
        }
        return null;
    }
}
