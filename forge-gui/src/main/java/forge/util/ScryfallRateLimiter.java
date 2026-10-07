package forge.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Global pacing/cooldown gate for api.scryfall.com endpoints. Never applies to cards.scryfall.io
 * CDN URLs -- those are unthrottled; see {@link forge.gui.download.CdnUuidCache}.
 *
 * <p>Scryfall caps {@code /cards/search} and {@code /cards/named} at 2/sec (500ms); everything
 * else at 10/sec (100ms).
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

    private ScryfallRateLimiter() {}

    /** True for any api.scryfall.com request -- card lookup, search, named, and bulk-data listing. */
    public static boolean isApiUrl(String url) {
        return url != null && url.startsWith(API_HOST_PREFIX);
    }

    /** Whether {@code url} is in Scryfall's 2/sec-limited bucket (search, named). */
    private static boolean isSlowEndpoint(String url) {
        return url != null && (url.contains("/cards/search") || url.contains("/cards/named"));
    }

    /** Whether we are still backing off after Scryfall rate limited us. Clears an expired cooldown. */
    public static boolean isCoolingDown() {
        final Date until = cooldownUntil;
        if (until == null) {
            return false;
        }
        if (until.after(new Date())) {
            return true;
        }
        cooldownUntil = null;
        return false;
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

    /** Records a cooldown if {@code responseCode} is 429 for a Scryfall API url; no-op otherwise. */
    public static void noteIfRateLimited(int responseCode, String url, String retryAfterHeader) {
        if (responseCode == 429 && isApiUrl(url)) {
            noteRateLimited(url, parseRetryAfterSeconds(retryAfterHeader));
        }
    }

    /** True (and logs) if url is a Scryfall API URL and we're currently backing off. */
    public static boolean shouldSkip(String url) {
        if (!isApiUrl(url) || !isCoolingDown()) {
            return false;
        }
        System.err.println("Currently in cooldown period for scryfall downloads. Skipping download attempt for: " + url);
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
        if (!isCoolingDown()) {
            return;
        }
        long lastLoggedAt = 0;
        while (isCoolingDown()) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            long remainingSec = Math.max(0, cooldownUntil.getTime() - System.currentTimeMillis()) / 1000 + 1;
            String message = "Rate limited by Scryfall -- waiting " + remainingSec + "s before continuing...";
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

    /** Blocks the calling thread (never the EDT) until it's this caller's turn. No-op for non-API URLs. */
    public static void acquire(String url) {
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
