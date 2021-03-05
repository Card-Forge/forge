package forge.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.gui.FThreads;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.toolbox.FOverlay;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class LoadingOverlay extends FOverlay {
    private static final float INSETS = Utils.scale(10);
    private static final float LOGO_SIZE_FACTOR = 0.7f;
    private static final float INSETS_FACTOR = 0.025f;
    private static final FSkinFont FONT = FSkinFont.get(22);
    private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_ACTIVE).alphaColor(0.75f);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);

    public static void show(String caption0, final Runnable runnable) {
        final LoadingOverlay loader = new LoadingOverlay(caption0);
        loader.show(); //show loading overlay then delay running remaining logic so UI can respond
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                        loader.hide();
                        loader.finishedloading(); //setLoadingaMatch to false
                    }
                });
            }
        });
    }

    public static void runBackgroundTask(String caption0, final Runnable task) {
        final LoadingOverlay loader = new LoadingOverlay(caption0);
        loader.show();
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                task.run();
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        loader.hide();
                    }
                });
            }
        });
    }

    private String caption;

    public LoadingOverlay(String caption0) {
        caption = caption0;
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
        g.setAlphaComposite(0.6f);
        g.fillRect(Color.BLACK, 0, 0, getWidth(), getHeight());
        g.setAlphaComposite(oldAlpha);
        //overlay
        g.fillRect(BACK_COLOR, x, y, panelWidth, panelHeight);
        g.drawRect(Utils.scale(2), FORE_COLOR, x, y, panelWidth, panelHeight);
        y += padding;
        if (FSkin.hdLogo == null)
            g.drawImage(FSkinImage.LOGO, (getWidth() - logoSize) / 2f, y, logoSize, logoSize);
        else
            g.drawImage(FSkin.hdLogo, (getWidth() - logoSize) / 2f, y, logoSize, logoSize);
        y += logoSize + padding;
        g.drawText(caption, FONT, FORE_COLOR, x, y, panelWidth, getHeight(), false, Align.center, false);
    }
}
