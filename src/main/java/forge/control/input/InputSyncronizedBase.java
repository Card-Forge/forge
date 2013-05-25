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
    
    
    protected final void stop() {
        setFinished();
        FThreads.invokeInNewThread(new Runnable() {
            @Override
            public void run() {
                getQueue().removeInput(InputSyncronizedBase.this);
                cdlDone.countDown();
            }
        });
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
        onCardSelected(c);
    }

    protected void onCardSelected(Card c) {}
    protected void onCancel() {}
    protected void onOk() {}

}