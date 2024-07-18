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

import java.util.List;
import java.util.Map;

import forge.game.card.CardCopyService;
import org.apache.commons.lang3.ObjectUtils;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.zone.ZoneType;

/**
 * <p>
 * Abstract Spell class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Spell extends SpellAbility implements java.io.Serializable, Cloneable {

    /** Constant <code>serialVersionUID=-7930920571482203460L</code>. */
    private static final long serialVersionUID = -7930920571482203460L;

    private static boolean performanceMode = false;

    public static void setPerformanceMode(boolean performanceMode){
        Spell.performanceMode=performanceMode;
    }

    private boolean castFaceDown = false;

    public Spell(final Card sourceCard, final Cost abCost) {
        super(sourceCard, abCost);

        this.setStackDescription(sourceCard.getSpellText());
        this.getRestrictions().setZone(ZoneType.Hand);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        Card card = this.getHostCard();
        if (card.isInPlay()) {
            return false;
        }

        Player activator = this.getActivatingPlayer();
        if (activator == null) {
            activator = card.getController();
            if (activator == null) {
            	return false;
            }
        }

        final Game game = activator.getGame();
        if (game.getStack().isSplitSecondOnStack()) {
            return false;
        }

        // Save the original cost and the face down info for a later check since the LKI copy will overwrite them
        ManaCost origCost = card.getState(card.isFaceDown() ? CardStateName.Original : card.getCurrentStateName()).getManaCost();

        // do performanceMode only for cases where the activator is different than controller
        if (!Spell.performanceMode && !card.getController().equals(activator)) {
            // always make a lki copy in this case?
            card = CardCopyService.getLKICopy(card);
            card.setController(activator, 0);
        }

        card = ObjectUtils.firstNonNull(getAlternateHost(card), card);

        if (!this.getRestrictions().canPlay(card, this)) {
            return false;
        }

        // for uncastables like lotus bloom, check if manaCost is blank (except for morph spells)
        // but ignore if it comes from PlayEffect
        if (!isCastFaceDown() && !isCastFromPlayEffect()
                && isBasicSpell() && origCost.isNoCost()) {
            return false;
        }

        if (!CostPayment.canPayAdditionalCosts(this.getPayCosts(), this, false)) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkRestrictions(Card host, Player activator) {
        return !StaticAbilityCantBeCast.cantBeCastAbility(this, host, activator);
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            throw new RuntimeException("Spell : clone() error, " + ex);
        }
    }

    @Override
    public boolean isSpell() { return true; }
    @Override
    public boolean isAbility() { return false; }

    /**
     * @return the castFaceDown
     */
    @Override
    public boolean isCastFaceDown() {
        return castFaceDown;
    }

    /**
     * @param faceDown the castFaceDown to set
     */
    public void setCastFaceDown(boolean faceDown) {
        this.castFaceDown = faceDown;
    }

    public Card getAlternateHost(Card source) {
        boolean lkicheck = false;

        // need to be done before so it works with Vivien and Zoetic Cavern
        if (source.isFaceDown() && source.isInZone(ZoneType.Exile)) {
            if (!source.isLKI()) {
                source = CardCopyService.getLKICopy(source);
            }

            source.forceTurnFaceUp();
            lkicheck = true;
        }

        if (isBestow() && !source.isBestowed()) {
            if (!source.isLKI()) {
                source = CardCopyService.getLKICopy(source);
            }

            source.animateBestow(false);
            lkicheck = true;
        } else if (isCastFaceDown()) {
            // need a copy of the card to turn facedown without trigger anything
            if (!source.isLKI()) {
                source = CardCopyService.getLKICopy(source);
            }
            source.turnFaceDownNoUpdate();
            lkicheck = true;
        } else if (getCardState() != null && source.getCurrentStateName() != getCardStateName() && getHostCard().getState(getCardStateName()) != null) {
            if (!source.isLKI()) {
                source = CardCopyService.getLKICopy(source);
            }
            CardStateName stateName = getCardStateName();
            if (!source.hasState(stateName)) {
                source.addAlternateState(stateName, false);
                source.getState(stateName).copyFrom(getHostCard().getState(stateName), true);
            }

            source.setState(stateName, false);
            if (getHostCard().isDoubleFaced()) {
                source.setBackSide(getHostCard().getRules().getSplitType().getChangedStateName().equals(stateName));
            }

            // need to reset CMC
            source.setLKICMC(-1);
            source.setLKICMC(source.getCMC());
            lkicheck = true;
        } else if (hasParam("Prototype")) {
            if (!source.isLKI()) {
                source = CardCopyService.getLKICopy(source);
            }
            Long next = source.getGame().getNextTimestamp();
            source.addCloneState(CardFactory.getCloneStates(source, source, this), next);
            lkicheck = true;
        }

        return lkicheck ? source : null;
    }

    public boolean isCounterableBy(final SpellAbility sa) {
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(getHostCard());
        repParams.put(AbilityKey.SpellAbility, this);
        repParams.put(AbilityKey.Cause, sa);
        List<ReplacementEffect> list = getHostCard().getGame().getReplacementHandler().getReplacementList(ReplacementType.Counter, repParams, ReplacementLayer.CantHappen);
        return list.isEmpty();
    }
}
