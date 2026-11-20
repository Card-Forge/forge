package forge.web;

import com.google.gson.JsonObject;
import forge.util.IHasForgeLog;
import forge.web.protocol.ToBrowser.HostChoice;
import forge.web.protocol.ToBrowser.HostChoiceKind;
import forge.web.protocol.MessageJson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Questions the host asks outside a match. The calling thread waits for the browser's answer, as desktop waits for its dialog.
 * These are kept apart from {@link forge.web.match.MatchQuestions}, which holds one seat's questions during its game. These questions go to the
 * browser that holds the host's seat at the time. They are shown on any screen, and they time out because no game ends them.
 */
final class HostQuestions implements IHasForgeLog {
    /** Long enough for someone to read the list and decide. Short enough that a question to a closed browser times out. */
    private static final long ANSWER_TIMEOUT_MINUTES = 5;

    private final Map<Integer, CompletableFuture<List<Integer>>> open = new ConcurrentHashMap<>();
    /** The questions still waiting, to ask a browser that reloaded. */
    private final Map<Integer, HostChoice> pending = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger();
    private volatile Consumer<JsonObject> toBrowser = message -> { };
    private final long timeoutMillis;

    HostQuestions() {
        this(TimeUnit.MINUTES.toMillis(ANSWER_TIMEOUT_MINUTES));
    }

    HostQuestions(final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    void setSink(final Consumer<JsonObject> sink) {
        toBrowser = sink;
    }

    /** Sends the open questions again, so a browser that reloaded can still answer them. */
    void replay(final Consumer<JsonObject> sink) {
        for (final HostChoice again : pending.values()) {
            sink.accept(MessageJson.encode(again));
        }
    }

    /** Asks the browser to pick from a list and waits. When images is given, each option is shown as a picture of the kind pictured names, a card or a pack. Returns the chosen indices, or null when nobody answers. */
    List<Integer> ask(final HostChoiceKind kind, final String title, final String message, final List<String> options, final int min, final int max,
            final List<String> images, final String pictured) {
        final int id = nextId.incrementAndGet();
        final HostChoice request = new HostChoice(id, kind, title, message, min, max, options, images, pictured);
        final CompletableFuture<List<Integer>> answer = new CompletableFuture<>();
        open.put(id, answer);
        pending.put(id, request);
        toBrowser.accept(MessageJson.encode(request));
        try {
            return answer.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (final ExecutionException | TimeoutException e) {
            netLog.warn("No answer to the host's question: {}", message);
            return null;
        } finally {
            open.remove(id);
            pending.remove(id);
        }
    }

    void answer(final int id, final List<Integer> value) {
        final CompletableFuture<List<Integer>> waiting = open.get(id);
        if (waiting != null) {
            waiting.complete(value);
        }
    }
}
