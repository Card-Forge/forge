package forge;
//import java.util.*; //unused

public class InputUtil
{
    //plays activated abilities and instants
    static public void playInstantAbility(Card card, PlayerZone zone)
    {
		if (zone.is(Constant.Zone.Hand, AllZone.HumanPlayer) || zone.is(Constant.Zone.Play) && 
				(card.getController().equals(AllZone.HumanPlayer) || card.canAnyPlayerActivate()))
	    AllZone.GameAction.playCard(card);
    }
    
    //plays activated abilities and any card including land, sorceries, and instants
    static public void playAnyCard(Card card, PlayerZone zone)
    {
		if(zone.is(Constant.Zone.Hand, AllZone.HumanPlayer)){	// activate from hand
			if (card.isLand())
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
						GameAction.playLand(card, zone);
				}
				else //play the land
					GameAction.playLand(card, zone);
			} //land
			else
			{
				AllZone.GameAction.playCard(card);
			}
		}
		// (sol) Can activate if: You control it & it's in play or if your opponent controls it and Any Player Can activate.
		else if (zone.is(Constant.Zone.Play) && 
				(card.getController().equals(AllZone.HumanPlayer) || card.canAnyPlayerActivate()))
				AllZone.GameAction.playCard(card);
    }//selectCard()
    
}