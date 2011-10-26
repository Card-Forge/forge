package forge.card.spellability;

import forge.Card;

/**
 * <p>
 * Abstract Ability_Sub class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Ability_Sub extends SpellAbility implements java.io.Serializable {
    /** Constant <code>serialVersionUID=4650634415821733134L</code>. */
    private static final long serialVersionUID = 4650634415821733134L;

    private SpellAbility parent = null;

    /**
     * <p>
     * Constructor for Ability_Sub.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public Ability_Sub(final Card sourceCard, final Target tgt) {
        super(SpellAbility.Ability, sourceCard);
        setTarget(tgt);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        // this should never be on the Stack by itself
        return false;
    }

    /**
     * <p>
     * chkAI_Drawback.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean chkAI_Drawback();

    /** {@inheritDoc} */
    public abstract boolean doTrigger(boolean mandatory);

    /**
     * <p>
     * Setter for the field <code>parent</code>.
     * </p>
     * 
     * @param parent
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void setParent(final SpellAbility parent) {
        this.parent = parent;
    }

    /**
     * <p>
     * Getter for the field <code>parent</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getParent() {
        return parent;
    }
}
