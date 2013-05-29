package forge.gui.input;

import forge.Card;

public abstract class InputSelectCards extends InputSelectManyBase<Card> {
    private static final long serialVersionUID = -6609493252672573139L;

    protected InputSelectCards(int min, int max) {
        super(min, max);
    }

    @Override
    protected void onCardSelected(Card c, boolean isRmb) {
        if ( !selectEntity(c) )
            return;
        
        refresh();
    }
}
