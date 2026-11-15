package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.game.Game;
import forge.game.player.Player;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.GuiBase;
import forge.player.PlayerControllerHuman;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
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
    /** Tracked by message, not by id, because a restarted match numbers its questions from the start again. */
    final Set<JsonObject> answered = Collections.newSetFromMap(new IdentityHashMap<>());

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

    /** Connects the host, named Host, and seats it at a new table opened with open ("lobby" or "invite"). */
    TestBrowser hostAt(final String open) throws InterruptedException {
        final TestBrowser host = connect("host");
        sessions.onMessage(host, TestBrowser.action("claimHost"));
        sessions.onMessage(host, message("setName", "name", "Host"));
        send(host, TestBrowser.action(open));
        // A table left by an earlier test can still send messages until the new one opens. Commands are accepted only after that
        final JsonObject opening = host.awaitMatching("hello", h -> "opening".equals(h.get("screen").getAsString()), "the host's table never started opening");
        final JsonObject opened = host.awaitMatching("hello", h -> host.got.indexOf(h) > host.got.indexOf(opening)
                && "setup".equals(h.get("screen").getAsString()), "the host's table never opened");
        host.awaitMatching("lobby", l -> host.got.indexOf(l) > host.got.indexOf(opened) && l.has("table")
                && l.getAsJsonObject("table").get("mySeat").getAsInt() >= 0, "the host never sat at its table");
        return host;
    }

    static void onUi(final Runnable r) {
        GuiBase.getInterface().invokeInEdtAndWait(r);
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

    /** The named person in a presence message, or null if they are not listed. */
    static JsonObject person(final JsonObject presence, final String name) {
        for (final var p : presence.getAsJsonArray("people")) {
            if (name.equals(str(p.getAsJsonObject(), "name"))) {
                return p.getAsJsonObject();
            }
        }
        return null;
    }

    /** Gives each question its default answer until the web seat has priority. Only then has the game dealt its zones and is safe to end. */
    void awaitPriority(final TestBrowser host) {
        final String[] seen = { "no game" };
        // A press sent before the input is ready is lost, so it is sent again
        final long[] lastPress = { 0 };
        awaitTrue(() -> {
            final ServerGameLobby lobby = sessions.hostLobby();
            final Game game = lobby == null || lobby.getHostedMatch() == null ? null : lobby.getHostedMatch().getGame();
            if (game == null) {
                return false;
            }
            for (final JsonObject m : host.got) {
                if ("request".equals(m.get("t").getAsString()) && answered.add(m)) {
                    sessions.onMessage(host, TestBrowser.reply(m.get("id").getAsInt(), m.get("default")));
                }
            }
            for (final Player p : game.getPlayers()) {
                if (p.getController() instanceof PlayerControllerHuman human) {
                    final Object input = human.getInputQueue().getInput();
                    seen[0] = (input == null ? "no input" : input.getClass().getSimpleName()) + ", messages "
                            + host.got.stream().map(m -> m.get("t").getAsString() + (m.has("kind") ? ":" + m.get("kind").getAsString() : "")).distinct().toList()
                            + ", turn " + game.getPhaseHandler().getTurn() + " " + game.getPhaseHandler().getPhase() + ", over " + game.isGameOver();
                    if (input instanceof InputPassPriority) {
                        return true;
                    }
                    if (input != null && System.currentTimeMillis() - lastPress[0] > 500) {
                        lastPress[0] = System.currentTimeMillis();
                        // At a table of more than two the toss winner chooses who goes first. OK does not answer that prompt
                        final JsonObject prompt = host.last("prompt");
                        final JsonArray players = prompt == null ? null : prompt.getAsJsonArray("selectablePlayers");
                        if (players != null && !players.isEmpty()) {
                            sessions.onMessage(host, message("selectPlayer", "key", players.get(0).getAsJsonObject().get("ref").getAsInt()));
                        } else {
                            // Keeping the opening hand is a separate input, answered with OK
                            sessions.onMessage(host, TestBrowser.action("ok"));
                        }
                    }
                }
            }
            return false;
        }, () -> "the web seat never got priority: " + seen[0]);
    }

    /** The names at a table's seats, with null for a seat that has no name. */
    static List<String> seatNames(final JsonObject table) {
        final List<String> out = new ArrayList<>();
        table.getAsJsonArray("seats").forEach(s -> out.add(s.getAsJsonObject().has("name")
                ? s.getAsJsonObject().get("name").getAsString() : null));
        return out;
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
