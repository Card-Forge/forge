package forge.util;

import com.badlogic.gdx.Gdx;

public class Utils {
    private final static float ppcX = Gdx.graphics.getPpcX();
    private final static float ppcY = Gdx.graphics.getPpcY();
    private final static float AVG_FINGER_SIZE_CM = 1.1f;

    public final static float AVG_FINGER_WIDTH = Math.round(cmToPixelsX(AVG_FINGER_SIZE_CM)); //round to nearest int to reduce floating point display issues
    public final static float AVG_FINGER_HEIGHT = Math.round(cmToPixelsY(AVG_FINGER_SIZE_CM));

    public static float cmToPixelsX(float cm) {
        return ppcX * cm;
    }
    public static float cmToPixelsY(float cm) {
        return ppcY * cm;
    }

    public static long secondsToTimeSpan(float seconds) {
        return (long)(seconds * 1000000000l);
    }
}
