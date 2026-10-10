package forge.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * MyRandom's per-thread seeding contract: seeded streams are isolated
 * between sibling threads, and threads spawned from a seeded thread inherit
 * its stream (so deterministic simulation survives helper threads).
 */
public class MyRandomThreadLocalTest {

    @AfterMethod
    public void restoreUnseeded() {
        MyRandom.setRandom(new SecureRandom());
    }

    @Test
    public void testSeededStreamsAreIsolatedBetweenThreads() throws Exception {
        MyRandom.setRandom(new Random(1));
        AtomicLong otherFirst = new AtomicLong();
        Thread other = new Thread(() -> {
            MyRandom.setRandom(new Random(2));
            otherFirst.set(MyRandom.getRandom().nextLong());
        });
        other.start();
        other.join();

        // this thread's stream is untouched by the sibling's setRandom
        AssertJUnit.assertEquals(new Random(1).nextLong(),
                MyRandom.getRandom().nextLong());
        AssertJUnit.assertEquals(new Random(2).nextLong(), otherFirst.get());
    }

    @Test
    public void testSpawnedThreadInheritsSeededStream() throws Exception {
        Random seeded = new Random(42);
        MyRandom.setRandom(seeded);
        AtomicReference<Random> childSees = new AtomicReference<>();
        Thread child = new Thread(() -> childSees.set(MyRandom.getRandom()));
        child.start();
        child.join();

        // inheritable: the child continues the parent's seeded stream rather
        // than silently falling back to a fresh unseeded SecureRandom
        AssertJUnit.assertSame(seeded, childSees.get());
    }
}
