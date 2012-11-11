package forge.control.input;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.Card;

public class InputSelectManyCards extends InputSelectMany<Card> {
    private static final long serialVersionUID = -6609493252672573139L;

    protected List<Card> allowedCards = null;
    protected Predicate<Card> allowedFilter = null;
    
    protected final Function<List<Card>, Input> onComplete; 


//    public InputSelectManyCards(final List<Card> allowedList, int min, int max, final Function<List<T>, Input> onDone)
//    {
//        super(min, max, onDone);
//        allowedCards = allowedList;
//    }
//    
    public InputSelectManyCards(final Predicate<Card> allowedRule, int min, int max, final Function<List<Card>, Input> onDone)
    {
        super(min, max);
        allowedFilter = allowedRule;
        onComplete = onDone;
    }        
    
    @Override
    public void selectCard(final Card c) {
        selectEntity(c);
    }
    
    protected boolean isValidChoice(Card choice) {
        if ( allowedCards != null && !allowedCards.contains(choice)) return false;
        if ( allowedFilter != null && !allowedFilter.apply(choice)) return false;
        return true;
    }
    
    protected Input onDone(){
        return onComplete.apply(selected);
    }
}