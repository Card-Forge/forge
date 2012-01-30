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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.SetUtils;
import forge.card.CardSet;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.BoosterPack;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.PreconDeck;

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
    private final JLabel descLabel = new JLabel();
    private final JScrollPane description;

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
        this.bChangeState.setFont(new java.awt.Font("Dialog", 0, 10));

        description = new JScrollPane( descLabel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        descLabel.setSize(descLabel.getWidth() - description.getVerticalScrollBar().getWidth(), descLabel.getHeight());
        
        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(this.detail, "w 239, h 303, grow, flowy, wrap");
        this.add(this.description, "w 239, h 303, grow, flowy, wrap");
        this.add(this.bChangeState, "align 50% 0%, wrap");
        this.add(this.picture, "wmin 239, hmin 323, grow");
    }

    private static Dimension shrinkedComponent = new Dimension(239, 0);
    private static Dimension expandedComponent = new Dimension(239, 303);
    
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
        this.description.setVisible(!isCard);
        description.setMaximumSize(isCard ? shrinkedComponent : expandedComponent);
        detail.setMaximumSize(!isCard ? shrinkedComponent : expandedComponent);
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
        } else {
            if( card instanceof BoosterPack )
            {
                BoosterPack booster = (BoosterPack) card;
                CardSet set = SetUtils.getSetByCodeOrThrow(booster.getSet());
                String tpl = "<html><b>%s booster pack.</b><br>Contains %d cards.<br><br>Buy it to reveal the cards and add them to your inventory.</html>";
                descLabel.setText(String.format(tpl, set.getName(), set.getBoosterData().getTotal()));
            } else if ( card instanceof PreconDeck )
            {
                PreconDeck deck = (PreconDeck) card;
                String desc = deck.getDescription();
                String tpl = "<html><center>%s</center>%s<br><br>This deck contains the following cards:<br>%s</html>";
                String decklist = StringUtils.join( deck.getDeck().getMain().toItemListString(), "<br>");
                descLabel.setText(String.format(tpl,  deck.getName(), desc, decklist ));
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

            if (c.isFlip()) {
                this.bChangeState.setVisible(true);
                this.bChangeState.setText("Flip");
            } else if (c.isDoubleFaced()) {
                this.bChangeState.setVisible(true);
                this.bChangeState.setText("Transform");
            } else {
                this.bChangeState.setVisible(false);
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
