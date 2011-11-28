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
package forge.view.match;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.GuiDisplayUtil;
import forge.ImageCache;
import forge.control.match.ControlHand;
import forge.view.toolbox.FRoundedPanel;

/**
 * VIEW - Swing components for user hand.
 * 
 */
@SuppressWarnings("serial")
public class ViewHand extends JScrollPane {
    private FRoundedPanel pnlContent;
    private final ControlHand control;
    private List<CardPanel> cardPanels = new ArrayList<CardPanel>();

    /**
     * VIEW - Swing components for user hand.
     */
    public ViewHand() {
        super(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        this.setOpaque(false);
        this.getViewport().setOpaque(false);
        this.getHorizontalScrollBar().setUnitIncrement(16);
        this.setBorder(null);

        // After all components are in place, instantiate controller.
        this.control = new ControlHand(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlHand
     */
    public ControlHand getController() {
        return this.control;
    }

    /**
     * Rebuilds layout of the hand panel. Card panels are removed, the height
     * and card aspect ratio are used to set layout column width, then card
     * panels are added to the fresh layout.
     * 
     */
    // This design choice was made to allow the functionality of a JPanel
    // while maintaining a scale-able view. Overridden paint methods could
    // do this, but require heavy coding.
    public void refreshLayout() {
        // Remove all panels and recalculate layout scaling based on aspect
        // ratio.
        this.pnlContent = new FRoundedPanel();
        this.pnlContent.setBackground(AllZone.getSkin().getClrTheme());
        this.pnlContent.setCorners(new boolean[] { true, false, false, true });
        this.pnlContent.setLayout(new MigLayout("insets 3 10 3 10"));
        this.pnlContent.setSize(this.getViewport().getSize());
        this.pnlContent.validate();
        this.setViewportView(this.pnlContent);

        final int h = this.getViewport().getHeight() - 6;
        this.pnlContent.setLayout(new MigLayout("align center"));

        // Re-insert panel instances. Possible memory management problem
        // from re-adding pre-existing panels. Doublestrike 22-11-11
        this.cardPanels = new ArrayList<CardPanel>();
        for (final Card c : this.control.getCards()) {
            final CardPanel temp = new CardPanel(c);
            this.cardPanels.add(temp);
            this.pnlContent.add(temp, "h " + h + "px!, w " + (int) (h * 0.7) + "px!");
            this.control.addCardPanelListeners(temp);
        }
        // Notify system of change.
    }

    /**
     * The Class CardPanel.
     */
    public class CardPanel extends JPanel {
        private static final long serialVersionUID = 509877513760665415L;
        private Card card = null;
        private final Image img;
        private int w, h = 0;

        /**
         * <p>
         * Constructor for CardPanel.
         * </p>
         * 
         * @param c
         *            &emsp; Card object.
         */
        public CardPanel(final Card c) {
            super();
            this.card = c;
            this.img = ImageCache.getImage(this.card);

            this.setToolTipText("<html>" + c.getName() + "<br>" + GuiDisplayUtil.formatCardType(c) + "</html>");

            // No image?
            if (this.img == null) {
                this.setBorder(new LineBorder(new Color(240, 240, 240), 1));
                this.setLayout(new MigLayout("wrap, insets 2, gap 0"));
                this.setOpaque(true);
                this.setBackground(new Color(200, 200, 200));

                final JLabel lblManaCost = new JLabel(c.getManaCost());
                lblManaCost.setHorizontalAlignment(SwingConstants.RIGHT);

                final JLabel lblCardName = new JLabel(c.getName());
                lblCardName.setHorizontalAlignment(SwingConstants.CENTER);

                final JLabel lblPowerToughness = new JLabel("");
                lblPowerToughness.setHorizontalAlignment(SwingConstants.RIGHT);

                if (c.isFaceDown()) {
                    lblCardName.setText("Morph");
                    lblManaCost.setText("");
                }

                if (c.isCreature()) {
                    lblPowerToughness.setText(c.getNetAttack() + " / " + c.getNetDefense());
                }

                this.add(lblManaCost, "w 90%!");
                this.add(lblCardName, "w 90%!");
                this.add(lblPowerToughness, "w 90%!, gaptop 25");
            } else {
                this.setBorder(new LineBorder(Color.black, 1));
                this.w = this.img.getWidth(null);
                this.h = this.img.getHeight(null);
            }
        }

        /**
         * Gets the card.
         * 
         * @return Card
         */
        public Card getCard() {
            return this.card;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);
            if (this.img != null) {
                final Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(this.img, 0, 0, this.getWidth(), this.getHeight(), 0, 0, this.w, this.h, null);
            } else {
                g.setColor(new Color(200, 200, 200));
                g.drawRect(1, 1, this.getWidth(), this.getHeight());
            }
        }
    }

    /**
     * Gets the card panels.
     * 
     * @return List<CardPanel>
     */
    public List<CardPanel> getCardPanels() {
        return this.cardPanels;
    }
}
