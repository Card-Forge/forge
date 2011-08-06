package forge;

import java.util.HashMap;

public class Trigger_LandPlayed extends Trigger {

	public Trigger_LandPlayed(String n, HashMap<String, String> params, Card host) {
		super(n, params, host);
	}

	public Trigger_LandPlayed(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_LandPlayed(this.name, this.mapParams, this.hostCard);
	}

	@Override
	public Card getTriggeringCard(HashMap<String, Object> runParams) {
		return (Card)runParams.get("Card");
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Card"), mapParams.get("ValidCard").split(","), hostCard))
			{
				return false;
			}
		}
		return true;
	}

}
