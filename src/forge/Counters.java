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
    BLAZE(),
    BRIBERY(),
    CHARGE(),
    DIVINITY(),
    EON(),
    FADE(),
    HOOFPRINT(),
    ICE(),
    INFECTION(),
    KI(),
    LEVEL(),
    LOYALTY(),
    M1M1("-1/-1"),
    MANA(),
    MINING(),
    PHYLACTERY,
    P0M1("+0/-1"),
    P0M2("+0/-2"),
    P1P1("+1/+1"),
    P1P0("+1/+0"),
    QUEST(),
    SPORE(),
    STORAGE(),
    TIME(),
    TOWER("tower"),
    VERSE(),
    WIND();
    
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
