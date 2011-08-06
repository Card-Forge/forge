package forge;

import java.util.HashMap;

public class Trigger_DamageDone extends Trigger {

	public Trigger_DamageDone(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		Card src = (Card)runParams.get("DamageSource");
		Object tgt = runParams.get("DamageTarget");
		
		if(mapParams.containsKey("SourceValid"))
		{
			if(!src.isValidCard(mapParams.get("SourceValid").split(" "), hostCard.getController(), hostCard))
			{
				System.out.println("DmgSrc not valid. Fail.");
				return false;
			}
		}
		
		if(mapParams.containsKey("TargetValid"))
		{
			if(!matchesValid(tgt,mapParams.get("TargetValid").split(","),hostCard))
			{
				System.out.println("DmgTgt not valid. Fail.");
				return false;
			}
		}
		
		if(mapParams.containsKey("CombatDamage"))
		{
			if(mapParams.get("CombatDamage").equals("True"))
			{
				if(!(src.isAttacking() || src.isBlocking()))
				{
					System.out.println("DmgSrc not in combat. Fail.");
					return false;
				}
				if(tgt instanceof Card)
				{
					if(!(((Card)tgt).isAttacking() || ((Card)tgt).isBlocking()))
					{
						System.out.println("DmgTgt not in combat. Fail.");
						return false;
					}
				}
			}
		}
		
		return true;
	}

}
