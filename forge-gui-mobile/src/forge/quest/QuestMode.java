package forge.quest;

/** 
 * TODO: Write javadoc for this type.
 *
 */

public enum QuestMode {
    // Do not apply checkstyle here, to maintain compatibility with old saves
    Fantasy,
    Classic,
    Gauntlet;

    /**
     * TODO: Write javadoc for this method.
     * @param value
     * @param classic2
     * @return
     */
    public static QuestMode smartValueOf(String value, QuestMode defaultValue) {
        if (null == value) {
            return defaultValue;
        }

        final String valToCompate = value.trim();
        for (final QuestMode v : QuestMode.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        return QuestMode.Classic;
    }
}
