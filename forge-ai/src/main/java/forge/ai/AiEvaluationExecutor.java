/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;

/**
 * The watchdog boundary an AI controller evaluates its candidates behind.
 *
 * <p>The AI runs a priority decision on a worker thread and waits for it with a timeout, so a
 * pathological board cannot hang the game thread. That boundary is worth keeping; creating and
 * destroying an OS thread for every single priority decision to get it is not. This hands out the
 * same semantics over a pool that keeps idle workers around for a minute, so a game that makes
 * thousands of decisions creates a thread once rather than thousands of times.</p>
 *
 * <h2>Why a shared pool and not one worker per controller</h2>
 *
 * <p>Decisions are not as serial as they look. Every simulated game copy builds its own players and
 * therefore its own controllers, and the AI in a copy takes priority and declares combat while an
 * outer decision is still on the stack. A single worker owned by one controller would deadlock the
 * nested decision behind the outer one; a worker owned by each controller would leave a parked
 * thread behind for every game copy. A pool sized by actual concurrency does neither: the ordinary
 * serial case reuses one thread, and a nested decision gets a second.</p>
 *
 * <p>The pool hands work only to idle workers, so a run that ignores cancellation and keeps its
 * thread forever simply removes that thread from the pool. That is exactly what the old
 * thread-per-decision code did with a stuck evaluation, and it is why reuse cannot let one bad
 * evaluation stall later decisions.</p>
 */
public final class AiEvaluationExecutor {
    /** Kept in step with the old thread name so existing logs and thread dumps still read the same. */
    private static final String THREAD_NAME_PREFIX = "Game AI Eval";
    /** How long an idle worker is kept before it is allowed to exit. */
    private static final long IDLE_TIMEOUT_SECONDS = 60L;

    private static final AtomicInteger THREAD_INDEX = new AtomicInteger();

    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS,
            // A synchronous queue is what makes this grow on demand: a task is either taken by an
            // idle worker immediately or a new worker is started for it, and it is never left
            // waiting behind a busy one.
            new SynchronousQueue<>(),
            runnable -> {
                final Thread t = new Thread(runnable, THREAD_NAME_PREFIX + "-" + THREAD_INDEX.incrementAndGet());
                // The game must be able to exit while an evaluation is stuck.
                t.setDaemon(true);
                return t;
            });

    private AiEvaluationExecutor() {
    }

    /**
     * Runs {@code body} on a worker.
     *
     * @return a handle for waiting on, cancelling and diagnosing the run
     */
    public static <T> Evaluation<T> submit(final Callable<T> body) {
        return new Evaluation<>(body);
    }

    /** Number of pooled workers currently alive. Diagnostics and tests only. */
    public static int getWorkerCount() {
        return WORKERS.getPoolSize();
    }

    /** One submitted evaluation. */
    public static final class Evaluation<T> {
        private final CountDownLatch finished = new CountDownLatch(1);
        private final Future<T> future;
        private volatile Thread runningThread;

        private Evaluation(final Callable<T> body) {
            future = WORKERS.submit(() -> {
                runningThread = Thread.currentThread();
                try {
                    return body.call();
                } finally {
                    runningThread = null;
                    finished.countDown();
                }
            });
        }

        /** As {@link Future#get(long, TimeUnit)}. */
        public T get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }

        /**
         * The thread the body is running on, or {@code null} if it has not started or has finished.
         * For diagnostics only — a stack trace taken from it is a best-effort sample.
         */
        public Thread getRunningThread() {
            return runningThread;
        }

        /** Interrupts the run and asks it to stop. Does not wait. */
        public void cancel() {
            future.cancel(true);
        }

        /**
         * Waits for the body to actually return, whether it completed or unwound.
         *
         * <p>Distinct from {@link Future#get}, which reports a cancelled task as finished the moment
         * it is cancelled even though the body is still running on the worker.</p>
         *
         * @return {@code true} if the body is no longer running
         */
        public boolean awaitCompletion(final long timeout, final TimeUnit unit) {
            try {
                return finished.await(timeout, unit);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return finished.getCount() == 0L;
            }
        }

        /**
         * Last resort for a run that ignored cancellation: stop the thread if the JVM still permits
         * it. If it does not, the thread is left running and simply never returns to the pool.
         */
        public void abandon() {
            PerfProbe.count(PerfCounter.EVAL_WORKERS_ABANDONED);
            final Thread stuck = runningThread;
            if (stuck != null) {
                // see #8302: the evaluation may be stuck inside a single ability check or an
                // infinite loop and never reach the cooperative exit
                try {
                    stuck.stop();
                } catch (final UnsupportedOperationException | NoSuchMethodError ex) {
                    // Stop support: dropped by Android and Java 20 / 26 removed it completely - so sadly thread will keep running
                }
            }
        }
    }
}
