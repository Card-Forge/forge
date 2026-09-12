package forge.net;

import forge.gamemodes.net.server.RemoteClient;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Chat is accepted before login and rebroadcast to every peer, so a client
 * that sends it in a loop costs the host and everyone else. The limit is a
 * token bucket sized for a person typing, and switchable off.
 */
public class ChatRateLimitTest {

    /** Far above any legitimate burst, low enough to fail fast. */
    private static final int DRAIN_CEILING = 10_000;

    @Test(timeOut = 30_000)
    public void testChatIsRateLimitedAndRefills() throws Exception {
        final RemoteClient client = new RemoteClient(null);

        // Bounded, not "while (allowChatMessage())". An unbounded drain loop
        // terminates only if the limiter works, so with the limiter removed --
        // the case this exists to detect -- it spins forever and the run hangs
        // instead of failing. A hang reads as "still running", which is worse.
        int drained = 0;
        while (drained < DRAIN_CEILING && client.allowChatMessage()) {
            drained++;
        }
        Assert.assertTrue(drained > 0, "A burst should be allowed - people do type");
        Assert.assertTrue(drained < DRAIN_CEILING,
                "Bucket never emptied after " + DRAIN_CEILING + " messages; not rate limiting");

        // One token per refill interval, so this cannot be shortened much.
        Thread.sleep(1100);
        Assert.assertTrue(client.allowChatMessage(),
                "Allowance must come back - a rate limit that never refills is a mute");
    }

    @Test(timeOut = 30_000)
    public void testLimitCanBeSwitchedOff() {
        System.setProperty("forge.net.chatBurst", "0");
        try {
            final RemoteClient client = new RemoteClient(null);
            for (int i = 0; i < DRAIN_CEILING; i++) {
                Assert.assertTrue(client.allowChatMessage(),
                        "Disabled limit must never refuse (refused at " + i + ")");
            }
        } finally {
            System.clearProperty("forge.net.chatBurst");
        }
    }

}
