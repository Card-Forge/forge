package forge;

import java.util.HashMap;

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
		return new Trigger_Attacks(mapParams,hostCard);
	}
}
