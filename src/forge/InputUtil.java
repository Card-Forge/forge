package forge;
//import java.util.*; //unused

public class InputUtil
{
    //plays activated abilities and instants
    static public void playInstantAbility(Card card, PlayerZone zone)
    {
	if(zone.is(Constant.Zone.Hand) || zone.is(Constant.Zone.Play, Constant.Player.Human))
	    AllZone.GameAction.playCard(card);
    }
    
    //plays activated abilities and any card including land, sorceries, and instants
    static public void playAnyCard(Card card, PlayerZone zone)
    {
	if(zone.is(Constant.Zone.Hand, Constant.Player.Human) && 
	    (card.isLand()))
	{
	    AllZone.Human_Hand.remove(card);
	    AllZone.Human_Play.add(card);
	}
	else if(zone.is(Constant.Zone.Hand, Constant.Player.Human) && 
	    card.getManaCost().equals("0"))//for Mox Ruby and the like
	{
	    AllZone.Human_Hand.remove(card);
	    AllZone.Stack.add(card.getSpellAbility()[0]);
	}
	else if (zone.is(Constant.Zone.Hand, Constant.Player.Human) || zone.is(Constant.Zone.Play, Constant.Player.Human))
	    AllZone.GameAction.playCard(card);
    }//selectCard()
}