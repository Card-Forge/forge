package forge;

import java.util.HashMap;

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
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(Card c)
	{
        c.setTriggeringObject("Attacker",runParams.get("Attacker"));
        c.setTriggeringObject("Blocker",runParams.get("Blocker"));
	}
}
