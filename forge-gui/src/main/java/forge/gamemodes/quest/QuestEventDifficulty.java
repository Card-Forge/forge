package forge.gamemodes.quest;

import org.apache.commons.lang3.StringUtils;

import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.model.FModel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum QuestEventDifficulty {
    EASY  ("easy",      1. ),
    MEDIUM("medium",    1.5),
    HARD  ("hard",      2. ),
    EXPERT("very hard", 3. ),
    WILD("wild", FModel.getQuestPreferences().getPrefDouble(QPref.WILD_OPPONENTS_MULTIPLIER) );

    private final String inFile;
    private final double multiplier;

    QuestEventDifficulty(final String storedInFile, final double multiplier) {
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
