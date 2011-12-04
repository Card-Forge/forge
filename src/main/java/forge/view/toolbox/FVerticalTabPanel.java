/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    private final CardLayout cards;
    private final JPanel pnlContent;
    private final List<VTab> allVTabs;
    private int w, h;

    private final FSkin skin;
    private int active;
    private final Color activeColor, inactiveColor, hoverColor;
    private final Border inactiveBorder, hoverBorder;

    private boolean tabsOnRightSide;

    /**
     * Constructor, will automatically place tabs on left side.
     * 
     * @param childPanels
     *            &emsp; JPanels to be placed in tabber
     * @wbp.parser.constructor
     */
    public FVerticalTabPanel(final List<JPanel> childPanels) {
        this(childPanels, false);
    }

    /**
     * Assembles vertical tab panel from list of child panels. Tooltip on tab is
     * same as tooltip on child panel. Title of tab is same as name of child
     * panel.
     * 
     * @param childPanels
     *            &emsp; JPanels to be placed in tabber
     * @param b
     *            &emsp; boolean, true if tabs are on right side, false for left
     *            side.
     */
    public FVerticalTabPanel(final List<JPanel> childPanels, boolean b) {
        // General inits and skin settings
        super();
        tabsOnRightSide = b;
        this.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
        this.setOpaque(false);
        final int size = childPanels.size();
        this.skin = AllZone.getSkin();
        this.hoverColor = this.skin.getClrHover();
        this.activeColor = this.skin.getClrActive();
        this.inactiveColor = this.skin.getClrInactive();
        this.hoverBorder = new MatteBorder(1, 1, 1, 0, this.skin.getClrBorders());
        this.inactiveBorder = new MatteBorder(1, 1, 1, 0, new Color(0, 0, 0, 0));

        final int pctTabH = ((100 - 2 - 2) / size);
        final int pctTabW = 11;
        final int pctInsetH = 3;
        final int pctSpacing = 1;

        // Content panel and card layout inits
        this.cards = new CardLayout();
        this.pnlContent = new JPanel();
        this.pnlContent.setOpaque(false);
        this.pnlContent.setLayout(this.cards);

        // If tabs are on the left side, content panel is added
        // immediately to define grid.
        if (tabsOnRightSide) {
            this.add(this.pnlContent, "span 1 " + (size + 2) + ", w " + (100 - pctTabW) + "%!, h 100%!");
            this.pnlContent.setBorder(new MatteBorder(0, 0, 0, 1, this.skin.getClrBorders()));
        }

        // Add top spacer in any case.
        final JPanel topSpacer = new JPanel();
        topSpacer.setOpaque(false);
        this.add(topSpacer, "w " + pctTabW + "%!, h " + pctInsetH + "%!");

        // If tabs are on right side, content panel
        // must be added after spacer, which then defines the grid.
        if (!tabsOnRightSide) {
            this.add(this.pnlContent, "span 1 " + (size + 2) + ", w " + (100 - pctTabW) + "%!, h 100%!");
            this.pnlContent.setBorder(new MatteBorder(0, 1, 0, 0, this.skin.getClrBorders()));
        }

        // Add all tabs
        VTab tab;
        this.allVTabs = new ArrayList<VTab>();

        for (int i = 0; i < size; i++) {
            tab = new VTab(childPanels.get(i).getName(), i);
            tab.setToolTipText(childPanels.get(i).getToolTipText());

            if (i == 0) {
                tab.setBackground(this.activeColor);
                this.active = 0;
            } else {
                tab.setBackground(this.inactiveColor);
            }

            this.add(tab, "w " + pctTabW + "%!, h " + (pctTabH - pctSpacing) + "%!, gapbottom " + pctSpacing + "%!");
            this.allVTabs.add(tab);

            // Add card to content panel
            this.pnlContent.add(childPanels.get(i), "CARD" + i);
        }

        final JPanel bottomSpacer = new JPanel();
        bottomSpacer.setOpaque(false);
        this.add(bottomSpacer, "w 10%!, h " + (pctInsetH + pctSpacing) + "%!");
    }

    /**
     * Programatically flips tab layout to specified number (without needing a
     * mouse event).
     * 
     * @param index
     *            &emsp; Tab number, starting from 0
     */
    public void showTab(final int index) {
        if (index >= this.allVTabs.size()) {
            return;
        }

        this.allVTabs.get(this.active).setBackground(this.inactiveColor);
        this.active = index;
        this.cards.show(this.pnlContent, "CARD" + index);
        this.allVTabs.get(this.active).setBackground(this.activeColor);
    }

    /**
     * Gets the content panel.
     * 
     * @return JPanel
     */
    public JPanel getContentPanel() {
        return this.pnlContent;
    }

    /**
     * A single instance of a vertical tab, with paintComponent overridden to
     * provide vertical-ness. Also manages root level hover and click effects.
     * 
     */
    public class VTab extends JPanel {
        private String msg;
        private int id;

        // ID is used to retrieve this tab from the list of allVTabs.
        /**
         * Creates the actual clickable tab.
         * 
         * @param txt
         *            &emsp; String text in tab
         * @param i
         *            &emsp; int index
         */
        VTab(final String txt, final int i) {
            super();
            this.setLayout(new MigLayout("insets 0, gap 0"));
            this.setOpaque(true);
            this.msg = txt;
            this.id = i;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (VTab.this.id != FVerticalTabPanel.this.active) {
                        VTab.this.setBorder(FVerticalTabPanel.this.hoverBorder);
                        VTab.this.setBackground(FVerticalTabPanel.this.hoverColor);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (VTab.this.id != FVerticalTabPanel.this.active) {
                        VTab.this.setBorder(FVerticalTabPanel.this.inactiveBorder);
                        VTab.this.setBackground(FVerticalTabPanel.this.inactiveColor);
                    }
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    FVerticalTabPanel.this.allVTabs.get(FVerticalTabPanel.this.active).setBackground(
                            FVerticalTabPanel.this.inactiveColor);
                    FVerticalTabPanel.this.active = VTab.this.id;
                    FVerticalTabPanel.this.cards.show(FVerticalTabPanel.this.pnlContent, "CARD" + VTab.this.id);
                    VTab.this.setBackground(FVerticalTabPanel.this.activeColor);
                }
            });
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            w = this.getWidth();
            h = this.getHeight();

            // Careful with this font scale factor; the vertical tabs will be
            // unreadable
            // if small window, too big if large window.
            g.setFont(FVerticalTabPanel.this.skin.getFont1().deriveFont(Font.PLAIN, (int) (h * 0.2)));

            // Rotate, draw string, rotate back (to allow hover border to be
            // painted properly)
            final Graphics2D g2d = (Graphics2D) g;
            final AffineTransform at = g2d.getTransform();

            if (tabsOnRightSide) {
                at.rotate(Math.toRadians(90), 0, 0);
                g2d.setTransform(at);
                g2d.setColor(AllZone.getSkin().getClrText());
                g2d.drawString(this.msg, 5, -4);
            } else {
                at.rotate(Math.toRadians(-90), 0, 0);
                g2d.setTransform(at);
                g2d.setColor(AllZone.getSkin().getClrText());
                g2d.drawString(this.msg, 5 - h, w - 4);
            }

            if (tabsOnRightSide) {
                at.rotate(Math.toRadians(-90), 0, 0);
            } else {
                at.rotate(Math.toRadians(90), 0, 0);
            }

            g2d.setTransform(at);
        }

        /**
         * @param txt0
         *            &emsp; String
         */
        public void setText(String txt0) {
            this.msg = txt0;
        }
    }

    /** @return List<VTab> */
    public List<VTab> getAllVTabs() {
        return allVTabs;
    }
}
