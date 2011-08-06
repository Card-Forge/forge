package forge;

import java.util.ArrayList;

import forge.card.mana.ManaCost;

public class GameInfo {
	private boolean computerStartedThisGame = false;
	
	private int humanNumberOfTimesMulliganed;
	private boolean humanMulliganedToZero;
	
	private boolean preventCombatDamageThisTurn;
	private boolean assignedFirstStrikeDamageThisCombat;
	private boolean resolvedFirstStrikeDamageThisCombat;
	
	private ArrayList<Card_Color> globalColorChanges = new ArrayList<Card_Color>();

	public int getHumanNumberOfTimesMulliganed()
	{
		return humanNumberOfTimesMulliganed;
	}
	
	public void addHumanNumberOfTimesMulliganed(int n)
	{
		humanNumberOfTimesMulliganed+=n;
	}
	
	public void setHumanNumberOfTimesMulliganed(int n)
	{
		humanNumberOfTimesMulliganed = n;
	}
	
	public boolean getHumanMulliganedToZero()
	{
		return humanMulliganedToZero;
	}
	
	public void setHumanMulliganedToZero(boolean b)
	{
		humanMulliganedToZero = b;
	}

	public void setPreventCombatDamageThisTurn(boolean b) {
		preventCombatDamageThisTurn = b;
	}

	public boolean isPreventCombatDamageThisTurn() {
		return preventCombatDamageThisTurn;
	}
	
	public void setAssignedFirstStrikeDamageThisCombat(boolean b) {
		assignedFirstStrikeDamageThisCombat = b;
	}

	public boolean getAssignedFirstStrikeDamageThisCombat() {
		return assignedFirstStrikeDamageThisCombat;
	}
	
	public void setResolvedFirstStrikeDamageThisCombat(boolean b)
	{
		resolvedFirstStrikeDamageThisCombat = b;
	}
	
	public boolean getResolvedFirstStrikeDamageThisCombat() {
		return resolvedFirstStrikeDamageThisCombat;
	}

	public void setComputerStartedThisGame(boolean computerStartedThisGame) {
		this.computerStartedThisGame = computerStartedThisGame;
	}

	public boolean isComputerStartedThisGame() {
		return computerStartedThisGame;
	}

	public long addColorChanges(String s, Card c, boolean addToColors, boolean bIncrease) {
    	if (bIncrease)
    		Card_Color.increaseTimestamp();
    	globalColorChanges.add(new Card_Color(new ManaCost(s), c, addToColors, false));
    	return Card_Color.getTimestamp();
	}

	public void removeColorChanges(String s, Card c, boolean addTo, long timestamp) {
		Card_Color removeCol = null;
    	for(Card_Color cc : globalColorChanges)
    		if (cc.equals(s, c, addTo, timestamp))
    			removeCol = cc;
    	
    	if (removeCol != null)
    		globalColorChanges.remove(removeCol);
	}
	
	public void clearColorChanges() {	
		// clear the global color changes at end of each game
		globalColorChanges.clear();
	}
		
	public ArrayList<Card_Color> getColorChanges() {
		return globalColorChanges;
	}
	
	
}
