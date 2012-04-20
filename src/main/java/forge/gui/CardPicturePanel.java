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

package forge.gui;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import forge.Card;
import forge.ImageCache;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.view.arcane.ScaledImagePanel;
import forge.view.arcane.ScaledImagePanel.MultipassType;
import forge.view.arcane.ScaledImagePanel.ScalingType;

/**
 * The class CardPicturePanel. Shows the full-sized image in a label. if there's
 * no picture, the cardname is displayed instead.
 * 
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public final class CardPicturePanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=-3160874016387273383L</code>. */
    private static final long serialVersionUID = -3160874016387273383L;

    private Card card;
    private InventoryItem inventoryItem;

    // private JLabel label;
    // private ImageIcon icon;
    private final ScaledImagePanel panel;
    private Image currentImange;

    /**
     * <p>
     * Constructor for CardPicturePanel.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public CardPicturePanel(final Card c) {
        super(new BorderLayout());
        // add(label = new JLabel(icon = new ImageIcon()));
        this.panel = new ScaledImagePanel();
        this.add(this.panel);
        this.panel.setScalingBlur(false);
        this.panel.setScalingType(ScalingType.bicubic);
        this.panel.setScalingMultiPassType(MultipassType.none);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent e) {
                CardPicturePanel.this.update();
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                CardPicturePanel.this.update();
            }
        });

        this.setCard(c);
    }

    /**
     * <p>
     * update.
     * </p>
     */
    public void update() {
        this.setCard(this.getCard());
    }

    /**
     * Sets the card.
     * 
     * @param cp
     *            the new card
     */
    public void setCard(final InventoryItem cp) {
        this.card = null;
        this.inventoryItem = cp;
        if (!this.isShowing()) {
            return;
        }

        this.setImage();
    }

    /** {@inheritDoc} */
    @Override
    public void setCard(final Card c) {
        this.card = c;
        this.inventoryItem = null;
        this.setImage();
    }

    private void setImage() {
        final Insets i = this.getInsets();
        Image image = null;
        if (this.inventoryItem != null) {
            image = ImageCache.getImage(this.inventoryItem, this.getWidth() - i.left - i.right, this.getHeight()
                    - i.top - i.bottom);
        }
        if ((this.card != null) && (image == null)) {
            image = ImageCache.getImage(this.card, this.getWidth() - i.left - i.right - 2, this.getHeight() - i.top
                    - i.bottom - 2);
        }

        if (image != this.currentImange) {
            this.currentImange = image;
            this.panel.setImage(image, null);
            this.panel.repaint();
        }
        // if(image == null) {
        // label.setIcon(null);
        // //avoid a hard reference to the image while not needed
        // icon.setImage(null);
        // label.setText(card.isFaceDown()? "Morph":card.getName());
        // } else if(image != icon.getImage()) {
        // icon.setImage(image);
        // label.setIcon(icon);
        // }
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    @Override
    public Card getCard() {
        if ((this.card == null) && (this.inventoryItem != null) && (this.inventoryItem instanceof CardPrinted)) {
            this.card = ((CardPrinted) this.inventoryItem).toForgeCard();
        }
        return this.card;
    }
}
