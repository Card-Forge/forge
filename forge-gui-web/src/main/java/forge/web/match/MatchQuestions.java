package forge.web.match;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import forge.util.IHasForgeLog;
import forge.util.LogSafe;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.MessageJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Blocking prompts that wait for the browser's answer. A request stays open when the browser reloads, and replay sends it to the new connection. */
public final class MatchQuestions implements IHasForgeLog {
    private record Pending(JsonObject request, JsonElement defaultAnswer, Predicate<JsonElement> valid, CompletableFuture<JsonElement> answer) {}

    private final Consumer<JsonObject> send;
    private final Supplier<String> who;
    private final Map<Integer, Pending> open = new LinkedHashMap<>();
    private int nextId = 1;

    /** {@code who} gives the name of the player asked, for the log. */
    public MatchQuestions(final Consumer<JsonObject> send, final Supplier<String> who) {
        this.send = send;
        this.who = who;
    }

    /** Sends one of the requests in {@link ToBrowser} and waits for its answer. Returns the default answer when cancelAll runs or the wait is interrupted. */
    public JsonElement await(final Record payload, final Predicate<JsonElement> valid) {
        final int id;
        final Pending pending;
        synchronized (this) {
            id = nextId++;
            final JsonObject request = MessageJson.encodeRequest(payload);
            request.addProperty("id", id);
            final JsonElement defaultAnswer = request.has("default") ? request.get("default") : JsonNull.INSTANCE;
            pending = new Pending(request, defaultAnswer, valid, new CompletableFuture<>());
            open.put(id, pending);
        }
        netLog.info("Asked {} #{}: {}", who.get(), id, pending.request().get("kind").getAsString());
        send.accept(pending.request());
        try {
            return pending.answer().get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return pending.defaultAnswer();
        } catch (final ExecutionException e) {
            return pending.defaultAnswer();
        } finally {
            synchronized (this) {
                open.remove(id);
            }
        }
    }

    public void complete(final int id, final JsonElement value) {
        final Pending pending;
        synchronized (this) {
            pending = open.get(id);
        }
        if (pending == null) {
            netLog.warn("Reply from {} for unknown request {}", who.get(), id);
            return;
        }
        if (value == null || !pending.valid().test(value)) {
            netLog.warn("Invalid reply from {} for request {}: {}", who.get(), id, LogSafe.forLog(String.valueOf(value), 200));
            return;
        }
        pending.answer().complete(value);
    }

    public void cancelAll() {
        final List<Pending> all;
        synchronized (this) {
            all = new ArrayList<>(open.values());
        }
        for (final Pending p : all) {
            p.answer().complete(p.defaultAnswer());
        }
    }

    public void replay(final Consumer<JsonObject> to) {
        final List<JsonObject> requests = new ArrayList<>();
        synchronized (this) {
            for (final Pending p : open.values()) {
                requests.add(p.request().deepCopy());
            }
        }
        requests.forEach(to);
    }
}
