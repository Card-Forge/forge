package forge.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

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

    public static Vector2 getIntersection(Vector2 l1p1, Vector2 l1p2, Vector2 l2p1, Vector2 l2p2) {
        Vector2 result = new Vector2();

        // Denominator for ua and ub are the same, so store this calculation
        float d = (l2p2.y - l2p1.y) * (l1p2.x - l1p1.x) - (l2p2.x - l2p1.x) * (l1p2.y - l1p1.y);

        //n_a and n_b are calculated as separate values for readability
        float n_a = (l2p2.x - l2p1.x) * (l1p1.y - l2p1.y) - (l2p2.y - l2p1.y) * (l1p1.x - l2p1.x);
        float n_b = (l1p2.x - l1p1.x) * (l1p1.y - l2p1.y) - (l1p2.y - l1p1.y) * (l1p1.x - l2p1.x);

        // Make sure there is not a division by zero - this also indicates that
        // the lines are parallel.  
        // If n_a and n_b were both equal to zero the lines would be on top of each 
        // other (coincidental).  This check is not done because it is not 
        // necessary for this implementation (the parallel check accounts for this).
        if (d != 0) {
            // Calculate the intermediate fractional point that the lines potentially intersect.
            float ua = n_a / d;
            float ub = n_b / d;

            // The fractional point will be between 0 and 1 inclusive if the lines
            // intersect.  If the fractional calculation is larger than 1 or smaller
            // than 0 the lines would need to be longer to intersect.
            if (ua >= 0d && ua <= 1d && ub >= 0d && ub <= 1d) {
                result.x = l1p1.x + (ua * (l1p2.x - l1p1.x));
                result.y = l1p1.y + (ua * (l1p2.y - l1p1.y));
                return result;
            }
        }

        //if lines are parallel or don't intersect, just return the midpoint of first line
        return getMidpoint(l1p1, l1p2);
    }

    public static Vector2 getMidpoint(Vector2 p1, Vector2 p2) {
        Vector2 result = new Vector2();
        result.x = (p1.x + p2.x) / 2;
        result.y = (p1.y + p2.y) / 2;
        return result;
    }

    //get rectangle defining the interestion between two other rectangles
    public static Rectangle getIntersection(Rectangle r1, Rectangle r2) {
        float left = Math.max(r1.x, r2.x);
        float right = Math.min(r1.x + r1.width, r2.x + r2.width);
        if (right > left) {
            float top = Math.max(r1.y, r2.y);
            float bottom = Math.min(r1.y + r1.height, r2.y + r2.height);
            if (bottom > top) {
                return new Rectangle(left, top, right - left, bottom - top);
            }
        }
        return null;
    }
}
