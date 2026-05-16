package forge.ai.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.esotericsoftware.minlog.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * HTTP client for the LLM sidecar service.
 *
 * <p>Calls are asynchronous and fail-soft: any transport, status or parsing
 * error is swallowed (logged at debug level) and surfaced as an empty
 * {@link Optional}, so the game loop is never disrupted by the LLM.</p>
 */
public final class DeckRecognitionClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration RECOGNIZE_TIMEOUT = Duration.ofSeconds(90);

    private final String baseUrl;
    private final Gson gson = new GsonBuilder().create();
    private final ExecutorService executor;
    private final HttpClient httpClient;

    public DeckRecognitionClient(final String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "forge-llm-deck-recognition");
            t.setDaemon(true);
            return t;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .executor(executor)
                .build();
    }

    /**
     * Synchronous, short-timeout availability probe of {@code GET /health}.
     * Intended to be called once when attaching the feature.
     *
     * @return true if the sidecar responded 200
     */
    public boolean isSidecarHealthy() {
        try {
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(HEALTH_TIMEOUT)
                    .GET()
                    .build();
            final HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (final Exception ex) {
            Log.debug("DeckRecognition: sidecar health check failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously POST a recognition request. Never completes exceptionally:
     * failures resolve to an empty {@link Optional}.
     */
    public CompletableFuture<Optional<RecognitionResult>> recognizeAsync(final RecognitionRequest request) {
        final HttpRequest httpRequest;
        try {
            httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/recognize"))
                    .timeout(RECOGNIZE_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
                    .build();
        } catch (final RuntimeException ex) {
            Log.debug("DeckRecognition: failed to build request: " + ex.getMessage());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .handle((resp, err) -> {
                    if (err != null) {
                        Log.debug("DeckRecognition: request failed: " + err.getMessage());
                        return Optional.<RecognitionResult>empty();
                    }
                    if (resp.statusCode() != 200) {
                        Log.debug("DeckRecognition: sidecar returned HTTP " + resp.statusCode());
                        return Optional.<RecognitionResult>empty();
                    }
                    try {
                        return Optional.ofNullable(gson.fromJson(resp.body(), RecognitionResult.class));
                    } catch (final RuntimeException ex) {
                        Log.debug("DeckRecognition: failed to parse response: " + ex.getMessage());
                        return Optional.<RecognitionResult>empty();
                    }
                });
    }

    /** Release the background executor. Safe to call multiple times. */
    public void shutdown() {
        executor.shutdownNow();
    }
}
