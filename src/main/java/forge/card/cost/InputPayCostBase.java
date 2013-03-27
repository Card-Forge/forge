package forge.card.cost;

import forge.control.input.InputPayment;
import forge.control.input.InputSyncronizedBase;

/** 
 * TODO: Write javadoc for this type.
 *
 */
abstract class InputPayCostBase extends InputSyncronizedBase implements InputPayment { 
    private static final long serialVersionUID = -2967434867139585579L;
    boolean bPaid = false;
    
    @Override
    final public void selectButtonCancel() {
        this.cancel();
    }

    final protected void done() {
        bPaid = true;
        this.stop();
    }

    final public void cancel() {
        this.stop();
    }
    
    final public boolean isPaid() { return bPaid; }
}