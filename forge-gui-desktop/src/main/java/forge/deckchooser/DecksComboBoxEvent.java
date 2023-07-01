package forge.deckchooser;

import java.util.EventObject;

import forge.deck.DeckType;

@SuppressWarnings("serial")
public class DecksComboBoxEvent extends EventObject {
    private final DeckType deckType;

    public DecksComboBoxEvent(Object source, DeckType deckType0) {
        super(source);
        deckType = deckType0;
    }

    public DeckType getDeckType() {
        return deckType;
    }
}
