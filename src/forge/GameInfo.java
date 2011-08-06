package forge;

public class GameInfo {
	private int computerCanPlayNumberOfLands;
	private int humanCanPlayNumberOfLands;
	
	private boolean computerPlayedFirstLandThisTurn;
	private boolean humanPlayedFirstLandThisTurn;
	
	private boolean preventCombatDamageThisTurn;

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

	

	public void setPreventCombatDamageThisTurn(boolean b) {
		preventCombatDamageThisTurn = b;
	}

	public boolean isPreventCombatDamageThisTurn() {
		return preventCombatDamageThisTurn;
	}

	
	
}
