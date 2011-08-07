package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Cost;

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
			SpellAbility sa = ((SpellAbility)runParams.get("CastSA"));
			if(sa.getTarget() == null)
			{
				if(sa.getTargetCard() == null)
				{
					if(sa.getTargetList() == null)
					{
						if(sa.getTargetPlayer() == null)
						{
							return false;
						}
						else
						{
							if(!matchesValid(sa.getTargetPlayer(),mapParams.get("TargetsValid").split(","),hostCard))
							{
								return false;
							}
						}
					}
					else
					{
						boolean validTgtFound = false;
						for(Card tgt : sa.getTargetList())
						{
							if(matchesValid(tgt,mapParams.get("TargetsValid").split(","),hostCard))
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
				}
				else
				{
					if(!matchesValid(sa.getTargetCard(),mapParams.get("TargetsValid").split(","),hostCard))
					{
						return false;
					}
				}
			}
			else
			{
				if(sa.getTarget().doesTarget())
				{
					boolean validTgtFound = false;
					for(Card tgt : sa.getTarget().getTargetCards())
					{
						if(tgt.isValidCard(mapParams.get("TargetsValid").split(","), hostCard.getController(), hostCard))
						{
							validTgtFound = true;
							break;
						}
					}
					
					for(Player p : sa.getTarget().getTargetPlayers())
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
		}
		
		if(mapParams.containsKey("NonTapCost"))
		{			
			Cost cost = (Cost)(runParams.get("Cost"));
			if(cost.getTap()) return false;
		}
		
		return true;
	}

	@Override
	public Trigger getCopy() {
		Trigger copy = new Trigger_SpellAbilityCast(mapParams,hostCard);
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
		sa.setTriggeringObject("Card",((SpellAbility)runParams.get("CastSA")).getSourceCard());
        sa.setTriggeringObject("SpellAbility",runParams.get("CastSA"));
        sa.setTriggeringObject("Player", runParams.get("Player"));
        sa.setTriggeringObject("Activator", runParams.get("Activator"));
	}
}
