package forge;

import java.util.HashMap;

public class Trigger_Sacrificed extends Trigger {

	public Trigger_Sacrificed(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!((Card)runParams.get("Sacrificed")).isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard))
			{
				System.out.println("Test failed: Card not valid.");
				return false;
			}
		}
		return true;
	}

}
