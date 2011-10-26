package forge.card.cost;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Class CostPart.
 */
public abstract class CostPart {

    /** The is reusable. */
    protected boolean isReusable = false;

    /** The is undoable. */
    protected boolean isUndoable = false;

    /** The optional. */
    protected boolean optional = false;

    /** The optional type. */
    protected String optionalType = null;

    /** The amount. */
    protected String amount = "1";

    /** The type. */
    protected String type = "Card";

    /** The type description. */
    protected String typeDescription = null;

    /**
     * Instantiates a new cost part.
     */
    public CostPart() {
    }

    /**
     * Instantiates a new cost part.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostPart(final String amount, final String type, final String description) {
        this.amount = amount;
        this.type = type;
        this.typeDescription = description;
    }

    /**
     * Gets the amount.
     * 
     * @return the amount
     */
    public final String getAmount() {
        return amount;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * Gets the this.
     * 
     * @return the this
     */
    public final boolean getThis() {
        return type.equals("CARDNAME");
    }

    /**
     * Gets the type description.
     * 
     * @return the type description
     */
    public final String getTypeDescription() {
        return typeDescription;
    }

    /**
     * Gets the descriptive type.
     * 
     * @return the descriptive type
     */
    public final String getDescriptiveType() {
        return typeDescription == null ? type : typeDescription;
    }

    /**
     * Checks if is reusable.
     * 
     * @return true, if is reusable
     */
    public final boolean isReusable() {
        return isReusable;
    }

    /**
     * Checks if is undoable.
     * 
     * @return true, if is undoable
     */
    public final boolean isUndoable() {
        return isUndoable;
    }

    /**
     * Gets the optional type.
     * 
     * @return the optional type
     */
    public final String getOptionalType() {
        return optionalType;
    }

    /**
     * Sets the optional type.
     * 
     * @param optionalType
     *            the new optional type
     */
    public final void setOptionalType(final String optionalType) {
        this.optionalType = optionalType;
    }

    /**
     * Convert amount.
     * 
     * @return the integer
     */
    public final Integer convertAmount() {
        Integer i = null;
        try {
            i = Integer.parseInt(amount);
        } catch (NumberFormatException e) {
        }
        return i;
    }

    /**
     * Can pay.
     * 
     * @param ability
     *            the ability
     * @param source
     *            the source
     * @param activator
     *            the activator
     * @param cost
     *            the cost
     * @return true, if successful
     */
    public abstract boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost);

    /**
     * Decide ai payment.
     * 
     * @param ability
     *            the ability
     * @param source
     *            the source
     * @param payment
     *            the payment
     * @return true, if successful
     */
    public abstract boolean decideAIPayment(SpellAbility ability, Card source, Cost_Payment payment);

    /**
     * Pay ai.
     * 
     * @param ability
     *            the ability
     * @param source
     *            the source
     * @param payment
     *            the payment
     */
    public abstract void payAI(SpellAbility ability, Card source, Cost_Payment payment);

    /**
     * Pay human.
     * 
     * @param ability
     *            the ability
     * @param source
     *            the source
     * @param payment
     *            the payment
     * @return true, if successful
     */
    public abstract boolean payHuman(SpellAbility ability, Card source, Cost_Payment payment);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public abstract String toString();

    /**
     * Refund.
     * 
     * @param source
     *            the source
     */
    public abstract void refund(Card source);
}
