package forge.card.cost;

import forge.Singletons;
import forge.control.input.InputPayment;
import forge.control.input.InputSyncronizedBase;

/** 
 * TODO: Write javadoc for this type.
 *
 */
abstract class InputPayCostBase extends InputSyncronizedBase implements InputPayment { 
    /**
     * TODO: Write javadoc for Constructor.
     * @param player
     */
    public InputPayCostBase() {
        super(Singletons.getControl().getPlayer());
    }

    private static final long serialVersionUID = -2967434867139585579L;
    boolean bPaid = false;
    
    final protected void done() {
        bPaid = true;
        this.stop();
    }

    @Override
    final protected void onCancel() {
        this.stop();
    }
    
    final public boolean isPaid() { return bPaid; }
}