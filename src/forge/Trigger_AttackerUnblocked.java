package forge;

import java.util.HashMap;

public class Trigger_AttackerUnblocked extends Trigger {

	public Trigger_AttackerUnblocked(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_AttackerUnblocked(mapParams,hostCard);
	}
	
	@Override
	public Card getTriggeringCard(HashMap<String,Object> runParams)
	{
		return (Card)runParams.get("Attacker");
	}
}
