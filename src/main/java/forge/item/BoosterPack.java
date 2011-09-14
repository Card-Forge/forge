package forge.item;

import java.util.List;

import net.slightlymagic.braids.util.lambda.Lambda1;

import forge.SetUtils;
import forge.card.BoosterGenerator;
import forge.card.CardSet;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class BoosterPack implements InventoryItemFromSet {

    public final static Lambda1<BoosterPack, CardSet> fnFromSet = new Lambda1<BoosterPack, CardSet>() {
        @Override public BoosterPack apply(CardSet arg1) { return new BoosterPack(arg1); } };
    
    private final CardSet cardSet;
    private final String name;
    
    private List<CardPrinted> cards = null;

    public BoosterPack(String set) {
        this(SetUtils.getSetByCodeOrThrow(set));
    }

    public BoosterPack(CardSet set) {
        cardSet = set;
        name = cardSet.getName() + " Booster Pack";
    }
    
    
    @Override public String getSet() { return cardSet.getCode(); }
    @Override public String getName() { return name; }

    @Override public String getImageFilename() {
        // TODO: need images for boosters
        return null;
    }

    public List<CardPrinted> getCards() {
        if (null == cards)
        {
            BoosterGenerator gen = new BoosterGenerator(cardSet);
            cards = gen.getBoosterPack();
            // TODO: Add land here!
        }
        return cards;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cardSet == null) ? 0 : cardSet.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoosterPack other = (BoosterPack) obj;
        if (cardSet == null) {
            if (other.cardSet != null)
                return false;
        } else if (!cardSet.equals(other.cardSet))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItem#getType()
     */
    @Override
    public String getType() {
        return "Booster Pack";
    }
    
    


}
