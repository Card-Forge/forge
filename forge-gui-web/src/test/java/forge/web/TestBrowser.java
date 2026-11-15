package forge.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import forge.web.match.BrowserModel;
import forge.web.match.MatchProbe;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.MessageJson;
import org.testng.Assert;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A fake browser. It records every message the server sends and the game they describe, and can answer for the web seat. */
final class TestBrowser implements BrowserChannel {
    private static final int WAIT_MILLIS = 60_000;
    private static final int POLL_MILLIS = 10;
    private static final int MAX_RETRIES = 12;

    final List<JsonObject> got = new CopyOnWriteArrayList<>();
    final BrowserModel model = new BrowserModel();
    final CountDownLatch gameOver = new CountDownLatch(1);
    /** Counted down when a held browser reaches its own first main phase. It then waits there for {@link #release}. */
    final CountDownLatch atOwnMain = new CountDownLatch(1);
    /** Whether the server closed this browser's connection, as it does for a connection over its limit. */
    volatile boolean closed;
    /** The newest hello, which shows the session's current state. {@link #forget} does not clear it. */
    volatile JsonObject hello;
    volatile boolean reloaded;

    private final WebGuiGame gui;
    private final boolean plays;
    private final boolean holds;
    private final long answerMillis;
    private final ScheduledExecutorService actions;
    private final Deque<String> cardsToUse = new ConcurrentLinkedDeque<>();
    private final Set<Integer> answered = ConcurrentHashMap.newKeySet();
    private final AtomicInteger retries = new AtomicInteger();
    private final AtomicLong promptVersion = new AtomicLong();
    private volatile boolean released;
    private volatile JsonObject lastPrompt;
    private volatile int root = -1;
    private volatile JsonArray localPlayers = new JsonArray();

    /** A browser that only records messages. */
    TestBrowser() {
        this(null, false, false, 0);
    }

    /** Answers every request for the web seat. When plays is true it also passes priority and makes the picks a prompt needs. */
    TestBrowser(final WebGuiGame gui, final boolean plays) {
        this(gui, plays, false, 20);
    }

    /** A browser that plays but waits in its own first main phase until released. It delays each answer, and reloads once while the first request is open. */
    static TestBrowser held(final WebGuiGame gui, final long answerMillis) {
        return new TestBrowser(gui, true, true, answerMillis);
    }

