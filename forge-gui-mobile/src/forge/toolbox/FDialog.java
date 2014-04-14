package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinTexture;
import forge.assets.FSkinColor.Colors;
import forge.screens.FScreen;
import forge.util.Utils;

public abstract class FDialog extends FOverlay {
    protected static final FSkinColor TITLE_FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    protected static final FSkinColor TITLE_BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    protected static final FSkinColor TITLE_BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
    public static final float TITLE_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);

    private FLabel lblTitlebar;
    
    protected FDialog(String title) {
        lblTitlebar = add(new FLabel.Builder().text(title).fontSize(16).align(HAlignment.LEFT).build());
    }

    @Override
    protected final void doLayout(float width, float height) {
        float contentHeight = layoutAndGetHeight(width, height - TITLE_HEIGHT);
        lblTitlebar.setBounds(0, height - TITLE_HEIGHT - contentHeight, width, TITLE_HEIGHT);

        //move all children below titlebar
        float dy = lblTitlebar.getBottom();
        for (FDisplayObject child : getChildren()) {
            if (child != lblTitlebar) {
                child.setTop(child.getTop() + dy);
            }
        }
    }

    protected abstract float layoutAndGetHeight(float width, float maxHeight);

    @Override
    protected void drawBackground(Graphics g) {
        float y = lblTitlebar.getTop();
        float w = getWidth();
        float h = getHeight() - y;
        g.drawImage(FSkinTexture.BG_TEXTURE, 0, y, w, h);
        g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, 0, y, w, h);

        //draw custom background behind titlebar
        g.fillRect(TITLE_BACK_COLOR, 0, y, w, TITLE_HEIGHT);
        g.drawLine(1, TITLE_BORDER_COLOR, 0, y, w, TITLE_HEIGHT);
    }
}
