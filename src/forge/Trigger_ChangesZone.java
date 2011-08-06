package forge;

import java.util.HashMap;

public class Trigger_ChangesZone extends Trigger {

	public Trigger_ChangesZone(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams)
	{
		if(mapParams.containsKey("Origin"))
		{
			if(!mapParams.get("Origin").equals("Any"))
			{
				if(!mapParams.get("Origin").equals((String)runParams.get("Origin")))
				{
					System.out.println("Test failed: Origin (should be" + mapParams.get("Origin") + "but was " + ((String)runParams.get("Origin")) + ")");
					return false;
				}
			}
		}
		
		if(mapParams.containsKey("Destination"))
		{
			if(!mapParams.get("Destination").equals("Any"))
			{
				if(!mapParams.get("Destination").equals((String)runParams.get("Destination")))
				{
					System.out.println("Test failed: Origin (should be" + mapParams.get("Destination") + "but was " + ((String)runParams.get("Destination")) + ")");
					return false;
				}
			}
		}
		
		if(mapParams.containsKey("ValidCard"))
		{
			Card moved = (Card)runParams.get("MovedCard");
			if(!moved.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard))
			{
				System.out.println("Test failed: MovedCard was not valid.");
				return false;
			}
		}
		
		return true;
	}

}
