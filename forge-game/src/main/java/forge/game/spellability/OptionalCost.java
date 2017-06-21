package forge.game.spellability;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum OptionalCost {
    Conspire("Conspire"),
    Buyback("Buyback"),
    Entwine("Entwine"),
    Kicker1("Kicker"),
    Kicker2("Kicker"),
    Retrace("Retrace"),
    Surge("Surge"), // no real OptionalCost but used there
    AltCost(""), // used by prowl
    Generic("Generic"); // used by "Dragon Presence" and pseudo-kicker cards

    private String name;
    
    OptionalCost(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
