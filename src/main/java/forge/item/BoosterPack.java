package forge.item;

import java.util.List;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import forge.SetUtils;
import forge.card.BoosterGenerator;
import forge.card.CardRules;
import forge.card.CardSet;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class BoosterPack implements InventoryItemFromSet {

    /** The Constant fnFromSet. */
    public static final Lambda1<BoosterPack, CardSet> fnFromSet = new Lambda1<BoosterPack, CardSet>() {
        @Override
        public BoosterPack apply(final CardSet arg1) {
            return new BoosterPack(arg1);
        }
    };

    private final CardSet cardSet;
    private final String name;

    private List<CardPrinted> cards = null;

    /**
     * Instantiates a new booster pack.
     *
     * @param set the set
     */
    public BoosterPack(final String set) {
        this(SetUtils.getSetByCodeOrThrow(set));
    }

    /**
     * Instantiates a new booster pack.
     *
     * @param set the set
     */
    public BoosterPack(final CardSet set) {
        cardSet = set;
        name = cardSet.getName() + " Booster Pack";
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    /**
     * @return String
     */
    @Override
    public final String getSet() {
        return cardSet.getCode();
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getName()
     */
    /**
     * @return String
     */
    @Override
    public final String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    /**
     * @return String
     */
    @Override
    public final String getImageFilename() {
        return "booster/" + cardSet.getCode() + ".png";
    }

    private CardPrinted getRandomBasicLand(final CardSet set) {
        return Predicate.and(CardPrinted.Predicates.printedInSets(set.getCode()),
                CardRules.Predicates.Presets.IS_BASIC_LAND, CardPrinted.fnGetRules).random(
                CardDb.instance().getAllCards());
    }

    private CardPrinted getLandFromNearestSet() {
        List<CardSet> sets = SetUtils.getAllSets();
        int iThisSet = sets.indexOf(cardSet);
        for (int iSet = iThisSet; iSet < sets.size(); iSet++) {
            CardPrinted land = getRandomBasicLand(sets.get(iSet));
            if (null != land) {
                return land;
            }
        }
        // if not found (though that's impossible)
        return getRandomBasicLand(SetUtils.getSetByCode("M12"));
    }

    private void generate() {
        BoosterGenerator gen = new BoosterGenerator(cardSet);
        cards = gen.getBoosterPack();

        int cntLands = cardSet.getBoosterData().getLand();
        if (cntLands > 0) {
            cards.add(getLandFromNearestSet());
        }
    }

    /**
     * Gets the cards.
     *
     * @return the cards
     */
    public final List<CardPrinted> getCards() {
        if (null == cards) {
            generate();
        }
        return cards;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /**
     * @return int
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cardSet == null) ? 0 : cardSet.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BoosterPack other = (BoosterPack) obj;
        if (cardSet == null) {
            if (other.cardSet != null) {
                return false;
            }
        } else if (!cardSet.equals(other.cardSet)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItem#getType()
     */
    /**
     * @return String
     */
    @Override
    public final String getType() {
        return "Booster Pack";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    /**
     * @return Object
     */
    @Override
    public final Object clone() {
        return new BoosterPack(cardSet); // it's ok to share a reference to
                                         // cardSet which is static anyway
    }

}
