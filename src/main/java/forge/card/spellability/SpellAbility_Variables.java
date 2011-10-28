package forge.card.spellability;

import java.util.ArrayList;

import forge.Constant;

/**
 * <p>
 * SpellAbility_Variables class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbility_Variables {
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
    public SpellAbility_Variables() {
    }

    // default values for Sorcery speed abilities
    /** The zone. */
    private Constant.Zone zone = Constant.Zone.Battlefield;

    /** The phases. */
    private ArrayList<String> phases = new ArrayList<String>();

    /** The b sorcery speed. */
    private boolean sorcerySpeed = false;

    /** The b any player. */
    private boolean anyPlayer = false;

    /** The b opponent turn. */
    private boolean opponentTurn = false;

    /** The b player turn. */
    private boolean playerTurn = false;

    /** The activation limit. */
    private int activationLimit = -1;

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

    /** The prowl. */
    private ArrayList<String> prowl = null;

    /** The s is present. */
    private String isPresent = null;

    /** The present compare. */
    private String presentCompare = "GE1"; // Default Compare to Greater or
                                           // Equal to 1

    /** The present defined. */
    private String presentDefined = null;

    /** The present zone. */
    private Constant.Zone presentZone = Constant.Zone.Battlefield;

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
    public final void setZone(final Constant.Zone zone) {
        this.zone = zone;
    }

    /**
     * <p>
     * Getter for the field <code>zone</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final Constant.Zone getZone() {
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
    public final void setPhases(final String phasesString) {
        for (final String s : phasesString.split(",")) {
            this.phases.add(s);
        }
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
    public final Constant.Zone getPresentZone() {
        return this.presentZone;
    }

    /**
     * Sets the present zone.
     * 
     * @param presentZone
     *            the new present zone
     */
    public final void setPresentZone(final Constant.Zone presentZone) {
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
     * @param pwAbility
     *            the new pw ability
     */
    public final void setPwAbility(final boolean pwAbility) {
        this.pwAbility = pwAbility; // TODO Add 0 to parameter's name.
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
     * @param lifeTotal
     *            the lifeTotal to set
     */
    public final void setLifeTotal(final String lifeTotal) {
        this.lifeTotal = lifeTotal; // TODO Add 0 to parameter's name.
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
     * @param lifeAmount
     *            the lifeAmount to set
     */
    public final void setLifeAmount(final String lifeAmount) {
        this.lifeAmount = lifeAmount; // TODO Add 0 to parameter's name.
    }

    /**
     * Gets the phases.
     * 
     * @return the phases
     */
    public final ArrayList<String> getPhases() {
        return this.phases;
    }

    /**
     * Sets the phases.
     * 
     * @param phases
     *            the new phases
     */
    public final void setPhases(final ArrayList<String> phases) {
        this.phases = phases; // TODO Add 0 to parameter's name.
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
     * @param sVarOperand the sVarOperand to set
     */
    public final void setsVarOperand(final String sVarOperand) {
        this.sVarOperand = sVarOperand; // TODO Add 0 to parameter's name.
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
     * @param sVarToCheck the sVarToCheck to set
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
     * @param sVarOperator the sVarOperator to set
     */
    public final void setsVarOperator(final String sVarOperator) {
        this.sVarOperator = sVarOperator; // TODO: Add 0 to parameter's name.
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
     * @param cardsInHand the cardsInHand to set
     */
    public final void setCardsInHand(final int cardsInHand) {
        this.cardsInHand = cardsInHand; // TODO: Add 0 to parameter's name.
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
        return anyPlayer;
    }

} // end class SpellAbility_Variables
