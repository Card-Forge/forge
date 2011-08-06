
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
	
	////////////////////////////////
	///
	/// replaces AllZone.GameAction.discard* methods
	///
	////////////////////////////////
	
	public CardList discard(final int num, final SpellAbility sa) {
		int max = AllZoneUtil.getPlayerHand(this).size();
		max = Math.min(max, num);
		CardList discarded = new CardList();
		for(int i = 0; i < max; i++) {
			CardList hand = AllZoneUtil.getPlayerHand(this);

			if(hand.size() > 0) {
				CardList basicLandsInPlay = AllZoneUtil.getPlayerTypeInPlay(this, "Basic");
				if(basicLandsInPlay.size() > 5) {
					CardList basicLandsInHand = hand.getType("Basic");
					if(basicLandsInHand.size() > 0) {
						discarded.add(hand.get(0));
						doDiscard(basicLandsInHand.get(CardUtil.getRandomIndex(basicLandsInHand)), sa);
					}
					else{
						CardListUtil.sortAttackLowFirst(hand);
						CardListUtil.sortNonFlyingFirst(hand);
						discarded.add(hand.get(0));
						doDiscard(hand.get(0), sa);
					}
				}
				else {
					CardListUtil.sortCMC(hand);
					discarded.add(hand.get(0));
					doDiscard(hand.get(0), sa);
				}
			}
		}
		return discarded;
	}//end discard
	
	public void discardUnless(int num, String uType, SpellAbility sa) {
		CardList hand = AllZoneUtil.getPlayerHand(this);
        CardList tHand = hand.getType(uType);
        
        if(tHand.size() > 0) {
            CardListUtil.sortCMC(tHand);
            tHand.reverse();
            tHand.get(0).getController().discard(tHand.get(0), sa);  //this got changed to doDiscard basically
            return;
        }
        AllZone.ComputerPlayer.discard(num, sa);
	}
	
	public void handToLibrary(final int numToLibrary, final String libPosIn) {
		String libPos = libPosIn;
		for(int i = 0; i < numToLibrary; i++) {
			if(libPos.equals("Top") || libPos.equals("Bottom")) libPos = libPos.toLowerCase();
			else {
				Random r = new Random();
				if(r.nextBoolean()) libPos = "top";
				else libPos = "bottom";
			}
			CardList hand = new CardList();
			hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());

			CardList blIP = new CardList();
			blIP.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards());
			blIP = blIP.getType("Basic");
			if(blIP.size() > 5) {
				CardList blIH = hand.getType("Basic");
				if(blIH.size() > 0) {
					Card card = blIH.get(CardUtil.getRandomIndex(blIH));
					AllZone.Computer_Hand.remove(card);
					if(libPos.equals("top")) AllZone.Computer_Library.add(card, 0);
					else AllZone.Computer_Library.add(card);
					//return;
				}
				else {

					CardListUtil.sortAttackLowFirst(hand);
					CardListUtil.sortNonFlyingFirst(hand);
					if(libPos.equals("top")) AllZone.Computer_Library.add(hand.get(0), 0);
					else AllZone.Computer_Library.add(hand.get(0));
					AllZone.Computer_Hand.remove(hand.get(0));
					//return;
				}
			} 
			else {
				CardListUtil.sortCMC(hand); 
				if(libPos.equals("top")) AllZone.Computer_Library.add(hand.get(0), 0);
				else AllZone.Computer_Library.add(hand.get(0));
				AllZone.Computer_Hand.remove(hand.get(0));
				//return;
			}
        }
	}
	
	
	///////////////////////////
	
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
	
	public void sacrificePermanent(String prompt, CardList choices) {
		if(choices.size() > 0) {
			//TODO - this could probably use better AI
			CardListUtil.sortDefense(choices);
			choices.reverse();
			CardListUtil.sortAttackLowFirst(choices);
			Card c = choices.get(0);
			AllZone.GameAction.sacrificeDestroy(c);
		}
	}
	
}//end AIPlayer class