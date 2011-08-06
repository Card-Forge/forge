
package forge;


public class AIPlayer extends Player{
	
	public AIPlayer(String myName) {
		this(myName, 20, 0);
	}
	
	public AIPlayer(String myName, int myLife, int myPoisonCounters) {
		super(myName, myLife, myPoisonCounters);
	}
	
	public Player getOpponent() {
		return AllZone.HumanPlayer;
	}
	
	////////////////
	///
	/// Methods to ease transition to Abstract Player class
	///
	///////////////
	
	public boolean isHuman() { return false; }
	public boolean isComputer() { return true; }
	public boolean isPlayer(Player p1) {
		return p1.getName().equals(this.name);
	}
	
	///////////////
	///
	/// End transition methods
	///
	///////////////
	
}//end AIPlayer class