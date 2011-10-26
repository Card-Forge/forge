package forge.quest.data.bazaar;

/**
 * <p>
 * QuestStallDefinition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestStallDefinition {

    /** The name. */
    public String name;

    /** The display name. */
    public String displayName;

    /** The icon name. */
    public String iconName;

    /** The fluff. */
    public String fluff;

    /**
     * <p>
     * Constructor for QuestStallDefinition.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param displayName
     *            a {@link java.lang.String} object.
     * @param fluff
     *            a {@link java.lang.String} object.
     * @param iconName
     *            a {@link java.lang.String} object.
     */
    public QuestStallDefinition(final String name,
            final String displayName, final String fluff, final String iconName) {
        this.name = name;
        this.displayName = displayName;
        this.fluff = fluff;
        this.iconName = iconName;
    }
}
