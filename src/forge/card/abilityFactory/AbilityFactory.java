package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_Restriction;
import forge.card.spellability.Target;

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
	private boolean isDb = false;
	
	public boolean isAbility()
	{
		return isAb;
	}
	
	public boolean isSpell()
	{
		return isSp;
	}
	
	public boolean isDrawback() {
		return isDb;
	}
	
	private Cost abCost = null;
	
	public Cost getAbCost()
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
	
	private String API = "";
	public String getAPI() { return API; }
	
	//*******************************************************
	
	public HashMap<String,String> getMapParams(String abString, Card hostCard) {
		HashMap<String,String> mapParameters = new HashMap<String,String>();
		
		if (!(abString.length() > 0))
			throw new RuntimeException("AbilityFactory : getAbility -- abString too short in " + hostCard.getName());
		
		String a[] = abString.split("\\|");
		
		for (int aCnt = 0; aCnt < a.length; aCnt ++)
		    a[aCnt] = a[aCnt].trim();
		
		if (!(a.length > 0))
			throw new RuntimeException("AbilityFactory : getAbility -- a[] too short in " + hostCard.getName());
			
		for (int i=0; i<a.length; i++)
		{
			String aa[] = a[i].split("\\$");
			
			for (int aaCnt = 0; aaCnt < aa.length; aaCnt ++)
		        aa[aaCnt] = aa[aaCnt].trim();
			
			if (aa.length != 2){
				StringBuilder sb = new StringBuilder();
				sb.append("AbilityFactory Parsing Error in getAbility() : Split length of ");
				sb.append(a[i]).append(" in ").append(hostCard.getName()).append(" is not 2.");
				throw new RuntimeException(sb.toString());
			}
			
			mapParameters.put(aa[0], aa[1]);
		}
		
		return mapParameters;
	}
	
	public SpellAbility getAbility(String abString, Card hostCard){
		
		SpellAbility SA = null;
		
		hostC = hostCard;
		
		mapParams = getMapParams(abString, hostCard);
		
		// parse universal parameters

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
		else if (mapParams.containsKey("DB")) {
			isDb = true;
			API = mapParams.get("DB");
		}
		else
			throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());

		if (!isDb){
			if (!mapParams.containsKey("Cost"))
				throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
			abCost = new Cost(mapParams.get("Cost"), hostCard.getName(), isAb);
		}
		
		if (mapParams.containsKey("ValidTgts"))
		{
			hasValid = true;
			isTargeted = true;
		}
		
		if (mapParams.containsKey("Tgt"))
		{
			isTargeted = true;
		}
		
		if (isTargeted)
		{
			String min = mapParams.containsKey("TargetMin") ? mapParams.get("TargetMin") : "1";
			String max = mapParams.containsKey("TargetMax") ? mapParams.get("TargetMax") : "1";
			
			if (hasValid){
				// TgtPrompt now optional
				StringBuilder sb = new StringBuilder();
				if (hostC != null) sb.append(hostC + " - ");
				String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");
				sb.append(prompt);
				abTgt = new Target(hostC,sb.toString(), mapParams.get("ValidTgts").split(","), min, max);
			}
			else
				abTgt = new Target(hostC,mapParams.get("Tgt"), min, max);
			
			if (mapParams.containsKey("TgtZone"))	// if Targeting something not in play, this Key should be set
				abTgt.setZone(mapParams.get("TgtZone"));
			
			// Target Type mostly for Counter: Spell,Activated,Triggered,Ability (or any combination of) 
			// Ability = both activated and triggered abilities
			if (mapParams.containsKey("TargetType"))
				abTgt.setTargetSpellAbilityType(mapParams.get("TargetType"));
			
			// TargetValidTargeting most for Counter: e.g. target spell that targets X.
			if (mapParams.containsKey("TargetValidTargeting"))
				abTgt.setSAValidTargeting(mapParams.get("TargetValidTargeting"));
		}
		
		hasSubAb = mapParams.containsKey("SubAbility");
		
		hasSpDesc = mapParams.containsKey("SpellDescription");		
		
		// ***********************************
		// Match API keywords
		
	      if (API.equals("DealDamage"))
	      {
			AbilityFactory_DealDamage dd = new AbilityFactory_DealDamage(this);

			if (isAb)
				SA = dd.getAbility();
			else if (isSp)
				SA = dd.getSpell();
			else if (isDb)
				SA = dd.getDrawback();
	      }
	      
	      if (API.equals("DamageAll")){
	    	  AbilityFactory_DealDamage dd = new AbilityFactory_DealDamage(this);
	    	  if (isAb)
	    		  SA = dd.getAbilityDamageAll();
	    	  else if (isSp)
	    		  SA = dd.getSpellDamageAll();
	    	  else if (isDb)
	    		  SA = dd.getDrawbackDamageAll();
	      }
		
		if (API.equals("PutCounter")){
			if (isAb)
				SA = AbilityFactory_Counters.createAbilityPutCounters(this);
			else if (isSp)
				SA = AbilityFactory_Counters.createSpellPutCounters(this);
			else if (isDb)
				SA = AbilityFactory_Counters.createDrawbackPutCounters(this);
		}
		
		if (API.equals("PutCounterAll")){
			if (isAb)
				SA = AbilityFactory_Counters.createAbilityPutCounterAll(this);
			else if (isSp)
				SA = AbilityFactory_Counters.createSpellPutCounterAll(this);
			else if (isDb)
				SA = AbilityFactory_Counters.createDrawbackPutCounterAll(this);
		}
		
		if (API.equals("RemoveCounter")){
			if (isAb)
				SA = AbilityFactory_Counters.createAbilityRemoveCounters(this);
			else if (isSp)
				SA = AbilityFactory_Counters.createSpellRemoveCounters(this);
			else if (isDb)
				SA = AbilityFactory_Counters.createDrawbackRemoveCounters(this);
		}
		
		if (API.equals("Proliferate")){
			if (isAb)
				SA = AbilityFactory_Counters.createAbilityProliferate(this);
			else if (isSp)
				SA = AbilityFactory_Counters.createSpellProliferate(this);
			else if (isDb)
				SA = AbilityFactory_Counters.createDrawbackProliferate(this);
		}

		if (API.equals("ChangeZone")){
			if (isAb)
				SA = AbilityFactory_ChangeZone.createAbilityChangeZone(this);
			else if (isSp)
				SA = AbilityFactory_ChangeZone.createSpellChangeZone(this);
			else if (isDb)
				SA = AbilityFactory_ChangeZone.createDrawbackChangeZone(this);
		}
		
		if (API.equals("ChangeZoneAll")){
			if (isAb)
				SA = AbilityFactory_ChangeZone.createAbilityChangeZoneAll(this);
			else if (isSp)
				SA = AbilityFactory_ChangeZone.createSpellChangeZoneAll(this);
			else if (isDb)
				SA = AbilityFactory_ChangeZone.createDrawbackChangeZoneAll(this);
		}
		
		if (API.equals("Pump"))
		{
			AbilityFactory_Pump afPump = new AbilityFactory_Pump(this);
			
			if (isAb)
				SA = afPump.getAbility();
			else if (isSp)
				SA = afPump.getSpell();
			else if (isDb)
				SA = afPump.getDrawback();
			
			if (isAb || isSp)
				hostCard.setSVar("PlayMain1", "TRUE");
		}
		
		if (API.equals("PumpAll")) {
			AbilityFactory_Pump afPump = new AbilityFactory_Pump(this);
			
			if (isAb)
				SA = afPump.getPumpAllAbility();
			else if (isSp)
				SA = afPump.getPumpAllSpell();
			else if (isDb)
				SA = afPump.getPumpAllDrawback();
			
			if (isAb || isSp)
				hostCard.setSVar("PlayMain1", "TRUE");
		}
		
		if (API.equals("GainLife")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilityGainLife(this);
			else if (isSp)
				SA = AbilityFactory_AlterLife.createSpellGainLife(this);
			else if (isDb)
				SA = AbilityFactory_AlterLife.createDrawbackGainLife(this);
		}

		if (API.equals("LoseLife")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilityLoseLife(this);
			else if (isSp)
				SA = AbilityFactory_AlterLife.createSpellLoseLife(this);
			else if (isDb)
				SA = AbilityFactory_AlterLife.createDrawbackLoseLife(this);
		}
		
		if (API.equals("SetLife")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilitySetLife(this);
			else if (isSp)
				SA = AbilityFactory_AlterLife.createSpellSetLife(this);
			else if (isDb)
				SA = AbilityFactory_AlterLife.createDrawbackSetLife(this);
		}
		
		if (API.equals("Poison")){
			if (isAb)
				SA = AbilityFactory_AlterLife.createAbilityPoison(this);
			else if (isSp)
				SA = AbilityFactory_AlterLife.createSpellPoison(this);
			else if (isDb)
				SA = AbilityFactory_AlterLife.createDrawbackPoison(this);
		}
		
		if (API.equals("Fog")){
			if (isAb)
				SA = AbilityFactory_Combat.createAbilityFog(this);
			else if (isSp)
				SA = AbilityFactory_Combat.createSpellFog(this);
			else if (isDb)
				SA = AbilityFactory_Combat.createDrawbackFog(this);
		}
		
		if (API.equals("Untap")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityUntap(this);
			else if (isSp)
				SA = AbilityFactory_PermanentState.createSpellUntap(this);
			else if (isDb)
				SA = AbilityFactory_PermanentState.createDrawbackUntap(this);
		}
		
		if (API.equals("UntapAll")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityUntapAll(this);
			else if (isSp)
				SA = AbilityFactory_PermanentState.createSpellUntapAll(this);
			//else if (isDb)
				//SA = AbilityFactory_PermanentState.createDrawbackUntapAll(this);
		}
		
		if (API.equals("Tap")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityTap(this);
			else if (isSp)
				SA = AbilityFactory_PermanentState.createSpellTap(this);
			else if (isDb)
				SA = AbilityFactory_PermanentState.createDrawbackTap(this);
		}
		
		if (API.equals("TapAll")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityTapAll(this);
			else if (isSp)
				SA = AbilityFactory_PermanentState.createSpellTapAll(this);
			else if (isDb)
				SA = AbilityFactory_PermanentState.createDrawbackTapAll(this);
		}
		
		if (API.equals("TapOrUntap")){
			if (isAb)
				SA = AbilityFactory_PermanentState.createAbilityTapOrUntap(this);
			else if (isSp)
				SA = AbilityFactory_PermanentState.createSpellTapOrUntap(this);
			else if (isDb)
				SA = AbilityFactory_PermanentState.createDrawbackTapOrUntap(this);
		}
		
		if (API.equals("Regenerate")){
			if (isAb)
				SA = AbilityFactory_Regenerate.getAbility(this);
			else if (isSp)
				SA = AbilityFactory_Regenerate.getSpell(this);
		}
		
		if (API.equals("Draw")){
			if (isAb)
				SA = AbilityFactory_ZoneAffecting.createAbilityDraw(this);
			else if (isSp)
				SA = AbilityFactory_ZoneAffecting.createSpellDraw(this);
			else if (isDb)
				SA = AbilityFactory_ZoneAffecting.createDrawbackDraw(this);
		}
		
		if (API.equals("Mill")){
			if (isAb)
				SA = AbilityFactory_ZoneAffecting.createAbilityMill(this);
			else if (isSp)
				SA = AbilityFactory_ZoneAffecting.createSpellMill(this);
			else if (isDb)
				SA = AbilityFactory_ZoneAffecting.createDrawbackMill(this);
		}
		
		if (API.equals("Scry")){
			if (isAb)
				SA = AbilityFactory_ZoneAffecting.createAbilityScry(this);
			else if (isSp)
				SA = AbilityFactory_ZoneAffecting.createSpellScry(this);
			else if (isDb)
				SA = AbilityFactory_ZoneAffecting.createDrawbackScry(this);
		}
		
		if(API.equals("RearrangeTopOfLibrary")){
			if(isAb)
				SA = AbilityFactory_ZoneAffecting.createRearrangeTopOfLibraryAbility(this);
			else if(isSp)
				SA = AbilityFactory_ZoneAffecting.createRearrangeTopOfLibrarySpell(this);
			else if(isDb)
				SA = AbilityFactory_ZoneAffecting.createRearrangeTopOfLibraryDrawback(this);
		}
		
		if (API.equals("Sacrifice")){
			if (isAb)
				SA = AbilityFactory_Sacrifice.createAbilitySacrifice(this);
			else if (isSp)
				SA = AbilityFactory_Sacrifice.createSpellSacrifice(this);
			else if (isDb)
				SA = AbilityFactory_Sacrifice.createDrawbackSacrifice(this);
		}
		
		if (API.equals("Destroy")){
			if (isAb)
				SA = AbilityFactory_Destroy.createAbilityDestroy(this);
			else if (isSp)
				SA = AbilityFactory_Destroy.createSpellDestroy(this);
			else if( isDb ) {
				SA = AbilityFactory_Destroy.createDrawbackDestroy(this);
			}
		}
		
		if (API.equals("DestroyAll")){
			if (isAb)
				SA = AbilityFactory_Destroy.createAbilityDestroyAll(this);
			else if (isSp)
				SA = AbilityFactory_Destroy.createSpellDestroyAll(this);
			else if (isDb)
				SA = AbilityFactory_Destroy.createDrawbackDestroyAll(this);
		}
		
		if (API.equals("Mana")){
			String produced = mapParams.get("Produced");
			if (isAb)
				SA = AbilityFactory_Mana.createAbilityMana(this, produced);
			if (isSp)
				SA = AbilityFactory_Mana.createSpellMana(this, produced);
			if (isDb)
				SA = AbilityFactory_Mana.createDrawbackMana(this, produced);
		}
		
		if (API.equals("ManaReflected")){
			// Reflected mana will have a filler for produced of "1"
			if (isAb)
				SA = AbilityFactory_Mana.createAbilityManaReflected(this, "1");
			if (isSp){	// shouldn't really happen i think?
				SA = AbilityFactory_Mana.createSpellManaReflected(this, "1");
			}
		}
		
		if(API.equals("Token")){
			AbilityFactory_Token AFT = new AbilityFactory_Token(this);
			
			if(isAb)
				SA = AFT.getAbility();
			else if(isSp)
				SA = AFT.getSpell();
			else if(isDb)
				SA = AFT.getDrawback();
		}
		
		if (API.equals("GainControl")) {
			AbilityFactory_GainControl afControl = new AbilityFactory_GainControl(this);
			
			if (isAb)
				SA = afControl.getAbility();
			else if (isSp)
				SA = afControl.getSpell();
		}
		
		if (API.equals("Discard")){
			if (isAb)
				SA = AbilityFactory_ZoneAffecting.createAbilityDiscard(this);
			else if (isSp)
				SA = AbilityFactory_ZoneAffecting.createSpellDiscard(this);
			else if (isDb)
				SA = AbilityFactory_ZoneAffecting.createDrawbackDiscard(this);
		}
		
		if(API.equals("Counter")){
			AbilityFactory_CounterMagic c = new AbilityFactory_CounterMagic(this);
			
			if (isTargeted)	// Since all "Counter" ABs Counter things on the Stack no need for it to be everywhere
				abTgt.setZone("Stack");
			
			if(isAb)
				SA = c.getAbilityCounter(this);
			else if(isSp)
				SA = c.getSpellCounter(this);
		}
		
		if (API.equals("AddTurn")){
			if (isAb)
				SA = AbilityFactory_Turns.createAbilityAddTurn(this);
			else if (isSp)
				SA = AbilityFactory_Turns.createSpellAddTurn(this);
			else if (isDb)
				SA = AbilityFactory_Turns.createDrawbackAddTurn(this);
		}

        if (API.equals("Clash")){
            if(isAb)
                SA = AbilityFactory_Clash.getAbility(this);
            else if (isSp)
                SA = AbilityFactory_Clash.getSpell(this);
            else if(isDb)
                SA = AbilityFactory_Clash.getDrawback(this);

        }
		
		if(API.equals("Animate")) {
			if(isAb)
				SA = AbilityFactory_Animate.createAbilityAnimate(this);
			else if(isSp)
				SA = AbilityFactory_Animate.createSpellAnimate(this);
			else if(isDb)
				SA = AbilityFactory_Animate.createDrawbackAnimate(this);
		}
		
		if (API.equals("Effect")){
			if(isAb)
				SA = AbilityFactory_Effect.createAbilityEffect(this);
			else if(isSp)
				SA = AbilityFactory_Effect.createSpellEffect(this);
			else if(isDb)
				SA = AbilityFactory_Effect.createDrawbackEffect(this);
		}
		
		if (API.equals("WinsGame")){
			if(isAb)
				SA = AbilityFactory_EndGameCondition.createAbilityWinsGame(this);
			else if(isSp)
				SA = AbilityFactory_EndGameCondition.createSpellWinsGame(this);
			else if(isDb)
				SA = AbilityFactory_EndGameCondition.createDrawbackWinsGame(this);
		}
		
		if (API.equals("LosesGame")){
			if(isAb)
				SA = AbilityFactory_EndGameCondition.createAbilityLosesGame(this);
			else if(isSp)
				SA = AbilityFactory_EndGameCondition.createSpellLosesGame(this);
			else if(isDb)
				SA = AbilityFactory_EndGameCondition.createDrawbackLosesGame(this);
		}
		
		if (API.equals("RevealHand")){
			if (isAb)
				SA = AbilityFactory_ZoneAffecting.createAbilityRevealHand(this);
			else if (isSp)
				SA = AbilityFactory_ZoneAffecting.createSpellRevealHand(this);
			else if (isDb)
				SA = AbilityFactory_ZoneAffecting.createDrawbackRevealHand(this);
		}
		
		if (SA == null)
			throw new RuntimeException("AbilityFactory : SpellAbility was not created for "+hostCard.getName()+". Looking for API: "+API);

		// *********************************************
		// set universal properties of the SpellAbility
		
		SA.setAbilityFactory(this);
		
		if(hasSubAbility())
			SA.setSubAbility(getSubAbility());
		
        if (hasSpDesc)
        {
        	StringBuilder sb = new StringBuilder();
        	
        	
        	if (!isDb){	// SubAbilities don't have Costs or Cost descriptors 
	        	if (mapParams.containsKey("PrecostDesc"))
	        		sb.append(mapParams.get("PrecostDesc")).append(" ");
	        	if (mapParams.containsKey("CostDesc"))
	        		sb.append(mapParams.get("CostDesc")).append(" ");
	        	else 
	        		sb.append(abCost.toString());
        	}
        	
	        sb.append(mapParams.get("SpellDescription"));
        	
        	SA.setDescription(sb.toString());
        }
        else
        	SA.setDescription("");
        
        // StackDescriptions are overwritten by the AF type instead of through this
        //if (!isTargeted)
        //	SA.setStackDescription(hostCard.getName());
        
        SA.setRestrictions(buildRestrictions(SA));

        return SA;
	}
	
	// Easy creation of SubAbilities
	public Ability_Sub getSubAbility(){
		Ability_Sub abSub = null;

       String sSub = getMapParams().get("SubAbility");
       
       if (sSub.startsWith("SVar="))
          sSub = getHostCard().getSVar(sSub.split("=")[1]);
       
       if (sSub.startsWith("DB$"))
       {
          AbilityFactory afDB = new AbilityFactory();
          abSub = (Ability_Sub)afDB.getAbility(sSub, getHostCard());
       }
       else{
    	   // Older style Drawback doesn't create an abSub
    	   // on Resolution use getMapParams().get("SubAbility"); to call Drawback
       }

        return abSub;
	}
	
	public SpellAbility_Restriction buildRestrictions(SpellAbility SA){
        // SpellAbility_Restrictions should be added in here
        SpellAbility_Restriction restrict = SA.getRestrictions(); 
        if (mapParams.containsKey("ActivatingZone"))
        	restrict.setActivateZone(mapParams.get("ActivatingZone"));
        
        if (mapParams.containsKey("Flashback")){
        	SA.setFlashBackAbility(true);
        	restrict.setActivateZone("Graveyard");
        }
        
        if (mapParams.containsKey("SorcerySpeed"))
        	restrict.setSorcerySpeed(true);
        
        if (mapParams.containsKey("PlayerTurn"))
        	restrict.setPlayerTurn(true);
        
        if (mapParams.containsKey("OpponentTurn"))
        	restrict.setOpponentTurn(true);
        
        if (mapParams.containsKey("AnyPlayer"))
        	restrict.setAnyPlayer(true);
        
        if (mapParams.containsKey("ActivationLimit"))
        	restrict.setActivationLimit(Integer.parseInt(mapParams.get("ActivationLimit")));
        
        if (mapParams.containsKey("ActivationNumberSacrifice"))
        	restrict.setActivationNumberSacrifice(Integer.parseInt(mapParams.get("ActivationNumberSacrifice")));

        if (mapParams.containsKey("ActivatingPhases")) {
        	String phases = mapParams.get("ActivatingPhases");
        	
        	if (phases.contains("->")){
        		// If phases lists a Range, split and Build Activate String
        		// Combat_Begin->Combat_End (During Combat)
        		// Draw-> (After Upkeep)
        		// Upkeep->Combat_Begin (Before Declare Attackers)
        		
        		String[] split = phases.split("->", 2);
        		phases = AllZone.Phase.buildActivateString(split[0], split[1]);
        	}
        		
        	restrict.setActivatePhases(phases);
        }
        
        if (mapParams.containsKey("ActivatingCardsInHand"))
        	restrict.setActivateCardsInHand(Integer.parseInt(mapParams.get("ActivatingCardsInHand")));
        
        if (mapParams.containsKey("Threshold"))
        	restrict.setThreshold(true);
        
        if (mapParams.containsKey("Planeswalker"))
        	restrict.setPlaneswalker(true);
        
        if (mapParams.containsKey("IsPresent")){
        	restrict.setIsPresent(mapParams.get("IsPresent"));
        	if (mapParams.containsKey("PresentCompare"))
        		restrict.setPresentCompare(mapParams.get("PresentCompare"));
        }
        
        if (mapParams.containsKey("IsNotPresent")){
        	restrict.setIsPresent(mapParams.get("IsNotPresent"));
        	restrict.setPresentCompare("EQ0");
        }
        return restrict;
	}
	
	public static boolean playReusable(SpellAbility sa){
		// TODO probably also consider if winter orb or similar are out

		return (sa.getPayCosts().isReusuableResource() && AllZone.Phase.is(Constant.Phase.End_Of_Turn) 
				&& AllZone.Phase.isNextTurn(AllZone.ComputerPlayer));
	}
	
	public static boolean isSorcerySpeed(SpellAbility sa){
		if (sa.isSpell())
			return sa.getSourceCard().isSorcery();
		else if (sa.isAbility())
			return sa.getRestrictions().getSorcerySpeed();
		
		return false;
	}
	
	// Utility functions used by the AFs
	public static int calculateAmount(Card card, String amount, SpellAbility ability){
		// amount can be anything, not just 'X' as long as sVar exists
		
		// If Amount is -X, strip the minus sign before looking for an SVar of that kind
		int multiplier = 1;
		if (amount.startsWith("-")){
			multiplier = -1;
			amount = amount.substring(1);
		}
		
		if (!card.getSVar(amount).equals(""))
		{
			String calcX[] = card.getSVar(amount).split("\\$");
			if (calcX.length == 1 || calcX[1].equals("none"))
				return 0;
			
			if (calcX[0].startsWith("Count"))
				return CardFactoryUtil.xCount(card, calcX[1]) * multiplier;

			else if (ability != null){
				//Player attribute counting
				if( calcX[0].startsWith("TargetedPlayer")) {
					ArrayList<Player> players = new ArrayList<Player>();
					SpellAbility saTargeting = (ability.getTarget() == null) ?  findParentsTargetedPlayer(ability) : ability;
					if(saTargeting.getTarget() != null) {
						players.addAll(saTargeting.getTarget().getTargetPlayers());
					}
					else {
						players.addAll(getDefinedPlayers(card, saTargeting.getAbilityFactory().getMapParams().get("Defined"), saTargeting));
					}
					return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
				}
				
				CardList list = new CardList();
				if (calcX[0].startsWith("Sacrificed"))
					list = findRootAbility(ability).getSacrificedCost();
				
				else if (calcX[0].startsWith("Discarded"))
					list = findRootAbility(ability).getDiscardedCost();
				
				else if( calcX[0].startsWith("Exiled")) {
					list = findRootAbility(ability).getExiledCost();
				}
				else if( calcX[0].startsWith("Tapped")) {
					list = findRootAbility(ability).getTappedCost();
				}
				
				else if (calcX[0].startsWith("Targeted")){
					Target t = ability.getTarget();
					if(null != t) {
						ArrayList<Object> all = t.getTargets();
						if(!all.isEmpty() && all.get(0) instanceof SpellAbility) {
							SpellAbility saTargeting = findParentsTargetedSpellAbility(ability);
							list = new CardList();
							ArrayList<SpellAbility> sas = saTargeting.getTarget().getTargetSAs();
							for(SpellAbility sa : sas) {
								list.add(sa.getSourceCard());
							}
						}
						else {
							SpellAbility saTargeting = findParentsTargetedCard(ability);
							list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
						}
					}
					else {
						//TODO - this may needs edits to handle SpellAbility defineds?
						list = new CardList(findParentsTargetedCard(ability).getTarget().getTargetCards().toArray());
					}
				}
				else if (calcX[0].startsWith("Triggered")) {
					list = new CardList();
					list.add((Card)ability.getSourceCard().getTriggeringObject(calcX[0].substring(9)));
				}
				else if (calcX[0].startsWith("Remembered")) {
					// Add whole Remembered list to handlePaid
					list = new CardList();
					for(Card c : card.getRemembered())
						list.add(AllZoneUtil.getCardState(c));
				}
				else
					return 0;
				
				return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
			}
			
			else
				return 0;
		}

		return Integer.parseInt(amount) * multiplier;
	}
	
	// should the three getDefined functions be merged into one? Or better to have separate?
	// If we only have one, each function needs to Cast the Object to the appropriate type when using
	// But then we only need update one function at a time once the casting is everywhere.
	// Probably will move to One function solution sometime in the future
	public static ArrayList<Card> getDefinedCards(Card hostCard, String def, SpellAbility sa){
		ArrayList<Card> cards = new ArrayList<Card>();
		String defined = (def == null) ? "Self" : def;	// default to Self
		
		Card c = null; 
		
		if (defined.equals("Self"))
			c = hostCard;
		
		else if (defined.equals("Equipped"))
			c = hostCard.getEquippingCard();

		else if (defined.equals("Enchanted"))
			c = hostCard.getEnchantingCard();

		else if (defined.equals("Targeted")){
			SpellAbility parent = findParentsTargetedCard(sa);
			cards.addAll(parent.getTarget().getTargetCards());
		}
		
		else if (defined.startsWith("Triggered")){
            Object crd = hostCard.getTriggeringObject(defined.substring(9));
            if(crd instanceof Card)
            {
                c = AllZoneUtil.getCardState((Card)crd);
            }
        }
		
		else if (defined.equals("Remembered")){
			for(Card rem : hostCard.getRemembered()){
				// Get current state of each remembered card
				cards.add(AllZoneUtil.getCardState(rem));
			}
		}
			
		
		if (c != null)
			cards.add(c);
		
		return cards;
	}
	
	
	public static ArrayList<Player> getDefinedPlayers(Card card, String def, SpellAbility sa){
		ArrayList<Player> players = new ArrayList<Player>();
		String defined = (def == null) ? "You" : def;
		
		if (defined.equals("Targeted")){
			Target tgt = sa.getTarget();
			SpellAbility parent = sa;

			do{
				
				if (!(parent instanceof Ability_Sub)) // did not find any targets
					return players;
				parent = ((Ability_Sub)parent).getParent();
				tgt = parent.getTarget();
			}while(tgt == null || tgt.getTargetPlayers().size() == 0);
			
			players.addAll(tgt.getTargetPlayers());
		}
		else if (defined.equals("TargetedController")){
			ArrayList<Card> list = getDefinedCards(card, "Targeted", sa);
			ArrayList<SpellAbility> sas = getDefinedSpellAbilities(card, "Targeted", sa);

			for(Card c : list){
				Player p = c.getController();
				if (!players.contains(p))
					players.add(p);
			}
			for(SpellAbility s : sas) {
				Player p = s.getSourceCard().getController();
				if(!players.contains(p)) players.add(p);
			}
		}
		else if (defined.equals("TargetedOwner")){
			ArrayList<Card> list = getDefinedCards(card, "Targeted", sa);

			for(Card c : list){
				Player p = c.getOwner();
				if (!players.contains(p))
					players.add(p);
			}
		}
        else if (defined.startsWith("Triggered")){
            Object o = null;
            if (defined.endsWith("Controller")){
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0,triggeringType.length()-10);
                Object c = sa.getSourceCard().getTriggeringObject(triggeringType);
                if(c instanceof Card)
                {
                    o = ((Card)c).getController();
                }
            }
            else if (defined.endsWith("Owner")){
                String triggeringType = defined.substring(9);
                triggeringType = triggeringType.substring(0,triggeringType.length()-5);
                Object c = (Card)sa.getSourceCard().getTriggeringObject(triggeringType);
                if(c instanceof Card)
                {
                    o = ((Card)c).getOwner();
                }
		    }
            else {
                String triggeringType = defined.substring(9);
                o = sa.getSourceCard().getTriggeringObject(triggeringType);
            }
            if(o != null)
            {
                if(o instanceof Player)
                {
                    Player p = (Player)o;
                    if (!players.contains(p))
                        players.add(p);
                }
            }
        }
		else if (defined.equals("EnchantedController")){
			Player p = card.getEnchantingCard().getController();
			if (!players.contains(p))
				players.add(p);
		}
		else if (defined.equals("EnchantedOwner")){
			Player p = card.getEnchantingCard().getOwner();
			if (!players.contains(p))
				players.add(p);
		}
		else if (defined.equals("AttackingPlayer")){
			Player p = AllZone.Combat.getAttackingPlayer();
			if (!players.contains(p))
				players.add(p);
		}
		else if (defined.equals("DefendingPlayer")){
			Player p = AllZone.Combat.getDefendingPlayer();
			if (!players.contains(p))
				players.add(p);
		}
		else{
			if (defined.equals("You") || defined.equals("Each"))
				players.add(sa.getActivatingPlayer());
			
			if (defined.equals("Opponent") || defined.equals("Each"))
				players.add(sa.getActivatingPlayer().getOpponent());
		}
		return players;
	}
	
	public static ArrayList<SpellAbility> getDefinedSpellAbilities(Card card, String def, SpellAbility sa){
		ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
		String defined = (def == null) ? "Self" : def;	// default to Self
		
		SpellAbility s = null; 
		
		//TODO - this probably needs to be fleshed out a bit, but the basics work
		if (defined.equals("Self")) {
			s = sa;
		}
		else if(defined.equals("Targeted")) {
			SpellAbility parent = findParentsTargetedSpellAbility(sa);
			sas.addAll(parent.getTarget().getTargetSAs());
		}
		
		if (s != null)
			sas.add(s);
		
		return sas;
	}
	
	public static ArrayList<Object> getDefinedObjects(Card card, String def, SpellAbility sa){
		ArrayList<Object> objects = new ArrayList<Object>();
		String defined = (def == null) ? "Self" : def;
		
		objects.addAll(getDefinedPlayers(card, defined, sa));
		objects.addAll(getDefinedCards(card, defined, sa));
		objects.addAll(getDefinedSpellAbilities(card, defined, sa));
		return objects;
	}
	
	
	public static SpellAbility findRootAbility(SpellAbility sa){
		SpellAbility parent = sa;
		while (parent instanceof Ability_Sub)
			parent = ((Ability_Sub)parent).getParent();
		
		return parent;
	}
	
	public static SpellAbility findParentsTargetedCard(SpellAbility sa){
		SpellAbility parent = sa;
		
		do{
			if (!(parent instanceof Ability_Sub))
				return parent;
			parent = ((Ability_Sub)parent).getParent();
		}while(parent.getTarget() == null || parent.getTarget().getTargetCards().size() == 0);
		
		return parent;
	}
	
	private static SpellAbility findParentsTargetedSpellAbility(SpellAbility sa){
		SpellAbility parent = sa;
		
		do{
			if (!(parent instanceof Ability_Sub))
				return parent;
			parent = ((Ability_Sub)parent).getParent();
		}while(parent.getTarget() == null || parent.getTarget().getTargetSAs().size() == 0);
		
		return parent;
	}
	
	public static SpellAbility findParentsTargetedPlayer(SpellAbility sa){
		SpellAbility parent = sa;
		
		do{
			if (!(parent instanceof Ability_Sub))
				return parent;
			parent = ((Ability_Sub)parent).getParent();
		}while(parent.getTarget() == null || parent.getTarget().getTargetPlayers().size() == 0);
		
		return parent;
	}
	
	public static void HandleRemembering(AbilityFactory AF)
	{
		if(!AF.getMapParams().containsKey("RememberTargets"))
		{
			return;
		}
		
		if(AF.getMapParams().containsKey("ForgetOtherTargets"))
		{
			AF.getHostCard().clearRemembered();
		}
		
		for(Card c : AF.getAbTgt().getTargetCards())
		{
			AF.getHostCard().addRemembered(c);
		}
	}
}