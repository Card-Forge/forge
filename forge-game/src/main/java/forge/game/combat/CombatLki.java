package forge.game.combat;

import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CombatLki {

    public final FCollectionView<AttackingBand> relatedBands;
    public final boolean isAttacker;

    public CombatLki(boolean isAttacker, FCollectionView<AttackingBand> relatedBands) {
        this.isAttacker = isAttacker;
        this.relatedBands = new FCollection<AttackingBand>(relatedBands);;
    }

    public AttackingBand getFirstBand() {
        return relatedBands.isEmpty() ? null : relatedBands.get(0);
    }

}
