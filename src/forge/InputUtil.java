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
		
		//hacky stuff: see if there's cycling/transmute/other hand abilities on the land:
		SpellAbility[] sa = card.getSpellAbility();
		if (sa.length > 0)
		{
			int count = 0;
			for (SpellAbility s : sa)
			{
				if (s.canPlay() && (s instanceof Ability_Hand))
					count++;
			}
			if (count > 0)
				AllZone.GameAction.playCard(card);
			else //play the land
			{
				AllZone.Human_Hand.remove(card);
				AllZone.Human_Play.add(card);
				AllZone.GameInfo.addHumanCanPlayNumberOfLands(-1);
				AllZone.GameInfo.setHumanPlayedFirstLandThisTurn(true);
		    	
			}
		}
		else //play the land
		{
			AllZone.Human_Hand.remove(card);
			AllZone.Human_Play.add(card);
			AllZone.GameInfo.addHumanCanPlayNumberOfLands(-1);
			AllZone.GameInfo.setHumanPlayedFirstLandThisTurn(true);
		}
	} //land
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