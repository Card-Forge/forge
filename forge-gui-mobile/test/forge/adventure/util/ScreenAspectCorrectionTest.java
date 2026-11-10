package forge.adventure.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ScreenAspectCorrectionTest {
    private static final float EPS = 0.0001f;

    @Test
    public void sameAspectNeedsNoCorrection() {
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 1280, 720), 1f, EPS);
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 1920, 1080), 1f, EPS);
    }

    @Test
    public void widerScreenShrinksWidth() {
        // 20:9 phone in landscape
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 2400, 1080), (16f / 9f) / (20f / 9f), EPS);
    }

    @Test
    public void narrowerScreenWidensWidth() {
        // 4:3 window
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 1024, 768), (16f / 9f) / (4f / 3f), EPS);
    }

    @Test
    public void correctedCardKeepsRatioOnScreen() {
        float cardRatio = 488f / 680f;
        float worldH = 100f;
        float screenW = 2400f, screenH = 1080f;
        float correction = ScreenAspectCorrection.widthCorrection(480, 270, screenW, screenH);
        float worldW = worldH * cardRatio * correction;
        float onScreenRatio = (worldW * screenW / 480f) / (worldH * screenH / 270f);
        Assert.assertEquals(onScreenRatio, cardRatio, EPS);
    }

    @Test
    public void invalidSizesFallBackToNoCorrection() {
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 0, 0), 1f, EPS);
    }
}
