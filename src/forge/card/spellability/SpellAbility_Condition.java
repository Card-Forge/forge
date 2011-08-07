
package forge.card.spellability;


import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.Phase;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

public class SpellAbility_Condition extends SpellAbility_Variables{
	// A class for handling SpellAbility Conditions. These restrictions include: 
	// Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player, 
	// Threshold, Metalcraft, LevelRange, etc
	// Each value will have a default, that can be overridden (mostly by AbilityFactory)
	// The CanPlay function will use these values to determine if the current game state is ok with these restrictions

	public SpellAbility_Condition() { }

	public void setConditions(HashMap<String,String> params) {
		if (params.containsKey("Condition")) {
			String value = params.get("Condition");
			if(value.equals("Threshold")) setThreshold(true);
			if(value.equals("Metalcraft")) setMetalcraft(true);
			if(value.equals("Hellbent")) setHellbent(true);
		}

		if (params.containsKey("ConditionZone"))
			setZone(params.get("ContitionZone"));

		if (params.containsKey("ConditionSorcerySpeed"))
			setSorcerySpeed(true);

		if (params.containsKey("ConditionPlayerTurn"))
			setPlayerTurn(true);

		if (params.containsKey("ConditionOpponentTurn"))
			setOpponentTurn(true);

		if (params.containsKey("ConditionAnyPlayer"))
			setAnyPlayer(true);

		if (params.containsKey("ConditionPhases")) {
			String phases = params.get("ConditionPhases");

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

		if (params.containsKey("ConditionCardsInHand"))
			setActivateCardsInHand(Integer.parseInt(params.get("ConditionCardsInHand")));



		//Condition version of IsPresent stuff
		if (params.containsKey("ConditionPresent")){
			setIsPresent(params.get("ConditionPresent"));
			if (params.containsKey("ConditionCompare"))
				setPresentCompare(params.get("ConditionCompare"));
		}

		if(params.containsKey("ConditionDefined")) {
			setPresentDefined(params.get("ConditionDefined"));
		}

		if (params.containsKey("ConditionNotPresent")){
			setIsPresent(params.get("ConditionNotPresent"));
			setPresentCompare("EQ0");
		}

		//basically PresentCompare for life totals:
		if(params.containsKey("ConditionLifeTotal")){
			lifeTotal = params.get("ConditionLifeTotal");
			if(params.containsKey("ConditionLifeAmount")) {
				lifeAmount = params.get("ConditionLifeAmount");
			}				
		}
	}//setConditions

	public boolean checkConditions(SpellAbility sa) {

		Player activator = sa.getActivatingPlayer();
		if (activator == null){
			activator = sa.getSourceCard().getController();
			System.out.println(sa.getSourceCard().getName() + " Did not have activator set in SpellAbility_Condition.checkConditions()");
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

		if (bSorcerySpeed && !Phase.canCastSorcery(activator))
			return false;

		if (bPlayerTurn && !AllZone.Phase.isPlayerTurn(activator))
			return false;

		if (bOpponentTurn && AllZone.Phase.isPlayerTurn(activator))
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

		if (nCardsInHand != -1){
			// Can handle Library of Alexandria, or Hellbent
			if (AllZoneUtil.getPlayerHand(activator).size() != nCardsInHand)
				return false;
		}

		if (sIsPresent != null){
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.getValidCards(sIsPresent.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

			int right = 1;
			String rightString = presentCompare.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
			}
			else {
				right = Integer.parseInt(presentCompare.substring(2));
			}
			int left = list.size();

			if (!Card.compare(left, presentCompare, right))
				return false;
		}

		if(presentDefined != null) {
			CardList list;
			if (presentDefined == null)
				list = AllZoneUtil.getCardsInPlay();
			else{
				list = new CardList(AbilityFactory.getDefinedCards(sa.getSourceCard(), presentDefined, sa));
			}

			list = list.getValidCards(sIsPresent.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

			int right;
			String rightString = presentCompare.substring(2);
			try{	// If this is an Integer, just parse it
				right = Integer.parseInt(rightString);
			}
			catch(NumberFormatException e){	// Otherwise, grab it from the SVar
				right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
			}

			int left = list.size();

			return Card.compare(left, presentCompare, right);
		}
		else if (sIsPresent != null) {
			CardList list = AllZoneUtil.getCardsInPlay();

			list = list.getValidCards(sIsPresent.split(","), activator, sa.getSourceCard());

			int right = 1;
			String rightString = presentCompare.substring(2);
			if(rightString.equals("X")) {
				right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
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

		return true;
	}

}//end class SpellAbility_Condition
