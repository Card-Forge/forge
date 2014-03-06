package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.Forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;

public class FLabel extends FDisplayObject {
    public static class Builder {
        //========== Default values for FLabel are set here.
        private float      bldIconScaleFactor = 0.8f;
        private int        bldFontSize        = 14;
        private HAlignment bldAlignment       = HAlignment.LEFT;
        private Vector2    bldInsets          = new Vector2(0, 0);

        private boolean bldSelectable       = false;
        private boolean bldSelected         = false;
        private boolean bldOpaque           = false;
        private boolean bldIconInBackground = false;
        private boolean bldIconScaleAuto    = true;
        private boolean bldEnabled          = true;

        private String bldText;
        private FImage bldIcon;
        private Runnable bldCommand;

        public FLabel build() { return new FLabel(this); }

        // Begin builder methods.
        public Builder text(final String s0) { this.bldText = s0; return this; }
        public Builder icon(final FImage i0) { this.bldIcon = i0; return this; }
        public Builder align(final HAlignment a0) { this.bldAlignment = a0; return this; }
        public Builder insets(final Vector2 v0) { this.bldInsets = v0; return this; }
        public Builder opaque(final boolean b0) { this.bldOpaque = b0; return this; }
        public Builder opaque() { opaque(true); return this; }
        public Builder selectable(final boolean b0) { this.bldSelectable = b0; return this; }
        public Builder selectable() { selectable(true); return this; }
        public Builder selected(final boolean b0) { this.bldSelected = b0; return this; }
        public Builder selected() { selected(true); return this; }
        public Builder command(final Runnable c0) { this.bldCommand = c0; return this; }
        public Builder fontSize(final int i0) { this.bldFontSize = i0; return this; }
        public Builder enabled(final boolean b0) { this.bldEnabled = b0; return this; }
        public Builder iconScaleAuto(final boolean b0) { this.bldIconScaleAuto = b0; return this; }
        public Builder iconScaleFactor(final float f0) { this.bldIconScaleFactor = f0; return this; }
        public Builder iconInBackground(final boolean b0) { this.bldIconInBackground = b0; return this; }
        public Builder iconInBackground() { iconInBackground(true); return this; }
    }

    // sets better defaults for button labels
    public static class ButtonBuilder extends Builder {
        public ButtonBuilder() {
            opaque();
            align(HAlignment.CENTER);
        }
    }

    private static final FSkinColor clrText = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor clrMain = FSkinColor.get(Colors.CLR_INACTIVE);
    private static final FSkinColor d50 = clrMain.stepColor(-50);
    private static final FSkinColor d30 = clrMain.stepColor(-30);
    private static final FSkinColor d10 = clrMain.stepColor(-10);
    private static final FSkinColor l10 = clrMain.stepColor(10);
    private static final FSkinColor l20 = clrMain.stepColor(20);
    private static final FSkinColor l30 = clrMain.stepColor(30);

    private float iconScaleFactor;
    private FSkinFont font;
    private HAlignment alignment;
    private Vector2 insets;
    private boolean selectable, selected, opaque, iconInBackground, iconScaleAuto, pressed;

    private String text;
    private FImage icon;
    private Runnable command;

    // Call this using FLabel.Builder()...
    protected FLabel(final Builder b0) {
        iconScaleFactor = b0.bldIconScaleFactor;
        font = FSkinFont.get(b0.bldFontSize);
        alignment = b0.bldAlignment;
        insets = b0.bldInsets;
        selectable = b0.bldSelectable;
        selected = b0.bldSelected;
        opaque = b0.bldOpaque;
        iconInBackground = b0.bldIconInBackground;
        iconScaleAuto = b0.bldIconScaleAuto;
        text = b0.bldText != null ? b0.bldText : "";
        icon = b0.bldIcon;
        command = b0.bldCommand;
        setEnabled(b0.bldEnabled);
    }

    public boolean getSelected() {
        return selected;
    }
    public void setSelected(final boolean b0) {
        selected = b0;
    }

    public String getText() {
        return text;
    }
    public void setText(final String text0) {
        text = text0;
    }

    public FImage getIcon() {
        return icon;
    }
    public void setIcon(final FImage icon0) {
        icon = icon0;
    }

    @Override
    public final boolean touchDown(float x, float y) {
        if (opaque || selectable) {
            pressed = true;
            return true;
        }
        return false;
    }

    @Override
    public final boolean touchUp(float x, float y) {
        if (pressed) {
            pressed = false;
            return true;
        }
        return false;
    }

    @Override
    public final boolean tap(float x, float y, int count) {
        boolean handled = false;
        if (selectable) {
            setSelected(!selected);
            handled = true;
        }
        if (command != null) {
            command.run();
            handled = true;
        }
        return handled;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        if (pressed) {
            g.drawRect(1, d50, 0, 0, w, h);
            g.drawRect(1, d10, 1, 1, w - 2, h - 2);
            g.fillGradientRect(d50, d10, true, 2, 2, w - 4, h - 4);
        }
        else if (selected && (opaque || selectable)) {
            g.drawRect(1, d30, 0, 0, w, h);
            g.drawRect(1, l10, 1, 1, w - 2, h - 2);
            g.fillGradientRect(d30, l10, true, 2, 2, w - 4, h - 4);
        }
        else if (opaque) {
            g.drawRect(1, d50, 0, 0, w, h);
            g.drawRect(1, l10, 1, 1, w - 2, h - 2);
            g.fillGradientRect(d10, l20, true, 2, 2, w - 4, h - 4);
        }
        else if (selectable) {
            g.drawRect(1, l10, 0, 0, w, h);
            g.drawRect(1, l30, 1, 1, w - 2, h - 2);
        }

        drawContent(g, w, h, pressed);
    }

    protected void drawContent(Graphics g, float w, float h, final boolean pressed) {
        float x = insets.x;
        float y = insets.y;
        w -= 2 * x;
        h -= 2 * x;
        if (pressed) { //while pressed, translate graphics so icon and text appear shifted down and to the right
            x++;
            y++;
        }

        if (icon != null) {
            float iconWidth = icon.getWidth();
            float iconHeight = icon.getHeight();
            float aspectRatio = iconWidth / iconHeight;

            if (iconInBackground || iconScaleAuto) {
                iconHeight = h * iconScaleFactor;
                iconWidth = iconHeight * aspectRatio;
            }
            if (iconInBackground || text.isEmpty()) {
                if (alignment == HAlignment.CENTER) {
                    x += (w - iconWidth) / 2;
                }
                y += (h - iconHeight) / 2;
            }
            else {
                x = 0; //TODO: calculation these
                y = 0;
            }

            g.drawImage(icon, x, y, iconWidth, iconHeight);

            if (!text.isEmpty()) {
                x += iconWidth;
                g.startClip(x, y, w, h);
                g.drawText(text, font, clrText, x, y, w, h, false, HAlignment.LEFT, true);
                g.endClip();
            }
        }
        else if (!text.isEmpty()) {
            g.startClip(x, y, w, h);
            g.drawText(text, font, clrText, x, y, w, h, false, alignment, true);
            g.endClip();
        }
    }
}
