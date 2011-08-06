package forge;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Trigger {
	
	protected HashMap<String,String> mapParams = new HashMap<String,String>();
	public HashMap<String,String> getMapParams()
	{
		return mapParams;
	}
	
	protected SpellAbility overridingAbility = null;
	public SpellAbility getOverridingAbility()
	{
		return overridingAbility;
	}
	public void setOverridingAbility(SpellAbility sa)
	{
		overridingAbility = sa;
	}
	
	protected Card hostCard;
	public Card getHostCard()
	{
		return hostCard;
	}
	public void setHostCard(Card c)
	{
		hostCard = c;
	}
	
	public Trigger(HashMap<String,String> params, Card host)
	{
		mapParams = params;
		hostCard = host;
	}
	
	public String toString()
	{
		return mapParams.get("TriggerDescription");
	}
	
	public boolean zonesCheck()
	{
		if(mapParams.containsKey("TriggerZones"))
		{
			ArrayList<String> triggerZones = new ArrayList<String>();
			for(String s :  mapParams.get("TriggerZones").split(","))
			{
				triggerZones.add(s);
			}
			if(AllZone.getZone(hostCard) == null)
			{
				return false;
			}
			if(!triggerZones.contains(AllZone.getZone(hostCard).getZoneName()))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public boolean requirementsCheck()
	{
		if(mapParams.containsKey("Metalcraft"))
		{
			if(mapParams.get("Metalcraft").equals("True") && !hostCard.getController().hasMetalcraft())
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("Threshold"))
		{
			if(mapParams.get("Threshold").equals("True") && !hostCard.getController().hasThreshold())
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("PlayersPoisoned"))
		{
			if(mapParams.get("PlayersPoisoned").equals("You") && hostCard.getController().getPoisonCounters() == 0)
			{
				return false;
			}
			else if(mapParams.get("PlayersPoisoned").equals("Opponent") && hostCard.getController().getOpponent().getPoisonCounters() == 0)
			{
				return false;
			}
			else if(mapParams.get("PlayersPoisoned").equals("Each") && !(hostCard.getController().poisonCounters != 0 && hostCard.getController().getPoisonCounters() != 0 ))
			{
				return false;
			}
		}
		
		if (mapParams.containsKey("IsPresent")){
			String sIsPresent = mapParams.get("IsPresent");
			String presentCompare = "GE1";
			String presentZone = "Battlefield";
			String presentPlayer = "Any";
			if(mapParams.containsKey("PresentCompare"))
			{
				presentCompare = mapParams.get("PresentCompare");
			}
			if(mapParams.containsKey("PresentZone"))
			{
				presentZone = mapParams.get("PresentZone");
			}
			if(mapParams.containsKey("PresentPlayer"))
			{
				presentPlayer = mapParams.get("PresentPlayer");
			}
			CardList list = new CardList();
			if(presentPlayer.equals("You") || presentPlayer.equals("Any"))
			{
				list.add(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController()));
			}
			if(presentPlayer.equals("Opponent") || presentPlayer.equals("Any"))
			{
				list.add(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController().getOpponent()));
			}
			
			list = list.getValidCards(sIsPresent.split(","), hostCard.getController(), hostCard);
			
			int right = 1;
			String rightString = presentCompare.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
			}
			else {
				right = Integer.parseInt(presentCompare.substring(2));
			}
			int left = list.size();
			
			if (!Card.compare(left, presentCompare, right))
			{
				return false;
			}
				
		}
		
		return true;
	}
	
	protected boolean matchesValid(Object o,String[] valids,Card srcCard)
	{
		if(o instanceof Card)
		{
			Card c = (Card)o;
			return c.isValidCard(valids, srcCard.getController(), srcCard);
		}
		
		if(o instanceof Player)
		{
			for(String v : valids)
			{
				if(v.equalsIgnoreCase("Player"))
				{
					return true;
				}
				if(v.equalsIgnoreCase("Opponent"))
				{
					if(o.equals(srcCard.getController().getOpponent()))
					{
						return true;
					}
				}
				if(v.equalsIgnoreCase("You"))
				{
					return o.equals(srcCard.getController());
				}
			}
		}
		
		return false;
	}
	
	public abstract boolean performTest(HashMap<String,Object> runParams);
}
