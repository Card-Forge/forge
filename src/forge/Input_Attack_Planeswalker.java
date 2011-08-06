package forge;
public class Input_Attack_Planeswalker extends Input
{
  private static final long serialVersionUID = 5738375759147611797L;

  public void showMessage()
  {
    ButtonUtil.enableOnlyOK();
    AllZone.Display.showMessage("Planeswalker Declare Attackers:\r\nSelect creatures that you want to attack " +AllZone.pwCombat.getPlaneswalker());
    
    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    CardList creats = new CardList(play.getCards());
    creats = creats.getType("Creature");
    CardList attackers = new CardList(AllZone.Combat.getAttackers());
    
	for (int i = 0;i<creats.size(); i++)
	{
	   	Card c = creats.get(i);
	   	if (CombatUtil.canAttack(c) && c.getKeyword().contains("This card attacks each turn if able.")
	   		&& !attackers.contains(c))
	   	{
	   		AllZone.pwCombat.addAttacker(c);
	   		if (!c.getKeyword().contains("Vigilance"))
	   				c.tap();
	   	}
	}
  }
  public void selectButtonOK()
  {
    
	//AllZone.Phase.nextPhase();
	//for debugging: System.out.println("need to nextPhase(Input_Attack_Planeswalker.selectButtonOK) = true; Note, this has not been tested, did it work?");
	AllZone.Phase.setNeedToNextPhase(true);
    this.stop();
  }
  public void selectCard(Card card, PlayerZone zone)
  {
    if(zone.is(Constant.Zone.Play, Constant.Player.Human) &&
       card.isCreature()    &&
       card.isUntapped()    &&
       CombatUtil.canAttack(card)
       )
    {
      if(! card.getKeyword().contains("Vigilance"))
      {
        card.tap();

        //otherwise cards stay untapped, not sure why this is needed but it works
        AllZone.Human_Play.updateObservers();
      }
      AllZone.pwCombat.addAttacker(card);

      //for Castle Raptors, since it gets a bonus if untapped
      for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
			Command com = GameActionUtil.commands.get(effect);
			com.execute();
	  }
      
      GameActionUtil.executeCardStateEffects();

      CombatUtil.showCombat();
    }
  }//selectCard()
}