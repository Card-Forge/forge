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
package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.Singletons;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.CardPrinted;
import forge.item.InventoryItem;

/**
 * This panel is to be placed in the right part of a deck editor.
 * 
 */
public class CardPanelLite extends CardPanelBase {

    private static final long serialVersionUID = -7134546689397508597L;

    // Controls to show card details
    /** The detail. */
    private final CardDetailPanel detail = new CardDetailPanel(null);
    private final CardPicturePanel picture = new CardPicturePanel(null);
    private final JButton bChangeState = new JButton();

    /**
     * 
     * Constructor.
     */
    public CardPanelLite() {
        this.bChangeState.setVisible(false);
        this.bChangeState.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CardPanelLite.this.bChangeStateActionPerformed(e);
            }
        });
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.bChangeState.setFont(new java.awt.Font("Dialog", 0, 10));
        }

        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(this.detail, "w 239, h 303, grow, flowy, wrap");
        this.add(this.bChangeState, "align 50% 0%, wrap");
        this.add(this.picture, "wmin 239, hmin 323, grow");
    }

    /**
     * 
     * ShowCard.
     * 
     * @param card
     *            an InventoryItem
     */
    @Override
    public final void showCard(final InventoryItem card) {
        this.picture.setCard(card);
        final boolean isCard = (card != null) && (card instanceof CardPrinted);
        this.detail.setVisible(isCard);
        if (isCard) {
            final Card toSet = ((CardPrinted) card).toForgeCard();

            this.detail.setCard(toSet);
            if (toSet.hasAlternateState()) {
                this.bChangeState.setVisible(true);
                if (toSet.isFlip()) {
                    this.bChangeState.setText("Flip");
                } else {
                    this.bChangeState.setText("Transform");
                }
            }
        }
    }

    /**
     * Sets the card.
     * 
     * @param c
     *            the new card
     */
    public final void setCard(final Card c) {
        this.picture.setCard(c);
        if (c != null) {
            this.detail.setCard(c);
            if (c.hasAlternateState()) {
                this.bChangeState.setVisible(true);
                if (c.isFlip()) {
                    this.bChangeState.setText("Flip");
                } else {
                    this.bChangeState.setText("Transform");
                }
            }
        }
    }

    /**
     * 
     * getCard.
     * 
     * @return Card
     */
    public final Card getCard() {
        return this.detail.getCard();
    }

    private void bChangeStateActionPerformed(final ActionEvent e) {
        final Card cur = this.detail.getCard();
        if (cur != null) {
            if (cur.isDoubleFaced()) {
                if (cur.getCurState().equals("Transformed")) {
                    cur.setState("Original");
                } else {
                    cur.setState("Transformed");
                }
            }

            this.setCard(cur);
        }
    }

}
