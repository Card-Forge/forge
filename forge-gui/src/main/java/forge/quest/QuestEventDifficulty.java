package forge.quest;

import org.apache.commons.lang3.StringUtils;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum QuestEventDifficulty {
    EASY  ("easy",      1. ),
    MEDIUM("medium",    1.5),
    HARD  ("hard",      2. ),
    EXPERT("very hard", 3. );

    private final String inFile;
    private final double multiplier;

    private QuestEventDifficulty(final String storedInFile, final double multiplier) {
        inFile = storedInFile;
        this.multiplier = multiplier;
    }

    public final String getTitle() {
        return inFile;
    }

    public final double getMultiplier() {
        return this.multiplier;
    }

    public static QuestEventDifficulty fromString(final String src) {
        if ( StringUtils.isBlank(src) )
            return MEDIUM; // player have custom files, that didn't specify a valid difficulty

        for(QuestEventDifficulty qd : QuestEventDifficulty.values()) {
            if( src.equalsIgnoreCase(qd.inFile) || src.equalsIgnoreCase(qd.name()) )
                return qd;
        }
        return null;
    }
}
