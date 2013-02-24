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

import forge.Card;
import forge.CardCharacteristicName;
import forge.Command;
import forge.gui.framework.ICDoc;
import forge.gui.match.views.VPicture;
import forge.item.IPaperCard;
import forge.item.InventoryItem;

/**
 * Controls the card picture panel in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CPicture implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private Card currentCard = null;
    private boolean flipped = false;

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     * @param c
     *            &emsp; Card object
     */
    public void showCard(final Card c) {
        boolean canFlip = c != null && c.isDoubleFaced();
        this.currentCard = c;
        flipped = canFlip && c.getCurState() == CardCharacteristicName.Transformed; 
        VPicture.SINGLETON_INSTANCE.getLblFlipcard().setVisible(canFlip);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setCard(c);
    }

    public void showCard(final InventoryItem item) {
        if ( item instanceof IPaperCard ) {
            showCard(((IPaperCard)item).getMatchingForgeCard());
            return;
        }

        this.currentCard = null;
        VPicture.SINGLETON_INSTANCE.getLblFlipcard().setVisible(false);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setCard(item);
    }

    /**
     * Gets the current card.
     * 
     * @return Card
     */
    public Card getCurrentCard() {
        return this.currentCard;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        VPicture.SINGLETON_INSTANCE.getPnlPicture().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                flipCard();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    /** */
    public void flipCard() {
        flipped = !flipped;
        Card cd = VPicture.SINGLETON_INSTANCE.getPnlPicture().getCard();
        if ( null == cd ) return;
        
        CardCharacteristicName newState = flipped && cd.isDoubleFaced() ? CardCharacteristicName.Transformed : CardCharacteristicName.Original; 
        CardCharacteristicName oldState = cd.getCurState();
        if ( oldState != newState ) { 
            cd.setState(newState);
        }
        CDetail.SINGLETON_INSTANCE.showCard(this.currentCard);
        VPicture.SINGLETON_INSTANCE.getPnlPicture().setImage();
        if ( oldState != newState ) {
            cd.setState(oldState);
        }
    }
}
