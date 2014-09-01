package forge.screens.match.views;

import forge.FThreads;
import forge.game.player.Player;

public class VFlashbackZone extends VCardDisplayArea {
    private final Player player;

    public VFlashbackZone(Player player0) {
        player = player0;
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            refreshCardPanels(player.getCardsActivableInExternalZones(false));
        }
    };
}
