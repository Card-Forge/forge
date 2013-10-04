package forge.gui.menus;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import forge.Singletons;
import forge.control.RestartUtil;
import forge.control.FControl.Screens;
import forge.util.TypeUtil;

public final class ForgeMenu {
    private static final int minItemWidth = 100;
    private static final int itemHeight = 25;

    private JPopupMenu popupMenu;
    private IMenuProvider provider;
    private static HashMap<KeyStroke, JMenuItem> activeShortcuts = new HashMap<KeyStroke, JMenuItem>();

    public ForgeMenu() {
        refresh();
    }
    
    public void show() {
        show(false);
    }
    
    public void show(boolean hideIfAlreadyShown) {
        Singletons.getView().getNavigationBar().showForgeMenu(hideIfAlreadyShown);
    }
    
    public void hide() {
        popupMenu.setVisible(false);
    }
    
    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void setProvider(IMenuProvider provider0) {
        provider = provider0;
        refresh();
    }
    
    public void refresh() {
        activeShortcuts.clear();
        popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                Singletons.getView().getNavigationBar().onForgeMenuHidden();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {}
        });
        if (provider != null) {
            List<JMenu> menus = provider.getMenus();
            if (menus != null) {
                for (JMenu m : menus) {
                    m.setBorderPainted(false);
                    add(m);
                }
            }
        }
        add(LayoutMenu.getMenu());
        add(HelpMenu.getMenu());
        addSeparator();
        add(getMenuItem_Restart());
        add(getMenuItem_Exit());
    }
    
    public void add(JMenuItem item) {
        item = popupMenu.add(item);
        setupItem(item);
    }
    
    public void addSeparator() {
        popupMenu.addSeparator();
    }
    
    private void setupMenu(JMenu menu) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            setupItem(menu.getItem(i));
        }
    }
    
    private void setupItem(JMenuItem item) {
        if (item == null) { return; }

        item.setPreferredSize(new Dimension(Math.max(item.getPreferredSize().width, minItemWidth), itemHeight));
        
        KeyStroke shortcut = item.getAccelerator();
        if (shortcut != null) {
            activeShortcuts.put(shortcut, item);
        }

        JMenu subMenu = TypeUtil.safeCast(item, JMenu.class);
        if (subMenu != null) {
            setupMenu(subMenu);
        }
    }
    
    public boolean handleKeyEvent(KeyEvent e) {
        JMenuItem item = activeShortcuts.get(KeyStroke.getKeyStrokeForEvent(e));
        if (item != null) {
            hide(); //ensure menu doesn't stay open if currently open
            item.doClick();
            return true;
        }
        return false;
    }

    private static JMenuItem getMenuItem_Restart() {
        JMenuItem menuItem = new JMenuItem("Restart");
        menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.addActionListener(getRestartAction());
        return menuItem;        
    }
    
    private static ActionListener getRestartAction() {
        return new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isHomeScreenActive()) {
                    String userPrompt = "Please confirm you want to restart Forge.\n\n";
                    if (!MenuUtil.getUserConfirmation(userPrompt, "Restart Forge")) {
                        return;
                    }
                }
                RestartUtil.restartApplication(null);                
            }
        };
    }
    
    private static JMenuItem getMenuItem_Exit() {
        JMenuItem menuItem = new JMenuItem("Exit");
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.addActionListener(getExitAction());
        return menuItem;
    }
    
    private static ActionListener getExitAction() {
        return new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isHomeScreenActive()) {                    
                    String userPrompt = "Please confirm you want to close Forge.\n\n";
                    if (!MenuUtil.getUserConfirmation(userPrompt, "Exit Forge")) {
                        return;
                    }
                }
                System.exit(0);                
            }
        };
    }
    
    private static boolean isHomeScreenActive() {
        return Singletons.getControl().getState() == Screens.HOME_SCREEN;
    }
}
