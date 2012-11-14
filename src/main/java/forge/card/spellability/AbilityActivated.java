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
package forge.card.spellability;

import java.util.ArrayList;

import forge.Card;
import forge.Singletons;

import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.staticability.StaticAbility;
import forge.game.GlobalRuleChange;
import forge.game.zone.ZoneType;

/**
 * <p>
 * Abstract Ability_Activated class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbilityActivated extends SpellAbility implements java.io.Serializable {
    /** Constant <code>serialVersionUID=1L</code>. */
    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param manacost
     *            a {@link java.lang.String} object.
     */
    public AbilityActivated(final Card card, final String manacost) {
        this(card, new Cost(card, manacost, true), null);
    }

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public AbilityActivated(final Card sourceCard, final Cost abCost, final Target tgt) {
        super(sourceCard);
        this.setManaCost(abCost.getTotalMana());
        this.setPayCosts(abCost);
        if ((tgt != null) && tgt.doesTarget()) {
            this.setTarget(tgt);
        }
    }

    public abstract AbilityActivated getCopy(); /* {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (Singletons.getModel().getGame().getStack().isSplitSecondOnStack() && !this.isManaAbility()) {
            return false;
        }

        final Card c = this.getSourceCard();
        if (c.isFaceDown() && this.isIntrinsic()) {
            // Intrinsic abilities can't be activated by face down cards
            return false;
        }

        // CantBeActivated static abilities
        for (final Card ca : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantBeActivated", c, this)) {
                    return false;
                }
            }
        }

        if (c.hasKeyword("CARDNAME's activated abilities can't be activated.") || this.isSuppressed()) {
            return false;
        }

        if (this.isCycling() 
                && Singletons.getModel().getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noCycling)) {
            return false;
        }

        if (!(this.getRestrictions().canPlay(c, this))) {
            return false;
        }

        return CostPayment.canPayAdditionalCosts(this.getPayCosts(), this);
    }
    
    /* (non-Javadoc)
     * @see forge.card.spellability.SpellAbility#resolve()
     */
}
