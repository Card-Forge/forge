package forge.util;

import com.badlogic.gdx.Gdx;

public class Utils {
    private static final float ppcX = Gdx.graphics.getPpcX();
    private static final float ppcY = Gdx.graphics.getPpcY();
    private static final float AVG_FINGER_SIZE_CM = 1.1f;
    public static final float AVG_FINGER_WIDTH = Math.round(cmToPixelsX(AVG_FINGER_SIZE_CM)); //round to nearest int to reduce floating point display issues
    public static final float AVG_FINGER_HEIGHT = Math.round(cmToPixelsY(AVG_FINGER_SIZE_CM));

    public static final float BASE_WIDTH = 320f;
    public static final float BASE_HEIGHT = 480f;
    public static final float WIDTH_RATIO = ((float)Gdx.graphics.getWidth() / BASE_WIDTH);
    public static final float HEIGHT_RATIO = ((float)Gdx.graphics.getHeight() / BASE_HEIGHT);
    public static final float MIN_RATIO = Math.min(WIDTH_RATIO, HEIGHT_RATIO);
    public static final float MAX_RATIO = Math.max(WIDTH_RATIO, HEIGHT_RATIO);

    public static float cmToPixelsX(float cm) {
        return ppcX * cm;
    }
    public static float cmToPixelsY(float cm) {
        return ppcY * cm;
    }

    public static float scaleX(float value) {
        return Math.round(value * WIDTH_RATIO);
    }

    public static float scaleY(float value) {
        return Math.round(value * HEIGHT_RATIO);
    }

    public static float scaleMin(float value) {
        return Math.round(value * MIN_RATIO);
    }

    public static float scaleMax(float value) {
        return Math.round(value * MAX_RATIO);
    }

    public static long secondsToTimeSpan(float seconds) {
        return (long)(seconds * 1000000000l);
    }
}
