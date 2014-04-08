package forge.gui;

import forge.view.FView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class MouseUtil {
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
    public static void resetCursor() {
        setCursor(Cursor.getDefaultCursor());
    }
    public static void setCursor(int cursorType) {
        setCursor(Cursor.getPredefinedCursor(cursorType));
    }
    public static void setCursor(Cursor cursor0) {
        if (cursor == cursor0) { return; }
        cursor = cursor0;
        if (cursorLockCount > 0) { return; }
        FView.SINGLETON_INSTANCE.getLpnDocument().setCursor(cursor);
    }

    public static void setComponentCursor(final Component comp, final int cursorType) {
        setComponentCursor(comp, Cursor.getPredefinedCursor(cursorType));
    }
    public static void setComponentCursor(final Component comp, final Cursor cursor0) {
        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(cursor0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                resetCursor();
            }
        });
    }
}
