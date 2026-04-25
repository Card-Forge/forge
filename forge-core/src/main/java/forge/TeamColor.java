package forge;

/**
 * Represents the display color associated with a team.
 * Stored as a String (name()) in serializable objects across the network.
 */
public enum TeamColor {
    NONE("None",   "#808080"),
    RED("Red",     "#FF4444"),
    BLUE("Blue",   "#4444FF"),
    GREEN("Green", "#44BB44"),
    WHITE("White", "#EEEEEE"),
    BLACK("Black", "#222222"),
    ORANGE("Orange","#FF8800"),
    PURPLE("Purple","#AA44AA"),
    YELLOW("Yellow","#DDDD00"),
    CYAN("Cyan",   "#00AACC");

    private final String displayName;
    private final String hexColor;

    TeamColor(final String displayName, final String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    /** Human-readable label used in UI combo boxes. */
    public String getDisplayName() {
        return displayName;
    }

    /** CSS-style hex color string, e.g. {@code "#FF4444"}. */
    public String getHexColor() {
        return hexColor;
    }

    /**
     * Safe parse: returns {@code NONE} instead of throwing when the name is
     * unrecognized or {@code null} (useful for deserialization of old data).
     */
    public static TeamColor fromName(final String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        try {
            return TeamColor.valueOf(name);
        } catch (final IllegalArgumentException e) {
            return NONE;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}

