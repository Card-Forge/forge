
package forge;

import forge.gui.GuiUtils;

public class Input_Mulligan extends Input {
    private static final long serialVersionUID = -8112954303001155622L;
    
    @Override
    public void showMessage() {
        ButtonUtil.enableAll();
        AllZone.Display.getButtonOK().setText("No");
        AllZone.Display.getButtonCancel().setText("Yes");
        AllZone.Display.showMessage("Do you want to Mulligan?");
    }
    
    @Override
    public void selectButtonOK() {
        end();
    }
    
    @Override
    public void selectButtonCancel() {
    	AllZone.GameInfo.setHumanMulliganedToZero(false);
    	
    	CardList hand = AllZoneUtil.getPlayerHand(AllZone.HumanPlayer);
    	for(Card c : hand)
    		AllZone.GameAction.moveToLibrary(c);

        for(int i = 0; i < 100; i++)
            AllZone.HumanPlayer.shuffle();
        
        int newHand = hand.size() - 1;
        
        AllZone.GameInfo.addHumanNumberOfTimesMulliganed(1);
        
        //System.out.println("Mulliganed this turn:" + AllZone.GameInfo.getHumanNumberOfTimesMulliganed());
        
        if(AllZone.QuestData != null)
        {
        	if (AllZone.QuestData.getSleightOfHandLevel() >= 1 && AllZone.GameInfo.getHumanNumberOfTimesMulliganed() == 1)
        		newHand++;
        }
        for(int i = 0; i < newHand; i++)
            AllZone.HumanPlayer.drawCard();
        
        if(newHand == 0) {
        	AllZone.GameInfo.setHumanMulliganedToZero(true);
        	end();
        }
    }//selectButtonOK()
    
    void end() {
    	
        CardList CHandList = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
        PlayerZone CPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
        PlayerZone CHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
        
    	//Computer mulligan
        Card[] hand = AllZone.Computer_Hand.getCards();
        CardList handCards = new CardList(hand);
        Card dummy = handCards.get(0);
        //Computer mulligans if there are no cards with converted mana cost of 0 in its hand
        if(handCards.getValidCards("Card.cmcEQ0",AllZone.ComputerPlayer,dummy).size() == 0) {
	        for(int i = 0; i < hand.length; i++) {
	            AllZone.Computer_Library.add(hand[i]);
	            AllZone.Computer_Hand.remove(hand[i]);
	        }
	        
	        for(int i = 0; i < 100; i++)
	            AllZone.ComputerPlayer.shuffle();
	        	        
	        int newHand = hand.length - 1;
	        for(int i = 0; i < newHand; i++)
	            AllZone.ComputerPlayer.drawCard();
        }
        
        //Human Leylines
        ButtonUtil.reset();
        CardList HHandList = AllZoneUtil.getPlayerHand(AllZone.HumanPlayer);

        for(Card c : HHandList){
        	if(c.getName().startsWith("Leyline")) {
        		if (GameActionUtil.showYesNoDialog(c, "Put onto Battlefield?")) 
        			AllZone.GameAction.moveToPlay(c);
        	}
        }

        //Computer Leylines
        for(int i = 0; i < CHandList.size() ; i++) {
        	if(CHandList.get(i).getName().startsWith("Leyline") && !(CHandList.get(i).getName().startsWith("Leyline of Singularity")
        			&& AllZoneUtil.getCardsInPlay("Leyline of Singularity").size() > 0)) {
        		CPlay.add(CHandList.get(i));
        		CHand.remove(CHandList.get(i));
        		AllZone.GameAction.checkStateEffects();
        	}

        }
        if(AllZone.GameAction.Start_Cut == true && !(HHandList.contains(AllZone.GameAction.HumanCut) 
        		|| CHandList.contains(AllZone.GameAction.ComputerCut)))  {
        	AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer),AllZone.GameAction.HumanCut);
        	AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer),AllZone.GameAction.ComputerCut);
        }
        AllZone.GameAction.checkStateEffects();
        Phase.GameBegins = 1;
        AllZone.Phase.setNeedToNextPhase(false);
        stop();
    }
}
