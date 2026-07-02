package forge.haptic;

import com.google.common.eventbus.Subscribe;
import forge.LobbyPlayer;
import forge.game.event.GameEvent;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventPlayerPoisoned;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

/**
 * Centralized vibration with preference toggles and intensity scaling.
 * Match events are received via EventBus; adventure/UI events call
 * the static {@link #vibrate} method directly.
 *
 * Vibration is routed to either the device or a connected controller
 * based on the most recent input type, never both at once.
 */
public final class HapticEngine {

    public static final HapticEngine instance = new HapticEngine();

    private HapticEngine() {}

    @Subscribe
    public void receiveEvent(final GameEvent evt) {
        if (evt instanceof GameEventPlayerLivesChanged e) {
            if (e.newLives() < e.oldLives() && isLocalPlayer(e.player())) {
                int lifeLost = e.oldLives() - e.newLives();
                vibrate(FPref.UI_VIBRATE_ON_LIFE_LOSS, Math.min(lifeLost * 100, 2000));
            }
        } else if (evt instanceof GameEventPlayerPoisoned e) {
            if (isLocalPlayer(e.receiver())) {
                vibrate(FPref.UI_VIBRATE_ON_LIFE_LOSS, Math.min(e.amount() * 200, 2000));
            }
        }
    }

    private boolean isLocalPlayer(forge.game.player.PlayerView player) {
        LobbyPlayer local = GamePlayerUtil.getGuiPlayer();
        return local != null && player.isLobbyPlayer(local);
    }

    public static void vibrate(FPref pref, int durationMs) {
        if (durationMs <= 0) return;
        if (!FModel.getPreferences().getPrefBoolean(pref)) return;
        int intensity = FModel.getPreferences().getPrefInt(FPref.UI_VIBRATE_INTENSITY);
        if (intensity <= 0) return;

        if (GuiBase.getInterface().useControllerForHaptics()) {
            GuiBase.getInterface().vibrateController(durationMs, intensity / 100f);
        } else {
            GuiBase.getInterface().vibrate(durationMs, intensity * 255 / 100);
        }
    }

}
