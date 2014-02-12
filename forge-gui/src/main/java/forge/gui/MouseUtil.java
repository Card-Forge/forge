package forge.gui;

import forge.view.FView;

import java.awt.*;

public final class MouseUtil {
    private MouseUtil() { }

    // Add existing Cursor values as needed.
    public enum MouseCursor {
        WAIT_CURSOR (Cursor.WAIT_CURSOR),
        DEFAULT_CURSOR (Cursor.DEFAULT_CURSOR),
        HAND_CURSOR (Cursor.HAND_CURSOR);
        // Scaffolding...
        private int value;
        private MouseCursor(int value) { this.value = value; }
        public int toInt() { return value; }
        public static MouseCursor fromInt(int value){
            for (final MouseCursor c : MouseCursor.values()) {
                if (c.value == value) {
                    return c;
                }
            }
            throw new IllegalArgumentException("No Enum specified for this int");
        }
    };
    
    private static Cursor cursor;
    private static int cursorLockCount;

    /**
     * Lock cursor as it is currently displayed until unlockCursor called
     */
    public static void lockCursor() {
        cursorLockCount++;
    }
    public static void unlockCursor() {
        if (cursorLockCount == 0) { return; }
        if (--cursorLockCount == 0) {
            //update displayed cursor after cursor unlocked
            FView.SINGLETON_INSTANCE.getLpnDocument().setCursor(cursor);
        }
    }

    /**
     * The only reliable way to ensure the mouse cursor is set properly in Forge.
     * 
     * @param mouseCursor one of the predefined {@code Cursor} types.
     */
    public static void setMouseCursor(MouseCursor cursor0) {
        setCursor(Cursor.getPredefinedCursor(cursor0.toInt()));
    }
    public static void setCursor(Cursor cursor0) {
        if (cursor == cursor0) { return; }
        cursor = cursor0;
        if (cursorLockCount > 0) { return; }
        FView.SINGLETON_INSTANCE.getLpnDocument().setCursor(cursor);
    }
}
