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
				System.out.println("Attacker = " + ((Card)runParams.get("Attacker")).getName());
				System.out.println("Test failed: Attacker not valid.");
				return false;
			}
		}
		if(mapParams.containsKey("ValidBlocker"))
		{
			if(!matchesValid(runParams.get("Blocker"),mapParams.get("ValidBlocker").split(","),hostCard))
			{
				System.out.println("Blocker = " + ((Card)runParams.get("Blocker")).getName());
				System.out.println("Test failed: Blocker not valid.");
				return false;
			}
		}
		
		return true;
	}

}
