package forge.achievement;

import forge.assets.FSkinProp;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class Hellbent extends Achievement {
    public Hellbent() {
        super("Hellbent", "Win a game with no cards",
                "in your hand", 1,
                "in your hand or library", 2,
                "in your hand, library, or graveyard", 3,
                "in your hand, library, graveyard, or battlefield", 4,
                FSkinProp.IMG_HELLBENT);
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
    public String getSubTitle() {
        return null;
    }
}
