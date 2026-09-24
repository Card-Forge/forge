package forge.screens.match;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.Timer;
import com.google.common.collect.ImmutableList;

import forge.gamemodes.net.client.FGameClient;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

/**
 * Modal dialog launchers for every reconnect state. Driven by
 * {@link MatchController#onReconnectStateChanged}.
 */
public final class ReconnectModals {

    private ReconnectModals() {}

    public static ReconnectingHandle showReconnecting(final FGameClient client,
            final Runnable onViewBattlefield, final Runnable onReturnToMain) {
        final Localizer loc = Localizer.getInstance();
        final ReconnectingHandle handle = new ReconnectingHandle();
        final FOptionPane pane = new FOptionPane(
                loc.getMessage("lblReconnectBodyNoCountdown", 1, FGameClient.getTotalReconnectAttempts()),
                null,
                loc.getMessage("lblReconnectTitle"),
                FOptionPane.WARNING_ICON,
                null,
                ImmutableList.of(
                        loc.getMessage("lblReconnectViewBattlefield"),
                        loc.getMessage("lblReconnectReturnToMain")),
                0,
                result -> {
                    handle.stopCountdown();
                    if (handle.programmaticDismiss) {
                        return;
                    }
                    if (result != null && result == 1) {
                        client.cancelReconnect();
                        onReturnToMain.run();
                    } else {
                        // result 0 (View Battlefield) or null/other (Esc/Back) — hide, continue in background
                        onViewBattlefield.run();
                    }
                }) {
            @Override
            public boolean keyDown(final int keyCode) {
                // Override FOptionPane's default mapping of Esc/Back to the final button (Return to Main).
                // For this dialog we want Esc/Back to behave like "View Battlefield" — dismiss silently,
                // reconnect continues in the background.
                if (keyCode == Keys.ESCAPE || keyCode == Keys.BACK || keyCode == Keys.BUTTON_B) {
                    setResult(0);
                    return true;
                }
                return super.keyDown(keyCode);
            }
        };
        handle.pane = pane;
        pane.show();
        return handle;
    }

    public static void showFailed(final FGameClient client, final Runnable onReturnToMain) {
        final Localizer loc = Localizer.getInstance();
        FOptionPane.showOptionDialog(
                loc.getMessage("lblReconnectFailedMessage"),
                loc.getMessage("lblReconnectFailedTitle"),
                FOptionPane.WARNING_ICON,
                ImmutableList.of(loc.getMessage("lblReconnectRejoin"), loc.getMessage("lblReconnectReturnToMain")),
                1, result -> {
            if (result != null && result == 0) {
                client.rejoinManually();
            } else {
                client.beginShutdown();
                onReturnToMain.run();
            }
        });
    }

    public static void showSeatLost(final FGameClient client, final Runnable onReturnToMain) {
        final Localizer loc = Localizer.getInstance();
        FOptionPane.showMessageDialog(
                loc.getMessage("lblReconnectSeatLostMessage"),
                loc.getMessage("lblReconnectSeatLostTitle"),
                FOptionPane.WARNING_ICON,
                result -> {
                    client.beginShutdown();
                    onReturnToMain.run();
                });
    }

    /** Handle returned by {@link #showReconnecting}. Owns the per-second countdown. */
    public static final class ReconnectingHandle {
        private FOptionPane pane;
        private Timer.Task countdown;
        private boolean programmaticDismiss;

        public void update(final int attemptIndex, final int totalAttempts, final int delaySeconds) {
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
            countdown = Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        cancel();
                        countdown = null;
                        return;
                    }
                    final int snapshot = remaining[0];
                    Gdx.app.postRunnable(() -> pane.setMessage(Localizer.getInstance().getMessage(
                            "lblReconnectBody", displayAttempt, totalAttempts, snapshot)));
                }
            }, 1f, 1f);
        }

        /** Called when a reconnect state transition (CONNECTED, FAILED, SEAT_LOST) supersedes the dialog. */
        public void dismiss() {
            programmaticDismiss = true;
            stopCountdown();
            if (pane != null) {
                pane.hide();
            }
        }

        private void stopCountdown() {
            if (countdown != null) {
                countdown.cancel();
                countdown = null;
            }
        }
    }
}
