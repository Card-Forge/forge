package forge.card;

import forge.Constant;

/** 
 * Holds byte values for each color magic has.
 *
 */
public class MagicColor {

    public static final byte WHITE = 1 << 1;
    public static final byte BLUE = 1 << 2;
    public static final byte BLACK = 1 << 3;
    public static final byte RED = 1 << 4;
    public static final byte GREEN = 1 << 5;

    public static final byte ALL_COLORS = BLACK | BLUE | WHITE | RED | GREEN;
    public static final int NUMBER_OR_COLORS = 5;

    public static final byte[] WUBRG = new byte[] { WHITE, BLUE, BLACK, RED, GREEN }; 
    
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

    public static String toShortString(String color) {
        if (color.equalsIgnoreCase(Constant.Color.SNOW)) return "S"; // compatibility
        return toShortString(fromName(color));
    }
    
    public static String toLongString(String color) {
        if (color.equalsIgnoreCase("s")) return Constant.Color.SNOW; // compatibility
        return toLongString(fromName(color));
    }
        
    public static String toShortString(byte color) {
        switch(color){
            case GREEN: return "G";
            case RED: return "R";
            case BLUE: return "U";
            case BLACK: return "B";
            case WHITE: return "W";
            default: return "1";
        }
    }

    public static String toLongString(byte color) {
        switch(color){
            case GREEN: return Constant.Color.GREEN ;
            case RED: return Constant.Color.RED;
            case BLUE: return Constant.Color.BLUE;
            case BLACK: return Constant.Color.BLACK;
            case WHITE: return Constant.Color.WHITE;
            default: return Constant.Color.COLORLESS;
        }
    }
}
