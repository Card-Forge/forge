package forge.card.trigger;
import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;



public class Trigger_Blocks extends Trigger {

	public Trigger_Blocks(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Blocker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		if(mapParams.containsKey("ValidBlocked"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidBlocked").split(","),hostCard))
			{
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Blocks(mapParams,hostCard);
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
        sa.setTriggeringObject("Blocker",runParams.get("Blocker"));
		sa.setTriggeringObject("Attacker",runParams.get("Attacker"));
	}
}
