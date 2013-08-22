package forge.gui.menubar;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import forge.gui.menus.ForgeMenu;
import forge.gui.menus.HelpMenu;
import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class FMenuBar extends JMenuBar {

    JLabel statusCaption;

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
        setStatusCaption();
        repaint();
    }

    /**
     * Adds a label to the right-hand side of the MenuBar which can
     * be used to show hints or status information.
     */
    private void setStatusCaption() {
        add(Box.createHorizontalGlue()); // align right hack/patch.
        statusCaption = new JLabel();
        statusCaption.setForeground(getForeground());
        statusCaption.setFont(FSkin.getItalicFont(11));
        statusCaption.setOpaque(false);
        add(statusCaption);
    }

    public void setStatusText(String text) {
        statusCaption.setText(text.trim() + "  ");
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
