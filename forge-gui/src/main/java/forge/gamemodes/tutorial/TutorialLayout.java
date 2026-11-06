package forge.gamemodes.tutorial;

/**
 * Which match screen layout the player is looking at, so the tutorial can say where things are on <em>their</em> screen.
 * The key suffix picks the layout specific line of a tutorial text, for example {@code msgTutGuiZones} +
 * {@code Desktop}.
 */
public enum TutorialLayout {
    /** The desktop GUI: docked panels, a box per phase, zone counters under the avatar. */
    DESKTOP("Desktop"),
    /** Mobile held upright: phase labels in a row, zone tabs below the battlefield. */
    MOBILE_PORTRAIT("Portrait"),
    /** Mobile held sideways with the normal layout: zone tabs in columns beside the battlefield. */
    MOBILE_LANDSCAPE("Landscape"),
    /** Mobile compact landscape layout: a control column with a phase button per player. */
    MOBILE_COMPACT("Compact"),
    /** Mobile compact landscape layout with the single phase label that opens a list of every player's stops. */
    MOBILE_COMPACT_UNIFIED("CompactUnified");

    private final String suffix;

    TutorialLayout(final String suffix0) {
        suffix = suffix0;
    }

    /** Appended to a language key to get the text for this layout. */
    public String suffix() {
        return suffix;
    }

    public boolean isMobile() {
        return this != DESKTOP;
    }

    public boolean isCompact() {
        return this == MOBILE_COMPACT || this == MOBILE_COMPACT_UNIFIED;
    }
}
