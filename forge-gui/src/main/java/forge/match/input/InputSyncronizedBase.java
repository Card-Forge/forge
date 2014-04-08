package forge.match.input;

import forge.FThreads;
import forge.GuiBase;

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
            GuiBase.getInterface().reportException(e);
        }
    }

    public final void relaseLatchWhenGameIsOver() {
        cdlDone.countDown();
    }


    public void showAndWait() {
        GuiBase.getInterface().getInputQueue().setInput(this);
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
        GuiBase.getInterface().getInputQueue().removeInput(InputSyncronizedBase.this);
        cdlDone.countDown();
    }

    protected void onStop() { }
}