package forge.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class AITimeoutTrackerTest {
    private static final long SEC = TimeUnit.SECONDS.toNanos(1);

    @Test
    public void checkIsNoOpWhenNotArmed() {
        AITimeoutTracker.check();
        Assert.assertEquals(AITimeoutTracker.remainingNanos(), Long.MAX_VALUE);
    }

    @Test(expectedExceptions = AITimeoutTracker.Expired.class)
    public void checkThrowsAfterDeadline() {
        final Object t = AITimeoutTracker.arm(System.nanoTime() - 1, false);
        try {
            AITimeoutTracker.check();
        } finally {
            AITimeoutTracker.disarm(t);
        }
    }

    @Test
    public void disarmRestoresPreviousState() {
        final Object t = AITimeoutTracker.arm(System.nanoTime() - 1, false);
        AITimeoutTracker.disarm(t);
        AITimeoutTracker.check(); // must not throw
    }

    @Test
    public void nestedArmCanOnlyTighten() {
        final long now = System.nanoTime();
        final Object outer = AITimeoutTracker.arm(now + 10 * SEC, false);
        final Object inner = AITimeoutTracker.arm(now + 60 * SEC, false);
        try {
            Assert.assertTrue(AITimeoutTracker.remainingNanos() <= 10 * SEC);
        } finally {
            AITimeoutTracker.disarm(inner);
            AITimeoutTracker.disarm(outer);
        }
        Assert.assertEquals(AITimeoutTracker.remainingNanos(), Long.MAX_VALUE);
    }

    @Test
    public void interruptIsHonouredOnlyWhenRequested() {
        final Object t = AITimeoutTracker.arm(System.nanoTime() + 10 * SEC, true);
        Thread.currentThread().interrupt();
        try {
            AITimeoutTracker.check();
            Assert.fail("expected Expired");
        } catch (AITimeoutTracker.Expired expected) {
            // ok
        } finally {
            Thread.interrupted(); // clear the flag for other tests
            AITimeoutTracker.disarm(t);
        }
    }

    @Test
    public void unarmedCallerGetsABoundedWait() {
        Assert.assertTrue(AITimeoutTracker.remainingNanos(910, 5 * SEC) <= 5 * SEC);
    }

    @Test
    public void barIsHiddenBeforeShowThresholdAndAfterEnd() {
        AITimeoutTracker.begin(911, 10 * SEC);
        Assert.assertTrue(AITimeoutTracker.isActive(911));
        Assert.assertTrue(AITimeoutTracker.getRemainingFraction(911) < 0f);
        AITimeoutTracker.end(911);
        Assert.assertFalse(AITimeoutTracker.isActive(911));
    }

    @Test
    public void leakedSessionDoesNotPoisonNextOne() throws Exception {
        final long old = AITimeoutTracker.staleGraceNanos;
        AITimeoutTracker.staleGraceNanos = TimeUnit.MILLISECONDS.toNanos(50);
        try {
            AITimeoutTracker.begin(912, TimeUnit.MILLISECONDS.toNanos(10)); // never ended
            Thread.sleep(150);
            AITimeoutTracker.begin(912, 5 * SEC);
            try {
                Assert.assertTrue(AITimeoutTracker.deadlineNanos(912, 0) - System.nanoTime() > 4 * SEC);
            } finally {
                AITimeoutTracker.end(912);
            }
        } finally {
            AITimeoutTracker.staleGraceNanos = old;
        }
    }

    @Test
    public void workerThreadStopsAtDeadline() throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
        final boolean[] stopped = {false};
        final Thread w = new Thread(() -> {
            final Object t = AITimeoutTracker.arm(deadline, true);
            try {
                while (true) { AITimeoutTracker.check(); }
            } catch (AITimeoutTracker.Expired e) {
                stopped[0] = true;
            } finally {
                AITimeoutTracker.disarm(t);
            }
        });
        w.start();
        w.join(2000);
        Assert.assertTrue(stopped[0], "busy loop with check() must abort at the deadline");
    }
}