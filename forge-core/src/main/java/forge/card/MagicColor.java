package forge.card;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Holds byte values for each color magic has.
 */
public final class MagicColor {

    public static final byte COLORLESS = 0;
    public static final byte WHITE     = 1 << 0;
    public static final byte BLUE      = 1 << 1;
    public static final byte BLACK     = 1 << 2;
    public static final byte RED       = 1 << 3;
    public static final byte GREEN     = 1 << 4;

    public static final int NUMBER_OR_COLORS = 5;

    public static final byte[] WUBRG  = new byte[] { WHITE, BLUE, BLACK, RED, GREEN };
    public static final byte[] WUBRGC = new byte[] { WHITE, BLUE, BLACK, RED, GREEN, COLORLESS };

    /**
     * Private constructor to prevent instantiation.
     */
    private MagicColor() {
    }

    public static byte fromName(String s) {
        if (s == null) {
            return 0;
        }
        if (s.length() == 2) { //if name is two characters, check for combination of two colors
            return (byte)(fromName(s.charAt(0)) | fromName(s.charAt(1)));
        }
        s = s.toLowerCase();
        if (s.length() == 1) {
            switch (s) {
                case "w": return MagicColor.WHITE;
                case "u": return MagicColor.BLUE;
                case "b": return MagicColor.BLACK;
                case "r": return MagicColor.RED;
                case "g": return MagicColor.GREEN;
            }
        } else {
            switch (s) {
                case Constant.WHITE: return MagicColor.WHITE;
                case Constant.BLUE: return MagicColor.BLUE;
                case Constant.BLACK: return MagicColor.BLACK;
                case Constant.RED: return MagicColor.RED;
                case Constant.GREEN: return MagicColor.GREEN;
            }
        }
        return 0; // colorless
    }

    public static byte fromName(final char c) {
        switch (Character.toLowerCase(c)) {
            case 'w': return MagicColor.WHITE;
            case 'u': return MagicColor.BLUE;
            case 'b': return MagicColor.BLACK;
            case 'r': return MagicColor.RED;
            case 'g': return MagicColor.GREEN;
        }
        return 0; // unknown means 'colorless'
    }

    public static String toShortString(final String color) {
        if (color.equalsIgnoreCase(Constant.SNOW)) {
            return "S";
        } // compatibility
        return toShortString(fromName(color));
    }

    public static String toShortString(final byte color) {
        switch (color){
            case WHITE: return "W";
            case BLUE:  return "U";
            case BLACK: return "B";
            case RED:   return "R";
            case GREEN: return "G";
            default:    return "1";
        }
    }

    public static String toLongString(final byte color) {
        switch (color){
            case WHITE: return Constant.WHITE;
            case BLUE:  return Constant.BLUE;
            case BLACK: return Constant.BLACK;
            case RED:   return Constant.RED;
            case GREEN: return Constant.GREEN ;
            default:    return Constant.COLORLESS;
        }
    }

    public static int getIndexOfFirstColor(final byte color){
        for (int i = 0; i < NUMBER_OR_COLORS; i++) {
            if ((color & WUBRG[i]) != 0) {
                return i;
            }
        }
        return -1; // colorless
    }

    /**
     * The Interface Color.
     */
    public static final class Constant {
        /** The White. */
        public static final String WHITE = "white";

        /** The Blue. */
        public static final String BLUE = "blue";

        /** The Black. */
        public static final String BLACK = "black";

        /** The Red. */
        public static final String RED = "red";

        /** The Green. */
        public static final String GREEN = "green";

        /** The Colorless. */
        public static final String COLORLESS = "colorless";

        /** The only colors. */
        public static final ImmutableList<String> ONLY_COLORS = ImmutableList.of(WHITE, BLUE, BLACK, RED, GREEN);
        public static final ImmutableList<String> COLORS_AND_COLORLESS = ImmutableList.of(WHITE, BLUE, BLACK, RED, GREEN, COLORLESS);

        /** The Snow. */
        public static final String SNOW = "snow";

        /** The Basic lands. */
        public static final ImmutableList<String> BASIC_LANDS = ImmutableList.of("Plains", "Island", "Swamp", "Mountain", "Forest");
        public static final ImmutableList<String> SNOW_LANDS = ImmutableList.of("Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp", "Snow-Covered Mountain", "Snow-Covered Forest");
        public static final ImmutableMap<String, String> ANY_MANA_CONVERSION = new ImmutableMap.Builder<String, String>()
                .put("ManaColorConversion", "Additive")
                .put("WhiteConversion", "All")
                .put("BlueConversion", "All")
                .put("BlackConversion", "All")
                .put("RedConversion", "All")
                .put("GreenConversion", "All")
                .put("ColorlessConversion", "All")
                .build();

        /**
         * Private constructor to prevent instantiation.
         */
        private Constant() {
        }
    }
}
