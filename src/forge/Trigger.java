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
	
	public boolean requirementsCheck()
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
				System.out.println("Requirement failed: Location (should be among " + mapParams.get("TriggerZones") + " but was null) (I don't think that should be possible)");
				return false;
			}
			if(!triggerZones.contains(AllZone.getZone(hostCard).getZoneName()))
			{
				System.out.println("Requirement failed: Location (should be among " + mapParams.get("TriggerZones") + " but was " + AllZone.getZone(hostCard).getZoneName() + ")");
				return false;
			}
		}
		
		if(mapParams.containsKey("Metalcraft"))
		{
			if(mapParams.get("Metalcraft").equals("True") && !hostCard.getController().hasMetalcraft())
			{
				System.out.println("Requirement failed: Metalcraft");
				return false;
			}
		}
		
		if(mapParams.containsKey("Threshold"))
		{
			if(mapParams.get("Threshold").equals("True") && !hostCard.getController().hasThreshold())
			{
				System.out.println("Requirement failed: Threshold");
				return false;
			}
		}
		
		if(mapParams.containsKey("PlayersPoisoned"))
		{
			if(mapParams.get("PlayersPoisoned").equals("You") && hostCard.getController().getPoisonCounters() == 0)
			{
				System.out.println("Requirement failed: Poisoned(You)");
				return false;
			}
			else if(mapParams.get("PlayersPoisoned").equals("Opponent") && hostCard.getController().getOpponent().getPoisonCounters() == 0)
			{
				System.out.println("Requirement failed: Poisoned(Opponent)");
				return false;
			}
			else if(mapParams.get("PlayersPoisoned").equals("Each") && !(hostCard.getController().poisonCounters != 0 && hostCard.getController().getPoisonCounters() != 0 ))
			{
				System.out.println("Requirement failed: Poisoned(Each)");
				return false;
			}
		}
		
		if(mapParams.containsKey("ControlsNoValid"))
		{
			for(Card c : AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, hostCard.getController()))
			{
				if(c.isValidCard(mapParams.get("ControlsNoValid").split(","), hostCard.getController(), hostCard))
				{
					System.out.println("Requirement failed: Controlled a forbidden permanent.");
					return false;
				}
			}
		}
		
		if(mapParams.containsKey("ControlsValid"))
		{
			boolean foundValid = false;
			for(Card c : AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, hostCard.getController()))
			{
				if(c.isValidCard(mapParams.get("ControlsValid").split(","), hostCard.getController(), hostCard))
				{
					foundValid = true;
				}
			}
			
			if(!foundValid)
			{
				System.out.println("Requirement failed: Did not control required permanent.");
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
