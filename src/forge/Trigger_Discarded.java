package forge;

import java.util.HashMap;

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
		
		if(mapParams.containsKey("ValidCause"))
		{
			if(!matchesValid(runParams.get("Cause"),mapParams.get("ValidSource").split(","),hostCard))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_Discarded(mapParams,hostCard);
	}
}
