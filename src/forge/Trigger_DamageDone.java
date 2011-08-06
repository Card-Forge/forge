package forge;

import java.util.HashMap;

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
				if(!(src.isAttacking() || src.isBlocking()))
				{
					return false;
				}
				if(tgt instanceof Card)
				{
					if(!(((Card)tgt).isAttacking() || ((Card)tgt).isBlocking()))
					{
						return false;
					}
				}
			}
			else if(mapParams.get("CombatDamage").equals("False"))
			{
				if(src.isAttacking() || src.isBlocking())
				{
					return false;
				}
				if(tgt instanceof Card)
				{
					if(((Card)tgt).isAttacking() || ((Card)tgt).isBlocking())
					{
						return false;
					}
				}
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
		
		return copy;
	}
	
	@Override
	public Card getTriggeringCard(HashMap<String,Object> runParams)
	{
		return null;
	}
}
