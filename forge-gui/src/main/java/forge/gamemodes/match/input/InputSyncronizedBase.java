package forge.gamemodes.match.input;

import java.util.concurrent.CountDownLatch;

import forge.gui.FThreads;
import forge.gui.error.BugReporter;
import forge.player.PlayerControllerHuman;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized {
    private static final long serialVersionUID = 8756177361251703052L;
    private final CountDownLatch cdlDone;

    public InputSyncronizedBase(final PlayerControllerHuman controller) {
        super(controller);
        cdlDone = new CountDownLatch(1);
    }

    @Override
    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(false);
        try {
            cdlDone.await();
        } catch (final InterruptedException e) {
            BugReporter.reportException(e);
        }
    }

    @Override
    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }

    public void showAndWait() {
        getController().getInputQueue().setInput(this);
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
        if (getController().getInputQueue().getInput() != null) {
            getController().getInputQueue().removeInput(InputSyncronizedBase.this);
        }
        cdlDone.countDown();
    }

    protected void onStop() { }
}
