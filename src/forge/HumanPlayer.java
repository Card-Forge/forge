
package forge;


public class HumanPlayer extends Player{
	
	public HumanPlayer(String myName) {
		this(myName, 20, 0);
	}
	
	public HumanPlayer(String myName, int myLife, int myPoisonCounters) {
		super(myName, myLife, myPoisonCounters);
	}
	
	////////////////
	///
	/// Methods to ease transition to Abstract Player class
	///
	///////////////
	
	public boolean isHuman() { return true; }
	public boolean isComputer() { return false; }
	public boolean isPlayer(Player p1) {
		return p1.getName().equals(this.name);
	}
	
	///////////////
	///
	/// End transition methods
	///
	///////////////
	
	public Player getOpponent() {
		return AllZone.ComputerPlayer;
	}
	
}//end HumanPlayer class