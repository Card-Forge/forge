package forge.util;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Global pacing/cooldown gate for api.scryfall.com endpoints. Never applies to cards.scryfall.io
 * CDN URLs -- those are unthrottled; see {@link forge.gui.download.CdnUuidCache}.
 *
 * <p>Scryfall caps {@code /cards/search} and {@code /cards/named} at 2/sec (500ms); everything
 * else at 10/sec (100ms).
 *
 * <p>Third-party sites that host some images (the ones {@code token-images.txt} points at, e.g.
 * oldborder-shandalar.net) publish no limit, so each of them is paced as a whole at one request per
 * second and left alone for a while after it answers 429 or 503. Register a host with
 * {@link #paceHost(String)}; everything else (the Scryfall CDN, Forge's own host) is not paced.
 */
public final class ScryfallRateLimiter {
    private static final String API_HOST_PREFIX = "https://api.scryfall.com/";
    private static final long SEARCH_INTERVAL_MS = 500;  // /cards/search, /cards/named -- 2/sec
    private static final long DEFAULT_INTERVAL_MS = 100; // everything else -- 10/sec
    /** Fallback only -- Scryfall's 429 response almost always carries a Retry-After header, which takes priority. */
    private static final long DEFAULT_COOLDOWN_SECONDS = 30;

    private static final Object searchPacingLock = new Object();
    private static long lastSearchRequestAt = 0;
    private static final Object defaultPacingLock = new Object();
    private static long lastDefaultRequestAt = 0;
    private static volatile Date cooldownUntil = null;

    /** Hosts we know nothing about: one request per second, since no limit is published. */
    private static final long HOST_INTERVAL_MS = 1000;
    private static final long HOST_DEFAULT_COOLDOWN_SECONDS = 60;
    private static final Map<String, PacedHost> pacedHosts = new ConcurrentHashMap<>();

    private static final class PacedHost {
        final String name;
        final Object lock = new Object();
        long lastRequestAt = 0;
        volatile long cooldownUntilMs = 0;

        PacedHost(String name) {
            this.name = name;
        }
    }

    static {
        paceHost("https://oldborder-shandalar.net/");
    }

    private ScryfallRateLimiter() {}

    /**
     * Starts pacing the host of {@code url} (one request per second, cooldown on 429/503). Scryfall's
     * own hosts are never registered: the API has its own buckets and the CDN is unthrottled.
     */
    public static void paceHost(String url) {
        final String host = hostOf(url);
        if (host != null && !host.equals("scryfall.com") && !host.endsWith(".scryfall.com") && !host.endsWith(".scryfall.io")) {
            pacedHosts.computeIfAbsent(host, PacedHost::new);
        }
    }

    private static PacedHost pacedHost(String url) {
        final String host = hostOf(url);
        return host == null ? null : pacedHosts.get(host);
    }

    private static String hostOf(String url) {
        if (url == null) {
            return null;
        }
        final int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int end = url.length();
        for (final char stop : new char[] {'/', ':', '?', '#'}) {
            final int at = url.indexOf(stop, schemeEnd + 3);
            if (at >= 0 && at < end) {
                end = at;
            }
        }
        final String host = url.substring(schemeEnd + 3, end).toLowerCase(Locale.ROOT);
        return host.isEmpty() ? null : host;
    }

    /** True for any api.scryfall.com request -- card lookup, search, named, and bulk-data listing. */
    public static boolean isApiUrl(String url) {
        return url != null && url.startsWith(API_HOST_PREFIX);
    }

    /** Whether {@code url} is in Scryfall's 2/sec-limited bucket (search, named). */
    private static boolean isSlowEndpoint(String url) {
        return url != null && (url.contains("/cards/search") || url.contains("/cards/named"));
    }

    /**
     * Whether we are still backing off after Scryfall rate limited us. An expired cooldown is left in place rather
     * than cleared: many threads ask at once, and a clear could erase a cooldown that a 429 set in the meantime
     * or leave {@link #awaitCooldownCleared} reading null.
     */
    public static boolean isCoolingDown() {
        final Date until = cooldownUntil;
        return until != null && until.after(new Date());
    }

    /**
     * Record that Scryfall returned 429, so we stop asking for a while. Always logged (not
     * debug-only): this is the one place that explains every subsequent "in cooldown" skip.
     *
     * @param retryAfterSeconds value of the response's {@code Retry-After} header, or {@code <= 0}
     *                          if absent/unparseable, in which case a short default is used.
     */
    public static void noteRateLimited(String triggerUrl, long retryAfterSeconds) {
        long seconds = retryAfterSeconds > 0 ? retryAfterSeconds : DEFAULT_COOLDOWN_SECONDS;
        cooldownUntil = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds));
        System.err.println("==== Scryfall returned 429 for: " + triggerUrl
                + " -- backing off all api.scryfall.com requests until " + cooldownUntil + " ====");
    }

    /** Parses a {@code Retry-After} header value (seconds) into seconds, or {@code -1} if absent/unparseable. */
    public static long parseRetryAfterSeconds(String headerValue) {
        if (headerValue == null) {
            return -1;
        }
        try {
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Records a cooldown if {@code responseCode} is 429 for a Scryfall API url, or 429/503 for a paced
     * third-party host; no-op otherwise.
     */
    public static void noteIfRateLimited(int responseCode, String url, String retryAfterHeader) {
        if (responseCode == 429 && isApiUrl(url)) {
            noteRateLimited(url, parseRetryAfterSeconds(retryAfterHeader));
            return;
        }
        final PacedHost host = pacedHost(url);
        if (host != null && (responseCode == 429 || responseCode == 503)) {
            final long retryAfter = parseRetryAfterSeconds(retryAfterHeader);
            final long seconds = retryAfter > 0 ? retryAfter : HOST_DEFAULT_COOLDOWN_SECONDS;
            host.cooldownUntilMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            System.err.println("==== " + host.name + " returned " + responseCode + " for: " + url
                    + " -- backing off all " + host.name + " requests for " + seconds + "s ====");
        }
    }

    /** Whether the host of {@code url} is being backed off from: the API after a 429, or a paced host after a 429/503. */
    public static boolean isCoolingDown(String url) {
        if (isApiUrl(url)) {
            return isCoolingDown();
        }
        final PacedHost host = pacedHost(url);
        return host != null && host.cooldownUntilMs > System.currentTimeMillis();
    }

    /** True (and logs) if url is a Scryfall API URL or a paced host's and we're currently backing off. */
    public static boolean shouldSkip(String url) {
        if (!isCoolingDown(url)) {
            return false;
        }
        System.err.println("Currently in cooldown period for " + (isApiUrl(url) ? "scryfall" : pacedHost(url).name)
                + " downloads. Skipping download attempt for: " + url);
        return true;
    }

    /**
     * Blocks the calling thread until any active cooldown clears, polling {@code cancelled}.
     * No-op if not cooling down. Only call from a cancelable background job (e.g. the bulk
     * downloader) -- never from a gameplay fire-and-forget path, where failing fast is correct.
     */
    public static void awaitCooldownCleared(java.util.function.BooleanSupplier cancelled) {
        awaitCooldownCleared(cancelled, null);
    }

    /** Same as {@link #awaitCooldownCleared(java.util.function.BooleanSupplier)}, but reports a status string (throttled to ~5s) via {@code onWaiting} for a visible progress UI. */
    public static void awaitCooldownCleared(java.util.function.BooleanSupplier cancelled,
                                             java.util.function.Consumer<String> onWaiting) {
        awaitCooldownCleared(API_HOST_PREFIX, cancelled, onWaiting);
    }

    /** Same as above for the host of {@code url}: the API, or a paced third-party host. */
    public static void awaitCooldownCleared(String url, java.util.function.BooleanSupplier cancelled,
                                             java.util.function.Consumer<String> onWaiting) {
        if (!isCoolingDown(url)) {
            return;
        }
        final PacedHost host = pacedHost(url);
        long lastLoggedAt = 0;
        while (isCoolingDown(url)) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            final long untilMs = host == null ? cooldownUntil.getTime() : host.cooldownUntilMs;
            long remainingSec = Math.max(0, untilMs - System.currentTimeMillis()) / 1000 + 1;
            String message = "Rate limited by " + (host == null ? "Scryfall" : host.name) + " -- waiting " + remainingSec + "s before continuing...";
            if (onWaiting != null) {
                onWaiting.accept(message);
            }
            long now = System.currentTimeMillis();
            if (now - lastLoggedAt >= 5000) {
                System.out.println("  " + message);
                lastLoggedAt = now;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Blocks the calling thread (never the EDT) until it's this caller's turn. No-op for urls that are not paced. */
    public static void acquire(String url) {
        final PacedHost host = pacedHost(url);
        if (host != null) {
            synchronized (host.lock) {
                final long wait = host.lastRequestAt + HOST_INTERVAL_MS - System.currentTimeMillis();
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                host.lastRequestAt = System.currentTimeMillis();
            }
            return;
        }
        if (!isApiUrl(url)) {
            return;
        }
        if (isSlowEndpoint(url)) {
            synchronized (searchPacingLock) {
                final long wait = lastSearchRequestAt + SEARCH_INTERVAL_MS - System.currentTimeMillis();
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                lastSearchRequestAt = System.currentTimeMillis();
            }
        } else {
            synchronized (defaultPacingLock) {
                final long wait = lastDefaultRequestAt + DEFAULT_INTERVAL_MS - System.currentTimeMillis();
                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                lastDefaultRequestAt = System.currentTimeMillis();
            }
        }
    }
}
