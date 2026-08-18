package forge.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Single global pacing/cooldown gate for Scryfall's api.scryfall.com endpoints (card lookup,
 * search, download-by-name). Never applies to cards.scryfall.io CDN URLs -- those are pre-resolved
 * and intentionally unthrottled; see CdnUuidCache.
 *
 * <p>Scryfall's documented hard per-endpoint-category limits (see
 * https://scryfall.com/docs/api): {@code /cards/search}, {@code /cards/named},
 * {@code /cards/random}, and {@code /cards/collection} are capped at 2/sec (500ms); every other
 * method is capped at 10/sec (100ms). Forge only ever calls {@code search} (CDN UUID resolution)
 * and {@code named} (a legacy fuzzy-name lookup) from the slow bucket -- everything else (the
 * per-card {@code /cards/{set}/{cn}/{lang}} lookup) is in the fast bucket. Pacing every request
 * at the fast bucket's 100ms, including search, was 5x faster than Scryfall actually allows for
 * that endpoint and was the real cause of repeated 429s during bulk CDN-cache warm-up.
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

    /** True (and logs) if url is a Scryfall API URL and we're currently backing off. */
    public static boolean shouldSkip(String url) {
        if (!isApiUrl(url) || !isCoolingDown()) {
            return false;
        }
        System.err.println("Currently in cooldown period for scryfall downloads. Skipping download attempt for: " + url);
        return true;
    }

    /**
     * Blocks the calling thread until any active cooldown clears, polling {@code cancelled} so a
     * long-running background job (the bulk downloader) can still be cancelled while waiting.
     * No-op if not currently cooling down.
     *
     * <p>Only call this from a background thread that may legitimately block for minutes at a
     * time -- never from a gameplay fire-and-forget path (e.g. {@code CdnUuidCache}'s implicit
     * cache-miss sync), where failing fast and moving on is the correct behavior. Without this,
     * a large multi-set/multi-thousand-file bulk run that trips one 429 early on races through
     * its entire remaining backlog in skip-and-print mode -- far faster than the cooldown can
     * ever clear -- and ends up downloading nothing for the rest of the run.
     */
    public static void awaitCooldownCleared(java.util.function.BooleanSupplier cancelled) {
        awaitCooldownCleared(cancelled, null);
    }

    /**
     * Same as {@link #awaitCooldownCleared(java.util.function.BooleanSupplier)}, but also invokes
     * {@code onWaiting} (throttled to roughly once every 5 seconds) with a human-readable status
     * string, so a caller with a visible progress UI can show that it's genuinely waiting rather
     * than appearing to hang.
     */
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

    /**
     * Blocks the calling thread (never the EDT) until it's this caller's turn. No-op for
     * non-API URLs, so CDN/cardforge downloads never pay this cost. Search/named requests pace
     * independently from everything else, so a burst of one kind never forces the other to wait
     * longer than its own documented limit requires.
     */
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
