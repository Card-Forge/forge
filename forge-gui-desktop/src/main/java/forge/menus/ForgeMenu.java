package forge.menus;

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
import forge.gui.GuiUtils;
import forge.screens.home.online.OnlineMenu;
import forge.util.Localizer;
import forge.util.ReflectionUtil;


public final class ForgeMenu {

    private JPopupMenu popupMenu;
    private IMenuProvider provider;
    private static HashMap<KeyStroke, JMenuItem> activeShortcuts = new HashMap<>();

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
        add(new LayoutMenu().getMenu());
        add(new AudioMenu().getMenu());
        add(HelpMenu.getMenu());
        addSeparator();
        add(OnlineMenu.getMenu());
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

        GuiUtils.setMenuItemSize(item);

        KeyStroke shortcut = item.getAccelerator();
        if (shortcut != null) {
            activeShortcuts.put(shortcut, item);
        }

        JMenu subMenu = ReflectionUtil.safeCast(item, JMenu.class);
        if (subMenu != null) {
            setupMenu(subMenu);
        }
    }
    
    public boolean handleKeyEvent(KeyEvent e) {
        if (popupMenu.isEnabled()) {
            JMenuItem item = activeShortcuts.get(KeyStroke.getKeyStrokeForEvent(e));
            if (item != null) {
                hide(); //ensure menu doesn't stay open if currently open
                item.doClick();
                return true;
            }
        }
        return false;
    }

    private static JMenuItem getMenuItem_Restart() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblRestart"));
        menuItem.setMnemonic(KeyEvent.VK_R);
        menuItem.addActionListener(getRestartAction());
        return menuItem;
    }

    private static ActionListener getRestartAction() {
        return e -> Singletons.getControl().restartForge();
    }

    private static JMenuItem getMenuItem_Exit() {
        final Localizer localizer = Localizer.getInstance();
        JMenuItem menuItem = new JMenuItem(localizer.getMessage("lblExit"));
        menuItem.setMnemonic(KeyEvent.VK_X);
        menuItem.addActionListener(getExitAction());
        return menuItem;
    }

    private static ActionListener getExitAction() {
        return e -> Singletons.getControl().exitForge();
    }
}
