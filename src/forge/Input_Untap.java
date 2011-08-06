package forge;
public class Input_Untap extends Input
{
  private static final long serialVersionUID = 3452595801560263386L;
	
  public void showMessage()
  {
    //GameActionUtil.executeUpkeepEffects();

    PlayerZone p =  AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
    Card[] c = p.getCards();

    for(int i = 0; i < c.length; i++)
      c[i].setSickness(false);

    if(isMarbleTitanInPlay())
      marbleUntap();
    else if(!isStasisInPlay())
      regularUntap();

    GameActionUtil.executeUpkeepEffects();
    
    //otherwise land seems to stay tapped when it is really untapped
    AllZone.Human_Play.updateObservers();
    
    //AllZone.Phase.nextPhase();
    //for debugging: System.out.println("need to nextPhase(Input_Untap) = true");
    AllZone.Phase.setNeedToNextPhase(true);
  }
  private void marbleUntap()
  {
    PlayerZone p =  AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
    Card[] c = p.getCards();

    for(int i = 0; i < c.length; i++)
      if(c[i].getNetAttack() < 3)
        c[i].untap();
  }
  private boolean isMarbleTitanInPlay() //or Meekstone
  {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());
    
    all = all.filter(new CardListFilter()
    {
		public boolean addCard(Card c) {
			return c.getName().equals("Meekstone") || c.getName().equals("Marble Titan");
		}	
    });
    
    return all.size() > 0;
  }
  
  private boolean isStasisInPlay()
  {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());
    
    all = all.filter(new CardListFilter()
    {
		public boolean addCard(Card c) {
			return c.getName().equals("Stasis");
		}	
    });
    
    return all.size() > 0;
  }
  
 
  
  private void regularUntap()
  {
    PlayerZone p =  AllZone.getZone(Constant.Zone.Play, AllZone.Phase.getActivePlayer());
    Card[] c = p.getCards();

    for(int i = 0; i < c.length; i++)
    {
      if(!c[i].getKeyword().contains("This card doesn't untap during your untap step.") && 
    	 !c[i].getKeyword().contains("This card doesn't untap during your next untap step."))
    	  c[i].untap();
      else
    	  c[i].removeExtrinsicKeyword("This card doesn't untap during your next untap step.");

    }
  }//regularUntap()
}