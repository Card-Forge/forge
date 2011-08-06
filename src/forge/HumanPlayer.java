
package forge;


public class HumanPlayer extends Player{
	
	public HumanPlayer(String myName) {
		this(myName, 20, 0);
	}
	
	public HumanPlayer(String myName, int myLife, int myPoisonCounters) {
		super(myName, myLife, myPoisonCounters);
	}
	
	public Player getOpponent() {
		return AllZone.ComputerPlayer;
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
	
	public boolean dredge() {
		boolean dredged = false;
		String choices[] = {"Yes", "No"};
		Object o = AllZone.Display.getChoice("Do you want to dredge?", choices);
		if(o.equals("Yes")) {
			Card c = (Card) AllZone.Display.getChoice("Select card to dredge", getDredge().toArray());
			//rule 702.49a
			if(getDredgeNumber(c) <= AllZone.Human_Library.size()) {

				//might have to make this more sophisticated
				//dredge library, put card in hand
				AllZone.GameAction.moveToHand(c);

				for(int i = 0; i < getDredgeNumber(c); i++) {
					Card c2 = AllZone.Human_Library.get(0);
					AllZone.GameAction.moveToGraveyard(c2);
				}
				dredged = true;
			}
			else {
				dredged = false;
			}
		}
		return dredged;
	}
	
}//end HumanPlayer class