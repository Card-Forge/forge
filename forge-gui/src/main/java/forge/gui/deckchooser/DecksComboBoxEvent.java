package forge.gui.deckchooser;

import java.util.EventObject;

import forge.gui.deckchooser.DecksComboBox.DeckType;

@SuppressWarnings("serial")
public class DecksComboBoxEvent extends EventObject {

    private final DeckType _deckType;

    public DecksComboBoxEvent(Object source, DeckType deckType) {
        super(source);
        _deckType = deckType;
    }

    public DeckType getDeckType() {
        return _deckType;
    }

}
