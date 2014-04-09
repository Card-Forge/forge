package forge;

import java.io.PrintStream;

import com.badlogic.gdx.Gdx;

import forge.util.ThreadUtil;

public class FThreads {
    private FThreads() { } //don't allow creating instance

    public static void assertExecutedByEdt(final boolean mustBeEDT) {
        if (isGuiThread() != mustBeEDT) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            final String methodName = trace[2].getClassName() + "." + trace[2].getMethodName();
            String modalOperator = mustBeEDT ? " must be" : " may not be";
            throw new IllegalStateException(methodName + modalOperator + " accessed from the event dispatch thread.");
        }
    }

    public static void invokeInEdtLater(Runnable runnable) {
        Gdx.app.postRunnable(runnable);
    }

    public static void invokeInEdtNowOrLater(Runnable proc) {
        if (isGuiThread()) {
            proc.run();
        }
        else {
            invokeInEdtLater(proc);
        }
    }

    public static void invokeInEdtAndWait(final Runnable proc) {
        if (isGuiThread()) {
            // Just run in the current thread.
            proc.run();
        }
        else {
            //TODO
        }
    }

    public static boolean isGuiThread() {
        return !ThreadUtil.isGameThread();
    }

    public static void delayInEDT(int milliseconds, final Runnable inputUpdater) {
        Runnable runInEdt = new Runnable() {
            @Override
            public void run() {
                FThreads.invokeInEdtNowOrLater(inputUpdater);
            }
        };
        ThreadUtil.delay(milliseconds, runInEdt);
    }

    public static String debugGetCurrThreadId() {
        return isGuiThread() ? "EDT" : Thread.currentThread().getName();
    }

    public static String prependThreadId(String message) {
        return debugGetCurrThreadId() + " > " + message;
    }

    public static void dumpStackTrace(PrintStream stream) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        stream.printf("%s > %s called from %s%n", debugGetCurrThreadId(),
                trace[2].getClassName() + "." + trace[2].getMethodName(), trace[3].toString());
        int i = 0;
        for (StackTraceElement se : trace) {
            if (i<2) { i++; }
            else { stream.println(se.toString()); }
        }
    }

    public static String debugGetStackTraceItem(int depth, boolean shorter) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String lastItem = trace[depth].toString();
        if (shorter) {
            int lastPeriod = lastItem.lastIndexOf('.');
            lastPeriod = lastItem.lastIndexOf('.', lastPeriod-1);
            lastPeriod = lastItem.lastIndexOf('.', lastPeriod-1);
            lastItem = lastItem.substring(lastPeriod+1);
            return String.format("%s > from %s", debugGetCurrThreadId(), lastItem);
        }
        return String.format("%s > %s called from %s", debugGetCurrThreadId(),
                trace[2].getClassName() + "." + trace[2].getMethodName(), lastItem);
    }

    public static String debugGetStackTraceItem(int depth) {
        return debugGetStackTraceItem(depth, false);
    }
}
