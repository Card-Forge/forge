package forge;

import java.util.HashMap;

public class Trigger_Battles extends Trigger {

	public Trigger_Battles(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("Side"))
		{
			if(!mapParams.get("Side").equals(((String)runParams.get("Side"))))
			{
				System.out.println("Test failed: Battler should be " + mapParams.get("Side") + " but was " + ((String)runParams.get("Side")) + ".");
				return false;
			}
		}
		if(mapParams.containsKey("Unblocked"))
		{
			if(mapParams.get("Unblocked").equals("True"))
			{
				if(AllZone.Combat.getBlockers((Card)runParams.get("Battler")).size() != 0)
				{
					System.out.println("Test failed: Battler should've been unblocked.");
					return false;
				}
			}
			else
			{
				if(AllZone.Combat.getBlockers((Card)runParams.get("Battler")).size() == 0)
				{
					System.out.println("Test failed: Battler should've been blocked.");
					return false;
				}
			}
		}
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Battler"),mapParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Battler not valid.");
				return false;
			}
		}
		
		return true;
	}

}
