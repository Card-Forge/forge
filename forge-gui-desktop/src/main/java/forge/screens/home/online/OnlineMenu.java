package forge.screens.home.online;

import forge.gui.FNetOverlay;
import forge.util.Localizer;
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Returns a JMenu containing options for online play.
 */
public final class OnlineMenu {

    public static JMenu getMenu() {
        JMenu menu = new JMenu(Localizer.getInstance().getMessage("lblOnline"));
        menu.setMnemonic(KeyEvent.VK_O);
        menu.add(getMenuItem_ConnectToServer());
        menu.add(new JSeparator());
        menu.add(chatItem);
        return menu;
    }

    public static final JCheckBoxMenuItem chatItem;

    static {
        chatItem = new JCheckBoxMenuItem(Localizer.getInstance().getMessage("lblShowChatPanel"));
        chatItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((JMenuItem)e.getSource()).isSelected()) {
                    FNetOverlay.SINGLETON_INSTANCE.show();
                }
                else {
                    FNetOverlay.SINGLETON_INSTANCE.hide();
                }
            }
        });
    }

    private static JMenuItem getMenuItem_ConnectToServer() {
        JMenuItem menuItem = new JMenuItem(Localizer.getInstance().getMessage("lblConnectToServer"));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CSubmenuOnlineLobby.SINGLETON_INSTANCE.connectToServer();
            }
        });
        return menuItem;
    }
}