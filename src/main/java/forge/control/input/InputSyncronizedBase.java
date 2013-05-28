package forge.control.input;

import java.util.concurrent.CountDownLatch;

import forge.Card;
import forge.FThreads;
import forge.error.BugReporter;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized { 
    private static final long serialVersionUID = 8756177361251703052L;
    

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
    
    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }
    
    protected final void stop() {
        // ensure input won't accept any user actions.
        FThreads.invokeInEdtNowOrLater(new Runnable() { @Override public void run() { setFinished(); } });

        // thread irrelevant 
        getQueue().removeInput(InputSyncronizedBase.this);
        cdlDone.countDown();
    }

    @Override
    public final void selectButtonCancel() {
        if( isFinished() ) return;
        onCancel();
    }

    @Override
    public final void selectButtonOK() {
        if( isFinished() ) return;
        onOk();
    }

    @Override
    public final void selectCard(Card c, boolean isMetaDown) {
        if( isFinished() ) return;
        onCardSelected(c, isMetaDown);
    }

    protected void onCardSelected(Card c, boolean isRmb) {}
    protected void onCancel() {}
    protected void onOk() {}

}