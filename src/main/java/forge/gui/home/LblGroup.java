package forge.gui.home;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import forge.Singletons;
import forge.gui.framework.ILocalRepaint;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences.FPref;

/** 
 * Custom JLabel for title of menu item groups.
 * Handles repainting and listening for hover and click events.
 */
@SuppressWarnings("serial")
public class LblGroup extends JLabel implements ILocalRepaint {
    private boolean hovered = false;
    private boolean collapsed = true;

    private final Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
    private final Color l20 = FSkin.stepColor(clrTheme, 20);
    private final Color l25 = FSkin.stepColor(clrTheme, 25);
    private final Color l40 = FSkin.stepColor(clrTheme, 40);
    private final Color d20 = FSkin.stepColor(clrTheme, -20);
    private final Color d80 = FSkin.stepColor(clrTheme, -80);

    /** 
     * Custom JLabel for title of menu item groups.
     * Handles repainting and listening for hover and click events.
     * 
     * @param e0 {@link forge.gui.home.EMenuGroup}
     */
    public LblGroup(final EMenuGroup e0) {
        super("    + " + e0.getTitle());
        this.setFont(FSkin.getBoldFont(16));
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
     * @param e0 {@link forge.gui.home.EMenuGroup}
     */
    public void groupClick(final EMenuGroup e0) {
        final Component[] menuObjects = this.getParent().getComponents();

        // Toggle label items in this group
        for (final Component c : menuObjects) {
            if (c.getName() != null && c.getName().equals(e0.toString())) {
                if (c.isVisible()) {
                    c.setVisible(false);
                    Singletons.getModel().getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "false");
                }
                else {
                    c.setVisible(true);
                    Singletons.getModel().getPreferences().setPref(
                            FPref.valueOf("SUBMENU_" + e0.toString()), "true");
                }

                Singletons.getModel().getPreferences().save();
                break;
            }
        }

        if (this.collapsed) {
            this.collapsed = false;
            this.setText("    - " + e0.getTitle());
        }
        else {
            this.collapsed = true;
            this.setText("    + " + e0.getTitle());
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

            g.setColor(l20);
            g.fillRect(0, 0, getWidth(), getHeight());

            GradientPaint edge = new GradientPaint(w - 10, 0, l25, w, 0, d80, false);
            g2d.setPaint(edge);
            g2d.fillRect(w - 10, 0, w, h);

            g2d.setColor(l40);
            g2d.drawLine(0, 0, w - 6, 0);
            g2d.setColor(d20);
            g2d.drawLine(0, h - 1, w - 6, h - 1);
        }

        super.paintComponent(g);
        g2d.dispose();
    }
}
