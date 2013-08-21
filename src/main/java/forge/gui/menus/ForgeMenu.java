package forge.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import forge.Singletons;
import forge.control.RestartUtil;
import forge.control.FControl.Screens;
import forge.gui.menubar.MenuUtil;

public final class ForgeMenu {
    private ForgeMenu() { }
    
    public static JMenu getMenu() {        
        JMenu menu = new JMenu("Forge");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(getMenuItem_Restart());
        menu.add(getMenuItem_Exit());                
        return menu;        
    }
    
    private static JMenuItem getMenuItem_Exit() {
        JMenuItem menuItem = new JMenuItem("Exit");
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

    private static JMenuItem getMenuItem_Restart() {
        JMenuItem menuItem = new JMenuItem("Restart");        
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
    
    private static boolean isHomeScreenActive() {
        return Singletons.getControl().getState() == Screens.HOME_SCREEN;
    }
    
}
