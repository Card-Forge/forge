package forge.game;

/**
 * GameType is an enum to determine the type of current game. :)
 */
public enum GameType {

    /** The Constructed. */
    Constructed(false),
 /** The Sealed. */
 Sealed(true),
 /** The Draft. */
 Draft(true),
 /** The Commander. */
 Commander(false),
 /** The Quest. */
 Quest(true);

    private final boolean bLimited;

    /**
     * Checks if is limited.
     *
     * @return true, if is limited
     */
    public final boolean isLimited() {
        return this.bLimited;
    }

    /**
     * Instantiates a new game type.
     *
     * @param isLimited the is limited
     */
    GameType(final boolean isLimited) {
        this.bLimited = isLimited;
    }

    /**
     * Smart value of.
     *
     * @param value the value
     * @return the game type
     */
    public static GameType smartValueOf(final String value) {
        final String valToCompate = value.trim();
        for (final GameType v : GameType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }
}
