package forge.item;

import java.util.List;

import forge.BoosterGenerator;
import forge.SetUtils;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Booster implements InventoryItemFromSet {

    private final String cardSet;
    private final String name;
    
    private List<CardPrinted> cards = null;

    public Booster(String set) {
        cardSet = set;
        name = SetUtils.getSetByCodeOrThrow(set).getName() + " booster";
    }

    @Override public String getSet() { return cardSet; }
    @Override public String getName() { return name; }

    @Override public String getImageFilename() {
        // TODO: images for boosters
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


}
