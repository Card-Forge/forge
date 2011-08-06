package forge;

import java.util.HashMap;

public class Trigger_Drawn extends Trigger {

	public Trigger_Drawn(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		Card draw = ((Card)runParams.get("Card"));

		if(mapParams.containsKey("ValidCard"))
		{
			if(!draw.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_Drawn(mapParams,hostCard);
	}
	
	@Override
	public Card getTriggeringCard(HashMap<String,Object> runParams)
	{
		return (Card)runParams.get("Card");
	}
}
