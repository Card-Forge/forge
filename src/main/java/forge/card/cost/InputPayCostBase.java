package forge.card.cost;

import forge.control.input.InputSyncronizedBase;

/** 
 * TODO: Write javadoc for this type.
 *
 */
abstract class InputPayCostBase extends InputSyncronizedBase { 
    private static final long serialVersionUID = -2967434867139585579L;
    
    private final CostPayment payment;
    /**
     * TODO: Write javadoc for Constructor.
     * @param cdl
     * @param payment
     */
    public InputPayCostBase(CostPayment payment0) {
        payment = payment0;
    }

    @Override
    final public void selectButtonCancel() {
        this.cancel();
    }

    final protected void done() {
        this.stop();
    }

    final public void cancel() {
        payment.cancelCost();
        this.stop();
    }
}