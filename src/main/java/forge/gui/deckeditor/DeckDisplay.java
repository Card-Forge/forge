package forge.gui.deckeditor;

import forge.card.CardPoolView;
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
    void setDeck(CardPoolView top, CardPoolView bottom, GameType gameType);

    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    CardPoolView getTop();

    //bottom shows cards that the user has chosen for his library
    CardPoolView getBottom();

    void setTitle(String message);

    Deck getDeck();
    GameType getGameType();
}
