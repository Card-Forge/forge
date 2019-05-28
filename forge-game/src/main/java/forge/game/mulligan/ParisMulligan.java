package forge.game.mulligan;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ParisMulligan extends AbstractMulligan {
    public ParisMulligan(Player p, boolean firstMullFree) {
        super(p, firstMullFree);
    }

    public boolean canMulligan() {
        return !kept && !player.getZone(ZoneType.Hand).isEmpty();
    }

    public int handSizeAfterNextMulligan() {
        int extraCard = firstMulliganFree ? 1 : 0;

        return player.getMaxHandSize() - timesMulliganed + extraCard;
    }
}
