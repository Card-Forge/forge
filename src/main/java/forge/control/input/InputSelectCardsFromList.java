package forge.control.input;

import java.util.List;

import forge.Card;

public class InputSelectCardsFromList extends InputSelectCards {
    private static final long serialVersionUID = 6230360322294805986L;
    
    private final List<Card> validChoices;

    public InputSelectCardsFromList(int min, int max, List<Card> validCards) {
        super(min, Math.min(max, validCards.size())); // to avoid hangs
        this.validChoices = validCards;
    }
    
    @Override
    protected final boolean isValidChoice(Card choice) {
        return validChoices.contains(choice);
    }
    
}