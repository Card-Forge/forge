package forge.trackable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Regression test for https://github.com/Card-Forge/forge/issues/11535.
 *
 * <p>The netplay delta sync reads a tracker's delayed prop changes via
 * {@link Tracker#getDelayedPropsFor} from outside the game thread while the
 * game thread mutates the queue through {@link Tracker#addDelayedPropChange}
 * and {@link Tracker#clearDelayed}. Before the queue accessors were
 * synchronized, this threw ConcurrentModificationException and killed the
 * host's Event Dispatch Thread mid-game.
 */
public class TrackerConcurrencyTest {

    private static final class DummyObject extends TrackableObject {
        DummyObject(final int id, final Tracker tracker) {
            super(id, tracker);
        }
    }

    @Test
    public void testGetDelayedPropsForSafeDuringConcurrentMutation() throws InterruptedException {
        final Tracker tracker = new Tracker();
        final TrackableObject obj = new DummyObject(1, tracker);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean writerDone = new AtomicBoolean(false);
        final CountDownLatch start = new CountDownLatch(2);

        // The crash happened inside a freeze bracket — delta sync only reads
        // delayed props while the tracker is frozen.
        tracker.freeze();

        final Thread writer = new Thread(() -> {
            try {
                start.countDown();
                start.await();
                final long deadline = System.nanoTime() + 1_500_000_000L;
                long i = 0;
                while (System.nanoTime() < deadline) {
                    tracker.addDelayedPropChange(obj, TrackableProperty.Life, (int) (i++ % 40));
                    if (i % 25 == 0) {
                        tracker.clearDelayed();
                    }
                }
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                writerDone.set(true);
            }
        }, "game-thread");

        final Thread reader = new Thread(() -> {
            try {
                start.countDown();
                start.await();
                while (!writerDone.get()) {
                    tracker.getDelayedPropsFor(obj);
                }
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            }
        }, "sync-thread");

        writer.start();
        reader.start();
        writer.join(10_000);
        reader.join(10_000);

        final Throwable thrown = failure.get();
        if (thrown != null) {
            AssertJUnit.fail("Concurrent delayed-prop access failed: " + thrown);
        }
    }
}
