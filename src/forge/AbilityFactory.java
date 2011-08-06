package forge;

import java.util.ArrayList;
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
		
		for (int aCnt = 0; aCnt < a.length; aCnt ++)
		    a[aCnt] = a[aCnt].trim();
		
		if (!(a.length > 1))
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
		else if (mapParams.containsKey("DB")) {
			isDb = true;
			API = mapParams.get("DB");
		}
		else
			throw new RuntimeException("AbilityFactory : getAbility -- no API in " + hostCard.getName());

		if (!isDb){
			if (!mapParams.containsKey("Cost"))
				throw new RuntimeException("AbilityFactory : getAbility -- no Cost in " + hostCard.getName());
			abCost = new Ability_Cost(mapParams.get("Cost"), hostCard.getName(), isAb);
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
				String prompt = mapParams.containsKey("TgtPrompt") ? mapParams.get("TgtPrompt") : "Select target " + mapParams.get("ValidTgts");
				abTgt = new Target(prompt, mapParams.get("ValidTgts").split(","), min, max);
			}
			else
				abTgt = new Target(mapParams.get("Tgt"), min, max);
			
			if (mapParams.containsKey("TgtZone"))	// if Targeting something not in play, this Key should be set
				abTgt.setZone(mapParams.get("TgtZone"));
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
		
		// Fetch, Retrieve and Bounce should be converted ChangeZone 
		/*
		if (API.equals("Fetch")){
			if (isAb)
				SA = AbilityFactory_Fetch.createAbilityFetch(this);
			else if (isSp)
				SA = AbilityFactory_Fetch.createSpellFetch(this);
		}
		
		if (API.equals("Retrieve")){
			if (isAb)
				SA = AbilityFactory_Fetch.createAbilityRetrieve(this);
			else if (isSp)
				SA = AbilityFactory_Fetch.createSpellRetrieve(this);
		}
		
		if (API.equals("Bounce")){
			if (isAb)
				SA = AbilityFactory_Bounce.createAbilityBounce(this);
			else if (isSp)
				SA = AbilityFactory_Bounce.createSpellBounce(this);
			hostCard.setSVar("PlayMain1", "TRUE");
		}
		*/
		// Convert above abilities to gain Drawback
		
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
		}
		
		if (API.equals("DestroyAll")){
			if (isAb)
				SA = AbilityFactory_Destroy.createAbilityDestroyAll(this);
			else if (isSp)
				SA = AbilityFactory_Destroy.createSpellDestroyAll(this);
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
			ComputerAI_counterSpells2.KeywordedCounterspells.add(hostC.getName());
			
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
		
		if (SA == null)
			throw new RuntimeException("AbilityFactory : SpellAbility was not created. Did you add the API section?");

		// *********************************************
		// set universal properties of the SpellAbility
		
		SA.setAbilityFactory(this);
		
		if(hasSubAbility())
			SA.setSubAbility(getSubAbility());
		
        if (hasSpDesc)
        {
        	if(isDb)
        		throw new RuntimeException("AbilityFactory : getAbility -- SpellDescription on Drawback in " + hostCard.getName());
        	StringBuilder sb = new StringBuilder();
        	
        	if (mapParams.containsKey("PrecostDesc"))
        		sb.append(mapParams.get("PrecostDesc")).append(" ");
        	if (mapParams.containsKey("CostDesc"))
        		sb.append(mapParams.get("CostDesc")).append(" ");
        	else sb.append(abCost.toString());
        	
	        sb.append(mapParams.get("SpellDescription"));
        	
        	SA.setDescription(sb.toString());
        }
        
        if (!isTargeted)
        	SA.setStackDescription(hostCard.getName());
        
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
        return restrict;
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
				CardList list;
				
				if (calcX[0].startsWith("Sacrificed"))
					list = findRootAbility(ability).getSacrificedCost();
				
				else if (calcX[0].startsWith("Discarded"))
					list = findRootAbility(ability).getDiscardedCost();
				
				else if (calcX[0].startsWith("Targeted")){
					SpellAbility saTargeting = (ability.getTarget() == null) ?  findParentsTargetedCard(ability) : ability;
					list = new CardList(saTargeting.getTarget().getTargetCards().toArray());
				}
				else
					return 0;
				
				return CardFactoryUtil.handlePaid(list, calcX[1]) * multiplier;
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

		if (c != null)
			cards.add(c);
		
		return cards;
	}
	
	
	public static ArrayList<Player> getDefinedPlayers(Card card, String def, SpellAbility sa){
		ArrayList<Player> players = new ArrayList<Player>();
		String defined = (def == null) ? "You" : def;
		
		players = new ArrayList<Player>();
		if (defined.equals("Targeted")){
			Target tgt = sa.getTarget();
			if (tgt != null && tgt.getTargetPlayers().size() != 0){
				players.addAll(tgt.getTargetPlayers());
				return players;
			}
			
			SpellAbility parent;
			do{
				parent = ((Ability_Sub)sa).getParent();
			}while(parent.getTarget() == null && parent.getTarget().getTargetPlayers().size() == 0);
			
			players.addAll(parent.getTarget().getTargetPlayers());
		}
		else{
			if (defined.equals("You") || defined.equals("Each"))
				players.add(sa.getActivatingPlayer());
			
			if (defined.equals("Opponent") || defined.equals("Each"))
				players.add(sa.getActivatingPlayer().getOpponent());
		}
		return players;
	}
	
	public static ArrayList<Object> getDefinedObjects(Card card, String def, SpellAbility sa){
		ArrayList<Object> objects = new ArrayList<Object>();
		String defined = (def == null) ? "Self" : def;
		
		objects = new ArrayList<Object>();
		if (defined.equals("Targeted")){
			Target tgt = sa.getTarget();
			if (tgt != null && tgt.getTargets().size() != 0){
				objects.addAll(tgt.getTargets());
				return objects;
			}
			
			SpellAbility parent;
			do{
				parent = ((Ability_Sub)sa).getParent();
			}while(parent.getTarget() == null && parent.getTarget().getTargets().size() == 0);
			
			objects.addAll(parent.getTarget().getTargets());
		}
		else{
			// Player checks
			if (defined.equals("You") || defined.equals("Each"))
				objects.add(sa.getActivatingPlayer());
			
			if (defined.equals("Opponent") || defined.equals("Each"))
				objects.add(sa.getActivatingPlayer().getOpponent());

			// Card checks
			Card c = null;

			if (defined.equals("Self"))
				c = card;
			
			if (defined.equals("Equipped"))
				c = card.getEquippingCard();

			if (defined.equals("Enchanted"))
				c = card.getEnchantingCard();
			
			if (c != null)
				objects.add(c);
		}
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
			parent = ((Ability_Sub)parent).getParent();
		}while(parent.getTarget() == null && parent.getTarget().getTargetCards().size() == 0);
		
		return parent;
	}
}