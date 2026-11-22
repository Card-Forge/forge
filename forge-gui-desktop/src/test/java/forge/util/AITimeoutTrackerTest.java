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
        Assert.assertTrue(AITimeoutTracker.remainingNanos(910L, 5 * SEC) <= 5 * SEC);
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

    @Test
    public void sessionsOfDifferentGamesDoNotShareDeadlinesOrBars() {
        // every game numbers its players from 0: same player id, different game
        final long a = AITimeoutTracker.key(7001, 0);
        final long b = AITimeoutTracker.key(7002, 0);
        Assert.assertNotEquals(a, b);
        AITimeoutTracker.begin(a, 10 * SEC);
        try {
            Assert.assertTrue(AITimeoutTracker.isActive(a));
            Assert.assertFalse(AITimeoutTracker.isActive(b), "a session in game 1 must not be visible in game 2");
            Assert.assertTrue(AITimeoutTracker.deadlineNanos(b, 1 * SEC) - System.nanoTime() <= 1 * SEC,
                    "game 2 must get its own fallback deadline, not game 1's");
        } finally {
            AITimeoutTracker.end(a);
        }
    }

    @Test
    public void idleSlotsArePurgedAndKeysStayUsable() {
        for (int g = 0; g < 500; g++) { // many finished games
            final long k = AITimeoutTracker.key(8000 + g, 1);
            AITimeoutTracker.begin(k, 5 * SEC);
            AITimeoutTracker.end(k);
        }
        final long live = AITimeoutTracker.key(9000, 1);
        AITimeoutTracker.begin(live, 5 * SEC);
        try {
            Assert.assertTrue(AITimeoutTracker.isActive(live));
        } finally {
            AITimeoutTracker.end(live);
        }
    }
}
