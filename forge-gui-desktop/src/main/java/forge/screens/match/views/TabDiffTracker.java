package forge.screens.match.views;

import javax.swing.Timer;

import forge.gui.framework.DragTab;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Tracks the change in a tab's item count between updates and briefly pulses
 * the tab red when the count increases while the tab is hidden. Baseline
 * resets when the tab becomes visible (the user is watching it).
 *
 * <p>Returns the delta to display in the tab label, or 0 when the
 * "New Card Count in Tabs" preference is off.
 */
final class TabDiffTracker {
    private static final int FLASH_STEPS = 5;
    private static final int FLASH_DELAY_MS = 80;

    private int baseCount = -1;
    private int lastDelta = 0;
    private Timer flashTimer;

    int update(final int count, final DragTab tab) {
        if (baseCount < 0 || tab.isSelected()) {
            baseCount = count;
            lastDelta = 0;
        }
        final int delta = count - baseCount;
        final boolean prefOn = FModel.getPreferences().getPrefBoolean(FPref.UI_ZONE_TAB_NEW_COUNT);
        if (prefOn && delta > lastDelta) {
            startFlash(tab);
        }
        lastDelta = delta;
        return prefOn ? delta : 0;
    }

    private void startFlash(final DragTab tab) {
        if (flashTimer != null && flashTimer.isRunning()) {
            flashTimer.stop();
        }
        final int[] counter = {0};
        flashTimer = new Timer(FLASH_DELAY_MS, e -> {
            counter[0]++;
            final float halfSteps = FLASH_STEPS / 2f;
            final float intensity = counter[0] <= halfSteps
                    ? counter[0] / halfSteps
                    : 1f - (counter[0] - halfSteps) / halfSteps;
            tab.setFlashIntensity(intensity);
            if (counter[0] >= FLASH_STEPS) {
                tab.setFlashIntensity(0f);
                flashTimer.stop();
            }
        });
        flashTimer.setInitialDelay(0);
        flashTimer.start();
    }
}
