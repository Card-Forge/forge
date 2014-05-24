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

    public static void show(FDisplayObject owner0, String text0, FSkinColor foreColor0, FSkinColor backColor0, FSkinFont font0) {
        FMagnifyView view = new FMagnifyView();
        view.owner = owner0;
        view.text = text0;
        view.foreColor = foreColor0;
        view.backColor = backColor0;
        view.font = font0;
        view.show();
    }
    private FMagnifyView() {
    }

    @Override
    protected void updateSizeAndPosition() {
        float x = owner.getScreenPosition().x;
        float y = owner.getScreenPosition().y + owner.getHeight();
        paneSize = updateAndGetPaneSize(owner.getWidth(), y);
        float height = paneSize.getHeight();
        if (height > y) {
            height = y;
        }
        y -= height;

        setBounds(Math.round(x), Math.round(y), Math.round(owner.getWidth()), Math.round(height));
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
