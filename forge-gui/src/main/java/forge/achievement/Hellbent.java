package forge.achievement;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class Hellbent extends Achievement {
    public Hellbent() {
        super("Hellbent", "Hellbent", "Win a game with no cards in your", 0,
                "hand", 1,
                "hand or library", 2,
                "hand, library, or graveyard", 3,
                "hand, library, graveyard, or battlefield", 4);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            if (player.getZone(ZoneType.Hand).size() == 0) {
                if (player.getZone(ZoneType.Library).size() == 0) {
                    if (player.getZone(ZoneType.Graveyard).size() == 0) {
                        if (player.getZone(ZoneType.Battlefield).size() == 0) {
                            return 4;
                        }
                        return 3;
                    }
                    return 2;
                }
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
