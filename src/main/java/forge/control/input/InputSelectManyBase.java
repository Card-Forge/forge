package forge.control.input;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.GameEntity;
import forge.view.ButtonUtil;

public abstract class InputSelectManyBase<T extends GameEntity> extends InputSyncronizedBase implements InputSelectMany<T> {

    private static final long serialVersionUID = -2305549394512889450L;

    protected final List<T> selected;
    protected boolean bCancelled = false;
    protected final int min;
    protected final int max;
    public boolean allowUnselect = false;
    private boolean allowCancel = false;
    
    private String message = "Source-Card-Name - Select %d more card(s)";



    protected InputSelectManyBase(int min, int max) {
        selected = new ArrayList<T>();
        if (min > max) {
            throw new IllegalArgumentException("Min must not be greater than Max");
        }
        this.min = min;
        this.max = max;
    }

    protected void refresh() {
        if (hasAllTargets()) {
            selectButtonOK();
        } else {
            this.showMessage();
        }
    }
    
    @Override
    public final void showMessage() {
        showMessage(getMessage());

        boolean canCancel = (min == 0 && selected.isEmpty()) || allowCancel;
        boolean canOk = hasEnoughTargets();

        if (canOk && canCancel) {
            ButtonUtil.enableAllFocusOk();
        }
        if (!canOk && canCancel) {
            ButtonUtil.enableOnlyCancel();
        }
        if (canOk && !canCancel) {
            ButtonUtil.enableOnlyOk();
        }
        if (!canOk && !canCancel) {
            ButtonUtil.disableAll();
        }
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
        bCancelled = true;
        this.stop();
    }

    @Override
    public final boolean hasCancelled() {
        return bCancelled;
    }

    @Override
    public final List<T> getSelected() {
        return selected;
    }

    @Override
    public final void selectButtonOK() {
        this.stop();
    }

    public void setMessage(String message0) {
        this.message = message0; // TODO: Add 0 to parameter's name.
    }

    // must define these
    protected abstract boolean isValidChoice(T choice);

    // might re-define later
    protected boolean hasEnoughTargets() { return selected.size() >= min; }
    protected boolean hasAllTargets() { return selected.size() >= max; }

    protected boolean selectEntity(T c) {
        if (!isValidChoice(c)) {
            return false;
        }
        
        if ( selected.contains(c)  ) {
            if ( allowUnselect ) { 
                this.selected.remove(c);
                onSelectStateChanged(c, false);
            } else 
                return false;
        } else {
            this.selected.add(c);
            onSelectStateChanged(c, true);
        }
        return true;
    }


    protected void onSelectStateChanged(T c, boolean newState) {
        if( c instanceof Card )
            ((Card)c).setUsedToPay(newState); // UI supports card highlighting though this abstraction-breaking mechanism
    } 


    protected void afterStop() {
        for(T c : selected)
            if( c instanceof Card)
                ((Card)c).setUsedToPay(false);

        super.afterStop(); // It's ultimatelly important to keep call to super class!
    
   }



    public final boolean isUnselectAllowed() { return allowUnselect; }
    public final void setUnselectAllowed(boolean allow) { this.allowUnselect = allow; }

    public final void setCancelAllowed(boolean allow) { this.allowCancel = allow ; }
}
