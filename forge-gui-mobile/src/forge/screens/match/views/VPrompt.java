package forge.screens.match.views;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.assets.TextRenderer;
import forge.menu.FMagnifyView;
import forge.toolbox.FButton;
import forge.toolbox.FButton.Corner;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class VPrompt extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;
    public static final float BTN_WIDTH = HEIGHT * 1.5f;
    public static final float PADDING = Utils.scaleMin(2);

    private static final FSkinColor BACK_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont FONT = FSkinFont.get(14);

    private final TextRenderer renderer = new TextRenderer();
    private final FButton btnOk, btnCancel;
    private String message;

    public VPrompt(String okText, String cancelText, FEventHandler okCommand, FEventHandler cancelCommand) {
        btnOk = add(new FButton(okText, okCommand));
        btnCancel = add(new FButton(cancelText, cancelCommand));
        btnOk.setSize(BTN_WIDTH, HEIGHT);
        btnCancel.setSize(BTN_WIDTH, HEIGHT);
        btnOk.setCorner(Corner.BottomLeft);
        btnCancel.setCorner(Corner.BottomRight);
    }

    public FButton getBtnOk() {
        return btnOk;
    }

    public FButton getBtnCancel() {
        return btnCancel;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message0) {
        message = message0;
    }

    /** Flashes animation on input panel if play is currently waiting on input. */
    public void remind() {
        //SDisplayUtil.remind(view);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        //if not enough room for prompt at given size, show magnify view
        float offsetX = BTN_WIDTH + PADDING;
        float maxWidth = getWidth() - 2 * offsetX;
        float maxHeight = getHeight() - 2 * PADDING;
        TextBounds textBounds = renderer.getWrappedBounds(message, FONT, maxWidth);
        if (textBounds.height > maxHeight) {
            FMagnifyView.show(this, message, FORE_COLOR, BACK_COLOR, FONT, offsetX, maxWidth);
        }
        return true;
    }

    @Override
    protected void doLayout(float width, float height) {
        btnCancel.setLeft(width - BTN_WIDTH);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.fillRect(BACK_COLOR, 0, 0, w, h);
        if (!StringUtils.isEmpty(message)) {
            float x = BTN_WIDTH + PADDING;
            float y = PADDING;
            renderer.drawText(g, message, FONT, FORE_COLOR, x, y, w - 2 * x, h - 2 * y, true, HAlignment.CENTER, true);
        }
    }
}
