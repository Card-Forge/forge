package forge.screens.home.online;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import forge.gui.FNetOverlay;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options for online play.
 */
public final class OnlineMenu {

    public static JMenu getMenu() {
        JMenu menu = new JMenu(Localizer.getInstance().getMessage("lblOnline"));
        menu.setMnemonic(KeyEvent.VK_O);
        menu.add(getMenuItem_ConnectToServer());
        menu.add(getMenuItem_OpenNetworkLogs());
        menu.add(chatItem);
        return menu;
    }

    public static final JCheckBoxMenuItem chatItem;

    static {
        chatItem = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblShowChatPanel"));
        chatItem.addActionListener(e -> {
            if (((JMenuItem)e.getSource()).isSelected()) {
                FNetOverlay.SINGLETON_INSTANCE.show();
            }
            else {
                FNetOverlay.SINGLETON_INSTANCE.hide();
            }
        });
    }

    private static JMenuItem getMenuItem_ConnectToServer() {
        JMenuItem menuItem = new JMenuItem(Localizer.getInstance().getMessage("lblConnectToServer"));
        menuItem.addActionListener(e -> CSubmenuOnlineLobby.SINGLETON_INSTANCE.connectToServer());
        return menuItem;
    }

    private static JMenuItem getMenuItem_OpenNetworkLogs() {
        JMenuItem menuItem = new JMenuItem(Localizer.getInstance().getMessage("lblOpenNetworkLogs"));
        menuItem.addActionListener(e -> {
            File dir = new File(ForgeConstants.NETWORK_LOGS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try {
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + dir.getCanonicalPath());
                } else {
                    Desktop.getDesktop().open(dir);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        return menuItem;
    }
}