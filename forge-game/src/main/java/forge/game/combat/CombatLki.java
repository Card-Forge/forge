package forge.game.combat;

import java.util.Collections;
import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CombatLki {

    public final List<AttackingBand> relatedBands;
    public final boolean isAttacker;
    
    
    public CombatLki(boolean isAttacker, List<AttackingBand> relatedBands) {
        this.isAttacker = isAttacker;
        this.relatedBands = Collections.unmodifiableList(relatedBands);
    }
    
    public AttackingBand getFirstBand() {
        return relatedBands.isEmpty() ? null : relatedBands.get(0);
    }

}
