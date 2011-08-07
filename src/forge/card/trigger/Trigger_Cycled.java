package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_Cycled extends Trigger {

	public Trigger_Cycled(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Cycled(mapParams, hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}

	@Override
	public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card",runParams.get("Card"));
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Card"),mapParams.get("ValidCard").split(","), hostCard))
			{
				return false;
			}
		}
		return true;
	}

}
