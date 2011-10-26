package forge;

/**
 * <p>
 * ZCTrigger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public enum ZCTrigger {

    /** The ENTERFIELD. */
    ENTERFIELD("comes into play", "any > field"), // explanation: zone before
                                                  // last trigger check ">" zone
                                                  // card currently in
    /** The LEAVEFIELD. */
    LEAVEFIELD("leaves play", "field > any"),

    /** The DESTROY. */
    DESTROY("is put into a graveyard from play", "field > grave"),

    /** The ENTERGRAVE. */
    ENTERGRAVE("is put into a graveyard from anywhere", "any > grave");

    /** The rule text. */
    public String ruleText;

    /** The trigger zones. */
    public String[] triggerZones;

    /**
     * <p>
     * Constructor for ZCTrigger.
     * </p>
     * 
     * @param text
     *            a {@link java.lang.String} object.
     * @param tofrom
     *            a {@link java.lang.String} object.
     */
    ZCTrigger(final String text, final String tofrom) {
        this.ruleText = text;
        this.triggerZones = tofrom.split(" > ");
    }

    /**
     * <p>
     * triggerOn.
     * </p>
     * 
     * @param sourceZone
     *            a {@link java.lang.String} object.
     * @param destintationZone
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean triggerOn(final String sourceZone, final String destintationZone) {
        return ((triggerZones[0].equals("any") || triggerZones[0].equals(sourceZone)) && (triggerZones[1].equals("any") || triggerZones[0]
                .equals(sourceZone)));
    }

    /**
     * <p>
     * getTrigger.
     * </p>
     * 
     * @param description
     *            a {@link java.lang.String} object.
     * @return a {@link forge.ZCTrigger} object.
     */
    public static ZCTrigger getTrigger(final String description) {
        for (ZCTrigger t : ZCTrigger.values()) {
            if (t.ruleText.equals(description)) {
                return t;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ruleText;
    }
}
