package forge.screens;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.screens.home.HomeScreen;
import forge.toolbox.FDialog;
import forge.toolbox.FOverlay;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class LoadingOverlay extends FOverlay {
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
                    }
                });
            }
        });
    }

    private final String caption;

    private LoadingOverlay(String caption0) {
        caption = caption0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    public void drawOverlay(Graphics g) {
        float insets = FDialog.INSETS;
        float x = insets;
        float w = getWidth() - 2 * insets;

        float padding = w * HomeScreen.INSETS_FACTOR;
        float logoSize = w * HomeScreen.LOGO_SIZE_FACTOR;
        float fontHeight = FONT.getCapHeight();
        float panelHeight = logoSize + fontHeight + 4 * padding;

        float y = (getHeight() - panelHeight) / 2;
        g.fillRect(BACK_COLOR, x, y, w, panelHeight);
        g.drawRect(Utils.scaleMax(2), FORE_COLOR, x, y, w, panelHeight);
        y += padding;
        g.drawImage(FSkinImage.LOGO, (getWidth() - logoSize) / 2f, y, logoSize, logoSize);
        y += logoSize + padding;
        g.drawText(caption, FONT, FORE_COLOR, x, y, w, getHeight(), false, HAlignment.CENTER, false);
    }
}
