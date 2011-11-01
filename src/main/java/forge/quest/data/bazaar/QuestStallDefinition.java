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
    private String name;

    /** The display name. */
    private String displayName;

    /** The icon name. */
    private String iconName;

    /** The fluff. */
    private String fluff;

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
    public QuestStallDefinition(final String name, final String displayName, final String fluff, final String iconName) {
        this.setName(name);
        this.setDisplayName(displayName);
        this.setFluff(fluff);
        this.setIconName(iconName);
    }

    /**
     * @return the fluff
     */
    public String getFluff() {
        return fluff;
    }

    /**
     * @param fluff
     *            the fluff to set
     */
    public void setFluff(String fluff) {
        this.fluff = fluff; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the iconName
     */
    public String getIconName() {
        return iconName;
    }

    /**
     * @param iconName
     *            the iconName to set
     */
    public void setIconName(String iconName) {
        this.iconName = iconName; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name; // TODO: Add 0 to parameter's name.
    }
}
