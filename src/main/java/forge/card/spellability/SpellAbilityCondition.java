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

import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.CardList;
import forge.Constant.Zone;
import forge.Phase;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;

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
        }

        if (params.containsKey("ConditionZone")) {
            this.setZone(Zone.smartValueOf(params.get("ContitionZone")));
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
            String phases = params.get("ConditionPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                final String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }

            this.setPhases(phases);
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

        if (this.isSorcerySpeed() && !Phase.canCastSorcery(activator)) {
            return false;
        }

        if (this.isPlayerTurn() && !AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if ((this.getActivationLimit() != -1) && (this.getNumberTurnActivations() >= this.getActivationLimit())) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            boolean isPhase = false;
            final String currPhase = AllZone.getPhase().getPhase();
            for (final String s : this.getPhases()) {
                if (s.equals(currPhase)) {
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
            boolean has = AllZoneUtil.isCardInPlay("Crown of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Scepter of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Throne of Empires", p);
            if (!has) {
                return false;
            }
        }
        if (this.isNotAllM12Empires()) {
            final Player p = sa.getSourceCard().getController();
            boolean has = AllZoneUtil.isCardInPlay("Crown of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Scepter of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Throne of Empires", p);
            if (has) {
                return false;
            }
        }

        if (this.getCardsInHand() != -1) {
            // Can handle Library of Alexandria, or Hellbent
            if (activator.getCardsIn(Zone.Hand).size() != this.getCardsInHand()) {
                return false;
            }
        }

        if (this.getIsPresent() != null) {
            CardList list = new CardList();
            if (this.getPresentDefined() != null) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), this.getPresentDefined(), sa).toArray());
            } else {
                list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            }

            list = list.getValidCards(this.getIsPresent().split(","), sa.getActivatingPlayer(), sa.getSourceCard());

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

            if (!AllZoneUtil.compare(left, this.getPresentCompare(), right)) {
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

            if (!AllZoneUtil.compare(life, this.getLifeAmount(), right)) {
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

            if (!AllZoneUtil.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }

        }

        return true;
    }

} // end class SpellAbility_Condition
