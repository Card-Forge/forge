package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

public class Trigger_Shuffled extends Trigger{

    public Trigger_Shuffled(HashMap<String, String> params, Card host) {
		super(params, host);
	}

    @Override
    public boolean performTest(HashMap<String, Object> runParams)
    {
        if(mapParams.containsKey("ValidPlayer"))
        {
            if(!matchesValid(runParams.get("Player"),mapParams.get("ValidPlayer").split(","),hostCard))
            {
                return false;
            }
        }

         return true;
    }

    @Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Shuffled(mapParams,hostCard);
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
		sa.setTriggeringObject("Player",runParams.get("Player"));
	}
}
