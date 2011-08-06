public class Input_Cleanup extends Input
{
	private static final long serialVersionUID = -4164275418971547948L;
	
	public void showMessage() 
    {
	ButtonUtil.disableAll();
	int n = AllZone.Human_Hand.getCards().length;

	//MUST showMessage() before stop() or it will overwrite the next Input's message
	AllZone.Display.showMessage("Cleanup Phase: You can only have a maximum of 7 cards, you currently have " +n +" cards in your hand - select a card to discard");

	//goes to the next phase
	if(n <= 7)
	{   
	    CombatUtil.removeAllDamage();
	    
	    //AllZone.Phase.nextPhase();
	    //for debugging: System.out.println("need to nextPhase(Input_Cleanup.showMessage(), n<=7) = true");
	    AllZone.Phase.setNeedToNextPhase(true);
	}	
    }
    public void selectCard(Card card, PlayerZone zone) 
    {
	if(zone.is(Constant.Zone.Hand, Constant.Player.Human))
	{
	    AllZone.GameAction.discard(card);
	    showMessage();
	    //We need a nextPhase here or else it will never get the needToNextPhase from showMessage() (because there isn't a nextPhsae() in the stack).
	    System.out.println("There better not be a nextPhase() on the stack!");
	    if(AllZone.Phase != null)
	    {
	      if(AllZone.Phase.isNeedToNextPhase())
	      {
	    	  AllZone.Phase.setNeedToNextPhase(false);
	    	  AllZone.Phase.nextPhase();
	      }
	    }
	}
    }//selectCard()
}
