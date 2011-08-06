/**
 * Counters.java
 * 
 * Created on 17.02.2010
 */

package forge;


/**
 * The class Counters.
 * 
 * @version V0.0 17.02.2010
 * @author Clemens Koza
 */
public enum Counters {
    AGE(),
    ARROW(),
    BLAZE(),
    BLOOD(),
    BRIBERY(),
    CHARGE(),
    CORPSE(),
    CREDIT(),
    DELAY(),
    DEPLETION(),
    DEVOTION(),
    DIVINITY(),
    DOOM(),
    ENERGY(),
    EON(),
    FADE(),
    FUSE(),
    GLYPH(),
    HEALING(),
    HOOFPRINT(),
    ICE(),
    INFECTION(),
    INTERVENTION(),
    JAVELIN(),
    KI(),
    LEVEL(),
    LOYALTY(),
    LUCK(),
    M0M1("-0/-1"),
    M1M1("-1/-1"),
    M2M1("-2/-1"),
    MANA(),
    MINING(),
    OMEN(),
    PAGE(),
    PRESSURE(),
    PHYLACTERY,
    POLYP(),
    PUPA(),
    P0M1("+0/-1"),
    P0M2("+0/-2"),
    P0P1("+0/+1"),
    P1P0("+1/+0"),
    P1P1("+1/+1"),
    P1P2("+1/+2"),
    P2P2("+2/+2"),
    QUEST(),
    SCREAM(),
    SLEIGHT(),
    SPORE(),
    STORAGE(),
    TIDE(),
    TIME(),
    TOWER("tower"),
    TREASURE(),
    VERSE(),
    WIND(),
    WISH();
    
    private String name;
    
    private Counters() {
        this.name = name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }
    
    private Counters(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    public static Counters getType(String name)
    {
    	return Enum.valueOf(Counters.class, name.replace("/", "").replaceAll("\\+", "p").replaceAll("\\-", "m").toUpperCase());
    }
}
