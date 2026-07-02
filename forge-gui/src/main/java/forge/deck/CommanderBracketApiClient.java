package forge.deck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import forge.util.BuildInfo;
import forge.util.JsonUtil;

final class CommanderBracketApiClient {
    private static final String API_URL = "https://mtg-assistant.up.railway.app/decks/analyze-complete";
    private static final int CONNECT_TIMEOUT_MILLIS = 10000;
    private static final int READ_TIMEOUT_MILLIS = 45000;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1500L;

    private final ResultHandler resultHandler;
    private final Consumer<CommanderBracketService.BracketUpdate> updateListener;
    private final Map<String, CommanderBracketService.RemoteResult> memoryCache = new ConcurrentHashMap<>();
    private final Map<String, Long> failureCooldowns = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    private final PriorityQueue<ApiRequest> queue = new PriorityQueue<>();
    private final AtomicLong sequence = new AtomicLong();
    private final Thread worker;
    private long nextRequestTimeMillis = 0L;

    CommanderBracketApiClient(final ResultHandler resultHandler,
                              final Consumer<CommanderBracketService.BracketUpdate> updateListener) {
        this.resultHandler = resultHandler;
        this.updateListener = updateListener;
        worker = new Thread(this::processQueue, "CommanderBracket API");
        worker.setDaemon(true);
        worker.start();
    }

    CommanderBracketService.RemoteResult getCachedResult(final String deckHash) {
        return memoryCache.get(deckHash);
    }

    boolean enqueue(final Deck deck, final DeckProxy deckProxy, final String decklist, final String deckHash,
                    final Priority priority) {
        if (memoryCache.containsKey(deckHash) || isCoolingDown(deckHash)) {
            return false;
        }
        if (!inFlight.add(deckHash)) {
            promoteQueuedRequest(deck, deckProxy, decklist, deckHash, priority);
            return true;
        }

        synchronized (queue) {
            queue.add(new ApiRequest(deck, deckProxy, decklist, deckHash, priority, sequence.incrementAndGet()));
            queue.notifyAll();
        }
        return true;
    }

    boolean isActive(final String deckHash) {
        return active.contains(deckHash);
    }

    boolean isPending(final String deckHash) {
        return inFlight.contains(deckHash);
    }

    private void processQueue() {
        while (true) {
            final ApiRequest request;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    }
                    catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                request = queue.poll();
            }

