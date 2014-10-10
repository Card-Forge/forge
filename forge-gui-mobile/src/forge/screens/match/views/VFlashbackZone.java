package forge.screens.match.views;

import forge.FThreads;
import forge.game.player.PlayerView;

public class VFlashbackZone extends VCardDisplayArea {
    private final PlayerView player;

    public VFlashbackZone(PlayerView player0) {
        player = player0;
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            refreshCardPanels(player.getFlashback());
        }
    };
}
