package forge.quest;

import org.apache.commons.lang3.StringUtils;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum QuestEventDifficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard"),
    EXPERT("very hard");
    
    String inFile;
    
    private QuestEventDifficulty(String storedInFile) {
        inFile = storedInFile;
    }
    
    public final String getTitle() {
        return inFile;
    }
    
    public static QuestEventDifficulty fromString(String src) {
        if ( StringUtils.isBlank(src) )
            return MEDIUM; // player have custom files, that didn't specify a valid difficulty

        for(QuestEventDifficulty qd : QuestEventDifficulty.values()) {
            if( src.equalsIgnoreCase(qd.inFile) || src.equalsIgnoreCase(qd.name()) )
                return qd;
        }
        return null;
    }
}
