package forge.card.staticAbility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Player;
import forge.StaticEffect;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class StaticAbility_Continuous {

    /**
     * 
     * TODO Write javadoc for this method.
     * @param stAb a StaticAbility
     */
    public static void applyContinuousAbility(final StaticAbility stAb) {
        HashMap<String, String> params = stAb.getMapParams();
        Card hostCard = stAb.getHostCard();

        StaticEffect se = new StaticEffect();
        CardList affectedCards =  getAffectedCards(stAb);
        ArrayList<Player> affectedPlayers =  getAffectedPlayers(stAb);

        se.setAffectedCards(affectedCards);
        se.setAffectedPlayers(affectedPlayers);
        se.setParams(params);
        se.setTimestamp(hostCard.getTimestamp());
        AllZone.getStaticEffects().addStaticEffect(se);

        int powerBonus = 0;
        int toughnessBonus = 0;
        int setPower = -1;
        int setToughness = -1;
        String[] addKeywords = null;
        String[] addAbilities = null;
        String[] addSVars = null;
        String[] addTypes = null;
        String[] removeTypes = null;
        String addColors = null;
        String[] addTriggers = null;
        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;

        if (params.containsKey("SetPower")) {
            String setP = params.get("SetPower");
            setPower = setP.matches("[0-9][0-9]?") ? Integer.parseInt(setP)
                    : CardFactoryUtil.xCount(hostCard, hostCard.getSVar(setP));
        }

        if (params.containsKey("SetToughness")) {
            String setT = params.get("SetToughness");
            setToughness = setT.matches("[0-9][0-9]?") ? Integer.parseInt(setT)
                    : CardFactoryUtil.xCount(hostCard, hostCard.getSVar(setT));
        }

        if (params.containsKey("AddPower")) {
            if (params.get("AddPower").equals("X")) {
                powerBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
                se.setXValue(powerBonus);
            }
            else if (params.get("AddPower").equals("Y")) {
                powerBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y"));
                se.setYValue(powerBonus);
            } else {
                powerBonus = Integer.valueOf(params.get("AddPower"));
            }
        }

        if (params.containsKey("AddToughness")) {
            if (params.get("AddToughness").equals("X")) {
                toughnessBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
                se.setXValue(toughnessBonus);
            }
            else if (params.get("AddToughness").equals("Y")) {
                toughnessBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y"));
                se.setYValue(toughnessBonus);
            } else {
                toughnessBonus = Integer.valueOf(params.get("AddToughness"));
            }
        }

        if (params.containsKey("AddKeyword")) {
            addKeywords = params.get("AddKeyword").split(" & ");
        }

        if (params.containsKey("AddAbility")) {
            String[] sVars = params.get("AddAbility").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addAbilities = sVars;
        }

        if (params.containsKey("AddSVar")) {
            addSVars = params.get("AddSVar").split(" & ");
        }

        if (params.containsKey("AddType")) {
            addTypes = params.get("AddType").split(" & ");
            if (addTypes[0].equals("ChosenType")) {
                String chosenType = hostCard.getChosenType();
                addTypes[0] = chosenType;
                se.setChosenType(chosenType);
            }
        }

        if (params.containsKey("RemoveType")) {
            removeTypes = params.get("RemoveType").split(" & ");
            if (removeTypes[0].equals("ChosenType")) {
                String chosenType = hostCard.getChosenType();
                removeTypes[0] = chosenType;
                se.setChosenType(chosenType);
            }
        }

        if (params.containsKey("RemoveSuperTypes")) {
            removeSuperTypes = true;
        }

        if (params.containsKey("RemoveCardTypes")) {
            removeCardTypes = true;
        }

        if (params.containsKey("RemoveSubTypes")) {
            removeSubTypes = true;
        }

        if (params.containsKey("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }

        if (params.containsKey("AddColor")) {
            addColors = CardUtil.getShortColorsString(
                    new ArrayList<String>(Arrays.asList(params.get("AddColor").split(" & "))));
        }

        if (params.containsKey("SetColor")) {
            addColors = CardUtil.getShortColorsString(
                    new ArrayList<String>(Arrays.asList(params.get("SetColor").split(" & "))));
            se.setOverwriteColors(true);
        }

        if (params.containsKey("AddTrigger")) {
            String[] sVars = params.get("AddTrigger").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addTriggers = sVars;
        }

        //modify players
        for (Player p : affectedPlayers) {

            // add keywords
            if (addKeywords != null) {
                for (String keyword : addKeywords) {
                    p.addKeyword(keyword);
                }
            }
        }

        //start modifying the cards
        for (int i = 0; i < affectedCards.size(); i++) {
            Card affectedCard = affectedCards.get(i);

            // set P/T
            if (params.containsKey("CharacteristicDefining")) {
                if (setPower != -1) {
                    affectedCard.setBaseAttack(setPower);
                }
                if (setToughness != -1) {
                    affectedCard.setBaseDefense(setToughness);
                }
            }
            else  //non CharacteristicDefining
                if (setPower != -1 || setToughness != -1) {
                    affectedCard.addNewPT(setPower, setToughness, hostCard.getTimestamp());
                }

            // add P/T bonus
            affectedCard.addSemiPermanentAttackBoost(powerBonus);
            affectedCard.addSemiPermanentDefenseBoost(toughnessBonus);

            // add keywords
            if (addKeywords != null) {
                for (String keyword : addKeywords) {
                    affectedCard.addExtrinsicKeyword(keyword);
                }
            }

            // add abilities
            if (addAbilities != null) {
                for (String abilty : addAbilities) {
                    if (abilty.startsWith("AB")) { // grant the ability
                        AbilityFactory af = new AbilityFactory();
                        SpellAbility sa = af.getAbility(abilty, affectedCard);
                        sa.setType("Temporary");
                        affectedCard.addSpellAbility(sa);
                    }
                }
            }

            // add SVars
            if (addSVars != null) {
                for (String sVar : addSVars) {
                    affectedCard.setSVar(sVar, hostCard.getSVar(sVar));
                }
            }

            // add Types
            if (addTypes != null || removeTypes != null) {
                affectedCard.addChangedCardTypes(addTypes, removeTypes, removeSuperTypes, removeCardTypes,
                        removeSubTypes, removeCreatureTypes, hostCard.getTimestamp());
            }

            // add colors
            if (addColors != null) {
                long t = affectedCard.addColor(addColors, affectedCard, !se.isOverwriteColors(), true);
                se.addTimestamp(affectedCard, t);
            }

            // add triggers
            if (addTriggers != null) {
                for (String trigger : addTriggers) {
                    Trigger actualTrigger = TriggerHandler.parseTrigger(trigger, affectedCard, false);
                    actualTrigger.setTemporary(true);
                    affectedCard.addTrigger(actualTrigger);
                    AllZone.getTriggerHandler().registerTrigger(actualTrigger);
                }
            }

            // remove triggers
            if (params.containsKey("RemoveTriggers") || params.containsKey("RemoveAllAbilities")) {
                ArrayList<Trigger> triggers = affectedCard.getTriggers();
                for (Trigger trigger : triggers) {
                    trigger.setSuppressed(true);
                }
            }
            //affectedCard.updateObservers();
        }
    }

    private static ArrayList<Player> getAffectedPlayers(final StaticAbility stAb) {
        HashMap<String, String> params = stAb.getMapParams();
        Card hostCard = stAb.getHostCard();
        Player controller = hostCard.getController();

        ArrayList<Player> players = new ArrayList<Player>();

        if (!params.containsKey("Affected")) {
            return players;
        }

        String[] strngs = params.get("Affected").split(",");

        for (String str : strngs) {
            if (str.equals("Player") || str.equals("You")) {
                players.add(controller);
            }

            if (str.equals("Player") || str.equals("Opponent")) {
                players.add(controller.getOpponent());
            }
        }

        return players;
    }

    private static CardList getAffectedCards(final StaticAbility stAb) {
        HashMap<String, String> params = stAb.getMapParams();
        Card hostCard = stAb.getHostCard();
        Player controller = hostCard.getController();

        if (params.containsKey("CharacteristicDefining")) {
            return new CardList(hostCard); // will always be the card itself
        }

        // non - CharacteristicDefining
        CardList affectedCards;
        String affectedZone = "Battlefield"; // default

        if (params.containsKey("AffectedZone")) {
            affectedZone = params.get("AffectedZone");
        }

        affectedCards = AllZoneUtil.getCardsInZone(affectedZone);

        if (params.containsKey("Affected") && !params.get("Affected").contains(",")) {
            if (params.get("Affected").contains("Self")) {
                affectedCards = new CardList(hostCard);
            } else if (params.get("Affected").contains("EnchantedBy")) {
                affectedCards = new CardList(hostCard.getEnchantingCard());
            } else if (params.get("Affected").contains("EquippedBy")) {
                affectedCards = new CardList(hostCard.getEquippingCard());
            }
        }

        if (params.containsKey("Affected")) {
            affectedCards = affectedCards.getValidCards(
                    params.get("Affected").split(","), controller, hostCard);
        }

        return affectedCards;
    }

}
