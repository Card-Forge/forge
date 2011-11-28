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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import forge.Card;
import forge.ImageCache;
import forge.control.match.ControlCardviewer;
import forge.view.toolbox.CardDetailPanel;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FVerticalTabPanel;

/**
 * Vertical tab panel for viewing card picture and/or details.
 * 
 */
public class ViewCardviewer {
    private final List<JPanel> panelList;
    private final ControlCardviewer control;

    private final CardPicPanel pnlCardPic;
    private final CardDetailPanel pnlCardDetail;
    private final FVerticalTabPanel vtpCardviewer;

    private int w, h;

    /**
     * Instantiates a new view cardviewer.
     */
    public ViewCardviewer() {
        // Assemble card pic viewer
        this.panelList = new ArrayList<JPanel>();

        this.pnlCardPic = new CardPicPanel();
        this.pnlCardPic.setOpaque(false);
        this.pnlCardPic.setName("Picture");
        this.pnlCardPic.setToolTipText("Card Picture");
        this.panelList.add(this.pnlCardPic);

        this.pnlCardDetail = new CardDetailPanel();
        this.pnlCardDetail.setOpaque(false);
        this.pnlCardDetail.setName("Detail");
        this.pnlCardDetail.setToolTipText("Card Text");
        this.panelList.add(this.pnlCardDetail);

        this.vtpCardviewer = new FVerticalTabPanel(this.panelList);

        // After all components are in place, instantiate controller.
        this.control = new ControlCardviewer(this);
    }

    /**
     * Gets the controller.
     * 
     * @return ControlCardviewer
     */
    public ControlCardviewer getController() {
        return this.control;
    }

    /**
     * Card picture handling in side panel of match.
     * 
     */
    @SuppressWarnings("serial")
    public class CardPicPanel extends FPanel {
        private Card card = null;

        /**
         * Sets the card.
         * 
         * @param c
         *            &emsp; Card object
         */
        public void setCard(final Card c) {
            this.card = c;
            this.repaint();
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
         * @see forge.view.toolbox.FPanel#paintComponent(java.awt.Graphics)
         */
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            if (this.card != null) {
                ViewCardviewer.this.w = this.getWidth();
                ViewCardviewer.this.h = (int) (ViewCardviewer.this.w / 0.7);
                final BufferedImage img = ImageCache.getImage(this.card, ViewCardviewer.this.w, ViewCardviewer.this.h);
                g.drawImage(img, 0, ((this.getHeight() - ViewCardviewer.this.h) / 2), null);
            }
        }
    }

    /**
     * Gets the pnl card pic.
     * 
     * @return CardPicPanel
     */
    public CardPicPanel getPnlCardPic() {
        return this.pnlCardPic;
    }

    /**
     * Gets the pnl card detail.
     * 
     * @return CardDetailPanel
     */
    public CardDetailPanel getPnlCardDetail() {
        return this.pnlCardDetail;
    }

    /**
     * Gets the vtp cardviewer.
     * 
     * @return FVerticalTabPanel
     */
    public FVerticalTabPanel getVtpCardviewer() {
        return this.vtpCardviewer;
    }
}
