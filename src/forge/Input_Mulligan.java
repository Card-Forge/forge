
package forge;

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
    	
        Card[] hand = AllZone.Human_Hand.getCards();
        for(int i = 0; i < hand.length; i++) {
            AllZone.Human_Library.add(hand[i]);
            AllZone.Human_Hand.remove(hand[i]);
        }
        
        for(int i = 0; i < 100; i++)
            AllZone.HumanPlayer.shuffle();
        
        
        int newHand = hand.length - 1;
        
        AllZone.GameInfo.addHumanNumberOfTimesMulliganed(1);
        
        //System.out.println("Mulliganed this turn:" + AllZone.GameInfo.getHumanNumberOfTimesMulliganed());
        
        if(AllZone.QuestData != null)
        {
        	if (AllZone.QuestData.getSleightOfHandLevel() >= 1 && AllZone.GameInfo.getHumanNumberOfTimesMulliganed() == 1)
        		newHand = hand.length;
        }
        for(int i = 0; i < newHand; i++)
            AllZone.HumanPlayer.drawCard();
        
        if(newHand == 0) {
        	AllZone.GameInfo.setHumanMulliganedToZero(true);
        	end();
        }
    }//selectButtonOK()
    
    void end() {
        ButtonUtil.reset();
        CardList HHandList = new CardList(AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer).getCards());
        PlayerZone HPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
        PlayerZone HHand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
        for(int i = 0; i < HHandList.size() ; i++) {
        	if(HHandList.get(i).getName().startsWith("Leyline")) {
            String[] choices = {"Yes", "No"};
            Object q = null;
            q = AllZone.Display.getChoiceOptional("Put " + HHandList.get(i).getName() + " into play?", choices);
            if(q == null || q.equals("No"));
            else {
            	HPlay.add(HHandList.get(i));
            	HHand.remove(HHandList.get(i));
            }
        }
        }
        CardList CHandList = new CardList(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        PlayerZone CPlay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
        PlayerZone CHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
        for(int i = 0; i < CHandList.size() ; i++) {
        	if(CHandList.get(i).getName().startsWith("Leyline") && (AllZoneUtil.getCardsInPlay("Leyline of Singularity").size() == 0)) {
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
