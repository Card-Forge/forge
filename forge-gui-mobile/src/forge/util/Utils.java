package forge.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Utils {
    public static final float BASE_WIDTH = 320f;
    public static final float BASE_HEIGHT = 480f;
    private static final float SCREEN_WIDTH = (float)Gdx.graphics.getWidth();
    private static final float SCREEN_HEIGHT = (float)Gdx.graphics.getHeight();
    private static final float HEIGHT_RATIO = SCREEN_HEIGHT / BASE_HEIGHT;

    private static final float AVG_FINGER_SIZE_CM = 1.1f;

    //Swap commented out line below to specify average finger size
    private static final float ppcX = Gdx.graphics.getPpcX(), ppcY = Gdx.graphics.getPpcY();
    //private static final float ppcX = 169f / AVG_FINGER_SIZE_CM, ppcY = 237f / AVG_FINGER_SIZE_CM;
    //private static final float scaleX = 1.41f, scaleY = 1.25f;
    //private static final float ppcX = Gdx.graphics.getPpcX() * scaleX, ppcY = Gdx.graphics.getPpcY() * scaleY;

    //round to nearest int to reduce floating point display issues
    //reduce if either would take up too large a percentage of the screen to prevent layouts not working
    private static final float MIN_FINGER_SIZE = scale(40); //scaled value of 40 is approximately how tall the Prompt buttons would need to be to fit their text
    private static final float MIN_FINGERS_WIDE = 5; //ensure screen considered to be at least 5 "fingers" wide
    private static final float MIN_FINGERS_TALL = MIN_FINGERS_WIDE * BASE_HEIGHT / BASE_WIDTH; //ensure screen tall enough based on fingers wide and base ratio

    public static final float AVG_FINGER_WIDTH = Math.round(Math.min(Math.max(cmToPixelsX(AVG_FINGER_SIZE_CM), MIN_FINGER_SIZE), SCREEN_WIDTH / MIN_FINGERS_WIDE));
    public static final float AVG_FINGER_HEIGHT = Math.round(Math.min(Math.max(cmToPixelsY(AVG_FINGER_SIZE_CM), MIN_FINGER_SIZE), SCREEN_HEIGHT / MIN_FINGERS_TALL));

    public static float cmToPixelsX(float cm) {
        return ppcX * cm;
    }
    public static float cmToPixelsY(float cm) {
        return ppcY * cm;
    }

    public static float scale(float value) {
        //use height ratio to prioritize making fonts look good
        //fonts can always auto-scale down if container not wide enough
        return Math.round(value * HEIGHT_RATIO);
    }

    public static long secondsToTimeSpan(float seconds) {
        return (long)(seconds * 1000000000l);
    }

    public static Vector2 getIntersection(Vector2 l1p1, Vector2 l1p2, Vector2 l2p1, Vector2 l2p2, Vector2 outVector) {
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
                outVector.x = l1p1.x + (ua * (l1p2.x - l1p1.x));
                outVector.y = l1p1.y + (ua * (l1p2.y - l1p1.y));
                return outVector;
            }
        }
        //if lines are parallel or don't intersect, just return the midpoint of first line
        return getMidpoint(l1p1, l1p2, outVector);
    }

    public static Vector2 getIntersection(Vector2 l1p1, Vector2 l1p2, Vector2 l2p1, Vector2 l2p2) {
        return getIntersection(l1p1, l1p2, l2p1, l2p2, new Vector2());
    }

    public static Vector2 getMidpoint(Vector2 p1, Vector2 p2, Vector2 outVector) {
        outVector.x = (p1.x + p2.x) / 2;
        outVector.y = (p1.y + p2.y) / 2;
        return outVector;
    }

    public static Vector2 getMidpoint(Vector2 p1, Vector2 p2) {
        return getMidpoint(p1, p2, new Vector2());
    }

    public static Vector2 getArbitaryPoint(float x1, float y1, float x2, float y2, float percentage, Vector2 outVector) {
        outVector.x = x1 * (1 - percentage) + x2 * percentage;
        outVector.y = y1 * (1 - percentage) + y2 * percentage;
        return outVector;
    }

    public static Vector2 getArbitaryPoint(float x1, float y1, float x2, float y2, float percentage) {
        return getArbitaryPoint(x1, y1, x2, y2, percentage, new Vector2());
    }

    public static Rectangle getTransitionPosition(Rectangle start, Rectangle end, float percentage, Vector2 scratchV1, Vector2 scratchV2, Rectangle outRect) {
        getArbitaryPoint(start.x, start.y, end.x, end.y, percentage, scratchV1);
        getArbitaryPoint(start.x + start.width, start.y + start.height, end.x + end.width, end.y + end.height, percentage, scratchV2);
        outRect.set(scratchV1.x, scratchV1.y, scratchV2.x - scratchV1.x, scratchV2.y - scratchV1.y);
        return outRect;
    }

    public static Rectangle getTransitionPosition(Rectangle start, Rectangle end, float percentage) {
        return getTransitionPosition(start, end, percentage, new Vector2(), new Vector2(), new Rectangle());
    }

    public static Rectangle getIntersection(Rectangle r1, Rectangle r2, Rectangle outRect) {
        if (r1 == null || r2 == null || outRect == null) return null;

        float left = Math.max(r1.x, r2.x);
        float right = Math.min(r1.x + r1.width, r2.x + r2.width);
        if (right > left) {
            float top = Math.max(r1.y, r2.y);
            float bottom = Math.min(r1.y + r1.height, r2.y + r2.height);
            if (bottom > top) {
                outRect.set(left, top, right - left, bottom - top);
                return outRect;
            }
        }
        return null;
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
