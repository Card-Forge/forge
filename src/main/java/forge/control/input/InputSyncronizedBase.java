package forge.control.input;

import java.util.concurrent.CountDownLatch;

import forge.Card;
import forge.FThreads;
import forge.error.BugReporter;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized { 
    private static final long serialVersionUID = 8756177361251703052L;
    
    private boolean finished = false;
    private final CountDownLatch cdlDone;

    
    public InputSyncronizedBase() {
        cdlDone = new CountDownLatch(1);
    }
    
    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(false);
        try{
            cdlDone.await();
        } catch (InterruptedException e) {
            BugReporter.reportException(e);
        }
    }
    

    @Override
    protected void afterStop() {
        finished = true;
        cdlDone.countDown();
    }
    

    @Override
    public final void selectButtonCancel() {
        if( finished ) return;
        onCancel();
    }

    @Override
    public final void selectButtonOK() {
        if( finished ) return;
        onOk();
    }

    @Override
    public final void selectCard(Card c, boolean isMetaDown) {
        if( finished ) return;
        onCardSelected(c);
    }

    protected final boolean isFinished() { return finished; }
    protected void onCardSelected(Card c) {}
    protected void onCancel() {}
    protected void onOk() {}

}