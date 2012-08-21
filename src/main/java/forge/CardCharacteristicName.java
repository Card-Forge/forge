package forge;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CardCharacteristicName {
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
    public static CardCharacteristicName smartValueOf(String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final CardCharacteristicName v : CardCharacteristicName.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        if ("Flip".equalsIgnoreCase(value)) {
            return CardCharacteristicName.Flipped;
        }
        if ("DoubleFaced".equalsIgnoreCase(value)) {
            return CardCharacteristicName.Transformed;
        }

        throw new IllegalArgumentException("No element named " + value + " in enum CardCharactersticName");
    }
}
