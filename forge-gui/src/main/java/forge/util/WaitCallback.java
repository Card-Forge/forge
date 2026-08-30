package forge.util;

import forge.gui.FThreads;

import java.util.function.Consumer;

public abstract class WaitCallback<T> implements Consumer<T>, Runnable {
    public class Lock {
    }

    private final Lock lock = new Lock();

    private T result;

    @Override
    public final void accept(T result0) {
        result = result0;
        synchronized (lock) {
            lock.notify();
        }
    }

    public final T invokeAndWait() {
        FThreads.assertExecutedByEdt(false); //not supported if on UI thread
        FThreads.invokeInEdtLater(this);
        try {
            synchronized (lock) {
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
