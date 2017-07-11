package forge.match.input;

import java.util.concurrent.CountDownLatch;

import forge.FThreads;
import forge.error.BugReporter;
import forge.game.Game;
import forge.player.PlayerControllerHuman;
import forge.util.ThreadUtil;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized {
    private static final long serialVersionUID = 8756177361251703052L;
    private CountDownLatch cdlDone;

    private boolean waitingOnGameThread;
    private Runnable runnableToRun;

    public InputSyncronizedBase(final PlayerControllerHuman controller) {
        super(controller);
        reset();
    }

    private void reset() {
        cdlDone = new CountDownLatch(1);
    }

    @Override
    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(false);
        try{
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
        waitingOnGameThread = ThreadUtil.isGameThread();

        getController().getInputQueue().setInput(this);
        awaitLatchRelease();

        while (runnableToRun != null) {
            Runnable r = runnableToRun;
            runnableToRun = null;

            reset();
            r.run();

            awaitLatchRelease();
        }
    }

    protected final void runOnGameThread(Game game, Runnable r) {
        if (waitingOnGameThread) {
            runnableToRun = r;
            cdlDone.countDown();
            return;
        }

        game.getAction().invoke(r);
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
