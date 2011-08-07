package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_TapsForMana extends Trigger {

	public Trigger_TapsForMana(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		Card tapper = (Card)runParams.get("Card");

		if(mapParams.containsKey("ValidCard")) {
			if(!tapper.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_TapsForMana(mapParams,hostCard);
		if(overridingAbility != null) {
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(SpellAbility sa) {
		sa.setTriggeringObject("Card", runParams.get("Card"));
		sa.setTriggeringObject("Player", runParams.get("Player"));
		sa.setTriggeringObject("Produced", runParams.get("Produced"));
	}
}
