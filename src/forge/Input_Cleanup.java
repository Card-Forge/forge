
package forge;

public class Input_Cleanup extends Input {
    private static final long serialVersionUID = -4164275418971547948L;

    @Override
    public void showMessage() {
    	if (AllZone.Phase.getPlayerTurn().isComputer()){
    		AI_CleanupDiscard();
    		return;
    	}
    	
        ButtonUtil.disableAll();
        int n = AllZone.Human_Hand.getCards().length;
        
        //MUST showMessage() before stop() or it will overwrite the next Input's message
        StringBuffer sb = new StringBuffer();
        sb.append("Cleanup Phase: You can only have a maximum of ").append(AllZone.HumanPlayer.getMaxHandSize());
        sb.append(" cards, you currently have ").append(n).append(" cards in your hand - select a card to discard");
        AllZone.Display.showMessage(sb.toString());
        
        //goes to the next phase
        if(n <= AllZone.HumanPlayer.getMaxHandSize() || AllZone.HumanPlayer.getMaxHandSize() == -1) {
            CombatUtil.removeAllDamage();
            
            //for debugging: System.out.println("need to nextPhase(Input_Cleanup.showMessage(), n<=7) = true");
            AllZone.Phase.setNeedToNextPhase(true);
        }
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        if(zone.is(Constant.Zone.Hand, AllZone.HumanPlayer)) {
            card.getController().discard(card, null);
            if (AllZone.Stack.size() == 0)
            	showMessage();
        }
    }//selectCard()
    
    
    public void AI_CleanupDiscard(){
    	int size = AllZone.Computer_Hand.getCards().length;
    	
    	if (AllZone.ComputerPlayer.getMaxHandSize() != -1){
    		int numDiscards = size - AllZone.ComputerPlayer.getMaxHandSize(); 
    		AllZone.ComputerPlayer.discard(numDiscards, null);
    	}
        CombatUtil.removeAllDamage();
        
        AllZone.Phase.setNeedToNextPhase(true);
    }
}
