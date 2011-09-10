/**
 * Counters.java
 *
 * Created on 17.02.2010
 */

package forge;


/**
 * The class Counters.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public enum Counters {
    AGE(),
    ARROW(),
    ARROWHEAD(),
    AWAKENING(),
    BLAZE(),
    BLOOD(),
    BOUNTY(),
    BRIBERY(),
    CARRION(),
    CHARGE(),
    CORPSE(),
    CREDIT(),
    CURRENCY(),
    DEATH(),
    DELAY(),
    DEPLETION(),
    DEVOTION(),
    DIVINITY(),
    DOOM(),
    ENERGY(),
    EON(),
    FADE(),
    FEATHER(),
    FLOOD(),
    FUSE(),
    GLYPH(),
    GOLD(),
    GROWTH(),
    HATCHLING(),
    HEALING(),
    HOOFPRINT(),
    ICE(),
    INFECTION(),
    INTERVENTION(),
    JAVELIN(),
    KI(),
    LEVEL("Level"),
    LORE(),
    LOYALTY(),
    LUCK(),
    M0M1("-0/-1"),
    M0M2("-0/-2"),
    M1M0("-1/-0"),
    M1M1("-1/-1"),
    M2M1("-2/-1"),
    M2M2("-2/-2"),
    MANA(),
    MINING(),
    MIRE(),
    OMEN(),
    ORE(),
    PAGE(),
    PETAL(),
    PIN(),
    PLAGUE(),
    PRESSURE(),
    PHYLACTERY,
    POLYP(),
    PUPA(),
    P0P1("+0/+1"),
    P1P0("+1/+0"),
    P1P1("+1/+1"),
    P1P2("+1/+2"),
    P2P2("+2/+2"),
    QUEST(),
    SCREAM(),
    SHELL(),
    SHIELD(),
    SHRED(),
    SLEEP(),
    SLEIGHT(),
    SOOT(),
    SPORE(),
    STORAGE(),
    TIDE(),
    TIME(),
    TOWER("tower"),
    TRAINING(),
    TRAP(),
    TREASURE(),
    VELOCITY(),
    VERSE(),
    VITALITY(),
    WIND(),
    WISH();

    private String name;

    /**
     * <p>Constructor for Counters.</p>
     */
    private Counters() {
        this.name = name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }

    /**
     * <p>Constructor for Counters.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    private Counters(final String nameIn) {
        this.name = nameIn;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>getType.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link forge.Counters} object.
     */
    public static Counters getType(final String name) {
        String replacedName = name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase();
        return Enum.valueOf(Counters.class, replacedName);
    }
}
