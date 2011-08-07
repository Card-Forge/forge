package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

public class Trigger_Attacks extends Trigger {

	public Trigger_Attacks(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("Alone"))
		{
			CardList otherAttackers = (CardList)runParams.get("OtherAttackers");
			if(otherAttackers == null)				
			{
				return false;
			}
			if(mapParams.get("Alone").equals("True"))
			{
				if(otherAttackers.size() != 0)
				{
					return false;
				}
			}
			else
			{
				if(otherAttackers.size() == 0)
				{
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Attacks(mapParams,hostCard);
		if(overridingAbility != null)
		{
			copy.setOverridingAbility(overridingAbility);
		}
		copy.setName(name);
        copy.setID(ID);
		
		return copy;
	}
	
	@Override
	public void setTriggeringObjects(SpellAbility sa)
	{
		sa.setTriggeringObject("Attacker",runParams.get("Attacker"));
	}
}
