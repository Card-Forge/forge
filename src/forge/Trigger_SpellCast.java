package forge;

import java.util.HashMap;

public class Trigger_SpellCast extends Trigger {

	public Trigger_SpellCast(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		if(mapParams.containsKey("ValidCard"))
		{
			Card cast = ((Spell)runParams.get("CastSA")).getSourceCard();
			if(!matchesValid(cast,mapParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Cast card was not valid.");
				return false;
			}
		}
		
		if(mapParams.containsKey("TargetsValid"))
		{
			Spell sp = ((Spell)runParams.get("CastSA"));
			if(sp.getTarget().doesTarget())
			{
				boolean validTgtFound = false;
				for(Card tgt : sp.getTarget().getTargetCards())
				{
					if(tgt.isValidCard(mapParams.get("TargetsValid").split(","), hostCard.getController(), hostCard))
					{
						validTgtFound = true;
						break;
					}
				}
				
				for(Player p : sp.getTarget().getTargetPlayers())
				{
					if(matchesValid(p,mapParams.get("TargetsValid").split(","),hostCard))
					{
						validTgtFound = true;
						break;
					}
				}
				
				if(!validTgtFound)
				{
					System.out.println("Test failed: Cast card did not target a valid card.");
					return false;
				}
			}
			else
			{
				System.out.println("Test failed: Cast card did not target but it should have.");
				return false;
			}
		}
		
		return true;
	}

}