            boolean requeued = false;
            try {
                active.add(request.deckHash);
                updateListener.accept(new CommanderBracketService.BracketUpdate(request.deckHash));
                throttle();
                final CommanderBracketService.RemoteResult result = postDeck(request.decklist, request.deckHash);
                failureCooldowns.remove(request.deckHash);
                memoryCache.put(request.deckHash, result);
                resultHandler.accept(request.deck, request.deckProxy, result);
            }
            catch (final RetryAfterException e) {
                nextRequestTimeMillis = Math.max(nextRequestTimeMillis, System.currentTimeMillis() + e.retryAfterMillis);
                requeue(request);
                requeued = true;
            }
            catch (final Exception ignored) {
                failureCooldowns.put(request.deckHash, System.currentTimeMillis() + 15L * 60L * 1000L);
            }
            finally {
                active.remove(request.deckHash);
                updateListener.accept(new CommanderBracketService.BracketUpdate(request.deckHash));
                if (!requeued) {
                    inFlight.remove(request.deckHash);
                }
            }
        }
    }

    private void promoteQueuedRequest(final Deck deck, final DeckProxy deckProxy, final String decklist,
                                      final String deckHash, final Priority priority) {
        if (priority != Priority.HIGH) {
            return;
        }
        synchronized (queue) {
            final ApiRequest queued = queue.stream()
                    .filter(request -> request.deckHash.equals(deckHash) && request.priority == Priority.LOW)
                    .findFirst()
                    .orElse(null);
            if (queued != null) {
                queue.remove(queued);
                queue.add(new ApiRequest(deck, deckProxy, decklist, deckHash, Priority.HIGH, sequence.incrementAndGet()));
                queue.notifyAll();
            }
        }
    }

    private boolean isCoolingDown(final String deckHash) {
        final Long retryAfter = failureCooldowns.get(deckHash);
        if (retryAfter == null) {
            return false;
        }
        if (retryAfter > System.currentTimeMillis()) {
            return true;
        }
        failureCooldowns.remove(deckHash);
        return false;
    }

    private void requeue(final ApiRequest request) {
        synchronized (queue) {
            queue.add(request);
            queue.notifyAll();
        }
    }

    private void throttle() throws InterruptedException {
        final long now = System.currentTimeMillis();
        final long waitMillis = Math.max(0L, nextRequestTimeMillis - now);
        if (waitMillis > 0L) {
            Thread.sleep(waitMillis);
        }
        nextRequestTimeMillis = System.currentTimeMillis() + MIN_REQUEST_INTERVAL_MILLIS;
    }

    private CommanderBracketService.RemoteResult postDeck(final String decklist, final String deckHash) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection)new URL(API_URL).openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", BuildInfo.getUserAgent());

            final String commander = firstCommanderName(decklist);
            final String escapedDecklist = JsonUtil.escape(decklist);
            final String payload = commander == null
                    ? "{\"decklist\":\"" + escapedDecklist + "\"}"
                    : "{\"decklist\":\"" + escapedDecklist + "\",\"commander\":\""
                    + JsonUtil.escape(commander) + "\"}";
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            final int status = connection.getResponseCode();
            if (status == 429) {
                throw new RetryAfterException(parseRetryAfter(connection.getHeaderField("Retry-After")));
            }
            final InputStream responseStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            final String body = readAll(responseStream);
            if (status >= 400) {
                throw new IOException("CommanderBracket API error " + status + ": " + body);
            }
            return CommanderBracketService.RemoteResult.fromResponse(deckHash, body);
        }
        finally {
            connection.disconnect();
        }
    }

    private static String firstCommanderName(final String decklist) {
        boolean inCommanderSection = false;
        for (final String rawLine : decklist.split("\\R")) {
            final String line = rawLine.trim();
            if (line.equalsIgnoreCase("// Commander")) {
                inCommanderSection = true;
                continue;
            }
            if (line.startsWith("//") && inCommanderSection) {
                return null;
            }
            if (inCommanderSection && !line.isEmpty()) {
                return line.replaceFirst("^\\d+\\s+", "");
            }
        }
        return null;
    }

    private static long parseRetryAfter(final String retryAfter) {
        if (StringUtils.isBlank(retryAfter)) {
            return 60000L;
        }
        try {
            return Math.max(1L, Long.parseLong(retryAfter.trim())) * 1000L;
        }
        catch (final NumberFormatException e) {
            return 60000L;
        }
    }

    private static String readAll(final InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    enum Priority {
        HIGH,
        LOW
    }

    interface ResultHandler {
        void accept(Deck deck, DeckProxy deckProxy, CommanderBracketService.RemoteResult result);
    }

    private static final class ApiRequest implements Comparable<ApiRequest> {
        private final Deck deck;
        private final DeckProxy deckProxy;
        private final String decklist;
        private final String deckHash;
        private final Priority priority;
        private final long sequence;

        private ApiRequest(final Deck deck, final DeckProxy deckProxy, final String decklist, final String deckHash,
                           final Priority priority, final long sequence) {
            this.deck = deck;
            this.deckProxy = deckProxy;
            this.decklist = decklist;
            this.deckHash = deckHash;
            this.priority = priority;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(final ApiRequest other) {
            final int priorityCompare = priority.compareTo(other.priority);
            return priorityCompare != 0 ? priorityCompare : Long.compare(sequence, other.sequence);
        }
    }

    private static final class RetryAfterException extends IOException {
        private final long retryAfterMillis;

        private RetryAfterException(final long retryAfterMillis) {
            this.retryAfterMillis = retryAfterMillis;
        }
    }
}
