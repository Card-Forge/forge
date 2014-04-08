package forge.screens.home;

import forge.gui.framework.ILocalRepaint;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinnedLabel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 
 * Custom JLabel for title of menu item groups.
 * Handles repainting and listening for hover and click events.
 */
@SuppressWarnings("serial")
public class LblGroup extends SkinnedLabel implements ILocalRepaint {
    private static final boolean isCompactMenu = FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_MAIN_MENU);
    private static EMenuGroup activeMenuGroup = null;
        
    private boolean hovered = false;

    private final SkinColor clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final SkinColor l20 = clrTheme.stepColor(20);
    private final SkinColor l25 = clrTheme.stepColor(25);
    private final SkinColor l40 = clrTheme.stepColor(40);
    private final SkinColor d20 = clrTheme.stepColor(-20);
    private final SkinColor d80 = clrTheme.stepColor(-80);

    /** 
     * Custom JLabel for title of menu item groups.
     * Handles repainting and listening for hover and click events.
     * 
     * @param e0 {@link forge.screens.home.EMenuGroup}
     */
    public LblGroup(final EMenuGroup e0) {
        super("  " + e0.getTitle());

        this.setFont(FSkin.getBoldFont(14));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent evt) {
                groupClick(e0);
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                hovered = true;
                repaintSelf();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                hovered = false;
                repaintSelf();
            }
        });
    }

    /**
     * Handler for click on group title.
     * @param menuGroup {@link forge.screens.home.EMenuGroup}
     */
    public void groupClick(final EMenuGroup menuGroup) {
        toggleMenuGroupCollapseState(menuGroup);
    }
    
    private void toggleMenuGroupCollapseState(final EMenuGroup menuGroup) {
        if (isCompactMenu) {
            if (menuGroup != activeMenuGroup) {
                if (activeMenuGroup != null) {
                    setMenuGroupCollapseState(activeMenuGroup);
                }
                setMenuGroupCollapseState(menuGroup);       
                activeMenuGroup = menuGroup;
            }
        } else {
            setMenuGroupCollapseState(menuGroup);
            activeMenuGroup = menuGroup;
        }        
    }
    
    private void setMenuGroupCollapseState(final EMenuGroup e0) {
        final Component[] menuObjects = this.getParent().getComponents();

        // Toggle label items in this group
        for (final Component c : menuObjects) {
            if (c.getName() != null && c.getName().equals(e0.toString())) {
                if (c.isVisible()) {
                    c.setVisible(false);
                    FModel.getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "false");
                }
                else {
                    c.setVisible(true);
                    FModel.getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "true");
                }

                FModel.getPreferences().save();
                break;
            }
        }
    }

    @Override
    public void repaintSelf() {
        final Dimension d = this.getSize();
        repaint(0, 0, d.width, d.height);
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g.create();

        if (this.hovered) {
            int w = getWidth();
            int h = getHeight();

            FSkin.setGraphicsColor(g, l20);
            g.fillRect(0, 0, getWidth(), getHeight());

            FSkin.setGraphicsGradientPaint(g2d, w - 10, 0, l25, w, 0, d80);
            g2d.fillRect(w - 10, 0, w, h);

            FSkin.setGraphicsColor(g, l40);
            g2d.drawLine(0, 0, w - 6, 0);
            FSkin.setGraphicsColor(g, d20);
            g2d.drawLine(0, h - 1, w - 6, h - 1);
        }

        super.paintComponent(g);
        g2d.dispose();
    }
}
