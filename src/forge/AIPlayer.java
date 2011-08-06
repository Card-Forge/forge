
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
	
	protected Card getPlayerCard() {
		return AllZone.CardFactory.ComputerNullCard;
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
	
	protected void doScry(final CardList topN, final int N) {
		int num = N;
		for(int i = 0; i < num; i++) {
            boolean b = false;
            if(topN.get(i).getType().contains("Basic")) {
                CardList bl = new CardList(
                        AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
                bl = bl.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        if(c.getType().contains("Basic")) return true;
                        
                        return false;
                    }
                });
                
                if(bl.size() > 5) // if control more than 5 Basic land, probably don't need more
                b = true;
            } else if(topN.get(i).getType().contains("Creature")) {
                CardList cl = new CardList(
                        AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
                cl = cl.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        if(c.getType().contains("Creature")) return true;
                        
                        return false;
                    }
                });
                
                if(cl.size() > 5) // if control more than 5 Creatures, probably don't need more
                b = true;
            }
            if(b == true) {
                AllZone.Computer_Library.add(topN.get(i));
                topN.remove(i);
            }
        }
        num = topN.size();
        if(num > 0) for(int i = 0; i < num; i++) // put the rest on top in random order
        {
            Random rndm = new Random();
            int r = rndm.nextInt(topN.size());
            AllZone.Computer_Library.add(topN.get(r), 0);
            topN.remove(r);
        }
	}
	
}//end AIPlayer class