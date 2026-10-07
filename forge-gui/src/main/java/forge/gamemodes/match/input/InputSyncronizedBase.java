package forge.gamemodes.match.input;

import forge.game.UndoHistory;
import forge.game.UndoRequestedException;
import forge.util.IHasForgeLog;
import forge.gui.FThreads;
import forge.gui.error.BugReporter;
import forge.player.PlayerControllerHuman;

import java.util.concurrent.CountDownLatch;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized, IHasForgeLog {

    private static final long serialVersionUID = 8756177361251703052L;
    private final CountDownLatch cdlDone;

    public InputSyncronizedBase(final PlayerControllerHuman controller) {
        super(controller);
        cdlDone = new CountDownLatch(1);
    }

    @Override
    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(false);
        netLog.trace("awaitLatchRelease() starting on {}, thread = {}", this.getClass().getSimpleName(), Thread.currentThread().getName());
        try {
            getController().getGui().awaitInput(cdlDone);
        } catch (final InterruptedException e) {
            BugReporter.reportException(e);
        }
        netLog.trace("awaitLatchRelease() UNBLOCKED on {}, thread = {}", this.getClass().getSimpleName(), Thread.currentThread().getName());
    }

    @Override
    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }

    public void showAndWait() {
        throwIfUndoAgreed();
        getController().getInputQueue().setInput(this);
        awaitLatchRelease();
        throwIfUndoAgreed();
    }

    /**
     * Whether this prompt is one of the points a player can undo back to (see {@link UndoHistory}).
     * Only these give way to an agreed undo: elsewhere the game is in the middle of something.
     */
    public boolean isUndoPoint() {
        return false;
    }

    private void throwIfUndoAgreed() {
        if (isUndoPoint() && getController().getGame().getUndoHistory().hasPendingUndo()) {
            throw new UndoRequestedException();
        }
    }

    @Override
    public final void stop() {
        netLog.trace("stop() called on {}, latch count before = {}", this.getClass().getSimpleName(), cdlDone.getCount());
        onStop();

        // ensure input won't accept any user actions.
        FThreads.invokeInEdtNowOrLater(this::setFinished);

        // thread irrelevant
        if (getController().getInputQueue().getInput() != null) {
            getController().getInputQueue().removeInput(InputSyncronizedBase.this);
        }
        cdlDone.countDown();
        netLog.trace("stop() done, latch count after = {}", cdlDone.getCount());
    }

    protected void onStop() { }
}
