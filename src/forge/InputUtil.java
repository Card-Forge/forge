package forge;
//import java.util.*; //unused

public class InputUtil
{
    //plays activated abilities and instants
    static public void playInstantAbility(Card card, PlayerZone zone)
    {
    	// let playCard handle 
	    AllZone.GameAction.playCard(card);
    }
    
    //plays activated abilities and any card including land, sorceries, and instants
    static public void playAnyCard(Card card, PlayerZone zone)
    {
    	// let playCard handle 
		AllZone.GameAction.playCard(card);
    }//selectCard()
    
}