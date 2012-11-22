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
import java.util.List;

import forge.game.phase.PhaseType;
import forge.game.zone.ZoneType;

/**
 * <p>
 * SpellAbilityVariables class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbilityVariables {
    // A class for handling SpellAbility Variables. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, Hellbent, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)

    /**
     * <p>
     * Constructor for SpellAbility_Variables.
     * </p>
     */
    public SpellAbilityVariables() {
    }

    /**
     * 
     * @param sav
     * SpellAbilityVariables
     */
    public void setVariables(SpellAbilityVariables sav) {
        this.zone = sav.getZone();
        this.phases = new ArrayList<PhaseType>(sav.getPhases());
        this.sorcerySpeed = sav.isSorcerySpeed();
        this.instantSpeed = sav.isInstantSpeed();
        this.anyPlayer = sav.isAnyPlayer();
        this.setOpponentOnly(sav.isAnyPlayer());
        this.opponentTurn = sav.isOpponentTurn();
        this.playerTurn = sav.isPlayerTurn();
        this.activationLimit = sav.getActivationLimit();
        this.numberTurnActivations = sav.getNumberTurnActivations();
        this.activationNumberSacrifice = sav.getActivationNumberSacrifice();
        this.cardsInHand = sav.getCardsInHand();
        this.threshold = sav.isThreshold();
        this.metalcraft = sav.isThreshold();
        this.hellbent = sav.isHellbent();
        this.prowl = new ArrayList<String>(sav.getProwl());
        this.isPresent = sav.getIsPresent();
        this.presentCompare = sav.getPresentCompare();
        this.presentDefined = sav.getPresentDefined();
        this.presentZone = sav.getPresentZone();
        this.sVarToCheck = sav.getsVarToCheck();
        this.sVarOperator = sav.getsVarOperator();
        this.sVarOperand = sav.getsVarOperand();
        this.lifeTotal = sav.getLifeTotal();
        this.lifeAmount = sav.getLifeAmount();
        this.manaSpent = sav.getManaSpent();
        this.pwAbility = sav.isPwAbility();
        this.allM12Empires = sav.isAllM12Empires();
        this.notAllM12Empires = sav.isNotAllM12Empires();
    }

    // default values for Sorcery speed abilities
    /** The zone. */
    private ZoneType zone = ZoneType.Battlefield;

    /** The phases. */
    private List<PhaseType> phases = new ArrayList<PhaseType>();

    /** The b sorcery speed. */
    private boolean sorcerySpeed = false;

    /** The b instant speed. */
    private boolean instantSpeed = false;

    /** The b any player. */
    private boolean anyPlayer = false;

    /** The b any player. */
    private boolean opponentOnly = false;

    /** The b opponent turn. */
    private boolean opponentTurn = false;

    /** The b player turn. */
    private boolean playerTurn = false;

    /** The activation limit. */
    private int activationLimit = -1;

    /** The limitToCheck to check. */
    private String limitToCheck = null;

    /** The number turn activations. */
    private int numberTurnActivations = 0;

    /** The activation number sacrifice. */
    private int activationNumberSacrifice = -1;

    /** The n cards in hand. */
    private int cardsInHand = -1;

    /** The threshold. */
    private boolean threshold = false;

    /** The metalcraft. */
    private boolean metalcraft = false;

    /** The hellbent. */
    private boolean hellbent = false;

    /** The Kicked. */
    private boolean kicked = false;

    /** The prowl. */
    private ArrayList<String> prowl = new ArrayList<String>();

    /** The s is present. */
    private String isPresent = null;

    /** The present compare. */
    private String presentCompare = "GE1"; // Default Compare to Greater or
                                           // Equal to 1

    /** The present defined. */
    private String presentDefined = null;

    /** The present zone. */
    private ZoneType presentZone = ZoneType.Battlefield;

    /** The svar to check. */
    private String sVarToCheck = null;

    /** The svar operator. */
    private String sVarOperator = "GE";

    /** The svar operand. */
    private String sVarOperand = "1";

    /** The life total. */
    private String lifeTotal = null;

    /** The life amount. */
    private String lifeAmount = "GE1";

    /** The mana spent. */
    private String manaSpent = "";

    /** The pw ability. */
    private boolean pwAbility = false;

    /** The all m12 empires. */
    private boolean allM12Empires = false;

    /** The not all m12 empires. */
    private boolean notAllM12Empires = false;

    /**
     * <p>
     * Setter for the field <code>notAllM12Empires</code>.
     * </p>
     * 
     * @param b
     *            a boolean
     */
    public final void setNotAllM12Empires(final boolean b) {
        this.notAllM12Empires = b;
    }

    /**
     * <p>
     * Getter for the field <code>notAllM12Empires</code>.
     * </p>
     * 
     * @return a boolean
     */
    public final boolean getNotAllM12Empires() {
        return this.isNotAllM12Empires();
    }

    /**
     * <p>
     * Setter for the field <code>allM12Empires</code>.
     * </p>
     * 
     * @param b
     *            a boolean
     */
    public final void setAllM12Empires(final boolean b) {
        this.allM12Empires = b;
    }

    /**
     * <p>
     * Getter for the field <code>allM12Empires</code>.
     * </p>
     * 
     * @return a boolean
     */
    public final boolean getAllM12Empires() {
        return this.isAllM12Empires();
    }

    /**
     * <p>
     * Setter for the field <code>manaSpent</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setManaSpent(final String s) {
        this.manaSpent = s;
    }

    /**
     * <p>
     * Getter for the field <code>manaSpent</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getManaSpent() {
        return this.manaSpent;
    }

    /**
     * <p>
     * Setter for the field <code>zone</code>.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     */
    public final void setZone(final ZoneType zone) {
        this.zone = zone;
    }

    /**
     * <p>
     * Getter for the field <code>zone</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final ZoneType getZone() {
        return this.zone;
    }

    /**
     * <p>
     * setSorcerySpeed.
     * </p>
     * 
     * @param bSpeed
     *            a boolean.
     */
    public final void setSorcerySpeed(final boolean bSpeed) {
        this.sorcerySpeed = bSpeed;
    }

    /**
     * <p>
     * getSorcerySpeed.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSorcerySpeed() {
        return this.sorcerySpeed;
    }

    /**
     * <p>
     * setInstantSpeed.
     * </p>
     * 
     * @param bSpeed
     *            a boolean.
     */
    public final void setInstantSpeed(final boolean bSpeed) {
        this.instantSpeed = bSpeed;
    }

    /**
     * <p>
     * getInstantSpeed.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isInstantSpeed() {
        return this.instantSpeed;
    }

    /**
     * <p>
     * setAnyPlayer.
     * </p>
     * 
     * @param anyPlayer
     *            a boolean.
     */
    public final void setAnyPlayer(final boolean anyPlayer) {
        this.anyPlayer = anyPlayer;
    }

    /**
     * <p>
     * setPlayerTurn.
     * </p>
     * 
     * @param bTurn
     *            a boolean.
     */
    public final void setPlayerTurn(final boolean bTurn) {
        this.playerTurn = bTurn;
    }

    /**
     * <p>
     * getPlayerTurn.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getPlayerTurn() {
        return this.isPlayerTurn();
    }

    /**
     * <p>
     * setOpponentTurn.
     * </p>
     * 
     * @param bTurn
     *            a boolean.
     */
    public final void setOpponentTurn(final boolean bTurn) {
        this.opponentTurn = bTurn;
    }

    /**
     * <p>
     * getOpponentTurn.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getOpponentTurn() {
        return this.isOpponentTurn();
    }

    /**
     * <p>
     * Setter for the field <code>activationLimit</code>.
     * </p>
     * 
     * @param limit
     *            a int.
     */
    public final void setActivationLimit(final int limit) {
        this.activationLimit = limit;
    }

    /**
     * <p>
     * abilityActivated.
     * </p>
     */
    public final void abilityActivated() {
        this.numberTurnActivations++;
    }

    /**
     * <p>
     * Getter for the field <code>numberTurnActivations</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumberTurnActivations() {
        return this.numberTurnActivations;
    }

    /**
     * <p>
     * resetTurnActivations.
     * </p>
     */
    public final void resetTurnActivations() {
        this.numberTurnActivations = 0;
    }

    /**
     * <p>
     * Setter for the field <code>activationNumberSacrifice</code>.
     * </p>
     * 
     * @param num
     *            a int.
     */
    public final void setActivationNumberSacrifice(final int num) {
        this.activationNumberSacrifice = num;
    }

    /**
     * <p>
     * Getter for the field <code>activationNumberSacrifice</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getActivationNumberSacrifice() {
        return this.activationNumberSacrifice;
    }

    /**
     * <p>
     * Setter for the field <code>phases</code>.
     * </p>
     * 
     * @param phasesString
     *            a {@link java.lang.String} object.
     */
    public final void setPhases(final List<PhaseType> phases) {
        this.phases.addAll(phases);
    }

    /**
     * <p>
     * setActivateCardsInHand.
     * </p>
     * 
     * @param cards
     *            a int.
     */
    public final void setActivateCardsInHand(final int cards) {
        this.setCardsInHand(cards);
    }

    // specific named conditions
    /**
     * <p>
     * Setter for the field <code>hellbent</code>.
     * </p>
     * 
     * @param bHellbent
     *            a boolean.
     */
    public final void setHellbent(final boolean bHellbent) {
        this.hellbent = bHellbent;
    }

    /**
     * <p>
     * Setter for the field <code>threshold</code>.
     * </p>
     * 
     * @param bThreshold
     *            a boolean.
     */
    public final void setThreshold(final boolean bThreshold) {
        this.threshold = bThreshold;
    }

    /**
     * <p>
     * Setter for the field <code>metalcraft</code>.
     * </p>
     * 
     * @param bMetalcraft
     *            a boolean.
     */
    public final void setMetalcraft(final boolean bMetalcraft) {
        this.metalcraft = bMetalcraft;
    }

    /**
     * @return the kicked
     */
    public boolean isKicked() {
        return kicked;
    }

    /**
     * @param kicked0 the kicked to set
     */
    public void setKicked(boolean kicked) {
        this.kicked = kicked;
    }


    /**
     * <p>
     * Setter for the field <code>prowl</code>.
     * </p>
     * 
     * @param types
     *            the new prowl
     */
    public final void setProwl(final ArrayList<String> types) {
        this.prowl = types;
    }

    // IsPresent for Valid battlefield stuff

    /**
     * <p>
     * setIsPresent.
     * </p>
     * 
     * @param present
     *            a {@link java.lang.String} object.
     */
    public final void setIsPresent(final String present) {
        this.isPresent = present;
    }

    /**
     * <p>
     * Setter for the field <code>presentCompare</code>.
     * </p>
     * 
     * @param compare
     *            a {@link java.lang.String} object.
     */
    public final void setPresentCompare(final String compare) {
        this.presentCompare = compare;
    }

    /**
     * Gets the present zone.
     * 
     * @return the present zone
     */
    public final ZoneType getPresentZone() {
        return this.presentZone;
    }

    /**
     * Sets the present zone.
     * 
     * @param presentZone
     *            the new present zone
     */
    public final void setPresentZone(final ZoneType presentZone) {
        this.presentZone = presentZone;
    }

    /**
     * <p>
     * Setter for the field <code>presentDefined</code>.
     * </p>
     * 
     * @param defined
     *            a {@link java.lang.String} object.
     */
    public final void setPresentDefined(final String defined) {
        this.presentDefined = defined;
    }

    // used to define as a Planeswalker ability
    /**
     * <p>
     * setPlaneswalker.
     * </p>
     * 
     * @param bPlaneswalker
     *            a boolean.
     */
    public final void setPlaneswalker(final boolean bPlaneswalker) {
        this.setPwAbility(bPlaneswalker);
    }

    /**
     * <p>
     * getPlaneswalker.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getPlaneswalker() {
        return this.isPwAbility();
    }

    // Checking the values of SVars (Mostly for Traps)
    /**
     * <p>
     * Setter for the field <code>svarToCheck</code>.
     * </p>
     * 
     * @param sVar
     *            a {@link java.lang.String} object.
     */
    public final void setSvarToCheck(final String sVar) {
        this.setsVarToCheck(sVar);
    }

    /**
     * <p>
     * Setter for the field <code>svarOperator</code>.
     * </p>
     * 
     * @param operator
     *            a {@link java.lang.String} object.
     */
    public final void setSvarOperator(final String operator) {
        this.setsVarOperator(operator);
    }

    /**
     * <p>
     * Setter for the field <code>svarOperand</code>.
     * </p>
     * 
     * @param operand
     *            a {@link java.lang.String} object.
     */
    public final void setSvarOperand(final String operand) {
        this.setsVarOperand(operand);
    }

    /**
     * Gets the activation limit.
     * 
     * @return the activationLimit
     */
    public final int getActivationLimit() {
        return this.activationLimit;
    }

    /**
     * <p>
     * Setter for the field <code>limitToCheck</code>.
     * </p>
     * 
     * @param limit
     *            a {@link java.lang.String} object.
     */
    public final void setLimitToCheck(final String limit) {
        this.limitToCheck = limit;
    }

    /**
     * <p>
     * Getter for the field <code>limitToCheck</code>.
     * </p>
     * 
     * @return the limitToCheck
     *            a {@link java.lang.String} object.
     */
    public final String getLimitToCheck() {
        return this.limitToCheck;
    }

    /**
     * Checks if is threshold.
     * 
     * @return the threshold
     */
    public final boolean isThreshold() {
        return this.threshold;
    }

    /**
     * Checks if is metalcraft.
     * 
     * @return the metalcraft
     */
    public final boolean isMetalcraft() {
        return this.metalcraft;
    }

    /**
     * Checks if is hellbent.
     * 
     * @return the hellbent
     */
    public final boolean isHellbent() {
        return this.hellbent;
    }

    /**
     * Checks if is pw ability.
     * 
     * @return the pwAbility
     */
    public final boolean isPwAbility() {
        return this.pwAbility;
    }

    /**
     * Sets the pw ability.
     * 
     * @param pwAbility0
     *            the new pw ability
     */
    public final void setPwAbility(final boolean pwAbility0) {
        this.pwAbility = pwAbility0;
    }

    /**
     * Checks if is player turn.
     * 
     * @return the playerTurn
     */
    public final boolean isPlayerTurn() {
        return this.playerTurn;
    }

    /**
     * Gets the prowl.
     * 
     * @return the prowl
     */
    public final ArrayList<String> getProwl() {
        return this.prowl;
    }

    /**
     * Gets the present compare.
     * 
     * @return the presentCompare
     */
    public final String getPresentCompare() {
        return this.presentCompare;
    }

    /**
     * Gets the life total.
     * 
     * @return the lifeTotal
     */
    public final String getLifeTotal() {
        return this.lifeTotal;
    }

    /**
     * Sets the life total.
     * 
     * @param lifeTotal0
     *            the lifeTotal to set
     */
    public final void setLifeTotal(final String lifeTotal0) {
        this.lifeTotal = lifeTotal0;
    }

    /**
     * Gets the life amount.
     * 
     * @return the lifeAmount
     */
    public final String getLifeAmount() {
        return this.lifeAmount;
    }

    /**
     * Sets the life amount.
     * 
     * @param lifeAmount0
     *            the lifeAmount to set
     */
    public final void setLifeAmount(final String lifeAmount0) {
        this.lifeAmount = lifeAmount0;
    }

    /**
     * Gets the phases.
     * 
     * @return the phases
     */
    public final List<PhaseType> getPhases() {
        return this.phases;
    }


    /**
     * Gets the present defined.
     * 
     * @return the presentDefined
     */
    public final String getPresentDefined() {
        return this.presentDefined;
    }

    /**
     * Checks if is all m12 empires.
     * 
     * @return the allM12Empires
     */
    public final boolean isAllM12Empires() {
        return this.allM12Empires;
    }

    /**
     * Checks if is not all m12 empires.
     * 
     * @return the notAllM12Empires
     */
    public final boolean isNotAllM12Empires() {
        return this.notAllM12Empires;
    }

    /**
     * Gets the s var operand.
     * 
     * @return the sVarOperand
     */
    public final String getsVarOperand() {
        return this.sVarOperand;
    }

    /**
     * Sets the s var operand.
     * 
     * @param sVarOperand0
     *            the sVarOperand to set
     */
    public final void setsVarOperand(final String sVarOperand0) {
        this.sVarOperand = sVarOperand0;
    }

    /**
     * Gets the s var to check.
     * 
     * @return the sVarToCheck
     */
    public final String getsVarToCheck() {
        return this.sVarToCheck;
    }

    /**
     * Sets the s var to check.
     * 
     * @param sVarToCheck
     *            the sVarToCheck to set
     */
    public final void setsVarToCheck(final String sVarToCheck) {
        this.sVarToCheck = sVarToCheck;
    }

    /**
     * Gets the s var operator.
     * 
     * @return the sVarOperator
     */
    public final String getsVarOperator() {
        return this.sVarOperator;
    }

    /**
     * Sets the s var operator.
     * 
     * @param sVarOperator0
     *            the sVarOperator to set
     */
    public final void setsVarOperator(final String sVarOperator0) {
        this.sVarOperator = sVarOperator0;
    }

    /**
     * Checks if is opponent turn.
     * 
     * @return the opponentTurn
     */
    public final boolean isOpponentTurn() {
        return this.opponentTurn;
    }

    /**
     * Gets the cards in hand.
     * 
     * @return the cardsInHand
     */
    public final int getCardsInHand() {
        return this.cardsInHand;
    }

    /**
     * Sets the cards in hand.
     * 
     * @param cardsInHand0
     *            the cardsInHand to set
     */
    public final void setCardsInHand(final int cardsInHand0) {
        this.cardsInHand = cardsInHand0;
    }

    /**
     * Gets the checks if is present.
     * 
     * @return the isPresent
     */
    public final String getIsPresent() {
        return this.isPresent;
    }

    /**
     * Checks if is any player.
     * 
     * @return the anyPlayer
     */
    public final boolean isAnyPlayer() {
        return this.anyPlayer;
    }

    /**
     * @return the opponentOnly
     */
    public boolean isOpponentOnly() {
        return opponentOnly;
    }

    /**
     * @param opponentOnly the opponentOnly to set
     */
    public void setOpponentOnly(boolean opponentOnly) {
        this.opponentOnly = opponentOnly;
    }

} // end class SpellAbilityVariables
