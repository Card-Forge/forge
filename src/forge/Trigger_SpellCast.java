package forge;

import java.util.HashMap;

public class Trigger_SpellCast extends Trigger {

	public Trigger_SpellCast(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		Card cast = ((Spell)runParams.get("CastSA")).getSourceCard();
		
		if(mapParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(cast.getController(),mapParams.get("ValidPlayer").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidCard"))
		{			
			if(!matchesValid(cast,mapParams.get("ValidCard").split(","),hostCard))
			{
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
					return false;
				}
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		return new Trigger_SpellCast(mapParams,hostCard);
	}
}
