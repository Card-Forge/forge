package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.util.Utils;

public abstract class FDialog extends FOverlay {
    private static final FSkinColor TITLE_BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
    private static final float TITLE_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);
    private static float INSET_X = 10;

    private FLabel lblTitlebar;
    private float totalHeight;

    protected FDialog(String title) {
        lblTitlebar = add(new FLabel.Builder().text(title).icon(FSkinImage.FAVICON).fontSize(12).align(HAlignment.LEFT).build());
    }

    @Override
    protected final void doLayout(float width, float height) {
        width -= 2 * INSET_X;

        float contentHeight = layoutAndGetHeight(width, height - TITLE_HEIGHT);
        totalHeight = contentHeight + TITLE_HEIGHT;
        float y = (height - totalHeight) / 2;

        lblTitlebar.setBounds(INSET_X, y, width, TITLE_HEIGHT);

        //shift all children into position below titlebar
        float dy = lblTitlebar.getBottom();
        for (FDisplayObject child : getChildren()) {
            if (child != lblTitlebar) {
                child.setLeft(child.getLeft() + INSET_X);
                child.setTop(child.getTop() + dy);
            }
        }
    }

    protected abstract float layoutAndGetHeight(float width, float maxHeight);

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);

        float x = INSET_X;
        float y = lblTitlebar.getTop();
        float w = getWidth() - 2 * x;
        float h = totalHeight;
        g.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);

        //draw custom background behind titlebar
        g.fillRect(TITLE_BACK_COLOR, x, y, w, TITLE_HEIGHT);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float x = INSET_X;
        float y = lblTitlebar.getTop();
        float w = getWidth() - 2 * x;
        float h = totalHeight;

        //draw border around dialog
        g.drawRect(1, BORDER_COLOR, x, y, w, h);

        //draw bottom border of titlebar
        y += TITLE_HEIGHT;
        g.drawLine(1, BORDER_COLOR, x, y, x + w, y);
    }
}
