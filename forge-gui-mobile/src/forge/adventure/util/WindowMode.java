package forge.adventure.util;

import forge.Forge;
import forge.adventure.data.SettingData;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.OperatingSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * The desktop display modes offered in the settings menus. They're stored as the fullScreen and borderlessFullScreen
 * settings, which the launcher reads before the preferences load, and mirrored to their preferences.
 * Each mode takes effect the next time the game starts.
 */
public enum WindowMode {
    WINDOWED("lblWindowed"),
    FULLSCREEN("lblFullScreen"),
    //fills the screen while other apps and monitors can be used without the game minimizing, see GameLauncher
    BORDERLESS("lblBorderlessFullScreen");

    private final String labelKey;

    WindowMode(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel() {
        return Forge.getLocalizer().getMessage(labelKey);
    }

    /** Borderless isn't offered on macOS, where the menu bar and Dock would still cover the window. */
    public static List<WindowMode> getAvailable() {
        List<WindowMode> modes = new ArrayList<>(List.of(values()));
        if (OperatingSystem.isMac()) {
            modes.remove(BORDERLESS);
        }
        return modes;
    }

    public static String[] getAvailableLabels() {
        return getAvailable().stream().map(WindowMode::getLabel).toArray(String[]::new);
    }

    public static WindowMode getCurrent() {
        SettingData settings = Config.instance().getSettingData();
        if (!settings.fullScreen) {
            return WINDOWED;
        }
        return settings.borderlessFullScreen && !OperatingSystem.isMac() ? BORDERLESS : FULLSCREEN;
    }

    public static WindowMode fromLabel(String label) {
        for (WindowMode mode : values()) {
            if (mode.getLabel().equals(label)) {
                return mode;
            }
        }
        return null;
    }

    public void apply() {
        SettingData settings = Config.instance().getSettingData();
        settings.fullScreen = this != WINDOWED;
        settings.borderlessFullScreen = this == BORDERLESS;
        Config.instance().saveSettings();
        FModel.getPreferences().setPref(FPref.UI_FULLSCREEN_MODE, settings.fullScreen);
        FModel.getPreferences().setPref(FPref.UI_BORDERLESS_FULLSCREEN, settings.borderlessFullScreen);
        FModel.getPreferences().save();
    }
}
