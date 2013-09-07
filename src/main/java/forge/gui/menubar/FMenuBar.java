package forge.gui.menubar;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import forge.gui.menus.ForgeMenu;
import forge.gui.menus.HelpMenu;
import forge.gui.menus.LayoutMenu;
import forge.gui.toolbox.FSkin;

@SuppressWarnings("serial")
public class FMenuBar extends JMenuBar {

    private JLabel statusCaption;
    private IMenuProvider provider;

    public FMenuBar(JFrame f) {
        f.setJMenuBar(this);
        setPreferredSize(new Dimension(f.getWidth(), 26));
        refresh();
    }

    public void setupMenuBar(IMenuProvider provider0) {
        this.provider = provider0;
        refresh();
    }
    
    public void refresh() {
        removeAll();
        add(ForgeMenu.getMenu());
        addProviderMenus();
        add(LayoutMenu.getMenu());
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

    private void addProviderMenus() {
        if (provider != null) {
            List<JMenu> menus = provider.getMenus();
            if (menus != null) {
                for (JMenu m : menus) {
                    m.setBorderPainted(false);
                    add(m);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) {
            c.setEnabled(enabled);
        }
    }

}
