package forge.card.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

public abstract class Trigger {

    private static int nextID = 0;

    public static void resetIDs()
    {
        nextID = 50000;
    }

    protected int ID = nextID++;
	
	protected String name;
	public String getName()
	{
		return name;
	}
	public void setName(String n)
	{
		name = n;
	}

    public void setID(int id)
    {
        ID = id;
    }

	protected HashMap<String,String> mapParams = new HashMap<String,String>();
	public HashMap<String,String> getMapParams()
	{
		return mapParams;
	}
	
	protected HashMap<String,Object> runParams;
	public void setRunParams(HashMap<String,Object> rp)
	{
		runParams = rp;
	}
	public HashMap<String,Object> getRunParams()
	{
		return runParams;
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
	
	private HashMap<String, Object> storedTriggeredObjects = null;
	public void setStoredTriggeredObjects(HashMap<String, Object> storedTriggeredObjects) {
		this.storedTriggeredObjects = storedTriggeredObjects;
	}
	public HashMap<String, Object> getStoredTriggeredObjects() {
		return storedTriggeredObjects;
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
	
	public Trigger(String n,HashMap<String,String> params, Card host)
	{
		name = n;
		mapParams = new HashMap<String,String>();
		for(Map.Entry<String,String> entry : params.entrySet())
		{
			mapParams.put(entry.getKey(),entry.getValue());
		}
		hostCard = host;
	}
	
	public Trigger(HashMap<String,String> params, Card host)
	{
		mapParams = new HashMap<String,String>();
		for(Map.Entry<String,String> entry : params.entrySet())
		{
			mapParams.put(entry.getKey(),entry.getValue());
		}
		hostCard = host;
	}
	
	public String toString()
	{
		if (mapParams.containsKey("TriggerDescription")) {
			return mapParams.get("TriggerDescription").replace("CARDNAME", hostCard.getName());
		}
		else return "";
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

    public boolean phasesCheck()
    {
        if(mapParams.containsKey("TriggerPhases"))
        {
            String phases = mapParams.get("TriggerPhases");

        	if (phases.contains("->")){
        		// If phases lists a Range, split and Build Activate String
        		// Combat_Begin->Combat_End (During Combat)
        		// Draw-> (After Upkeep)
        		// Upkeep->Combat_Begin (Before Declare Attackers)

        		String[] split = phases.split("->", 2);
        		phases = AllZone.Phase.buildActivateString(split[0], split[1]);
        	}
            ArrayList<String> triggerPhases = new ArrayList<String>();
            for(String s :  phases.split(","))
			{
				triggerPhases.add(s);
			}
            if(!triggerPhases.contains(AllZone.Phase.getPhase()))
            {
                return false;
            }
        }
        
        if(mapParams.containsKey("PlayerTurn"))
            if(!AllZone.Phase.isPlayerTurn(hostCard.getController()))
                return false;
        
        if(mapParams.containsKey("OpponentTurn"))
            if(AllZone.Phase.isPlayerTurn(hostCard.getController()))
                return false;

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
		
		if(mapParams.containsKey("Hellbent"))
		{
			if(mapParams.get("Hellbent").equals("True") && !hostCard.getController().hasHellbent())
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
			else if(mapParams.get("PlayersPoisoned").equals("Each") && !(hostCard.getController().getPoisonCounters() != 0 && hostCard.getController().getPoisonCounters() != 0 ))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("LifeTotal")){
			String player = mapParams.get("LifeTotal");
			String lifeCompare = "GE1";
			int life = 1;
			
			if(player.equals("You")) {
				life = hostCard.getController().getLife();
			}
			if(player.equals("Opponent")) {
				life = hostCard.getController().getOpponent().getLife();
			}
			
			if(mapParams.containsKey("LifeAmount")) {
				lifeCompare = mapParams.get("LifeAmount");
			}
			
			
			int right = 1;
			String rightString = lifeCompare.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
			}
			else {
				right = Integer.parseInt(lifeCompare.substring(2));
			}
			
			if(!Card.compare(life, lifeCompare, right)) {
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
				list.addAll(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController()));
			}
			if(presentPlayer.equals("Opponent") || presentPlayer.equals("Any"))
			{
				list.addAll(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController().getOpponent()));
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
		
		if (mapParams.containsKey("IsPresent2")){
			String sIsPresent = mapParams.get("IsPresent2");
			String presentCompare = "GE1";
			String presentZone = "Battlefield";
			String presentPlayer = "Any";
			if(mapParams.containsKey("PresentCompare2"))
			{
				presentCompare = mapParams.get("PresentCompare2");
			}
			if(mapParams.containsKey("PresentZone2"))
			{
				presentZone = mapParams.get("PresentZone2");
			}
			if(mapParams.containsKey("PresentPlayer2"))
			{
				presentPlayer = mapParams.get("PresentPlayer2");
			}
			CardList list = new CardList();
			if(presentPlayer.equals("You") || presentPlayer.equals("Any"))
			{
				list.addAll(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController()));
			}
			if(presentPlayer.equals("Opponent") || presentPlayer.equals("Any"))
			{
				list.addAll(AllZoneUtil.getCardsInZone(presentZone,hostCard.getController().getOpponent()));
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
	
	
	public boolean matchesValid(Object o,String[] valids,Card srcCard)
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
				if(v.equalsIgnoreCase("Player") || v.equalsIgnoreCase("Each"))
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
				if(v.equalsIgnoreCase("EnchantedController")) {
					return ((Player)o).isPlayer(srcCard.getEnchantingCard().getController());
				}
			}
		}
		
		return false;
	}
	
	public boolean isSecondary()
	{
		if(mapParams.containsKey("Secondary"))
		{
			if(mapParams.get("Secondary").equals("True"))
				return true;
		}
		return false;
	}

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Trigger))
            return false;

        return this.ID == ((Trigger)o).ID;
    }
	
	public abstract boolean performTest(HashMap<String,Object> runParams);
	
	public abstract Trigger getCopy();
	
	public abstract void setTriggeringObjects(SpellAbility sa);
}
