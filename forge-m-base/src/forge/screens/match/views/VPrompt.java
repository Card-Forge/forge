package forge.screens.match.views;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FLabel;
import forge.utils.Utils;

public class VPrompt extends FContainer {
    public static final float BTN_WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private static final FSkinColor backColor = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor foreColor = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinFont font = FSkinFont.get(11);

    private final FButton btnOk, btnCancel;
    private String message = "This is where the prompt would be.\nLine 2 of the prompt.\nLine 3 of the prompt.";

    public VPrompt() {
        btnOk = add(new FButton("Yes"));
        btnCancel = add(new FButton("No"));
        btnOk.setSize(BTN_WIDTH, HEIGHT);
        btnCancel.setSize(BTN_WIDTH, HEIGHT);
    }

    @Override
    protected void doLayout(float width, float height) {
        btnCancel.setLeft(width - BTN_WIDTH);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.fillRect(backColor, 0, 0, w, h);
        g.drawText(message, font, foreColor, BTN_WIDTH, 0, w - 2 * BTN_WIDTH, h,
                true, HAlignment.CENTER, true);
    }
}
