package forge.quest.data.bazaar;

/**
 * <p>QuestStallDefinition class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestStallDefinition {
    public String name;
    public String displayName;
    public String iconName;
    public String fluff;

    /**
     * <p>Constructor for QuestStallDefinition.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param displayName a {@link java.lang.String} object.
     * @param fluff a {@link java.lang.String} object.
     * @param iconName a {@link java.lang.String} object.
     */
    public QuestStallDefinition(String name, String displayName, String fluff, String iconName) {
        this.name = name;
        this.displayName = displayName;
        this.fluff = fluff;
        this.iconName = iconName;
    }
}
