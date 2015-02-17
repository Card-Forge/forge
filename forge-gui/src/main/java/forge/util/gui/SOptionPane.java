package forge.util.gui;

import forge.GuiBase;
import forge.assets.FSkinProp;

public class SOptionPane {
    public static final FSkinProp QUESTION_ICON = FSkinProp.ICO_QUESTION;
    public static final FSkinProp INFORMATION_ICON = FSkinProp.ICO_INFORMATION;
    public static final FSkinProp WARNING_ICON = FSkinProp.ICO_WARNING;
    public static final FSkinProp ERROR_ICON = FSkinProp.ICO_ERROR;

    public static void showMessageDialog(String message) {
        showMessageDialog(message, "Forge", INFORMATION_ICON);
    }

    public static void showMessageDialog(String message, String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(String message) {
        showMessageDialog(message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(String message, String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(String message, String title, FSkinProp icon) {
        showOptionDialog(message, title, icon, new String[] {"OK"}, 0);
    }

    public static boolean showConfirmDialog(String message) {
        return showConfirmDialog(message, "Forge");
    }

    public static boolean showConfirmDialog(String message, String title) {
        return showConfirmDialog(message, title, "Yes", "No", true);
    }

    public static boolean showConfirmDialog(String message, String title, boolean defaultYes) {
        return showConfirmDialog(message, title, "Yes", "No", defaultYes);
    }

    public static boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes) {
        String[] options = {yesButtonText, noButtonText};
        int reply = SOptionPane.showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(String message, String title, FSkinProp icon, String[] options) {
        return showOptionDialog(message, title, icon, options, 0);
    }

    public static int showOptionDialog(String message, String title, FSkinProp icon, String[] options, int defaultOption) {
        return GuiBase.getInterface().showOptionDialog(message, title, icon, options, defaultOption);
    }

    public static String showInputDialog(String message, String title) {
        return showInputDialog(message, title, null, "", null);
    }

    public static String showInputDialog(String message, String title, FSkinProp icon) {
        return showInputDialog(message, title, icon, "", null);
    }

    public static String showInputDialog(String message, String title, FSkinProp icon, String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null);
    }

    public static String showInputDialog(String message, String title, FSkinProp icon, String initialInput, String[] inputOptions) {
        return GuiBase.getInterface().showInputDialog(message, title, icon, initialInput, inputOptions);
    }

    private SOptionPane() {
    }
}
