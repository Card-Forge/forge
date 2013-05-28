package forge.control.input;

import java.util.concurrent.CountDownLatch;

import forge.FThreads;
import forge.Singletons;
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
        Singletons.getControl().getInputQueue().removeInput(InputSyncronizedBase.this);
        cdlDone.countDown();
    }
}