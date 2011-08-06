
package forge.gui.input;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.GameActionUtil;
import forge.Phase;

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
        
        if(AllZone.QuestData != null)
        {
        	if (AllZone.QuestData.getInventory().hasItem("Sleight") && AllZone.GameInfo.getHumanNumberOfTimesMulliganed() == 1)
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
    	//Computer mulligan
        CardList CHandList = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);

        Card dummy = CHandList.get(0);
        //Computer mulligans if there are no cards with converted mana cost of 0 in its hand
        while(CHandList.size() > 5 && CHandList.getValidCards("Card.cmcEQ0",AllZone.ComputerPlayer,dummy).size() < 2) {
        	for(Card c : CHandList)
        		AllZone.GameAction.moveToLibrary(c);
	        
	        for(int i = 0; i < 100; i++)
	            AllZone.ComputerPlayer.shuffle();
	        	        
	        int newHand = CHandList.size() - 1;
	        for(int i = 0; i < newHand; i++)
	            AllZone.ComputerPlayer.drawCard();
	        
	        CHandList = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
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
        for(Card c : CHandList){
        	if(c.getName().startsWith("Leyline") && !(c.getName().startsWith("Leyline of Singularity")
        			&& AllZoneUtil.getCardsInPlay("Leyline of Singularity").size() > 0)) {
        		AllZone.GameAction.moveToPlay(c);
        		AllZone.GameAction.checkStateEffects();
        	}

        }
        if(AllZone.GameAction.isStartCut() && !(HHandList.contains(AllZone.GameAction.getHumanCut()) 
        		|| CHandList.contains(AllZone.GameAction.getComputerCut())))  {
        	AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer),AllZone.GameAction.getHumanCut());
        	AllZone.GameAction.moveTo(AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer),AllZone.GameAction.getComputerCut());
        }
        AllZone.GameAction.checkStateEffects();
        Phase.setGameBegins(1);
        AllZone.Phase.setNeedToNextPhase(false);
        stop();
    }
}
