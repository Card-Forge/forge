package forge.screens.match;

import java.util.Arrays;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import forge.gamemodes.net.client.FGameClient;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.localinstance.skin.FSkinProp;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;

/**
 * Dialog launchers for every reconnect state: RECONNECTING (in progress),
 * FAILED (server unreachable), SEAT_LOST (server reached but match is gone).
 */
public final class ReconnectModals {

    private ReconnectModals() {
    }

    public static ReconnectingHandle showReconnecting(final FGameClient client,
            final Runnable onViewBattlefield, final Runnable onReturnToMain) {
        FThreads.assertExecutedByEdt(true);
        final Localizer loc = Localizer.getInstance();
        final ReconnectingHandle handle = new ReconnectingHandle();
        handle.pane = new FOptionPane(
                loc.getMessage("lblReconnectBodyNoCountdown", 1, FGameClient.getTotalReconnectAttempts()),
                loc.getMessage("lblReconnectTitle"),
                FSkin.getImage(FSkinProp.ICO_WARNING),
                null,
                Arrays.asList(loc.getMessage("lblReconnectViewBattlefield"),
                        loc.getMessage("lblReconnectReturnToMain")),
                0);
        SwingUtilities.invokeLater(() -> {
            handle.pane.setVisible(true);
            handle.stopCountdown();
            final int result = handle.pane.getResult();
            handle.pane.dispose();
            if (handle.programmaticDismiss) {
                return;
            }
            if (result == 1) {
                client.cancelReconnect();
                onReturnToMain.run();
            } else {
                // result 0 (View Battlefield) or -1 (Esc / window X) — treat both as "hide, continue in background"
                onViewBattlefield.run();
            }
        });
        return handle;
    }

    public static void showFailed(final FGameClient client, final Runnable onReturnToMain) {
        final Localizer loc = Localizer.getInstance();
        final int choice = SOptionPane.showOptionDialog(
                loc.getMessage("lblReconnectFailedMessage"),
                loc.getMessage("lblReconnectFailedTitle"),
                SOptionPane.ERROR_ICON,
                Arrays.asList(loc.getMessage("lblReconnectRejoin"),
                        loc.getMessage("lblReconnectReturnToMain")),
                0);
        if (choice == 0) {
            client.rejoinManually();
        } else {
            client.beginShutdown();
            onReturnToMain.run();
        }
    }

    public static void showSeatLost(final FGameClient client, final Runnable onReturnToMain) {
        final Localizer loc = Localizer.getInstance();
        SOptionPane.showMessageDialog(
                loc.getMessage("lblReconnectSeatLostMessage"),
                loc.getMessage("lblReconnectSeatLostTitle"),
                SOptionPane.WARNING_ICON);
        client.beginShutdown();
        onReturnToMain.run();
    }

    /** Handle returned by {@link #showReconnecting}. Owns the per-second countdown. */
    public static final class ReconnectingHandle {
        private FOptionPane pane;
        private Timer countdown;
        private boolean programmaticDismiss;

        public void update(final int attemptIndex, final int totalAttempts, final int delaySeconds) {
            SwingUtilities.invokeLater(() -> {
                if (pane == null) return;
                stopCountdown();
                final int displayAttempt = attemptIndex + 1;
                if (delaySeconds <= 0) {
                    pane.setMessage(Localizer.getInstance().getMessage(
                            "lblReconnectBodyNoCountdown", displayAttempt, totalAttempts));
                    return;
                }
                final int[] remaining = { delaySeconds };
                pane.setMessage(Localizer.getInstance().getMessage(
                        "lblReconnectBody", displayAttempt, totalAttempts, remaining[0]));
                countdown = new Timer(1000, e -> {
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        stopCountdown();
                        return;
                    }
                    pane.setMessage(Localizer.getInstance().getMessage(
                            "lblReconnectBody", displayAttempt, totalAttempts, remaining[0]));
                });
                countdown.start();
            });
        }

        /** Called when a reconnect state transition (CONNECTED, FAILED, SEAT_LOST) supersedes the dialog. Caller must already be on EDT. */
        public void dismiss() {
            programmaticDismiss = true;
            stopCountdown();
            if (pane != null) {
                pane.setResult(-1);
            }
        }

        private void stopCountdown() {
            if (countdown != null) {
                countdown.stop();
                countdown = null;
            }
        }
    }
}
