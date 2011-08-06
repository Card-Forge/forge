package forge;

import java.util.ArrayList;
import java.util.HashMap;

public class TriggerHandler {

	private ArrayList<Trigger> registeredTriggers = new ArrayList<Trigger>();
	
	public static Trigger parseTrigger(String trigParse,Card host)
	{
		HashMap<String,String> mapParams = parseParams(trigParse);
		return new Trigger(mapParams,host);
	}
	
	private static HashMap<String,String> parseParams(String trigParse)
	{
		HashMap<String,String> mapParams = new HashMap<String,String>();
		
		if(trigParse.length() == 0)
			throw new RuntimeException("TriggerFactory : registerTrigger -- trigParse too short");
		
		String params[] = trigParse.split("\\|");
		
		for(int i=0;i<params.length;i++)
		{
			params[i] = params[i].trim();
		}
		
		for(String param : params)
		{
			String[] splitParam = param.split("\\$");
			for(int i=0;i<splitParam.length;i++)
			{
				splitParam[i] = splitParam[i].trim();
			}
			
			if(splitParam.length != 2)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("TriggerFactory Parsing Error in registerTrigger() : Split length of ");
				sb.append(param).append(" is not 2.");
				throw new RuntimeException(sb.toString());
			}
			
			mapParams.put(splitParam[0], splitParam[1]);
		}
		
