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
package forge.game.spellability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.player.Player;
import forge.game.player.PlayerController.FullControlFlag;
import forge.game.staticability.StaticAbilityCantBeCast;

/**
 * <p>
 * Abstract Ability_Activated class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbilityActivated extends SpellAbility implements Cloneable {

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param manacost
     *            a {@link java.lang.String} object.
     */
    public AbilityActivated(final Card card, final String manacost) {
        this(card, new Cost(manacost, true), null);
    }

    /**
     * <p>
     * Constructor for Ability_Activated.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @param abCost
     *            a {@link forge.game.cost.Cost} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     */
    public AbilityActivated(final Card sourceCard, final Cost abCost, final TargetRestrictions tgt) {
        super(sourceCard, abCost);
        this.setTargetRestrictions(tgt);
    }

    public boolean isActivatedAbility() { return !isTrigger(); }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        // CR 118.6 cost is unpayable
        if (getPayCosts().hasManaCost() && getPayCosts().getCostMana().getManaCostFor(this).isNoCost()) {
            return false;
        }

        Player player = getActivatingPlayer();
        if (player == null) {
            player = this.getHostCard().getController();
        }
        
        final Game game = player.getGame();
        if (game.getStack().isSplitSecondOnStack() && !this.isManaAbility()) {
            return false;
        }

        final Card c = this.getHostCard();

        if (isSuppressed()) {
            return false;
        }
        if (c.isDetained()) {
            return false;
        }

        if (!getRestrictions().canPlay(c, this)) {
            return false;
        }

        return player.getController().isFullControl(FullControlFlag.AllowPaymentStartWithMissingResources)
                || CostPayment.canPayAdditionalCosts(this.getPayCosts(), this, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkRestrictions(Card host, Player activator) {
        return !StaticAbilityCantBeCast.cantBeActivatedAbility(this, host, activator);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPossible() {
    	//consider activated abilities possible always and simply disable if not currently playable
    	//the exception is to consider them not possible if there's a zone or activator restriction that's not met

        // FIXME: Something is potentially leading to hard-to-reproduce conditions where this method is getting called
        // with no activator set for the SA (by the AI). Most likely deserves a better fix in the future.
        if (this.getActivatingPlayer() == null) {
            this.setActivatingPlayer(this.getHostCard().getController());
            System.out.println(this.getHostCard().getName() + " Did not have activator set in AbilityActivated.isPossible");
        }

    	return this.getRestrictions().checkZoneRestrictions(this.getHostCard(), this) &&
    		   this.getRestrictions().checkActivatorRestrictions(this.getHostCard(), this);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean promptIfOnlyPossibleAbility() {
    	return false; //TODO: allow showing prompt based on whether ability has cost that requires user input and possible "misclick protection" setting
    	//return !this.isManaAbility(); //prompt user for non-mana activated abilities even is only possible ability
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("AbilityActivated : clone() error, " + ex);
        }
    }
}
