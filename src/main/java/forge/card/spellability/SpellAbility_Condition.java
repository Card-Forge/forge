package forge.card.spellability;


import forge.*;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

import java.util.HashMap;

/**
 * <p>SpellAbility_Condition class.</p>
 *
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbility_Condition extends SpellAbility_Variables {
    // A class for handling SpellAbility Conditions. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by AbilityFactory)
    // The CanPlay function will use these values to determine if the current game state is ok with these restrictions

    /**
     * <p>Constructor for SpellAbility_Condition.</p>
     */
    public SpellAbility_Condition() {
    }

    /**
     * <p>setConditions.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     */
    public void setConditions(HashMap<String, String> params) {
        if (params.containsKey("Condition")) {
            String value = params.get("Condition");
            if (value.equals("Threshold")) setThreshold(true);
            if (value.equals("Metalcraft")) setMetalcraft(true);
            if (value.equals("Hellbent")) setHellbent(true);
        }

        if (params.containsKey("ConditionZone"))
            setZone(params.get("ContitionZone"));

        if (params.containsKey("ConditionSorcerySpeed"))
            setSorcerySpeed(true);

        if (params.containsKey("ConditionPlayerTurn"))
            setPlayerTurn(true);

        if (params.containsKey("ConditionOpponentTurn"))
            setOpponentTurn(true);

        if (params.containsKey("ConditionPhases")) {
            String phases = params.get("ConditionPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }

            setPhases(phases);
        }

        if (params.containsKey("ConditionCardsInHand"))
            setActivateCardsInHand(Integer.parseInt(params.get("ConditionCardsInHand")));

        //Condition version of IsPresent stuff
        if (params.containsKey("ConditionPresent")) {
            setIsPresent(params.get("ConditionPresent"));
            if (params.containsKey("ConditionCompare"))
                setPresentCompare(params.get("ConditionCompare"));
        }

        if (params.containsKey("ConditionDefined")) {
            setPresentDefined(params.get("ConditionDefined"));
        }

        if (params.containsKey("ConditionNotPresent")) {
            setIsPresent(params.get("ConditionNotPresent"));
            setPresentCompare("EQ0");
        }

        //basically PresentCompare for life totals:
        if (params.containsKey("ConditionLifeTotal")) {
            lifeTotal = params.get("ConditionLifeTotal");
            if (params.containsKey("ConditionLifeAmount")) {
                lifeAmount = params.get("ConditionLifeAmount");
            }
        }
        
        if(params.containsKey("ConditionManaSpent")) {
        	setManaSpent(params.get("ConditionManaSpent"));
        }
    }//setConditions

    /**
     * <p>checkConditions.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public boolean checkConditions(SpellAbility sa) {

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = sa.getSourceCard().getController();
            System.out.println(sa.getSourceCard().getName() + " Did not have activator set in SpellAbility_Condition.checkConditions()");
        }

        if (hellbent) {
            if (!activator.hasHellbent())
                return false;
        }
        if (threshold) {
            if (!activator.hasThreshold())
                return false;
        }
        if (metalcraft) {
            if (!activator.hasMetalcraft())
                return false;
        }

        if (bSorcerySpeed && !Phase.canCastSorcery(activator))
            return false;

        if (bPlayerTurn && !AllZone.getPhase().isPlayerTurn(activator))
            return false;

        if (bOpponentTurn && AllZone.getPhase().isPlayerTurn(activator))
            return false;

        if (activationLimit != -1 && numberTurnActivations >= activationLimit)
            return false;

        if (phases.size() > 0) {
            boolean isPhase = false;
            String currPhase = AllZone.getPhase().getPhase();
            for (String s : phases) {
                if (s.equals(currPhase)) {
                    isPhase = true;
                    break;
                }
            }

            if (!isPhase)
                return false;
        }

        if (nCardsInHand != -1) {
            // Can handle Library of Alexandria, or Hellbent
            if (AllZoneUtil.getPlayerHand(activator).size() != nCardsInHand)
                return false;
        }

        if (sIsPresent != null) {
            CardList list = new CardList();
            if (presentDefined != null) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), presentDefined, sa).toArray());
            } else {
                list = AllZoneUtil.getCardsInPlay();
            }

            list = list.getValidCards(sIsPresent.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            int right;
            String rightString = presentCompare.substring(2);
            try {    // If this is an Integer, just parse it
                right = Integer.parseInt(rightString);
            } catch (NumberFormatException e) {    // Otherwise, grab it from the SVar
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
            }

            int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right))
                return false;
        }

        if (lifeTotal != null) {
            int life = 1;
            if (lifeTotal.equals("You")) {
                life = activator.getLife();
            }
            if (lifeTotal.equals("Opponent")) {
                life = activator.getOpponent().getLife();
            }

            int right = 1;
            String rightString = lifeAmount.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
            } else {
                right = Integer.parseInt(lifeAmount.substring(2));
            }

            if (!AllZoneUtil.compare(life, lifeAmount, right)) {
                return false;
            }
        }
        
        if(null != manaSpent) {
        	if(!sa.getSourceCard().getColorsPaid().contains(manaSpent)) {
        		return false;
        	}
        }

        return true;
    }

}//end class SpellAbility_Condition