    private TestBrowser(final WebGuiGame gui, final boolean plays, final boolean holds, final long answerMillis) {
        this.gui = gui;
        this.plays = plays;
        this.holds = holds;
        this.answerMillis = answerMillis;
        this.released = !holds;
        this.actions = gui == null ? null : Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "TestBrowser");
            t.setDaemon(true);
            return t;
        });
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

    /** Ends the thread a playing browser answers on. Call it once its match has ended. */
    void stop() {
        if (actions != null) {
            actions.shutdownNow();
        }
    }

    /** Lets a held browser continue. From its own first main phase it uses each "Zone:Card Name" in turn. */
    void release(final String... cardNames) {
        for (final String name : cardNames) {
            cardsToUse.add(name);
        }
        released = true;
        actOnLatestPrompt();
    }

    static JsonObject action(final String type) {
        final JsonObject m = new JsonObject();
        m.addProperty("t", type);
        return m;
    }

    /** Passes a browser's command to the match. */
    static void command(final WebGuiGame gui, final JsonObject message) {
        gui.onCommand(null, MessageJson.decode(message));
    }

    static JsonObject reply(final int id, final JsonElement value) {
        final JsonObject r = action("reply");
        r.addProperty("id", id);
        r.add("value", value);
        return r;
    }

    List<JsonObject> all(final String type) {
        return got.stream().filter(m -> type.equals(m.get("t").getAsString())).collect(Collectors.toList());
    }

    JsonObject last(final String type) {
        final List<JsonObject> matches = all(type);
        return matches.isEmpty() ? null : matches.get(matches.size() - 1);
    }

    /** The turn in the latest state, or 0 before the first turn. */
    int turn() {
        final JsonObject state = last("state");
        final JsonObject game = state == null ? null : MatchProbe.objects(model).get(state.get("root").getAsInt());
        return game == null || !game.has("Turn") ? 0 : game.get("Turn").getAsInt();
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

    /** Waits until the latest table matches wanted. An earlier lobby message can describe a table that has since changed. */
    JsonObject awaitLobby(final Predicate<JsonObject> wanted) throws InterruptedException {
        for (int i = 0; i < WAIT_MILLIS / POLL_MILLIS; i++) {
            final JsonObject latest = latestTable();
            if (latest != null && wanted.test(latest)) {
                return latest;
            }
            Thread.sleep(POLL_MILLIS);
        }
        return null;
    }

    JsonObject awaitLobbyWithSeat() throws InterruptedException {
        return awaitLobby(l -> l.get("mySeat").getAsInt() >= 0);
    }

    JsonObject latestTable() {
        JsonObject latest = null;
        for (final JsonObject m : got) {
            if ("lobby".equals(m.get("t").getAsString()) && m.has("table")) {
                latest = m.getAsJsonObject("table");
            }
        }
        return latest;
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

    JsonObject awaitLobby(final Predicate<JsonObject> wanted, final String why) throws InterruptedException {
        return found(awaitLobby(wanted), why);
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

    private void onRequest(final JsonObject m) {
        final int id = m.get("id").getAsInt();
        if (!answered.add(id)) {
            // After a reload the server sends open requests again
            return;
        }
        if (holds && !reloaded) {
            reloaded = true;
            actions.schedule(() -> {
                answered.remove(id);
                gui.attach(this);
            }, 20, TimeUnit.MILLISECONDS);
        }
        final JsonElement value = answerFor(m);
        actions.schedule(() -> {
            command(gui, reply(id, value));
            // A declined request leaves the same input open without a new prompt
            if (plays) {
                actOnLatestPrompt();
            }
        }, answerMillis, TimeUnit.MILLISECONDS);
    }

    // An optional single choice defaults to "none". Taking the first option is what plays or activates the clicked card
    private static JsonElement answerFor(final JsonObject request) {
        final JsonElement def = request.get("default");
        if ("choices".equals(request.get("kind").getAsString()) && def.isJsonArray() && def.getAsJsonArray().isEmpty()
                && request.getAsJsonArray("options").size() > 0 && request.get("max").getAsInt() != 0) {
            final JsonArray first = new JsonArray();
            first.add(0);
            return first;
        }
        return def;
    }

    // Every change to the prompt resends it, so act once, on the latest one
    private void actOnLatestPrompt() {
        final long version = promptVersion.incrementAndGet();
        actions.schedule(() -> {
            final JsonObject prompt = lastPrompt;
            if (promptVersion.get() == version && prompt != null) {
                act(prompt);
            }
        }, 50, TimeUnit.MILLISECONDS);
    }

    private void act(final JsonObject prompt) {
        if (!released && inOwnFirstMain()) {
            atOwnMain.countDown();
            return;
        }
        // A rejected click sends no new prompt, so act again if nothing arrives. Stop after MAX_RETRIES so the wait can fail
        final long seen = promptVersion.get();
        actions.schedule(() -> {
            if (promptVersion.get() == seen && retries.incrementAndGet() <= MAX_RETRIES) {
                actOnLatestPrompt();
            }
        }, 1500, TimeUnit.MILLISECONDS);
        if (!prompt.getAsJsonObject("ok").get("enabled").getAsBoolean()) {
            selectUnchosen(prompt);
            return;
        }
        // Cards are used only from the priority prompt with an empty stack. Cost prompts such as "Sacrifice X?" are answered with OK
        final boolean priority = prompt.has("priority") && prompt.get("priority").getAsBoolean();
        final boolean canUse = priority && inOwnFirstMain() && stackEmpty();
        final Integer card = canUse ? nextScriptedCard() : null;
        if (card != null) {
            select(card);
        } else if (cardsToUse.isEmpty() || !canUse) {
            // Passing with something on the stack lets it resolve
            command(gui, action("ok"));
        }
    }

    // Clicking a card that is already selected deselects it, so pick one that is not
    private void selectUnchosen(final JsonObject prompt) {
        final Set<Integer> chosen = new HashSet<>();
        prompt.getAsJsonArray("highlighted").forEach(k -> chosen.add(k.getAsInt()));
        for (final JsonElement ref : prompt.getAsJsonArray("selectable")) {
            final int key = ref.getAsJsonObject().get("ref").getAsInt();
            if (!chosen.contains(key)) {
                select(key);
                return;
            }
        }
        // A prompt that offers players is answered by clicking the first player's avatar
        final JsonArray players = prompt.getAsJsonArray("selectablePlayers");
        if (players != null && !players.isEmpty()) {
            final JsonObject msg = action("selectPlayer");
            msg.addProperty("key", players.get(0).getAsJsonObject().get("ref").getAsInt());
            command(gui, msg);
        } else if (prompt.getAsJsonObject("cancel").get("enabled").getAsBoolean()) {
            command(gui, action("cancel"));
        }
    }

    private void select(final int key) {
        final JsonObject msg = action("selectCard");
        msg.addProperty("key", key);
        command(gui, msg);
    }

    private boolean inOwnFirstMain() {
        final JsonObject game = MatchProbe.objects(model).get(root);
        return game != null && !localPlayers.isEmpty() && game.has("PlayerTurn") && game.has("Phase")
                && game.getAsJsonObject("PlayerTurn").get("ref").getAsInt() == localPlayers.get(0).getAsInt()
                && "MAIN1".equals(game.get("Phase").getAsString());
    }

    private boolean stackEmpty() {
        final JsonObject game = MatchProbe.objects(model).get(root);
        return game != null && (!game.has("Stack") || game.getAsJsonArray("Stack").isEmpty());
    }

    // Each "Zone:Card Name" entry is clicked again until its card leaves that zone, because a click can be rejected
    private Integer nextScriptedCard() {
        final Map<Integer, JsonObject> objects = MatchProbe.objects(model);
        final JsonObject player = objects.get(localPlayers.get(0).getAsInt());
        while (!cardsToUse.isEmpty()) {
            final String[] entry = cardsToUse.peek().split(":", 2);
            final Integer key = findIn(objects, player, entry[0], entry[1]);
            if (key != null) {
                return key;
            }
            cardsToUse.poll();
        }
        return null;
    }

    private static Integer findIn(final Map<Integer, JsonObject> objects, final JsonObject player, final String zone, final String name) {
        if (player == null || !player.has(zone)) {
            return null;
        }
        for (final JsonElement ref : player.getAsJsonArray(zone)) {
            if (ref.isJsonObject()) {
                final int key = ref.getAsJsonObject().get("ref").getAsInt();
                if (name.equals(nameOf(objects, objects.get(key)))) {
                    return key;
                }
            }
        }
        return null;
    }

    private static String nameOf(final Map<Integer, JsonObject> objects, final JsonObject card) {
        if (card == null || !card.has("CurrentState")) {
            return null;
        }
        final JsonObject state = objects.get(card.getAsJsonObject("CurrentState").get("ref").getAsInt());
        return state == null || !state.has("Name") ? null : state.get("Name").getAsString();
    }
}
