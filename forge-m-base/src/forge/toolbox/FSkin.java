package forge.toolbox;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class FSkin {
    public enum Backgrounds implements SkinProp {
        BG_SPLASH (null),
        BG_TEXTURE (null),
        BG_MATCH (null);

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Backgrounds(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param c0 &emsp; Colors property (from enum)
     * @return {@link forge.toolbox.FSkin.SkinColor}
     */
    public static SkinColor getColor(final Colors c0) {
        return SkinColor.baseColors.get(c0);
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

    public static class SkinColor {
        private static final HashMap<Colors, SkinColor> baseColors = new HashMap<Colors, SkinColor>();
        private static final HashMap<String, SkinColor> derivedColors = new HashMap<String, SkinColor>();
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
        private SkinColor(Colors baseColor0) {
            this(baseColor0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
        }
        private SkinColor(Colors baseColor0, int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
            this.baseColor = baseColor0;
            this.brightnessDelta = brightnessDelta0;
            this.step = step0;
            this.contrastStep = contrastStep0;
            this.alpha = alpha0;
            this.updateColor();
        }

        private SkinColor getDerivedColor(int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
            String key = this.baseColor.name() + "|" + brightnessDelta0 + "|" + step0 + "|" + contrastStep0 + "|" + alpha0;
            SkinColor derivedColor = derivedColors.get(key);
            if (derivedColor == null) {
                derivedColor = new SkinColor(this.baseColor, brightnessDelta0, step0, contrastStep0, alpha0);
                derivedColors.put(key, derivedColor);
            }
            return derivedColor;
        }

        public SkinColor brighter() {
            return getDerivedColor(this.brightnessDelta + 1, this.step, this.contrastStep, this.alpha);
        }

        public SkinColor darker() {
            return getDerivedColor(this.brightnessDelta - 1, this.step, this.contrastStep, this.alpha);
        }

        public SkinColor stepColor(int step0) {
            if (this.step != NO_STEP) {
                step0 += this.step;
            }
            return getDerivedColor(this.brightnessDelta, step0, this.contrastStep, this.alpha);
        }

        public SkinColor getContrastColor(int contrastStep0) {
            if (this.contrastStep != NO_STEP) {
                contrastStep0 += this.contrastStep;
            }
            return getDerivedColor(this.brightnessDelta, this.step, contrastStep0, this.alpha);
        }

        public SkinColor getHighContrastColor() {
            return getContrastColor(255);
        }

        public SkinColor alphaColor(int alpha0) {
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
                this.color = FSkin.stepColor(this.color, this.step);
            }
            if (this.contrastStep != NO_STEP) {
                this.color = FSkin.stepColor(this.color, isColorBright(this.color) ? -this.contrastStep : this.contrastStep);
            }
            if (this.alpha != NO_ALPHA) {
                this.color = FSkin.alphaColor(this.color, this.alpha);
            }
        }
    }

    public enum Colors implements SkinProp {
        CLR_THEME                   (new int[] {70, 10}),
        CLR_BORDERS                 (new int[] {70, 30}),
        CLR_ZEBRA                   (new int[] {70, 50}),
        CLR_HOVER                   (new int[] {70, 70}),
        CLR_ACTIVE                  (new int[] {70, 90}),
        CLR_INACTIVE                (new int[] {70, 110}),
        CLR_TEXT                    (new int[] {70, 130}),
        CLR_PHASE_INACTIVE_ENABLED  (new int[] {70, 150}),
        CLR_PHASE_INACTIVE_DISABLED (new int[] {70, 170}),
        CLR_PHASE_ACTIVE_ENABLED    (new int[] {70, 190}),
        CLR_PHASE_ACTIVE_DISABLED   (new int[] {70, 210}),
        CLR_THEME2                  (new int[] {70, 230}),
        CLR_OVERLAY                 (new int[] {70, 250});

        private Color color;
        private int[] coords;

        /** @param xy &emsp; int[] coordinates */
        Colors(final int[] xy) { this.coords = xy; }

        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }

        public static void updateAll() {
            for (final Colors c : Colors.values()) {
                c.updateColor();
            }
            if (SkinColor.baseColors.size() == 0) { //initialize base skin colors if needed
                for (final Colors c : Colors.values()) {
                    SkinColor.baseColors.put(c, new SkinColor(c));
                }
            }
            else { //update existing SkinColors if baseColors already initialized
                for (final SkinColor c : SkinColor.baseColors.values()) {
                    c.updateColor();
                }
                for (final SkinColor c : SkinColor.derivedColors.values()) {
                    c.updateColor();
                }
            }
        }

        private void updateColor() {
            int[] coords = this.getCoords();
            color = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(coords[0], coords[1]));
        }
    }

    public static void drawImage(SpriteBatch batch, SkinImage skinImage, float x, float y) {
        batch.draw(skinImage.image, x, y);
    }
    public static void drawImage(SpriteBatch batch, SkinImage skinImage, float x, float y, float w, float h) {
        batch.draw(skinImage.image, x, y, w, h);
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link forge.toolbox.FSkin.SkinImage}
     */
    public static SkinImage getImage(final SkinProp s0) {
        SkinImage image = SkinImage.images.get(s0);
        if (image == null) {
            throw new NullPointerException("Can't find an image for SkinProp " + s0);
        }
        return image;
    }

    public static class SkinImage {
        private static final Map<SkinProp, SkinImage> images = new HashMap<SkinProp, SkinImage>();

        private static Texture getTexture(SkinProp s0, int x0, int y0, int w0, int h0) {
         // Test if requested sub-image in inside bounds of preferred sprite.
            // (Height and width of preferred sprite were set in loadFontAndImages.)
            if (x0 + w0 > pxPreferredSprite.getWidth() || y0 + h0 > pxPreferredSprite.getHeight()) {
                return txDefaultSprite;
            }

            // Test if various points of requested sub-image are transparent.
            // If any return true, image exists.
            int x = 0, y = 0;
            Color c;

            // Center
            x = (x0 + w0 / 2);
            y = (y0 + h0 / 2);
            c = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(x, y));
            if (c.getAlpha() != 0) { return txPreferredSprite; }

            x += 2;
            y += 2;
            c = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(x, y));
            if (c.getAlpha() != 0) { return txPreferredSprite; }

            x -= 4;
            c = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(x, y));
            if (c.getAlpha() != 0) { return txPreferredSprite; }

            y -= 4;
            c = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(x, y));
            if (c.getAlpha() != 0) { return txPreferredSprite; }

            x += 4;
            c = FSkin.getColorFromPixel(pxPreferredSprite.getPixel(x, y));
            if (c.getAlpha() != 0) { return txPreferredSprite; }

            return txDefaultSprite;
        }

        private static void setImage(final SkinProp s0) {
            int[] coords = s0.getCoords();
            int x0 = coords[0];
            int y0 = coords[1];
            int w0 = coords[2];
            int h0 = coords[3];

            setImage(s0, new TextureRegion(getTexture(s0, x0, y0, w0, h0), x0, y0, w0, h0));
        }

        private static void setImage(final SkinProp s0, TextureRegion image0) {
            SkinImage skinImage = images.get(s0);
            if (skinImage == null) {
                skinImage = new SkinImage(image0);
                images.put(s0, skinImage);
            }
            else {
                skinImage.changeImage(image0);
            }
        }

        protected TextureRegion image;

        private SkinImage(TextureRegion image0) {
            this.image = image0;
        }

        protected void changeImage(TextureRegion image0) {
            this.image = image0;
        }

        protected SkinImage clone() {
            return new SkinImage(this.image);
        }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ZoneImages implements SkinProp {
        IMG_HAND        (new int[] {280, 40, 40, 40}),
        IMG_LIBRARY     (new int[] {280, 0, 40, 40}),
        IMG_EXILE       (new int[] {320, 40, 40, 40}),
        IMG_FLASHBACK   (new int[] {280, 80, 40, 40}),
        IMG_GRAVEYARD   (new int[] {320, 0, 40, 40}),
        IMG_POISON      (new int[] {320, 80, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ZoneImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ManaImages implements SkinProp {
        IMG_BLACK       (new int[] {360, 160, 40, 40}),
        IMG_RED         (new int[] {400, 160, 40, 40}),
        IMG_COLORLESS   (new int[] {440, 160, 40, 40}),
        IMG_BLUE        (new int[] {360, 200, 40, 40}),
        IMG_GREEN       (new int[] {400, 200, 40, 40}),
        IMG_WHITE       (new int[] {440, 200, 40, 40}),
        IMG_2B          (new int[] {360, 400, 40, 40}),
        IMG_2G          (new int[] {400, 400, 40, 40}),
        IMG_2R          (new int[] {440, 400, 40, 40}),
        IMG_2U          (new int[] {440, 360, 40, 40}),
        IMG_2W          (new int[] {400, 360, 40, 40}),
        IMG_BLACK_GREEN (new int[] {360, 240, 40, 40}),
        IMG_BLACK_RED   (new int[] {400, 240, 40, 40}),
        IMG_GREEN_BLUE  (new int[] {360, 280, 40, 40}),
        IMG_GREEN_WHITE (new int[] {440, 280, 40, 40}),
        IMG_RED_GREEN   (new int[] {360, 320, 40, 40}),
        IMG_RED_WHITE   (new int[] {400, 320, 40, 40}),
        IMG_BLUE_BLACK  (new int[] {440, 240, 40, 40}),
        IMG_BLUE_RED    (new int[] {440, 320, 40, 40}),
        IMG_WHITE_BLACK (new int[] {400, 280, 40, 40}),
        IMG_WHITE_BLUE  (new int[] {360, 360, 40, 40}),
        IMG_PHRYX_BLUE  (new int[] {320, 200, 40, 40}),
        IMG_PHRYX_WHITE (new int[] {320, 240, 40, 40}),
        IMG_PHRYX_RED   (new int[] {320, 280, 40, 40}),
        IMG_PHRYX_GREEN (new int[] {320, 320, 40, 40}),
        IMG_PHRYX_BLACK (new int[] {320, 360, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ManaImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ColorlessManaImages implements SkinProp {
        IMG_0   (new int[] {640, 200, 20, 20}),
        IMG_1   (new int[] {660, 200, 20, 20}),
        IMG_2   (new int[] {640, 220, 20, 20}),
        IMG_3   (new int[] {660, 220, 20, 20}),
        IMG_4   (new int[] {640, 240, 20, 20}),
        IMG_5   (new int[] {660, 240, 20, 20}),
        IMG_6   (new int[] {640, 260, 20, 20}),
        IMG_7   (new int[] {660, 260, 20, 20}),
        IMG_8   (new int[] {640, 280, 20, 20}),
        IMG_9   (new int[] {660, 280, 20, 20}),
        IMG_10  (new int[] {640, 300, 20, 20}),
        IMG_11  (new int[] {660, 300, 20, 20}),
        IMG_12  (new int[] {640, 320, 20, 20}),
        IMG_13  (new int[] {660, 320, 20, 20}),
        IMG_14  (new int[] {640, 340, 20, 20}),
        IMG_15  (new int[] {660, 340, 20, 20}),
        IMG_16  (new int[] {640, 360, 20, 20}),
        IMG_17  (new int[] {660, 360, 20, 20}),
        IMG_18  (new int[] {640, 380, 20, 20}),
        IMG_19  (new int[] {660, 380, 20, 20}),
        IMG_20  (new int[] {640, 400, 20, 20}),
        IMG_X   (new int[] {660, 400, 20, 20}),
        IMG_Y   (new int[] {640, 420, 20, 20}),
        IMG_Z   (new int[] {660, 420, 20, 20});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ColorlessManaImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum GameplayImages implements SkinProp {
        IMG_SNOW            (new int[] {320, 160, 40, 40}),
        IMG_TAP             (new int[] {640, 440, 20, 20}),
        IMG_UNTAP           (new int[] {660, 440, 20, 20}),
        IMG_CHAOS           (new int[] {320, 400, 40, 40}),
        IMG_SLASH           (new int[] {660, 400, 10, 13}),
        IMG_ATTACK          (new int[] {160, 320, 80, 80, 32, 32}),
        IMG_DEFEND          (new int[] {160, 400, 80, 80, 32, 32}),
        IMG_SUMMONSICK      (new int[] {240, 400, 80, 80, 32, 32}),
        IMG_PHASING         (new int[] {240, 320, 80, 80, 32, 32}),
        IMG_COSTRESERVED    (new int[] {240, 240, 80, 80, 40, 40}),
        IMG_COUNTERS1       (new int[] {0, 320, 80, 80}),
        IMG_COUNTERS2       (new int[] {0, 400, 80, 80}),
        IMG_COUNTERS3       (new int[] {80, 320, 80, 80}),
        IMG_COUNTERS_MULTI  (new int[] {80, 400, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        GameplayImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum Foils implements SkinProp {
        FOIL_01     (new int[] {0, 0, 400, 570}),
        FOIL_02     (new int[] {400, 0, 400, 570}),
        FOIL_03     (new int[] {0, 570, 400, 570}),
        FOIL_04     (new int[] {400, 570, 400, 570}),
        FOIL_05     (new int[] {0, 1140, 400, 570}),
        FOIL_06     (new int[] {400, 1140, 400, 570}),
        FOIL_07     (new int[] {0, 1710, 400, 570}),
        FOIL_08     (new int[] {400, 1710, 400, 570}),
        FOIL_09     (new int[] {0, 2280, 400, 570}),
        FOIL_10     (new int[] {400, 2280, 400, 570});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Foils(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum OldFoils implements SkinProp {
        FOIL_11     (new int[] {0, 0, 400, 570}),
        FOIL_12     (new int[] {400, 0, 400, 570}),
        FOIL_13     (new int[] {0, 570, 400, 570}),
        FOIL_14     (new int[] {400, 570, 400, 570}),
        FOIL_15     (new int[] {0, 1140, 400, 570}),
        FOIL_16     (new int[] {400, 1140, 400, 570}),
        FOIL_17     (new int[] {0, 1710, 400, 570}),
        FOIL_18     (new int[] {400, 1710, 400, 570}),
        FOIL_19     (new int[] {0, 2280, 400, 570}),
        FOIL_20     (new int[] {400, 2280, 400, 570});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        OldFoils(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum DockIcons implements SkinProp {
        ICO_SHORTCUTS    (new int[] {160, 640, 80, 80}),
        ICO_SETTINGS     (new int[] {80, 640, 80, 80}),
        ICO_ENDTURN      (new int[] {320, 640, 80, 80}),
        ICO_CONCEDE      (new int[] {240, 640, 80, 80}),
        ICO_REVERTLAYOUT (new int[] {400, 720, 80, 80}),
        ICO_OPENLAYOUT   (new int[] {0, 800, 80, 80}),
        ICO_SAVELAYOUT   (new int[] {80, 800, 80, 80}),
        ICO_DECKLIST     (new int[] {400, 640, 80, 80}),
        ICO_ALPHASTRIKE  (new int[] {160, 800, 80, 80}),
        ICO_ARCSOFF      (new int[] {240, 800, 80, 80}),
        ICO_ARCSON       (new int[] {320, 800, 80, 80}),
        ICO_ARCSHOVER    (new int[] {400, 800, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        DockIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum QuestIcons implements SkinProp {
        ICO_ZEP         (new int[] {0, 480, 80, 80}),
        ICO_GEAR        (new int[] {80, 480, 80, 80}),
        ICO_GOLD        (new int[] {160, 480, 80, 80}),
        ICO_ELIXIR      (new int[] {240, 480, 80, 80}),
        ICO_BOOK        (new int[] {320, 480, 80, 80}),
        ICO_BOTTLES     (new int[] {400, 480, 80, 80}),
        ICO_BOX         (new int[] {480, 480, 80, 80}),
        ICO_COIN        (new int[] {560, 480, 80, 80}),
        ICO_CHARM       (new int[] {480, 800, 80, 80}),

        ICO_FOX         (new int[] {0, 560, 80, 80}),
        ICO_LEAF        (new int[] {80, 560, 80, 80}),
        ICO_LIFE        (new int[] {160, 560, 80, 80}),
        ICO_COINSTACK   (new int[] {240, 560, 80, 80}),
        ICO_MAP         (new int[] {320, 560, 80, 80}),
        ICO_NOTES       (new int[] {400, 560, 80, 80}),
        ICO_HEART       (new int[] {480, 560, 80, 80}),
        ICO_BREW        (new int[] {560, 560, 80, 80}),
        ICO_STAKES      (new int[] {400, 560, 80, 80}),

        ICO_MINUS       (new int[] {560, 640, 80, 80}),
        ICO_PLUS        (new int[] {480, 640, 80, 80}),
        ICO_PLUSPLUS    (new int[] {480, 720, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        QuestIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum InterfaceIcons implements SkinProp {
        ICO_QUESTION        (new int[] {560, 800, 32, 32}),
        ICO_INFORMATION     (new int[] {592, 800, 32, 32}),
        ICO_WARNING         (new int[] {560, 832, 32, 32}),
        ICO_ERROR           (new int[] {592, 832, 32, 32}),
        ICO_DELETE          (new int[] {640, 480, 20, 20}),
        ICO_DELETE_OVER     (new int[] {660, 480, 20, 20}),
        ICO_EDIT            (new int[] {640, 500, 20, 20}),
        ICO_EDIT_OVER       (new int[] {660, 500, 20, 20}),
        ICO_OPEN            (new int[] {660, 520, 20, 20}),
        ICO_MINUS           (new int[] {660, 620, 20, 20}),
        ICO_NEW             (new int[] {660, 540, 20, 20}),
        ICO_PLUS            (new int[] {660, 600, 20, 20}),
        ICO_PRINT           (new int[] {660, 640, 20, 20}),
        ICO_SAVE            (new int[] {660, 560, 20, 20}),
        ICO_SAVEAS          (new int[] {660, 580, 20, 20}),
        ICO_CLOSE           (new int[] {640, 640, 20, 20}),
        ICO_LIST            (new int[] {640, 660, 20, 20}),
        ICO_CARD_IMAGE           (new int[] {660, 660, 20, 20}),
        ICO_UNKNOWN         (new int[] {0, 720, 80, 80}),
        ICO_LOGO            (new int[] {480, 0, 200, 200}),
        ICO_FLIPCARD        (new int[] {400, 0, 80, 120}),
        ICO_FAVICON         (new int[] {0, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        InterfaceIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum LayoutImages implements SkinProp {
        IMG_HANDLE  (new int[] {320, 450, 80, 20}),
        IMG_CUR_L   (new int[] {564, 724, 32, 32}),
        IMG_CUR_R   (new int[] {564, 764, 32, 32}),
        IMG_CUR_T   (new int[] {604, 724, 32, 32}),
        IMG_CUR_B   (new int[] {604, 764, 32, 32}),
        IMG_CUR_TAB (new int[] {644, 764, 32, 32});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        LayoutImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum EditorImages implements SkinProp {
        IMG_STAR_OUTINE     (new int[] {640, 460, 20, 20}),
        IMG_STAR_FILLED     (new int[] {660, 460, 20, 20}),
        IMG_ARTIFACT        (new int[] {280, 720, 40, 40}),
        IMG_CREATURE        (new int[] {240, 720, 40, 40}),
        IMG_ENCHANTMENT     (new int[] {320, 720, 40, 40}),
        IMG_INSTANT         (new int[] {360, 720, 40, 40}),
        IMG_LAND            (new int[] {120, 720, 40, 40}),
        IMG_MULTI           (new int[] {80, 720, 40, 40}),
        IMG_PLANESWALKER    (new int[] {200, 720, 40, 40}),
        IMG_PACK            (new int[] {80, 760, 40, 40}),
        IMG_SORCERY         (new int[] {160, 720, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        EditorImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum ButtonImages implements SkinProp {
        IMG_BTN_START_UP        (new int[] {480, 200, 160, 80}),
        IMG_BTN_START_OVER      (new int[] {480, 280, 160, 80}),
        IMG_BTN_START_DOWN      (new int[] {480, 360, 160, 80}),

        IMG_BTN_UP_LEFT         (new int[] {80, 0, 40, 40}),
        IMG_BTN_UP_CENTER       (new int[] {120, 0, 1, 40}),
        IMG_BTN_UP_RIGHT        (new int[] {160, 0, 40, 40}),

        IMG_BTN_OVER_LEFT       (new int[] {80, 40, 40, 40}),
        IMG_BTN_OVER_CENTER     (new int[] {120, 40, 1, 40}),
        IMG_BTN_OVER_RIGHT      (new int[] {160, 40, 40, 40}),

        IMG_BTN_DOWN_LEFT       (new int[] {80, 80, 40, 40}),
        IMG_BTN_DOWN_CENTER     (new int[] {120, 80, 1, 40}),
        IMG_BTN_DOWN_RIGHT      (new int[] {160, 80, 40, 40}),

        IMG_BTN_FOCUS_LEFT      (new int[] {80, 120, 40, 40}),
        IMG_BTN_FOCUS_CENTER    (new int[] {120, 120, 1, 40}),
        IMG_BTN_FOCUS_RIGHT     (new int[] {160, 120, 40, 40}),

        IMG_BTN_TOGGLE_LEFT     (new int[] {80, 160, 40, 40}),
        IMG_BTN_TOGGLE_CENTER   (new int[] {120, 160, 1, 40}),
        IMG_BTN_TOGGLE_RIGHT    (new int[] {160, 160, 40, 40}),

        IMG_BTN_DISABLED_LEFT   (new int[] {80, 200, 40, 40}),
        IMG_BTN_DISABLED_CENTER (new int[] {120, 200, 1, 40}),
        IMG_BTN_DISABLED_RIGHT  (new int[] {160, 200, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ButtonImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** Properties of various components that make up the skin.
     * This interface allows all enums to be under the same roof.
     * It also enforces a getter for coordinate locations in sprites. */
    public interface SkinProp {
        /** @return int[] */
        int[] getCoords();
    }

    private static Map<Integer, SkinImage> avatars;
    private static Map<Integer, Font> fixedFonts = new HashMap<Integer, Font>();

    /** @return {@link java.awt.font} */
    public static Font getFixedFont(final int size) {
        Font fixedFont = fixedFonts.get(size);
        if (fixedFont == null) {
            fixedFont = new Font("Monospaced", Font.PLAIN, size);
            fixedFonts.put(size, fixedFont);
        }
        return fixedFont;
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont() {
        return FSkin.getFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont(final int size) {
        return SkinFont.get(Font.PLAIN, size);
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont() {
        return FSkin.getBoldFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont(final int size) {
        return SkinFont.get(Font.BOLD, size);
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont() {
        return FSkin.getItalicFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont(final int size) {
        return SkinFont.get(Font.ITALIC, size);
    }

    public static class SkinFont {
        private static Font baseFont;
        private static Map<String, SkinFont> fonts = new HashMap<String, SkinFont>();

        private static SkinFont get(final int style0, final int size0) {
            String key = style0 + "|" + size0;
            SkinFont skinFont = fonts.get(key);
            if (skinFont == null) {
                skinFont = new SkinFont(style0, size0);
                fonts.put(key, skinFont);
            }
            return skinFont;
        }

        private static void setBaseFont(Font baseFont0) {
            baseFont = baseFont0;

            //update all cached skin fonts
            for (SkinFont skinFont : fonts.values()) {
                skinFont.updateFont();
            }
        }

        private final int style, size;
        private Font font;

        private SkinFont(final int style0, final int size0) {
            this.style = style0;
            this.size = size0;
            this.updateFont();
        }

        public int getSize() {
            return this.font.getSize();
        }

        private void updateFont() {
            this.font = baseFont.deriveFont(this.style, this.size);
        }
    }

    /*private static void addEncodingSymbol(String key, SkinProp skinProp) {
        String path = NewConstants.CACHE_SYMBOLS_DIR + "/" + key.replace("/", "") + ".png";
        getImage(skinProp).save(path, 13, 13);
    }

    public static String encodeSymbols(String str, boolean formatReminderText) {
        String pattern, replacement;

        if (formatReminderText) {
            //format reminder text in italics (or hide if preference set)
            pattern = "\\((.+)\\)";
            replacement = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_HIDE_REMINDER_TEXT) ?
                    "" : "<i>\\($1\\)</i>";
            str = str.replaceAll(pattern, replacement);
        }

        //format mana symbols to display as icons
        pattern = "\\{([A-Z0-9]+)\\}|\\{([A-Z0-9]+)/([A-Z0-9]+)\\}"; //fancy pattern needed so "/" can be omitted from replacement
        try {
            replacement = "<img src='" + Gdx.files.internal(NewConstants.CACHE_SYMBOLS_DIR + "/$1$2$3.png").toURI().toURL().toString() + "'>";
            str = str.replaceAll(pattern, replacement);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return "<html>" + str + "</html>"; //must wrap in <html> tag for images to appear
    }*/

    private static final String
        FILE_SKINS_DIR = "skins/",
        FILE_ICON_SPRITE = "sprite_icons.png",
        FILE_FOIL_SPRITE = "sprite_foils.png",
        FILE_OLD_FOIL_SPRITE = "sprite_old_foils.png",
        FILE_AVATAR_SPRITE = "sprite_avatars.png",
        FILE_FONT = "font1.ttf",
        FILE_SPLASH = "bg_splash.png",
        FILE_MATCH_BG = "bg_match.jpg",
        FILE_TEXTURE_BG = "bg_texture.jpg",
        DEFAULT_DIR = FILE_SKINS_DIR + "default/";

    private static ArrayList<String> allSkins;
    private static int currentSkinIndex;
    private static String preferredDir;
    private static String preferredName;
    private static Pixmap pxDefaultSprite, pxPreferredSprite, pxDefaultAvatars, pxPreferredAvatars;
    private static Texture txDefaultSprite, txPreferredSprite, txFoils, txOldFoils, txDefaultAvatars, txPreferredAvatars;
    private static int defaultFontSize = 12;
    private static boolean loaded = false;

    public static void changeSkin(final String skinName) {
        /*final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();*/

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()
        loadLight(skinName, false);
        loadFull(false);
    }

    /**
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     * 
     * 
     * @param skinName
     *            the skin name
     */
    public static void loadLight(final String skinName, final boolean onInit) {
        if (onInit) {
            if (allSkins == null) { //initialize
                allSkins = new ArrayList<String>();
                ArrayList<String> skinDirectoryNames = getSkinDirectoryNames();
                for (int i = 0; i < skinDirectoryNames.size(); i++) {
                    allSkins.add(/*WordUtils.capitalize(*/skinDirectoryNames.get(i).replace('_', ' '))/*)*/;
                }
                Collections.sort(allSkins);
            }
        }

        currentSkinIndex = allSkins.indexOf(skinName);

        // Non-default (preferred) skin name and dir.
        FSkin.preferredName = skinName.toLowerCase().replace(' ', '_');
        FSkin.preferredDir = FILE_SKINS_DIR + preferredName + "/";

        if (onInit) {
            final FileHandle f = Gdx.files.internal(preferredDir + FILE_SPLASH);
            if (!f.exists()) {
                FSkin.loadLight("default", onInit);
            }
            else {
                final Texture img;
                try {
                    img = new Texture(f);

                    final int h = img.getHeight();
                    final int w = img.getWidth();

                    SkinImage.setImage(Backgrounds.BG_SPLASH, new TextureRegion(img, 0, 0, w, h - 100));
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            loaded = true;
        }
    }

    /**
     * Loads two sprites: the default (which should be a complete
     * collection of all symbols) and the preferred (which may be
     * incomplete).
     * 
     * Font must be present in the skin folder, and will not
     * be replaced by default.  The fonts are pre-derived
     * in this method and saved in a HashMap for future access.
     * 
     * Color swatches must be present in the preferred
     * sprite, and will not be replaced by default.
     * 
     * Background images must be present in skin folder,
     * and will not be replaced by default.
     * 
     * Icons, however, will be pulled from the two sprites. Obviously,
     * preferred takes precedence over default, but if something is
     * missing, the default picture is retrieved.
     */
    public static void loadFull(final boolean onInit) {
        if (onInit) {
            // Preferred skin name must be called via loadLight() method,
            // which does some cleanup and init work.
            if (FSkin.preferredName.isEmpty()) { FSkin.loadLight("default", onInit); }
        }

        //FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Processing image sprites: ", 5);

        // Grab and test various sprite files.
        final FileHandle f1 = Gdx.files.internal(DEFAULT_DIR + FILE_ICON_SPRITE);
        final FileHandle f2 = Gdx.files.internal(preferredDir + FILE_ICON_SPRITE);
        final FileHandle f3 = Gdx.files.internal(DEFAULT_DIR + FILE_FOIL_SPRITE);
        final FileHandle f4 = Gdx.files.internal(DEFAULT_DIR + FILE_AVATAR_SPRITE);
        final FileHandle f5 = Gdx.files.internal(preferredDir + FILE_AVATAR_SPRITE);
        final FileHandle f6 = Gdx.files.internal(DEFAULT_DIR + FILE_OLD_FOIL_SPRITE);

        try {
            pxDefaultSprite = new Pixmap(f1);
            txDefaultSprite = new Texture(f1);
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            pxPreferredSprite = new Pixmap(f2);
            txPreferredSprite = new Texture(f2);
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            txFoils = new Texture(f3);
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            txOldFoils = f6.exists() ? new Texture(f6) : new Texture(f3);
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            pxDefaultAvatars = new Pixmap(f4);
            txDefaultAvatars = new Texture(f4);

            if (f5.exists()) {
                pxDefaultAvatars = new Pixmap(f5);
                txPreferredAvatars = new Texture(f5);
            }

            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Initialize fonts
        /*if (onInit) { //set default font size only once onInit
            Font f = UIManager.getDefaults().getFont("Label.font");
            if (f != null) {
                FSkin.defaultFontSize = f.getSize();
            }
        }
        SkinFont.setBaseFont(GuiUtils.newFont(FILE_SKINS_DIR + preferredName + "/" + FILE_FONT));*/

        // Put various images into map (except sprite and splash).
        // Exceptions handled inside method.
        SkinImage.setImage(Backgrounds.BG_TEXTURE, new TextureRegion(new Texture(preferredDir + FILE_TEXTURE_BG)));
        SkinImage.setImage(Backgrounds.BG_MATCH, new TextureRegion(new Texture(preferredDir + FILE_MATCH_BG)));

        // Run through enums and load their coords.
        Colors.updateAll();
        for (final ZoneImages e : ZoneImages.values())                    { SkinImage.setImage(e); }
        for (final DockIcons e : DockIcons.values())                      { SkinImage.setImage(e); }
        for (final InterfaceIcons e : InterfaceIcons.values())            { SkinImage.setImage(e); }
        for (final ButtonImages e : ButtonImages.values())                { SkinImage.setImage(e); }
        for (final QuestIcons e : QuestIcons.values())                    { SkinImage.setImage(e); }

        for (final EditorImages e : EditorImages.values())                { SkinImage.setImage(e); }
        for (final ManaImages e : ManaImages.values())                    { SkinImage.setImage(e); }
        for (final ColorlessManaImages e : ColorlessManaImages.values())  { SkinImage.setImage(e); }
        for (final GameplayImages e : GameplayImages.values())            { SkinImage.setImage(e); }
        for (final LayoutImages e : LayoutImages.values())                { SkinImage.setImage(e); }

        // Foils have a separate sprite, so uses a specific method.
        for (final Foils e : Foils.values()) { FSkin.setFoil(e, false); }
        for (final OldFoils e : OldFoils.values()) { FSkin.setFoil(e, true); }

        // Assemble avatar images
        FSkin.assembleAvatars();

        // Images loaded; can start UI init.
        //FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Creating display components.");
        loaded = true;

        // Clear references to pixmap
        pxDefaultSprite.dispose();
        pxDefaultSprite = null;
        pxPreferredSprite.dispose();
        pxPreferredSprite = null;
        pxDefaultAvatars.dispose();
        pxDefaultAvatars = null;
        if (pxPreferredAvatars != null) {
            pxPreferredAvatars.dispose();
            pxPreferredAvatars = null;
        }

        //establish encoding symbols
        /*FileHandle dir = Gdx.files.internal(NewConstants.CACHE_SYMBOLS_DIR);
        if (!dir.mkdir()) { //ensure symbols directory exists and is empty
            for (FileHandle file : dir.listFileHandles()) {
                file.delete();
            }
        }

        addEncodingSymbol("W", ManaImages.IMG_WHITE);
        addEncodingSymbol("U", ManaImages.IMG_BLUE);
        addEncodingSymbol("B", ManaImages.IMG_BLACK);
        addEncodingSymbol("R", ManaImages.IMG_RED);
        addEncodingSymbol("G", ManaImages.IMG_GREEN);
        addEncodingSymbol("W/U", ManaImages.IMG_WHITE_BLUE);
        addEncodingSymbol("U/B", ManaImages.IMG_BLUE_BLACK);
        addEncodingSymbol("B/R", ManaImages.IMG_BLACK_RED);
        addEncodingSymbol("R/G", ManaImages.IMG_RED_GREEN);
        addEncodingSymbol("G/W", ManaImages.IMG_GREEN_WHITE);
        addEncodingSymbol("W/B", ManaImages.IMG_WHITE_BLACK);
        addEncodingSymbol("U/R", ManaImages.IMG_BLUE_RED);
        addEncodingSymbol("B/G", ManaImages.IMG_BLACK_GREEN);
        addEncodingSymbol("R/W", ManaImages.IMG_RED_WHITE);
        addEncodingSymbol("G/U", ManaImages.IMG_GREEN_BLUE);
        addEncodingSymbol("2/W", ManaImages.IMG_2W);
        addEncodingSymbol("2/U", ManaImages.IMG_2U);
        addEncodingSymbol("2/B", ManaImages.IMG_2B);
        addEncodingSymbol("2/R", ManaImages.IMG_2R);
        addEncodingSymbol("2/G", ManaImages.IMG_2G);
        addEncodingSymbol("W/P", ManaImages.IMG_PHRYX_WHITE);
        addEncodingSymbol("U/P", ManaImages.IMG_PHRYX_BLUE);
        addEncodingSymbol("B/P", ManaImages.IMG_PHRYX_BLACK);
        addEncodingSymbol("R/P", ManaImages.IMG_PHRYX_RED);
        addEncodingSymbol("G/P", ManaImages.IMG_PHRYX_GREEN);
        for (int i = 0; i <= 20; i++) {
            addEncodingSymbol(String.valueOf(i), ColorlessManaImages.valueOf("IMG_" + i));
        }
        addEncodingSymbol("X", ColorlessManaImages.IMG_X);
        addEncodingSymbol("Y", ColorlessManaImages.IMG_Y);
        addEncodingSymbol("Z", ColorlessManaImages.IMG_Z);
        addEncodingSymbol("C", GameplayImages.IMG_CHAOS);
        addEncodingSymbol("Q", GameplayImages.IMG_UNTAP);
        addEncodingSymbol("S", GameplayImages.IMG_SNOW);
        addEncodingSymbol("T", GameplayImages.IMG_TAP);

        // Set look and feel after skin loaded
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Setting look and feel...");
        ForgeLookAndFeel laf = new ForgeLookAndFeel();
        laf.setForgeLookAndFeel(Singletons.getView().getFrame());*/
    }

    /**
     * Gets the name.
     * 
     * @return Name of the current skin.
     */
    public static String getName() {
        return FSkin.preferredName;
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkinDirectoryNames() {
        final ArrayList<String> mySkins = new ArrayList<String>();

        final FileHandle dir;
        if (Gdx.app.getType() == ApplicationType.Desktop) {
            dir = Gdx.files.internal("./bin/" + FILE_SKINS_DIR); //needed to iterate over directory for Desktop
        }
        else {
            dir = Gdx.files.internal(FILE_SKINS_DIR);
        }
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("FSkin > can't find skins directory!");
        }
        else {
            for (FileHandle skinFile : dir.list()) {
                String skinName = skinFile.name();
                if (skinName.equalsIgnoreCase(".svn")) { continue; }
                if (skinName.equalsIgnoreCase(".DS_Store")) { continue; }
                mySkins.add(skinName);
            }
        }

        return mySkins;
    }

    public static Iterable<String> getAllSkins() {
        return allSkins;
    }

    /** @return Map<Integer, Image> */
    public static Map<Integer, SkinImage> getAvatars() {
        return avatars;
    }

    public static boolean isLoaded() { return loaded; }

    /**
     * <p>
     * getColorFromPixel.
     * </p>
     * 
     * @param {@link java.lang.Integer} pixel information
     */
    private static Color getColorFromPixel(final int pixel) {
        int r, g, b, a;
        a = (pixel >> 24) & 0x000000ff;
        r = (pixel >> 16) & 0x000000ff;
        g = (pixel >> 8) & 0x000000ff;
        b = (pixel) & 0x000000ff;
        return new Color(r, g, b, a);
    }

    private static void assembleAvatars() {
        FSkin.avatars = new HashMap<Integer, SkinImage>();
        int counter = 0;
        Color pxTest;

        if (pxPreferredAvatars != null) {
            final int pw = pxPreferredAvatars.getWidth();
            final int ph = pxPreferredAvatars.getHeight();

            for (int j = 0; j < ph; j += 100) {
                for (int i = 0; i < pw; i += 100) {
                    if (i == 0 && j == 0) { continue; }
                    pxTest = FSkin.getColorFromPixel(pxPreferredAvatars.getPixel(i + 50, j + 50));
                    if (pxTest.getAlpha() == 0) { continue; }
                    FSkin.avatars.put(counter++, new SkinImage(new TextureRegion(txPreferredAvatars, i, j, 100, 100)));
                }
            }
        }

        final int aw = pxDefaultAvatars.getWidth();
        final int ah = pxDefaultAvatars.getHeight();

        for (int j = 0; j < ah; j += 100) {
            for (int i = 0; i < aw; i += 100) {
                if (i == 0 && j == 0) { continue; }
                pxTest = FSkin.getColorFromPixel(pxDefaultAvatars.getPixel(i + 50, j + 50));
                if (pxTest.getAlpha() == 0) { continue; }
                FSkin.avatars.put(counter++, new SkinImage(new TextureRegion(txDefaultAvatars, i, j, 100, 100)));
            }
        }
    }

    private static void setFoil(final SkinProp s0, boolean isOldStyle) {
        int[] tempCoords = s0.getCoords();
        int x0 = tempCoords[0];
        int y0 = tempCoords[1];
        int w0 = tempCoords[2];
        int h0 = tempCoords[3];

        SkinImage.setImage(s0, isOldStyle ? new TextureRegion(txOldFoils, x0, y0, w0, h0) : new TextureRegion(txFoils, x0, y0, w0, h0));
    }
}
