package forge.util;

import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadService;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * The pool that runs on-demand image downloads is sized when it is made, from the platform GuiBase reports at that
 * moment: a mobile port must get fewer workers than a desktop.
 */
@Test(groups = {"UnitTest"})
public class ImageFetcherTest {

    /** Makes a download pool as the given platform, checks that it holds the platform's workers and returns how many. */
    private static int workersOn(final boolean android, final boolean ios) {
        final boolean wasAndroid = GuiBase.isAndroid();
        final boolean wasIOS = GuiBase.isIOS();
        GuiBase.setIsAndroid(android);
        GuiBase.setIsIOS(ios);
        final ThreadPoolExecutor pool = (ThreadPoolExecutor) ImageFetcher.newDownloadPool();
        try {
            Assert.assertEquals(pool.getCorePoolSize(), pool.getMaximumPoolSize(), "a pool of fixed size");
            Assert.assertEquals(pool.getMaximumPoolSize(), GuiDownloadService.parallelDownloads());
            return pool.getMaximumPoolSize();
        } finally {
            pool.shutdownNow();
            GuiBase.setIsAndroid(wasAndroid);
            GuiBase.setIsIOS(wasIOS);
        }
    }

    @Test
    public void downloadPool_hasFewerWorkersOnMobile() {
        final int desktop = workersOn(false, false);
        final int android = workersOn(true, false);
        final int ios = workersOn(false, true);

        Assert.assertEquals(desktop, 20);
        Assert.assertTrue(android >= 4 && android <= 6, "workers on Android: " + android);
        Assert.assertEquals(ios, android);
    }
}
