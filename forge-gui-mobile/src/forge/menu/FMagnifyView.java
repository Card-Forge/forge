package forge.menu;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

public class FMagnifyView extends FDropDown {
    private static final float PADDING = Utils.scaleMin(5);
    private static final TextRenderer renderer = new TextRenderer(true);

    private FDisplayObject owner;
    private String text;
    private FSkinColor foreColor, backColor;
    private FSkinFont font;
    private float offsetX, width;

    public static void show(FDisplayObject owner0, String text0, FSkinColor foreColor0, FSkinColor backColor0, FSkinFont font0, float offsetX0, float width0) {
        FMagnifyView view = new FMagnifyView();
        view.owner = owner0;
        view.text = text0;
        view.foreColor = foreColor0;
        view.backColor = backColor0;
        view.font = font0;
        view.offsetX = offsetX0;
        view.width = width0;
        view.show();
    }
    private FMagnifyView() {
    }

    @Override
    protected void updateSizeAndPosition() {
        float x = owner.getScreenPosition().x + offsetX;
        float y = owner.getScreenPosition().y + owner.getHeight();
        paneSize = updateAndGetPaneSize(width, y);
        float height = paneSize.getHeight();
        if (height > y) {
            height = y;
        }
        y -= height;

        setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    @Override
    protected boolean autoHide() {
        return true;
    }

    @Override
    protected FDisplayObject getDropDownOwner() {
        return owner;
    }

    @Override
    protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxVisibleHeight) {
        TextBounds bounds = renderer.getWrappedBounds(text, font, maxWidth - 2 * PADDING);
        return new ScrollBounds(maxWidth, bounds.height + 2 * PADDING);
    }

    @Override
    public void drawBackground(Graphics g) {
        super.drawBackground(g);
        g.fillRect(backColor, 0, 0, getWidth(), getHeight());
        renderer.drawText(g, text, font, foreColor, PADDING - getScrollLeft(), PADDING - getScrollTop(), getScrollWidth() - 2 * PADDING, getScrollHeight() - 2 * PADDING, true, HAlignment.LEFT, false);
    }
}
