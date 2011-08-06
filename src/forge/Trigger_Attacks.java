package forge;

import java.util.HashMap;

public class Trigger_Attacks extends Trigger {

	public Trigger_Attacks(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("Unblocked"))
		{
			if(mapParams.get("Unblocked").equals("True"))
			{
				if(AllZone.Combat.getBlockers((Card)runParams.get("Battler")).size() != 0)
				{
					return false;
				}
			}
			else
			{
				if(AllZone.Combat.getBlockers((Card)runParams.get("Battler")).size() == 0)
				{
					return false;
				}
			}
		}
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		if(mapParams.containsKey("ValidBlocker"))
		{
			boolean foundValidBlocker = false;
			CardList blockers = AllZone.Combat.getBlockers((Card)runParams.get("Attacker"));
			for(Card c : blockers)
			{
				if(c.isValidCard(mapParams.get("ValidBlocker").split(","), hostCard.getController(), hostCard))
				{
					foundValidBlocker = true;
					break;
				}
			}
			
			if(!foundValidBlocker)
			{
				return false;
			}
		}
		
		return true;
	}

}
