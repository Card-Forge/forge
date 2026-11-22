package forge.net;

import forge.util.LogSafe;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Remote text must not be able to write into the host's log or into another
 * player's chat pane. Chat is accepted before login and rebroadcast to
 * everyone, and the username is echoed into both.
 *
 * <p>The connection cap and the login deadline are not covered here: reaching
 * them needs a raw protocol peer, and such a test would be stress-gated out of
 * CI anyway.
 */
public class AbuseLimitsTest {

    @Test(timeOut = 30_000)
    public void testNeutralisesRemoteText() {
        // A newline forges a log record; forLog keeps it visible as an escape.
        final String forged = "hi\n21:04:11 [INFO ] Server: host granted admin to mallory";
        Assert.assertFalse(LogSafe.forLog(forged).contains("\n"));
        Assert.assertEquals(LogSafe.forLog("a\rb"), "a\\rb");
        Assert.assertEquals(LogSafe.forLog("a\0b"), "a\\u0000b");
        Assert.assertEquals(LogSafe.forLog("a\u009bb"), "a\\u009bb");

        // A UI has no use for these: a carriage return lets one player paint
        // fake system lines in another's chat pane.
        Assert.assertEquals(LogSafe.forDisplay("hi\nthere"), "hithere");
        Assert.assertEquals(LogSafe.forDisplay("a\u0007b"), "ab");

        final String safe = LogSafe.forLog("x".repeat(10_000), 100);
        Assert.assertTrue(safe.length() < 200, "Should be bounded, was " + safe.length());
        Assert.assertTrue(safe.endsWith("[truncated]"), "Truncation should be visible");

        // Over-correction guard: ordinary text must survive untouched.
        Assert.assertEquals(LogSafe.forLog("Alice: nice topdeck"), "Alice: nice topdeck");
        Assert.assertEquals(LogSafe.forDisplay("Alice: nice topdeck"), "Alice: nice topdeck");
        Assert.assertNull(LogSafe.forLog(null));
    }

}
