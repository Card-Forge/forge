package forge.adventure.util;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
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

    @DataProvider(name = "devices")
    public Object[][] devices() {
        // name, screen width, screen height as held in landscape
        return new Object[][]{
                {"720p desktop", 1280, 720},
                {"1080p desktop", 1920, 1080},
                {"16:10 laptop", 1920, 1200},
                {"ultrawide monitor", 3440, 1440},
                {"iPad 4:3", 2048, 1536},
                {"iPad Pro 11", 2388, 1668},
                {"iPhone SE", 1334, 750},
                {"iPhone 15", 2556, 1179},
                {"19.5:9 phone", 2340, 1080},
                {"Xperia 1 21:9", 2520, 1080},
                {"Galaxy Z Fold 4 cover", 2268, 832},
                {"Galaxy Z Fold 5 cover", 2316, 904},
                {"Galaxy Z Fold inner", 2176, 1812},
                {"Pixel Fold inner", 2208, 1840},
                {"square", 1000, 1000},
        };
    }

    @Test(dataProvider = "devices")
    public void cardKeepsRatioInLandscape(String device, int sw, int sh) {
        assertOnScreenRatio(device, 480f, 270f, sw, sh, false);
    }

    @Test(dataProvider = "devices")
    public void cardKeepsRatioInPortrait(String device, int sw, int sh) {
        // same panel held upright: the screen sizes swap and the world becomes 270x480
        assertOnScreenRatio(device, 270f, 480f, sh, sw, false);
    }

    @Test(dataProvider = "devices")
    public void tooltipKeepsRatioInPortrait(String device, int sw, int sh) {
        // the tooltip keeps its width in portrait and scales its height instead
        assertOnScreenRatio(device, 270f, 480f, sh, sw, true);
    }

    @Test(dataProvider = "devices")
    public void wideScreensShrinkAndNarrowScreensWiden(String device, int sw, int sh) {
        float screenAspect = (float) sw / sh;
        float correction = ScreenAspectCorrection.widthCorrection(480f, 270f, sw, sh);
        if (Math.abs(screenAspect - 16f / 9f) < 0.0001f)
            Assert.assertEquals(correction, 1f, EPS, device);
        else
            Assert.assertEquals(correction < 1f, screenAspect > 16f / 9f, device);
    }

    @Test(dataProvider = "devices")
    public void correctionStaysInUsableRange(String device, int sw, int sh) {
        float landscape = ScreenAspectCorrection.widthCorrection(480f, 270f, sw, sh);
        float portrait = ScreenAspectCorrection.widthCorrection(270f, 480f, sh, sw);
        Assert.assertTrue(landscape > 0.5f && landscape < 2f, device + " landscape " + landscape);
        Assert.assertTrue(portrait > 0.5f && portrait < 2f, device + " portrait " + portrait);
    }

    @Test(dataProvider = "devices")
    public void rotatingTheDeviceInvertsTheCorrection(String device, int sw, int sh) {
        float landscape = ScreenAspectCorrection.widthCorrection(480f, 270f, sw, sh);
        float portrait = ScreenAspectCorrection.widthCorrection(270f, 480f, sh, sw);
        Assert.assertEquals(landscape * portrait, 1f, EPS, device);
    }

    @Test
    public void xperia21by9IsExactlySixteenOverTwentyOne() {
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 2520, 1080), 16f / 21f, EPS);
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(270, 480, 1080, 2520), 21f / 16f, EPS);
    }

    @Test
    public void ipad4by3() {
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(480, 270, 2048, 1536), 4f / 3f, EPS);
        Assert.assertEquals(ScreenAspectCorrection.widthCorrection(270, 480, 1536, 2048), 3f / 4f, EPS);
    }

    @Test
    public void foldInnerScreenIsAlmostSquare() {
        // 2176x1812: in landscape the cards must get clearly wider, in portrait clearly narrower
        float landscape = ScreenAspectCorrection.widthCorrection(480, 270, 2176, 1812);
        float portrait = ScreenAspectCorrection.widthCorrection(270, 480, 1812, 2176);
        Assert.assertTrue(landscape > 1.4f && landscape < 1.5f);
        Assert.assertTrue(portrait > 0.65f && portrait < 0.7f);
    }

    @Test
    public void foldCoverScreenIsTheTallestCase() {
        // Galaxy Z Fold 4 cover display, roughly 24.5:9
        float landscape = ScreenAspectCorrection.widthCorrection(480, 270, 2268, 832);
        float portrait = ScreenAspectCorrection.widthCorrection(270, 480, 832, 2268);
        Assert.assertTrue(landscape > 0.6f && landscape < 0.7f);
        Assert.assertTrue(portrait > 1.5f && portrait < 1.6f);
    }

    private static void assertOnScreenRatio(String device, float worldW, float worldH, float screenW, float screenH, boolean scaleHeight) {
        float cardRatio = 488f / 680f;
        float correction = ScreenAspectCorrection.widthCorrection(worldW, worldH, screenW, screenH);
        float cardW = 100f * cardRatio;
        float cardH = 100f;
        if (scaleHeight)
            cardH /= correction;
        else
            cardW *= correction;
        float onScreen = (cardW * screenW / worldW) / (cardH * screenH / worldH);
        Assert.assertEquals(onScreen, cardRatio, EPS, device);
    }
}
