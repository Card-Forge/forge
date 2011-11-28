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

import forge.deck.Deck;
import forge.game.GameType;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;

/**
 * Created by IntelliJ IDEA. User: dhudson Date: 6/24/11
 * 
 * @author Forge
 * @version $Id$
 */
public interface DeckDisplay {
    /**
     * 
     * setDeck.
     * 
     * @param top
     *            ItemPoolView<CardPrinted>
     * @param bottom
     *            ItemPoolView<CardPrinted>
     * @param gameType
     *            GameType
     */
    void setDeck(ItemPoolView<CardPrinted> top, ItemPoolView<CardPrinted> bottom, GameType gameType);

    /**
     * 
     * setItems.
     * 
     * @param <T>
     *            InventoryItem
     * @param topParam
     *            ItemPoolView<T>
     * @param bottomParam
     *            ItemPoolView<T>
     * @param gt
     *            GameType
     */
    <T extends InventoryItem> void setItems(ItemPoolView<T> topParam, ItemPoolView<T> bottomParam, GameType gt);

    /**
     * 
     * Top shows available card pool. if constructed, top shows all cards if
     * sealed, top shows 5 booster packs if draft, top shows cards that were
     * chosen
     * 
     * @return ItemPoolView<InventoryItem>
     */
    ItemPoolView<InventoryItem> getTop();

    //
    /**
     * 
     * Bottom shows cards that the user has chosen for his library.
     * 
     * @return ItemPoolView<InventoryItem>
     */
    ItemPoolView<InventoryItem> getBottom();

    /**
     * 
     * Set title.
     * 
     * @param message
     *            String
     */
    void setTitle(String message);

    /**
     * 
     * Get deck.
     * 
     * @return Deck
     */
    Deck getDeck();

    /**
     * 
     * Get game type.
     * 
     * @return GameType
     */
    GameType getGameType();
}
