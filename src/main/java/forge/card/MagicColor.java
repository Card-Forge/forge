package forge.card;

import forge.Constant;

/** 
 * Holds byte values for each color magic has
 *
 */
public class MagicColor {

    public static final byte BLACK = 1 << 3;
    public static final byte BLUE = 1 << 2;
    public static final byte GREEN = 1 << 5;
    public static final byte RED = 1 << 4;
    public static final byte WHITE = 1 << 1;

    public static final byte ALL_COLORS = BLACK | BLUE | WHITE | RED | GREEN;
    public static final int NUMBER_OR_COLORS = 5;
    

    public static byte fromName(String s) {
        if (s.equalsIgnoreCase(Constant.Color.WHITE) || s.equalsIgnoreCase("w")) {
            return MagicColor.WHITE;
        }
        if (s.equalsIgnoreCase(Constant.Color.BLUE) || s.equalsIgnoreCase("u")) {
            return MagicColor.BLUE;
        }
        if (s.equalsIgnoreCase(Constant.Color.BLACK) || s.equalsIgnoreCase("b")) {
            return MagicColor.BLACK;
        }
        if (s.equalsIgnoreCase(Constant.Color.RED) || s.equalsIgnoreCase("r")) {
            return MagicColor.RED;
        }
        if (s.equalsIgnoreCase(Constant.Color.GREEN) || s.equalsIgnoreCase("g")) {
            return MagicColor.GREEN;
        }
        return 0; // colorless
    }

    public static String toShortString(byte color) {
        if ( color == GREEN ) return "G";
        if ( color == RED ) return "R";
        if ( color == BLUE ) return "U";
        if ( color == BLACK ) return "B";
        if ( color == WHITE ) return "W";
        return "1";
    }

    public static String toLongString(byte color) {
        if ( color == GREEN ) return Constant.Color.GREEN ;
        if ( color == RED ) return Constant.Color.RED;
        if ( color == BLUE ) return Constant.Color.BLUE;
        if ( color == BLACK ) return Constant.Color.BLACK;
        if ( color == WHITE ) return Constant.Color.WHITE;
        return Constant.Color.COLORLESS;
    }
}