		return mapParams;
	}
	
	public void registerTrigger(Trigger trig)
	{
		System.out.println("Registering from " + trig.getHostCard().getName());
		registeredTriggers.add(trig);
	}
	
	public void clearRegistered()
	{
		registeredTriggers.clear();
	}
	
	public void removeRegisteredTrigger(Trigger trig)
	{
		registeredTriggers.remove(trig);
	}
	
	public void removeAllFromCard(Card crd)
	{
		System.out.println("Removing all registered triggers originating from card: " + crd.getName());
		for(int i=0;i<registeredTriggers.size();i++)
		{
			if(registeredTriggers.get(i).getHostCard().equals(crd))
			{
				registeredTriggers.remove(i);
				i--;
			}
		}
	}
	
	public void runTrigger(String mode,HashMap<String,Object> runParams)
	{
		for(Trigger regtrig : registeredTriggers)
		{
			if(!requirementsCheck(regtrig))
			{
				continue;
			}
			
			HashMap<String,String> trigParams = regtrig.getMapParams();
			
			if(mode.equals(trigParams.get("Mode")))
			{
				System.out.println("Mode is " + mode);
				if(mode.equals("DamageDone"))
				{
					if(!DamageDoneTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("ChangesZone"))
				{
					if(!ChangesZoneTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("SpellCast"))
				{
					if(!SpellCastTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("LifeGained"))
				{
					if(!LifeGainedTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("LifeLost"))
				{
					if(!LifeLostTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("BeginningOfPhase"))
				{
					if(!BeginningOfPhaseTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("EndOfPhase"))
				{
					if(!EndOfPhaseTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("Attacks"))
				{
					if(!AttacksTest(regtrig,runParams))
					{
						continue;
					}
				}
				else if(mode.equals("Blocks"))
				{
					if(!BlocksTest(regtrig,runParams))
					{
						continue;
					}
				}
				
				
				//All tests passed, execute ability.
				System.out.println("All tests succeeded.");
				AbilityFactory AF = new AbilityFactory();
				
				SpellAbility sa = regtrig.getOverridingAbility();
				if(sa == null)
				{
					sa = AF.getAbility(regtrig.getHostCard().getSVar(trigParams.get("Execute")), regtrig.getHostCard());
				}
				sa.setActivatingPlayer(regtrig.getHostCard().getController());
				if(sa.getStackDescription().equals(""))
				{
					sa.setStackDescription(trigParams.get("TriggerDescription"));
				}
				if(trigParams.containsKey("Optional"))
				{
					if(trigParams.get("Optional").equals("True"))
					{
						if(regtrig.getHostCard().getController().equals(AllZone.HumanPlayer))
						{
							StringBuilder buildQuestion = new StringBuilder("Use triggered ability of ");
							buildQuestion.append(regtrig.getHostCard().getName()).append("(").append(regtrig.getHostCard().getUniqueNumber()).append(")?");
							if(!GameActionUtil.showYesNoDialog(regtrig.getHostCard(), buildQuestion.toString()))
							{
								System.out.println("Player refused optional activation. Fail.");
								continue;
							}
						}
						else
						{
							if(!sa.canPlayAI())
							{
								System.out.println("AI refused optional activation. Fail.");
								continue;
							}
						}
					}
				}
				AllZone.GameAction.playSpellAbility(sa);
			}			
		}
	}
	
	private boolean DamageDoneTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		HashMap<String,String> trigParams = trigger.getMapParams();
		
		Card hostCard = trigger.getHostCard();
		Card src = (Card)runParams.get("DamageSource");
		Object tgt = runParams.get("DamageTarget");
		
		if(trigParams.containsKey("SourceValid"))
		{
			if(!src.isValidCard(trigParams.get("SourceValid").split(" "), hostCard.getController(), hostCard))
			{
				System.out.println("DmgSrc not valid. Fail.");
				return false;
			}
		}
		
		if(trigParams.containsKey("TargetValid"))
		{
			if(!matchesValid(tgt,trigParams.get("TargetValid").split(","),hostCard))
			{
				System.out.println("DmgTgt not valid. Fail.");
				return false;
			}
		}
		
		if(trigParams.containsKey("CombatDamage"))
		{
			if(trigParams.get("CombatDamage").equals("True"))
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
	
	private boolean ChangesZoneTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		HashMap<String,String> trigParams = trigger.getMapParams();
		
		Card hostCard = trigger.getHostCard();
		
		if(trigParams.containsKey("Origin"))
		{
			if(!trigParams.get("Origin").equals("Any"))
			{
				if(!trigParams.get("Origin").equals((String)runParams.get("Origin")))
				{
					System.out.println("Test failed: Origin (should be" + trigParams.get("Origin") + "but was " + ((String)runParams.get("Origin")) + ")");
					return false;
				}
			}
		}
		
		if(trigParams.containsKey("Destination"))
		{
			if(!trigParams.get("Destination").equals("Any"))
			{
				if(!trigParams.get("Destination").equals((String)runParams.get("Destination")))
				{
					System.out.println("Test failed: Origin (should be" + trigParams.get("Destination") + "but was " + ((String)runParams.get("Destination")) + ")");
					return false;
				}
			}
		}
		
		if(trigParams.containsKey("ValidCard"))
		{
			Card moved = (Card)runParams.get("MovedCard");
			if(!moved.isValidCard(trigParams.get("ValidCard").split(","), hostCard.getController(), hostCard))
			{
				System.out.println("Test failed: MovedCard was not valid.");
				return false;
			}
		}
		
		return true;
	}
	
	private boolean SpellCastTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = trigger.getMapParams();
		
		if(trigParams.containsKey("ValidCard"))
		{
			Card cast = ((Spell)runParams.get("CastSA")).getSourceCard();
			if(!matchesValid(cast,trigParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Cast card was not valid.");
				return false;
			}
		}
		
		if(trigParams.containsKey("TargetsValid"))
		{
			Spell sp = ((Spell)runParams.get("CastSA"));
			if(sp.getTarget().doesTarget())
			{
				boolean validTgtFound = false;
				for(Card tgt : sp.getTarget().getTargetCards())
				{
					if(tgt.isValidCard(trigParams.get("TargetsValid").split(","), hostCard.getController(), hostCard))
					{
						validTgtFound = true;
						break;
					}
				}
				
				for(Player p : sp.getTarget().getTargetPlayers())
				{
					if(matchesValid(p,trigParams.get("TargetsValid").split(","),hostCard))
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
	
	private boolean LifeGainedTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = new HashMap<String,String>();
		
		if(trigParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),trigParams.get("ValidPlayer").split(","),hostCard))
			{
				System.out.println("Test failed: Player was not valid.");
				return false;
			}
		}
		
		return true;
	}
	
	private boolean LifeLostTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = new HashMap<String,String>();
		
		if(trigParams.containsKey("ValidPlayer"))
		{
			if(!matchesValid(runParams.get("Player"),trigParams.get("ValidPlayer").split(","),hostCard))
			{
				System.out.println("Test failed: Player was not valid.");
				return false;
			}
		}
		
		return true;
	}
	
	private boolean BeginningOfPhaseTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = trigger.getMapParams();
		if(trigParams.containsKey("Phase"))
		{
			if(!trigParams.get("Phase").equals(runParams.get("Phase")))
			{
				System.out.println("Test failed: Phase was wrong (should be " + trigParams.get("Phase") + " but was " + runParams.get("Phase")+ ")");
				return false;
			}
		}
		if(trigParams.containsKey("Player"))
		{
			if(!matchesValid(runParams.get("Player"),trigParams.get("Player").split(","),hostCard))
			{
				System.out.println("Test failed: Player was wrong.");
				return false;
			}
		}
		return true;
	}
	
	private boolean EndOfPhaseTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = trigger.getMapParams();
		if(trigParams.containsKey("Phase"))
		{
			if(!trigParams.get("Phase").equals(runParams.get("Phase")))
			{
				System.out.println("Test failed: Phase was wrong (should be " + trigParams.get("Phase") + " but was " + runParams.get("Phase")+ ")");
				return false;
			}
		}
		if(trigParams.containsKey("Player"))
		{
			if(!matchesValid(runParams.get("Player"),trigParams.get("Player").split(","),hostCard))
			{
				System.out.println("Test failed: Player was wrong.");
				return false;
			}
		}
		return true;
	}
	
	private boolean AttacksTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = trigger.getMapParams();
		
		if(trigParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Attacker"),trigParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Attacker not valid.");
				return false;
			}
		}
		
		return true;
	}
	
	private boolean BlocksTest(Trigger trigger,HashMap<String, Object> runParams)
	{
		Card hostCard = trigger.getHostCard();
		HashMap<String,String> trigParams = trigger.getMapParams();
		
		if(trigParams.containsKey("ValidCard"))
		{
			if(!matchesValid(runParams.get("Blocker"),trigParams.get("ValidCard").split(","),hostCard))
			{
				System.out.println("Test failed: Blocker not valid.");
				return false;
			}
		}
		
		return true;
	}
	
	private boolean matchesValid(Object o,String[] valids,Card srcCard)
	{
		if(o instanceof Card)
		{
			Card c = (Card)o;
			return c.isValidCard(valids, srcCard.getController(), srcCard);
		}
		
		if(o instanceof Player)
		{
			for(String v : valids)
			{
				if(v.equalsIgnoreCase("Player"))
				{
					return true;
				}
				if(v.equalsIgnoreCase("Opponent"))
				{
					if(o.equals(srcCard.getController().getOpponent()))
					{
						return true;
					}
				}
				if(v.equalsIgnoreCase("You"))
				{
					return o.equals(srcCard.getController());
				}
			}
		}
		
		return false;
	}
	
	private boolean requirementsCheck(Trigger trigger)
	{
		HashMap<String,String> trigParams = trigger.getMapParams();
		Card hostCard = trigger.getHostCard();
		
		if(trigParams.containsKey("TriggerZones"))
		{
			ArrayList<String> triggerZones = new ArrayList<String>();
			for(String s :  trigParams.get("TriggerZones").split(","))
			{
				triggerZones.add(s);
			}
			if(AllZone.getZone(hostCard) == null)
			{
				System.out.println("Requirement failed: Location (should be among " + trigParams.get("TriggerZones") + " but was null) (I don't think that should be possible)");
				return false;
			}
			if(!triggerZones.contains(AllZone.getZone(hostCard).getZoneName()))
			{
				System.out.println("Requirement failed: Location (should be among " + trigParams.get("TriggerZones") + " but was " + AllZone.getZone(hostCard).getZoneName() + ")");
				return false;
			}
		}
		
		if(trigParams.containsKey("Metalcraft"))
		{
			if(trigParams.get("Metalcraft").equals("True") && !hostCard.getController().hasMetalcraft())
			{
				System.out.println("Requirement failed: Metalcraft");
				return false;
			}
		}
		
		if(trigParams.containsKey("Threshold"))
		{
			if(trigParams.get("Threshold").equals("True") && !hostCard.getController().hasThreshold())
			{
				System.out.println("Requirement failed: Threshold");
				return false;
			}
		}
		
		if(trigParams.containsKey("PlayersPoisoned"))
		{
			if(trigParams.get("PlayersPoisoned").equals("You") && hostCard.getController().getPoisonCounters() == 0)
			{
				System.out.println("Requirement failed: Poisoned(You)");
				return false;
			}
			else if(trigParams.get("PlayersPoisoned").equals("Opponent") && hostCard.getController().getOpponent().getPoisonCounters() == 0)
			{
				System.out.println("Requirement failed: Poisoned(Opponent)");
				return false;
			}
			else if(trigParams.get("PlayersPoisoned").equals("Each") && !(hostCard.getController().poisonCounters != 0 && hostCard.getController().getPoisonCounters() != 0 ))
			{
				System.out.println("Requirement failed: Poisoned(Each)");
				return false;
			}
		}
		
		if(trigParams.containsKey("ControlsNoValid"))
		{
			for(Card c : AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, hostCard.getController()))
			{
				if(c.isValidCard(trigParams.get("ControlsNoValid").split(","), hostCard.getController(), hostCard))
				{
					System.out.println("Requirement failed: Controlled a forbidden permanent.");
					return false;
				}
			}
		}
		
		if(trigParams.containsKey("ControlsValid"))
		{
			boolean foundValid = false;
			for(Card c : AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, hostCard.getController()))
			{
				if(c.isValidCard(trigParams.get("ControlsValid").split(","), hostCard.getController(), hostCard))
				{
					foundValid = true;
				}
			}
			
			if(!foundValid)
			{
				System.out.println("Requirement failed: Did not control required permanent.");
				return false;
			}
		}
		
		return true;
	}
}
