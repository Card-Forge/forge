package forge.match.input;

import java.util.concurrent.CountDownLatch;

import forge.FThreads;
import forge.error.BugReporter;
import forge.player.PlayerControllerHuman;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized {
    private static final long serialVersionUID = 8756177361251703052L;
    private final CountDownLatch cdlDone;

    public InputSyncronizedBase(final PlayerControllerHuman controller) {
        super(controller);
        cdlDone = new CountDownLatch(1);
    }

    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(getGui(), false);
        try{
            cdlDone.await();
        }
        catch (InterruptedException e) {
            BugReporter.reportException(e, getGui());
        }
    }

    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }

    public void showAndWait() {
        getGameView().getInputQueue().setInput(this);
        awaitLatchRelease();
    }
    
    protected final void stop() {
        onStop();

        // ensure input won't accept any user actions.
        FThreads.invokeInEdtNowOrLater(getGui(), new Runnable() {
            @Override
            public void run() {
                setFinished();
            }
        });

        // thread irrelevant
        getGameView().getInputQueue().removeInput(InputSyncronizedBase.this);
        cdlDone.countDown();
    }

    protected void onStop() { }
}