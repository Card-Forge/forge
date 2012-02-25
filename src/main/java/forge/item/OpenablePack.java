package forge.item;

import java.util.Arrays;
import java.util.List;

import net.slightlymagic.braids.util.UtilFunctions;

import forge.Singletons;
import forge.card.BoosterData;
import forge.card.BoosterGenerator;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.util.Predicate;

/**
 * TODO: Write javadoc for this type.
 */
public abstract class OpenablePack implements InventoryItemFromSet {
    protected final BoosterData contents;
    protected final String name;
    private List<CardPrinted> cards = null;
    
    private BoosterGenerator generator = null; 

    public OpenablePack(final String name0, final BoosterData boosterData) {
        this.contents = boosterData;
        this.name = name0;
    }    

    
    @Override
    public final String getName() {
        return this.name + " " + this.getType();
    }    
    
    @Override
    public final String getEdition() {
        return this.contents.getEdition();
    }    
    
    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public final List<CardPrinted> getCards() {
        if (null == this.cards) {
            cards = this.generate();
        }
        return this.cards;
    }    
    

    public int getTotalCards() {
        return contents.getTotal();
    }


    /*
     * (non-Javadoc)
     * 
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
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final OpenablePack other = (OpenablePack) obj;
        if (this.contents == null) {
            if (other.contents != null) {
                return false;
            }
        } else if (!this.contents.equals(other.contents)) {
            return false;
        }
        return true;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    /**
     * Hash code.
     * 
     * @return int
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.contents == null) ? 0 : this.contents.hashCode());
        return result;
    }


    protected List<CardPrinted> generate() {
        if ( null == generator ) {
            generator = new BoosterGenerator(this.contents.getEditionFilter());
        }
        List<CardPrinted> myCards = generator.getBoosterPack(this.contents);
    
        final int cntLands = this.contents.getLand();
        if (cntLands > 0) {
            myCards.add(this.getLandFromNearestSet());
        }
        return myCards;
    }


    private CardPrinted getLandFromNearestSet() {
        final CardEdition[] editions = UtilFunctions.iteratorToArray(Singletons.getModel().getEditions().iterator(), new CardEdition[]{});
        final int iThisSet = Arrays.binarySearch(editions, this.contents);
        for (int iSet = iThisSet; iSet < editions.length; iSet++) {
            final CardPrinted land = this.getRandomBasicLand(editions[iSet]);
            if (null != land) {
                return land;
            }
        }
        // if not found (though that's impossible)
        return this.getRandomBasicLand(Singletons.getModel().getEditions().get("M12"));
    }


    protected CardPrinted getRandomBasicLand(final CardEdition set) { 
        return getRandomBasicLands(set, 1).get(0);
    }
    
    protected List<CardPrinted> getRandomBasicLands(final CardEdition set, int count) {
        return Predicate.and(CardPrinted.Predicates.printedInSets(set.getCode()),
                CardRules.Predicates.Presets.IS_BASIC_LAND, CardPrinted.FN_GET_RULES)
                .random(CardDb.instance().getAllCards(), count);
    }
    
}