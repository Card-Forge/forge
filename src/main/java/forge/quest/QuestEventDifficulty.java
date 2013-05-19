package forge.quest;

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
        for(QuestEventDifficulty qd : QuestEventDifficulty.values()) {
            if( src.equalsIgnoreCase(qd.inFile) || src.equalsIgnoreCase(qd.name()) )
                return qd;
        }
        return null;
    }
}
