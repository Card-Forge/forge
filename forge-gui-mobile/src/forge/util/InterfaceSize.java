package forge.util;

/**
 * How large text, buttons and lists are drawn ({@code FPref.UI_INTERFACE_SIZE}, desktop only).
 * <p>
 * Forge's interface is designed for a phone: {@link Utils#scale} blows a 480 point tall layout up to the height of the
 * window. On a big monitor that makes every label and button several times the size a desktop program uses. The
 * setting scales that down: at 75% a 1440 px window is laid out like a 1080 px one (640 points tall instead of 480), so
 * the same screens show more and draw everything smaller. Layouts that size themselves from the window (the Arena match
 * board) are not affected.
 * <p>
 * Read once at start-up, before {@link Utils} is first used; changing it takes a restart.
 */
public final class InterfaceSize {
    public static final String AUTO = "Auto";
    public static final String[] CHOICES = { AUTO, "50%", "60%", "70%", "80%", "90%", "100%" };
    /** Auto: windows up to this tall get the full size; taller ones draw at the size this one would. */
    private static final float AUTO_FULL_HEIGHT = 1080f;

    private static String setting = "100%"; //phones and tablets, and anything that never sets it: the original size

    private InterfaceSize() {
    }

    /** Sets the size the interface is drawn at (desktop launcher, before the window opens). */
    public static void set(String value) {
        setting = value == null || value.isEmpty() ? AUTO : value;
    }

    /** The setting as chosen ("Auto" or a percentage). */
    public static String get() {
        return setting;
    }

    /** The factor {@link Utils} applies to its height-based scale, for a window this tall at start-up. */
    public static float factor(float windowHeight) {
        if (AUTO.equalsIgnoreCase(setting)) {
            return windowHeight > AUTO_FULL_HEIGHT ? AUTO_FULL_HEIGHT / windowHeight : 1f;
        }
        try {
            float percent = Float.parseFloat(setting.replace("%", "").trim());
            return Math.max(0.4f, Math.min(percent / 100f, 1.5f));
        } catch (NumberFormatException e) {
            return 1f;
        }
    }

    /** "Auto (75%)" for the Auto setting at this window height, the percentage otherwise. */
    public static String describe(String value, float windowHeight) {
        if (AUTO.equalsIgnoreCase(value)) {
            float f = windowHeight > AUTO_FULL_HEIGHT ? AUTO_FULL_HEIGHT / windowHeight : 1f;
            return AUTO + " (" + Math.round(f * 100) + "%)";
        }
        return value;
    }
}
