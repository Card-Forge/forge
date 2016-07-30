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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPlayOption;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * <p>
 * SpellAbilityRestriction class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityRestriction extends SpellAbilityVariables {
    // A class for handling SpellAbility Restrictions. These restrictions
    // include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)
    // The canPlay function will use these values to determine if the current
    // game state is ok with these restrictions

    /**
     * <p>
     * Constructor for SpellAbilityRestriction.
     * </p>
     */
    public SpellAbilityRestriction() {
    }

    /**
     * <p>
     * setRestrictions.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final void setRestrictions(final Map<String, String> params) {
        if (params.containsKey("Activation")) {
            final String value = params.get("Activation");
            if (value.equals("Threshold")) {
                this.setThreshold(true);
            }
            if (value.equals("Metalcraft")) {
                this.setMetalcraft(true);
            }
            if (value.equals("Delirium")) {
                this.setDelirium(true);
            }
            if (value.equals("Hellbent")) {
                this.setHellbent(true);
            }
            if (value.startsWith("Prowl")) {
                final List<String> prowlTypes = new ArrayList<String>();
                prowlTypes.add("Rogue");
                if (value.split("Prowl").length > 1) {
                    prowlTypes.add(value.split("Prowl")[1]);
                }
                this.setProwlTypes(prowlTypes);
            }
        }

        if (params.containsKey("ActivationZone")) {
            this.setZone(ZoneType.smartValueOf(params.get("ActivationZone")));
        }

        if (params.containsKey("Flashback")) {
            this.setZone(ZoneType.Graveyard);
        }

        if (params.containsKey("SorcerySpeed")) {
            this.setSorcerySpeed(true);
        }

        if (params.containsKey("InstantSpeed")) {
            this.setInstantSpeed(true);
        }

        if (params.containsKey("PlayerTurn")) {
            this.setPlayerTurn(true);
        }

        if (params.containsKey("OpponentTurn")) {
            this.setOpponentTurn(true);
        }

        if (params.containsKey("AnyPlayer")) {
            this.setAnyPlayer(true);
        }

        if (params.containsKey("AnyOpponent")) {
            this.setOpponentOnly(true);
        }

        if (params.containsKey("EnchantedControllerActivator")) {
            this.setEnchantedControllerOnly(true);
        }

        if (params.containsKey("OwnerOnly")) {
            this.setOwnerOnly(true);
        }

        if (params.containsKey("ActivationLimit")) {
            this.setLimitToCheck(params.get("ActivationLimit"));
        }

        if (params.containsKey("GameActivationLimit")) {
            this.setGameLimitToCheck(params.get("GameActivationLimit"));
        }

        if (params.containsKey("ActivationPhases")) {
            this.setPhases(PhaseType.parseRange(params.get("ActivationPhases")));
        }

        if (params.containsKey("ActivationCardsInHand")) {
            this.setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));
        }

        if (params.containsKey("ActivationChosenColor")) {
            this.setColorToCheck(params.get("ActivationChosenColor"));
        }

        if (params.containsKey("Planeswalker")) {
            this.setPwAbility(true);
        }

        if (params.containsKey("IsPresent")) {
            this.setIsPresent(params.get("IsPresent"));
            if (params.containsKey("PresentCompare")) {
                this.setPresentCompare(params.get("PresentCompare"));
            }
            if (params.containsKey("PresentZone")) {
                this.setPresentZone(ZoneType.smartValueOf(params.get("PresentZone")));
            }
        }

        if (params.containsKey("IsNotPresent")) {
            this.setIsPresent(params.get("IsNotPresent"));
            this.setPresentCompare("EQ0");
        }

        // basically PresentCompare for life totals:
        if (params.containsKey("ActivationLifeTotal")) {
            this.setLifeTotal(params.get("ActivationLifeTotal"));
            if (params.containsKey("ActivationLifeAmount")) {
                this.setLifeAmount(params.get("ActivationLifeAmount"));
            }
        }

        if (params.containsKey("CheckSVar")) {
            this.setSvarToCheck(params.get("CheckSVar"));
        }
        if (params.containsKey("SVarCompare")) {
            this.setSvarOperator(params.get("SVarCompare").substring(0, 2));
            this.setSvarOperand(params.get("SVarCompare").substring(2));
        }
    } // end setRestrictions()

    /**
     * <p>
     * checkZoneRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkZoneRestrictions(final Card c, final SpellAbility sa) {
        if (this.getZone() == null) {
            return true;
        }

        final Player activator = sa.getActivatingPlayer();
        final Zone cardZone = activator.getGame().getZoneOf(c);
        if (cardZone == null || !cardZone.is(this.getZone())) {
            // If Card is not in the default activating zone, do some additional checks
            // Not a Spell, or on Battlefield, return false
            if (!sa.isSpell() || (cardZone != null && ZoneType.Battlefield.equals(cardZone.getZoneType()))
                    || !this.getZone().equals(ZoneType.Hand)) {
                return false;
            }
            if (cardZone != null && cardZone.is(ZoneType.Stack)) {
                return false;
            }
            if (sa.isSpell()) {
                final CardPlayOption o = c.mayPlay(sa.getMayPlayHost());
                if (o != null && o.getPlayer() == activator) {
                     return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * <p>
     * checkTimingRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkTimingRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (this.isPlayerTurn() && !game.getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && !game.getPhaseHandler().getPlayerTurn().isOpponentOf(activator)) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            boolean isPhase = false;
            final PhaseType currPhase = game.getPhaseHandler().getPhase();
            for (final PhaseType s : this.getPhases()) {
                if (s == currPhase) {
                    isPhase = true;
                    break;
                }
            }

            if (!isPhase) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * checkActivatorRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkActivatorRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        if (this.isAnyPlayer()) {
            return true;
        }

        if (this.isOwnerOnly()) {
            return activator.equals(c.getOwner());
        }

        if (activator.equals(c.getController()) && !this.isOpponentOnly() && !isEnchantedControllerOnly()) {
            return true;
        }

        if (activator.isOpponentOf(c.getController()) && this.isOpponentOnly()) {
            return true;
        }
        
        if (c.getEnchantingCard() != null && activator.equals(c.getEnchantingCard().getController()) && this.isEnchantedControllerOnly()) {
        	return true;
        }

        if (sa.isSpell()) {
            final CardPlayOption o = c.mayPlay(sa.getMayPlayHost());
            if (o != null && o.getPlayer() == activator) {
                return true;
            }
        }
        
        return false;
    }
    
    public final boolean checkOtherRestrictions(final Card c, final SpellAbility sa, final Player activator) {
        final Game game = activator.getGame();

        if (this.getCardsInHand() != -1) {
            if (activator.getCardsIn(ZoneType.Hand).size() != this.getCardsInHand()) {
                return false;
            }
        }

        if (this.getColorToCheck() != null) {
            if (!sa.getHostCard().hasChosenColor(this.getColorToCheck())) {
                return false;
            }
        }
        if (this.isHellbent()) {
            if (!activator.hasHellbent()) {
                return false;
            }
        }
        if (this.isThreshold()) {
            if (!activator.hasThreshold()) {
                return false;
            }
        }
        if (this.isMetalcraft()) {
            if (!activator.hasMetalcraft()) {
                return false;
            }
        }
        if (this.isDelirium()) {
            if (!activator.hasDelirium()) {
                return false;
            }
        }
        if (this.isSurge()) {
            if (!activator.hasSurge()) {
                return false;
            }
        }
        if (this.getProwlTypes() != null && !this.getProwlTypes().isEmpty()) {
            // only true if the activating player has damaged the opponent with
            // one of the specified types
            boolean prowlFlag = false;
            for (final String type : this.getProwlTypes()) {
                if (activator.hasProwl(type)) {
                    prowlFlag = true;
                }
            }
            if (!prowlFlag) {
                return false;
            }
        }
        if (this.getIsPresent() != null) {
            CardCollectionView list = game.getCardsIn(this.getPresentZone());

            list = CardLists.getValidCards(list, this.getIsPresent().split(","), activator, c, sa);

            int right = 1;
            final String rightString = this.getPresentCompare().substring(2);
            right = AbilityUtils.calculateAmount(c, rightString, sa);
            final int left = list.size();

            if (!Expressions.compare(left, this.getPresentCompare(), right)) {
                return false;
            }
        }

        if (this.getLifeTotal() != null) {
            int life = 1;
            if (this.getLifeTotal().equals("You")) {
                life = activator.getLife();
            }
            if (this.getLifeTotal().equals("OpponentSmallest")) {
                life = activator.getOpponentsSmallestLifeTotal();
            }

            int right = 1;
            final String rightString = this.getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getHostCard(), sa.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(this.getLifeAmount().substring(2));
            }

            if (!Expressions.compare(life, this.getLifeAmount(), right)) {
                return false;
            }
        }

        if (this.isPwAbility()) {
            if (!c.hasKeyword("CARDNAME's loyalty abilities can be activated at instant speed.")
                    && !activator.canCastSorcery()) {
                return false;
            }
            final int limits = c.getAmountOfKeyword("May activate CARDNAME's loyalty abilities once");
            int numActivates = 0;
            for (final SpellAbility pwAbs : c.getAllSpellAbilities()) {
                // check all abilities on card that have their planeswalker
                // restriction set to confirm they haven't been activated
                final SpellAbilityRestriction restrict = pwAbs.getRestrictions();
                if (restrict.isPwAbility()) {
                    numActivates += restrict.getNumberTurnActivations();
                }
            }
            if (numActivates > limits) {
                return false;
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityUtils.calculateAmount(c, this.getsVarToCheck(), sa);
            final int operandValue = AbilityUtils.calculateAmount(c, this.getsVarOperand(), sa);

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityUtils.calculateAmount(sa.getHostCard(), this.getsVarToCheck(), sa);
            final int operandValue = AbilityUtils.calculateAmount(sa.getHostCard(), this.getsVarOperand(), sa);

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }
        }
    	return true;
    }

    /**
     * <p>
     * canPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean canPlay(final Card c, final SpellAbility sa) {
        if (c.isPhasedOut() || c.isUsedToPay()) {
            return false;
        }

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = c.getController();
            sa.setActivatingPlayer(activator);
            System.out.println(c.getName() + " Did not have activator set in SpellAbilityRestriction.canPlay()");
        }

        if (this.isSorcerySpeed() && !activator.canCastSorcery()) {
            return false;
        }

        if (!checkTimingRestrictions(c, sa)) {
            return false;
        }

        if (!checkActivatorRestrictions(c, sa)) {
            return false;
        }

        if (!checkZoneRestrictions(c, sa)) {
            return false;
        }
        
        if (!checkOtherRestrictions(c, sa, activator)) {
            return false;
        }

        if (this.getLimitToCheck() != null) {
            String limit = this.getLimitToCheck();
            int activationLimit = AbilityUtils.calculateAmount(c, limit, sa);
            this.setActivationLimit(activationLimit);

            if ((this.getActivationLimit() != -1) && (this.getNumberTurnActivations() >= this.getActivationLimit())) {
                return false;
            }
        }

        if (this.getGameLimitToCheck() != null) {
            String limit = this.getGameLimitToCheck();
            int gameActivationLimit = AbilityUtils.calculateAmount(c, limit, sa);
            this.setGameActivationLimit(gameActivationLimit);

            if ((this.getGameActivationLimit() != -1) && (this.getNumberGameActivations() >= this.getGameActivationLimit())) {
                return false;
            }
        }

        return true;
    } // canPlay()

} // end class SpellAbilityRestriction
