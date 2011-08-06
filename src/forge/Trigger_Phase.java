package forge;

import java.util.HashMap;

public class Trigger_Phase extends Trigger {

	public Trigger_Phase(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("Part"))
		{
			if(!mapParams.get("Part").equals(runParams.get("Part")))
			{
				System.out.println("Test failed: Phase part was wrong (should be " + mapParams.get("Part") + " but was " + runParams.get("Part") + ")");
				return false;
			}
		}
		if(mapParams.containsKey("Phase"))
		{
			if(!mapParams.get("Phase").equals(runParams.get("Phase")))
			{
				System.out.println("Test failed: Phase was wrong (should be " + mapParams.get("Phase") + " but was " + runParams.get("Phase")+ ")");
				return false;
			}
		}
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),mapParams.get("Player").split(","),hostCard))
			{
				System.out.println("Test failed: Player was wrong.");
				return false;
			}
		}
		return true;
	}

}
