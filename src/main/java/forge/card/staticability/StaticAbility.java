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
package forge.card.staticability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.GameEntity;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility.
 */
public class StaticAbility {

    private Card hostCard = null;

    private HashMap<String, String> mapParams = new HashMap<String, String>();

    private int layer = 0;

    /** The temporarily suppressed. */
    private boolean temporarilySuppressed = false;

    /** The suppressed. */
    private final boolean suppressed = false;

    /**
     * <p>
     * getHostCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostCard;
    }

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams() {
        return this.mapParams;
    }

    // *******************************************************

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams(final String abString, final Card hostCard) {
        final HashMap<String, String> mapParameters = new HashMap<String, String>();

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

    // In which layer should the ability be applied (for continuous effects
    // only)
    /**
     * Gets the layer.
     * 
     * @return the layer
     */
    public final int generateLayer() {

        if (!this.mapParams.get("Mode").equals("Continuous")) {
            return 0;
        }

        if (this.mapParams.containsKey("AddType") || this.mapParams.containsKey("RemoveType")
                || this.mapParams.containsKey("RemoveCardType") || this.mapParams.containsKey("RemoveSubType")
                || this.mapParams.containsKey("RemoveSuperType")) {
            return 4;
        }

        if (this.mapParams.containsKey("AddColor") || this.mapParams.containsKey("RemoveColor")
                || this.mapParams.containsKey("SetColor")) {
            return 5;
        }

        if (this.mapParams.containsKey("RemoveAllAbilities") || this.mapParams.containsKey("GainsAbilitiesOf")) {
            return 6; // Layer 6
        }

        if (this.mapParams.containsKey("AddKeyword") || this.mapParams.containsKey("AddAbility")
                || this.mapParams.containsKey("AddTrigger") || this.mapParams.containsKey("RemoveTriggers")
                || this.mapParams.containsKey("RemoveKeyword")) {
            return 7; // Layer 6 (dependent)
        }

        if (this.mapParams.containsKey("CharacteristicDefining")) {
            return 8; // Layer 7a
        }

        if (this.mapParams.containsKey("AddPower") || this.mapParams.containsKey("AddToughness")
                || this.mapParams.containsKey("SetPower") || this.mapParams.containsKey("SetToughness")) {
            return 9; // This is the collection of 7b and 7c
        }

        return 10; // rules change
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
        this.mapParams = this.getMapParams(params, host);
        this.hostCard = host;
        this.layer = this.generateLayer();
    }

