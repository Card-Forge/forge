package forge.gui.deckeditor;

import forge.deck.Deck;
import forge.game.GameType;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 * Date: 6/24/11
 *
 * @author Forge
 * @version $Id$
 */
public interface DeckDisplay {
    void setDeck(ItemPoolView<CardPrinted> top, ItemPoolView<CardPrinted> bottom, GameType gameType);

    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    ItemPoolView<InventoryItem> getTop();

    //bottom shows cards that the user has chosen for his library
    ItemPoolView<InventoryItem> getBottom();

    void setTitle(String message);

    Deck getDeck();
    GameType getGameType();
}
