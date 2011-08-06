package forge;

import java.util.HashMap;

public class Trigger_Phase extends Trigger {

	public Trigger_Phase(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("Phase"))
		{
			if(!mapParams.get("Phase").equals(runParams.get("Phase")))
			{
				return false;
			}
		}
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),mapParams.get("Player").split(","),hostCard))
			{
				return false;
			}
		}
		return true;
	}

}