    /**
     * Instantiates a new static ability.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     */
    public StaticAbility(final HashMap<String, String> params, final Card host) {
        this.mapParams = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            this.mapParams.put(entry.getKey(), entry.getValue());
        }
        this.layer = this.generateLayer();
        this.hostCard = host;
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     */
    public final void applyAbility(final String mode) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return;
        }

        if (mode.equals("Continuous")) {
            StaticAbilityContinuous.applyContinuousAbility(this);
        }
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
     * @param b
     *            the b
     * @return the int
     */
    public final int applyAbility(final String mode, final Card source, final GameEntity target, final int in,
            final boolean b) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return in;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return in;
        }

        if (mode.equals("PreventDamage")) {
            return StaticAbilityPreventDamage.applyPreventDamageAbility(this, source, target, in, b);
        }

        return in;
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final Player activator) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeCast")) {
            return StaticAbilityCantBeCast.applyCantBeCastAbility(this, card, activator);
        }

        if (mode.equals("CantPlayLand")) {
            return StaticAbilityCantBeCast.applyCantPlayLandAbility(this, card, activator);
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
     * @param sa
     *            the SpellAbility
     * @param originalCost
     *            the originalCost
     * @return the modified ManaCost
     */
    public final ManaCost applyAbility(final String mode, final SpellAbility sa, final ManaCost originalCost) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return originalCost;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return originalCost;
        }

        if (mode.equals("RaiseCost")) {
            return StaticAbilityCostChange.applyRaiseCostAbility(this, sa, originalCost);
        }
        if (mode.equals("ReduceCost")) {
            return StaticAbilityCostChange.applyReduceCostAbility(this, sa, originalCost);
        }
        if (mode.equals("SetCost")) { //Set cost is only used by Trinisphere
            return StaticAbilityCostChange.applyRaiseCostAbility(this, sa, originalCost);
        }

        return originalCost;
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
    public final Cost getCostAbility(final String mode, final Card card, final GameEntity target) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return null;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return null;
        }

        if (mode.equals("CantAttackUnless")) {
            return StaticAbilityCantAttackBlock.applyCantAttackUnlessAbility(this, card, target);
        }

        if (mode.equals("CantBlockUnless")) {
            return StaticAbilityCantAttackBlock.applyCantBlockUnlessAbility(this, card);
        }

        return null;
    }

    /**
     * Check conditions.
     * 
     * @return true, if successful
     */
    public final boolean checkConditions() {
        final Player controller = this.hostCard.getController();

        if (this.hostCard.isPhasedOut()) {
            return false;
        }

        if (this.mapParams.containsKey("EffectZone")) {
            if (!this.mapParams.get("EffectZone").equals("All")
                    && !ZoneType.listValueOf(this.mapParams.get("EffectZone"))
                        .contains(AllZone.getZoneOf(this.hostCard).getZoneType())) {
                return false;
            }
        } else {
            if (!this.hostCard.isInZone(ZoneType.Battlefield)) { // default
                return false;
            }
        }

        if (this.mapParams.containsKey("FatefulHour") && controller.getLife() > 5) {
            return false;
        }

        if (this.mapParams.containsKey("Threshold") && !controller.hasThreshold()) {
            return false;
        }

        if (this.mapParams.containsKey("Hellbent") && !controller.hasHellbent()) {
            return false;
        }

        if (this.mapParams.containsKey("Metalcraft") && !controller.hasMetalcraft()) {
            return false;
        }

        if (this.mapParams.containsKey("PlayerTurn") && !Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(controller)) {
            return false;
        }

        if (this.mapParams.containsKey("OpponentTurn") && !Singletons.getModel().getGameState().getPhaseHandler().isPlayerTurn(controller.getOpponent())) {
            return false;
        }

        if (this.mapParams.containsKey("OpponentAttackedWithCreatureThisTurn")
                && !controller.getOpponent().getAttackedWithCreatureThisTurn()) {
            return false;
        }

        if (this.mapParams.containsKey("Phases")) {
            List<PhaseType> phases = PhaseType.parseRange(this.mapParams.get("Phases"));
            if (!phases.contains(Singletons.getModel().getGameState().getPhaseHandler().getPhase())) {
                return false;
            }
        }

        if (this.mapParams.containsKey("TopCardOfLibraryIs")) {
            if (controller.getCardsIn(ZoneType.Library).isEmpty()) {
                return false;
            }
            final Card topCard = controller.getCardsIn(ZoneType.Library).get(0);
            if (!topCard.isValid(this.mapParams.get("TopCardOfLibraryIs").split(","), controller, this.hostCard)) {
                return false;
            }
        }

        /*
         * if(mapParams.containsKey("isPresent")) { String isPresent =
         * mapParams.get("isPresent"); CardList list =
         * AllZoneUtil.getCardsInPlay();
         * 
         * list = list.getValidCards(isPresent.split(","), controller,
         * hostCard);
         * 
         * }
         */

        if (this.mapParams.containsKey("CheckSVar")) {
            final int sVar = AbilityFactory.calculateAmount(this.hostCard, this.mapParams.get("CheckSVar"), null);
            String comparator = "GE1";
            if (this.mapParams.containsKey("SVarCompare")) {
                comparator = this.mapParams.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(this.hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("CheckSecondSVar")) {
            final int sVar = AbilityFactory.calculateAmount(this.hostCard, this.mapParams.get("CheckSecondSVar"), null);
            String comparator = "GE1";
            if (this.mapParams.containsKey("SecondSVarCompare")) {
                comparator = this.mapParams.get("SecondSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(this.hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("CheckThirdSVar")) {
            final int sVar = AbilityFactory.calculateAmount(this.hostCard, this.mapParams.get("CheckThirdSVar"), null);
            String comparator = "GE1";
            if (this.mapParams.containsKey("ThirdSVarCompare")) {
                comparator = this.mapParams.get("ThirdSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(this.hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (this.mapParams.containsKey("CheckFourthSVar")) {
            final int sVar = AbilityFactory.calculateAmount(this.hostCard, this.mapParams.get("CheckFourthSVar"), null);
            String comparator = "GE1";
            if (this.mapParams.containsKey("FourthSVarCompare")) {
                comparator = this.mapParams.get("FourthSVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(this.hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sets the temporarily suppressed.
     * 
     * @param supp
     *            the new temporarily suppressed
     */
    public final void setTemporarilySuppressed(final boolean supp) {
        this.temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (this.suppressed || this.temporarilySuppressed);
    }

    /**
     * @return the layer
     */
    public int getLayer() {
        return layer;
    }

    /**
     * @param layer the layer to set
     */
    public void setLayer(int layer) {
        this.layer = layer;
    }

} // end class StaticEffectFactory
