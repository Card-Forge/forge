package forge.util;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate-limit responses reach {@link ScryfallRateLimiter} from many workers in any order, and one that carries a
 * short Retry-After must never cut short a cooldown that another response has already set. Retry-After is whole
 * seconds, so the cooldowns here are 1 s (short) and 3 s (long), and each test looks again after 1.5 s: past the
 * short one, well before the long one. Paced hosts get a name of their own per test so that no test sees another's
 * cooldown; the API has a single cooldown, which each test waits out before the next one starts.
 */
@Test(groups = {"UnitTest"})
public class ScryfallRateLimiterTest {

    private static final String API_URL = "https://api.scryfall.com/cards/test";
    private static final String SHORT = "1";
    private static final String LONG = "3";
    private static final long LOOK_AGAIN_MS = 1500;
    private static final AtomicInteger hostCount = new AtomicInteger();

    @AfterMethod
    public void waitOutApiCooldown() {
        ScryfallRateLimiter.awaitCooldownCleared(() -> false);
    }

    private static String newPacedHostUrl() {
        final String url = "https://paced-" + hostCount.incrementAndGet() + ".example.test/image.jpg";
        ScryfallRateLimiter.paceHost(url);
        return url;
    }

    /**
     * Sends one long and {@code shortsPerUrl} short responses for each url from as many threads as possible, all
     * released together, so that the order they land in is up to the scheduler.
     */
    private static void raceResponses(final List<String> urls, final int shortsPerUrl) throws Exception {
        final List<Runnable> responses = new ArrayList<>();
        for (final String url : urls) {
            final boolean api = ScryfallRateLimiter.isApiUrl(url);
            responses.add(() -> ScryfallRateLimiter.noteIfRateLimited(429, url, LONG));
            for (int i = 0; i < shortsPerUrl; i++) {
                final int code = api || i % 2 == 0 ? 429 : 503; // a paced host answers either
                responses.add(() -> ScryfallRateLimiter.noteIfRateLimited(code, url, SHORT));
            }
        }
        Collections.shuffle(responses);

        final ExecutorService pool = Executors.newFixedThreadPool(Math.min(responses.size(), 64));
        try {
            final CountDownLatch go = new CountDownLatch(1);
            final List<Future<Void>> done = new ArrayList<>();
            for (final Runnable response : responses) {
                final Callable<Void> task = () -> {
                    go.await();
                    response.run();
                    return null;
                };
                done.add(pool.submit(task));
            }
            go.countDown();
            for (final Future<Void> f : done) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void apiCooldown_shorterRetryAfterDoesNotShortenIt() throws InterruptedException {
        ScryfallRateLimiter.noteIfRateLimited(429, API_URL, LONG);
        ScryfallRateLimiter.noteIfRateLimited(429, API_URL, SHORT);

        Thread.sleep(LOOK_AGAIN_MS);
        Assert.assertTrue(ScryfallRateLimiter.isCoolingDown(), "a later, shorter Retry-After ended the API cooldown early");
    }

    @Test
    public void pacedHostCooldown_shorterRetryAfterDoesNotShortenIt() throws InterruptedException {
        final String url = newPacedHostUrl();
        ScryfallRateLimiter.noteIfRateLimited(429, url, LONG);
        ScryfallRateLimiter.noteIfRateLimited(503, url, SHORT);

        Thread.sleep(LOOK_AGAIN_MS);
        Assert.assertTrue(ScryfallRateLimiter.isCoolingDown(url), "a later, shorter Retry-After ended the host cooldown early");
    }

    @Test
    public void pacedHostCooldown_longerRetryAfterExtendsIt() throws InterruptedException {
        final String url = newPacedHostUrl();
        ScryfallRateLimiter.noteIfRateLimited(429, url, SHORT);
        ScryfallRateLimiter.noteIfRateLimited(429, url, LONG);

        Thread.sleep(LOOK_AGAIN_MS);
        Assert.assertTrue(ScryfallRateLimiter.isCoolingDown(url), "a later, longer Retry-After did not extend the host cooldown");
    }

    @Test
    public void concurrentResponses_neverShortenApiCooldown() throws Exception {
        raceResponses(Collections.singletonList(API_URL), 23);

        Thread.sleep(LOOK_AGAIN_MS);
        Assert.assertTrue(ScryfallRateLimiter.isCoolingDown(), "concurrent responses ended the API cooldown early");
    }

    @Test
    public void concurrentResponses_neverShortenPacedHostCooldowns() throws Exception {
        final List<String> urls = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            urls.add(newPacedHostUrl());
        }
        raceResponses(urls, 3);

        Thread.sleep(LOOK_AGAIN_MS);
        for (final String url : urls) {
            Assert.assertTrue(ScryfallRateLimiter.isCoolingDown(url), "concurrent responses ended the cooldown of " + url + " early");
        }
    }
}
