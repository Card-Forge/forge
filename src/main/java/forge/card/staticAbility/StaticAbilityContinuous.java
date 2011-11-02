package forge.card.staticAbility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Constant.Zone;
import forge.Player;
import forge.StaticEffect;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * The Class StaticAbility_Continuous.
 */
public class StaticAbilityContinuous {

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     */
    public static void applyContinuousAbility(final StaticAbility stAb) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        final StaticEffect se = new StaticEffect();
        final CardList affectedCards = StaticAbilityContinuous.getAffectedCards(stAb);
        final ArrayList<Player> affectedPlayers = StaticAbilityContinuous.getAffectedPlayers(stAb);

        se.setAffectedCards(affectedCards);
        se.setAffectedPlayers(affectedPlayers);
        se.setParams(params);
        se.setTimestamp(hostCard.getTimestamp());
        AllZone.getStaticEffects().addStaticEffect(se);

        int powerBonus = 0;
        int toughnessBonus = 0;
        String setP = "";
        int setPower = -1;
        String setT = "";
        int setToughness = -1;
        String[] addKeywords = null;
        String[] addHiddenKeywords = null;
        String[] removeKeywords = null;
        String[] addAbilities = null;
        String[] addSVars = null;
        String[] addTypes = null;
        String[] removeTypes = null;
        String addColors = null;
        String[] addTriggers = null;
        boolean removeAllAbilities = false;
        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;

        if (params.containsKey("SetPower")) {
            setP = params.get("SetPower");
            setPower = setP.matches("[0-9][0-9]?") ? Integer.parseInt(setP) : CardFactoryUtil.xCount(hostCard,
                    hostCard.getSVar(setP));
        }

        if (params.containsKey("SetToughness")) {
            setT = params.get("SetToughness");
            setToughness = setT.matches("[0-9][0-9]?") ? Integer.parseInt(setT) : CardFactoryUtil.xCount(hostCard,
                    hostCard.getSVar(setT));
        }

        if (params.containsKey("AddPower")) {
            if (params.get("AddPower").equals("X")) {
                powerBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
                se.setXValue(powerBonus);
            } else if (params.get("AddPower").equals("Y")) {
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
            } else if (params.get("AddToughness").equals("Y")) {
                toughnessBonus = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y"));
                se.setYValue(toughnessBonus);
            } else {
                toughnessBonus = Integer.valueOf(params.get("AddToughness"));
            }
        }

        if (params.containsKey("AddKeyword")) {
            addKeywords = params.get("AddKeyword").split(" & ");
        }

        if (params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords = params.get("AddHiddenKeyword").split(" & ");
        }

        if (params.containsKey("RemoveKeyword")) {
            removeKeywords = params.get("RemoveKeyword").split(" & ");
        }

        if (params.containsKey("RemoveAllAbilities")) {
            removeAllAbilities = true;
        }

