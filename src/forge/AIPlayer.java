
package forge;

import java.util.Random;


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
	
	////////////////////////////////
	///
	/// replaces AllZone.GameAction.draw* methods
	///
	////////////////////////////////
	
	public void mayDrawCard() {
		mayDrawCards(1);
	}
	
	public void mayDrawCards(int n) {
		if(AllZone.Computer_Library.size() > n) {
			drawCards(n);
		}
	}
	
	public boolean dredge() {
		Random random = new Random();
		boolean use = random.nextBoolean();
		if(use) {
			CardList tmp = getDredge();
			tmp.shuffle();
			Card c = tmp.get(0);
			//rule 702.49a
			if(getDredgeNumber(c) <= AllZone.Computer_Library.size() ) {
				//dredge library, put card in hand
				AllZone.GameAction.moveToHand(c);
				//put dredge number in graveyard
				for(int i = 0; i < getDredgeNumber(c); i++) {
					Card c2 = AllZone.Computer_Library.get(0);
					AllZone.GameAction.moveToGraveyard(c2);
				}
			}
			else {
				use = false;
			}
		}
		return use;
	}
	
}//end AIPlayer class