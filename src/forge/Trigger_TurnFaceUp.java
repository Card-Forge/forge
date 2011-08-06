package forge;

import java.util.HashMap;

public class Trigger_TurnFaceUp extends Trigger {

	public Trigger_TurnFaceUp(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Morpher"),mapParams.get("ValidCard").split(","),hostCard))
			{	
				return false;
			}
		}
		
		return true;
	}

}
