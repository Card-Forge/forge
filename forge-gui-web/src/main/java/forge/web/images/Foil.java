package forge.web.images;

import forge.ImageKeys;
import forge.card.CardDb;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.item.PaperCard;

import java.awt.image.BufferedImage;

/**
 * Finds which pictures are foil, from Forge's foil mark on the name in the image key. It also paints the foil on them.
 * The page shows cards as plain images, and one WebGL context per card is too costly, so the foil is painted here.
 * The page's foil.ts runs the same shader on the hovered preview only, where the streaks follow the pointer.
 */
public final class Foil {
    // The look, with the names fragCardShaderHolo uses in forge-gui-mobile's Shaders. This code follows that shader line for line
    private static final double FOIL_STRENGTH = 0.85, INK_FLOOR = 0.02, VIVID = 0.35, STREAK_DENSITY = 1.15, STREAK_WIDTH = 0.27,
            ARCH = -2.9, KINK = 0.95, SERRATION = 0.04, HAIR = 0.75, WIDTH_VAR = 2.1, GLOW = 0.25, WHITE_MIX = 0.35,
            HUE_TOP = 0.55, HUE_SPAN = 0.95;
    private static final double[] HUE_OF_CHANNEL = {0, 0.6667, 0.3333}, LUMA = {0.299, 0.587, 0.114};

    private Foil() {
    }

    public static String key(final PaperCard card) {
        return key(card.getImageKey(false), card.isFoil());
    }

    /** A card in a game is foil when its paper card is, or when the match made it one at random. */
    public static String key(final CardView card, final CardStateView state) {
        return key(state.getImageKey(), card.hasPaperFoil() || card.getCurrentState().getFoilIndex() > 0);
    }

    private static String key(final String imageKey, final boolean foil) {
        if (!foil || imageKey == null || !imageKey.startsWith(ImageKeys.CARD_PREFIX)) {
            return imageKey;
        }
        final int end = nameEnd(imageKey);
        return imageKey.substring(0, end) + CardDb.foilSuffix + imageKey.substring(end);
    }

    private static int nameEnd(final String imageKey) {
        final int separator = imageKey.indexOf(CardDb.NameSetSeparator);
        if (separator >= 0) {
            return separator;
        }
        return imageKey.length() - (imageKey.endsWith(ImageKeys.BACKFACE_POSTFIX) ? ImageKeys.BACKFACE_POSTFIX.length() : 0);
    }

    /** Which streak pattern a foil's picture has, from 1, or 0 for a key that is not foil. The browser computes the same number for the preview it draws itself. */
    public static int seed(final String imageKey) {
        if (!imageKey.startsWith(ImageKeys.CARD_PREFIX) || !CardDb.CardRequest.isFoilCardName(imageKey.substring(0, nameEnd(imageKey)))) {
            return 0;
        }
        return 1 + imageKey.chars().sum() % 50;
    }

