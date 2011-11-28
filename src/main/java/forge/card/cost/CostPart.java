/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cost;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Class CostPart.
 */
public abstract class CostPart {

    /** The is reusable. */
    private boolean isReusable = false;

    /** The is undoable. */
    private boolean isUndoable = false;

    /** The optional. */
    // private boolean optional = false;

    /** The optional type. */
    private String optionalType = null;

    /** The amount. */
    private String amount = "1";

    /** The type. */
    private String type = "Card";

    /** The type description. */
    private String typeDescription = null;

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
        this.setAmount(amount);
        this.setType(type);
        this.setTypeDescription(description);
    }

    /**
     * Gets the amount.
     * 
     * @return the amount
     */
    public final String getAmount() {
        return this.amount;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public final String getType() {
        return this.type;
    }

    /**
     * Gets the this.
     * 
     * @return the this
     */
    public final boolean getThis() {
        return this.getType().equals("CARDNAME");
    }

    /**
     * Gets the type description.
     * 
     * @return the type description
     */
    public final String getTypeDescription() {
        return this.typeDescription;
    }

    /**
     * Gets the descriptive type.
     * 
     * @return the descriptive type
     */
    public final String getDescriptiveType() {
        return this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
    }

    /**
     * Checks if is reusable.
     * 
     * @return true, if is reusable
     */
    public final boolean isReusable() {
        return this.isReusable;
    }

    /**
     * Checks if is undoable.
     * 
     * @return true, if is undoable
     */
    public final boolean isUndoable() {
        return this.isUndoable;
    }

    /**
     * Gets the optional type.
     * 
     * @return the optional type
     */
    public final String getOptionalType() {
        return this.optionalType;
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
            i = Integer.parseInt(this.getAmount());
        } catch (final NumberFormatException e) {
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
    public abstract boolean decideAIPayment(SpellAbility ability, Card source, CostPayment payment);

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
    public abstract void payAI(SpellAbility ability, Card source, CostPayment payment);

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
    public abstract boolean payHuman(SpellAbility ability, Card source, CostPayment payment);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Refund.
     * 
     * @param source
     *            the source
     */
    public abstract void refund(Card source);

    /**
     * Sets the reusable.
     * 
     * @param isReusableIn
     *            the isReusable to set
     */
    public void setReusable(final boolean isReusableIn) {
        this.isReusable = isReusableIn;
    }

    /**
     * Sets the amount.
     * 
     * @param amountIn
     *            the amount to set
     */
    public void setAmount(final String amountIn) {
        this.amount = amountIn;
    }

    /**
     * Sets the type.
     * 
     * @param typeIn
     *            the type to set
     */
    public void setType(final String typeIn) {
        this.type = typeIn;
    }

    /**
     * Sets the type description.
     * 
     * @param typeDescriptionIn
     *            the typeDescription to set
     */
    public void setTypeDescription(final String typeDescriptionIn) {
        this.typeDescription = typeDescriptionIn;
    }

    /**
     * Sets the undoable.
     * 
     * @param isUndoableIn
     *            the isUndoable to set
     */
    public void setUndoable(final boolean isUndoableIn) {
        this.isUndoable = isUndoableIn;
    }
}
