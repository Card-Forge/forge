package forge;
import java.util.HashMap;



public class Trigger_Blocks extends Trigger {

	public Trigger_Blocks(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) {
		if(mapParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Blocker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		if(mapParams.containsKey("ValidBlocked"))
		{
			if(!matchesValid(runParams.get("Attacker"),mapParams.get("ValidCard").split(","),hostCard))
			{
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_Blocks(mapParams,hostCard);
	}
}
