package forge;

import java.util.HashMap;

public class Trigger_LifeGained extends Trigger {

	public Trigger_LifeGained(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{		
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),mapParams.get("ValidPlayer").split(","),hostCard))
			{
				System.out.println("Test failed: Player was not valid.");
				return false;
			}
		}
		
		return true;
	}

}
