package forge.game;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum GameType {
    Constructed(false),
    Sealed(true),
    Draft(true),
    Quest(true);

    private final boolean bLimited;
    public final boolean isLimited() { return bLimited; }

    GameType(final boolean isLimited) {
        bLimited = isLimited;
    }

    public static GameType smartValueOf(String value){
        String valToCompate = value.trim();
        for (GameType v : GameType.values()) {
            if ( v.name().compareToIgnoreCase(valToCompate) == 0 ) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }
}
