package forge;
import java.util.*;

public class Input_Block extends Input
{
  private static final long serialVersionUID = 6120743598368928128L;

  private Card currentAttacker = null;
  private ArrayList<Card> allBlocking = new ArrayList<Card>();

  public void showMessage()
  {
    //for Castle Raptors, since it gets a bonus if untapped
	for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
			Command com = GameActionUtil.commands.get(effect);
			com.execute();
	}
    GameActionUtil.executeCardStateEffects();


    //could add "Reset Blockers" button
    ButtonUtil.enableOnlyOK();

    if(currentAttacker == null)
      AllZone.Display.showMessage("To Block, click on your Opponents attacker first, then your blocker(s)");
    else
      AllZone.Display.showMessage("Select a creature to block " +currentAttacker.getName() +" (" +currentAttacker.getUniqueNumber()  +") ");

    CombatUtil.showCombat();
  }
  public void selectButtonOK()
  {
    ButtonUtil.reset();
    if(AllZone.pwCombat.getAttackers().length == 0)
    {
      
      //AllZone.Phase.nextPhase();
      //for debugging: System.out.println("need to nextPhase(Input_Cleanup.showMessage(), n<=7) = true");
      AllZone.Phase.setNeedToNextPhase(true);
    }
    else
    {
      AllZone.InputControl.setInput(new Input_Block_Planeswalker());
    }
  }
  public void selectCard(Card card, PlayerZone zone)
  {
    //is attacking?
    if(CardUtil.toList(AllZone.Combat.getAttackers()).contains(card))
    {
      currentAttacker = card;
    }
    else if(zone.is(Constant.Zone.Play, Constant.Player.Human) &&
            card.isCreature() &&
            card.isUntapped() &&
            CombatUtil.canBlock(currentAttacker, card))
    {
      if(currentAttacker != null && (! allBlocking.contains(card)))
      {
        allBlocking.add(card);
        AllZone.Combat.addBlocker(currentAttacker, card);
      }
    }
    showMessage();
  }//selectCard()
}
