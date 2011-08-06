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
		Trigger copy = new Trigger_TurnFaceUp(mapParams,hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(Card c)
	{
		c.setTriggeringObject("Card",runParams.get("Card"));
	}
}
