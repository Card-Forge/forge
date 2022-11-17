package forge.menu;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.toolbox.FCheckBox;
import forge.toolbox.FEvent.FEventHandler;

public class FCheckBoxMenuItem extends FMenuItem {
    public static final float CHECKBOX_SIZE = HEIGHT * 0.45f;
    public static final float PADDING = (HEIGHT - CHECKBOX_SIZE) / 3;
    public static FSkinColor foreColor() {
        if (Forge.isMobileAdventureMode)
            return FSkinColor.get(Colors.ADV_CLR_TEXT);
        return FSkinColor.get(Colors.CLR_TEXT);
    }
    public static FSkinColor getCheckboxColor() {
        return foreColor().alphaColor(0.5f);
    }

    private final boolean checked;

    public FCheckBoxMenuItem(String text0, boolean checked0, FEventHandler handler0) {
        this(text0, checked0, null, handler0, true);
    }
    public FCheckBoxMenuItem(String text0, boolean checked0, FEventHandler handler0, boolean enabled0) {
        this(text0, checked0, null, handler0, enabled0);
    }
    public FCheckBoxMenuItem(String text0, boolean checked0, FImage icon0, FEventHandler handler0) {
        this(text0, checked0, icon0, handler0, true);
    }
    public FCheckBoxMenuItem(String text0, boolean checked0, FImage icon0, FEventHandler handler0, boolean enabled0) {
        super(text0, icon0, handler0, enabled0);
        checked = checked0;
    }

    @Override
    public float getMinWidth() {
        return super.getMinWidth() + CHECKBOX_SIZE + 2 * PADDING - GAP_X;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);

        float w = CHECKBOX_SIZE;
        float h = CHECKBOX_SIZE;
        float x = getWidth() - PADDING - w;
        float y = (getHeight() - h) / 2;
        FCheckBox.drawCheckBox(g, getCheckboxColor(), foreColor(), checked, x, y, w, h);
    }
}
