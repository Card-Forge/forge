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
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;

/**
 * The Class CostPart.
 */
public abstract class CostPart {

    /** The optional. */
    // private boolean optional = false;

    /** The amount. */
    private String amount = "1";

    /** The type. */
    private final String type;

    /** The type description. */
    private final String typeDescription;

    /**
     * Instantiates a new cost part.
     */
    public CostPart() {
        type = "Card";
        typeDescription = null;
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
        this.type = type;
        this.typeDescription = description;
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
    public final boolean isTargetingThis() {
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
    public boolean isReusable() {
        return false;
    }

    /**
     * Checks if is undoable.
     * 
     * @return true, if is undoable
     */
    public boolean isUndoable() {
        return false;
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
     * @param game 
     * @return true, if successful
     */
    public abstract boolean canPay(SpellAbility ability, Card source, Player activator, Cost cost, GameState game);

    /**
     * Decide ai payment.
     * 
     * @param ai {@link forge.player.Player}
     * @param ability {@link forge.card.spellability.SpellAbility}
     * @param source {@link forge.Card}
     * @param payment {@link forge.card.cost.CostPayment}
     * @return true, if successful
     */
    public abstract boolean decideAIPayment(final Player ai, SpellAbility ability, Card source, CostPayment payment);

    /**
     * Pay ai.
     * 
     * @param ai {@link forge.player.Player}
     * @param ability {@link forge.card.spellability.SpellAbility}
     * @param source {@link forge.Card}
     * @param payment {@link forge.card.cost.CostPayment}
     * @param game 
     */
    public abstract void payAI(final Player ai, SpellAbility ability, Card source, CostPayment payment, GameState game);

    /**
     * Pay human.
     * 
     * @param ability {@link forge.card.spellability.SpellAbility}
     * @param source {@link forge.Card}
     * @param payment {@link forge.card.cost.CostPayment}
     * @param game 
     * @return true, if successful
     */
    public abstract boolean payHuman(SpellAbility ability, Card source, CostPayment payment, GameState game);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Refund. Overridden in classes which know how to refund.
     * 
     * @param source
     *            the source
     */
    public void refund(Card source) {}


    /**
     * Sets the amount.
     * 
     * @param amountIn
     *            the amount to set
     */
    public void setAmount(final String amountIn) {
        this.amount = amountIn;
    }
}
