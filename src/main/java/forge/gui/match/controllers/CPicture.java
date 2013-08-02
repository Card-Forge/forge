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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import forge.Card;
import forge.CardCharacteristicName;
import forge.Command;
import forge.Singletons;
import forge.card.CardEdition;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VPicture;
import forge.gui.toolbox.special.CardZoomer;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.util.MyRandom;

/**
 * Controls the card picture panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CPicture implements ICDoc {
    SINGLETON_INSTANCE;

    private Card currentCard = null;
    private boolean flipped = false;
    private boolean canFlip = false;
    private boolean cameFaceDown = false; 

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void showCard(final Card c, boolean showFlipped) {
        cameFaceDown = c.isFaceDown() && Singletons.getControl().mayShowCard(c);
        canFlip = c.isDoubleFaced() || c.isFlipCard() || cameFaceDown;
        currentCard = c;
        flipped = canFlip && (c.getCurState() == CardCharacteristicName.Transformed ||
                              c.getCurState() == CardCharacteristicName.Flipped || 
                              c.getCurState() == CardCharacteristicName.FaceDown); 
        VPicture.SINGLETON_INSTANCE.getLblFlipcard().setVisible(canFlip);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setCard(c);
                
        if ( showFlipped && canFlip )
            flipCard();
    }

    public void showCard(final InventoryItem item) {
        if (item instanceof IPaperCard) {
            Card c = ((IPaperCard)item).getMatchingForgeCard();
            if (((IPaperCard)item).isFoil() && c.getFoil() == 0) {
                c.setRandomFoil();
            }
            showCard(c, false);
            return;
        }
        
        canFlip = false;
        flipped = false;
        currentCard = null;
        VPicture.SINGLETON_INSTANCE.getLblFlipcard().setVisible(false);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setCard(item);
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
        
        VPicture.SINGLETON_INSTANCE.getPnlPicture().addMouseWheelListener(new MouseWheelListener() {            
            @Override
            public void mouseWheelMoved(MouseWheelEvent arg0) {
                if (arg0.getWheelRotation() < 0) {
                    CardZoomer.SINGLETON_INSTANCE.displayZoomedCard(currentCard);
                }
            }
        });

        VPicture.SINGLETON_INSTANCE.getPnlPicture().addMouseListener(new MouseAdapter() {
            private final boolean[] buttonsDown = new boolean[4];
            private boolean zoomed;

            @Override
            public void mousePressed(final MouseEvent e) {
                final int button = e.getButton();
                if (button < 1 || button > 3) {
                    return;
                }
                this.buttonsDown[button] = true;

                if (!this.zoomed && (this.buttonsDown[2] || (this.buttonsDown[1] && this.buttonsDown[3]))) {
                    //zoom card when middle mouse button down or both left and right mouse buttons down
                    if (CardZoomer.SINGLETON_INSTANCE.displayZoomedCard(currentCard, true)) {
                        this.zoomed = true;
                    }
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                final int button = e.getButton();
                if (button < 1 || button > 3) {
                    return;
                }
                if (!this.buttonsDown[button]) {
                    return;
                }
                this.buttonsDown[button] = false;

                if (this.zoomed) {
                    if (!this.buttonsDown[1] && !this.buttonsDown[2] && !this.buttonsDown[3]) {
                        //don't stop zooming until all mouse buttons released
                        CardZoomer.SINGLETON_INSTANCE.closeZoomer();
                        this.zoomed = false;
                    }
                    return; //don't handle click event below if zoom was open
                }

                if (button == 1) {
                    flipCard();
                }
            }
        });
    }

    @Override
    public void update() {
    }

    public void flipCard() {
        if (!canFlip || null == currentCard) { return; }
        
        flipped = !flipped;
        
        final CardCharacteristicName newState;
        if (flipped) {
            if (currentCard.isDoubleFaced()) {
                newState = CardCharacteristicName.Transformed;
            } else if (currentCard.isFlipCard()) {
                newState = CardCharacteristicName.Flipped;
            } else if ( cameFaceDown ) { 
                newState = CardCharacteristicName.FaceDown;
            } else {
                // if this is hit, then then showCard has been modified to handle additional types, but
                // this function is missing an else if statement above
                throw new RuntimeException("unhandled flippable card");
            }
        } else {
            newState = CardCharacteristicName.Original;
        }

        CardCharacteristicName oldState = currentCard.getCurState();
        if (oldState != newState) { 
            currentCard.setState(newState);
        }
        
        CDetail.SINGLETON_INSTANCE.showCard(this.currentCard);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setImage();
        if (oldState != newState) {
            currentCard.setState(oldState);
        }
    }
}
