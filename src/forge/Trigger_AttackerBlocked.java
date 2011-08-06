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

}
