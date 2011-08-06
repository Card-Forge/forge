
package forge;


public class Input_Untap extends Input {
    private static final long serialVersionUID = 3452595801560263386L;
    
    @Override
    public void showMessage() {
        //GameActionUtil.executeUpkeepEffects();
        
        PlayerZone p = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
        Card[] c = p.getCards();
        
        AllZone.GameAction.setPlayerTurn(AllZone.Phase.getActivePlayer());
        
        if (AllZone.Phase.getTurn() != 1 && 
        	!(AllZone.Phase.getActivePlayer().equals(Constant.Player.Human) && AllZone.Phase.getTurn() == 2) )
        {
	        for(int i = 0; i < c.length; i++)
	            c[i].setSickness(false);
        }
        
        //if(isMarbleTitanInPlay()) marbleUntap();
        //if(!isStasisInPlay()) doUntap();
        if(!AllZoneUtil.isCardInPlay("Stasis")) doUntap();
        
        if (AllZone.Phase.getTurn() != 1)
            GameActionUtil.executeUpkeepEffects();
        
        //otherwise land seems to stay tapped when it is really untapped
        AllZone.Human_Play.updateObservers();
        
        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_Untap) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    private boolean untapLessThanPower3()
    {
    	//checks for Marble Titan or Meekstone
        return AllZoneUtil.isCardInPlay("Marble Titan") || AllZoneUtil.isCardInPlay("Meekstone");
    }
     
    private void doUntap()
    {
    	PlayerZone p = AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
    	CardList list = new CardList(p.getCards());
    	list = list.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			if( untapLessThanPower3() && isWinterOrbInEffect() ) {
    				return !c.isLand() && c.getNetAttack() < 3;
    			}
    			else if( isWinterOrbInEffect() ) {
    				return !c.isLand();
    			}
    			else if (untapLessThanPower3()) {
    				return c.getNetAttack() < 3;
    			}

    			return true;
    		}
    	});

    	for(Card c : list) {
    		if (c.getBounceAtUntap() && c.getName().contains("Undiscovered Paradise") )
    		{
    			AllZone.GameAction.moveToHand(c);
    		}
    		else if(c.getKeyword().contains("You may choose not to untap CARDNAME during your untap step.")) {
    			if(c.isTapped()) {
    				if(c.getController().equals(Constant.Player.Human)) {
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
    		else if(!c.getKeyword().contains("CARDNAME doesn't untap during your untap step.")
    				&& !c.getKeyword().contains("This card doesn't untap during your next untap step.")) {
    			c.untap();
    		}
    		else c.removeExtrinsicKeyword("This card doesn't untap during your next untap step.");

    	}
    	if( isWinterOrbInEffect() ) {
    		if( AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer) ) {
    			//search for lands the computer has and only untap 1
    			CardList landList = new CardList(p.getCards());
    			landList = landList.filter(new CardListFilter()
    			{
    				public boolean addCard(Card c)
    				{
    					return c.isLand() && c.isTapped();
    				}
    			});
    			if( landList.size() > 0 ) {
    				landList.get(0).untap();
    			}
    		}
    		else {
    			Input target = new Input() {
    				private static final long serialVersionUID = 6653677835629939465L;
    				public void showMessage() {
    					AllZone.Display.showMessage("Winter Orb - Select one tapped land to untap");
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
    			CardList landList = new CardList(p.getCards());
    			landList = landList.filter(new CardListFilter()
    			{
    				public boolean addCard(Card c)
    				{
    					return c.isLand() && c.isTapped();
    				}
    			});
    			if( landList.size() > 0 ) {
    				AllZone.InputControl.setInput(target);
    			}

    		}
    	}
    }//end doUntap

    private boolean isWinterOrbInEffect() {
    	
    	CardList all = AllZoneUtil.getCardsInPlay("Winter Orb");

    	//if multiple Winter Orbs, check that at least 1 of them is untapped
    	for( int i = 0; i < all.size(); i++ ) {
    		if( all.get(i).isUntapped() ) {
    			return true;
    		}
    	}
    	return false;
    }

}