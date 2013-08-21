package forge.gui.menubar;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import forge.gui.menus.ForgeMenu;
import forge.gui.menus.HelpMenu;

@SuppressWarnings("serial")
public class FMenuBar extends JMenuBar {

    public FMenuBar(JFrame f) {
        f.setJMenuBar(this);
        setPreferredSize(new Dimension(f.getWidth(), 26));
        setupMenuBar(null);        
    }
    
    public void setupMenuBar(IMenuProvider provider) {        
        removeAll();
        add(ForgeMenu.getMenu());
        addProviderMenus(provider);
        add(HelpMenu.getMenu());        
        repaint();        
    }
    
    private void addProviderMenus(IMenuProvider provider) { 
        if (provider != null && provider.getMenus() != null) {
            for (JMenu m : provider.getMenus()) {                
                m.setBorderPainted(false);
                add(m); 
            }
        }        
    }

    /**
     * Enables or disables the MenuBar.
     * <p>
     * Note: Disabling a component does not disable its children.
     */
    public void setMenuBarEnabled(boolean isEnabled) {
        setEnabled(isEnabled);
        for (Component c : getComponents()) {
            c.setEnabled(isEnabled);
        }
    }
        
}
