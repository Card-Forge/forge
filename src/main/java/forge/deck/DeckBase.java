package forge.deck;

import java.io.Serializable;
import forge.item.CardPrinted;
import forge.item.IHasName;
import forge.item.ItemPoolView;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class DeckBase implements IHasName, Serializable, Comparable<DeckBase> {
    private static final long serialVersionUID = -7538150536939660052L;
    // gameType is from Constant.GameType, like GameType.Regular
    
    private final String name;
    private String comment = null;

    public DeckBase(String name0) {
        name = name0;
    }
    
    @Override
    public int compareTo(final DeckBase d) {
        return this.getName().compareTo(d.getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Deck) {
            final Deck d = (Deck) o;
            return this.getName().equals(d.getName());
        }
        return false;
    }    
    
    @Override
    public String getName() {
        return this.name;
    }

    public abstract ItemPoolView<CardPrinted> getCardPool();
    
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * <p>
     * getComment.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getComment() {
        return this.comment;
    }
    
    protected abstract DeckBase newInstance(String name0);  
    
    protected void cloneFieldsTo(DeckBase clone) {
        clone.comment = this.comment;
    }

    public DeckBase copyTo(String name0) {
        DeckBase obj = newInstance(name0);
        cloneFieldsTo(obj);
        return obj;
    }
}
