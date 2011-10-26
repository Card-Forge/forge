package forge.card.cost;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

/**
 * The Class CostPartWithList.
 */
public abstract class CostPartWithList extends CostPart {

    /** The list. */
    protected CardList list = null;

    /**
     * Gets the list.
     * 
     * @return the list
     */
    public final CardList getList() {
        return list;
    }

    /**
     * Sets the list.
     * 
     * @param setList
     *            the new list
     */
    public final void setList(final CardList setList) {
        list = setList;
    }

    /**
     * Reset list.
     */
    public final void resetList() {
        list = new CardList();
    }

    /**
     * Adds the to list.
     * 
     * @param c
     *            the c
     */
    public final void addToList(final Card c) {
        if (list == null) {
            resetList();
        }
        list.add(c);
    }

    /**
     * Adds the list to hash.
     * 
     * @param sa
     *            the sa
     * @param hash
     *            the hash
     */
    public final void addListToHash(final SpellAbility sa, final String hash) {
        for (Card card : list) {
            sa.addCostToHashList(card, hash);
        }
    }

    /**
     * Instantiates a new cost part with list.
     */
    public CostPartWithList() {
    }

    /**
     * Instantiates a new cost part with list.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostPartWithList(final String amount, final String type, final String description) {
        super(amount, type, description);
        resetList();
    }
}
