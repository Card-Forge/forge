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
import java.util.HashMap;
import java.util.List;

import forge.Card;

import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * <p>
 * SpellAbility_Condition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbilityCondition extends SpellAbilityVariables {
    // A class for handling SpellAbility Conditions. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)
    // The CanPlay function will use these values to determine if the current
    // game state is ok with these restrictions

    /**
     * <p>
     * Constructor for SpellAbility_Condition.
     * </p>
     */
    public SpellAbilityCondition() {
    }

    /**
     * <p>
     * setConditions.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     */
    public final void setConditions(final HashMap<String, String> params) {
        if (params.containsKey("Condition")) {
            final String value = params.get("Condition");
            if (value.equals("Threshold")) {
                this.setThreshold(true);
            }
            if (value.equals("Metalcraft")) {
                this.setMetalcraft(true);
            }
            if (value.equals("Hellbent")) {
                this.setHellbent(true);
            }
            if (value.equals("Kicked")) {
                this.setKicked(true);
            }
        }

        if (params.containsKey("ConditionZone")) {
            this.setZone(ZoneType.smartValueOf(params.get("ContitionZone")));
        }

        if (params.containsKey("ConditionSorcerySpeed")) {
            this.setSorcerySpeed(true);
        }

        if (params.containsKey("ConditionPlayerTurn")) {
            this.setPlayerTurn(true);
        }

        if (params.containsKey("ConditionOpponentTurn")) {
            this.setOpponentTurn(true);
        }

        if (params.containsKey("ConditionPhases")) {
            this.setPhases(PhaseType.parseRange(params.get("ConditionPhases")));
        }

        if (params.containsKey("ConditionAllM12Empires")) {
            this.setAllM12Empires(true);
        }
        if (params.containsKey("ConditionNotAllM12Empires")) {
            this.setNotAllM12Empires(true);
        }

        if (params.containsKey("ConditionCardsInHand")) {
            this.setActivateCardsInHand(Integer.parseInt(params.get("ConditionCardsInHand")));
        }

        // Condition version of IsPresent stuff
        if (params.containsKey("ConditionPresent")) {
            this.setIsPresent(params.get("ConditionPresent"));
            if (params.containsKey("ConditionCompare")) {
                this.setPresentCompare(params.get("ConditionCompare"));
            }
        }

        if (params.containsKey("ConditionDefined")) {
            this.setPresentDefined(params.get("ConditionDefined"));
        }

        if (params.containsKey("ConditionNotPresent")) {
            this.setIsPresent(params.get("ConditionNotPresent"));
            this.setPresentCompare("EQ0");
        }

        // basically PresentCompare for life totals:
        if (params.containsKey("ConditionLifeTotal")) {
            this.setLifeTotal(params.get("ConditionLifeTotal"));
            if (params.containsKey("ConditionLifeAmount")) {
                this.setLifeAmount(params.get("ConditionLifeAmount"));
            }
        }

        if (params.containsKey("ConditionManaSpent")) {
            this.setManaSpent(params.get("ConditionManaSpent"));
        }

        if (params.containsKey("ConditionCheckSVar")) {
            this.setSvarToCheck(params.get("ConditionCheckSVar"));
        }
        if (params.containsKey("ConditionSVarCompare")) {
            this.setSvarOperator(params.get("ConditionSVarCompare").substring(0, 2));
            this.setSvarOperand(params.get("ConditionSVarCompare").substring(2));
        }

    } // setConditions

    /**
     * <p>
     * checkConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkConditions(final SpellAbility sa) {

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = sa.getSourceCard().getController();
            System.out.println(sa.getSourceCard().getName()
                    + " Did not have activator set in SpellAbility_Condition.checkConditions()");
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
        if (this.isKicked()) {
            SpellAbility root = sa.getRootSpellAbility();
            if (!root.isKicked()) {
                return false;
            }
        }

        if (this.isSorcerySpeed() && !PhaseHandler.canCastSorcery(activator)) {
            return false;
        }

        if (this.isPlayerTurn() && !Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if ((this.getActivationLimit() != -1) && (this.getNumberTurnActivations() >= this.getActivationLimit())) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            boolean isPhase = false;
            final PhaseType currPhase = Singletons.getModel().getGameState().getPhaseHandler().getPhase();
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

        if (this.isAllM12Empires()) {
            final Player p = sa.getSourceCard().getController();
            boolean has = p.isCardInPlay("Crown of Empires");
            has &= p.isCardInPlay("Scepter of Empires");
            has &= p.isCardInPlay("Throne of Empires");
            if (!has) {
                return false;
            }
        }
        if (this.isNotAllM12Empires()) {
            final Player p = sa.getSourceCard().getController();
            boolean has = p.isCardInPlay("Crown of Empires");
            has &= p.isCardInPlay("Scepter of Empires");
            has &= p.isCardInPlay("Throne of Empires");
            if (has) {
                return false;
            }
        }

        if (this.getCardsInHand() != -1) {
            // Can handle Library of Alexandria, or Hellbent
            if (activator.getCardsIn(ZoneType.Hand).size() != this.getCardsInHand()) {
                return false;
            }
        }

        if (this.getIsPresent() != null) {
            List<Card> list = new ArrayList<Card>();
            if (this.getPresentDefined() != null) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), this.getPresentDefined(), sa));
            } else {
                list = Singletons.getModel().getGameState().getCardsIn(ZoneType.Battlefield);
            }

            list = CardLists.getValidCards(list, this.getIsPresent().split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            int right;
            final String rightString = this.getPresentCompare().substring(2);
            try { // If this is an Integer, just parse it
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException e) { // Otherwise, grab it from
                                                      // the
                // SVar
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
            }

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
            if (this.getLifeTotal().equals("Opponent")) {
                life = activator.getOpponent().getLife();
            }

            int right = 1;
            final String rightString = this.getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
            } else {
                right = Integer.parseInt(this.getLifeAmount().substring(2));
            }

            if (!Expressions.compare(life, this.getLifeAmount(), right)) {
                return false;
            }
        }

        if (null != this.getManaSpent()) {
            if (!sa.getSourceCard().getColorsPaid().contains(this.getManaSpent())) {
                return false;
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityFactory.calculateAmount(sa.getSourceCard(), this.getsVarToCheck(), sa);
            final int operandValue = AbilityFactory.calculateAmount(sa.getSourceCard(), this.getsVarOperand(), sa);

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }

        }

        return true;
    }

} // end class SpellAbility_Condition
