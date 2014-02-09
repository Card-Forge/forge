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

    /**
     * The only reliable way to ensure the mouse cursor is set properly in Forge.
     * 
     * @param mouseCursor one of the predefined {@code Cursor} types.
     */
    public static void setMouseCursor(MouseCursor cursor) {
        FView.SINGLETON_INSTANCE.getLpnDocument().setCursor(Cursor.getPredefinedCursor(cursor.toInt()));
    }
}
