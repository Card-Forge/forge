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
package forge.screens.match.controllers;

import forge.UiCommand;
import forge.Singletons;
import forge.card.CardDetailUtil;
import forge.game.card.Card;
import forge.gui.framework.ICDoc;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;
import forge.screens.match.views.VDetail;
import forge.toolbox.FMouseAdapter;

import java.awt.event.MouseEvent;

/**
 * Controls the card detail area in the match UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CDetail implements ICDoc {
    SINGLETON_INSTANCE;

    private VDetail view = VDetail.SINGLETON_INSTANCE;

    /**
     * Shows card details and/or picture in sidebar cardview tabber.
     * 
     * @param c &emsp; Card object
     */
    public void showCard(Card c) {
        if (c != null) {
            c = c.getCardForUi();
        }
        view.getLblFlipcard().setVisible(c != null && CardDetailUtil.isCardFlippable(c) && Singletons.getControl().mayShowCard(c));
        view.getPnlDetail().setCard(c);
        if (view.getParentCell() != null) {
            view.getParentCell().repaintSelf();
        }
    }

    public void showCard(InventoryItem item) {
        if (item instanceof IPaperCard) {
            showCard(Card.getCardForUi((IPaperCard)item));
        }
        else if (item instanceof InventoryItemFromSet) {
            view.getLblFlipcard().setVisible(false);
            view.getPnlDetail().setItem((InventoryItemFromSet)item);
            view.getParentCell().repaintSelf();
        }
        else {
            showCard((Card)null);
        }
    }

    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    @Override
    public void initialize() {
        view.getPnlDetail().addMouseListener(new FMouseAdapter() {
            @Override
            public void onLeftClick(final MouseEvent e) {
                CPicture.SINGLETON_INSTANCE.flipCard();
            }
        });
    }

    @Override
    public void update() {
    }
}
