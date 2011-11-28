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
package forge.gui.game;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import forge.Card;
import forge.CardContainer;
import forge.GuiDisplayUtil;
import forge.ImageCache;

/**
 * The class CardPanel. A card panel stores a card to display it on the
 * battlefield. An image is used if available.
 * 
 * @author Forge
 * @version $Id$
 */
public class CardPanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=509877513760665415L</code>. */
    private static final long serialVersionUID = 509877513760665415L;
    private Card card;

    /**
     * <p>
     * Constructor for CardPanel.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public CardPanel(final Card card) {
        this.setCard(card);
    }

    /**
     * <p>
     * addComponents.
     * </p>
     */
    private void addComponents() {
        final Card c = this.getCard();
        final Image cardImage = ImageCache.getImage(c);
        if (cardImage == null) {
            // show the card as text
            this.setLayout(new GridLayout(0, 1));

            this.add(new JLabel(c.isFaceDown() ? "Morph" : c.getName() + "   " + c.getManaCost()));
            this.add(new JLabel(GuiDisplayUtil.formatCardType(c)));

            final JLabel tapLabel = new JLabel("Tapped");
            tapLabel.setBackground(Color.white);
            tapLabel.setOpaque(true);

            if (c.isTapped()) {
                this.add(tapLabel);
            }

            if (c.isCreature()) {
                this.add(new JLabel(c.getNetAttack() + " / " + c.getNetDefense()));
            }
        } else {
            // show the card image
            this.setLayout(new GridLayout(1, 1));
            this.add(new JLabel(new ImageIcon(cardImage)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setCard(final Card card) {
        this.card = card;
        this.setBorder(GuiDisplayUtil.getBorder(card));
        this.addComponents();
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card getCard() {
        return this.card;
    }

    /**
     * Gets the connected card.
     * 
     * @return the connectedCard
     */
    public CardPanel getConnectedCard() {
        return this.connectedCard;
    }

    /**
     * Sets the connected card.
     * 
     * @param connectedCard
     *            the connectedCard to set
     */
    public void setConnectedCard(final CardPanel connectedCard) {
        this.connectedCard = connectedCard; // TODO: Add 0 to parameter's name.
    }

    // ~
    /** The connected card. */
    private CardPanel connectedCard;
    // ~
}