    /** Screens bright streaks in the colours of the spectrum onto the picture. Dark ink takes almost none, so text stays readable. */
    static void paint(final BufferedImage image, final int seed) {
        final int w = image.getWidth(), h = image.getHeight();
        final double seedA = fract(seed * 0.61803399), seedB = fract(seed * 0.75487767 + 0.31);
        final int[] row = new int[w];
        final double[] col = new double[3], rainbow = new double[3];
        for (int y = 0; y < h; y++) {
            final double v = (y + 0.5) / h;
            final double zone = 0.18 + 0.82 * (1 - smoothstep(0.25, 0.90, v));
            image.getRGB(0, y, w, 1, row, 0, w);
            for (int x = 0; x < w; x++) {
                final double qx = ((x + 0.5) / w - 0.5) * w / h, qy = v - 0.5;
                final double s0 = (qx * 0.796 + qy * 0.605) * STREAK_DENSITY + seed * 1.2;
                final double t = qx * 0.605 - qy * 0.796;
                final double low = zone * (0.30 + 0.70 * tri(s0 * 0.09 + seedB * 4 + t)) * ARCH
                        * (0.62 * wave(t * 2 + s0 * 0.11 + seedB * 3) + 0.38 * wave(t * 3 - s0 * 0.17 + seedA * 5))
                        + KINK * (smoothstep(0.35, 0.65, tri(t * 2 + s0 * 0.07 + seedA * 2)) - 0.5);
                final double teeth = (fract(t * 23 + s0 * 0.41 + seedB * 7) - 0.5) * (0.55 + 0.45 * wave(t * 5 + s0 * 0.29));
                final double sd = s0 + seedA + low + SERRATION * (0.35 + 0.65 * tri(s0 * 0.37 + seedA)) * teeth * 2;
                final double streak = Math.floor(sd), across = sd - streak;
                final double r = tri(streak * 0.75487767 + seedB);
                final double dist = Math.abs(across - 0.5);
                final double halfWidth = STREAK_WIDTH * 0.5 * (1 - WIDTH_VAR * 0.5 + WIDTH_VAR * tri(streak * 0.56984029 + seedA));
                final double amp = smoothstep(0.08, 0.40, r) * (0.55 + 0.45 * tri(t * 2 + r * 5));
                final double sh = s0 * 2.31 + seedB * 5 + low * 2.31 + (fract(t * 31 + s0 * 0.7) - 0.5) * 0.10;
                final double rh = tri(Math.floor(sh) * 0.6180339 + seedA);
                final double hair = (1 - smoothstep(0.03, 0.13, Math.abs(fract(sh) - 0.5))) * smoothstep(0.50, 0.90, rh)
                        * (0.45 + 0.55 * tri(t * 3 + rh * 5));
                final double cover = (1 - smoothstep(halfWidth - 0.07, halfWidth + 0.05, dist)) * amp
                        + (1 - smoothstep(0, 0.5, dist)) * amp * GLOW + hair * HAIR;
                final double hue = HUE_TOP - HUE_SPAN * v + seed * 0.55 + (across - 0.5) * 0.24;

                double lum = 0, grey = 0;
                for (int i = 0; i < 3; i++) {
                    col[i] = clamp((row[x] >> 16 - 8 * i & 0xff) / 255.0 * 1.25 - 0.12);
                    rainbow[i] = mix(clamp(Math.abs(fract(hue + HUE_OF_CHANNEL[i]) * 6 - 3) - 1), 1, WHITE_MIX);
                    lum += col[i] * LUMA[i];
                    grey += rainbow[i] * cover / 3;
                }
                final double colourful = smoothstep(0.06, 0.30, lum);
                final double strength = FOIL_STRENGTH * (INK_FLOOR + (1 - INK_FLOOR) * smoothstep(0.03, 0.34, lum));
                int out = row[x] & 0xff000000;
                for (int i = 0; i < 3; i++) {
                    final double screened = 1 - (1 - col[i]) * (1 - clamp(mix(grey, rainbow[i] * cover, colourful)));
                    final double lit = mix(screened, rainbow[i], clamp(cover) * VIVID * colourful);
                    out |= (int) Math.round(clamp(mix(col[i], lit, strength)) * 255) << 16 - 8 * i;
                }
                row[x] = out;
            }
            image.setRGB(0, y, w, 1, row, 0, w);
        }
    }

    private static double fract(final double x) {
        return x - Math.floor(x);
    }

    private static double tri(final double x) {
        return Math.abs(fract(x) - 0.5) * 2;
    }

    private static double wave(final double x) {
        return (tri(x) - 0.5) * 2;
    }

    private static double clamp(final double x) {
        return Math.max(0, Math.min(1, x));
    }

    private static double mix(final double a, final double b, final double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(final double from, final double to, final double x) {
        final double t = clamp((x - from) / (to - from));
        return t * t * (3 - 2 * t);
    }
}
