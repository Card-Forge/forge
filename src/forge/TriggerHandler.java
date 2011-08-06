package forge;

import java.util.ArrayList;
import java.util.HashMap;

public class TriggerHandler {

	private ArrayList<Trigger> registeredTriggers = new ArrayList<Trigger>();
	
	public static Trigger parseTrigger(String trigParse,Card host)
	{
		HashMap<String,String> mapParams = parseParams(trigParse);
		Trigger ret = null;
		
		String mode = mapParams.get("Mode");
		if(mode.equals("Attacks"))
		{
			ret = new Trigger_Attacks(mapParams,host);
		}
		else if(mode.equals("BeginningOfPhase"))
		{
			ret = new Trigger_BeginningOfPhase(mapParams,host);
		}
		else if(mode.equals("Blocks"))
		{
			ret = new Trigger_Blocks(mapParams,host);
		}
		else if(mode.equals("ChangesZone"))
		{
			ret = new Trigger_ChangesZone(mapParams,host);
		}
		else if(mode.equals("DamageDone"))
		{
			ret = new Trigger_DamageDone(mapParams,host);
		}
		else if(mode.equals("EndOfPhase"))
		{
			ret = new Trigger_EndOfPhase(mapParams,host);
		}
		else if(mode.equals("LifeGained"))
		{
			ret = new Trigger_LifeGained(mapParams,host);
		}
		else if(mode.equals("LifeLost"))
		{
			ret = new Trigger_LifeLost(mapParams,host);
		}
		else if(mode.equals("SpellCast"))
		{
			ret = new Trigger_SpellCast(mapParams,host);
		}
		
		return ret;
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
			if(!regtrig.requirementsCheck())
			{
				continue;
			}
			
			HashMap<String,String> trigParams = regtrig.getMapParams();
			
			if(mode.equals(trigParams.get("Mode")))
			{
				System.out.println("Mode is " + mode);
				if(!regtrig.performTest(runParams))
				{
					continue;
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
}
