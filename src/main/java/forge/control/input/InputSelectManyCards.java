package forge.control.input;

import forge.Card;

public abstract class InputSelectManyCards extends InputSelectMany<Card> {
    private static final long serialVersionUID = -6609493252672573139L;
    
    protected InputSelectManyCards(int min, int max)
    {
        super(min, max);
    }
    
    @Override
    public final void selectCard(final Card c) {
        selectEntity(c);
    }
}