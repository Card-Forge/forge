package forge;
import java.util.*;

@SuppressWarnings("unused") // java.util.*
public class Input_Main extends Input
{
	private static final long serialVersionUID = -2162856359060870957L;
	//Input_Draw changes this
    //public static boolean canPlayLand;
	public static boolean firstLandHasBeenPlayed;
	public static int canPlayNumberOfLands;

    public void showMessage()
    {
	ButtonUtil.enableOnlyOK();

	if(AllZone.Phase.getPhase().equals(Constant.Phase.Main1))
	    AllZone.Display.showMessage("Main 1 Phase: Play any card");
	else
	    AllZone.Display.showMessage("Main 2 Phase: Play any card");
    }
    public void selectButtonOK()
    {
    	//AllZone.Phase.nextPhase();
    	//for debugging: System.out.println("need to nextPhase(Input_Main.selectButtonOK) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    public void selectCard(Card card, PlayerZone zone)
    {
		//these if statements cannot be combined
		if(card.isLand() && zone.is(Constant.Zone.Hand, Constant.Player.Human))
		{
		    if(canPlayNumberOfLands > 0 )
		    {
		    	CardList fastbonds = CardFactoryUtil.getFastbonds(Constant.Player.Human);
		    	if (fastbonds.size() > 0){
		    		if (firstLandHasBeenPlayed)    	
		    		{
		    			for ( Card vard : fastbonds)
		    			{
		    				AllZone.GameAction.getPlayerLife(Constant.Player.Human).subtractLife(1);
		    			}
		    		}
		    	}
		    	InputUtil.playAnyCard(card, zone);
		    	canPlayNumberOfLands--;
		    	firstLandHasBeenPlayed = true;
	            AllZone.GameAction.checkStateEffects();
		    }
	
			//TODO: add code for exploration / fastbond here
		    
		}
		else
		{
	//	    SpellAbility sa = card.getSpellAbility()[0];
	//	    sa.setRandomTargetAI();
	//	    AllZone.Stack.add(sa);
		    InputUtil.playAnyCard(card, zone);
		}
    }//selectCard()
}