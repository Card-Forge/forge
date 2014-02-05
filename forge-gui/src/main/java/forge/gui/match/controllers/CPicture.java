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
package forge.gui.match.controllers;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;

import forge.Command;
import forge.Singletons;
import forge.card.CardCharacteristicName;
import forge.game.card.Card;
import forge.gui.CardPicturePanel;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VPicture;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.special.CardZoomer;
import forge.item.IPaperCard;
import forge.item.InventoryItem;

/**
 * Singleton controller for VPicture.
 * <p>
 * Can be used to display images associated with a {@code Card} or
 * {@code InventoryItem} in {@code CardPicturePanel}.<br>
 * <br>
 * Can also be used to display details associated with a {@code Card}.
 * 
 * @version: $Id:$
 * 
 */
public enum CPicture implements ICDoc {
    SINGLETON_INSTANCE;

    // For brevity, local shortcuts to singletons & child controls...
    private final VPicture view = VPicture.SINGLETON_INSTANCE;
    private final CardPicturePanel picturePanel = this.view.getPnlPicture();
    private final JLabel flipIndicator = this.view.getLblFlipcard();
    private final CardZoomer zoomer = CardZoomer.SINGLETON_INSTANCE;

    private Card currentCard = null;
    private CardCharacteristicName displayedState = CardCharacteristicName.Original;

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     */
    public void showCard(final Card c, boolean showFlipped) {
        if (null == c) {
            return;
        }

        currentCard = c;
        displayedState = c.getCurState();
        flipIndicator.setVisible(isCurrentCardFlippable());
        picturePanel.setCard(c);
        if (showFlipped && isCurrentCardFlippable()) {
            flipCard();
        }
    }

    /**
     * Displays image associated with either a {@code Card}
     * or {@code InventoryItem} instance.
     */
    public void showImage(final InventoryItem item) {
        if (item instanceof IPaperCard) {
            IPaperCard paperCard = ((IPaperCard)item);
            Card c = Card.getCardForUi(paperCard);
            if (paperCard.isFoil() && c.getFoil() == 0) {
                c.setRandomFoil();
            }
            showCard(c, false);
        } else {
            currentCard = null;
            flipIndicator.setVisible(false);
            picturePanel.setCard(item);
        }
    }

    public Card getCurrentCard() {
        return currentCard;
    }

    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    @Override
    public void initialize() {
        setMouseWheelListener();
        setMouseButtonListener();
    }

    /**
     * Adds a mouse button listener to CardPicturePanel.
     * <p><ul>
     * <li>Shows the displayed card in the zoomer while the middle mouse button or
     * both left and right buttons are held down simultaneously.
     * <li>Displays the alternate image for applicable cards on mouse click.
     */
    private void setMouseButtonListener() {
        this.picturePanel.addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(MouseEvent e) {
                flipCard();
            }

            @Override
            public void onMiddleMouseDown(MouseEvent e) {
                if (isCardDisplayed()) {
                    CardZoomer.SINGLETON_INSTANCE.doMouseButtonZoom(currentCard, displayedState);
                }
            }

            @Override
            public void onMiddleMouseUp(MouseEvent e) {
                if (isCardDisplayed()) {
                    CardZoomer.SINGLETON_INSTANCE.closeZoomer();
                }
            }
        });
    }

    /**
     * Adds a mouse wheel listener to CardPicturePanel.
     * <p>
     * Shows the displayed card in the zoomer if the mouse wheel is rotated
     * while the mouse pointer is hovering over the image.
     */
    private void setMouseWheelListener() {
        picturePanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent arg0) {
                if (isCardDisplayed()) {
                    if (arg0.getWheelRotation() < 0) {
                        zoomer.doMouseWheelZoom(currentCard, displayedState);
                    }
                }
            }
        });
    }

    private boolean isCardDisplayed() {
        return (currentCard != null);
    }

    @Override
    public void update() {
    }

    public void flipCard() {
        if (isCurrentCardFlippable()) {
            displayedState = getAlternateState(currentCard, displayedState);
            picturePanel.setCardImage(displayedState);
            setCardDetailPanel();
        }
    }

    /**
     * Displays details about the current card state in appropriate GUI panel.
     * <p>
     * It does this by temporarily setting the {@code CardCharacteristicName} state
     * of the card, extracting the details and then setting the card back to its
     * original state.
     * <p>
     * TODO: at the moment setting the state of {@code Card} does not appear to
     * trigger any significant functionality but potentially this could cause
     * unforeseen consequences. Recommend that a read-only mechanism is implemented
     * to get card details for a given {@code CardCharacteristicName} state that does
     * not require temporarily setting state of {@code Card} instance.
     */
    private void setCardDetailPanel() {
        CardCharacteristicName temp = currentCard.getCurState();
        currentCard.setState(displayedState);
        CDetail.SINGLETON_INSTANCE.showCard(currentCard);
        currentCard.setState(temp);
    }

    private boolean isCurrentCardFlippable() {
        if (currentCard == null) { return false; }

        return currentCard.isDoubleFaced() || currentCard.isFlipCard() ||
                (currentCard.isFaceDown() && isAuthorizedToViewFaceDownCard(currentCard));
    }

    /**
     * Card characteristic state machine.
     * <p>
     * Given a card and a state in terms of {@code CardCharacteristicName} this
     * will determine whether there is a valid alternate {@code CardCharacteristicName}
     * state for that card.
     * 
     * @param card the {@code Card}
     * @param currentState not necessarily {@code card.getCurState()}
     * @return the alternate {@code CardCharacteristicName} state or default if not applicable
     */
    public static CardCharacteristicName getAlternateState(final Card card, CardCharacteristicName currentState) {
        // Default. Most cards will only ever have an "Original" state represented by a single image.
        CardCharacteristicName alternateState = CardCharacteristicName.Original;

        if (card.isDoubleFaced()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.Transformed;
            }

        } else if (card.isFlipCard()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.Flipped;
            }

        } else if (card.isFaceDown()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.FaceDown;
            } else if (isAuthorizedToViewFaceDownCard(card)) {
                alternateState = CardCharacteristicName.Original;
            } else {
                alternateState = currentState;
            }
        }

        return alternateState;
    }

    /**
     * Prevents player from identifying opponent's face-down card.
     */
    public static boolean isAuthorizedToViewFaceDownCard(Card card) {
        return Singletons.getControl().mayShowCard(card);
    }
}
