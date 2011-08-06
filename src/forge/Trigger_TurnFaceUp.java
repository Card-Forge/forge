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
			if(!matchesValid(runParams.get("Card"),mapParams.get("ValidCard").split(","),hostCard))
			{	
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_TurnFaceUp(mapParams,hostCard);
	}
	
	@Override
	public Card getTriggeringCard(HashMap<String,Object> runParams)
	{
		return (Card)runParams.get("Card");
	}
}
