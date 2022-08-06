package forge.assets;

import com.badlogic.gdx.graphics.Color;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.TargetingOverlay;

import java.util.HashMap;

public class FSkinColor {
    public enum Colors {
        CLR_THEME                   (FSkinProp.CLR_THEME),
        CLR_BORDERS                 (FSkinProp.CLR_BORDERS),
        CLR_ZEBRA                   (FSkinProp.CLR_ZEBRA),
        CLR_HOVER                   (FSkinProp.CLR_HOVER),
        CLR_ACTIVE                  (FSkinProp.CLR_ACTIVE),
        CLR_INACTIVE                (FSkinProp.CLR_INACTIVE),
        CLR_TEXT                    (FSkinProp.CLR_TEXT),
        CLR_PHASE_INACTIVE_ENABLED  (FSkinProp.CLR_PHASE_INACTIVE_ENABLED),
        CLR_PHASE_INACTIVE_DISABLED (FSkinProp.CLR_PHASE_INACTIVE_DISABLED),
        CLR_PHASE_ACTIVE_ENABLED    (FSkinProp.CLR_PHASE_ACTIVE_ENABLED),
        CLR_PHASE_ACTIVE_DISABLED   (FSkinProp.CLR_PHASE_ACTIVE_DISABLED),
        CLR_THEME2                  (FSkinProp.CLR_THEME2),
        CLR_OVERLAY                 (FSkinProp.CLR_OVERLAY),
        CLR_COMBAT_TARGETING_ARROW  (FSkinProp.CLR_COMBAT_TARGETING_ARROW),
        CLR_NORMAL_TARGETING_ARROW  (FSkinProp.CLR_NORMAL_TARGETING_ARROW),
        CLR_PWATTK_TARGETING_ARROW  (FSkinProp.CLR_PWATTK_TARGETING_ARROW);

        private Color color;
        private final int x, y;
        private final FSkinProp skinProp;

        Colors(final FSkinProp skinProp0) {
            skinProp = skinProp0;
            int[] coords = skinProp.getCoords();
            x = coords[0];
            y = coords[1];
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setColor(Color color0) {
            color = color0;
        }

        public static Colors fromSkinProp(FSkinProp skinProp) {
            for (final Colors c : Colors.values()) {
                if (c.skinProp == skinProp) {
                    return c;
                }
            }
            return null;
        }
    }

    public static FSkinColor get(final Colors c0) {
        return baseColors.get(c0);
    }

