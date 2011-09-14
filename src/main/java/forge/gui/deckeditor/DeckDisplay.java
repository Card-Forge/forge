package forge.gui.deckeditor;

import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.card.InventoryItem;
import forge.deck.Deck;
import forge.game.GameType;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 * Date: 6/24/11
 *
 * @author Forge
 * @version $Id$
 */
public interface DeckDisplay {
    void setDeck(CardPoolView<CardPrinted> top, CardPoolView<CardPrinted> bottom, GameType gameType);

    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    CardPoolView<InventoryItem> getTop();

    //bottom shows cards that the user has chosen for his library
    CardPoolView<InventoryItem> getBottom();

    void setTitle(String message);

    Deck getDeck();
    GameType getGameType();
}
