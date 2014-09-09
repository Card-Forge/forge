package forge.util;

import com.badlogic.gdx.Gdx;

import forge.FThreads;
import forge.GuiBase;

public abstract class WaitRunnable implements Runnable {
    public class Lock {
    }

    private final Lock lock = new Lock();

    public void invokeAndWait() {
        FThreads.assertExecutedByEdt(GuiBase.getInterface(), false);
        Gdx.app.postRunnable(new Runnable() {
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
