package forge.menus;

import java.awt.Desktop;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import forge.localinstance.properties.ForgeConstants;
import forge.toolbox.FOptionPane;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.view.KeyboardShortcutsDialog;

import static forge.localinstance.properties.ForgeConstants.GITHUB_FORGE_URL;

public final class HelpMenu {
    private HelpMenu() { }

    public static JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        JMenu menu = new JMenu(localizer.getMessage("lblHelp"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(getMenu_GettingStarted());
        menu.add(getMenu_Troubleshooting());
        menu.add(getMenuItem_KeyboardShortcuts());
        menu.addSeparator();
        menu.add(getMenuItem_ReleaseNotes());
        menu.add(getMenuItem_License());
        menu.addSeparator();
        menu.add(getMenuItem_About());
        return menu;
    }

    private static JMenuItem getMenuItem_About() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblAboutForge")+ "...");
        menuItem.addActionListener(getAboutForgeAction());
        return menuItem;
    }

    private static ActionListener getAboutForgeAction() {
        return e -> {
            final Localizer localizer = Localizer.getInstance();
            FOptionPane.showMessageDialog(
                    "Version : " + BuildInfo.getVersionString(),
                    localizer.getMessage("lblAboutForge"));
        };
    }

    private static JMenu getMenu_Troubleshooting() {
        final Localizer localizer = Localizer.getInstance();
        JMenu mnu = new JMenu(localizer.getMessage("lblTroubleshooting"));
        mnu.add(getMenuItem_OpenLogFile());
        return mnu;
    }

    private static JMenu getMenu_GettingStarted() {
        final Localizer localizer = Localizer.getInstance();
        JMenu mnu = new JMenu(localizer.getMessage("lblGettingStarted"));
        mnu.add(getMenuItem_HowToPlayFile());
        mnu.addSeparator();
        mnu.add(getMenuItem_UrlLink("Forge Wiki", GITHUB_FORGE_URL + "wiki", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)));
        return mnu;
    }

    private static JMenuItem getMenuItem_KeyboardShortcuts() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblKeyboardShortcuts"));
        menuItem.addActionListener(e -> new KeyboardShortcutsDialog().setVisible(true));
        return menuItem;
    }

    private static JMenuItem getMenuItem_HowToPlayFile() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblHowtoPlay"));
        menuItem.addActionListener(getOpenFileAction(getFile(ForgeConstants.HOWTO_FILE)));
        return menuItem;
    }

    private static JMenuItem getMenuItem_OpenLogFile() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblOpenLogFile"));
        menuItem.addActionListener(getOpenFileAction(getAbsoluteFile(ForgeConstants.LOG_FILE)));
        return menuItem;
    }

    private static JMenuItem getMenuItem_License() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblForgeLicense"));
        menuItem.addActionListener(getOpenFileAction(getFile(ForgeConstants.LICENSE_FILE)));
        return menuItem;
    }

    private static JMenuItem getMenuItem_ReleaseNotes() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblReleaseNotes"));
        menuItem.addActionListener(getOpenFileAction(getFile(ForgeConstants.CHANGES_FILE)));
        return menuItem;
    }

    private static ActionListener getOpenFileAction(final File file) {
        return e -> {
            try {
                openFile(file);
            } catch (IOException e1) {
                // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                e1.printStackTrace();
            }
        };
    }

    protected static File getFile(String filename) {
        // !! Linux is case-sensitive so file name and extension need to match exactly !!
        File file = null;
        String filePath = FileUtil.pathCombine(System.getProperty("user.dir"), filename);
        if (FileUtil.doesFileExist(filePath)) {
            file = new File(filePath);
        }
        return file;
    }

    protected static File getAbsoluteFile(String filename) {
        File file = null;
        if (FileUtil.doesFileExist(filename)) {
            file = new File(filename);
        }
        return file;
    }

    /**
     * @see http://stackoverflow.com/questions/6273221/open-a-text-file-in-the-default-text-editor-via-java
     */
    private static void openFile(File file) throws IOException {
        if (file == null)
            return;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String cmd = "rundll32 url.dll,FileProtocolHandler " + file.getCanonicalPath();
            Runtime.getRuntime().exec(cmd);
        }
        else {
            Desktop.getDesktop().open(file);
        }
    }

    private static JMenuItem getMenuItem_UrlLink(String caption, String url) {
        JMenuItem menuItem = new JMenuItem(caption);
        menuItem.addActionListener(getLaunchUrlAction(url));
        return menuItem;
    }

    private static JMenuItem getMenuItem_UrlLink(String caption, String url, KeyStroke accelerator) {
        JMenuItem menuItem = getMenuItem_UrlLink(caption, url);
        menuItem.setAccelerator(accelerator);
        return menuItem;
    }

    private static ActionListener getLaunchUrlAction(final String url) {
        return e -> MenuUtil.openUrlInBrowser(url);
    }

}
