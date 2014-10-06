package forge.util;

import forge.FThreads;

public abstract class WaitCallback<T> extends Callback<T> implements Runnable {
    public class Lock {
    }

    private final Lock lock = new Lock();

    private T result;

    @Override
    public final void run(T result0) {
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
