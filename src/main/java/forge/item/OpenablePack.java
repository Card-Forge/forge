package forge.item;

import java.util.List;

import forge.card.BoosterData;
import forge.card.BoosterGenerator;

/**
 * TODO: Write javadoc for this type.
 */
public abstract class OpenablePack implements InventoryItemFromSet {
    protected final BoosterData contents;
    protected final String name;
    private List<CardPrinted> cards = null;

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
    

    protected abstract List<CardPrinted> generate();
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
    
}