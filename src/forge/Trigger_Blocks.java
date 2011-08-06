package forge;

import java.util.HashMap;

public class Trigger_Blocks extends Trigger {

	public Trigger_Blocks(HashMap<String, String> params, Card host) {
		super(params, host);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Blocker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Card not valid.");
				return false;
			}
		}		
		return true;
	}

}
