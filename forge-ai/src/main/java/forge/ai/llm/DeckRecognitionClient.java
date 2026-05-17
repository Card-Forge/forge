package forge.ai.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 * <p>Uses {@link HttpURLConnection} (rather than the JDK 11
 * {@code java.net.http} client) so the feature works on Android — Forge's
 * mobile build targets API levels below 34, where {@code java.net.http} is
 * unavailable.</p>
 *
 * <p>Calls are asynchronous and fail-soft: any transport, status or parsing
 * error is swallowed (logged at debug level) and surfaced as an empty
 * {@link Optional}, so the game loop is never disrupted by the LLM.</p>
 */
public final class DeckRecognitionClient {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int HEALTH_TIMEOUT_MS = 2_000;
    private static final int RECOGNIZE_TIMEOUT_MS = 90_000;

    private final String baseUrl;
    private final Gson gson = new GsonBuilder().create();
    private final ExecutorService executor;

    public DeckRecognitionClient(final String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "forge-llm-deck-recognition");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Synchronous, short-timeout availability probe of {@code GET /health}.
     * Intended to be called once when attaching the feature.
     *
     * @return true if the sidecar responded 200
     */
    public boolean isSidecarHealthy() {
        HttpURLConnection conn = null;
        try {
            conn = open("/health", "GET", HEALTH_TIMEOUT_MS);
            return conn.getResponseCode() == 200;
        } catch (final Exception ex) {
            Log.debug("DeckRecognition: sidecar health check failed: " + ex.getMessage());
            return false;
        } finally {
            disconnect(conn);
        }
    }

    /**
     * Asynchronously POST a recognition request. Never completes exceptionally:
     * failures resolve to an empty {@link Optional}.
     */
    public CompletableFuture<Optional<RecognitionResult>> recognizeAsync(final RecognitionRequest request) {
        return CompletableFuture.supplyAsync(() -> doRecognize(request), executor);
    }

    private Optional<RecognitionResult> doRecognize(final RecognitionRequest request) {
        HttpURLConnection conn = null;
        try {
            conn = open("/recognize", "POST", RECOGNIZE_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            final byte[] body = gson.toJson(request).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            final int status = conn.getResponseCode();
            if (status != 200) {
                Log.debug("DeckRecognition: sidecar returned HTTP " + status);
                return Optional.empty();
            }
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return Optional.ofNullable(gson.fromJson(reader, RecognitionResult.class));
            }
        } catch (final IOException | RuntimeException ex) {
            Log.debug("DeckRecognition: request failed: " + ex.getMessage());
            return Optional.empty();
        } finally {
            disconnect(conn);
        }
    }

    private HttpURLConnection open(final String path, final String method, final int readTimeout)
            throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private static void disconnect(final HttpURLConnection conn) {
        if (conn != null) {
            conn.disconnect();
        }
    }

    /** Release the background executor. Safe to call multiple times. */
    public void shutdown() {
        executor.shutdownNow();
    }
}
