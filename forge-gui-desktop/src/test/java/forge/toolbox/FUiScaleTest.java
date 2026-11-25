package forge.toolbox;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.awt.Component;
import java.awt.Graphics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/** Deterministic checks for scale resolution; no display or application startup is required. */
@Test(enabled = true)
public class FUiScaleTest {
    @AfterMethod
    public void resetHooks() {
        FUiScale.resetForTest();
    }

    public void providerValueIsHonored() {
        FUiScale.setScaleProviderForTest(() -> 1.5f);
        assertEquals(FUiScale.of((Component) null), 1.5f, 0f);
        assertEquals(FUiScale.of((Graphics) null), 1.5f, 0f);
    }

    public void invalidProviderUsesFallback() {
        float expected = FUiScale.fallback();
        FUiScale.setScaleProviderForTest(() -> Float.NaN);
        assertEquals(FUiScale.of((Component) null), expected, 0f);
        FUiScale.setScaleProviderForTest(() -> 0f);
        assertEquals(FUiScale.of((Graphics) null), expected, 0f);
    }

    public void scaleChangeNotifiesListenerOnce() throws Exception {
        FUiScale.resetForTest();
        FUiScale.setScaleProviderForTest(() -> 1f);
        FUiScale.of((Component) null); // baseline before the listener is registered

        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch fired = new CountDownLatch(1);
        Runnable listener = () -> {
            notifications.incrementAndGet();
            fired.countDown();
        };
        FUiScale.addScaleChangeListener(listener);
        try {
            FUiScale.setScaleProviderForTest(() -> 1.5f);
            FUiScale.checkScaleChange();
            assertTrue(fired.await(5, TimeUnit.SECONDS), "scale change listener was not notified");

            FUiScale.checkScaleChange(); // unchanged scale must not notify again
            SwingUtilities.invokeAndWait(() -> { }); // let any queued pass finish before counting
            assertEquals(notifications.get(), 1);
        } finally {
            FUiScale.removeScaleChangeListener(listener);
        }
    }
}
