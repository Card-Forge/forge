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
package forge.gui.deckeditor.elements;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardCharactersticName;
import forge.Singletons;
import forge.card.CardEdition;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.OpenablePack;
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
    private final JTextPane description = new JTextPane();
    private final JScrollPane descrScroll;

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

        this.descrScroll = new JScrollPane(this.description);

        this.setLayout(new MigLayout("fill, ins 0"));
        this.add(this.detail, "w 239, h 303, grow, flowy, wrap");
        this.add(this.descrScroll, "w 239, h 303, grow, flowy, wrap");
        this.add(this.bChangeState, "align 50% 0%, wrap");
        this.add(this.picture, "wmin 239, hmin 323, grow");

        this.description.setEditable(false);
        this.description.setCursor(null);
        this.description.setOpaque(false);
        this.description.setFocusable(false);
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
        this.descrScroll.setMaximumSize(isCard ? CardPanelLite.shrinkedComponent : CardPanelLite.expandedComponent);
        this.detail.setMaximumSize(!isCard ? CardPanelLite.shrinkedComponent : CardPanelLite.expandedComponent);
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
            if (card instanceof OpenablePack) {
                final OpenablePack booster = (OpenablePack) card;
                final CardEdition set = Singletons.getModel().getEditions().getEditionByCodeOrThrow(booster.getEdition());
                final String tpl = "%s %s.%n%nContains %d cards.%n%nBuy it to reveal the cards and add them to your inventory.";
                this.description.setText(String.format(tpl, set.getName(), booster.getType(), booster.getTotalCards()));
            } else if (card instanceof PreconDeck) {
                final PreconDeck deck = (PreconDeck) card;
                final String desc = deck.getDescription();
                final String tpl = "%s%n%n%s%n%nThis deck contains the following cards:%n%s";
                final String decklist = StringUtils.join(deck.getDeck().getMain().toItemListString(), "\n");
                this.description.setText(String.format(tpl, deck.getName(), desc, decklist));
                this.description.setCaretPosition(0);

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
                if (cur.getCurState() == CardCharactersticName.Transformed) {
                    cur.setState(CardCharactersticName.Original);
                } else {
                    cur.setState(CardCharactersticName.Transformed);
                }
            }

            this.setCard(cur);
        }
    }

}
