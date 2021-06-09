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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.card.ColorSet;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GameObjectPredicates;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.collect.FCollection;

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
    public final void setConditions(final Map<String, String> params) {
        if (params.containsKey("Condition")) {
            final String value = params.get("Condition");
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
            if (value.equals("Revolt")) {
                this.setRevolt(true);
            }
            if (value.equals("Desert")) {
                this.setDesert(true);
            }
            if (value.equals("Blessing")) {
                this.setBlessing(true);
            }
            if (value.equals("Kicked")) {
                this.kicked = true;
            }
            if (value.equals("Kicked 1")) {
                this.kicked1 = true;
            }
            if (value.equals("Kicked 2")) {
                this.kicked2 = true;
            }
            if (value.equals("Surge")) {
                this.surgeCostPaid = true;
            }
            if (value.equals("AllTargetsLegal")) {
                this.setAllTargetsLegal(true);
            }
            if (value.equals("AltCost"))
                this.altCostPaid = true;

            if (value.equals("OptionalCost")) {
                this.optionalCostPaid = true;
            }

            if (value.equals("Foretold")) {
                this.foretold = true;
            }

            if (params.containsKey("ConditionOptionalPaid")) {
                this.optionalBoolean = Boolean.parseBoolean(params.get("ConditionOptionalPaid"));
            }
        }

        if (params.containsKey("ConditionZone")) {
            this.setZone(ZoneType.smartValueOf(params.get("ConditionZone")));
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

        if (params.containsKey("ConditionGameTypes")) {
            this.setGameTypes(GameType.listValueOf(params.get("ConditionGameTypes")));
        }

        if (params.containsKey("ConditionChosenColor")) {
            this.setColorToCheck(params.get("ConditionChosenColor"));
        }

        if (params.containsKey("Presence")) {
            this.setPresenceCondition(params.get("Presence"));
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

        if (params.containsKey("ConditionPlayerDefined")) {
            this.setPlayerDefined(params.get("ConditionPlayerDefined"));
        }

        if (params.containsKey("ConditionPlayerContains")) {
            this.setPlayerContains(params.get("ConditionPlayerContains"));
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

        if (params.containsKey("ConditionShareAllColors")) {
            this.setShareAllColors(params.get("ConditionShareAllColors"));
        }

        if (params.containsKey("ConditionManaSpent")) {
            this.setManaSpent(params.get("ConditionManaSpent"));
        }

        if (params.containsKey("ConditionManaNotSpent")) {
            this.setManaNotSpent(params.get("ConditionManaNotSpent"));
        }

        if (params.containsKey("ConditionCheckSVar")) {
            this.setSvarToCheck(params.get("ConditionCheckSVar"));
        }
        if (params.containsKey("ConditionSVarCompare")) {
            this.setSvarOperator(params.get("ConditionSVarCompare").substring(0, 2));
            this.setSvarOperand(params.get("ConditionSVarCompare").substring(2));
        }
        if (params.containsKey("OrOtherConditionSVarCompare")) {
            //unless another SVar is specified, check against the same one
            if (params.containsKey("OrConditionCheckSVar")) {
                this.setSvarToCheck2(params.get("OrConditionCheckSVar"));
            } else {
                this.setSvarToCheck2(params.get("ConditionCheckSVar"));
            }
            this.setSvarOperator2(params.get("OrOtherConditionSVarCompare").substring(0, 2));
            this.setSvarOperand2(params.get("OrOtherConditionSVarCompare").substring(2));
        }
        if (params.containsKey("ConditionTargetValidTargeting")) {
            this.setTargetValidTargeting(params.get("ConditionTargetValidTargeting"));
        }
        if (params.containsKey("ConditionTargetsSingleTarget")) {
            this.setTargetsSingleTarget(true);
        }

    } // setConditions

    /**
     * <p>
     * checkConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean areMet(final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = sa.getHostCard().getController();
            System.out.println(sa.getHostCard().getName()
                    + " Did not have activator set in SpellAbility_Condition.checkConditions()");
        }
        final Game game = activator.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final Card host = sa.getHostCard();

        if (this.isHellbent() && !activator.hasHellbent()) return false;
        if (this.isThreshold() && !activator.hasThreshold()) return false;
        if (this.isMetalcraft() && !activator.hasMetalcraft()) return false;
        if (this.isDelirium() && !activator.hasDelirium()) return false;
        if (this.isRevolt() && !activator.hasRevolt()) return false;
        if (this.isDesert() && !activator.hasDesert()) return false;
        if (this.isBlessing() && !activator.hasBlessing()) return false;
        
        if (this.kicked && !sa.isKicked()) return false;
        if (this.kicked1 && !sa.isOptionalCostPaid(OptionalCost.Kicker1)) return false;
        if (this.kicked2 && !sa.isOptionalCostPaid(OptionalCost.Kicker2)) return false;
        if (this.altCostPaid && !sa.isOptionalCostPaid(OptionalCost.AltCost)) return false;
        if (this.surgeCostPaid && !sa.isSurged()) return false;
        if (this.foretold && !sa.isForetold()) return false;

        if (this.optionalCostPaid && this.optionalBoolean && !sa.isOptionalCostPaid(OptionalCost.Generic)) return false;
        if (this.optionalCostPaid && !this.optionalBoolean && sa.isOptionalCostPaid(OptionalCost.Generic)) return false;
        
        if (!this.getPresenceCondition().isEmpty()) {
            if (host.getCastFrom() == null || host.getCastSA() == null)
                return false;

            final String type = this.getPresenceCondition();

            int revealed = AbilityUtils.calculateAmount(host, "Revealed$Valid " + type, host.getCastSA());
            int ctrl = AbilityUtils.calculateAmount(host, "Count$Valid " + type + ".inZoneBattlefield+YouCtrl", host.getCastSA());

            if (revealed + ctrl == 0) {
                return false;
            }
        }

        if (this.isAllTargetsLegal()) {
            for (Card c : sa.getTargets().getTargetCards()) {
                if (!sa.canTarget(c)) {
                    return false;
                }
            }
        }

        if (this.getShareAllColors() != null) {
            List<Card> tgts = AbilityUtils.getDefinedCards(sa.getHostCard(), this.getShareAllColors(), sa);
            Card first = Iterables.getFirst(tgts, null);
            if (first == null) {
                return false;
            }
            byte firstColor = CardUtil.getColors(first).getColor();
            for (Card c : tgts) {
                if (CardUtil.getColors(c).getColor() != firstColor) {
                    return false;
                }
            }
        }

        if (this.isSorcerySpeed() && !activator.canCastSorcery()) {
            return false;
        }

        if (this.isPlayerTurn() && !phase.isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && !phase.getPlayerTurn().isOpponentOf(activator)) {
            return false;
        }

        if ((this.getActivationLimit() != -1) && (sa.getActivationsThisTurn() >= this.getActivationLimit())) {
            return false;
        }

        if ((this.getGameActivationLimit() != -1) && (sa.getActivationsThisGame() >= this.getGameActivationLimit())) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            if (!this.getPhases().contains(phase.getPhase())) {
                return false;
            }
        }

        if (this.getGameTypes().size() > 0) {
            if (!getGameTypes().contains(game.getRules().getGameType())) {
                return false;
            }
        }

        if (this.getCardsInHand() != -1) {
            // Can handle Library of Alexandria, or Hellbent
            if (activator.getCardsIn(ZoneType.Hand).size() != this.getCardsInHand()) {
                return false;
            }
        }

        if (this.getColorToCheck() != null) {
            if (!sa.getHostCard().hasChosenColor(this.getColorToCheck())) {
                return false;
            }
        }

        if (getIsPresent() != null) {
            FCollection<GameObject> list;
            if (getPresentDefined() != null) {
                list = AbilityUtils.getDefinedObjects(host, getPresentDefined(), sa);
            } else {
                list = new FCollection<GameObject>(game.getCardsIn(getPresentZone()));
            }

            final int left = Iterables.size(Iterables.filter(list, GameObjectPredicates.restriction(getIsPresent().split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa)));

            final String rightString = this.getPresentCompare().substring(2);
            int right = AbilityUtils.calculateAmount(host, rightString, sa);

            if (!Expressions.compare(left, this.getPresentCompare(), right)) {
                return false;
            }
        }

        if (this.getPlayerContains() != null) {
            List<Player> list = new ArrayList<>();
            if (this.getPlayerDefined() != null) {
                list.addAll(AbilityUtils.getDefinedPlayers(sa.getHostCard(), this.getPlayerDefined(), sa));
            }
            List<Player> contains = AbilityUtils.getDefinedPlayers(sa.getHostCard(), this.getPlayerContains(), sa);
            if (contains.isEmpty() || !list.containsAll(contains)) {
                return false;
            }
        }

        if (this.getLifeTotal() != null) {
            int life = 1;
            if (this.getLifeTotal().equals("OpponentSmallest")) {
                life = activator.getOpponentsSmallestLifeTotal();
            }
            else {
                life = AbilityUtils.getDefinedPlayers(host, this.getLifeTotal(), sa).getFirst().getLife();
            }

            int right = 1;
            final String rightString = this.getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = AbilityUtils.calculateAmount(host, host.getSVar("X"), sa);
            } else {
                right = Integer.parseInt(this.getLifeAmount().substring(2));
            }

            if (!Expressions.compare(life, this.getLifeAmount(), right)) {
                return false;
            }
        }
        if (this.getTargetValidTargeting() != null) {
            final TargetChoices matchTgt = sa.getTargets();
            if (matchTgt == null || matchTgt.getFirstTargetedSpell() == null
            		|| matchTgt.getFirstTargetedSpell().getTargets() == null) {
                return false;
            }

            boolean result = false;
    
            for (final GameObject o : matchTgt.getFirstTargetedSpell().getTargets()) {
                if (o.isValid(this.getTargetValidTargeting().split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa)) {
                    result = true;
                    break;
                }
            }
    
            if (!result) {
                return false;
            }
        }
        if (this.targetsSingleTarget()) {
            final TargetChoices matchTgt = sa.getTargets();
            if (matchTgt == null || matchTgt.getFirstTargetedSpell() == null
            		|| matchTgt.getFirstTargetedSpell().getTargets() == null) {
                return false;

            }

            Set<GameObject> targets = new HashSet<>();
            for (TargetChoices tc : sa.getAllTargetChoices()) {
                targets.addAll(tc);
                if (targets.size() > 1) {
                    return false;
                }
            }
            if (targets.size() != 1) {
                return false;
            }
        }

        if (StringUtils.isNotEmpty(getManaSpent())) {
            SpellAbility castSa = sa.getHostCard().getCastSA();
            if (castSa == null) {
                return false;
            }
            if (!castSa.getPayingColors().hasAllColors(ColorSet.fromNames(getManaSpent().split(" ")).getColor())) {
                return false;
            }
        }
        if (StringUtils.isNotEmpty(getManaNotSpent())) {
            SpellAbility castSa = sa.getHostCard().getCastSA();
            if (castSa != null && castSa.getPayingColors().hasAllColors(ColorSet.fromNames(getManaNotSpent().split(" ")).getColor())) {
                return false;
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityUtils.calculateAmount(host, this.getsVarToCheck(), sa);
            final int operandValue = AbilityUtils.calculateAmount(host, this.getsVarOperand(), sa);
            boolean secondCheck = false;
            if (this.getsVarToCheck2() != null) {
                final int svarValue2 = AbilityUtils.calculateAmount(host, this.getsVarToCheck2(), sa);
                final int operandValue2 = AbilityUtils.calculateAmount(host, this.getsVarOperand2(), sa);
                if (Expressions.compare(svarValue2, this.getsVarOperator2(), operandValue2)) {
                    secondCheck = true;
                }
            }

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue) && !secondCheck) {
                return false;
            }

        }

        return true;
    }

} // end class SpellAbility_Condition
