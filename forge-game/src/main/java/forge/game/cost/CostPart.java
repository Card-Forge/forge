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
package forge.game.cost;


import forge.game.CardTraitBase;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import org.apache.commons.lang3.StringUtils;

/**
 * The Class CostPart.
 */
public abstract class CostPart implements Comparable<CostPart> {
    private String originalAmount;
    private String amount;
    private final String originalType, originalTypeDescription;
    private String typeDescription, type;

    /**
     * Instantiates a new cost part.
     */
    public CostPart() {
        this("1", "Card", null);
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
        this.originalType = type;
        this.type = this.originalType;
        this.originalTypeDescription = description;
        this.typeDescription = originalTypeDescription;
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
    public final boolean payCostFromSource() {
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

    public final String getDescriptiveType() {
        String typeDesc = this.getTypeDescription();
        return typeDesc == null ? this.getType() : typeDesc;
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
     * Checks if is renewable.
     * 
     * @return true, if is renewable
     */
    public boolean isRenewable() {
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
        return StringUtils.isNumeric(amount) ? Integer.parseInt(amount) : null; 
    }

    /**
     * Can pay.
     * 
     * @param ability
     *            the ability
     * @return true, if successful
     */
    public abstract boolean canPay(SpellAbility ability);

    public abstract <T> T accept(final ICostVisitor<T> visitor);

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
    public void refund(Card source) {
    }

    /**
     * Sets the amount.
     * 
     * @param amountIn
     *            the amount to set
     */
    public void setAmount(final String amountIn) {
        this.originalAmount = amountIn;
        this.amount = this.originalAmount;
    }

    public final void applyTextChangeEffects(final CardTraitBase trait) {
        this.amount = AbilityUtils.applyAbilityTextChangeEffects(this.originalAmount, trait);
        this.type = AbilityUtils.applyAbilityTextChangeEffects(this.originalType, trait);
        this.typeDescription = AbilityUtils.applyDescriptionTextChangeEffects(this.originalTypeDescription, trait);
    }

    public abstract boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa);

    public int paymentOrder() { return 5; }

    @Override
    public int compareTo(CostPart o) {
        return this.paymentOrder() - o.paymentOrder();
    }
}
