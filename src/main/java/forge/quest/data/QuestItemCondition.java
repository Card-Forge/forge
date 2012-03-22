package forge.quest.data;

/** 
 * This class should store the quest items' properties that are to be serialized
 *
 */
public class QuestItemCondition {
    private int level;

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level; 
    }

    /**
     * Copy data from the parameter instance to 'this' instance 
     * @param current
     */
    public void takeDataFrom(QuestItemCondition source) {
        this.level = source.level;
    }
}
