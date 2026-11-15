package forge.web;

import com.google.gson.JsonObject;
import forge.web.protocol.BrowserChannel;
import org.testng.Assert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A fake browser. It records every message the server sends and the game they describe, and can answer for the web seat. */
final class TestBrowser implements BrowserChannel {
    private static final int WAIT_MILLIS = 60_000;
    private static final int POLL_MILLIS = 10;

    final List<JsonObject> got = new CopyOnWriteArrayList<>();
    /** Whether the server closed this browser's connection, as it does for a connection over its limit. */
    volatile boolean closed;
    /** The newest hello, which shows the session's current state. {@link #forget} does not clear it. */
    volatile JsonObject hello;

    /** A browser that only records messages. */
    TestBrowser() {
        this(null, false, false, 0);
    }

    @Override
    public void send(final JsonObject m) {
        got.add(m);
        switch (m.get("t").getAsString()) {
            case "hello" -> hello = m;
            case "decks" -> decks = m;
            case "state" -> {
                model.applyStateMessage(m);
                root = m.get("root").getAsInt();
                localPlayers = m.getAsJsonArray("localPlayers");
            }
            case "gameOver" -> gameOver.countDown();
            case "request" -> {
                if (gui != null) {
                    onRequest(m);
                }
            }
            case "prompt" -> {
                if (plays) {
                    lastPrompt = m;
                    retries.set(0);
                    actOnLatestPrompt();
                }
            }
            default -> { }
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    List<JsonObject> all(final String type) {
        return got.stream().filter(m -> type.equals(m.get("t").getAsString())).collect(Collectors.toList());
    }

    JsonObject last(final String type) {
        final List<JsonObject> matches = all(type);
        return matches.isEmpty() ? null : matches.get(matches.size() - 1);
    }

    /** The earliest message of the type that matches wanted, or null if none arrived in time. */
    JsonObject awaitMatching(final String type, final Predicate<JsonObject> wanted) throws InterruptedException {
        for (int i = 0; i < WAIT_MILLIS / POLL_MILLIS; i++) {
            for (final JsonObject m : got) {
                if (type.equals(m.get("t").getAsString()) && wanted.test(m)) {
                    return m;
                }
            }
            Thread.sleep(POLL_MILLIS);
        }
        return null;
    }

    /** The newest message of a type, or null if none arrived in time. */
    JsonObject awaitNewest(final String type) throws InterruptedException {
        return awaitAfter(0, type, m -> true);
    }

    /** The newest message of the type that matches wanted, or null if none arrived in time. */
    JsonObject awaitNewest(final String type, final Predicate<JsonObject> wanted) throws InterruptedException {
        return awaitAfter(0, type, wanted);
    }

    /** The newest matching message at or after index from, or null if none arrived in time. */
    JsonObject awaitAfter(final int from, final String type, final Predicate<JsonObject> wanted) throws InterruptedException {
        for (int i = 0; i < WAIT_MILLIS / POLL_MILLIS; i++) {
            for (int j = got.size() - 1; j >= from; j--) {
                final JsonObject m = got.get(j);
                if (type.equals(m.get("t").getAsString()) && wanted.test(m)) {
                    return m;
                }
            }
            Thread.sleep(POLL_MILLIS);
        }
        return null;
    }

    JsonObject awaitMatching(final String type, final Predicate<JsonObject> wanted, final String why) throws InterruptedException {
        return found(awaitMatching(type, wanted), why);
    }

    JsonObject awaitNewest(final String type, final String why) throws InterruptedException {
        return found(awaitNewest(type), why);
    }

    JsonObject awaitNewest(final String type, final Predicate<JsonObject> wanted, final String why) throws InterruptedException {
        return found(awaitNewest(type, wanted), why);
    }

    /** The waits that take a why fail with it when no matching message arrived in time. */
    private static JsonObject found(final JsonObject message, final String why) {
        Assert.assertNotNull(message, why);
        return message;
    }

    /** Clears the messages received so far, so an earlier message cannot satisfy a later wait. */
    void forget() {
        got.clear();
    }

}
