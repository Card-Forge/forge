package forge.assets;

import java.awt.Color;
import java.util.HashMap;

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
    
        public Color getColor() {
            return color;
        }
    
        public void setColor(Color color0) {
            color = color0;
        }
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
    private final int alpha;
    protected Color color;

    public Color getColor() { return color; }

    //private constructors for color that changes with skin (use FSkin.getColor())
    private FSkinColor(Colors baseColor0) {
        this(baseColor0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
    }
    private FSkinColor(Colors baseColor0, int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
        this.baseColor = baseColor0;
        this.brightnessDelta = brightnessDelta0;
        this.step = step0;
        this.contrastStep = contrastStep0;
        this.alpha = alpha0;
        this.updateColor();
    }

    private FSkinColor getDerivedColor(int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
        String key = this.baseColor.name() + "|" + brightnessDelta0 + "|" + step0 + "|" + contrastStep0 + "|" + alpha0;
        FSkinColor derivedColor = derivedColors.get(key);
        if (derivedColor == null) {
            derivedColor = new FSkinColor(this.baseColor, brightnessDelta0, step0, contrastStep0, alpha0);
            derivedColors.put(key, derivedColor);
        }
        return derivedColor;
    }

    public FSkinColor brighter() {
        return getDerivedColor(this.brightnessDelta + 1, this.step, this.contrastStep, this.alpha);
    }

    public FSkinColor darker() {
        return getDerivedColor(this.brightnessDelta - 1, this.step, this.contrastStep, this.alpha);
    }

    public FSkinColor stepColor(int step0) {
        if (this.step != NO_STEP) {
            step0 += this.step;
        }
        return getDerivedColor(this.brightnessDelta, step0, this.contrastStep, this.alpha);
    }

    public FSkinColor getContrastColor(int contrastStep0) {
        if (this.contrastStep != NO_STEP) {
            contrastStep0 += this.contrastStep;
        }
        return getDerivedColor(this.brightnessDelta, this.step, contrastStep0, this.alpha);
    }

    public FSkinColor getHighContrastColor() {
        return getContrastColor(255);
    }

    public FSkinColor alphaColor(int alpha0) {
        return getDerivedColor(this.brightnessDelta, this.step, this.contrastStep, alpha0);
    }

    protected void updateColor() {
        this.color = this.baseColor.color;
        if (this.brightnessDelta != NO_BRIGHTNESS_DELTA) {
            if (this.brightnessDelta < 0) {
                for (int i = 0; i > this.brightnessDelta; i--) {
                    this.color = this.color.darker();
                }
            }
            else {
                for (int i = 0; i < this.brightnessDelta; i++) {
                    this.color = this.color.brighter();
                }
            }
        }
        if (this.step != NO_STEP) {
            this.color = FSkinColor.stepColor(this.color, this.step);
        }
        if (this.contrastStep != NO_STEP) {
            this.color = FSkinColor.stepColor(this.color, FSkinColor.isColorBright(this.color) ? -this.contrastStep : this.contrastStep);
        }
        if (this.alpha != NO_ALPHA) {
            this.color = FSkinColor.alphaColor(this.color, this.alpha);
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
        int r = clr0.getRed();
        int g = clr0.getGreen();
        int b = clr0.getBlue();

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

        return new Color(r, g, b);
    }

    /** Returns RGB components of a color, with a new
     * value for alpha. 0 = transparent, 255 = opaque.
     *
     * @param clr0 {@link java.awt.Color}
     * @param alpha int
     * @return {@link java.awt.Color}
     */
    public static Color alphaColor(Color clr0, int alpha) {
        return new Color(clr0.getRed(), clr0.getGreen(), clr0.getBlue(), alpha);
    }

    /**
     * @see http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
     */
    public static boolean isColorBright(Color c) {
        int v = (int)Math.sqrt(
                c.getRed() * c.getRed() * 0.241 +
                c.getGreen() * c.getGreen() * 0.691 +
                c.getBlue() * c.getBlue() * 0.068);
        return v >= 130;
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
