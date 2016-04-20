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
package forge.game.staticability;

import forge.card.MagicColor;
import forge.game.CardTraitBase;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

/**
 * The Class StaticAbility.
 */
public class StaticAbility extends CardTraitBase {

    private final Set<StaticAbilityLayer> layers;
    private CardCollectionView ignoreEffectCards = new CardCollection();
    private final List<Player> ignoreEffectPlayers = new ArrayList<Player>();

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static Map<String, String> parseParams(final String abString, final Card hostCard) {
        final Map<String, String> mapParameters = Maps.newHashMap();

        if (!(abString.length() > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in "
                    + hostCard.getName() + ": [" + abString + "]");
        }

        final String[] a = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++) {
            a[aCnt] = a[aCnt].trim();
        }

        if (!(a.length > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- a[] too short in " + hostCard.getName());
        }

        for (final String element : a) {
            final String[] aa = element.split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(element).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }

    /**
     * Gets the {@link Set} of {@link StaticAbilityLayer}s in which this
     * {@link StaticAbility} is to be applied.
     * 
     * @return the applicable layers.
     */
    private final Set<StaticAbilityLayer> generateLayer() {
        if (!this.mapParams.get("Mode").equals("Continuous")) {
            return EnumSet.noneOf(StaticAbilityLayer.class);
        }

        final Set<StaticAbilityLayer> layers = EnumSet.noneOf(StaticAbilityLayer.class);
        if (this.mapParams.containsKey("GainControl")) {
            layers.add(StaticAbilityLayer.CONTROL);
        }

        if (this.mapParams.containsKey("ChangeColorWordsTo")) {
            layers.add(StaticAbilityLayer.TEXT);
        }

        if (this.mapParams.containsKey("AddType") || this.mapParams.containsKey("RemoveType")
                || this.mapParams.containsKey("RemoveCardTypes") || this.mapParams.containsKey("RemoveSubTypes")
                || this.mapParams.containsKey("RemoveSuperTypes") || this.mapParams.containsKey("RemoveCreatureTypes")) {
            layers.add(StaticAbilityLayer.TYPE);
        }

        if (this.mapParams.containsKey("AddColor") || this.mapParams.containsKey("RemoveColor")
                || this.mapParams.containsKey("SetColor")) {
            layers.add(StaticAbilityLayer.COLOR);
        }

        if (this.mapParams.containsKey("RemoveAllAbilities") || this.mapParams.containsKey("GainsAbilitiesOf")) {
            layers.add(StaticAbilityLayer.ABILITIES1);
        }

        if (this.mapParams.containsKey("AddKeyword") || this.mapParams.containsKey("AddAbility")
                || this.mapParams.containsKey("AddTrigger") || this.mapParams.containsKey("RemoveTriggers")
                || this.mapParams.containsKey("RemoveKeyword") || this.mapParams.containsKey("AddReplacementEffects")
                || this.mapParams.containsKey("AddSVar")) {
            layers.add(StaticAbilityLayer.ABILITIES2);
        }

        if (this.mapParams.containsKey("CharacteristicDefining")) {
            layers.add(StaticAbilityLayer.CHARACTERISTIC);
        }

        if (this.mapParams.containsKey("SetPower") || this.mapParams.containsKey("SetToughness")) {
            layers.add(StaticAbilityLayer.SETPT);
        }
        if (this.mapParams.containsKey("AddPower") || this.mapParams.containsKey("AddToughness")) {
            layers.add(StaticAbilityLayer.MODIFYPT);
        }

        if (layers.isEmpty() || this.mapParams.containsKey("AddHiddenKeyword")) {
        	layers.add(StaticAbilityLayer.RULES);
        }

        return layers;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String toString() {
        if (this.mapParams.containsKey("Description") && !this.isSuppressed()) {
            return this.mapParams.get("Description").replace("CARDNAME", this.hostCard.getName());
        } else {
            return "";
        }
    }

    // main constructor
    /**
     * Instantiates a new static ability.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     */
    public StaticAbility(final String params, final Card host) {
        this(parseParams(params, host), host);
    }

    /**
     * Instantiates a new static ability.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     */
    private StaticAbility(final Map<String, String> params, final Card host) {
        this.originalMapParams.putAll(params);
        this.mapParams.putAll(params);
        this.layers = this.generateLayer();
        this.hostCard = host;
    }

    public final CardCollectionView applyContinuousAbility(final StaticAbilityLayer layer) {
        if (!shouldApplyContinuousAbility(layer, false)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, layer);
    }

    public final CardCollectionView applyContinuousAbility(final StaticAbilityLayer layer, final CardCollectionView affected) {
        if (!shouldApplyContinuousAbility(layer, true)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, affected, layer);
    }

    /**
     * Check whether a continuous ability should be applied.
     * 
     * @param layer
     *            the {@link StaticAbilityLayer} under investigation.
     * @param ignoreTempSuppression
     *            whether to ignore temporary suppression of this ability, to be
     *            used when this ability has already begun applying in another
     *            layer and has since been removed from its host card by another
     *            effect (see rule 613.5).
     * @return {@code true} if and only if this is a continuous ability that
     *         affects the specified layer, it's not suppressed, and its
     *         conditions are fulfilled.
     */
    private boolean shouldApplyContinuousAbility(final StaticAbilityLayer layer, final boolean ignoreTempSuppression) {
        final boolean isSuppressed;
        if (ignoreTempSuppression) {
            isSuppressed = this.isNonTempSuppressed();
        } else {
            isSuppressed = this.isSuppressed();
        }
        return mapParams.get("Mode").equals("Continuous") && layers.contains(layer) && !isSuppressed && this.checkConditions();
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param source
     *            the source
     * @param target
     *            the target
     * @param in
     *            the in
     * @param isCombat
     *            the b
     * @return the int
     */
    public final int applyAbility(final String mode, final Card source, final GameEntity target, final int in,
            final boolean isCombat, final boolean isTest) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return in;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return in;
        }

        if (mode.equals("PreventDamage")) {
            return StaticAbilityPreventDamage.applyPreventDamageAbility(this, source, target, in, isCombat, isTest);
        }

        return in;
    }

    /**
     * Apply ability if it has the right mode.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param player
     *            the activator
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final Player player) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkPlayerSpecificConditions(player)) {
            return false;
        }

        if (mode.equals("CantBeCast")) {
            return StaticAbilityCantBeCast.applyCantBeCastAbility(this, card, player);
        }

        if (mode.equals("CantPlayLand")) {
            return StaticAbilityCantBeCast.applyCantPlayLandAbility(this, card, player);
        }

        return false;
    }

    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param spellAbility
     *            the ability
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final SpellAbility spellAbility) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeActivated")) {
            return StaticAbilityCantBeCast.applyCantBeActivatedAbility(this, card, spellAbility);
        }

        if (mode.equals("CantTarget")) {
            return StaticAbilityCantTarget.applyCantTargetAbility(this, card, spellAbility);
        }

        return false;
    }


    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("ETBTapped")) {
            return StaticAbilityETBTapped.applyETBTappedAbility(this, card);
        }

        if (mode.equals("GainAbilitiesOf")) {

        }

        return false;
    }

    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param target
     *            the target
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final GameEntity target) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantAttack")) {
            return StaticAbilityCantAttackBlock.applyCantAttackAbility(this, card, target);
        }

        return false;
    }

    public final Cost getAttackCost(final Card attacker, final GameEntity target) {
        if (this.isSuppressed() || !mapParams.get("Mode").equals("CantAttackUnless") || !this.checkConditions()) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getAttackCost(this, attacker, target);
    }

    public final Cost getBlockCost(final Card blocker, final Card attacker) {
        if (this.isSuppressed() || !mapParams.get("Mode").equals("CantBlockUnless") || !this.checkConditions()) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getBlockCost(this, blocker, attacker);
    }

    /**
     * Check conditions for static abilities acting on a specific player. Also
     * automatically check the general conditions.
     * 
     * @param player a {@link Player}.
     * @return true, if the static ability is applicable.
     * @see {@link StaticAbility#checkConditions()}
     */
    public final boolean checkPlayerSpecificConditions(final Player player) {
        if (!checkConditions()) {
            return false;
        }

        if (this.mapParams.containsKey("PlayerAttackedWithCreatureThisTurn")
                && !player.getAttackedWithCreatureThisTurn()) {
            return false;
        }
        
        return true;
    }

    /**
     * Check conditions.
     * 
     * @return true, if the static ability is applicable.
     */
    public final boolean checkConditions() {
        final Player controller = this.hostCard.getController();

        if (this.hostCard.isPhasedOut()) {
            return false;
        }

        if (this.mapParams.containsKey("EffectZone")) {
            if (!this.mapParams.get("EffectZone").equals("All")
                    && !ZoneType.listValueOf(this.mapParams.get("EffectZone"))
                        .contains(controller.getGame().getZoneOf(this.hostCard).getZoneType())) {
                return false;
            }
        } else {
            if (!this.hostCard.isInZone(ZoneType.Battlefield)) { // default
                return false;
            }
        }

        String condition = mapParams.get("Condition");
        if (null != condition) {
            if (condition.equals("Threshold") && !controller.hasThreshold()) return false;
            if (condition.equals("Hellbent") && !controller.hasHellbent()) return false;
            if (condition.equals("Metalcraft") && !controller.hasMetalcraft()) return false;
            if (condition.equals("Delirium") && !controller.hasDelirium()) return false;

            if (condition.equals("PlayerTurn")) {
                if (!controller.getGame().getPhaseHandler().isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("NotPlayerTurn")) {
                if (controller.getGame().getPhaseHandler().isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("PermanentOfEachColor")) {
                if ((controller.getColoredCardsInPlay(MagicColor.Constant.BLACK).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.BLUE).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.GREEN).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.RED).isEmpty()
                        || controller.getColoredCardsInPlay(MagicColor.Constant.WHITE).isEmpty())) {
                    return false;
                }
            } else if (condition.equals("FatefulHour")) {
                if (controller.getLife() > 5) {
                    return false;
                }
            }
        }

        if (this.mapParams.containsKey("Phases")) {
            List<PhaseType> phases = PhaseType.parseRange(this.mapParams.get("Phases"));
            if (!phases.contains(controller.getGame().getPhaseHandler().getPhase())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("TopCardOfLibraryIs")) {
            if (controller.getCardsIn(ZoneType.Library).isEmpty()) {
                return false;
            }
            final Card topCard = controller.getCardsIn(ZoneType.Library).get(0);
            if (!topCard.isValid(this.mapParams.get("TopCardOfLibraryIs").split(","), controller, this.hostCard, null)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, this.mapParams.get("CheckSVar"), this);
            String comparator = "GE1";
            if (this.mapParams.containsKey("SVarCompare")) {
                comparator = this.mapParams.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (this.mapParams.containsKey("CheckSecondSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, this.mapParams.get("CheckSecondSVar"), this);
            String comparator = "GE1";
            if (this.mapParams.containsKey("SecondSVarCompare")) {
                comparator = this.mapParams.get("SecondSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (this.mapParams.containsKey("CheckThirdSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, this.mapParams.get("CheckThirdSVar"), this);
            String comparator = "GE1";
            if (this.mapParams.containsKey("ThirdSVarCompare")) {
                comparator = this.mapParams.get("ThirdSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (this.mapParams.containsKey("CheckFourthSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, this.mapParams.get("CheckFourthSVar"), this);
            String comparator = "GE1";
            if (this.mapParams.containsKey("FourthSVarCompare")) {
                comparator = this.mapParams.get("FourthSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return the ignoreEffectCards
     */
    public CardCollectionView getIgnoreEffectCards() {
        return ignoreEffectCards;
    }

    /**
     * @param cards the ignoreEffectCards to set
     */
    public void setIgnoreEffectCards(final CardCollectionView cards) {
        ignoreEffectCards = cards;
    }

    /**
     * @return the ignoreEffectPlayers
     */
    public List<Player> getIgnoreEffectPlayers() {
        return ignoreEffectPlayers;
    }

    /**
     * @param p the ignoreEffectPlayers to add
     */
    public void addIgnoreEffectPlayers(final Player p) {
        ignoreEffectPlayers.add(p);
    }

    public void clearIgnoreEffects() {
        ignoreEffectPlayers.clear();
        ignoreEffectCards = new CardCollection();
    }

    /**
     * @return the layer
     */
    public Set<StaticAbilityLayer> getLayers() {
        return layers;
    }

} // end class StaticAbility
