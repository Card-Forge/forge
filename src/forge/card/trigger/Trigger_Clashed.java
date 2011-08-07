package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 5/16/11
 * Time: 10:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class Trigger_Clashed extends Trigger {

    public Trigger_Clashed(HashMap<String, String> params, Card host) {
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

		if(mapParams.containsKey("Won"))
		{
			if(!mapParams.get("Won").equals(runParams.get("Won")))
                return false;
		}

		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_Clashed(mapParams,hostCard);
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
        //No triggered-variables for you :(
	}
}
