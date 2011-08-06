package forge;
import java.util.*;

public class PhaseObserver implements Observer
{
  private boolean canPlayLand;

  public PhaseObserver()
  {
    AllZone.Phase.addObserver(this);
  }

  public boolean canPlayLand() {return canPlayLand;}
  public void playedLand()       {canPlayLand = false;}

  public void update(Observable o1, Object o2)
  {
    Player player = AllZone.Phase.getActivePlayer();
    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
    String phase = AllZone.Phase.getPhase();


    if(phase.equals(Constant.Phase.Main1))
    {
      if(player.equals(AllZone.HumanPlayer))
        canPlayLand = true;

      //untap all permanents
      for(int i = 0; i < play.size(); i++)
      {
        play.get(i).untap();
        play.get(i).setSickness(false);
      }
      //TODO: computer needs to draw a card
      if(AllZone.Phase.getTurn() > 1)
        player.drawCard();
    }

    else if(AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers, AllZone.HumanPlayer) &&
            AllZone.Combat.getAttackers().length == 0)
    {
	      
	      //AllZone.Phase.nextPhase();
    	  //for debugging: System.out.println("need to nextPhase(PhaseObserver.update,phase.is(Combat_Declare_Blockers & Human)) = true; Note, this has not been tested, did it work?");
	      AllZone.Phase.setNeedToNextPhase(true);
    }
    else if(AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility, AllZone.HumanPlayer) &&
            AllZone.Combat.getAttackers().length == 0)
    {
    	
        //AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(PhaseObserver.update,phase.is(Combat_Declare_Blockers_InstantAbility & Human)) = true; Note, this has not been tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    else if(AllZone.Phase.is(Constant.Phase.Combat_Damage, AllZone.HumanPlayer) &&
            AllZone.Combat.getAttackers().length == 0)
    {
    	
        //AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(PhaseObserver.update,phase.is(Combat_Damage & Human)) = true; Note, this has not been tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    else if(AllZone.Phase.is(Constant.Phase.Combat_Declare_Attackers, AllZone.HumanPlayer) &&
            AllZone.Combat.getAttackers().length == 0)
    {
      if(PlayerZoneUtil.getCardType(AllZone.Human_Play, "Creature").size() == 0)
      {
    	
        //AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(PhaseObserver.update,phase.is(Combat_Declare_attackers & Human)) = true; Note, this has not been tested, did it work?");
        AllZone.Phase.setNeedToNextPhase(true);
      }
    }

  }//update()
}
