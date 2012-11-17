package forge.control.input;

import java.util.ArrayList;
import java.util.List;

import forge.GameEntity;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class InputSelectMany<T extends GameEntity> extends Input {

    private static final long serialVersionUID = -2305549394512889450L;
    
    protected final List<T> selected = new ArrayList<T>();
    protected final int min;
    protected final int max;
    
    private String message = "Source-Card-Name - Select %d more card(s)";
    
    protected InputSelectMany(int min, int max)
    {
        if( min > max ) throw new IllegalArgumentException("Min must not be greater than Max");
        this.min = min;
        this.max = max;
    }
    
    @Override
    public final void showMessage() {
        String msgToShow = getMessage(); 
        CMatchUI.SINGLETON_INSTANCE.showMessage(msgToShow);

        boolean canCancel = (min == 0 && selected.isEmpty()) || canCancelWithSomethingSelected();
        boolean canOk = hasEnoughTargets();
            
        if (canOk && canCancel) ButtonUtil.enableAll();
        if (!canOk && canCancel) ButtonUtil.enableOnlyCancel();
        if (canOk && !canCancel) ButtonUtil.enableOnlyOK();
        if (!canOk && !canCancel) ButtonUtil.disableAll();
    }

    
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    protected  String getMessage() {
        return max == Integer.MAX_VALUE
            ? String.format(message, selected.size()) 
            : String.format(message, max - selected.size());
    }

    @Override
    public final void selectButtonCancel() {
        // this.stop();
        Input next = onCancel(); // might add ability to stack from here
        // if ( next != null ) {
        //   Singletons.getModel().getMatch().getInput().setInput(next);
        // }
        
        if ( null == next )
            this.stop();
        else 
            this.stopSetNext(next);
        
        // for a next use
        selected.clear();
    }

    @Override
    public final void selectButtonOK() {
        // should check here if it still gets into an infinite loop 
        // if an ability is put on stack before this input is stopped;
        // if it does, uncomment the 5 lines below, use them as method body
        
        // this.stop();
        Input next = onDone(); // might add ability to stack from here
        // if ( next != null ) {
        //   Singletons.getModel().getMatch().getInput().setInput(next);
        // }
        
        if ( null == next )
            this.stop();
        else 
            this.stopSetNext(next);
        
        // for a next use
        selected.clear();
    }

    @Override
    public void isClassUpdated() {}

    public void setMessage(String message0) {
        this.message = message0; // TODO: Add 0 to parameter's name.
    }
    
    
    // must define these
    protected abstract Input onDone();
    protected abstract boolean isValidChoice(T choice);
    
    // might re-define later
    protected Input onCancel() { return null; }
    protected boolean canCancelWithSomethingSelected() { return false; }
    protected boolean hasEnoughTargets() { return selected.size() >= min; }
    protected boolean hasAllTargets() { return selected.size() >= max; }
    
    
    protected void selectEntity(T c)
    {
        if ( selected.contains(c) || !isValidChoice(c) ) return;
        
        this.selected.add(c);
        this.showMessage();
        
        if ( hasAllTargets() )
            selectButtonOK();
    }
    
    
    
    
}
