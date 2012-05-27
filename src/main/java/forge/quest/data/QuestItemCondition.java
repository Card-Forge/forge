package forge.quest.data;

/** 
 * This class should store the quest items' properties that are to be serialized.
 *
 */
public class QuestItemCondition {
    private int level;
    private boolean selected = false;

    /** @return int */
    public int getLevel() {
        return level;
    }

    /** @param level int */
    public void setLevel(int level) {
        this.level = level;
    }

    /** @return boolean */
    public boolean isSelected() {
        return selected;
    }

    /** @param selected0 &emsp; boolean */
    public void setSelected(boolean selected0) {
        this.selected = selected0;
    }

    /**
     * Copy data from the parameter instance to 'this' instance.
     * @param source QuestItemCondition
     */
    public void takeDataFrom(QuestItemCondition source) {
        this.level = source.level;
    }
}
