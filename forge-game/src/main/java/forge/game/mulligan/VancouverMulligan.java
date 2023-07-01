package forge.game.mulligan;

import com.google.common.collect.ImmutableList;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class VancouverMulligan extends ParisMulligan {
    public VancouverMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    public void afterMulligan() {
        super.afterMulligan();
        if (player.getStartingHandSize() > player.getZone(ZoneType.Hand).size()) {
            player.getGame().getAction().scry(ImmutableList.of(player), 1, null);
        }
    }
}
