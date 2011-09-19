package forge.game;

/** 
 * GameType is an enum to determine the type of current game. :)
 */
public enum GameType {
    Constructed(false),
    Sealed(true),
    Draft(true),
    Commander(false),
    Quest(true);

    private final boolean bLimited;
    public final boolean isLimited() { return bLimited; }

    GameType(final boolean isLimited) {
        bLimited = isLimited;
    }

    public static GameType smartValueOf(final String value) {
        String valToCompate = value.trim();
        for (GameType v : GameType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }
}
