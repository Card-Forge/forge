package forge.game.replacement;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum ReplacementLayer {
    Control, // 616.1b
    Copy, // 616.1c
    Transform, // 616.1d
    Other;

    /**
     * TODO: Write javadoc for this method.
     * @param substring
     * @return
     */
    public static ReplacementLayer smartValueOf(String value) {
        if (value == null) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final ReplacementLayer v : ReplacementLayer.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum ReplacementLayer");
    }
}
