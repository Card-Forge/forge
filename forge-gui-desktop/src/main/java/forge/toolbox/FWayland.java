package forge.toolbox;

import java.awt.Toolkit;
import java.awt.Window;

import com.jetbrains.JBR;

/**
 * Native Wayland support provided by JetBrains Runtime (JBR).
 *
 * Wayland has no absolute window coordinates, so {@link Window#setLocation(int, int)} is a no-op
 * there. JBR provides a compositor-driven move session instead. All methods must remain safe when
 * running on a stock JDK/X11 toolkit without JBR classes.
 */
public final class FWayland {
    private static final boolean ACTIVE = Toolkit.getDefaultToolkit().getClass().getName().contains("WLToolkit");

    private FWayland() {
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    /**
     * Starts a compositor-handled window move from an active mouse press. Must be called while the
     * mouse button is held down and the pointer is still inside the window.
     *
     * @return whether the move session was started
     */
    public static boolean startMove(final Window window, final int mouseButton) {
        if (!ACTIVE) { return false; }
        try {
            if (!JBR.isWindowMoveSupported()) { return false; }
            JBR.getWindowMove().startMovingTogetherWithMouse(window, mouseButton);
            return true;
        }
        catch (final LinkageError | RuntimeException e) { //stock JDK or JBR without the service
            return false;
        }
    }
}
