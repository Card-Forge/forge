package forge.web;

import com.google.gson.JsonObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** One server runs for the whole class, because a restart between tests races with the server's own shutdown. */
abstract class WebHubTest {
    WebHub sessions;
    final List<TestBrowser> browsers = new CopyOnWriteArrayList<>();

    @BeforeClass
    public void startSessions() {
        WebTestSupport.initModel();
        sessions = new WebHub(gui(), 120_000, () -> { });
    }

    /** The GUI the sessions run on. It is their own, unless a test needs core's questions sent to the browser. */
    WebGuiBase gui() {
        return new WebGuiBase();
    }

    @AfterMethod(alwaysRun = true)
    public void disconnectBrowsers() {
        for (final TestBrowser browser : browsers) {
            sessions.disconnected(browser);
        }
        browsers.clear();
        afterDisconnecting();
    }

    /** Clears what a test leaves behind besides its browsers. It runs after the browsers disconnect. */
    void afterDisconnecting() {
    }

    @AfterClass(alwaysRun = true)
    public void stopSessions() {
        if (sessions != null) {
            sessions.shutdown();
            sessions = null;
        }
    }

    /** Connects a browser. The id "host" uses the host's link, and any other id uses a guest's link. */
    TestBrowser connect(final String id) {
        final TestBrowser browser = new TestBrowser();
        browsers.add(browser);
        sessions.connected(browser, id, "host".equals(id));
        return browser;
    }

    /** Clears the browser's received messages, then sends its message. A wait that follows sees only the replies to this message. */
    void send(final TestBrowser browser, final JsonObject message) {
        browser.forget();
        sessions.onMessage(browser, message);
    }

    /** Waits until the condition holds. The game thread deals the zones after the match starts, so its setup must be waited for. */
    static void awaitTrue(final BooleanSupplier wanted, final String why) {
        awaitTrue(wanted, () -> why);
    }

    static void awaitTrue(final BooleanSupplier wanted, final Supplier<String> why) {
        for (int i = 0; i < 6000 && !wanted.getAsBoolean(); i++) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(wanted.getAsBoolean(), why.get());
    }

    static String str(final JsonObject o, final String field) {
        return o.has(field) && !o.get(field).isJsonNull() ? o.get(field).getAsString() : null;
    }

    /** Builds a message of the given type from name, value pairs. A field with a null value is left out. */
    static JsonObject message(final String type, final Object... fields) {
        final JsonObject m = TestBrowser.action(type);
        for (int i = 0; i < fields.length; i += 2) {
            final Object value = fields[i + 1];
            if (value instanceof Number n) {
                m.addProperty((String) fields[i], n);
            } else if (value instanceof Boolean b) {
                m.addProperty((String) fields[i], b);
            } else if (value != null) {
                m.addProperty((String) fields[i], (String) value);
            }
        }
        return m;
    }
}
