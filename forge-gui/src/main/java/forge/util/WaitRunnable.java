package forge.util;

import forge.FThreads;

public abstract class WaitRunnable implements Runnable {
    public class Lock {
    }

    private final Lock lock = new Lock();

    public final void invokeAndWait() {
        FThreads.assertExecutedByEdt(false); //not supported if on UI thread
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                WaitRunnable.this.run();
                synchronized(lock) {
                    lock.notify();
                }
            }
        });
        try {
            synchronized(lock) {
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
