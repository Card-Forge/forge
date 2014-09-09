package forge.screens.match.views;

import forge.FThreads;
import forge.GuiBase;
import forge.view.PlayerView;

public class VFlashbackZone extends VCardDisplayArea {
    private final PlayerView player;

    public VFlashbackZone(PlayerView player0) {
        player = player0;
    }

    @Override
    public void update() {
        FThreads.invokeInEdtNowOrLater(GuiBase.getInterface(), updateRoutine);
    }

    private final Runnable updateRoutine = new Runnable() {
        @Override
        public void run() {
            refreshCardPanels(player.getFlashbackCards());
        }
    };
}
