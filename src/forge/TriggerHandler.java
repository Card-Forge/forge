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
			System.out.println("Creating: Attacks.");
			ret = new Trigger_Attacks(mapParams,host);
		}
		else if(mode.equals("Blocks"))
		{
			System.out.println("Creating: Blocks.");
			ret = new Trigger_Blocks(mapParams,host);
		}
		else if(mode.equals("ChangesZone"))
		{
			System.out.println("Creating: ChangesZone.");
			ret = new Trigger_ChangesZone(mapParams,host);
		}
		else if(mode.equals("DamageDone"))
		{
			System.out.println("Creating: DamageDone.");
			ret = new Trigger_DamageDone(mapParams,host);
		}
		else if(mode.equals("Discarded"))
		{
			System.out.println("Creating: Discarded.");
			ret = new Trigger_Discarded(mapParams,host);
		}
		else if(mode.equals("LifeGained"))
		{
			System.out.println("Creating: LifeGained.");
			ret = new Trigger_LifeGained(mapParams,host);
		}
		else if(mode.equals("LifeLost"))
		{
			System.out.println("Creating: LifeLost.");
			ret = new Trigger_LifeLost(mapParams,host);
		}
		else if(mode.equals("Phase"))
		{
			System.out.println("Creating: Phase.");
			ret = new Trigger_Phase(mapParams,host);
		}
		else if(mode.equals("Sacrificed"))
		{
			System.out.println("Creating: Sacrificed.");
			ret = new Trigger_Sacrificed(mapParams,host);
		}
		else if(mode.equals("SpellCast"))
		{
			System.out.println("Creating: SpellCast.");
			ret = new Trigger_SpellCast(mapParams,host);
		}
		else if(mode.equals("Taps"))
		{
			System.out.println("Creating: Taps.");
			ret = new Trigger_Taps(mapParams,host);
		}
		else if(mode.equals("Untaps"))
		{
			System.out.println("Creating: Untaps.");
			ret = new Trigger_Untaps(mapParams,host);
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
		//AP
		for(Trigger regtrig : registeredTriggers)
		{
			if(regtrig.getHostCard().getController().equals(AllZone.Phase.getPlayerTurn()))
			{
				runSingleTrigger(regtrig,mode,runParams);
			}
		}
		
		//NAP
		for(Trigger regtrig : registeredTriggers)
		{
			if(regtrig.getHostCard().getController().equals(AllZone.Phase.getPlayerTurn().getOpponent()))
			{
				runSingleTrigger(regtrig,mode,runParams);
			}
		}
	}
	
	private void runSingleTrigger(final Trigger regtrig, final String mode, final HashMap<String,Object> runParams)
	{
		if(!regtrig.zonesCheck())
		{
			return;
		}
		if(!regtrig.requirementsCheck())
		{
			return;
		}
		
		HashMap<String,String> trigParams = regtrig.getMapParams();
		
		if(mode.equals(trigParams.get("Mode")))
		{
			System.out.println("Mode is " + mode);
			if(!regtrig.performTest(runParams))
			{
				return;
			}				
			
			//All tests passed, execute ability.
			System.out.println("All tests succeeded.");
			AbilityFactory AF = new AbilityFactory();
			
			final SpellAbility[] sa = new SpellAbility[1];
			sa[0] = regtrig.getOverridingAbility();
			if(sa[0] == null)
			{
				sa[0] = AF.getAbility(regtrig.getHostCard().getSVar(trigParams.get("Execute")), regtrig.getHostCard());
			}
			sa[0].setActivatingPlayer(regtrig.getHostCard().getController());
			if(sa[0].getStackDescription().equals(""))
			{
				sa[0].setStackDescription(trigParams.get("TriggerDescription"));
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
							return;
						}
					}
					else
					{
						if(!sa[0].canPlayAI())
						{
							System.out.println("AI refused optional activation. Fail.");
							return;
						}
					}
				}
			}
			
			final Ability wrapperAbility = new Ability(regtrig.getHostCard(),"0") {
				@Override
				public String getStackDescription()
				{					
					return sa[0].getStackDescription();
				}
				
				@Override
				public void resolve()
				{
					if(!regtrig.requirementsCheck())
					{
						return;
					}
					
					sa[0].resolve();
				}
			};
			if(regtrig.getHostCard().getController().equals(AllZone.HumanPlayer))
			{
				AllZone.GameAction.playSpellAbility(wrapperAbility);
			}
			else
			{
				ComputerUtil.playStack(wrapperAbility);
			}
		}
	}
}
