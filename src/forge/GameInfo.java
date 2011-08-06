package forge;

public class GameInfo {
	private int computerCanPlayNumberOfLands;
	private int humanCanPlayNumberOfLands;
	
	private boolean computerPlayedFirstLandThisTurn;
	private boolean humanPlayedFirstLandThisTurn;
	
	private int humanNumberOfTimesMulliganed;
	private boolean humanMulliganedToZero;
	
	private boolean preventCombatDamageThisTurn;
	private boolean assignedFirstStrikeDamageThisCombat;
	private boolean resolvedFirstStrikeDamageThisCombat;

	public void setComputerCanPlayNumberOfLands(int n) {
		computerCanPlayNumberOfLands = n;
	}

	public int getComputerCanPlayNumberOfLands() {
		return computerCanPlayNumberOfLands;
	}
	
	public void addComputerCanPlayNumberOfLands(int n)
    {
		computerCanPlayNumberOfLands += n;
    }

	public void setHumanCanPlayNumberOfLands(int n) {
		humanCanPlayNumberOfLands = n;
	}

	public int getHumanCanPlayNumberOfLands() {
		return humanCanPlayNumberOfLands;
	}
	
	public void addHumanCanPlayNumberOfLands(int n)
    {
		humanCanPlayNumberOfLands += n;
    }
	
	
	public void setComputerPlayedFirstLandThisTurn(boolean b) {
		computerPlayedFirstLandThisTurn = b;
	}

	public boolean computerPlayedFirstLandThisTurn() {
		return computerPlayedFirstLandThisTurn;
	}

	public void setHumanPlayedFirstLandThisTurn(boolean b) {
		humanPlayedFirstLandThisTurn = b;
	}

	public boolean humanPlayedFirstLandThisTurn() {
		return humanPlayedFirstLandThisTurn;
	}

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

	
	
}
