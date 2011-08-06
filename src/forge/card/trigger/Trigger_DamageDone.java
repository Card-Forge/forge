package forge.card.trigger;

import java.util.HashMap;

import forge.Card;

public class Trigger_DamageDone extends Trigger {

	public Trigger_DamageDone(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		Card src = (Card)runParams.get("DamageSource");
		Object tgt = runParams.get("DamageTarget");
		
		if(mapParams.containsKey("ValidSource"))
		{
			if(!src.isValidCard(mapParams.get("ValidSource").split(" "), hostCard.getController(), hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidTarget"))
		{
			if(!matchesValid(tgt,mapParams.get("ValidTarget").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("CombatDamage"))
		{
			if(mapParams.get("CombatDamage").equals("True"))
			{
				if(!((Boolean)runParams.get("IsCombatDamage")))
                    return false;
			}
			else if(mapParams.get("CombatDamage").equals("False"))
			{
				if(((Boolean)runParams.get("IsCombatDamage")))
                    return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_DamageDone(mapParams,hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(Card c)
	{
		c.setTriggeringObject("Source",runParams.get("DamageSource"));
        c.setTriggeringObject("Target",runParams.get("DamageTarget"));
        c.setTriggeringObject("DamageAmount",runParams.get("DamageAmount"));
	}
}
