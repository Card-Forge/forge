
package forge.card.spellability;


import java.util.ArrayList;

import forge.Constant;

public class SpellAbility_Variables {
	// A class for handling SpellAbility Variables. These restrictions include: 
	// Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player, 
	// Threshold, Metalcraft, Hellbent, LevelRange, etc
	// Each value will have a default, that can be overridden (mostly by AbilityFactory)

	public SpellAbility_Variables(){	}

	// default values for Sorcery speed abilities
	protected String zone = Constant.Zone.Battlefield;
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

	protected String sIsPresent = null;
	protected String presentCompare = "GE1";	// Default Compare to Greater or Equal to 1
	protected String presentDefined = null;

	protected String lifeTotal = null;
	protected String lifeAmount = "GE1";

	protected boolean pwAbility = false;

	public void setZone(String zone){
		this.zone = zone;
	}

	public String getZone(){
		return zone;
	}

	public void setSorcerySpeed(boolean bSpeed){
		bSorcerySpeed = bSpeed;
	}

	public boolean getSorcerySpeed(){
		return bSorcerySpeed;
	}

	public void setAnyPlayer(boolean anyPlayer){
		bAnyPlayer = anyPlayer;
	}

	public boolean getAnyPlayer(){
		return bAnyPlayer;
	}

	public void setPlayerTurn(boolean bTurn){
		bPlayerTurn = bTurn;
	}

	public boolean getPlayerTurn(){
		return bPlayerTurn;
	}

	public void setOpponentTurn(boolean bTurn){
		bOpponentTurn = bTurn;
	}

	public boolean getOpponentTurn(){
		return bOpponentTurn;
	}

	public void setActivationLimit(int limit){
		activationLimit = limit;
	}

	public void abilityActivated(){
		numberTurnActivations++;
	}

	public int getNumberTurnActivations() {
		return numberTurnActivations;
	}

	public void resetTurnActivations(){
		numberTurnActivations = 0;
	}

	public void setActivationNumberSacrifice(int num) {
		activationNumberSacrifice = num;
	}

	public int getActivationNumberSacrifice() {
		return activationNumberSacrifice;
	}

	public void setPhases(String phasesString){
		for(String s : phasesString.split(","))
			phases.add(s);
	}

	public void setActivateCardsInHand(int cards){
		nCardsInHand = cards;
	}

	//specific named conditions
	public void setHellbent(boolean bHellbent) {
		hellbent = bHellbent;
	}

	public void setThreshold(boolean bThreshold){
		threshold = bThreshold;
	}

	public void setMetalcraft(boolean bMetalcraft) {
		metalcraft = bMetalcraft;
	}

	//IsPresent for Valid battlefield stuff

	public void setIsPresent(String present){
		sIsPresent = present;
	}

	public void setPresentCompare(String compare){
		presentCompare = compare;
	}

	public void setPresentDefined(String defined) {
		presentDefined = defined;
	}

	//used to define as a Planeswalker ability
	public void setPlaneswalker(boolean bPlaneswalker) { pwAbility = bPlaneswalker; }
	public boolean getPlaneswalker() { return pwAbility; }

	/*
	 * Restrictions of the future
	 * (can level Min level Max be done with isPresent?)
		int levelMin = 0;
		int levelMax = 0;
	 */


}//end class SpellAbility_Variables
