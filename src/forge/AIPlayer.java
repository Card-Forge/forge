
package forge;

import java.util.Random;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;


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
		CardList dredgers = getDredge();
		Random random = MyRandom.random;
		
		//use dredge if there are more than one of them in your graveyard 
		if(dredgers.size() > 1 || (dredgers.size() == 1 && random.nextBoolean())) {
			dredgers.shuffle();
			Card c = dredgers.get(0);
			//rule 702.49a
			if(getDredgeNumber(c) <= AllZone.Computer_Library.size() ) {
				//dredge library, put card in hand
				AllZone.GameAction.moveToHand(c);
				//put dredge number in graveyard
				for(int i = 0; i < getDredgeNumber(c); i++) {
					Card c2 = AllZone.Computer_Library.get(0);
					AllZone.GameAction.moveToGraveyard(c2);
				}
			return true;
			}
		}
		return false;
	}
	
	////////////////////////////////
	///
	/// replaces AllZone.GameAction.discard* methods
	///
	////////////////////////////////
	
	public CardList discard(final int num, final SpellAbility sa, boolean duringResolution) {
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
        AllZone.ComputerPlayer.discard(num, sa, false);
	}
	
	public void handToLibrary(final int numToLibrary, final String libPosIn) {
		String libPos = libPosIn;
		
		for(int i = 0; i < numToLibrary; i++) {
			int position;
			if (libPos.equalsIgnoreCase("Top"))
				position = 0;
			else if (libPos.equalsIgnoreCase("Bottom"))
				position = -1;
			else{
				Random r = MyRandom.random;
				if(r.nextBoolean()) 
					position = 0;
				else 
					position = -1;
			}
			CardList hand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);

			CardList blIP = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			
			blIP = blIP.getType("Basic");
			if(blIP.size() > 5) {
				CardList blIH = hand.getType("Basic");
				if(blIH.size() > 0) {
					Card card = blIH.get(CardUtil.getRandomIndex(blIH));
					
					AllZone.GameAction.moveToLibrary(card, position);
				}
				else {
					CardListUtil.sortAttackLowFirst(hand);
					CardListUtil.sortNonFlyingFirst(hand);
					
					AllZone.GameAction.moveToLibrary(hand.get(0), position);
				}
			} 
			else {
				CardListUtil.sortCMC(hand); 
				
				AllZone.GameAction.moveToLibrary(hand.get(0), position);
			}
        }
	}
	
	
	///////////////////////////
	
	protected void doScry(final CardList topN, final int N) {
		int num = N;
		for (int i = 0; i < num; i++) {
            boolean bottom = false;
            if (topN.get(i).isType("Basic")) {
                CardList bl = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                bl = bl.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        if (c.isType("Basic")) return true;
                        
                        return false;
                    }
                });
                
                bottom = bl.size() > 5; // if control more than 5 Basic land, probably don't need more
            } else if (topN.get(i).isCreature()) {
                CardList cl = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                cl = cl.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        if (c.isCreature()) return true;
                        
                        return false;
                    }
                });
                
                bottom = cl.size() > 5;  // if control more than 5 Creatures, probably don't need more
            }
            if (bottom) {
            	Card c = topN.get(i);
            	AllZone.GameAction.moveToBottomOfLibrary(c);
                //topN.remove(c);
            }
        }
        num = topN.size();
        for (int i = 0; i < num; i++) // put the rest on top in random order
        {
            Random rndm = MyRandom.random;
            int r = rndm.nextInt(topN.size());
            Card c = topN.get(r);
            AllZone.GameAction.moveToLibrary(c);
            topN.remove(r);
        }
	}
	
	public void sacrificePermanent(String prompt, CardList choices) {
		if(choices.size() > 0) {
			//TODO - this could probably use better AI
			Card c = CardFactoryUtil.AI_getWorstPermanent(choices,false,false,false,false);
			AllZone.GameAction.sacrificeDestroy(c);
		}
	}
	
	protected void clashMoveToTopOrBottom(Card c) {
		//computer just puts the card back until such time it can make a smarter decision
		AllZone.GameAction.moveToLibrary(c);
	}
	
}//end AIPlayer class