package forge.view.toolbox;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class FVerticalTabPanel extends FPanel {
    private CardLayout cards;
    private JPanel pnlContent;
    private List<VTab> allVTabs;

    private FSkin skin;
    private int active;
    private Color activeColor, inactiveColor, hoverColor;
    private Border inactiveBorder, hoverBorder;

    /**
     * Assembles vertical tab panel from list of child panels.  Tooltip
     * on tab is same as tooltip on child panel.  Title of tab is same
     * as name of child panel.
     * 
     * @param childPanels &emsp; JPanels to be placed  in tabber
     */
    public FVerticalTabPanel(List<JPanel> childPanels) {
        // General inits and skin settings
        super();
        setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        setOpaque(false);
        int size = childPanels.size();
        skin = AllZone.getSkin();
        hoverColor = skin.getClrHover();
        activeColor = skin.getClrActive();
        inactiveColor = skin.getClrInactive();
        hoverBorder = new MatteBorder(1, 0, 1, 1, skin.getClrBorders());
        inactiveBorder = new MatteBorder(1, 0, 1, 1, new Color(0, 0, 0, 0));

        final int pctTabH = (int) ((100 - 2 - 2) / size);
        final int pctTabW = 11;
        final int pctInsetH = 3;
        final int pctSpacing = 1;

        // Content panel and card layout inits
        cards = new CardLayout();
        pnlContent = new JPanel();
        pnlContent.setOpaque(false);
        pnlContent.setLayout(cards);
        pnlContent.setBorder(new MatteBorder(0, 0, 0, 1, skin.getClrBorders()));

        add(pnlContent, "span 1 " + (size + 2) + ", w " + (100 - pctTabW) + "%!, h 100%!");

        JPanel topSpacer = new JPanel();
        topSpacer.setOpaque(false);
        add(topSpacer, "w " + pctTabW + "%!, h " + pctInsetH + "%!");

        // Add all tabs
        VTab tab;
        allVTabs = new ArrayList<VTab>();

        for (int i = 0; i < size; i++) {
            tab = new VTab(childPanels.get(i).getName(), i);
            tab.setToolTipText(childPanels.get(i).getToolTipText());

            if (i == 0) {
                tab.setBackground(activeColor);
                active = 0;
            }
            else {
                tab.setBackground(inactiveColor);
            }

            add(tab, "w " + pctTabW + "%!, h " + (pctTabH - pctSpacing) + "%!, gapbottom " + pctSpacing + "%!");
            allVTabs.add(tab);

            // Add card to content panel
            pnlContent.add(childPanels.get(i), "CARD" + i);
        }

        JPanel bottomSpacer = new JPanel();
        bottomSpacer.setOpaque(false);
        add(bottomSpacer, "w 10%!, h " + (pctInsetH + pctSpacing) + "%!");
    }

    /**
     * Programatically flips tab layout to specified number (without needing
     * a mouse event).
     * 
     * @param index &emsp; Tab number, starting from 0
     */
    public void showTab(int index) {
        if (index >= this.allVTabs.size()) {
            return;
        }

        allVTabs.get(active).setBackground(inactiveColor);
        active = index;
        cards.show(pnlContent, "CARD" + index);
        allVTabs.get(active).setBackground(activeColor);
    }

    /** @return JPanel */
    public JPanel getContentPanel() {
        return pnlContent;
    }

    /**
     * A single instance of a vertical tab, with paintComponent overridden
     * to provide vertical-ness.  Also manages root level hover and click effects.
     *
     */
    private class VTab extends JPanel {
        private String msg;
        private int id, w;

        // ID is used to retrieve this tab from the list of allVTabs.
        VTab(String txt, int i) {
            super();
            setLayout(new MigLayout("insets 0, gap 0"));
            setOpaque(true);
            msg = txt;
            id = i;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (id != active) {
                        setBorder(hoverBorder);
                        setBackground(hoverColor);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (id != active) {
                        setBorder(inactiveBorder);
                        setBackground(inactiveColor);
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    allVTabs.get(active).setBackground(inactiveColor);
                    active = id;
                    cards.show(pnlContent, "CARD" + id);
                    setBackground(activeColor);
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            w = getWidth();

            // Careful with this font scale factor; the vertical tabs will be unreadable
            // if small window, too big if large window.
            g.setFont(skin.getFont1().deriveFont(Font.PLAIN, (int) (w * 0.68)));

            // Rotate, draw string, rotate back (to allow hover border to be painted properly)
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform at = g2d.getTransform();
            at.rotate(Math.toRadians(90), 0, 0);
            g2d.setTransform(at);
            g2d.setColor(AllZone.getSkin().getClrText());
            g2d.drawString(msg, 5, -4);

            at.rotate(Math.toRadians(-90), 0, 0);
            g2d.setTransform(at);
        }
    }
}
