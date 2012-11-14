/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.staticability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import forge.Card;
import forge.Singletons;

import forge.CardLists;
import forge.CardUtil;
import forge.StaticEffect;
import forge.StaticEffects;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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
     * 
     */
    public static void applyContinuousAbility(final StaticAbility stAb) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();

        final StaticEffect se = new StaticEffect();
        final List<Card> affectedCards = StaticAbilityContinuous.getAffectedCards(stAb);
        final ArrayList<Player> affectedPlayers = StaticAbilityContinuous.getAffectedPlayers(stAb);

        se.setAffectedCards(affectedCards);
        se.setAffectedPlayers(affectedPlayers);
        se.setParams(params);
        se.setTimestamp(hostCard.getTimestamp());
        se.setSource(hostCard);
        Singletons.getModel().getGame().getStaticEffects().addStaticEffect(se);

        int powerBonus = 0;
        int toughnessBonus = 0;
        String setP = "";
        int setPower = -1;
        String setT = "";
        int setToughness = -1;
        int keywordMultiplier = 1;

        String[] addKeywords = null;
        String[] addHiddenKeywords = null;
        String[] removeKeywords = null;
        String[] addAbilities = null;
        String[] addSVars = null;
        String[] addTypes = null;
        String[] removeTypes = null;
        String addColors = null;
        String[] addTriggers = null;
        ArrayList<SpellAbility> addFullAbs = null;
        boolean removeAllAbilities = false;
        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;
        
        //Global rules changes
        if (params.containsKey("GlobalRule")) {
            final StaticEffects effects = Singletons.getModel().getGame().getStaticEffects();
            if (params.get("GlobalRule").equals("Damage can't be prevented.")) {
                effects.setNoPrevention(true);
            }
        }

        if (params.containsKey("SetPower")) {
            setP = params.get("SetPower");
            setPower = setP.matches("[0-9][0-9]?") ? Integer.parseInt(setP)
                    : AbilityFactory.calculateAmount(hostCard, setP, null);
        }

        if (params.containsKey("SetToughness")) {
            setT = params.get("SetToughness");
            setToughness = setT.matches("[0-9][0-9]?") ? Integer.parseInt(setT)
                    : AbilityFactory.calculateAmount(hostCard, setT, null);
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

        if (params.containsKey("KeywordMultiplier")) {
            String multiplier = params.get("KeywordMultiplier");
            if (multiplier.equals("X")) {
                keywordMultiplier = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
                se.setXValue(keywordMultiplier);
            } else {
                keywordMultiplier = Integer.valueOf(multiplier);
            }
        }

        if (params.containsKey("AddKeyword")) {
            addKeywords = params.get("AddKeyword").split(" & ");
            final ArrayList<String> chosencolors = hostCard.getChosenColor();
            for (final String color : chosencolors) {
                for (int w = 0; w < addKeywords.length; w++) {
                    addKeywords[w] = addKeywords[w].replaceAll("ChosenColor", color.substring(0, 1).toUpperCase().concat(color.substring(1, color.length())));
                }
            }
            final String chosenType = hostCard.getChosenType();
            for (int w = 0; w < addKeywords.length; w++) {
                addKeywords[w] = addKeywords[w].replaceAll("ChosenType", chosenType);
            }
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
            final String colors = params.get("SetColor");
            if (colors.equals("ChosenColor")) {
                addColors = CardUtil.getShortColorsString(hostCard.getChosenColor());
            } else {
                addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(
                        colors.split(" & "))));
            }
            se.setOverwriteColors(true);
        }

        if (params.containsKey("AddTrigger")) {
            final String[] sVars = params.get("AddTrigger").split(" & ");
            for (int i = 0; i < sVars.length; i++) {
                sVars[i] = hostCard.getSVar(sVars[i]);
            }
            addTriggers = sVars;
        }

        if (params.containsKey("GainsAbilitiesOf")) {
            final String[] valids = params.get("GainsAbilitiesOf").split(",");
            ArrayList<ZoneType> validZones = new ArrayList<ZoneType>();
            validZones.add(ZoneType.Battlefield);
            if (params.containsKey("GainsAbilitiesOfZones")) {
                validZones.clear();
                for (String s : params.get("GainsAbilitiesOfZones").split(",")) {
                    validZones.add(ZoneType.smartValueOf(s));
                }
            }

            List<Card> cardsIGainedAbilitiesFrom = Singletons.getModel().getGame().getCardsIn(validZones);
            cardsIGainedAbilitiesFrom = CardLists.getValidCards(cardsIGainedAbilitiesFrom, valids, hostCard.getController(), hostCard);

            if (cardsIGainedAbilitiesFrom.size() > 0) {

                addFullAbs = new ArrayList<SpellAbility>();

                for (Card c : cardsIGainedAbilitiesFrom) {
                    for (SpellAbility sa : c.getSpellAbilities()) {
                        if (sa instanceof AbilityActivated) {
                            SpellAbility newSA = ((AbilityActivated) sa).getCopy();
                            newSA.setType("Temporary");
                            CardFactoryUtil.correctAbilityChainSourceCard(newSA, hostCard);
                            addFullAbs.add(newSA);
                        }
                    }
                }
            }
        }

        // modify players
        for (final Player p : affectedPlayers) {

            // add keywords
            if (addKeywords != null) {
                for (final String keyword : addKeywords) {
                    for (int i = 0; i < keywordMultiplier; i++) {
                        p.addKeyword(keyword);
                    }
                }
            }

            if (params.containsKey("SetMaxHandSize")) {
                String mhs = params.get("SetMaxHandSize");
                int max = mhs.matches("[0-9][0-9]?") ? Integer.parseInt(mhs)
                        : AbilityFactory.calculateAmount(hostCard, mhs, null);
                p.setMaxHandSize(max);
            }

            if (params.containsKey("RaiseMaxHandSize") && p.getMaxHandSize() != -1) {
                String rmhs = params.get("RaiseMaxHandSize");
                int rmax = rmhs.matches("[0-9][0-9]?") ? Integer.parseInt(rmhs)
                        : AbilityFactory.calculateAmount(hostCard, rmhs, null);
                p.setMaxHandSize(p.getMaxHandSize() + rmax);
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
            // TODO regular keywords currently don't try to use keyword multiplier
            // (Although nothing uses it at this time)
            if ((addKeywords != null) || (removeKeywords != null) || removeAllAbilities) {
                affectedCard.addChangedCardKeywords(addKeywords, removeKeywords, removeAllAbilities,
                        hostCard.getTimestamp());
            }

            // add HIDDEN keywords
            if (addHiddenKeywords != null) {
                for (final String k : addHiddenKeywords) {
                    for (int j = 0; j < keywordMultiplier; j++) {
                        affectedCard.addExtrinsicKeyword(k);
                    }
                }
            }

            // add SVars
            if (addSVars != null) {
                for (final String sVar : addSVars) {
                    String actualSVar = hostCard.getSVar(sVar);
                    String name = sVar;
                    if (actualSVar.startsWith("SVar")) {
                        actualSVar = actualSVar.split("SVar:")[1];
                        name = actualSVar.split(":")[0];
                        actualSVar = actualSVar.split(":")[1];
                    }
                    affectedCard.setSVar(name, actualSVar);
                }
            }

            if (addFullAbs != null) {
                for (final SpellAbility ab : addFullAbs) {
                    affectedCard.addSpellAbility(ab);
                }
            }

            // add abilities
            if (addAbilities != null) {
                for (final String abilty : addAbilities) {
                    if (abilty.startsWith("AB")) { // grant the ability
                        final AbilityFactory af = new AbilityFactory();
                        final SpellAbility sa = af.getAbility(abilty, affectedCard);
                        sa.setType("Temporary");
                        sa.setOriginalHost(hostCard);
                        affectedCard.addSpellAbility(sa);
                    }
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
                for (final Trigger trigger : affectedCard.getTriggers()) {
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
                final ArrayList<ReplacementEffect> replacementEffects = affectedCard.getReplacementEffects();
                for (final ReplacementEffect rE : replacementEffects) {
                    rE.setTemporarilySuppressed(true);
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

        for (Player p : Singletons.getModel().getGame().getPlayers()) {
            if (p.isValid(strngs, controller, hostCard)) {
                players.add(p);
            }
        }

        return players;
    }

    private static List<Card> getAffectedCards(final StaticAbility stAb) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        if (params.containsKey("CharacteristicDefining")) {
            return CardLists.createCardList(hostCard); // will always be the card itself
        }

        // non - CharacteristicDefining
        List<Card> affectedCards = new ArrayList<Card>();

        if (params.containsKey("AffectedZone")) {
            affectedCards.addAll(Singletons.getModel().getGame().getCardsIn(ZoneType.listValueOf(params.get("AffectedZone"))));
        } else {
            affectedCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        }

        if (params.containsKey("Affected") && !params.get("Affected").contains(",")) {
            if (params.get("Affected").contains("Self")) {
                affectedCards = CardLists.createCardList(hostCard);
            } else if (params.get("Affected").contains("EnchantedBy")) {
                affectedCards = CardLists.createCardList(hostCard.getEnchantingCard());
            } else if (params.get("Affected").contains("EquippedBy")) {
                affectedCards = CardLists.createCardList(hostCard.getEquippingCard());
            } else if (params.get("Affected").equals("EffectSource")) {
                affectedCards = new ArrayList<Card>(AbilityFactory.getDefinedCards(hostCard, params.get("Affected"), null));
                return affectedCards;
            }
        }

        if (params.containsKey("Affected")) {
            affectedCards = CardLists.getValidCards(affectedCards, params.get("Affected").split(","), controller, hostCard);
        }

        return affectedCards;
    }

}
