package forge.gamemodes.replay;

import forge.gamemodes.net.client.ReplayPlayer;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.localinstance.skin.FSkinProp;
import forge.util.IHasForgeLog;

import java.util.List;

/** The single entry point both the desktop and the mobile menus call to watch a recorded game. */
public final class ReplayLauncher implements IHasForgeLog {
    private ReplayLauncher() {
    }

    /**
     * Lets the player pick one of the recorded games and plays it. Safe to call from the UI thread: the list and the
     * file reading happen on a background thread, and playback itself runs on its own.
     */
    public static void chooseAndPlay() {
        if (ReplayPlayer.isPlaying()) {
            return;
        }
        FThreads.invokeInBackgroundThread(() -> {
            // check for Mobile since Desktop creates its own CMatchUI and has no limitations
            if (GuiBase.getInterface().isMatchInProgress()) {
                SOptionPane.showMessageDialog("Finish or leave your current match before watching a replay.",
                        "Replays", FSkinProp.ICO_INFORMATION);
                return;
            }
            final List<ReplayStore.Info> replays = ReplayStore.list();
            if (replays.isEmpty()) {
                SOptionPane.showMessageDialog("No replays yet. Games you play are recorded automatically "
                        + "(Preferences: record replays). Replays from older versions cannot be played.", "Replays", FSkinProp.ICO_INFORMATION);
                return;
            }
            final ReplayStore.Info choice = SGuiChoose.oneOrNone("Watch which game?", replays);
            if (choice == null) {
                return;
            }
            // the match screen is built on the UI thread, like any other match
            FThreads.invokeInEdtNowOrLater(() -> {
                try {
                    final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
                    ReplayPlayer.play(choice.file, gui);
                } catch (final Exception e) {
                    netLog.error(e, "Could not open replay {}", choice.file);
                    FThreads.invokeInBackgroundThread(() ->
                            SOptionPane.showErrorDialog("This replay could not be opened.\n" + e.getMessage(), "Replays"));
                }
            });
        });
    }
}
