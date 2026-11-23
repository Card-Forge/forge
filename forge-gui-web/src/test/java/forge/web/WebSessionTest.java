package forge.web;

import com.google.gson.JsonObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class WebSessionTest {
    /** Each has its quit scheduled until it is shut down. */
    private final List<WebHub> opened = new ArrayList<>();

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    @AfterMethod(alwaysRun = true)
    public void shutDown() {
        opened.forEach(WebHub::shutdown);
        opened.clear();
    }

    private WebHub opened() {
        final WebHub sessions = new WebHub(new WebGuiBase(), 60_000, () -> { });
        opened.add(sessions);
        return sessions;
    }

    // Fails if a browser gets the host's seat just by connecting, or if two browsers can both hold it
    @Test
    public void theFirstBrowserToAskGetsTheHostSeat() throws Exception {
        final WebHub sessions = opened();
        final TestBrowser first = new TestBrowser();
        final TestBrowser second = new TestBrowser();
        sessions.connected(first, "first", true);
        sessions.connected(second, "second", true);
        Assert.assertFalse(first.awaitNewest("hello").get("host").getAsBoolean(), "a browser hosted by arriving");
        Assert.assertTrue(first.awaitNewest("hello").get("canClaimHost").getAsBoolean(), "the seat was not offered");

        sessions.onMessage(first, TestBrowser.action("claimHost"));
        Assert.assertTrue(first.awaitNewest("hello").get("host").getAsBoolean(), "asking did not take the seat");

        sessions.onMessage(second, TestBrowser.action("claimHost"));
        Assert.assertFalse(second.awaitNewest("hello").get("host").getAsBoolean(), "two browsers took the same seat");
        Assert.assertNotNull(second.awaitMatching("error", m -> true), "the second browser was not told why");
    }

    private static JsonObject claim(final String password) {
        final JsonObject m = TestBrowser.action("claimHost");
        m.addProperty("password", password);
        return m;
    }

    // Fails if a browser on another machine can take the host's seat, which can stop the server or set up the table, without the host password once one is set, if this machine's own browser has to give it, or if another machine has to when none is set
    @Test
    public void theHostPasswordGuardsTheSeatOnlyFromAnotherMachine() throws Exception {
        ServerControl.hostPassword("open sesame");
        try {
            final WebHub sessions = opened();
            final TestBrowser remote = new TestBrowser();
            sessions.connected(remote, "remote", false);
            Assert.assertTrue(remote.awaitNewest("hello").get("hostPassword").getAsBoolean(), "the page was not told to ask for the password");
            sessions.onMessage(remote, claim("wrong"));
            Assert.assertNotNull(remote.awaitMatching("error", m -> true), "a wrong password was not refused");
            Assert.assertFalse(remote.awaitNewest("hello").get("host").getAsBoolean(), "a wrong password took the host's seat");
            sessions.onMessage(remote, claim("open sesame"));
            Assert.assertNotNull(remote.awaitNewest("hello", m -> m.get("host").getAsBoolean()), "the right password did not take the host's seat");

            final WebHub guarded = opened();
            final TestBrowser local = new TestBrowser();
            guarded.connected(local, "local", true);
            guarded.onMessage(local, TestBrowser.action("claimHost"));
            Assert.assertFalse(local.awaitNewest("hello").get("hostPassword").getAsBoolean());
            Assert.assertTrue(local.awaitNewest("hello").get("host").getAsBoolean(), "this machine's browser needed the password");
        } finally {
            ServerControl.hostPassword("");
        }
        final WebHub unguarded = opened();
        final TestBrowser remote = new TestBrowser();
        unguarded.connected(remote, "remote", false);
        unguarded.onMessage(remote, TestBrowser.action("claimHost"));
        Assert.assertNotNull(remote.awaitNewest("hello", m -> m.get("host").getAsBoolean()), "another machine could not take the seat with no password set");
    }

    // Fails if anyone with a link can make the server track an unlimited number of browsers
    @Test
    public void browsersBeyondTheLimitAreTurnedAwayUntilOthersLeave() {
        final WebHub sessions = opened();
        final List<TestBrowser> attached = new ArrayList<>();
        for (int i = 0; i < WebHub.MOST_SESSIONS; i++) {
            final TestBrowser r = new TestBrowser();
            sessions.connected(r, "b" + i, false);
            attached.add(r);
        }
        final TestBrowser oneTooMany = new TestBrowser();
        sessions.connected(oneTooMany, "extra", false);
        Assert.assertTrue(oneTooMany.closed, "a browser past the limit was let in");
        Assert.assertFalse(attached.get(0).closed);

        // A browser that has disconnected and holds nothing frees a place
        sessions.disconnected(attached.get(0));
        final TestBrowser next = new TestBrowser();
        sessions.connected(next, "extra", false);
        Assert.assertFalse(next.closed, "a browser was turned away although another had left");
    }

    // Fails if two tabs of one browser can both act for its seat, or the old tab's closing detaches the new one
    @Test
    public void aSecondTabClosesTheFirst() {
        final WebHub sessions = opened();
        final TestBrowser first = new TestBrowser();
        final TestBrowser second = new TestBrowser();
        sessions.connected(first, "same", true);
        sessions.connected(second, "same", true);
        Assert.assertTrue(first.closed, "the first tab stayed open");
        sessions.onMessage(first, TestBrowser.action("claimHost"));
        Assert.assertTrue(sessions.hostSeatFree(), "the closed tab took the host's seat");
        sessions.disconnected(first);
        Assert.assertEquals(sessions.playersHere(), 1, "the old tab's closing detached the new one");
    }
}
