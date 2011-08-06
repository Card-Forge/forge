package forge.card.trigger;

import java.util.HashMap;

import forge.Card;

public class Trigger_Unequip extends Trigger {

	public Trigger_Unequip(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		Card equipped = (Card)runParams.get("Card");
		Card equipment = (Card)runParams.get("Equipment");

		if(mapParams.containsKey("ValidCard")) {
			if(!equipped.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
				return false;
			}
		}

		if(mapParams.containsKey("ValidEquipment")) {
			if(!equipment.isValidCard(mapParams.get("ValidEquipment").split(","), hostCard.getController(), hostCard)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Unequip(mapParams,hostCard);
		if(overridingAbility != null) {
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);

		return copy;
	}

	@Override
	public void setTriggeringObjects(Card c) {
		c.setTriggeringObject("Card", runParams.get("Card"));
		c.setTriggeringObject("Equipment", runParams.get("Equipment"));
	}
}
