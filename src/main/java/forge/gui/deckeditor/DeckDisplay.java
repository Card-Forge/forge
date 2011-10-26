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
