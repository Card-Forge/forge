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
 * the static {@link #vibrate} methods directly.
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
                vibrate(FPref.UI_VIBRATE_ON_POISON, Math.min(e.amount() * 200, 2000));
            }
        }
    }

    private boolean isLocalPlayer(forge.game.player.PlayerView player) {
        LobbyPlayer local = GamePlayerUtil.getGuiPlayer();
        return local != null && player.isLobbyPlayer(local);
    }

    public static void vibrate(FPref pref, int baseDeviceMs) {
        vibrate(pref, baseDeviceMs, 0);
    }

    public static void vibrate(FPref pref, int baseDeviceMs, int baseControllerMs) {
        if (baseDeviceMs <= 0 && baseControllerMs <= 0) return;
        if (!FModel.getPreferences().getPrefBoolean(pref)) return;
        int deviceIntensity = getIntensity(FPref.UI_VIBRATE_DEVICE_INTENSITY);
        if (baseDeviceMs > 0 && deviceIntensity > 0) {
            GuiBase.getInterface().vibrate(baseDeviceMs, deviceIntensity * 255 / 100);
        }
        int controllerIntensity = getIntensity(FPref.UI_VIBRATE_CONTROLLER_INTENSITY);
        if (baseControllerMs > 0 && controllerIntensity > 0) {
            GuiBase.getInterface().vibrateController(baseControllerMs, controllerIntensity / 100f);
        }
    }

    private static int getIntensity(FPref pref) {
        try {
            return FModel.getPreferences().getPrefInt(pref);
        } catch (NumberFormatException e) {
            return 100;
        }
    }
}
