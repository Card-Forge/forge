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
    CHARGE(),
    DIVINITY(),
    FADE(),
    HOOFPRINT(),
    ICE(),
    LOYALTY(),
    M1M1("-1/-1"),
    MANA(),
    P0M1("+0/+1"),
    P1P1("+1/+1"),
    QUEST(),
    SPORE(),
    TIME(),
    TOWER("tower");
    
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
}
