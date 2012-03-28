package forge;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CardCharactersticName {
    Original,
    FaceDown,
    Flipped,
    Cloner,
    Transformed,
    Alternate,
    Cloned;

    /**
     * TODO: Write javadoc for this method.
     * @param substring
     * @return
     */
    public static CardCharactersticName smartValueOf(String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final CardCharactersticName v : CardCharactersticName.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        if ("Flip".equalsIgnoreCase(value)) {
            return CardCharactersticName.Flipped;
        }
        if ("DoubleFaced".equalsIgnoreCase(value)) {
            return CardCharactersticName.Transformed;
        }

        throw new IllegalArgumentException("No element named " + value + " in enum CardCharactersticName");
    }
}
