package forge.control.input;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.Card;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InputSelectManyCards extends Input {

    private static final long serialVersionUID = -2305549394512889450L;
    
    private List<Card> allowedCards = null;
    private Predicate<Card> allowedFilter = null;
    private final List<Card> selected = new ArrayList<Card>();
    private final Function<List<Card>, Input> onComplete; 
    private final int min;
    private final int max;
    
    private String message = "Source-Card-Name - Select %d more card(s)";
    
    private InputSelectManyCards(int min, int max, final Function<List<Card>, Input> onDone)
    {
        onComplete = onDone;
        if( min > max ) throw new IllegalArgumentException("Min must not be greater than Max");
        this.min = min;
        this.max = max;
    }
    
    public InputSelectManyCards(final List<Card> allowedList, int min, int max, final Function<List<Card>, Input> onDone)
    {
        this(min, max, onDone);
        allowedCards = allowedList;
    }
    
    public InputSelectManyCards(final Predicate<Card> allowedRule, int min, int max, final Function<List<Card>, Input> onDone)
    {
        this(min, max, onDone);
        allowedFilter = allowedRule;
    }    
    
    
    @Override
    public void showMessage() {
        String msgToShow = max == Integer.MAX_VALUE ? String.format(message, selected.size()) : String.format(message, max - selected.size()); 
        CMatchUI.SINGLETON_INSTANCE.showMessage(msgToShow);

        boolean canCancel = min == 0 && selected.isEmpty();
        boolean canOk = min <= selected.size();
            
        if (canOk && canCancel) ButtonUtil.enableAll();
        if (!canOk && canCancel) ButtonUtil.enableOnlyCancel();
        if (canOk && !canCancel) ButtonUtil.enableOnlyOK();
        if (!canOk && !canCancel) ButtonUtil.disableAll();
    }

    @Override
    public void selectButtonOK() {
        this.done();
    }
    
    @Override
    public void selectButtonCancel() {
        this.stop();
    }

    @Override
    public void selectCard(final Card c) {
        if ( selected.contains(c) ) return;
        if ( allowedCards != null && !allowedCards.contains(c)) return;
        if ( allowedFilter != null && !allowedFilter.apply(c)) return;

        this.selected.add(c);
        this.showMessage();
        
        if ( selected.size() == max )
            done();
    }

    public void done() {
        Input next = onComplete.apply(selected);
        if ( null == next )
            this.stop();
        else 
            this.stopSetNext(next);
    }

    @Override
    public void isClassUpdated() {}

    

    public String getMessage() {
        return message;
    }


    public void setMessage(String message0) {
        this.message = message0; // TODO: Add 0 to parameter's name.
    }

}
