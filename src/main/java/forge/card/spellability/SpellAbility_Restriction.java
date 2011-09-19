package forge.card.spellability;


import forge.*;
import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>SpellAbility_Restriction class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class SpellAbility_Restriction extends SpellAbility_Variables {
    // A class for handling SpellAbility Restrictions. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by AbilityFactory)
    // The canPlay function will use these values to determine if the current game state is ok with these restrictions


    /**
     * <p>Constructor for SpellAbility_Restriction.</p>
     */
    public SpellAbility_Restriction() {
    }

    /**
     * <p>setRestrictions.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public void setRestrictions(HashMap<String, String> params) {
        if (params.containsKey("Activation")) {
            String value = params.get("Activation");
            if (value.equals("Threshold")) setThreshold(true);
            if (value.equals("Metalcraft")) setMetalcraft(true);
            if (value.equals("Hellbent")) setHellbent(true);
            if (value.startsWith("Prowl")) {
                ArrayList<String> prowlTypes = new ArrayList<String>();
                prowlTypes.add("Rogue");
                if(value.split("Prowl").length > 1) {
                    prowlTypes.add(value.split("Prowl")[1]);
                }
                setProwl(prowlTypes);
            }
        }

        if (params.containsKey("ActivationZone"))
            setZone(Zone.smartValueOf(params.get("ActivationZone")));

        if (params.containsKey("Flashback")) {
            setZone(Zone.Graveyard);
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

        if (params.containsKey("ActivationCardsInHand"))
            setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));

        if (params.containsKey("Planeswalker"))
            setPlaneswalker(true);

        if (params.containsKey("IsPresent")) {
            setIsPresent(params.get("IsPresent"));
            if (params.containsKey("PresentCompare"))
                setPresentCompare(params.get("PresentCompare"));
            if (params.containsKey("PresentZone"))
            	setPresentZone(Zone.smartValueOf(params.get("PresentZone")));
        }

        if (params.containsKey("IsNotPresent")) {
            setIsPresent(params.get("IsNotPresent"));
            setPresentCompare("EQ0");
        }

        //basically PresentCompare for life totals:
        if (params.containsKey("ActivationLifeTotal")) {
            lifeTotal = params.get("ActivationLifeTotal");
            if (params.containsKey("ActivationLifeAmount")) {
                lifeAmount = params.get("ActivationLifeAmount");
            }
        }

        if (params.containsKey("CheckSVar")) {
            setSvarToCheck(params.get("CheckSVar"));
        }
        if (params.containsKey("SVarCompare")) {
            setSvarOperator(params.get("SVarCompare").substring(0, 2));
            setSvarOperand(params.get("SVarCompare").substring(2));
        }
    }//end setRestrictions()

    /**
     * <p>canPlay.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public boolean canPlay(Card c, SpellAbility sa) {
        if (!AllZone.getZoneOf(c).is(zone))
            return false;

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = c.getController();
            System.out.println(c.getName() + " Did not have activator set in SpellAbility_Restriction.canPlay()");
        }

        if (bSorcerySpeed && !Phase.canCastSorcery(activator))
            return false;

        if (bPlayerTurn && !AllZone.getPhase().isPlayerTurn(activator))
            return false;

        if (bOpponentTurn && AllZone.getPhase().isPlayerTurn(activator))
            return false;

        if (!bAnyPlayer && !activator.equals(c.getController()))
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
            if (activator.getCardsIn(Zone.Hand).size() != nCardsInHand)
                return false;
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
        if (prowl != null) {
            //only true if the activating player has damaged the opponent with one of the specified types 
            boolean prowlFlag = false;
            for (String type : prowl) {
                if (activator.hasProwl(type)) {
                    prowlFlag = true;
                }
            }
            if (!prowlFlag) {
                return false;
            }
        }
        if (sIsPresent != null) {
            CardList list = AllZoneUtil.getCardsIn(presentZone);

            list = list.getValidCards(sIsPresent.split(","), activator, c);

            int right = 1;
            String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(c, c.getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
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

        if (pwAbility) {
            // Planeswalker abilities can only be activated as Sorceries
            if (!Phase.canCastSorcery(activator))
                return false;

            for (SpellAbility pwAbs : c.getSpellAbility()) {
                // check all abilities on card that have their planeswalker restriction set to confirm they haven't been activated
                SpellAbility_Restriction restrict = pwAbs.getRestrictions();
                if (restrict.getPlaneswalker() && restrict.getNumberTurnActivations() > 0)
                    return false;
            }
        }

        if (svarToCheck != null) {
            int svarValue = AbilityFactory.calculateAmount(c, svarToCheck, sa);
            int operandValue = AbilityFactory.calculateAmount(c, svarOperand, sa);

            if (!AllZoneUtil.compare(svarValue, svarOperator, operandValue))
                return false;

        }

        return true;
    }//canPlay()

}//end class SpellAbility_Restriction
