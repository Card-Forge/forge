package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_AttackerBlocked extends Trigger {

	public Trigger_AttackerBlocked(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		if(mapParams.containsKey("ValidBlocker"))
		{
			if(!matchesValid(runParams.get("Blocker"),mapParams.get("ValidBlocker").split(","),hostCard))
			{
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_AttackerBlocked(mapParams,hostCard);
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
        sa.setTriggeringObject("Attacker",runParams.get("Attacker"));
        sa.setTriggeringObject("Blocker",runParams.get("Blocker"));
        sa.setTriggeringObject("NumBlockers", runParams.get("NumBlockers"));
	}
}
