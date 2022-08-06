package forge.toolbox;

import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class FRadioButton extends FLabel {
    private static final FSkinColor INNER_CIRCLE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor OUTER_CIRCLE_COLOR = INNER_CIRCLE_COLOR.alphaColor(0.5f);
    private static final float EXTRA_GAP = Utils.scale(3);
    
    private RadioButtonGroup group;

    public FRadioButton() {
        this("", false);
    }
    public FRadioButton(String text0) {
        this(text0, false);
    }
    public FRadioButton(String text0, boolean selected0) {
        super(new Builder().text(text0).align(Align.left).selectable().selected(selected0));
        setIcon(new RadioButtonIcon());
    }
    
    public RadioButtonGroup getGroup() {
        return group;
    }
    public void setGroup(RadioButtonGroup group0) {
        if (group != null) {
            group.buttons.remove(this);
        }
        group = group0;
        if (group != null) {
            group.buttons.add(this);
        }
    }

    @Override
    protected float getExtraGapBetweenIconAndText() {
        return EXTRA_GAP;
    }

    @Override
    public void setSelected(final boolean b0) {
        if (isSelected() == b0 || !b0) { return; } //don't support unselecting radio button

        if (b0 && group != null) { //if selecting and in group, unselect all other radio buttons in group
            for (FRadioButton button : group.buttons) {
                if (button != this) {
                    button.superSetSelected(false);
                }
            }
        }
        superSetSelected(b0);
    }

    private void superSetSelected(final boolean b0) {
        super.setSelected(b0);
    }

    private class RadioButtonIcon implements FImage {
        @Override
        public float getWidth() {
            return FRadioButton.this.getHeight();
        }

        @Override
        public float getHeight() {
            return FRadioButton.this.getHeight();
        }

        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            float radius = h / 3;
            x += w - radius;
            y += h / 2;
            g.drawCircle(Utils.scale(1), OUTER_CIRCLE_COLOR, x, y, radius);
            if (isSelected()) {
                g.fillCircle(INNER_CIRCLE_COLOR, x, y, radius / 2);
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        drawContent(g, getWidth(), getHeight(), false);
    }

    public static class RadioButtonGroup {
        private final List<FRadioButton> buttons = new ArrayList<>();
    }
}
