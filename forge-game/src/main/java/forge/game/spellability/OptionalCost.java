package forge.game.spellability;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum OptionalCost {
    Buyback("Buyback"),
    Entwine("Entwine"),
    Kicker1("Kicker"),
    Kicker2("Kicker"),
    Retrace("Retrace"),
    Jumpstart("Jump-start"),
    ReduceW("(to reduce white mana)"),
    ReduceU("(to reduce blue mana)"),
    ReduceB("(to reduce black mana)"),
    ReduceR("(to reduce red mana)"),
    ReduceG("(to reduce green mana)"),
    AltCost(""),
    Flash("Flash"), // used for Pay Extra for Flash
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
