package forge.gui;

import java.awt.Font;

/**
 * A replacement FontConstants to allow backward-compatibility with JRE 1.5
 */

public class ForgeFontConstants {
    public static final String DIALOG, DIALOG_INPUT, MONOSPACED, SANS_SERIF, SERIF;

    static {
        String dialog = "Dialog";
        String dialogInput = "DialogInput";
        String monospaced = "Monospaced";
        String sansSerif = "SansSerif";
        String serif = "Serif";
        try {
            dialog = Font.DIALOG;
            dialogInput = Font.DIALOG_INPUT;
            monospaced = Font.MONOSPACED;
            sansSerif = Font.SANS_SERIF;
            serif = Font.SERIF;
        } catch (NoSuchFieldError ignored) {
        }

        DIALOG = dialog;
        DIALOG_INPUT = dialogInput;
        MONOSPACED = monospaced;
        SANS_SERIF = sansSerif;
        SERIF = serif;
    }
}
