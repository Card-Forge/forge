package forge.assets;

import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;

public class FSkinColor {
    public enum Colors {
        CLR_THEME                   (70, 10),
        CLR_BORDERS                 (70, 30),
        CLR_ZEBRA                   (70, 50),
        CLR_HOVER                   (70, 70),
        CLR_ACTIVE                  (70, 90),
        CLR_INACTIVE                (70, 110),
        CLR_TEXT                    (70, 130),
        CLR_PHASE_INACTIVE_ENABLED  (70, 150),
        CLR_PHASE_INACTIVE_DISABLED (70, 170),
        CLR_PHASE_ACTIVE_ENABLED    (70, 190),
        CLR_PHASE_ACTIVE_DISABLED   (70, 210),
        CLR_THEME2                  (70, 230),
        CLR_OVERLAY                 (70, 250),
        CLR_COMBAT_TARGETING_ARROW  (70, 270),
        CLR_NORMAL_TARGETING_ARROW  (70, 290);

        private Color color;
        private final int x, y;
    
        /** @param xy &emsp; int[] coordinates */
        Colors(final int x0, final int y0) {
            x = x0;
            y = y0;
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
    }

    public static FSkinColor get(final Colors c0) {
        return baseColors.get(c0);
    }

    public static FSkinColor getStandardColor(int r, int g, int b) {
        return getStandardColor(r, g, b, 1);
    }
    public static FSkinColor getStandardColor(int r, int g, int b, float a) {
        float div = 255f;
        return getStandardColor(new Color((float)r / div, (float)g / div, (float)b / div, a));
    }
    public static FSkinColor getStandardColor(final Color c0) {
        return new FSkinColor(c0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
    }

    private static final HashMap<Colors, FSkinColor> baseColors = new HashMap<Colors, FSkinColor>();
    private static final HashMap<String, FSkinColor> derivedColors = new HashMap<String, FSkinColor>();
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
                    color = FSkinColor.stepColor(color, 10);
                }
            }
            else {
                for (int i = 0; i < brightnessDelta; i++) {
                    color = FSkinColor.stepColor(color, -10);
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
     * @param clr0 {@link java.awt.Color}
     * @param step int
     * @return {@link java.awt.Color}
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

        return new Color(r / 255, g / 255, b / 255, 0);
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
    }
}
