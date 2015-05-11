package forge.util.gui;

import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.GuiBase;
import forge.assets.FSkinProp;

public class SOptionPane {
    public static final FSkinProp QUESTION_ICON = FSkinProp.ICO_QUESTION;
    public static final FSkinProp INFORMATION_ICON = FSkinProp.ICO_INFORMATION;
    public static final FSkinProp WARNING_ICON = FSkinProp.ICO_WARNING;
    public static final FSkinProp ERROR_ICON = FSkinProp.ICO_ERROR;

    public static void showMessageDialog(final String message) {
        showMessageDialog(message, "Forge", INFORMATION_ICON);
    }

    public static void showMessageDialog(final String message, final String title) {
        showMessageDialog(message, title, INFORMATION_ICON);
    }

    public static void showErrorDialog(final String message) {
        showMessageDialog(message, "Forge", ERROR_ICON);
    }

    public static void showErrorDialog(final String message, final String title) {
        showMessageDialog(message, title, ERROR_ICON);
    }

    public static void showMessageDialog(final String message, final String title, final FSkinProp icon) {
        showOptionDialog(message, title, icon, ImmutableList.of("OK"), 0);
    }

    public static boolean showConfirmDialog(final String message) {
        return showConfirmDialog(message, "Forge");
    }

    public static boolean showConfirmDialog(final String message, final String title) {
        return showConfirmDialog(message, title, "Yes", "No", true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final boolean defaultYes) {
        return showConfirmDialog(message, title, "Yes", "No", defaultYes);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText) {
        return showConfirmDialog(message, title, yesButtonText, noButtonText, true);
    }

    public static boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        final List<String> options = ImmutableList.of(yesButtonText, noButtonText);
        final int reply = SOptionPane.showOptionDialog(message, title, QUESTION_ICON, options, defaultYes ? 0 : 1);
        return (reply == 0);
    }

    public static int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options) {
        return showOptionDialog(message, title, icon, options, 0);
    }

    public static int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return GuiBase.getInterface().showOptionDialog(message, title, icon, options, defaultOption);
    }

    public static String showInputDialog(final String message, final String title) {
        return showInputDialog(message, title, null, "", null);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon) {
        return showInputDialog(message, title, icon, "", null);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput) {
        return showInputDialog(message, title, icon, initialInput, null);
    }

    public static String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions) {
        return GuiBase.getInterface().showInputDialog(message, title, icon, initialInput, inputOptions);
    }

    private SOptionPane() {
    }
}
