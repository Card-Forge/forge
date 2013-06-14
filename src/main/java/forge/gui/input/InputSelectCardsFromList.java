package forge.gui.input;

import java.util.List;

import forge.Card;

public class InputSelectCardsFromList extends InputSelectCards {
    private static final long serialVersionUID = 6230360322294805986L;
    
    private final List<Card> validChoices;

    public InputSelectCardsFromList(int min, int max, List<Card> validCards) {
        super(Math.min(min, validCards.size()), Math.min(max, validCards.size())); // to avoid hangs
        this.validChoices = validCards;

        if ( min > validCards.size() )
            System.out.println(String.format("Trying to choose at least %d cards from a list with only %d cards!", min, validCards.size()));
    }
    
    @Override
    protected final boolean isValidChoice(Card choice) {
        return validChoices.contains(choice);
    }
    
}