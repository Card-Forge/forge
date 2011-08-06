package forge;

import java.util.HashMap;

public class AbilityFactory {
	
	private Card hostC = null;
	
	public Card getHostCard()
	{
		return hostC;
	}
		
	private HashMap<String,String> mapParams = new HashMap<String,String>();
	
	public HashMap<String,String> getMapParams()
	{
		return mapParams;
	}
	
	private boolean isAb = false;
	private boolean isSp = false;
	
	public boolean isAbility()
	{
		return isAb;
	}
	
	public boolean isSpell()
	{
		return isSp;
	}
	
	private Ability_Cost abCost = null;
	
	public Ability_Cost getAbCost()
	{
		return abCost;
	}
	
	private boolean isTargeted = false;
	private boolean hasValid = false;
	private Target abTgt = null;
	
	public boolean isTargeted()
	{
		return isTargeted;
	}
	
	public boolean hasValid()
	{
		return hasValid;
	}
	
	public Target getAbTgt()
	{
		return abTgt;
	}
	
	public boolean isCurse(){
		return mapParams.containsKey("IsCurse");
	}

	private boolean hasSubAb = false;
	
	public boolean hasSubAbility()
	{
		return hasSubAb;
	}
	
	private boolean hasSpDesc = false;

	public boolean hasSpDescription()
	{
		return hasSpDesc;
	}

	//*******************************************************
	
	public SpellAbility getAbility(String abString, Card hostCard){
		
		SpellAbility SA = null;
		
		hostC = hostCard;
		
		if (!(abString.length() > 0))
			throw new RuntimeException("AbilityFactory : getAbility -- abString too short in " + hostCard.getName());
		
		String a[] = abString.split("\\|");
		
		if (!(a.length > 1))
			throw new RuntimeException("AbilityFactory : getAbility -- a[] too short in " + hostCard.getName());
			
		for (int i=0; i<a.length; i++)
		{
			String aa[] = a[i].split("\\$");
			
			if (!(aa.length == 2))
				throw new RuntimeException("AbilityFactory : getAbility -- aa.length not 2 in " + hostCard.getName());
			
			mapParams.put(aa[0], aa[1]);
		}
		
		// parse universal parameters
		
		String API = "";
		if (mapParams.containsKey("AB"))
		{
			isAb = true;
			API = mapParams.get("AB");
		}
		else if (mapParams.containsKey("SP"))
		{
			isSp = true;
			API = mapParams.get("SP");
		}
		else
			throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());

		
		if (!mapParams.containsKey("Cost"))
			throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
		abCost = new Ability_Cost(mapParams.get("Cost"), hostCard.getName(), isAb);
		
		
		if (mapParams.containsKey("ValidTgts"))
		{
			hasValid = true;
			isTargeted = true;
		}
		
		if (mapParams.containsKey("ValidCards"))
			hasValid = true;
		
		if (mapParams.containsKey("Tgt"))
		{
			isTargeted = true;
		}
		
		if (isTargeted)
		{
			if (hasValid)
				abTgt = new Target("TgtV", mapParams.get("TgtPrompt"), mapParams.get("ValidTgts").split(","));
			else
				abTgt = new Target(mapParams.get("Tgt"));
			
			if (mapParams.containsKey("TgtZone"))	// if Targeting something not in play, this Key should be set
				abTgt.setZone(mapParams.get("TgtZone"));
		}
		
		hasSubAb = mapParams.containsKey("SubAbility");
		
		hasSpDesc = mapParams.containsKey("SpellDescription");		
		
		// ***********************************
		// Match API keywords
		
		if (API.equals("DealDamage"))
		{
			final int NumDmg[] = {-1};
            final String NumDmgX[] = {"none"};
            String tmpND = mapParams.get("NumDmg");
            if (tmpND.length() > 0)
            {
            	if (tmpND.matches("X"))
            		NumDmgX[0] = hostCard.getSVar(tmpND);
            	
            	else if (tmpND.matches("[0-9][0-9]?"))
            		NumDmg[0] = Integer.parseInt(tmpND);
            }

			AbilityFactory_DealDamage dd =  new AbilityFactory_DealDamage();
            
            if (isAb)
				SA = dd.getAbility(this, NumDmg[0], NumDmgX[0]);
			else if (isSp)
				SA = dd.getSpell(this, NumDmg[0], NumDmgX[0]);
			
            
		}
		
		// additional API keywords here
		if (API.equals("PutCounter")){
			if (isAb)
				SA = AbilityFactory_Counters.createAbilityPutCounters(this);
			if (isSp){
				SA = AbilityFactory_Counters.createSpellPutCounters(this);
			}
		}

		if (API.equals("Fetch")){
			if (isAb)
				SA = AbilityFactory_Fetch.createAbilityFetch(this);
			if (isSp){
				SA = AbilityFactory_Fetch.createSpellFetch(this);
			}
		}
		
		if (API.equals("GainLife")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilityGainLife(this);
			if (isSp){
				SA = AbilityFactory_AlterLife.createSpellGainLife(this);
			}
		}
		
		if (API.equals("LoseLife")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilityLoseLife(this);
			if (isSp){
				SA = AbilityFactory_AlterLife.createSpellLoseLife(this);
			}
		}
		
		if (API.equals("Fog")){
			if (isAb)
				SA = AbilityFactory_Combat.createAbilityFog(this);
			if (isSp){
				SA = AbilityFactory_Combat.createSpellFog(this);
			}
		}
		
		if (API.equals("Bounce")){
			if (isAb)
				SA = AbilityFactory_Bounce.createAbilityBounce(this);
			if (isSp){
				SA = AbilityFactory_Bounce.createSpellBounce(this);
			}
		}
		
		if (API.equals("Untap")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityUntap(this);
			if (isSp){
				SA = AbilityFactory_PermanentState.createSpellUntap(this);
			}
		}
		
		// *********************************************
		// set universal properties of the SpellAbility
        if (hasSpDesc)
        {
        	StringBuilder sb = new StringBuilder(abCost.toString());
        	sb.append(mapParams.get("SpellDescription"));
        	
        	SA.setDescription(sb.toString());
        }

        if (!isTargeted)
        	SA.setStackDescription(hostCard.getName());
        
        // SpellAbility_Restrictions should be added in here
        
        if (mapParams.containsKey("ActivatingZone"))
        	SA.getRestrictions().setActivateZone(mapParams.get("ActivatingZone"));
        
        if (mapParams.containsKey("SorcerySpeed"))
        	SA.getRestrictions().setSorcerySpeed(true);
        
        if (mapParams.containsKey("PlayerTurn"))
        	SA.getRestrictions().setPlayerTurn(true);
        
        if (mapParams.containsKey("AnyPlayer"))
        	SA.getRestrictions().setAnyPlayer(true);
        
        if (mapParams.containsKey("ActivationLimit"))
        	SA.getRestrictions().setActivationLimit(Integer.parseInt(mapParams.get("ActivationLimit")));

        
        return SA;
	}
	
	
}

