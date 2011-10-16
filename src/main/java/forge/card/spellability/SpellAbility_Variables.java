package forge.card.spellability;


import forge.Constant;

import java.util.ArrayList;

/**
 * <p>SpellAbility_Variables class.</p>
 *
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbility_Variables {
    // A class for handling SpellAbility Variables. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, Hellbent, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by AbilityFactory)

    /**
     * <p>Constructor for SpellAbility_Variables.</p>
     */
    public SpellAbility_Variables() {
    }

    // default values for Sorcery speed abilities
    protected Constant.Zone zone = Constant.Zone.Battlefield;
    protected ArrayList<String> phases = new ArrayList<String>();
    protected boolean bSorcerySpeed = false;
    protected boolean bAnyPlayer = false;
    protected boolean bOpponentTurn = false;
    protected boolean bPlayerTurn = false;

    protected int activationLimit = -1;
    protected int numberTurnActivations = 0;
    protected int activationNumberSacrifice = -1;

    protected int nCardsInHand = -1;
    protected boolean threshold = false;
    protected boolean metalcraft = false;
    protected boolean hellbent = false;
    protected ArrayList<String> prowl = null;

    protected String sIsPresent = null;
    protected String presentCompare = "GE1";    // Default Compare to Greater or Equal to 1
    protected String presentDefined = null;
    protected Constant.Zone presentZone = Constant.Zone.Battlefield;

    protected String svarToCheck = null;
    protected String svarOperator = "GE";
    protected String svarOperand = "1";

    protected String lifeTotal = null;
    protected String lifeAmount = "GE1";
    
    protected String manaSpent = "";

    protected boolean pwAbility = false;
    
    protected boolean allM12Empires = false;
    protected boolean notAllM12Empires = false;
    
    /**
     * <p>Setter for the field <code>notAllM12Empires</code>.</p>
     *
     * @param b a boolean
     */
    public void setNotAllM12Empires(boolean b) {
        notAllM12Empires = b;
    }
    
    /**
     * <p>Getter for the field <code>notAllM12Empires</code>.</p>
     *
     * @return a boolean
     */
    public boolean getNotAllM12Empires() {
        return notAllM12Empires;
    }
    
    /**
     * <p>Setter for the field <code>allM12Empires</code>.</p>
     *
     * @param b a boolean
     */
    public void setAllM12Empires(boolean b) {
        allM12Empires = b;
    }
    
    /**
     * <p>Getter for the field <code>allM12Empires</code>.</p>
     *
     * @return a boolean
     */
    public boolean getAllM12Empires() {
        return allM12Empires;
    }
    
    /**
     * <p>Setter for the field <code>manaSpent</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setManaSpent(String s) {
    	manaSpent = s;
    }
    
    /**
     * <p>Getter for the field <code>manaSpent</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getManaSpent() {
    	return manaSpent;
    }

    /**
     * <p>Setter for the field <code>zone</code>.</p>
     *
     * @param zone a {@link java.lang.String} object.
     */
    public void setZone(Constant.Zone zone) {
        this.zone = zone;
    }

    /**
     * <p>Getter for the field <code>zone</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public Constant.Zone getZone() {
        return zone;
    }

    /**
     * <p>setSorcerySpeed.</p>
     *
     * @param bSpeed a boolean.
     */
    public void setSorcerySpeed(boolean bSpeed) {
        bSorcerySpeed = bSpeed;
    }

    /**
     * <p>getSorcerySpeed.</p>
     *
     * @return a boolean.
     */
    public boolean getSorcerySpeed() {
        return bSorcerySpeed;
    }

    /**
     * <p>setAnyPlayer.</p>
     *
     * @param anyPlayer a boolean.
     */
    public void setAnyPlayer(boolean anyPlayer) {
        bAnyPlayer = anyPlayer;
    }

    /**
     * <p>getAnyPlayer.</p>
     *
     * @return a boolean.
     */
    public boolean getAnyPlayer() {
        return bAnyPlayer;
    }

    /**
     * <p>setPlayerTurn.</p>
     *
     * @param bTurn a boolean.
     */
    public void setPlayerTurn(boolean bTurn) {
        bPlayerTurn = bTurn;
    }

    /**
     * <p>getPlayerTurn.</p>
     *
     * @return a boolean.
     */
    public boolean getPlayerTurn() {
        return bPlayerTurn;
    }

    /**
     * <p>setOpponentTurn.</p>
     *
     * @param bTurn a boolean.
     */
    public void setOpponentTurn(boolean bTurn) {
        bOpponentTurn = bTurn;
    }

    /**
     * <p>getOpponentTurn.</p>
     *
     * @return a boolean.
     */
    public boolean getOpponentTurn() {
        return bOpponentTurn;
    }

    /**
     * <p>Setter for the field <code>activationLimit</code>.</p>
     *
     * @param limit a int.
     */
    public void setActivationLimit(int limit) {
        activationLimit = limit;
    }

    /**
     * <p>abilityActivated.</p>
     */
    public void abilityActivated() {
        numberTurnActivations++;
    }

    /**
     * <p>Getter for the field <code>numberTurnActivations</code>.</p>
     *
     * @return a int.
     */
    public int getNumberTurnActivations() {
        return numberTurnActivations;
    }

    /**
     * <p>resetTurnActivations.</p>
     */
    public void resetTurnActivations() {
        numberTurnActivations = 0;
    }

    /**
     * <p>Setter for the field <code>activationNumberSacrifice</code>.</p>
     *
     * @param num a int.
     */
    public void setActivationNumberSacrifice(int num) {
        activationNumberSacrifice = num;
    }

    /**
     * <p>Getter for the field <code>activationNumberSacrifice</code>.</p>
     *
     * @return a int.
     */
    public int getActivationNumberSacrifice() {
        return activationNumberSacrifice;
    }

    /**
     * <p>Setter for the field <code>phases</code>.</p>
     *
     * @param phasesString a {@link java.lang.String} object.
     */
    public void setPhases(String phasesString) {
        for (String s : phasesString.split(","))
            phases.add(s);
    }

    /**
     * <p>setActivateCardsInHand.</p>
     *
     * @param cards a int.
     */
    public void setActivateCardsInHand(int cards) {
        nCardsInHand = cards;
    }

    //specific named conditions
    /**
     * <p>Setter for the field <code>hellbent</code>.</p>
     *
     * @param bHellbent a boolean.
     */
    public void setHellbent(boolean bHellbent) {
        hellbent = bHellbent;
    }

    /**
     * <p>Setter for the field <code>threshold</code>.</p>
     *
     * @param bThreshold a boolean.
     */
    public void setThreshold(boolean bThreshold) {
        threshold = bThreshold;
    }

    /**
     * <p>Setter for the field <code>metalcraft</code>.</p>
     *
     * @param bMetalcraft a boolean.
     */
    public void setMetalcraft(boolean bMetalcraft) {
        metalcraft = bMetalcraft;
    }
    
    /**
     * <p>Setter for the field <code>prowl</code>.</p>
     *
     * @param bProwl a boolean.
     */
    public void setProwl(ArrayList<String> types) {
        prowl = types;
    }

    //IsPresent for Valid battlefield stuff

    /**
     * <p>setIsPresent.</p>
     *
     * @param present a {@link java.lang.String} object.
     */
    public void setIsPresent(String present) {
        sIsPresent = present;
    }

    /**
     * <p>Setter for the field <code>presentCompare</code>.</p>
     *
     * @param compare a {@link java.lang.String} object.
     */
    public void setPresentCompare(String compare) {
        presentCompare = compare;
    }

    public Constant.Zone getPresentZone() {
		return presentZone;
	}

	public void setPresentZone(Constant.Zone presentZone) {
		this.presentZone = presentZone;
	}

	/**
     * <p>Setter for the field <code>presentDefined</code>.</p>
     *
     * @param defined a {@link java.lang.String} object.
     */
    public void setPresentDefined(String defined) {
        presentDefined = defined;
    }

    //used to define as a Planeswalker ability
    /**
     * <p>setPlaneswalker.</p>
     *
     * @param bPlaneswalker a boolean.
     */
    public void setPlaneswalker(boolean bPlaneswalker) {
        pwAbility = bPlaneswalker;
    }

    /**
     * <p>getPlaneswalker.</p>
     *
     * @return a boolean.
     */
    public boolean getPlaneswalker() {
        return pwAbility;
    }

    //Checking the values of SVars (Mostly for Traps)
    /**
     * <p>Setter for the field <code>svarToCheck</code>.</p>
     *
     * @param SVar a {@link java.lang.String} object.
     */
    public void setSvarToCheck(String SVar) {
        svarToCheck = SVar;
    }

    /**
     * <p>Setter for the field <code>svarOperator</code>.</p>
     *
     * @param Operator a {@link java.lang.String} object.
     */
    public void setSvarOperator(String Operator) {
        svarOperator = Operator;
    }

    /**
     * <p>Setter for the field <code>svarOperand</code>.</p>
     *
     * @param Operand a {@link java.lang.String} object.
     */
    public void setSvarOperand(String Operand) {
        svarOperand = Operand;
    }

}//end class SpellAbility_Variables
