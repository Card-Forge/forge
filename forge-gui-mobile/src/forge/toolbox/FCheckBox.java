package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.util.Utils;

public class FCheckBox extends FLabel {
    private static final FSkinColor CHECK_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor BOX_COLOR = CHECK_COLOR.alphaColor(0.5f);

    public FCheckBox() {
        this("", false);
    }
    public FCheckBox(String text0) {
        this(text0, false);
    }
    public FCheckBox(String text0, boolean selected0) {
        super(new Builder().text(text0).align(HAlignment.LEFT).selectable().selected(selected0));
        setIcon(new CheckBoxIcon());
    }

    private class CheckBoxIcon implements FImage {
        @Override
        public float getWidth() {
            return FCheckBox.this.getHeight();
        }

        @Override
        public float getHeight() {
            return FCheckBox.this.getHeight();
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            drawCheckBox(g, isSelected(), x, y, w, h);
        }
    }

    @Override
    public void draw(Graphics g) {
        drawContent(g, getWidth(), getHeight(), false);
    }

    public static void drawCheckBox(Graphics g, boolean isChecked, float x, float y, float w, float h) {
        drawCheckBox(g, BOX_COLOR, CHECK_COLOR, isChecked, x, y, w, h);
    }
    public static void drawCheckBox(Graphics g, FSkinColor boxColor, FSkinColor checkColor, boolean isChecked, float x, float y, float w, float h) {
        g.drawRect(Utils.scaleMin(1), boxColor, x, y, w, h);
        if (isChecked) {
            //draw check mark
            float padX = Utils.scaleX(3);
            float thickness = Utils.scaleMin(2);
            x += padX;
            y += Utils.scaleY(1);
            w -= 2 * padX;
            h -= Utils.scaleY(3);
            g.drawLine(thickness, checkColor, x, y + h / 2, x + w / 2, y + h);
            g.drawLine(thickness, checkColor, x + w / 2, y + h, x + w, y);
        }
    }
}
