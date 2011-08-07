
package forge.card.spellability;


import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Phase;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;

public class SpellAbility_Restriction extends SpellAbility_Variables {
	// A class for handling SpellAbility Restrictions. These restrictions include: 
	// Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player, 
	// Threshold, Metalcraft, LevelRange, etc
	// Each value will have a default, that can be overridden (mostly by AbilityFactory)
	// The canPlay function will use these values to determine if the current game state is ok with these restrictions
	
	
	public SpellAbility_Restriction(){	}
	
	public void setRestrictions(HashMap<String,String> params) {
		if (params.containsKey("Activation")) {
			String value = params.get("Activation");
			if(value.equals("Threshold")) setThreshold(true);
			if(value.equals("Metalcraft")) setMetalcraft(true);
			if(value.equals("Hellbent")) setHellbent(true);
		}
		
		if (params.containsKey("ActivationZone"))
        	setZone(params.get("ActivationZone"));
        
        if (params.containsKey("Flashback")){
        	setZone("Graveyard");
        }
        
        if (params.containsKey("SorcerySpeed"))
        	setSorcerySpeed(true);
        
        if (params.containsKey("PlayerTurn"))
        	setPlayerTurn(true);
        
        if (params.containsKey("OpponentTurn"))
        	setOpponentTurn(true);
        
        if (params.containsKey("AnyPlayer"))
        	setAnyPlayer(true);
        
        if (params.containsKey("ActivationLimit"))
        	setActivationLimit(Integer.parseInt(params.get("ActivationLimit")));
        
        if (params.containsKey("ActivationNumberSacrifice"))
        	setActivationNumberSacrifice(Integer.parseInt(params.get("ActivationNumberSacrifice")));

        if (params.containsKey("ActivationPhases")) {
        	String phases = params.get("ActivationPhases");
        	
        	if (phases.contains("->")){
        		// If phases lists a Range, split and Build Activate String
        		// Combat_Begin->Combat_End (During Combat)
        		// Draw-> (After Upkeep)
        		// Upkeep->Combat_Begin (Before Declare Attackers)
        		
        		String[] split = phases.split("->", 2);
        		phases = AllZone.Phase.buildActivateString(split[0], split[1]);
        	}
        		
        	setPhases(phases);
        }
        
        if (params.containsKey("ActivationCardsInHand"))
        	setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));
        
        if (params.containsKey("Planeswalker"))
        	setPlaneswalker(true);
        
        if (params.containsKey("IsPresent")){
        	setIsPresent(params.get("IsPresent"));
        	if (params.containsKey("PresentCompare"))
        		setPresentCompare(params.get("PresentCompare"));
        }
        
        if (params.containsKey("IsNotPresent")){
        	setIsPresent(params.get("IsNotPresent"));
        	setPresentCompare("EQ0");
        }
        
        //basically PresentCompare for life totals:
        if(params.containsKey("ActivationLifeTotal")){
        	lifeTotal = params.get("ActivationLifeTotal");
        	if(params.containsKey("ActivationLifeAmount")) {
				lifeAmount = params.get("ActivationLifeAmount");
			}				
		}
	}//end setRestrictions()

	public boolean canPlay(Card c, SpellAbility sa){
		if (!AllZone.getZone(c).getZoneName().equals(zone))
			return false;
		
		Player activator = sa.getActivatingPlayer();
		if (activator == null){
			activator = c.getController();
			System.out.println(c.getName() + " Did not have activator set in SpellAbility_Restriction.canPlay()");
		}
		
		if (bSorcerySpeed && !Phase.canCastSorcery(activator))
			return false;
		
		if (bPlayerTurn && !AllZone.Phase.isPlayerTurn(activator))
			return false;
		
		if (bOpponentTurn && AllZone.Phase.isPlayerTurn(activator))
			return false;
		
		if (!bAnyPlayer && !activator.equals(c.getController()))
			return false;
		
		if (activationLimit != -1 && numberTurnActivations >= activationLimit)
			return false;
		
		if (phases.size() > 0){
			boolean isPhase = false;
			String currPhase = AllZone.Phase.getPhase();
			for(String s : phases){
				if (s.equals(currPhase)){
					isPhase = true;
					break;
				}
			}
			
			if (!isPhase)
				return false;
		}
		
		if(nCardsInHand != -1){
			if (AllZoneUtil.getPlayerHand(activator).size() != nCardsInHand)
				return false;
		}
		if(hellbent){
			if (!activator.hasHellbent())
				return false;
		}
		if(threshold){
			if (!activator.hasThreshold())
				return false;
		}
		if(metalcraft){
			if (!activator.hasMetalcraft())
				return false;
		}
		
		if (sIsPresent != null){
			CardList list = AllZoneUtil.getCardsInPlay();
			
			list = list.getValidCards(sIsPresent.split(","), activator, c);
			
			int right = 1;
			String rightString = presentCompare.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(c, c.getSVar("X"));
			}
			else {
				right = Integer.parseInt(presentCompare.substring(2));
			}
			int left = list.size();
			
			if (!Card.compare(left, presentCompare, right))
				return false;
		}
		
		if(lifeTotal != null) {
			int life = 1;
			if(lifeTotal.equals("You")) {
				life = activator.getLife();
			}
			if(lifeTotal.equals("Opponent")) {
				life = activator.getOpponent().getLife();
			}
			
			int right = 1;
			String rightString = lifeAmount.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
			}
			else {
				right = Integer.parseInt(lifeAmount.substring(2));
			}
			
			if(!Card.compare(life, lifeAmount, right)) {
				return false;
			}
		}
		
		if (pwAbility){
			// Planeswalker abilities can only be activated as Sorceries
			if (!Phase.canCastSorcery(activator))
				return false;
			
			for(SpellAbility pwAbs : c.getSpellAbility()){
				// check all abilities on card that have their planeswalker restriction set to confirm they haven't been activated
				SpellAbility_Restriction restrict = pwAbs.getRestrictions();
				if (restrict.getPlaneswalker() && restrict.getNumberTurnActivations() > 0)
					return false;
			}
		}
			
		return true;
	}//canPlay()
	
}//end class SpellAbility_Restriction
