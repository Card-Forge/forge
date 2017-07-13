package forge.match.input;

import java.util.concurrent.LinkedBlockingDeque;

import com.google.common.base.Function;
import forge.FThreads;
import forge.error.BugReporter;
import forge.player.PlayerControllerHuman;
import forge.util.ThreadUtil;

public abstract class InputSyncronizedBase extends InputBase implements InputSynchronized {
    private static final long serialVersionUID = 8756177361251703052L;
    private static final Runnable terminationMarker = new Runnable() { public void run() { } };

    // The gameTaskQueue indicates tasks to run while blocked on the game. To stop, add terminationMarker (as null is
    // not allowed).
    private LinkedBlockingDeque<Runnable> gameTaskQueue = new LinkedBlockingDeque<Runnable>();

    public InputSyncronizedBase(final PlayerControllerHuman controller) {
        super(controller);
    }

    @Override
    public void awaitLatchRelease() {
        FThreads.assertExecutedByEdt(false);
        try {
            Runnable r = gameTaskQueue.take();
            while (r != terminationMarker) {
                r.run();
                r = gameTaskQueue.take();
            }
        } catch (final InterruptedException e) {
            BugReporter.reportException(e);
        }
    }

    @Override
    public final void relaseLatchWhenGameIsOver() {
        gameTaskQueue.add(terminationMarker);
    }

    public void showAndWait() {
        final boolean isGameThread = ThreadUtil.isGameThread();

        if (isGameThread) {
            // If we're on the game thread, redirect the "run on game thread" function to instead go through us.
            getController().getGame().getAction().setInvokeFunction(new Function<Runnable, Void>() {
                public Void apply(Runnable r) {
                    gameTaskQueue.add(r);
                    return null;
                }
            });
        }

        getController().getInputQueue().setInput(this);
        awaitLatchRelease();

        if (isGameThread) {
            // Reset the invoke function to null.
            getController().getGame().getAction().setInvokeFunction(null);
            // There's a race that a new task may be queued up before we've reset the invoke function.
            // To handle this, schedule any remaining tasks normally.
            for (Runnable r : gameTaskQueue) {
                getController().getGame().getAction().invoke(r);
            }
        }
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
        gameTaskQueue.add(terminationMarker);
    }

    protected void onStop() { }
}