        if (params.containsKey("AddAbility")) {
            final String[] sVars = params.get("AddAbility").split(" & ");
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
                final String chosenType = hostCard.getChosenType();
                addTypes[0] = chosenType;
                se.setChosenType(chosenType);
            }
        }

        if (params.containsKey("RemoveType")) {
            removeTypes = params.get("RemoveType").split(" & ");
            if (removeTypes[0].equals("ChosenType")) {
                final String chosenType = hostCard.getChosenType();
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
            addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("AddColor").split(
                    " & "))));
        }

        if (params.containsKey("SetColor")) {
            addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("SetColor").split(
                    " & "))));
            se.setOverwriteColors(true);
        }

        if (params.containsKey("AddTrigger")) {
            final String[] sVars = params.get("AddTrigger").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addTriggers = sVars;
        }

        // modify players
        for (final Player p : affectedPlayers) {

            // add keywords
            if (addKeywords != null) {
                for (final String keyword : addKeywords) {
                    p.addKeyword(keyword);
                }
            }
        }

        // start modifying the cards
        for (int i = 0; i < affectedCards.size(); i++) {
            final Card affectedCard = affectedCards.get(i);

            // set P/T
            if (params.containsKey("CharacteristicDefining")) {
                if (setPower != -1) {
                    affectedCard.setBaseAttack(setPower);
                }
                if (setToughness != -1) {
                    affectedCard.setBaseDefense(setToughness);
                }
            } else // non CharacteristicDefining
            if ((setPower != -1) || (setToughness != -1)) {
                if (setP.startsWith("AffectedX")) {
                    setPower = CardFactoryUtil.xCount(affectedCard, hostCard.getSVar(setP));
                }
                if (setT.startsWith("AffectedX")) {
                    setToughness = CardFactoryUtil.xCount(affectedCard, hostCard.getSVar(setT));
                }
                affectedCard.addNewPT(setPower, setToughness, hostCard.getTimestamp());
            }

            // add P/T bonus
            affectedCard.addSemiPermanentAttackBoost(powerBonus);
            affectedCard.addSemiPermanentDefenseBoost(toughnessBonus);

            // add keywords
            if ((addKeywords != null) || (removeKeywords != null) || removeAllAbilities) {
                affectedCard.addChangedCardKeywords(addKeywords, removeKeywords, removeAllAbilities,
                        hostCard.getTimestamp());
            }

            // add HIDDEN keywords
            if (addHiddenKeywords != null) {
                for (final String k : addHiddenKeywords) {
                    affectedCard.addExtrinsicKeyword(k);
                }
            }

            // add abilities
            if (addAbilities != null) {
                for (final String abilty : addAbilities) {
                    if (abilty.startsWith("AB")) { // grant the ability
                        final AbilityFactory af = new AbilityFactory();
                        final SpellAbility sa = af.getAbility(abilty, affectedCard);
                        sa.setType("Temporary");
                        affectedCard.addSpellAbility(sa);
                    }
                }
            }

            // add SVars
            if (addSVars != null) {
                for (final String sVar : addSVars) {
                    affectedCard.setSVar(sVar, hostCard.getSVar(sVar));
                }
            }

            // add Types
            if ((addTypes != null) || (removeTypes != null)) {
                affectedCard.addChangedCardTypes(addTypes, removeTypes, removeSuperTypes, removeCardTypes,
                        removeSubTypes, removeCreatureTypes, hostCard.getTimestamp());
            }

            // add colors
            if (addColors != null) {
                final long t = affectedCard.addColor(addColors, affectedCard, !se.isOverwriteColors(), true);
                se.addTimestamp(affectedCard, t);
            }

            // add triggers
            if (addTriggers != null) {
                for (final String trigger : addTriggers) {
                    final Trigger actualTrigger = TriggerHandler.parseTrigger(trigger, affectedCard, false);
                    affectedCard.addTrigger(actualTrigger).setTemporary(true);
                }
            }

            // remove triggers
            if (params.containsKey("RemoveTriggers") || removeAllAbilities) {
                final ArrayList<Trigger> triggers = affectedCard.getTriggers();
                for (final Trigger trigger : triggers) {
                    trigger.setTemporarilySuppressed(true);
                }
            }

            // remove activated and static abilities
            if (removeAllAbilities) {
                final ArrayList<SpellAbility> abilities = affectedCard.getSpellAbilities();
                for (final SpellAbility ab : abilities) {
                    ab.setTemporarilySuppressed(true);
                }
                final ArrayList<StaticAbility> staticAbilities = affectedCard.getStaticAbilities();
                for (final StaticAbility stA : staticAbilities) {
                    stA.setTemporarilySuppressed(true);
                }
            }
        }
    }

    private static ArrayList<Player> getAffectedPlayers(final StaticAbility stAb) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        final ArrayList<Player> players = new ArrayList<Player>();

        if (!params.containsKey("Affected")) {
            return players;
        }

        final String[] strngs = params.get("Affected").split(",");

        for (final String str : strngs) {
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
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        if (params.containsKey("CharacteristicDefining")) {
            return new CardList(hostCard); // will always be the card itself
        }

        // non - CharacteristicDefining
        CardList affectedCards = new CardList();

        if (params.containsKey("AffectedZone")) {
            affectedCards.addAll(AllZoneUtil.getCardsIn(Zone.listValueOf(params.get("AffectedZone"))));
        } else {
            affectedCards = AllZoneUtil.getCardsIn(Zone.Battlefield);
        }

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
            affectedCards = affectedCards.getValidCards(params.get("Affected").split(","), controller, hostCard);
        }

        return affectedCards;
    }

}
