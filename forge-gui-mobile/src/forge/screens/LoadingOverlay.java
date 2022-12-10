package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gui.FThreads;
import forge.toolbox.FOverlay;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class LoadingOverlay extends FOverlay {
    private static final float INSETS = Utils.scale(10);
    private static final float LOGO_SIZE_FACTOR = 0.7f;
    private static final float INSETS_FACTOR = 0.025f;
    private static final FSkinFont FONT = FSkinFont.get(22);
    private BGAnimation bgAnimation;
    private Runnable runnable;
    private boolean afterMatch, alternate;

    private static FSkinColor getOverlayColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_ACTIVE).alphaColor(0.75f);
        return FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.75f);
    }

    private static FSkinColor getForeColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }

    public static void show(String caption0, final Runnable runnable) {
        show(caption0, false, runnable);
    }

    public static void show(String caption0, boolean textMode, final Runnable runnable) {
        final LoadingOverlay loader = new LoadingOverlay(caption0, textMode);
        loader.show(); //show loading overlay then delay running remaining logic so UI can respond
        ThreadUtil.invokeInGameThread(() -> FThreads.invokeInEdtLater(() -> {
            runnable.run();
            loader.hide();
            loader.finishedloading(); //setLoadingaMatch to false
        }));
    }

    public static void runBackgroundTask(String caption0, final Runnable task) {
        final LoadingOverlay loader = new LoadingOverlay(caption0, true);
        loader.show();
        FThreads.invokeInBackgroundThread(() -> {
            task.run();
            FThreads.invokeInEdtLater(() -> loader.hide());
        });
    }

    private String caption;
    private boolean textMode = false, match = false;
    private TextureRegion textureRegion;

    public LoadingOverlay(String caption0, boolean textOnly) {
        caption = caption0;
        textMode = textOnly;
    }

    public LoadingOverlay(Runnable runnable) {
        this(runnable, false, false);
    }
    public LoadingOverlay(Runnable toRun, boolean aftermatch, boolean otherTransition) {
        caption = "";
        textMode = true;
        textureRegion = Forge.takeScreenshot();
        match = true;
        bgAnimation = new BGAnimation();
        runnable = toRun;
        afterMatch = aftermatch;
        alternate = otherTransition;
    }

    public void setCaption(String caption0) {
        caption = caption0;
    }

    @Override
    public boolean isVisibleOnScreen(FScreen screen) {
        return true; //allow LoadingOverlay to remain visible while transitioning between screens
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    public void drawOverlay(Graphics g) {
        if (match) {
            if (bgAnimation != null) {
                bgAnimation.start();
                bgAnimation.drawBackground(g);
                return;
            }
        }
        if (!textMode) {
            float x = INSETS;
            float panelWidth = getWidth() - 2 * INSETS;

            if (Forge.isLandscapeMode()) {
                panelWidth = getHeight() - 2 * INSETS;
                x = (getWidth() - panelWidth) / 2;
            }

            float padding = panelWidth * INSETS_FACTOR;
            float logoSize = panelWidth * LOGO_SIZE_FACTOR;
            float fontHeight = FONT.getCapHeight();
            float panelHeight = logoSize + fontHeight + 4 * padding;

            float y = (getHeight() - panelHeight) / 2;
            float oldAlpha = g.getfloatAlphaComposite();
            //dark translucent back..
            g.fillRect(FSkinColor.getStandardColor(Color.BLACK).alphaColor(0.6f), 0, 0, getWidth(), getHeight());
            //overlay
            g.fillRect(getOverlayColor(), x, y, panelWidth, panelHeight);
            g.drawRect(Utils.scale(2), getForeColor(), x, y, panelWidth, panelHeight);
            y += padding;
            if (FSkin.getLogo() == null)
                g.drawImage(FSkinImage.LOGO, (getWidth() - logoSize) / 2f, y, logoSize, logoSize);
            else
                g.drawImage(FSkin.getLogo(), (getWidth() - logoSize) / 2f, y, logoSize, logoSize);
            y += logoSize + padding;
            g.drawText(caption, FONT, getForeColor(), x, y, panelWidth, getHeight(), false, Align.center, false);
        } else {
            g.drawText(caption, FONT, getForeColor(), 0, 0, getWidth(), getHeight(), true, Align.center, true);
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (match)
            return true;
        return super.keyDown(keyCode);
    }

    private class BGAnimation extends ForgeAnimation {
        float DURATION = afterMatch ? 0.8f : 1.3f;
        private float progress = 0;

        public void drawBackground(Graphics g) {
            float percentage = progress / DURATION;
            float oldAlpha = g.getfloatAlphaComposite();
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            if (afterMatch) {
                g.drawGrayTransitionImage(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), false, percentage);
            } else {
                if (alternate)
                    g.drawPortalFade(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage > 0.8f ? 0.8f : percentage, afterMatch);
                else
                    g.drawNoiseFade(textureRegion, 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight(), percentage);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            match = false;
            hide();
            if (runnable != null)
                runnable.run();
        }
    }
}
