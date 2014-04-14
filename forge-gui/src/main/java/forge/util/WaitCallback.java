package forge.util;

import forge.FThreads;

public abstract class WaitCallback<T> extends Callback<T> implements Runnable {
    private T result;

    @Override
    public final void run(T result0) {
        result = result0;
    }

    public final T invokeAndWait() {
        FThreads.assertExecutedByEdt(false); //not supported if on UI thread
        FThreads.invokeInEdtAndWait(this);
        return result;
    }
}
