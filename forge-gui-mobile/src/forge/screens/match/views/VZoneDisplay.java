package forge.screens.match.views;

import forge.FThreads;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class VZoneDisplay extends VCardDisplayArea {
    private final Player player;
    private final ZoneType zoneType;

    public VZoneDisplay(Player player0, ZoneType zoneType0) {
        player = player0;
        zoneType = zoneType0;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            refreshCardPanels(player.getZone(zoneType).getCards());
        }
    };
}
