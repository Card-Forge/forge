
package forge;

import java.util.ArrayList;


public class Input_Untap extends Input {
    private static final long serialVersionUID = 3452595801560263386L;
    
    @Override
    public void showMessage() {
        PlayerZone p = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
        Card[] c = p.getCards();
        
        AllZone.GameAction.setPlayerTurn(AllZone.Phase.getActivePlayer());
        
        if (AllZone.Phase.getTurn() != 1 && 
        	!(AllZone.Phase.getActivePlayer().equals(AllZone.HumanPlayer) && AllZone.Phase.getTurn() == 2) )
        {
	        for(int i = 0; i < c.length; i++)
	            c[i].setSickness(false);
        }
        
        if(!AllZoneUtil.isCardInPlay("Stasis")) doUntap();
        
        if (AllZone.Phase.getTurn() != 1)
            GameActionUtil.executeUpkeepEffects();
        
        AllZone.GameAction.resetActivationsPerTurn();
        
        //otherwise land seems to stay tapped when it is really untapped
        AllZone.Human_Play.updateObservers();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_Untap) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }

    
    private void doUntap()
    {
    	PlayerZone p = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
    	CardList list = new CardList(p.getCards());
    	
    	for(Card c : list) {
    		if (c.getBounceAtUntap() && c.getName().contains("Undiscovered Paradise") )
    		{
    			AllZone.GameAction.moveToHand(c);
    		}
    	}
    	
    	CardList allp = new CardList();
    	allp.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).getCards());
		allp.addAll(AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).getCards()); 
    	
		for(Card ca : allp) {
			if (ca.hasStartOfKeyword("Permanents don't untap during their controllers' untap steps")) {
	        	int KeywordPosition = ca.getKeywordPosition("Permanents don't untap during their controllers' untap steps");
	        	String parse = ca.getKeyword().get(KeywordPosition).toString();
	    		String k[] = parse.split(":");
	    		final String restrictions[] = k[1].split(",");
				list = list.filter(new CardListFilter() {
		    		public boolean addCard(Card c)
		    		{
		    			return !c.isValidCard(restrictions);
		    		} // filter out cards that should not untap
		    	});
			}
		} // end of Permanents don't untap during their controllers' untap steps
		
    	list = list.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			if( (isWinterOrbInEffect() && c.isLand()) ||
    					(isMunghaWurmInEffect()[0] && c.isLand())) return false;
    			return true;
    		}
    	});
    	list = list.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			if((AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue"))
    					&& c.isArtifact()) return false;
    			return true;
    		}
    	});
    	list = list.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			if((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel")
    					|| AllZoneUtil.isCardInPlay("Intruder Alarm")) && c.isCreature())
    				return false;
    			return true;
    		}
    	});

    	for(Card c : list) {
    		if(c.getKeyword().contains("You may choose not to untap CARDNAME during your untap step.")) {
    			if(c.isTapped()) {
    				if(c.getController().equals(AllZone.HumanPlayer)) {
    					String[] choices = {"Yes", "No"};
    					Object o = AllZone.Display.getChoice("Untap "+c.getName()+"?", choices);
    					String answer = (String) o;
    					if(null != answer && answer.equals("Yes")) {
    						c.untap();
    					}
    				}
    				else {  //computer
    					//computer probably doesn't want to untap based on this ability...
    				}
    			}
    		}
    		else if(isAnZerrinRuinsType(getAnZerrinRuinsTypes(), c)) {
    			//nothing to do, just doesn't let the card untap
    		}
    		else if((c.getCounters(Counters.WIND)>0) && AllZoneUtil.isCardInPlay("Freyalise's Winds")) {
    			//remove a WIND counter instead of untapping
    			c.subtractCounter(Counters.WIND, 1);
    		}
    		else if(!c.getKeyword().contains("CARDNAME doesn't untap during your untap step.")
    				&& !c.getKeyword().contains("This card doesn't untap during your next untap step.")) {
    			c.untap();
    		}
    		else c.removeExtrinsicKeyword("This card doesn't untap during your next untap step.");

    	}
    	if( isWinterOrbInEffect() || isMunghaWurmInEffect()[0] || isMunghaWurmInEffect()[1]) {
    		if( AllZone.Phase.getActivePlayer().equals(AllZone.ComputerPlayer) || isMunghaWurmInEffect()[1] ) {
    			//search for lands the computer has and only untap 1
    			CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.ComputerPlayer);
    			landList = landList.filter(AllZoneUtil.tapped);
    			if( landList.size() > 0 ) {
    				landList.get(0).untap();
    			}
    		}
    		else {
    				
    			Input target = new Input() {
    				private static final long serialVersionUID = 6653677835629939465L;
    				public void showMessage() {
    					AllZone.Display.showMessage("Select one tapped land to untap");
    					ButtonUtil.enableOnlyCancel();
    				}
    				public void selectButtonCancel() {stop();}
    				public void selectCard(Card c, PlayerZone zone) {
    					if(c.isLand() && zone.is(Constant.Zone.Play) && c.isTapped()) {
    						c.untap();
    						stop();
    					}
    				}//selectCard()
    			};//Input
    			CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer);
    			landList = landList.filter(AllZoneUtil.tapped);
    			if( landList.size() > 0 ) {
    				AllZone.InputControl.setInput(target);
    			}
    			
    		}
    	}
    	if( AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue")) {
    		if( AllZone.Phase.getActivePlayer().isComputer() ) {
    			CardList artList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
    			artList = artList.filter(AllZoneUtil.artifacts);
    			artList = artList.filter(AllZoneUtil.tapped);
    			if( artList.size() > 0 ) {
    				CardFactoryUtil.AI_getBestArtifact(artList).untap();
    			}
    		}
    		else {
    			Input target = new Input() {
					private static final long serialVersionUID = 5555427219659889707L;
					public void showMessage() {
    					AllZone.Display.showMessage("Select one tapped artifact to untap");
    					ButtonUtil.enableOnlyCancel();
    				}
    				public void selectButtonCancel() {stop();}
    				public void selectCard(Card c, PlayerZone zone) {
    					if(c.isArtifact() && zone.is(Constant.Zone.Play) 
    							&& c.getController().equals(AllZone.HumanPlayer)) {
    						c.untap();
    						stop();
    					}
    				}//selectCard()
    			};//Input
    			CardList artList = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    			artList = artList.filter(AllZoneUtil.artifacts);
    			artList = artList.filter(AllZoneUtil.tapped);
    			if( artList.size() > 0 ) {
    				AllZone.InputControl.setInput(target);
    			}
    		}
    	}
    	if((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel")) ) {
    		if( AllZone.Phase.getActivePlayer().equals(AllZone.ComputerPlayer) ) {
    			CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
    			creatures = creatures.filter(AllZoneUtil.tapped);
    			if( creatures.size() > 0 ) {
    				creatures.get(0).untap();
    			}
    		}
    		else {
    			Input target = new Input() {
					private static final long serialVersionUID = 5555427219659889707L;
					public void showMessage() {
    					AllZone.Display.showMessage("Select one creature to untap");
    					ButtonUtil.enableOnlyCancel();
    				}
    				public void selectButtonCancel() {stop();}
    				public void selectCard(Card c, PlayerZone zone) {
    					if(c.isCreature() && zone.is(Constant.Zone.Play) 
    							&& c.getController().equals(AllZone.HumanPlayer)) {
    						c.untap();
    						stop();
    					}
    				}//selectCard()
    			};//Input
    			CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
    			creatures = creatures.filter(AllZoneUtil.tapped);
    			if( creatures.size() > 0 ) {
    				AllZone.InputControl.setInput(target);
    			}
    		}
    	}
    }//end doUntap

    private boolean isWinterOrbInEffect() {
    	
    	CardList all = AllZoneUtil.getCardsInPlay("Winter Orb");
    	CardList all2 = AllZoneUtil.getCardsInPlay("Hokori, Dust Drinker");

    	//if multiple Winter Orbs, check that at least 1 of them is untapped
    	for( int i = 0; i < all.size(); i++ ) {
    		if( all.get(i).isUntapped() ) {
    			return true;
    		}
    	}
    	
    	if (all2.size() > 0){
    		return true;
    	}
    	
    	return false;
    }
    
    private boolean[] isMunghaWurmInEffect() {
    	
    	CardList all = AllZoneUtil.getCardsInPlay("Mungha Wurm");
    	
    	boolean[] HumanAI = new boolean[]{false,false};
    	
    	int i = 0;
    	
    	while (i < all.size()) {
    		Card c = all.get(i);
    		if (c.getController().equals(AllZone.HumanPlayer)) {
    			HumanAI[0] = true;
    		} 
    		
    		else if (c.getController().equals(AllZone.ComputerPlayer)) {
    			HumanAI[1] = true;
    		}
    		i++;
    	}
    	
    	return HumanAI;
    }
    
    private ArrayList<String> getAnZerrinRuinsTypes() {
    	ArrayList<String> types = new ArrayList<String>();
    	CardList ruins = AllZoneUtil.getCardsInPlay("An-Zerrin Ruins");
    	for(Card ruin:ruins) {
    		types.add(ruin.getChosenType());
    	}
    	return types;
    }
    
    private boolean isAnZerrinRuinsType(ArrayList<String> types, Card card) {
    	ArrayList<String> cardTypes = card.getType();
    	for(String type:cardTypes) {
    		if(types.contains(type)) return true;
    	}
    	return false;
    }

}