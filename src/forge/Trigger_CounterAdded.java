package forge;

import java.util.HashMap;

public class Trigger_CounterAdded extends Trigger {

	public Trigger_CounterAdded(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		Card addedTo = (Card)runParams.get("Card");
		Counters addedType = (Counters)runParams.get("CounterType");

		if(mapParams.containsKey("ValidCard")) {
			if(!addedTo.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
				return false;
			}
		}

		if(mapParams.containsKey("CounterType")) {
			String type = mapParams.get("CounterType");
			if(!type.equals(addedType.toString())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_CounterAdded(mapParams,hostCard);
		if(overridingAbility != null) {
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
