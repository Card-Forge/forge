package forge;

import java.util.ArrayList;

public class PhaseUtil {
	// ******* UNTAP PHASE *****
	public static void handleUntap(){

		Player turn = AllZone.Phase.getPlayerTurn();
        PlayerZone p = AllZone.getZone(Constant.Zone.Play, turn);
        Card[] c = p.getCards();
        
        AllZone.Phase.turnReset();
        
        AllZone.Combat.reset();
        AllZone.Combat.setAttackingPlayer(turn);
        AllZone.Combat.setDefendingPlayer(turn.getOpponent());
        
        AllZone.pwCombat.reset();
        AllZone.pwCombat.setAttackingPlayer(turn);
        AllZone.pwCombat.setDefendingPlayer(turn.getOpponent());
        
        // For tokens a player starts the game with they don't recover from Sum. Sickness on first turn
        if (!turn.isFirstTurn()){
	        for(int i = 0; i < c.length; i++)
	            c[i].setSickness(false);
        }
        turn.setFirstTurn(false);
        
        // Phasing would happen around here
        
        CardList lands = AllZoneUtil.getPlayerLandsInPlay(turn);
        lands = lands.filter(AllZoneUtil.untapped);
        turn.setNumPowerSurgeLands(lands.size());
        
        if(!AllZoneUtil.isCardInPlay("Stasis")) doUntap();
        
        AllZone.GameAction.resetActivationsPerTurn();
        
        //otherwise land seems to stay tapped when it is really untapped
        AllZone.Human_Play.updateObservers();
        
        AllZone.Phase.setNeedToNextPhase(true);
	}
	
    private static void doUntap()
    {
    	PlayerZone p = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getPlayerTurn());
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
	    		final Card card = ca;
				list = list.filter(new CardListFilter() {
		    		public boolean addCard(Card c)
		    		{
		    			return !c.isValidCard(restrictions,card.getController(),card);
		    		} // filter out cards that should not untap
		    	});
			}
		} // end of Permanents don't untap during their controllers' untap steps
		
    	list = list.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			if(canOnlyUntapOneLand() && c.isLand()) return false;
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
    					//if it is controlling something by staying tapped, leave it tapped
    					//if not, untap it
    					if(!(c.getGainControlTargets().size() > 0)) c.untap();
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
    	if( canOnlyUntapOneLand()) {
    		if( AllZone.Phase.getPlayerTurn().equals(AllZone.ComputerPlayer)) {
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
    		if( AllZone.Phase.getPlayerTurn().isComputer() ) {
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
    		if( AllZone.Phase.getPlayerTurn().equals(AllZone.ComputerPlayer) ) {
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

    private static boolean canOnlyUntapOneLand() {
    	CardList orbs = AllZoneUtil.getCardsInPlay("Winter Orb");
    	for(Card c : orbs){
        	//if any Winter Orb is untapped, effect is on
    		if (c.isUntapped())
    			return true;
    	}

    	if (AllZoneUtil.getCardsInPlay("Hokori, Dust Drinker").size() > 0)
    		return true;
    	
    	if (AllZoneUtil.getPlayerCardsInPlay(AllZone.Phase.getPlayerTurn(), "Mungha Wurm").size() > 0)
    		return true;
    	
    	return false;
    }
    
    private static ArrayList<String> getAnZerrinRuinsTypes() {
    	ArrayList<String> types = new ArrayList<String>();
    	CardList ruins = AllZoneUtil.getCardsInPlay("An-Zerrin Ruins");
    	for(Card ruin:ruins) {
    		types.add(ruin.getChosenType());
    	}
    	return types;
    }
    
    private static boolean isAnZerrinRuinsType(ArrayList<String> types, Card card) {
    	ArrayList<String> cardTypes = card.getType();
    	for(String type:cardTypes) {
    		if(types.contains(type)) return true;
    	}
    	return false;
    }
    
	// ******* UPKEEP PHASE *****
	public static void handleUpkeep(){
		if (skipUpkeep()){
	        AllZone.Phase.setNeedToNextPhase(true);
	        return;
		}
		
        if (AllZone.Phase.getTurn() != 1)
            GameActionUtil.executeUpkeepEffects();
	}
	
    public static boolean skipUpkeep()
    {
    	if (AllZoneUtil.isCardInPlay("Eon Hub"))
    		return true;
    	
    	Player turn = AllZone.Phase.getPlayerTurn();
    	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, turn);

    	if (turn.getCards(hand).size() == 0 && AllZoneUtil.isCardInPlay("Gibbering Descent", turn))
    		return true;
    	
    	return false;
    }
    
	// ******* DRAW PHASE *****
	public static void handleDraw(){
    	Player playerTurn = AllZone.Phase.getPlayerTurn();
    	
    	if (skipDraw(playerTurn)){
    		AllZone.Phase.setNeedToNextPhase(true);
    		return;
    	}
		
    	playerTurn.drawCard();
        GameActionUtil.executeDrawStepEffects();
    }
    
	private static boolean skipDraw(Player player){
		// starting player skips his draw
    	if(AllZone.Phase.getTurn() == 1){
            return true;
    	}
    	
    	if (AllZoneUtil.isCardInPlay("Necropotence", player) || AllZoneUtil.isCardInPlay("Yawgmoth's Bargain", player))
    		return true;
    	
    	return false;
	}	
	
	public static boolean isBeforeAttackersAreDeclared() {
		String phase = AllZone.Phase.getPhase();
		return phase.equals(Constant.Phase.Untap) || phase.equals(Constant.Phase.Upkeep)
			|| phase.equals(Constant.Phase.Draw) || phase.equals(Constant.Phase.Main1)
			|| phase.equals(Constant.Phase.Combat_Begin);
	}
	
	public static boolean isBeforeCombatDamage() {
		String phase = AllZone.Phase.getPhase();
		return phase.equals(Constant.Phase.Untap) || phase.equals(Constant.Phase.Upkeep)
			|| phase.equals(Constant.Phase.Draw) || phase.equals(Constant.Phase.Main1)
			|| phase.equals(Constant.Phase.Combat_Begin)
			|| phase.equals(Constant.Phase.Combat_Declare_Attackers)
			|| phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)
			|| phase.equals(Constant.Phase.Combat_Declare_Blockers)
			|| phase.equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility)
			|| phase.equals(Constant.Phase.Combat_FirstStrikeDamage);
	}
}
