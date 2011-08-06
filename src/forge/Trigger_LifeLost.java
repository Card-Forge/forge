package forge;

import java.util.HashMap;

public class Trigger_LifeLost extends Trigger {

	public Trigger_LifeLost(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),mapParams.get("ValidPlayer").split(","),hostCard))
			{
				return false;
			}
		}
		
		return true;
	}

}
