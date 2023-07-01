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

import java.io.Serializable;

import forge.card.CardType;
import org.apache.commons.lang3.StringUtils;

import forge.game.CardTraitBase;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostPart.
 */
public abstract class CostPart implements Comparable<CostPart>, Cloneable, Serializable {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    private String originalAmount;
    private String amount;
    private final String originalType, originalTypeDescription;
    private String typeDescription, type;

    protected transient SpellAbility payingTrigSA;

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

    public Integer getMaxAmountX(final SpellAbility ability, final Player payer, final boolean effect) {
        return null;
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
        return this.getType().equals("CARDNAME") || this.getType().equals("NICKNAME");
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
        if (typeDesc == null) {
            String typeS = this.getType();
            typeDesc = CardType.CoreType.isValidEnum(typeS) || typeS.equals("Card") ? typeS.toLowerCase() : typeS;
        }
        return typeDesc;
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

    public final int getAbilityAmount(SpellAbility ability) {
        return AbilityUtils.calculateAmount(ability.getHostCard(), getAmount(), ability);
    }

    public void setTrigger(SpellAbility sa) {
        payingTrigSA = sa;
    }

    /**
     * Can pay.
     *
     * @param ability
     *            the ability
     * @param payer
     * @return true, if successful
     */
    public abstract boolean canPay(SpellAbility ability, Player payer, boolean effect);

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

    public abstract boolean payAsDecided(Player payer, PaymentDecision pd, SpellAbility sa, final boolean effect);

    public int paymentOrder() { return 5; }

    public CostPart copy() {
    	CostPart clone = null;
        try {
            clone = (CostPart) clone();
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

    @Override
    public int compareTo(CostPart o) {
        return this.paymentOrder() - o.paymentOrder();
    }
}
