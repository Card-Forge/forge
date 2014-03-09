package forge.screens.match.input;

import forge.FThreads;
import forge.error.BugReporter;
import forge.screens.match.FControl;

import java.util.concurrent.CountDownLatch;

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

    public void showAndWait() {
        FControl.getInputQueue().setInput(this);
        awaitLatchRelease();
    }
    
    protected final void stop() {
        onStop();

        // ensure input won't accept any user actions.
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                setFinished();
            }
        });

        // thread irrelevant
        FControl.getInputQueue().removeInput(InputSyncronizedBase.this);
        cdlDone.countDown();
    }

    protected void onStop() { }
}