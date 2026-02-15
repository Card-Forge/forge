package forge.net;

import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Shared utilities for network test infrastructure.
 */
public final class TestUtils {

    private TestUtils() {} // Utility class

    /**
     * Format bytes in human-readable form (B, KB, MB, GB).
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Ensure FModel is initialized with HeadlessGuiDesktop for testing.
     * Thread-safe. Always ensures HeadlessGuiDesktop is active, even if another
     * test class set a different GuiBase interface (e.g. GuiDesktop).
     */
    public static synchronized void ensureFModelInitialized() {
        if (!(GuiBase.getInterface() instanceof HeadlessGuiDesktop)) {
            GuiBase.setInterface(new HeadlessGuiDesktop());
        }
        if (FModel.getPreferences() == null) {
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                return null;
            });
        }
    }
}