    public static FSkinColor getStandardColor(int r, int g, int b) {
        return getStandardColor(fromRGB(r, g, b));
    }
    public static FSkinColor getStandardColor(final Color c0) {
        return new FSkinColor(c0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
    }

    private static final HashMap<Colors, FSkinColor> baseColors = new HashMap<>();
    private static final HashMap<String, FSkinColor> derivedColors = new HashMap<>();
    private static final int NO_BRIGHTNESS_DELTA = 0;
    private static final int NO_STEP = -999; //needs to be large negative since small negative values are valid
    private static final int NO_ALPHA = -1;

    private final Colors baseColor;
    private final int brightnessDelta;
    private final int step;
    private final int contrastStep;
    private final float alpha;
    protected Color color;

    public Color getColor() { return color; }

    private FSkinColor(Colors baseColor0) {
        this(baseColor0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
    }
    private FSkinColor(Colors baseColor0, int brightnessDelta0, int step0, int contrastStep0, float alpha0) {
        baseColor = baseColor0;
        brightnessDelta = brightnessDelta0;
        step = step0;
        contrastStep = contrastStep0;
        alpha = alpha0;
        updateColor();
    }
    private FSkinColor(Color color0, int brightnessDelta0, int step0, int contrastStep0, float alpha0) {
        color = color0;
        baseColor = null;
        brightnessDelta = brightnessDelta0;
        step = step0;
        contrastStep = contrastStep0;
        alpha = alpha0;
        updateColor();
    }

    private FSkinColor getDerivedColor(int brightnessDelta0, int step0, int contrastStep0, float alpha0) {
        if (baseColor == null) { //handle deriving from standard color
            return new FSkinColor(color, brightnessDelta0, step0, contrastStep0, alpha0);
        }
        String key = baseColor.name() + "|" + brightnessDelta0 + "|" + step0 + "|" + contrastStep0 + "|" + alpha0;
        FSkinColor derivedColor = derivedColors.get(key);
        if (derivedColor == null) {
            derivedColor = new FSkinColor(baseColor, brightnessDelta0, step0, contrastStep0, alpha0);
            derivedColors.put(key, derivedColor);
        }
        return derivedColor;
    }

    public FSkinColor brighter() {
        return getDerivedColor(brightnessDelta + 1, step, contrastStep, alpha);
    }

    public FSkinColor darker() {
        return getDerivedColor(brightnessDelta - 1, step, contrastStep, alpha);
    }

    public FSkinColor stepColor(int step0) {
        if (step != NO_STEP) {
            step0 += step;
        }
        return getDerivedColor(brightnessDelta, step0, contrastStep, alpha);
    }

    public FSkinColor getContrastColor(int contrastStep0) {
        if (contrastStep != NO_STEP) {
            contrastStep0 += contrastStep;
        }
        return getDerivedColor(brightnessDelta, step, contrastStep0, alpha);
    }

    public FSkinColor getHighContrastColor() {
        return getContrastColor(255);
    }

    public FSkinColor alphaColor(float alpha0) {
        return getDerivedColor(brightnessDelta, step, contrastStep, alpha0);
    }

    protected void updateColor() {
        if (baseColor != null) {
            color = baseColor.color;
        }
        if (brightnessDelta != NO_BRIGHTNESS_DELTA) {
            if (brightnessDelta < 0) {
                for (int i = 0; i > brightnessDelta; i--) {
                    color = FSkinColor.stepColor(color, -20);
                }
            }
            else {
                for (int i = 0; i < brightnessDelta; i++) {
                    color = FSkinColor.stepColor(color, 20);
                }
            }
        }
        if (step != NO_STEP) {
            color = FSkinColor.stepColor(color, step);
        }
        if (contrastStep != NO_STEP) {
            color = FSkinColor.stepColor(color, FSkinColor.isColorBright(color) ? -contrastStep : contrastStep);
        }
        if (alpha != NO_ALPHA) {
            color = FSkinColor.alphaColor(color, alpha);
        }
    }

    /** Steps RGB components of a color up or down.
     * Returns opaque (non-alpha) stepped color.
     * Plus for lighter, minus for darker.
     *
     * @param clr0 {Color}
     * @param step int
     * @return {@link Color}
     */
    public static Color stepColor(Color clr0, int step) {
        float r = clr0.r * 255;
        float g = clr0.g * 255;
        float b = clr0.b * 255;

        // Darker
        if (step < 0) {
            r =  ((r + step > 0) ? r + step : 0);
            g =  ((g + step > 0) ? g + step : 0);
            b =  ((b + step > 0) ? b + step : 0);
        }
        else {
            r =  ((r + step < 255) ? r + step : 255);
            g =  ((g + step < 255) ? g + step : 255);
            b =  ((b + step < 255) ? b + step : 255);
        }

        return new Color(r / 255, g / 255, b / 255, clr0.a);
    }

    /**
     * Returns RGB components of a color, with a new
     * value for alpha. 0f = transparent, 1f = opaque.
     */
    public static Color alphaColor(Color clr0, float alpha) {
        return new Color(clr0.r, clr0.g, clr0.b, alpha);
    }

    /**
     * @see http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
     */
    public static boolean isColorBright(Color c) {
        double v = Math.sqrt(
                c.r * c.r * 0.241 +
                c.g * c.g * 0.691 +
                c.b * c.b * 0.068);
        return v > 0.5;
    }

    public static Color getHighContrastColor(Color c) {
        return isColorBright(c) ? Color.BLACK : Color.WHITE;
    }

    public static Color tintColor(Color source, Color tint, float alpha) {
        float r = (tint.r - source.r) * alpha + source.r;
        float g = (tint.g - source.g) * alpha + source.g;
        float b = (tint.b - source.b) * alpha + source.b;
        return new Color(r, g, b, 1f);
    }

    public static Color[] tintColors(Color source, Color[] tints, float alpha) {
        Color[] tintedColors = new Color[tints.length];
        for (int i = 0; i < tints.length; i++) {
            tintedColors[i] = tintColor(source, tints[i], alpha);
        }
        return tintedColors;
    }

    public static Color fromRGB(int r, int g, int b) {
        return new Color((float)r / 255f, (float)g / 255f, (float)b / 255f, 1f);
    }

    public static void updateAll() {
        if (FSkinColor.baseColors.size() == 0) { //initialize base skin colors if needed
            for (final Colors c : Colors.values()) {
                FSkinColor.baseColors.put(c, new FSkinColor(c));
            }
        }
        else { //update existing FSkinColors if baseColors already initialized
            for (final FSkinColor c : FSkinColor.baseColors.values()) {
                c.updateColor();
            }
            for (final FSkinColor c : FSkinColor.derivedColors.values()) {
                c.updateColor();
            }
        }
        TargetingOverlay.updateColors();
    }

    public float getAlpha() {
        return color.a;
    }
}
