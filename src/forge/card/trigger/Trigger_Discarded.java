package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_Discarded extends Trigger {

	public Trigger_Discarded(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Card"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"), mapParams.get("ValidPlayer").split(","), hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidCause"))
		{
			if(runParams.get("Cause") == null)
			{
				return false;
			}
			if(!matchesValid(runParams.get("Cause"),mapParams.get("ValidCause").split(","),hostCard))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Discarded(mapParams,hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(SpellAbility sa)
	{
		sa.setTriggeringObject("Card",runParams.get("Card"));
	}
}
