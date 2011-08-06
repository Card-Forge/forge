package forge;

import java.util.HashMap;

public class Trigger_SpellAbilityCast extends Trigger {

	public Trigger_SpellAbilityCast(HashMap<String, String> params, Card host) {
		super(params, host);
	}

	@Override
	public boolean performTest(HashMap<String, Object> runParams) 
	{
		SpellAbility SA = (SpellAbility)runParams.get("CastSA");
		Card cast = SA.getSourceCard();

		if(mapParams.get("Mode").equals("SpellCast"))
		{
			if(!SA.isSpell())
			{
				return false;
			}
		}
		else if(mapParams.get("Mode").equals("AbilityCast"))
		{
			if(!SA.isAbility())
			{
				return false;
			}
		}
		else if(mapParams.get("Mode").equals("SpellAbilityCast"))
		{
			//Empty block for readability.
		}
		
		if(mapParams.containsKey("ValidControllingPlayer"))
		{
			if(!matchesValid(cast.getController(),mapParams.get("ValidControllingPlayer").split(","),hostCard))
			{
				return false;
			}
		}
		
		if(mapParams.containsKey("ValidActivatingPlayer"))
		{
			if(!matchesValid(SA.getActivatingPlayer(),mapParams.get("ValidActivatingPlayer").split(","),hostCard))
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
			if(sp.getTarget() == null)
			{
				return false;
			}
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
		return new Trigger_SpellAbilityCast(mapParams,hostCard);
	}
}
