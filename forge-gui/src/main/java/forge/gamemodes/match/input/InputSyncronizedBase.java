package forge.gamemodes.match.input;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.FThreads;
import forge.gui.error.BugReporter;
import forge.player.PlayerControllerHuman;

import java.util.concurrent.CountDownLatch;

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
        NetworkDebugLogger.trace("[InputSyncronizedBase] awaitLatchRelease() starting on %s, thread = %s", this.getClass().getSimpleName(), Thread.currentThread().getName());
        try {
            cdlDone.await();
        } catch (final InterruptedException e) {
            BugReporter.reportException(e);
        }
        NetworkDebugLogger.trace("[InputSyncronizedBase] awaitLatchRelease() UNBLOCKED on %s, thread = %s", this.getClass().getSimpleName(), Thread.currentThread().getName());
    }

    @Override
    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }

    public void showAndWait() {
        getController().getInputQueue().setInput(this);
        awaitLatchRelease();
    }

    @Override
    public final void stop() {
        NetworkDebugLogger.trace("[InputSyncronizedBase] stop() called on %s, latch count before = %d", this.getClass().getSimpleName(), cdlDone.getCount());
        onStop();

        // ensure input won't accept any user actions.
        FThreads.invokeInEdtNowOrLater(this::setFinished);

        // thread irrelevant
        if (getController().getInputQueue().getInput() != null) {
            getController().getInputQueue().removeInput(InputSyncronizedBase.this);
        }
        cdlDone.countDown();
        NetworkDebugLogger.trace("[InputSyncronizedBase] stop() done, latch count after = %d", cdlDone.getCount());
    }

    protected void onStop() { }
}
