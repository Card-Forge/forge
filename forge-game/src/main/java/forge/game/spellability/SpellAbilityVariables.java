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

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.game.GameType;
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
public class SpellAbilityVariables implements Cloneable {
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
        this.phases = Sets.newEnumSet(sav.getPhases(), PhaseType.class);
        this.gameTypes = Sets.newEnumSet(sav.getGameTypes(), GameType.class);
        this.sorcerySpeed = sav.isSorcerySpeed();
        this.instantSpeed = sav.isInstantSpeed();
        this.activator = sav.getActivator();
        this.opponentTurn = sav.isOpponentTurn();
        this.playerTurn = sav.isPlayerTurn();
        this.activationLimit = sav.getActivationLimit();
        this.gameActivationLimit = sav.getGameActivationLimit();
        this.cardsInHand = sav.getCardsInHand();
        this.chosenColors = sav.getColorToCheck();
        this.threshold = sav.isThreshold();
        this.metalcraft = sav.isMetalcraft();
        this.delirium = sav.isDelirium();
        this.hellbent = sav.isHellbent();
        this.allTargetsLegal = sav.isAllTargetsLegal();
        this.shareAllColors = sav.getShareAllColors();
        this.isPresent = sav.getIsPresent();
        this.presentCompare = sav.getPresentCompare();
        this.presentDefined = sav.getPresentDefined();
        this.playerDefined = sav.getPlayerDefined();
        this.playerContains = sav.getPlayerContains();
        this.presentZone = sav.getPresentZone();
        this.sVarToCheck = sav.getsVarToCheck();
        this.sVarToCheck2 = sav.getsVarToCheck2();
        this.sVarOperator = sav.getsVarOperator();
        this.sVarOperator2 = sav.getsVarOperator2();
        this.sVarOperand = sav.getsVarOperand();
        this.sVarOperand2 = sav.getsVarOperand2();
        this.lifeTotal = sav.getLifeTotal();
        this.lifeAmount = sav.getLifeAmount();
        this.manaSpent = sav.getManaSpent();
        this.manaNotSpent = sav.getManaNotSpent();
        this.targetValidTargeting = sav.getTargetValidTargeting();
        this.targetsSingleTarget = sav.targetsSingleTarget();
        this.presenceCondition = sav.getPresenceCondition();
        this.classLevel = sav.getClassLevel();
        this.classLevelOperator = sav.getClassLevelOperator();
    }

    // default values for Sorcery speed abilities
    /** The zone. */
    private ZoneType zone = ZoneType.Battlefield;

    /** The phases. */
    private Set<PhaseType> phases = EnumSet.noneOf(PhaseType.class);

    /** The GameTypes */
    private Set<GameType> gameTypes = EnumSet.noneOf(GameType.class);

    /** The b sorcery speed. */
    private boolean sorcerySpeed = false;

    /** The b instant speed. */
    private boolean instantSpeed = false;

    /** The b any player. */
    private String activator = "You";

    /** The b opponent turn. */
    private boolean opponentTurn = false;

    /** The b player turn. */
    private boolean playerTurn = false;

    /** The activation limit. */
    private int activationLimit = -1;

    /** The game activation limit. */
    private int gameActivationLimit = -1;

    /** The limitToCheck to check. */
    private String limitToCheck = null;

    /** The gameLimitToCheck to check. */
    private String gameLimitToCheck = null;

    /** The n cards in hand. */
    private int cardsInHand = -1;
    private int cardsInHand2 = -1;

    // Conditional States for Cards
    private boolean threshold = false;
    private boolean metalcraft = false;
    private boolean delirium = false;
    private boolean hellbent = false;
    private boolean revolt = false;
    private boolean desert = false;
    private boolean blessing = false;

    private boolean allTargetsLegal = false;

    /** The s is present. */
    private String isPresent = null;

    /** The present compare. */
    private String presentCompare = "GE1"; // Default Compare to Greater or
                                           // Equal to 1

    /** The present defined. */
    private String presentDefined = null;

    /** The player defined. */
    private String playerDefined = null;

    /** The player contains. */
    private String playerContains = null;

    /** The present zone. */
    private ZoneType presentZone = ZoneType.Battlefield;

    /** The svar to check. */
    private String sVarToCheck = null;
    private String sVarToCheck2 = null;

    /** The svar operator. */
    private String sVarOperator = "GE";
    private String sVarOperator2 = "GE";

    /** The svar operand. */
    private String sVarOperand = "1";
    private String sVarOperand2 = "1";

    /** The life total. */
    private String lifeTotal = null;

    /** The life amount. */
    private String lifeAmount = "GE1";

    /** The shareAllColors. */
    private String shareAllColors = null;

    /** The mana spent. */
    private String manaSpent = "";
    private String manaNotSpent = "";

    /** The chosen colors string. */
    private String chosenColors = null;

    /** The target valid targeting */
    private String targetValidTargeting = null;

    /** The b targetsSingleTargeting */
    private boolean targetsSingleTarget = false;

    /** The Presence keyword value containing the relevant condition */
    private String presenceCondition = "";

    /** The class level. */
    private String classLevel = null;
    private String classLevelOperator = "EQ";

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

    public final void setManaNotSpent(final String s) {
        this.manaNotSpent = s;
    }
    public final String getManaNotSpent() {
        return this.manaNotSpent;
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

    public final void setSorcerySpeed(final boolean bSpeed) {
        this.sorcerySpeed = bSpeed;
    }

    public final boolean isSorcerySpeed() {
        return this.sorcerySpeed;
    }

    public final void setInstantSpeed(final boolean bSpeed) {
        this.instantSpeed = bSpeed;
    }

    public final boolean isInstantSpeed() {
        return this.instantSpeed;
    }

    public final void setActivator(final String player) {
        this.activator = player;
    }

    public String getActivator() {
        return this.activator;
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
     * Setter for the field <code>gameActivationLimit</code>.
     * </p>
     *
     * @param limit
     *            a int.
     */
    public final void setGameActivationLimit(final int limit) {
        this.gameActivationLimit = limit;
    }

    /**
     * <p>
     * Setter for the field <code>phases</code>.
     * </p>
     *
     * @param phases
     *            a {@link java.lang.String} object.
     */
    public final void setPhases(final Set<PhaseType> phases) {
        this.phases.addAll(phases);
    }

    /**
     * <p>
     * Setter for the field <code>gameTypes</code>.
     * </p>
     *
     * @param gameTypes
     */
    public final void setGameTypes(final Set<GameType> gameTypes) {
        this.gameTypes.clear();
        this.gameTypes.addAll(gameTypes);
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
    public final void setActivateCardsInHand2(final int cards) {
        this.setCardsInHand2(cards);
    }


    public final void setHellbent(final boolean bHellbent) {
        this.hellbent = bHellbent;
    }

    public final void setThreshold(final boolean bThreshold) {
        this.threshold = bThreshold;
    }

    public final void setMetalcraft(final boolean bMetalcraft) {  this.metalcraft = bMetalcraft;  }

    public void setDelirium(boolean delirium) {  this.delirium = delirium; }

    public void setRevolt(final boolean bRevolt) { revolt = bRevolt; }

    public void setDesert(final boolean bDesert) { desert = bDesert; }

    public void setBlessing(final boolean bBlessing) { blessing = bBlessing; }

    /** Optional Costs */
    protected boolean kicked = false;
    protected boolean kicked1 = false; // http://magiccards.info/query?q=o%3A%22kicker%22+not+o%3A%22multikicker%22+o%3A%22and%2For+{%22
    protected boolean kicked2 = false; // Some spells have 2 kickers with different effects
    protected boolean altCostPaid = false;
    protected boolean optionalCostPaid = false; // Undergrowth other Pseudo-kickers
    protected boolean optionalBoolean = true; // Just in case you need to check if something wasn't kicked, etc
    protected boolean surgeCostPaid = false;
    protected boolean foretold = false;

    /**
     * @return the allTargetsLegal
     */
    public boolean isAllTargetsLegal() {
        return allTargetsLegal;
    }

    /**
     * @param allTargets the allTargetsLegal to set
     */
    public void setAllTargetsLegal(boolean allTargets) {
        this.allTargetsLegal = allTargets;
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
    public final void setSvarToCheck2(final String sVar) {
        this.setsVarToCheck2(sVar);
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

    //for second possible SVar condition
    public final void setSvarOperator2(final String operator) {
        this.setsVarOperator2(operator);
    }
    public final void setSvarOperand2(final String operand) {
        this.setsVarOperand2(operand);
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
     * Gets the activation limit.
     *
     * @return the activationLimit
     */
    public final int getGameActivationLimit() {
        return this.gameActivationLimit;
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
     * Setter for the field <code>GamelimitToCheck</code>.
     * </p>
     *
     * @param limit
     *            a {@link java.lang.String} object.
     */
    public final void setGameLimitToCheck(final String limit) {
        this.gameLimitToCheck = limit;
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
     * <p>
     * Getter for the field <code>getGameLimitToCheck</code>.
     * </p>
     *
     * @return the getGameLimitToCheck
     *            a {@link java.lang.String} object.
     */
    public final String getGameLimitToCheck() {
        return this.gameLimitToCheck;
    }

    public final boolean isThreshold() {    return this.threshold;  }

    public final boolean isMetalcraft() {   return this.metalcraft; }

    public final boolean isDelirium() {     return this.delirium;  }

    public final boolean isHellbent() {     return this.hellbent;  }

    public final boolean isRevolt() {     return this.revolt;  }

    public final boolean isDesert() {     return this.desert;  }
    public final boolean isBlessing() {     return this.blessing;  }

    public String getShareAllColors() {
        return shareAllColors;
    }

    public void setShareAllColors(String shareAllColors) {
        this.shareAllColors = shareAllColors;
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
    public final Set<PhaseType> getPhases() {
        return this.phases;
    }

    /**
     * Gets the game types.
     *
     * @return the phases
     */
    public final Set<GameType> getGameTypes() {
        return this.gameTypes;
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
     * Set the player defined.
     *
     */
    public final void setPlayerDefined(final String b) {
        this.playerDefined = b;
    }

    /**
     * Gets the player defined.
     *
     * @return the playerDefined
     */
    public final String getPlayerDefined() {
        return this.playerDefined;
    }

    /**
     * Gets the player contains.
     *
     * @return the playerContains
     */
    public final String getPlayerContains() {
        return this.playerContains;
    }

    /**
     * Set the player contains.
     *
     */
    public final void setPlayerContains(final String contains) {
        this.playerContains = contains;
    }

    /**
     * Gets the s var operand.
     *
     * @return the sVarOperand
     */
    public final String getsVarOperand() {
        return this.sVarOperand;
    }
    public final String getsVarOperand2() {
        return this.sVarOperand2;
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
    public final void setsVarOperand2(final String sVarOperand0) {
        this.sVarOperand2 = sVarOperand0;
    }

    /**
     * Gets the s var to check.
     *
     * @return the sVarToCheck
     */
    public final String getsVarToCheck() {
        return this.sVarToCheck;
    }
    public final String getsVarToCheck2() {
        return this.sVarToCheck2;
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
    public final void setsVarToCheck2(final String sVarToCheck) {
        this.sVarToCheck2 = sVarToCheck;
    }

    /**
     * Gets the s var operator.
     *
     * @return the sVarOperator
     */
    public final String getsVarOperator() {
        return this.sVarOperator;
    }
    public final String getsVarOperator2() {
        return this.sVarOperator2;
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
    public final void setsVarOperator2(final String sVarOperator0) {
        this.sVarOperator2 = sVarOperator0;
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
    public final int getCardsInHand2() {
        return this.cardsInHand2;
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
    public final void setCardsInHand2(final int cardsInHand0) {
        this.cardsInHand2 = cardsInHand0;
    }

    /**
     * Gets the checks if is present.
     *
     * @return the isPresent
     */
    public final String getIsPresent() {
        return this.isPresent;
    }

    public final void setColorToCheck(final String s) {
        this.chosenColors = s;
    }

    /**
     * <p>
     * Getter for the field <code>ColorToCheck</code>.
     * </p>
     *
     * @return the String, chosenColors.
     */
    public final String getColorToCheck() {
        return this.chosenColors;
    }

	/**
	 * @return the targetValidTargeting
	 */
	public String getTargetValidTargeting() {
		return targetValidTargeting;
	}

	/**
	 * @param targetValidTargeting the targetValidTargeting to set
	 */
	public void setTargetValidTargeting(String targetValidTargeting) {
		this.targetValidTargeting = targetValidTargeting;
	}

    /**
     * @return the targetsSingleTarget
     */
	public boolean targetsSingleTarget() {
		return targetsSingleTarget;
	}

    /**
     * @param b the targetsSingleTarget to set
     */
    public void setTargetsSingleTarget(boolean b) {
        this.targetsSingleTarget = b;
    }

    public SpellAbilityVariables copy() {
        try {
            return (SpellAbilityVariables) clone();
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return null;
    }

    /**
     * @return the condition from the Presence keyword, empty if keyword is absent
     */
    public String getPresenceCondition() {
        return this.presenceCondition;
    }

    /**
     * @param s the condition from the Presence keyword
     */
    public void setPresenceCondition(String s) {
        this.presenceCondition = s;
    }

    public String getClassLevel() {
        return classLevel;
    }
    public void setClassLevel(String level) {
        classLevel = level;
    }

    public String getClassLevelOperator() {
        return classLevelOperator;
    }
    public void setClassLevelOperator(String op) {
        classLevelOperator = op;
    }
} // end class SpellAbilityVariables
