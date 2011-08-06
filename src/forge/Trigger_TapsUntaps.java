package forge;

import java.util.HashMap;

public class Trigger_TapsUntaps extends Trigger {

	public Trigger_TapsUntaps(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		Card untapper = (Card)runParams.get("Card");
		String Action = (String)runParams.get("Action");
		
		if(mapParams.containsKey("Action"))
		{
			if(!Action.equals(mapParams.get("Action")))
			{
				System.out.println("Test failed: Wrong action. (should be " + mapParams.get("Action") + "but was " + Action);
				return false;
			}
		}
		if(mapParams.containsKey("ValidCard"))
		{
			if(!untapper.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard))
			{
				System.out.println("Test failed: Untapper not valid.");
				return false;
			}
		}
		
		return true;
	}

}
