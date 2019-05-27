package forge.game.mulligan;

import com.google.common.collect.ImmutableList;
import forge.game.player.Player;

public class VancouverMulligan extends ParisMulligan {
    public VancouverMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    public void afterMulligan() {
        super.afterMulligan();
        player.getGame().getAction().scry(ImmutableList.of(player), 1, null);
    }
}
