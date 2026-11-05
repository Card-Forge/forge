package forge.screens.match;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.toolbox.ArenaArt;
import forge.toolbox.ArenaTheme;
import forge.util.Localizer;
import forge.view.FView;

/**
 * Two small round buttons in the top-right corner of the Arena board. The battlefield shows
 * no toolbar or log of its own: the gear opens the match tools, the scroll opens the log,
 * stack, card detail and combat tabs. Both start closed.
 */
@SuppressWarnings("serial")
public final class ArenaCornerMenu extends JComponent {
    private static final int GAP = 6;
    private static ArenaCornerMenu instance;

    private int hovered = -1;

    private ArenaCornerMenu() {
        setOpaque(false);
        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                setHovered(buttonAt(e.getX()));
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                setHovered(-1);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
                    toggle(buttonAt(e.getX()) == 0 ? EDocID.BUTTON_DOCK : EDocID.REPORT_LOG);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        setToolTipText(Localizer.getInstance().getMessage("lblArenaCornerMenu"));
    }

    /** Puts the buttons on the board, which has just been laid out, and closes the panels they open. */
    public static void install() {
        if (!ArenaTheme.isMatchBoard()) {
            return;
        }
        final JPanel content = FView.SINGLETON_INSTANCE.getPnlContent();
        if (instance == null) {
            instance = new ArenaCornerMenu();
            content.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    instance.place();
                }
            });
        }
        if (instance.getParent() != content) {
            content.add(instance);
        }
        content.setComponentZOrder(instance, 0);
        instance.place();
        setOpen(EDocID.BUTTON_DOCK, false);
        setOpen(EDocID.REPORT_LOG, false);
    }

    /** Opens the tabs, for when the game needs something picked from the stack list. */
    public static void showInfo() {
        setOpen(EDocID.REPORT_LOG, true);
    }

    private static void toggle(final EDocID doc) {
        final DragCell cell = doc.getDoc().getParentCell();
        if (cell != null) {
            setOpen(doc, !cell.isVisible());
        }
    }

    private static void setOpen(final EDocID doc, final boolean open) {
        final DragCell cell = doc.getDoc().getParentCell();
        if (cell != null) {
            cell.setVisible(open);
            if (open) {
                final JPanel content = FView.SINGLETON_INSTANCE.getPnlContent();
                content.setComponentZOrder(cell, 0);
                content.setComponentZOrder(instance, 0);
                cell.validate();
            }
            FView.SINGLETON_INSTANCE.getPnlContent().repaint();
        }
    }

    private void place() {
        final JPanel content = FView.SINGLETON_INSTANCE.getPnlContent();
        final int s = buttonSize(content.getHeight());
        final int w = 2 * s + GAP;
        setBounds(content.getWidth() - w - GAP, GAP, w, s);
        repaint();
    }

    private static int buttonSize(final int windowHeight) {
        return Math.max(24, Math.round(windowHeight * 0.03f));
    }

    private int buttonAt(final int x) {
        return x < getHeight() + GAP / 2 ? 0 : 1;
    }

    private void setHovered(final int index) {
        if (hovered != index) {
            hovered = index;
            repaint();
        }
    }

    @Override
    protected void paintComponent(final Graphics g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final int s = getHeight();
            for (int i = 0; i < 2; i++) {
                final int x = i * (s + GAP);
                final boolean hot = hovered == i;
                g.setColor(new Color(6, 8, 14, hot ? 235 : 170));
                g.fillOval(x, 0, s - 1, s - 1);
                g.setColor(new Color(255, 255, 255, hot ? 200 : 90));
                g.drawOval(x, 0, s - 1, s - 1);
                final Color icon = hot ? Color.WHITE : new Color(255, 255, 255, 150);
                final int iconSize = Math.round(s * 0.62f);
                if (i == 0) {
                    ArenaArt.paintGearIcon(g, x + s / 2, s / 2, iconSize, icon);
                } else {
                    ArenaArt.paintLogIcon(g, x + s / 2, s / 2, iconSize, icon);
                }
            }
        } finally {
            g.dispose();
        }
    }
}
