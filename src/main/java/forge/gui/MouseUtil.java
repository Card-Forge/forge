package forge.gui;

import java.awt.Cursor;

import forge.view.FView;

public final class MouseUtil {
    private MouseUtil() { }

    // Add existing Cursor values as needed.
    public enum MouseCursor {
        DEFAULT_CURSOR (Cursor.DEFAULT_CURSOR),
        HAND_CURSOR (Cursor.HAND_CURSOR);
        // Scaffolding...
        private int value;
        private MouseCursor(int value) { this.value = value; }
        public int getValue() { return value; }
    };

    /**
     * The only reliable way to ensure the mouse cursor is set properly in Forge.
     * 
     * @param mouseCursor one of the predefined {@code Cursor} types.
     */
    public static void setMouseCursor(MouseCursor cursor) {
        FView.SINGLETON_INSTANCE.getLpnDocument().setCursor(Cursor.getPredefinedCursor(cursor.getValue()));
    }

}
