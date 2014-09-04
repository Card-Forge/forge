package forge.util.gui;

import forge.assets.FSkinProp;
import forge.interfaces.IGuiBase;
import forge.view.CardView;

public class SOptionPane {
    public static final FSkinProp QUESTION_ICON = FSkinProp.ICO_QUESTION;
    public static final FSkinProp INFORMATION_ICON = FSkinProp.ICO_INFORMATION;
    public static final FSkinProp WARNING_ICON = FSkinProp.ICO_WARNING;
    public static final FSkinProp ERROR_ICON = FSkinProp.ICO_ERROR;

    public static void showMessageDialog(IGuiBase gui, String message) {
        showMessageDialog(gui, message, "Forge", INFORMATION_ICON);
    }

    public static void showMessageDialog(IGuiBase gui, String message, String title) {
        showMessageDialog(gui, message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(IGuiBase gui, String message) {
        showMessageDialog(gui, message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(IGuiBase gui, String message, String title) {
        showMessageDialog(gui, message, title, ERROR_ICON);
    }

    public static void showMessageDialog(IGuiBase gui, String message, String title, FSkinProp icon) {
        showOptionDialog(gui, message, title, icon, new String[] {"OK"}, 0);
    }

    public static boolean showConfirmDialog(IGuiBase gui, String message) {
        return showConfirmDialog(gui, message, "Forge");
    }

    public static boolean showConfirmDialog(IGuiBase gui, String message, String title) {
        return showConfirmDialog(gui, message, title, "Yes", "No", true);
    }

    public static boolean showConfirmDialog(IGuiBase gui, String message, String title, boolean defaultYes) {
        return showConfirmDialog(gui, message, title, "Yes", "No", defaultYes);
    }

    public static boolean showConfirmDialog(IGuiBase gui, String message, String title, String yesButtonText, String noButtonText) {
        return showConfirmDialog(gui, message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(IGuiBase gui, String message, String title, String yesButtonText, String noButtonText, boolean defaultYes) {
        String[] options = {yesButtonText, noButtonText};
        int reply = SOptionPane.showOptionDialog(gui, message, title, QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(IGuiBase gui, String message, String title, FSkinProp icon, String[] options) {
        return showOptionDialog(gui, message, title, icon, options, 0);
    }

    public static int showOptionDialog(IGuiBase gui, String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return gui.showOptionDialog(message, title, icon, options, defaultOption);
    }

    public static int showCardOptionDialog(IGuiBase gui, CardView card, String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return gui.showCardOptionDialog(card, message, title, icon, options, defaultOption);
    }

    public static String showInputDialog(IGuiBase gui, String message, String title) {
        return showInputDialog(gui, message, title, null, "", null);
    }

    public static String showInputDialog(IGuiBase gui, String message, String title, FSkinProp icon) {
        return showInputDialog(gui, message, title, icon, "", null);
    }

    public static String showInputDialog(IGuiBase gui, String message, String title, FSkinProp icon, String initialInput) {
        return showInputDialog(gui, message, title, icon, initialInput, null);
    }

    public static <T> T showInputDialog(IGuiBase gui, String message, String title, FSkinProp icon, T initialInput, T[] inputOptions) {
        return gui.showInputDialog(message, title, icon, initialInput, inputOptions);
    }

    private SOptionPane() {
    }
}
