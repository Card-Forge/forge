package forge.card.cost;

import java.util.concurrent.CountDownLatch;

import forge.control.input.InputBase;

/** 
 * TODO: Write javadoc for this type.
 *
 */
abstract class InputPayCostBase extends InputBase { 
    private static final long serialVersionUID = -2967434867139585579L;
    
    private final CountDownLatch cdlDone;
    private final CostPayment payment;
    /**
     * TODO: Write javadoc for Constructor.
     * @param cdl
     * @param payment
     */
    public InputPayCostBase(CountDownLatch cdl, CostPayment payment0) {
        cdlDone = cdl;
        payment = payment0;
    }

    @Override
    final public void selectButtonCancel() {
        this.cancel();
    }

    final protected void done() {
        this.stop();
        cdlDone.countDown();
    }

    final public void cancel() {
        this.stop();
        payment.cancelCost();
        cdlDone.countDown();
    }
}