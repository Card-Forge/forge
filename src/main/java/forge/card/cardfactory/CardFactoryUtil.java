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
package forge.card.cardfactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Command;
import forge.Constant;
import forge.CounterType;
import forge.GameEntity;
import forge.Singletons;
import forge.card.CardCharacteristics;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.CommonDrawback;
import forge.card.abilityfactory.SpellEffect;
import forge.card.abilityfactory.ai.CanPlayAsDrawbackAi;
import forge.card.cost.Cost;
import forge.card.mana.ManaCostShard;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementHandler;
import forge.card.replacement.ReplacementLayer;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputPayManaCostUtil;
import forge.game.event.TokenCreatedEvent;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.match.CMatchUI;
import forge.util.Aggregates;
import forge.util.MyRandom;

import forge.view.ButtonUtil;

/**
 * <p>
 * CardFactoryUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardFactoryUtil {
    private static Random random = MyRandom.getRandom();

    /**
     * <p>
     * getMostExpensivePermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getMostExpensivePermanentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = list;
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }

        return CardFactoryUtil.getMostExpensivePermanentAI(all);
    }

    /**
     * getMostExpensivePermanentAI.
     * 
     * @param all
     *            the all
     * @return the card
     */
    public static Card getMostExpensivePermanentAI(final List<Card> all) {
        if (all.size() == 0) {
            return null;
        }
        Card biggest = null;
        biggest = all.get(0);

        int bigCMC = 0;
        for (int i = 0; i < all.size(); i++) {
            final Card card = all.get(i);
            int curCMC = card.getCMC();

            // Add all cost of all auras with the same controller
            final List<Card> auras = CardLists.filterControlledBy(card.getEnchantedBy(), card.getController());
            curCMC += Aggregates.sum(auras, CardPredicates.Accessors.fnGetCmc) + auras.size();

            if (curCMC >= bigCMC) {
                bigCMC = curCMC;
                biggest = all.get(i);
            }
        }

        return biggest;
    }

    // for Sarkhan the Mad
    /**
     * <p>
     * getCheapestCreatureAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getCheapestCreatureAI(List<Card> list, final SpellAbility spell, final boolean targeted) {
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isCreature();
            }
        });
        return CardFactoryUtil.getCheapestPermanentAI(list, spell, targeted);
    }

    /**
     * <p>
     * getCheapestPermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getCheapestPermanentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = list;
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
        if (all.size() == 0) {
            return null;
        }

        // get cheapest card:
        Card cheapest = null;
        cheapest = all.get(0);

        for (int i = 0; i < all.size(); i++) {
            if (cheapest.getManaCost().getCMC() <= cheapest.getManaCost().getCMC()) {
                cheapest = all.get(i);
            }
        }

        return cheapest;

    }

    /**
     * <p>
     * getBestLandAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestLandAI(final List<Card> list) {
        final List<Card> land = CardLists.filter(list, CardPredicates.Presets.LANDS);
        if (!(land.size() > 0)) {
            return null;
        }

        // prefer to target non basic lands
        final List<Card> nbLand = CardLists.filter(land, Predicates.not(CardPredicates.Presets.BASIC_LANDS));

        if (nbLand.size() > 0) {
            // TODO - Rank non basics?
            return Aggregates.random(nbLand);
        }

        // if no non-basic lands, target the least represented basic land type
        String sminBL = "";
        int iminBL = 20000; // hopefully no one will ever have more than 20000
                            // lands of one type....
        int n = 0;
        for (String name : Constant.Color.BASIC_LANDS) {
            n = CardLists.getType(land, name).size();
            if ((n < iminBL) && (n > 0)) {
                // if two or more are tied, only the
                // first
                // one checked will be used
                iminBL = n;
                sminBL = name;
            }
        }
        if (iminBL == 20000) {
            return null; // no basic land was a minimum
        }

        final List<Card> bLand = CardLists.getType(land, sminBL);

        for (Card ut : Iterables.filter(bLand, CardPredicates.Presets.UNTAPPED)) {
            return ut;
        }


        return Aggregates.random(bLand); // random tapped land of least represented type
    }

    // The AI doesn't really pick the best enchantment, just the most expensive.
    /**
     * <p>
     * getBestEnchantmentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestEnchantmentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.ENCHANTMENTS);
        if (targeted) {
            all = CardLists.filter(all, new Predicate<Card>() {

                @Override
                public boolean apply(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }

        // get biggest Enchantment
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnGetCmc);
    }

    // The AI doesn't really pick the best artifact, just the most expensive.
    /**
     * <p>
     * getBestArtifactAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestArtifactAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.ARTIFACTS);
        if (all.size() == 0) {
            return null;
        }
        // get biggest Artifact
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnGetCmc);
    }

    /**
     * <p>
     * doesCreatureAttackAI.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean doesCreatureAttackAI(final Player ai, final Card card) {
        final List<Card> att = ComputerUtil.getAttackers(ai).getAttackers();

        return att.contains(card);
    }

    /**
     * <p>
     * evaluateCreatureList.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int evaluateCreatureList(final List<Card> list) {
        int value = 0;
        for (int i = 0; i < list.size(); i++) {
            value += CardFactoryUtil.evaluateCreature(list.get(i));
        }

        return value;
    }

    /**
     * <p>
     * evaluatePermanentList.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int evaluatePermanentList(final List<Card> list) {
        int value = 0;
        for (int i = 0; i < list.size(); i++) {
            value += list.get(i).getCMC() + 1;
        }

        return value;
    }

    /**
     * <p>
     * evaluateCreature.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int evaluateCreature(final Card c) {

        int value = 100;
        if (c.isToken()) {
            value = 80; // tokens should be worth less than actual cards
        }
        int power = c.getNetCombatDamage();
        final int toughness = c.getNetDefense();
        for (String keyword : c.getKeyword()) {
            if (keyword.equals("Prevent all combat damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                power = 0;
                break;
            }
        }
        value += power * 15;
        value += toughness * 10;
        value += c.getCMC() * 5;

        // Evasion keywords
        if (c.hasKeyword("Flying")) {
            value += power * 10;
        }
        if (c.hasKeyword("Horsemanship")) {
            value += power * 10;
        }
        if (c.hasKeyword("Unblockable")) {
            value += power * 10;
        } else {
            if (c.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                value += power * 6;
            }
            if (c.hasKeyword("Fear")) {
                value += power * 6;
            }
            if (c.hasKeyword("Intimidate")) {
                value += power * 6;
            }
            if (c.hasStartOfKeyword("CARDNAME can't be blocked except by")) {
                value += power * 5;
            }
            if (c.hasStartOfKeyword("CARDNAME can't be blocked by")) {
                value += power * 2;
            }
        }

        // Other good keywords
        if (power > 0) {
            if (c.hasKeyword("Double Strike")) {
                value += 10 + (power * 15);
            } else if (c.hasKeyword("First Strike")) {
                value += 10 + (power * 5);
            }
            if (c.hasKeyword("Deathtouch")) {
                value += 25;
            }
            if (c.hasKeyword("Lifelink")) {
                value += power * 10;
            }
            if (power > 1 && c.hasKeyword("Trample")) {
                value += (power - 1) * 5;
            }
            if (c.hasKeyword("Vigilance")) {
                value += (power * 5) + (toughness * 5);
            }
            if (c.hasKeyword("Wither")) {
                value += power * 10;
            }
            if (c.hasKeyword("Infect")) {
                value += power * 15;
            }
            value += c.getKeywordMagnitude("Rampage");
            if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                    + "put into a graveyard, put a +1/+1 counter on CARDNAME.")) {
                value += 2;
            }
            if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                    + "put into a graveyard, put a +2/+2 counter on CARDNAME.")) {
                value += 3;
            }
        }

        value += c.getKeywordMagnitude("Bushido") * 16;
        value += c.getAmountOfKeyword("Flanking") * 15;
        value += c.getAmountOfKeyword("Exalted") * 15;
        value += c.getKeywordMagnitude("Annihilator") * 50;


        // Defensive Keywords
        if (c.hasKeyword("Reach") && !c.hasKeyword("Flying")) {
            value += 5;
        }
        if (c.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            value += 3;
        }

        // Protection
        if (c.hasKeyword("Indestructible")) {
            value += 70;
        }
        if (c.hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) {
            value += 60;
        } else if (c.hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) {
            value += 50;
        }
        if (c.hasKeyword("Hexproof")) {
            value += 35;
        } else if (c.hasKeyword("Shroud")) {
            value += 30;
        }
        if (c.hasStartOfKeyword("Protection")) {
            value += 20;
        }
        if (c.hasStartOfKeyword("PreventAllDamageBy")) {
            value += 10;
        }
        value += c.getKeywordMagnitude("Absorb") * 11;

        // Bad keywords
        if (c.hasKeyword("Defender") || c.hasKeyword("CARDNAME can't attack.")) {
            value -= (power * 9) + 40;
        } else if (c.getSVar("SacrificeEndCombat").equals("True")) {
            value -= 40;
        }
        if (c.hasKeyword("CARDNAME can't block.")) {
            value -= 10;
        } else if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
            value -= 10;
        } else if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= toughness * 5;
        }

        if (c.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            value -= (toughness - 1) * 9;
        }

        if (c.hasKeyword("CARDNAME can't attack or block.")) {
            value = 50 + (c.getCMC() * 5); // reset everything - useless
        }
        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")) {
            if (c.isTapped()) {
                value = 50 + (c.getCMC() * 5); // reset everything - useless
            } else {
                value -= 50;
            }
        }
        if (c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")
               || c.hasKeyword("At the beginning of the end step, exile CARDNAME.")
               || c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
            value -= 50;
        } else if (c.hasStartOfKeyword("Cumulative upkeep")) {
            value -= 30;
        } else if (c.hasStartOfKeyword("At the beginning of your upkeep, destroy CARDNAME unless you pay")
                || c.hasStartOfKeyword("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")
                || c.hasStartOfKeyword("Upkeep:")) {
            value -= 20;
        } else if (c.hasStartOfKeyword("(Echo unpaid)")) {
            value -= 10;
        }

        if (c.hasStartOfKeyword("At the beginning of your upkeep, CARDNAME deals")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("Fading")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("Vanishing")) {
            value -= 20;
        }
        if (c.getSVar("Targeting").equals("Dies")) {
            value -= 25;
        }

        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility()) {
                value += 10;
            }
        }
        if (!c.getManaAbility().isEmpty()) {
            value += 10;
        }

        if (c.isUntapped()) {
            value += 1;
        }

        // paired creatures are more valuable because they grant a bonus to the other creature
        if (c.isPaired()) {
            value += 14;
        }

        return value;

    } // evaluateCreature

    // returns null if list.size() == 0
    /**
     * <p>
     * getBestAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestAI(final List<Card> list) {
        // Get Best will filter by appropriate getBest list if ALL of the list
        // is of that type
        if (CardLists.getNotType(list, "Creature").size() == 0) {
            return CardFactoryUtil.getBestCreatureAI(list);
        }

        if (CardLists.getNotType(list, "Land").size() == 0) {
            return CardFactoryUtil.getBestLandAI(list);
        }

        // TODO - Once we get an EvaluatePermanent this should call
        // getBestPermanent()
        return CardFactoryUtil.getMostExpensivePermanentAI(list);
    }

    /**
     * getBestCreatureAI.
     * 
     * @param list
     *            the list
     * @return the card
     */
    public static Card getBestCreatureAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.CREATURES);
        return Aggregates.itemWithMax(all, CardPredicates.Accessors.fnEvaluateCreature);
    }

    // This selection rates tokens higher
    /**
     * <p>
     * getBestCreatureToBounceAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final List<Card> list) {
        final int tokenBonus = 40;
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.CREATURES);
        Card biggest = null; // returns null if list.size() == 0
        int biggestvalue = 0;
        int newvalue = 0;

        if (all.size() != 0) {
            biggest = all.get(0);

            for (int i = 0; i < all.size(); i++) {
                biggestvalue = CardFactoryUtil.evaluateCreature(biggest);
                if (biggest.isToken()) {
                    biggestvalue += tokenBonus; // raise the value of tokens
                }
                newvalue = CardFactoryUtil.evaluateCreature(all.get(i));
                if (all.get(i).isToken()) {
                    newvalue += tokenBonus; // raise the value of tokens
                }
                if (biggestvalue < newvalue) {
                    biggest = all.get(i);
                }
            }
        }
        return biggest;
    }

    /**
     * <p>
     * getWorstAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstAI(final List<Card> list) {
        return CardFactoryUtil.getWorstPermanentAI(list, false, false, false, false);
    }

    // returns null if list.size() == 0
    /**
     * <p>
     * getWorstCreatureAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstCreatureAI(final List<Card> list) {
        List<Card> all = CardLists.filter(list, CardPredicates.Presets.CREATURES);
        // get smallest creature
        return Aggregates.itemWithMin(all, CardPredicates.Accessors.fnEvaluateCreature);
    }

    /**
     * <p>
     * getWorstPermanentAI.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @param biasEnch
     *            a boolean.
     * @param biasLand
     *            a boolean.
     * @param biasArt
     *            a boolean.
     * @param biasCreature
     *            a boolean.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstPermanentAI(final List<Card> list, final boolean biasEnch, final boolean biasLand,
            final boolean biasArt, final boolean biasCreature) {
        if (list.size() == 0) {
            return null;
        }

        if (biasEnch && Iterables.any(list, CardPredicates.Presets.ENCHANTMENTS)) {
            return CardFactoryUtil.getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ENCHANTMENTS), null, false);
        }

        if (biasArt && Iterables.any(list, CardPredicates.Presets.ARTIFACTS)) {
            return CardFactoryUtil.getCheapestPermanentAI(CardLists.filter(list, CardPredicates.Presets.ARTIFACTS), null, false);
        }

        if (biasLand && Iterables.any(list, CardPredicates.Presets.LANDS)) {
            return CardFactoryUtil.getWorstLand(CardLists.filter(list, CardPredicates.Presets.LANDS));
        }

        if (biasCreature && Iterables.any(list, CardPredicates.Presets.CREATURES)) {
            return CardFactoryUtil.getWorstCreatureAI(CardLists.filter(list, CardPredicates.Presets.CREATURES));
        }

        List<Card> lands = CardLists.filter(list, CardPredicates.Presets.LANDS);
        if (lands.size() > 6) {
            return CardFactoryUtil.getWorstLand(lands);
        }

        if ((CardLists.getType(list, "Artifact").size() > 0) || (CardLists.getType(list, "Enchantment").size() > 0)) {
            return CardFactoryUtil.getCheapestPermanentAI(CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.isArtifact() || c.isEnchantment();
                }
            }), null, false);
        }

        if (CardLists.getType(list, "Creature").size() > 0) {
            return CardFactoryUtil.getWorstCreatureAI(CardLists.getType(list, "Creature"));
        }

        // Planeswalkers fall through to here, lands will fall through if there
        // aren't very many
        return CardFactoryUtil.getCheapestPermanentAI(list, null, false);
    }


    /**
     * <p>
     * inputDestroyNoRegeneration.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputDestroyNoRegeneration(final List<Card> choices, final String message) {
        final Input target = new Input() {
            private static final long serialVersionUID = -6637588517573573232L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage(message);
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.contains(card)) {
                    Singletons.getModel().getGame().getAction().destroyNoRegeneration(card);
                    this.stop();
                }
            }
        };
        return target;
    } // inputDestroyNoRegeneration()

    /**
     * <p>
     * abilityUnearth.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityActivated abilityUnearth(final Card sourceCard, final String manaCost) {

        final Cost cost = new Cost(sourceCard, manaCost, true);
        class AbilityUnearth extends AbilityActivated {
            public AbilityUnearth(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityUnearth(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
                restrict.setZone(ZoneType.Graveyard);
                restrict.setSorcerySpeed(true);
                res.setRestrictions(restrict);
                return res;
            }

            private static final long serialVersionUID = -5633945565395478009L;

            @Override
            public void resolve() {
                final Card card = Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);

                card.addIntrinsicKeyword("At the beginning of the end step, exile CARDNAME.");
                card.addIntrinsicKeyword("Haste");
                card.setUnearthed(true);
            }

            @Override
            public boolean canPlayAI() {
                PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
                if (phase.getPhase().isAfter(PhaseType.MAIN1) || !phase.isPlayerTurn(getActivatingPlayer())) {
                    return false;
                }
                return ComputerUtil.canPayCost(this, getActivatingPlayer());
            }
        }
        final AbilityActivated unearth = new AbilityUnearth(sourceCard, cost, null);

        final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
        restrict.setZone(ZoneType.Graveyard);
        restrict.setSorcerySpeed(true);
        unearth.setRestrictions(restrict);

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append("Unearth: ").append(sourceCard.getName());
        unearth.setStackDescription(sbStack.toString());

        return unearth;
    } // abilityUnearth()

    /**
     * <p>
     * abilityMorphDown.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final Card sourceCard) {
        final Spell morphDown = new Spell(sourceCard) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                Singletons.getModel().getGame().getAction().moveToPlay(sourceCard);
            }

            @Override
            public boolean canPlay() {
                //Lands do not have SpellPermanents.
                if (sourceCard.isLand()) {
                    return (Singletons.getModel().getGame().getZoneOf(sourceCard).is(ZoneType.Hand) || sourceCard.hasKeyword("May be played"))
                            && sourceCard.getController().canCastSorcery();
                }
                else {
                    return sourceCard.getSpellPermanent().canPlay();
                }
            }
        };

        morphDown.setManaCost("3");
        morphDown.setDescription("(You may cast this face down as a 2/2 creature for 3.)");
        morphDown.setStackDescription("Morph - Creature 2/2");
        morphDown.setCastFaceDown(true);

        return morphDown;
    }

    /**
     * <p>
     * abilityMorphUp.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param a
     *            a int.
     * @param d
     *            a int.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityStatic abilityMorphUp(final Card sourceCard, final Cost cost) {
        final AbilityStatic morphUp = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                if (sourceCard.turnFaceUp()) {
                    // Run triggers
                    final Map<String, Object> runParams = new TreeMap<String, Object>();
                    runParams.put("Card", sourceCard);
                    Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.TurnFaceUp, runParams);

                    StringBuilder sb = new StringBuilder();
                    sb.append(this.getActivatingPlayer()).append(" has unmorphed ");
                    sb.append(sourceCard.getName());
                    Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
                }
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && sourceCard.isInPlay();
            }

        }; // morph_up

        String costDesc = cost.toString();
        // get rid of the ": " at the end
        costDesc = costDesc.substring(0, costDesc.length() - 2);
        final StringBuilder sb = new StringBuilder();
        sb.append("Morph");
        if (!cost.isOnlyManaCost()) {
            sb.append(" -");
        }
        sb.append(" ").append(costDesc).append(" (Turn this face up any time for its morph cost.)");
        morphUp.setDescription(sb.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - turn this card face up.");
        morphUp.setStackDescription(sbStack.toString());

        return morphUp;
    }

    /**
     * <p>
     * abilityCycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityCycle(final Card sourceCard, String cycleCost) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ Draw | Cost$ ");
        sb.append(cycleCost);
        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ Cycling ");
        sb.append("| SpellDescription$ Draw a card.");

        AbilityFactory af = new AbilityFactory();
        SpellAbility cycle = af.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityCycle()

    /**
     * <p>
     * abilityTypecycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTypecycle(final Card sourceCard, String cycleCost, final String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("AB$ ChangeZone | Cost$ ").append(cycleCost);

        String desc = type;
        if (type.equals("Basic")) {
            desc = "Basic land";
        }

        sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ ").append(desc).append("cycling ");
        sb.append("| Origin$ Library | Destination$ Hand |");
        sb.append("ChangeType$ ").append(type);
        sb.append(" | SpellDescription$ Search your library for a ").append(desc).append(" card, reveal it,");
        sb.append(" and put it into your hand. Then shuffle your library.");

        AbilityFactory af = new AbilityFactory();
        SpellAbility cycle = af.getAbility(sb.toString(), sourceCard);
        cycle.setIsCycling(true);

        return cycle;
    } // abilityTypecycle()

    /**
     * <p>
     * abilityTransmute.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param transmuteCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityTransmute(final Card sourceCard, String transmuteCost) {
        transmuteCost += " Discard<1/CARDNAME>";
        final Cost abCost = new Cost(sourceCard, transmuteCost, true);
        class AbilityTransmute extends AbilityActivated {
            public AbilityTransmute(final Card ca, final Cost co, final Target t) {
                super(ca, co, t);
            }

            @Override
            public AbilityActivated getCopy() {
                AbilityActivated res = new AbilityTransmute(getSourceCard(),
                        getPayCosts(), getTarget() == null ? null : new Target(getTarget()));
                CardFactoryUtil.copySpellAbility(this, res);
                res.getRestrictions().setZone(ZoneType.Hand);
                return res;
            }

            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean canPlay() {
                return super.canPlay() && sourceCard.getController().canCastSorcery();
            }

            @Override
            public void resolve() {
                final List<Card> cards = sourceCard.getController().getCardsIn(ZoneType.Library);
                final List<Card> sameCost = new ArrayList<Card>();

                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).getManaCost().getCMC() == sourceCard.getManaCost().getCMC()) {
                        sameCost.add(cards.get(i));
                    }
                }

                if (sameCost.size() == 0) {
                    return;
                }

                final Card o = GuiChoose.oneOrNone("Select a card", sameCost);
                if (o != null) {
                    // ability.setTargetCard((Card)o);

                    sourceCard.getController().discard(sourceCard, this);
                    final Card c1 = o;

                    Singletons.getModel().getGame().getAction().moveToHand(c1);

                }
                sourceCard.getController().shuffle();
            }
        }
        final SpellAbility transmute = new AbilityTransmute(sourceCard, abCost, null);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Transmute (").append(abCost.toString());
        sbDesc.append("Search your library for a card with the same converted mana cost as this card, reveal it, ");
        sbDesc.append("and put it into your hand. Then shuffle your library. Transmute only as a sorcery.)");
        transmute.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" Transmute: Search your library ");
        sbStack.append("for a card with the same converted mana cost.)");
        transmute.setStackDescription(sbStack.toString());

        transmute.getRestrictions().setZone(ZoneType.Hand);
        return transmute;
    } // abilityTransmute()

    /**
     * <p>
     * abilitySuspend.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param suspendCost
     *            a {@link java.lang.String} object.
     * @param timeCounters
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilitySuspend(final Card sourceCard, final String suspendCost, final String timeCounters) {
        // be careful with Suspend ability, it will not hit the stack
        Cost cost = new Cost(sourceCard, suspendCost, true);
        final SpellAbility suspend = new AbilityStatic(sourceCard, cost, null) {
            @Override
            public boolean canPlay() {
                if (!(this.getRestrictions().canPlay(sourceCard, this))) {
                    return false;
                }

                if (sourceCard.isInstant() || sourceCard.hasKeyword("Flash")) {
                    return true;
                }

                return sourceCard.getOwner().canCastSorcery();
            }

            @Override
            public boolean canPlayAI() {
                return true;
                // Suspend currently not functional for the AI,
                // seems to be an issue with regaining Priority after Suspension
            }

            @Override
            public void resolve() {
                final Card c = Singletons.getModel().getGame().getAction().exile(sourceCard);

                int counters = AbilityFactory.calculateAmount(c, timeCounters, this);
                c.addCounter(CounterType.TIME, counters, true);

                StringBuilder sb = new StringBuilder();
                sb.append(this.getActivatingPlayer()).append(" has suspended ");
                sb.append(c.getName()).append("with ");
                sb.append(counters).append(" time counters on it.");
                Singletons.getModel().getGame().getGameLog().add("ResolveStack", sb.toString(), 2);
            }
        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Suspend ").append(timeCounters).append(" - ").append(cost.toSimpleString());
        suspend.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" suspending for ");
        sbStack.append(timeCounters).append(" turns.)");
        suspend.setStackDescription(sbStack.toString());

        suspend.getRestrictions().setZone(ZoneType.Hand);
        return suspend;
    } // abilitySuspend()

    /**
     * <p>
     * entersBattleFieldWithCounters.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param type
     *            a {@link forge.CounterType} object.
     * @param n
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command entersBattleFieldWithCounters(final Card c, final CounterType type, final int n) {
        final Command addCounters = new Command() {
            private static final long serialVersionUID = 4825430555490333062L;

            @Override
            public void execute() {
                c.addCounter(type, n, true);
            }
        };
        return addCounters;
    }

    /**
     * <p>
     * fading.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command fading(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.FADE, power);
    } // fading

    /**
     * <p>
     * vanishing.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command vanishing(final Card sourceCard, final int power) {
        return entersBattleFieldWithCounters(sourceCard, CounterType.TIME, power);
    } // vanishing

    // List<Card> choices are the only cards the user can successful select
    /**
     * <p>
     * inputTargetChampionSac.
     * </p>
     * 
     * @param crd
     *            a {@link forge.Card} object.
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param targeted
     *            a boolean.
     * @param free
     *            a boolean.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputTargetChampionSac(final Card crd, final SpellAbility spell, final List<Card> choices,
            final String message, final boolean targeted, final boolean free) {
        final Input target = new Input() {
            private static final long serialVersionUID = -3320425330743678663L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                Singletons.getModel().getGame().getAction().sacrifice(crd, null);
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.contains(card)) {
                    if (card == spell.getSourceCard()) {
                        Singletons.getModel().getGame().getAction().sacrifice(spell.getSourceCard(), null);
                        this.stop();
                    } else {
                        spell.getSourceCard().setChampionedCard(card);
                        Singletons.getModel().getGame().getAction().exile(card);

                        this.stop();

                        // Run triggers
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Card", spell.getSourceCard());
                        runParams.put("Championed", card);
                        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.Championed, runParams);
                    }
                }
            } // selectCard()
        };
        return target;
    } // inputTargetSpecific()

    /**
     * <p>
     * masterOfTheWildHuntInputTargetCreature.
     * </p>
     * 
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input masterOfTheWildHuntInputTargetCreature(final SpellAbility spell, final List<Card> choices,
            final Command paid) {
        final Input target = new Input() {
            private static final long serialVersionUID = -1779224307654698954L;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select target wolf to damage for ").append(spell.getSourceCard());
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                if (choices.size() == 0) {
                    this.stop();
                }
                if (choices.contains(card)) {
                    spell.setTargetCard(card);
                    paid.execute();
                    this.stop();
                }
            } // selectCard()
        };
        return target;
    } // masterOfTheWildHuntInputTargetCreature()

    /**
     * <p>
     * modularInput.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input modularInput(final SpellAbility ability, final Card card) {
        final Input modularInput = new Input() {

            private static final long serialVersionUID = 2322926875771867901L;

            @Override
            public void showMessage() {
                CMatchUI.SINGLETON_INSTANCE.showMessage("Select target artifact creature");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card2) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card2);
                if (card2.isCreature() && card2.isArtifact() && zone.is(ZoneType.Battlefield)
                        && card.canBeTargetedBy(ability)) {
                    ability.setTargetCard(card2);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Put ").append(card.getCounters(CounterType.P1P1));
                    sb.append(" +1/+1 counter/s from ").append(card);
                    sb.append(" on ").append(card2);
                    ability.setStackDescription(sb.toString());
                    Singletons.getModel().getGame().getStack().add(ability);
                    this.stop();
                }
            }
        };
        return modularInput;
    }

    /**
     * <p>
     * getNumberOfManaSymbolsControlledByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsControlledByColor(final String colorAbb, final Player player) {
        final List<Card> cards = player.getCardsIn(ZoneType.Battlefield);
        return CardFactoryUtil.getNumberOfManaSymbolsByColor(colorAbb, cards);
    }

    /**
     * <p>
     * getNumberOfManaSymbolsByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param cards
     *            a {@link forge.CardList} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final List<Card> cards) {
        int count = 0;
        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);
            if (!c.isToken()) {
                String manaCost = c.getManaCost().toString();
                manaCost = manaCost.trim();
                count += CardFactoryUtil.countOccurrences(manaCost, colorAbb);
            }
        }
        return count;
    }

    /**
     * <p>
     * multiplyCost.
     * </p>
     * 
     * @param manacost
     *            a {@link java.lang.String} object.
     * @param multiplier
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    public static String multiplyCost(final String manacost, final int multiplier) {
        if (multiplier == 0) {
            return "";
        }
        if (multiplier == 1) {
            return manacost;
        }

        final String[] tokenized = manacost.split("\\s");
        final StringBuilder sb = new StringBuilder();

        if (Character.isDigit(tokenized[0].charAt(0))) {
            // cost starts with "colorless" number cost
            int cost = Integer.parseInt(tokenized[0]);
            cost = multiplier * cost;
            tokenized[0] = "" + cost;
            sb.append(tokenized[0]);
        } else {
            if (tokenized[0].contains("<")) {
                final String[] advCostPart = tokenized[0].split("<");
                final String costVariable = advCostPart[1].split(">")[0];
                final String[] advCostPartValid = costVariable.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartValid[0]);
                num = multiplier * num;
                tokenized[0] = advCostPart[0] + "<" + num;
                if (advCostPartValid.length > 1) {
                    tokenized[0] = tokenized[0] + "/" + advCostPartValid[1];
                }
                tokenized[0] = tokenized[0] + ">";
                sb.append(tokenized[0]);
            } else {
                for (int i = 0; i < multiplier; i++) {
                    // tokenized[0] = tokenized[0] + " " + tokenized[0];
                    sb.append((" "));
                    sb.append(tokenized[0]);
                }
            }
        }

        for (int i = 1; i < tokenized.length; i++) {
            if (tokenized[i].contains("<")) {
                final String[] advCostParts = tokenized[i].split("<");
                final String costVariables = advCostParts[1].split(">")[0];
                final String[] advCostPartsValid = costVariables.split("\\/", 2);
                // multiply the number part of the cost object
                int num = Integer.parseInt(advCostPartsValid[0]);
                num = multiplier * num;
                tokenized[i] = advCostParts[0] + "<" + num;
                if (advCostPartsValid.length > 1) {
                    tokenized[i] = tokenized[i] + "/" + advCostPartsValid[1];
                }
                tokenized[i] = tokenized[i] + ">";
                sb.append((" "));
                sb.append(tokenized[i]);
            } else {
                for (int j = 0; j < multiplier; j++) {
                    // tokenized[i] = tokenized[i] + " " + tokenized[i];
                    sb.append((" "));
                    sb.append(tokenized[i]);
                }
            }
        }

        String result = sb.toString();
        System.out.println("result: " + result);
        result = result.trim();
        return result;
    }

    /**
     * <p>
     * isTargetStillValid.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isTargetStillValid(final SpellAbility ability, final Card target) {

        if (Singletons.getModel().getGame().getZoneOf(target) == null) {
            return false; // for tokens that disappeared
        }

        final Card source = ability.getSourceCard();
        final Target tgt = ability.getTarget();
        if (tgt != null) {
            // Reconfirm the Validity of a TgtValid, or if the Creature is still
            // a Creature
            if (tgt.doesTarget()
                    && !target.isValid(tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getSourceCard())) {
                return false;
            }

            // Check if the target is in the zone it needs to be in to be
            // targeted
            if (!Singletons.getModel().getGame().getZoneOf(target).is(tgt.getZone())) {
                return false;
            }
        } else {
            // If an Aura's target is removed before it resolves, the Aura
            // fizzles
            if (source.isAura() && !target.isInZone(ZoneType.Battlefield)) {
                return false;
            }
        }

        // Make sure it's still targetable as well
        return target.canBeTargetedBy(ability);
    }

    // does "target" have protection from "card"?
    /**
     * <p>
     * hasProtectionFrom.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasProtectionFrom(final Card card, final Card target) {
        if (target == null) {
            return false;
        }

        return target.hasProtectionFrom(card);
    }

    /**
     * <p>
     * isCounterable.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isCounterable(final Card c) {
        if (c.hasKeyword("CARDNAME can't be countered.") || !c.getCanCounter()) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * isCounterableBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public static boolean isCounterableBy(final Card c, final SpellAbility sa) {
        if (!CardFactoryUtil.isCounterable(c)) {
            return false;
        }
        //TODO: Add code for Autumn's Veil here

        return true;
    }

    /**
     * <p>
     * getExternalZoneActivationCards.
     * </p>
     * 
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> getExternalZoneActivationCards(final Player activator) {
        final List<Card> cl = new ArrayList<Card>();
        final Player opponent = activator.getOpponent();

        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Graveyard), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Exile), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Library), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(activator.getZone(ZoneType.Command), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(opponent.getZone(ZoneType.Exile), activator));
        cl.addAll(CardFactoryUtil.getActivateablesFromZone(opponent.getZone(ZoneType.Graveyard), activator));

        return cl;
    }

    /**
     * <p>
     * getActivateablesFromZone.
     * </p>
     * 
     * @param zone
     *            a PlayerZone object.
     * @param activator
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static List<Card> getActivateablesFromZone(final PlayerZone zone, final Player activator) {

        Iterable<Card> cl = zone.getCards();

        // Only check the top card of the library
        if (zone.is(ZoneType.Library)) {
            cl = Iterables.limit(cl, 1);
        }

        if (activator.equals(zone.getPlayer())) {
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (zone.is(ZoneType.Graveyard) && c.hasUnearth()) {
                        return true;
                    }

                    if (c.hasKeyword("You may look at this card.")) {
                        return true;
                    }

                    if (c.isLand()
                            && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost"))) {
                        return true;
                    }

                    for (final SpellAbility sa : c.getSpellAbility()) {
                        final ZoneType restrictZone = sa.getRestrictions().getZone();
                        if (zone.is(restrictZone)) {
                            return true;
                        }

                        if (sa.isSpell()
                                && (c.hasKeyword("May be played") || c.hasKeyword("May be played without paying its mana cost")
                                        || (c.hasStartOfKeyword("Flashback") && zone.is(ZoneType.Graveyard)))
                                && restrictZone.equals(ZoneType.Hand)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        } else {
            // the activator is not the owner of the card
            cl = Iterables.filter(cl, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {

                    if (c.hasStartOfKeyword("May be played by your opponent")
                            || c.hasKeyword("Your opponent may look at this card.")) {
                        return true;
                    }
                    return false;
                }
            });
        }
        return Lists.newArrayList(cl);
    }

    /**
     * <p>
     * countOccurrences.
     * </p>
     * 
     * @param arg1
     *            a {@link java.lang.String} object.
     * @param arg2
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countOccurrences(final String arg1, final String arg2) {

        int count = 0;
        int index = 0;
        while ((index = arg1.indexOf(arg2, index)) != -1) {
            ++index;
            ++count;
        }
        return count;
    }

    /**
     * <p>
     * parseMath.
     * </p>
     * 
     * @param l
     *            an array of {@link java.lang.String} objects.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] parseMath(final String[] l) {
        final String[] m = { "none" };
        if (l.length > 1) {
            m[0] = l[1];
        }

        return m;
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int objectXCount(final ArrayList<Object> objects, final String s, final Card source) {
        if (objects.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        int n = 0;

        if (s.startsWith("Amount")) {
            n = objects.size();
        }

        return CardFactoryUtil.doXMath(n, m, source);
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     * 
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int playerXCount(final ArrayList<Player> players, final String s, final Card source) {
        if (players.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        int n = 0;

        // count valid cards on the battlefield
        if (l[0].contains("Valid")) {
            final String restrictions = l[0].replace("Valid ", "");
            final String[] rest = restrictions.split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            cardsonbattlefield = CardLists.getValidCards(cardsonbattlefield, rest, players.get(0), source);

            n = cardsonbattlefield.size();

            return CardFactoryUtil.doXMath(n, m, source);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("CardsInHand")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Hand).size(), m, source);
            }
        }

        if (sq[0].contains("DomainPlayer")) {
            final List<Card> someCards = new ArrayList<Card>();
            someCards.addAll(players.get(0).getCardsIn(ZoneType.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!CardLists.getType(someCards, basic[i]).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, source);
        }

        if (sq[0].contains("CardsInLibrary")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Library).size(), m, source);
            }
        }

        if (sq[0].contains("CardsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Graveyard).size(), m, source);
            }
        }
        if (sq[0].contains("LandsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(CardLists.getType(players.get(0).getCardsIn(ZoneType.Graveyard), "Land").size(), m,
                        source);
            }
        }

        if (sq[0].contains("CreaturesInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCreaturesInPlay().size(), m, source);
            }
        }

        if (sq[0].contains("CardsInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(ZoneType.Battlefield).size(), m, source);
            }
        }

        if (sq[0].contains("LifeTotal")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getLife(), m, source);
            }
        }

        if (sq[0].contains("TopOfLibraryCMC")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(Aggregates.sum(players.get(0).getCardsIn(ZoneType.Library, 1), CardPredicates.Accessors.fnGetCmc),
                        m, source);
            }
        }

        if (sq[0].contains("LandsPlayed")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getNumLandsPlayed(), m, source);
            }
        }

        if (sq[0].contains("CardsDrawn")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getNumDrawnThisTurn(), m, source);
            }
        }

        if (sq[0].contains("AttackersDeclared")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getAttackersDeclaredThisTurn(), m, source);
            }
        }

        if (sq[0].equals("DamageDoneToPlayerBy")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(source.getDamageDoneToPlayerBy(players.get(0).getName()), m, source);
            }
        }

        return CardFactoryUtil.doXMath(n, m, source);
    }

    /**
     * parseSVar TODO - flesh out javadoc for this method.
     * 
     * @param hostCard
     *            the Card with the SVar on it
     * @param amount
     *            a String
     * @return the calculated number
     */
    public static int parseSVar(final Card hostCard, final String amount) {
        int num = 0;
        if (amount == null) {
            return num;
        }

        try {
            num = Integer.valueOf(amount);
        } catch (final NumberFormatException e) {
            num = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(amount).split("\\$")[1]);
        }

        return num;
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.SpellAbility} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String s, final SpellAbility sa) {

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        final String[] sq;
        sq = l[0].split("\\.");

        if (sa != null) {
            // Count$Kicked.<numHB>.<numNotHB>
            if (sq[0].startsWith("Kicked")) {
                if (sa.isKicked()) {
                    return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Kicked
                } else {
                    return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not Kicked
                }
            }
            if (sq[0].startsWith("Kicked")) {
                if (sa.isKicked()) {
                    return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Kicked
                } else {
                    return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not Kicked
                }
            }
        }
        return xCount(c, s);
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String s) {
        int n = 0;

        final Player cardController = c.getController();
        final Player oppController = cardController.getOpponent();
        final Player activePlayer = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        // accept straight numbers
        if (l[0].contains("Number$")) {
            final String number = l[0].replace("Number$", "");
            if (number.equals("ChosenNumber")) {
                return CardFactoryUtil.doXMath(c.getChosenNumber(), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(number), m, c);
            }
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].replace("Count$", "");
        }

        if (l[0].startsWith("SVar$")) {
            final String sVar = l[0].replace("SVar$", "");
            return CardFactoryUtil.doXMath(CardFactoryUtil.xCount(c, c.getSVar(sVar)), m, c);
        }

        // Manapool
        if (l[0].contains("ManaPool")) {
            final String color = l[0].split(":")[1];
            if (color.equals("All")) {
                return c.getController().getManaPool().totalMana();
            } else {
                return c.getController().getManaPool().getAmountOfColor(color);
            }
        }

        // count valid cards in the garveyard
        if (l[0].contains("ValidGrave")) {
            String restrictions = l[0].replace("ValidGrave ", "");
            restrictions = restrictions.replace("Count$", "");
            final String[] rest = restrictions.split(",");
            List<Card> cards = Singletons.getModel().getGame().getCardsIn(ZoneType.Graveyard);
            cards = CardLists.getValidCards(cards, rest, cardController, c);

            n = cards.size();

            return CardFactoryUtil.doXMath(n, m, c);
        }
        // count valid cards on the battlefield
        if (l[0].contains("Valid")) {
            String restrictions = l[0].replace("Valid ", "");
            restrictions = restrictions.replace("Count$", "");
            final String[] rest = restrictions.split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            cardsonbattlefield = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);

            n = cardsonbattlefield.size();

            return CardFactoryUtil.doXMath(n, m, c);
        }

        if (l[0].contains("ImprintedCardPower")) {
            if (c.getImprinted().size() > 0) {
                return c.getImprinted().get(0).getNetAttack();
            }
        }

        if (l[0].contains("ImprintedCardToughness")) {
            if (c.getImprinted().size() > 0) {
                return c.getImprinted().get(0).getNetDefense();
            }
        }

        if (l[0].contains("ImprintedCardManaCost")) {
            if (c.getImprinted().get(0).getCMC() > 0) {
                return c.getImprinted().get(0).getCMC();
            }
        }

        if (l[0].contains("GreatestPowerYouControl")) {
            final List<Card> list = c.getController().getCreaturesInPlay();
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].contains("GreatestPowerYouDontControl")) {
            final List<Card> list = c.getController().getOpponent().getCreaturesInPlay();
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].contains("HighestCMCRemembered")) {
            final List<Card> list = new ArrayList<Card>();
            int highest = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    list.add(Singletons.getModel().getGame().getCardState((Card) o));
                }
            }
            for (final Card crd : list) {
                if (crd.getCMC() > highest) {
                    highest = crd.getCMC();
                }
            }
            return highest;
        }

        if (l[0].contains("RememberedSumPower")) {
            final List<Card> list = new ArrayList<Card>();
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    list.add(Singletons.getModel().getGame().getCardState((Card) o));
                }
            }
            return Aggregates.sum(Iterables.filter(list, CardPredicates.Presets.hasSecondStrike), CardPredicates.Accessors.fnGetAttack);
        }

        if (l[0].contains("RememberedSize")) {
            return c.getRemembered().size();
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid")) {
            return CardFactoryUtil.doXMath(c.getXManaCostPaid(), m, c);
        }

        if (sq[0].equals("YouDrewThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getNumDrawnThisTurn(), m, c);
        }
        if (sq[0].equals("OppDrewThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getNumDrawnThisTurn(), m, c);
        }

        if (sq[0].equals("StormCount")) {
            return CardFactoryUtil.doXMath(Singletons.getModel().getGame().getStack().getCardsCastThisTurn().size() - 1, m, c);
        }

        if (sq[0].equals("DamageDoneThisTurn")) {
            return CardFactoryUtil.doXMath(c.getDamageDoneThisTurn(), m, c);
        }

        if (sq[0].equals("BloodthirstAmount")) {
            return CardFactoryUtil.doXMath(c.getController().getBloodthirstAmount(), m, c);
        }

        if (sq[0].contains("RegeneratedThisTurn")) {
            return CardFactoryUtil.doXMath(c.getRegeneratedThisTurn(), m, c);
        }

        List<Card> someCards = new ArrayList<Card>();

        // Complex counting methods

        // TriggeringObjects
        if (sq[0].startsWith("Triggered")) {
            return CardFactoryUtil.doXMath((Integer) c.getTriggeringObject(sq[0].substring(9)), m, c);
        }

        // Count$Domain
        if (sq[0].equals("Domain")) {
            someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            for (String basic : Constant.Color.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$ActivePlayerDomain
        if (sq[0].contains("ActivePlayerDomain")) {
            someCards.addAll(activePlayer.getCardsIn(ZoneType.Battlefield));
            for (String basic : Constant.Color.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$ColoredCreatures *a DOMAIN for creatures*
        if (sq[0].contains("ColoredCreatures")) {
            someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            someCards = CardLists.filter(someCards, Presets.CREATURES);

            final String[] colors = { "green", "white", "red", "blue", "black" };

            for (final String color : colors) {
                if (!CardLists.getColor(someCards, color).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$YourStartingLife
        if (sq[0].contains("YourStartingLife")) {
            return CardFactoryUtil.doXMath(cardController.getStartingLife(), m, c);
        }

        // Count$OppStartingLife
        if (sq[0].contains("OppStartingLife")) {
            return CardFactoryUtil.doXMath(oppController.getStartingLife(), m, c);
        }

        // Count$YourLifeTotal
        if (sq[0].contains("YourLifeTotal")) {
            return CardFactoryUtil.doXMath(cardController.getLife(), m, c);
        }

        // Count$OppLifeTotal
        if (sq[0].contains("OppLifeTotal")) {
            return CardFactoryUtil.doXMath(oppController.getLife(), m, c);
        }

        // Count$DefenderLifeTotal
        if (sq[0].contains("DefenderLifeTotal")) {
            Player defender = Singletons.getModel().getGame().getCombat().getDefendingPlayerRelatedTo(c);
            return CardFactoryUtil.doXMath(defender.getLife(), m, c);
        }

        //  Count$TargetedLifeTotal (targeted player's life total)
        if (sq[0].contains("TargetedLifeTotal")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility parent = sa.getParentTargetingPlayer();
                if (parent.getTarget() != null) {
                    for (final Object tgtP : parent.getTarget().getTargetPlayers()) {
                        if (tgtP instanceof Player) {
                            return CardFactoryUtil.doXMath(((Player) tgtP).getLife(), m, c);
                        }
                    }
                }
            }
        }

        if (sq[0].contains("LifeYouLostThisTurn")) {
            return CardFactoryUtil.doXMath(cardController.getLifeLostThisTurn(), m, c);
        }

        if (sq[0].contains("LifeOppLostThisTurn")) {
            int lost = 0;
            for (Player opp : cardController.getOpponents()) {
                lost += opp.getLifeLostThisTurn();
            }
            return CardFactoryUtil.doXMath(lost, m, c);
        }

        if (sq[0].equals("TotalDamageDoneByThisTurn")) {
            return CardFactoryUtil.doXMath(c.getTotalDamageDoneBy(), m, c);
        }

        // Count$YourPoisonCounters
        if (sq[0].contains("YourPoisonCounters")) {
            return CardFactoryUtil.doXMath(cardController.getPoisonCounters(), m, c);
        }

        // Count$OppPoisonCounters
        if (sq[0].contains("OppPoisonCounters")) {
            return CardFactoryUtil.doXMath(oppController.getPoisonCounters(), m, c);
        }

        // Count$OppDamageThisTurn
        if (sq[0].contains("OppDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getAssignedDamage(), m, c);
        }

        // Count$YourDamageThisTurn
        if (sq[0].contains("YourDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getAssignedDamage(), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("OppTypeDamageThisTurn")) {
            final String[] type = sq[0].split(" ");
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getAssignedDamage(type[1]), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("YourTypeDamageThisTurn")) {
            final String[] type = sq[0].split(" ");
            return CardFactoryUtil.doXMath(c.getController().getAssignedDamage(type[1]), m, c);
        }

        if (sq[0].contains("YourLandsPlayed")) {
            return CardFactoryUtil.doXMath(c.getController().getNumLandsPlayed(), m, c);
        }

        // Count$HighestLifeTotal
        if (sq[0].contains("HighestLifeTotal")) {
            return CardFactoryUtil.doXMath(
                    Aggregates.max(Singletons.getModel().getGame().getPlayers(), Player.Accessors.FN_GET_LIFE), m, c);
        }

        // Count$LowestLifeTotal
        if (sq[0].contains("LowestLifeTotal")) {
            return CardFactoryUtil.doXMath(
                    Aggregates.min(Singletons.getModel().getGame().getPlayers(), Player.Accessors.FN_GET_LIFE), m, c);
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            final List<Card> topcard = cardController.getCardsIn(ZoneType.Library, 1);
            return CardFactoryUtil.doXMath(Aggregates.sum(topcard, CardPredicates.Accessors.fnGetCmc), m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            List<Card> enchantedControllerInPlay = new ArrayList<Card>();
            if (c.getEnchantingCard() != null) {
                enchantedControllerInPlay = c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield);
                enchantedControllerInPlay = CardLists.getType(enchantedControllerInPlay, "Creature");
            }
            return enchantedControllerInPlay.size();
        }

        // Count$LowestLibrary
        if (sq[0].contains("LowestLibrary")) {
            return Aggregates.min(Singletons.getModel().getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Library));
        }

        // Count$Chroma.<mana letter>
        if (sq[0].contains("Chroma")) {
            return CardFactoryUtil.doXMath(
                    CardFactoryUtil.getNumberOfManaSymbolsControlledByColor(sq[1], cardController), m, c);
        }

        // Count$Hellbent.<numHB>.<numNotHB>
        if (sq[0].contains("Hellbent")) {
            if (cardController.hasHellbent()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Hellbent
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not
                                                                               // Hellbent
            }
        }

        // Count$Metalcraft.<numMC>.<numNotMC>
        if (sq[0].contains("Metalcraft")) {
            if (cardController.hasMetalcraft()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$FatefulHour.<numFH>.<numNotFH>
        if (sq[0].contains("FatefulHour")) {
            if (cardController.getLife() <= 5) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            final String strZone = sq[0].substring(11);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (c.getCastFrom() == realZone) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("Threshold")) {
            if (cardController.hasThreshold()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("Landfall")) {
            if (cardController.hasLandfall()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }
        if (sq[0].startsWith("Kicked")) {
            if (c.isOptionalAdditionalCostsPaid("Kicker")) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].contains("GraveyardWithGE20Cards")) {
            if (Aggregates.max(Singletons.getModel().getGame().getPlayers(), Player.Accessors.countCardsInZone(ZoneType.Graveyard)) >= 20) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            final Card csource = c;
            List<Card> cl = c.getDevoured();
            cl = CardLists.getValidCards(cl, validDevoured.split(","), csource.getController(), csource);
            return CardFactoryUtil.doXMath(cl.size(), m, c);
        }

        // Count$CardPower
        if (sq[0].contains("CardPower")) {
            return CardFactoryUtil.doXMath(c.getNetAttack(), m, c);
        }
        // Count$CardToughness
        if (sq[0].contains("CardToughness")) {
            return CardFactoryUtil.doXMath(c.getNetDefense(), m, c);
        }
        // Count$CardPowerPlusToughness
        if (sq[0].contains("CardSumPT")) {
            return CardFactoryUtil.doXMath((c.getNetAttack() + c.getNetDefense()), m, c);
        }
        // Count$SumPower_valid
        if (sq[0].contains("SumPower")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            List<Card> filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);
            int sumPower = 0;
            for (int i = 0; i < filteredCards.size(); i++) {
                sumPower += filteredCards.get(i).getManaCost().getCMC();
            }
            return CardFactoryUtil.doXMath(sumPower, m, c);
        }
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            if (sq[0].contains("Equipped") && c.isEquipping()) {
                return CardFactoryUtil.doXMath(CardUtil.getConvertedManaCost(c.getEquipping().get(0)), m, c);
            } else {
                return CardFactoryUtil.doXMath(CardUtil.getConvertedManaCost(c), m, c);
            }
        }
        // Count$SumCMC_valid
        if (sq[0].contains("SumCMC")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            List<Card> cardsonbattlefield = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            List<Card> filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cardController, c);
            return Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc);
        }
        // Count$CardNumColors
        if (sq[0].contains("CardNumColors")) {
            return CardFactoryUtil.doXMath(CardUtil.getColors(c).size(), m, c);
        }
        // Count$ChosenNumber
        if (sq[0].contains("ChosenNumber")) {
            return CardFactoryUtil.doXMath(c.getChosenNumber(), m, c);
        }
        // Count$CardCounters.<counterType>
        if (sq[0].contains("CardCounters")) {
            return CardFactoryUtil.doXMath(c.getCounters(CounterType.getType(sq[1])), m, c);
        }
        // Count$TotalCounters.<counterType>_<valid>
        if (sq[0].contains("TotalCounters")) {
            final String[] restrictions = l[0].split("_");
            final CounterType cType = CounterType.getType(restrictions[1]);
            final String[] validFilter = restrictions[2].split(",");
            List<Card> validCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            validCards = CardLists.getValidCards(validCards, validFilter, cardController, c);
            int cCount = 0;
            for (final Card card : validCards) {
                cCount += card.getCounters(cType);
            }
            return CardFactoryUtil.doXMath(cCount, m, c);
        }
        // Count$TimesKicked
        if (sq[0].contains("TimesKicked")) {
            return CardFactoryUtil.doXMath(c.getMultiKickerMagnitude(), m, c);
        }
        if (sq[0].contains("NumCounters")) {
            final int num = c.getCounters(CounterType.getType(sq[1]));
            return CardFactoryUtil.doXMath(num, m, c);
        }

        // Count$IfMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfMainPhase")) {
            final PhaseHandler cPhase = Singletons.getModel().getGame().getPhaseHandler();
            if (cPhase.getPhase().isMain() && cPhase.getPlayerTurn().equals(cardController)) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$M12Empires.<numIf>.<numIfNot>
        if (sq[0].contains("AllM12Empires")) {
            boolean has = c.getController().isCardInPlay("Crown of Empires");
            has &= c.getController().isCardInPlay("Scepter of Empires");
            has &= c.getController().isCardInPlay("Throne of Empires");
            if (has) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Count$ThisTurnEntered <ZoneDestination> <ZoneOrigin> <Valid>
        // or
        // Count$ThisTurnEntered <ZoneDestination> <Valid>
        if (sq[0].contains("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");
            ZoneType destination, origin;
            String validFilter;

            destination = ZoneType.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = ZoneType.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }

            final List<Card> res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);

            return CardFactoryUtil.doXMath(res.size(), m, c);
        }

        // Count$AttackersDeclared
        if (sq[0].contains("AttackersDeclared")) {
            return CardFactoryUtil.doXMath(cardController.getAttackersDeclaredThisTurn(), m, c);
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        if (sq[0].contains("ThisTurnCast") || sq[0].contains("LastTurnCast")) {

            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];

            List<Card> res;

            if (workingCopy[0].contains("This")) {
                res = CardUtil.getThisTurnCast(validFilter, c);
            } else {
                res = CardUtil.getLastTurnCast(validFilter, c);
            }

            final int ret = CardFactoryUtil.doXMath(res.size(), m, c);
            return ret;
        }

        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final List<Card> res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c);
            if (res.size() > 0) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        // if a card was ever written to count two different zones,
        // make sure they don't get added twice.
        boolean mf = false, my = false, mh = false;
        boolean of = false, oy = false, oh = false;

        if (sq[0].contains("YouCtrl")) {
            if (!mf) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
                mf = true;
            }
        }

        if (sq[0].contains("InYourYard")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Graveyard));
                my = true;
            }
        }

        if (sq[0].contains("InYourLibrary")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Library));
                my = true;
            }
        }

        if (sq[0].contains("InYourHand")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Hand));
                mh = true;
            }
        }

        if (sq[0].contains("OppCtrl")) {
            if (!of) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Battlefield));
                of = true;
            }
        }

        if (sq[0].contains("InOppYard")) {
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Graveyard));
                oy = true;
            }
        }

        if (sq[0].contains("InOppHand")) {
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Hand));
                oh = true;
            }
        }

        if (sq[0].contains("InChosenHand")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Hand));
                }
                oh = true;
            }
        }

        if (sq[0].contains("InChosenYard")) {
            if (!oh) {
                if (c.getChosenPlayer() != null) {
                    someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Graveyard));
                }
                oh = true;
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            if (!mf) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Battlefield));
            }
            if (!of) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Battlefield));
            }
        }

        if (sq[0].contains("InAllYards")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Graveyard));
            }
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Graveyard));
            }
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(Singletons.getModel().getGame().getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(ZoneType.Hand));
            }
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(ZoneType.Hand));
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedHand")) {
            for (final SpellAbility sa : c.getCharacteristics().getSpellAbility()) {
                final SpellAbility parent = sa.getParentTargetingPlayer();
                if (parent != null) {
                    if (parent.getTarget() != null) {
                        for (final Object tgtP : parent.getTarget().getTargetPlayers()) {
                            if (tgtP instanceof Player) {
                                someCards.addAll(((Player) tgtP).getCardsIn(ZoneType.Hand));
                            }
                        }
                    }
                }
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InEnchantedHand")) {
            GameEntity o = c.getEnchanting();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Hand));
            }
        }

        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = CardLists.filter(someCards, CardPredicates.isType(sq[1]));
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }
            someCards = CardLists.filter(someCards, CardPredicates.nameEquals(sq[1]));
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        if (sq[0].contains("Untapped")) {
            someCards = CardLists.filter(someCards, Presets.UNTAPPED);
        }

        if (sq[0].contains("Tapped")) {
            someCards = CardLists.filter(someCards, Presets.TAPPED);
        }

//        String sq0 = sq[0].toLowerCase();
//        for(String color : Constant.Color.ONLY_COLORS) {
//            if( sq0.contains(color) )
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        if (sq[0].contains("White")) {
            someCards = CardLists.filter(someCards, Presets.WHITE);
        }

        if (sq[0].contains("Blue")) {
            someCards = CardLists.filter(someCards, Presets.BLUE);
        }

        if (sq[0].contains("Black")) {
            someCards = CardLists.filter(someCards, Presets.BLACK);
        }

        if (sq[0].contains("Red")) {
            someCards = CardLists.filter(someCards, Presets.RED);
        }

        if (sq[0].contains("Green")) {
            someCards = CardLists.filter(someCards, Presets.GREEN);
        }

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (CardUtil.getColors(c).size() > 1);
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (CardUtil.getColors(c).size() == 1);
                }
            });
        }

        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            if (CardUtil.getColors(c).size() > 1) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        // 1/10 - Count$MaxCMCYouCtrl
        if (sq[0].contains("MaxCMC")) {
            int mmc = 0;
            int cmc = 0;
            for (int i = 0; i < someCards.size(); i++) {
                cmc = someCards.get(i).getManaCost().getCMC();
                if (cmc > mmc) {
                    mmc = cmc;
                }
            }

            return CardFactoryUtil.doXMath(mmc, m, c);
        }

        n = someCards.size();

        return CardFactoryUtil.doXMath(n, m, c);
    }

    private static int doXMath(final int num, final String m, final Card c) {
        if (m.equals("none")) {
            return num;
        }

        final String[] s = m.split("\\.");
        int secondaryNum = 0;

        try {
            if (s.length == 2) {
                secondaryNum = Integer.parseInt(s[1]);
            }
        } catch (final Exception e) {
            secondaryNum = CardFactoryUtil.xCount(c, c.getSVar(s[1]));
        }

        if (s[0].contains("Plus")) {
            return num + secondaryNum;
        } else if (s[0].contains("NMinus")) {
            return secondaryNum - num;
        } else if (s[0].contains("Minus")) {
            return num - secondaryNum;
        } else if (s[0].contains("Twice")) {
            return num * 2;
        } else if (s[0].contains("Thrice")) {
            return num * 3;
        } else if (s[0].contains("HalfUp")) {
            return (int) (Math.ceil(num / 2.0));
        } else if (s[0].contains("HalfDown")) {
            return (int) (Math.floor(num / 2.0));
        } else if (s[0].contains("ThirdUp")) {
            return (int) (Math.ceil(num / 3.0));
        } else if (s[0].contains("ThirdDown")) {
            return (int) (Math.floor(num / 3.0));
        } else if (s[0].contains("Negative")) {
            return num * -1;
        } else if (s[0].contains("Times")) {
            return num * secondaryNum;
        } else if (s[0].contains("Mod")) {
            return num % secondaryNum;
        } else if (s[0].contains("LimitMax")) {
            if (num < secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }
        } else if (s[0].contains("LimitMin")) {
            if (num > secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }

        } else {
            return num;
        }
    }

    /**
     * <p>
     * doXMath.
     * </p>
     * 
     * @param num
     *            a int.
     * @param m
     *            an array of {@link java.lang.String} objects.
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int doXMath(final int num, final String[] m, final Card c) {
        if (m.length == 0) {
            return num;
        }

        return CardFactoryUtil.doXMath(num, m[0], c);
    }

    /**
     * <p>
     * handlePaid.
     * </p>
     * 
     * @param paidList
     *            a {@link forge.CardList} object.
     * @param string
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int handlePaid(final List<Card> paidList, final String string, final Card source) {
        if (paidList == null) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return CardFactoryUtil.doXMath(0, splitString[1], source);
            } else {
                return 0;
            }
        }
        if (string.startsWith("Amount")) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return CardFactoryUtil.doXMath(paidList.size(), splitString[1], source);
            } else {
                return paidList.size();
            }

        }
        if (string.contains("Valid")) {
            final String[] m = { "none" };

            String valid = string.replace("Valid ", "");
            final String[] l;
            l = valid.split("/"); // separate the specification from any math
            valid = l[0];
            if (l.length > 1) {
                m[0] = l[1];
            }
            final List<Card> list = CardLists.getValidCards(paidList, valid, source.getController(), source);
            return CardFactoryUtil.doXMath(list.size(), m, source);
        }

        int tot = 0;
        for (final Card c : paidList) {
            tot += CardFactoryUtil.xCount(c, string);
        }

        return tot;
    }

    /**
     * <p>
     * inputUntapUpToNType.
     * </p>
     * 
     * @param n
     *            a int.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputUntapUpToNType(final int n, final String type) {
        final Input untap = new Input() {
            private static final long serialVersionUID = -2167059918040912025L;

            private final int stop = n;
            private int count = 0;
            private List<Card> choices = new ArrayList<Card>();;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select a ").append(type).append(" to untap");
                CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card) {
                Zone zone = Singletons.getModel().getGame().getZoneOf(card);
                if (card.isType(type) && zone.is(ZoneType.Battlefield) && !choices.contains(card)) {
                    card.untap();
                    choices.add(card);
                    this.count++;
                    if (this.count == this.stop) {
                        this.stop();
                    }
                }
            } // selectCard()
        };

        return untap;
    }

    /**
     * <p>
     * getMostProminentCardName.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCardName(final List<Card> list) {

        if (list.size() == 0) {
            return "";
        }

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            final String name = c.getName();
            Integer currentCnt = map.get(name);
            map.put(name, currentCnt == null ? Integer.valueOf(1) : Integer.valueOf(1 + currentCnt));
        } // for

        int max = 0;
        String maxName = "";

        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String type = entry.getKey();
            // Log.debug(type + " - " + entry.getValue());

            if (max < entry.getValue()) {
                max = entry.getValue();
                maxName = type;
            }
        }
        return maxName;
    }

    /**
     * <p>
     * getMostProminentCreatureType.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCreatureType(final List<Card> list) {

        if (list.size() == 0) {
            return "";
        }

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            final ArrayList<String> typeList = c.getType();

            for (final String var : typeList) {
                if (CardUtil.isACreatureType(var)) {
                    if (!map.containsKey(var)) {
                        map.put(var, 1);
                    } else {
                        map.put(var, map.get(var) + 1);
                    }
                }
            }
        } // for

        int max = 0;
        String maxType = "";

        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String type = entry.getKey();
            // Log.debug(type + " - " + entry.getValue());

            if (max < entry.getValue()) {
                max = entry.getValue();
                maxType = type;
            }
        }

        return maxType;
    }

    /**
     * <p>
     * isMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    public static boolean isMostProminentColor(final List<Card> list, final String color) {

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            for (final String color2 : CardUtil.getColors(c)) {
                if (color2.equals("colorless")) {
                    // nothing to do
                } else if (!map.containsKey(color2)) {
                    map.put(color2, 1);
                } else {
                    map.put(color2, map.get(color2) + 1);
                }
            }
        } // for

        if (map.isEmpty() || !map.containsKey(color)) {
            return false;
        }

        int num = map.get(color);

        for (final Entry<String, Integer> entry : map.entrySet()) {

            if (num < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * getMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentColor(final List<Card> list) {

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            for (final String color : CardUtil.getColors(c)) {
                if (color.equals("colorless")) {
                    // nothing to do
                } else if (!map.containsKey(color)) {
                    map.put(color, 1);
                } else {
                    map.put(color, map.get(color) + 1);
                }
            }
        } // for

        int max = 0;
        String maxColor = "";

        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String color = entry.getKey();
            Log.debug(color + " - " + entry.getValue());

            if (max < entry.getValue()) {
                max = entry.getValue();
                maxColor = color;
            }
        }

        return maxColor;
    }

    public static List<String> getColorByProminence(final List<Card> list) {
        final HashMap<String, Integer> counts = new HashMap<String, Integer>();
        for (String color : Constant.Color.ONLY_COLORS) {
            counts.put(color, 0);
        }
        for (Card c : list) {
            List<String> colors = c.determineColor().toStringList();
            for (String col : colors) {
                if (counts.containsKey(col)) {
                    counts.put(col.toString(), counts.get(col.toString()) + 1);
                }
            }
        }
        ArrayList<String> res = new ArrayList<String>(counts.keySet());
        Collections.sort(res, new Comparator<String>() {
            @Override
            public int compare(final String a, final String b) {
                return counts.get(b) - counts.get(a);
            }
        });

        return res;
    }

    /**
     * <p>
     * getUsableManaSources.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     */
    public static int getUsableManaSources(final Player player) {
        List<Card> list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility am : c.getAIPlayableMana()) {
                    if (am.canPlay()) {
                        return true;
                    }
                }
                return false;
            }
        });

        return list.size();
    }

    /**
     * <p>
     * makeToken.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param imageName
     *            a {@link java.lang.String} object.
     * @param controller
     *            a {@link forge.game.player.Player} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param types
     *            an array of {@link java.lang.String} objects.
     * @param baseAttack
     *            a int.
     * @param baseDefense
     *            a int.
     * @param intrinsicKeywords
     *            an array of {@link java.lang.String} objects.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> makeToken(final String name, final String imageName, final Player controller,
            final String manaCost, final String[] types, final int baseAttack, final int baseDefense,
            final String[] intrinsicKeywords) {
        final List<Card> list = new ArrayList<Card>();
        final Card c = new Card();
        c.setName(name);
        c.setImageName(imageName);

        // TODO - most tokens mana cost is 0, this needs to be fixed
        // c.setManaCost(manaCost);
        c.addColor(manaCost);
        c.setToken(true);

        for (final String t : types) {
            c.addType(t);
        }

        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);

        final int multiplier = controller.getTokenDoublersMagnitude();
        for (int i = 0; i < multiplier; i++) {
            Card temp = CardFactoryUtil.copyStats(c);

            for (final String kw : intrinsicKeywords) {
                if (kw.startsWith("HIDDEN")) {
                    temp.addExtrinsicKeyword(kw);
                    // extrinsic keywords won't survive the copyStats treatment
                } else {
                    temp.addIntrinsicKeyword(kw);
                }
            }
            temp.setOwner(controller);
            temp.setToken(true);
            CardFactoryUtil.parseKeywords(temp, temp.getName());
            CardFactoryUtil.postFactoryKeywords(temp);
            Singletons.getModel().getGame().getAction().moveToPlay(temp);
            list.add(temp);
        }
        
        Singletons.getModel().getGame().getEvents().post(new TokenCreatedEvent());

        return list;
    }

    /**
     * <p>
     * copyTokens.
     * </p>
     * 
     * @param tokenList
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> copyTokens(final List<Card> tokenList) {
        final List<Card> list = new ArrayList<Card>();

        for (Card thisToken : tokenList) {
            list.addAll(copySingleToken(thisToken));
        }

        return list;
    }

    public static List<Card> copySingleToken(Card thisToken) {
        final ArrayList<String> tal = thisToken.getType();
        final String[] tokenTypes = new String[tal.size()];
        tal.toArray(tokenTypes);

        final List<String> kal = thisToken.getIntrinsicKeyword();
        final String[] tokenKeywords = new String[kal.size()];
        kal.toArray(tokenKeywords);
        final List<Card> tokens = CardFactoryUtil.makeToken(thisToken.getName(), thisToken.getImageName(),
                thisToken.getController(), thisToken.getManaCost().toString(), tokenTypes, thisToken.getBaseAttack(),
                thisToken.getBaseDefense(), tokenKeywords);

        for (final Card token : tokens) {
            token.setColor(thisToken.getColor());
        }
        return tokens;
    }

    /**
     * <p>
     * getBushidoEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<Ability> getBushidoEffects(final Card c) {
        final ArrayList<String> keywords = c.getKeyword();
        final ArrayList<Ability> list = new ArrayList<Ability>();

        final Card crd = c;

        for (final String kw : keywords) {
            if (kw.contains("Bushido")) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                final int magnitude = Integer.parseInt(s);

                final Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        final Command untilEOT = new Command() {

                            private static final long serialVersionUID = 3014846051064254493L;

                            @Override
                            public void execute() {
                                if (crd.isInPlay()) {
                                    crd.addTempAttackBoost(-1 * magnitude);
                                    crd.addTempDefenseBoost(-1 * magnitude);
                                }
                            }
                        };

                        Singletons.getModel().getGame().getEndOfTurn().addUntil(untilEOT);

                        crd.addTempAttackBoost(magnitude);
                        crd.addTempDefenseBoost(magnitude);
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(c);
                sb.append(" - (Bushido) gets +");
                sb.append(magnitude);
                sb.append("/+");
                sb.append(magnitude);
                sb.append(" until end of turn.");
                ability.setStackDescription(sb.toString());

                list.add(ability);
            }
        }
        return list;
    }

    /**
     * <p>
     * getNeededXDamage.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int getNeededXDamage(final SpellAbility ability) {
        // when targeting a creature, make sure the AI won't overkill on X
        // damage
        final Card target = ability.getTargetCard();
        int neededDamage = -1;

        if ((target != null)) {
            neededDamage = target.getNetDefense() - target.getDamage();
        }

        return neededDamage;
    }

    /**
     * getWorstLand
     * <p/>
     * This function finds the worst land a player has in play based on: worst
     * 1. tapped, basic land 2. tapped, non-basic land 3. untapped, basic land
     * 4. untapped, non-basic land best
     * <p/>
     * This is useful when the AI needs to find one of its lands to sacrifice
     * 
     * @param player
     *            - AllZone.getHumanPlayer() or AllZone.getComputerPlayer()
     * @return the worst land found based on the description above
     */
    public static Card getWorstLand(final Player player) {
        final List<Card> lands = player.getLandsInPlay();
        return CardFactoryUtil.getWorstLand(lands);
    } // end getWorstLand

    /**
     * <p>
     * getWorstLand.
     * </p>
     * 
     * @param lands
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstLand(final List<Card> lands) {
        Card worstLand = null;
        int maxScore = 0;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 2 : 0;
            score += tmp.isBasicLand() ? 1 : 0;
            if (score >= maxScore) {
                worstLand = tmp;
                maxScore = score;
            }
        }
        return worstLand;
    } // end getWorstLand

    // may return null
    /**
     * <p>
     * getRandomCard.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getRandomCard(final List<Card> list) {
        if (list.size() == 0) {
            return null;
        }

        final int index = CardFactoryUtil.random.nextInt(list.size());
        return list.get(index);
    }

    /**
     * <p>
     * playLandEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void playLandEffects(final Card c) {
        final Player player = c.getController();

        // > 0 because land amount isn't incremented until after playLandEffects
        final boolean extraLand = player.getNumLandsPlayed() > 0;

        if (extraLand) {
            final List<Card> fastbonds = player.getCardsIn(ZoneType.Battlefield, "Fastbond");
            for (final Card f : fastbonds) {
                final SpellAbility ability = new Ability(f, "0") {
                    @Override
                    public void resolve() {
                        f.getController().addDamage(1, f);
                    }
                };
                ability.setStackDescription("Fastbond - Deals 1 damage to you.");

                Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * isNegativeCounter.
     * </p>
     * 
     * @param c
     *            a {@link forge.CounterType} object.
     * @return a boolean.
     */
    public static boolean isNegativeCounter(final CounterType c) {
        return (c == CounterType.AGE) || (c == CounterType.BLAZE) || (c == CounterType.BRIBERY) || (c == CounterType.DOOM)
                || (c == CounterType.ICE) || (c == CounterType.M1M1) || (c == CounterType.M0M2) || (c == CounterType.M0M1)
                || (c == CounterType.TIME);
    }

    /**
     * <p>
     * Copies stats like power, toughness, etc.
     * </p>
     * 
     * @param sim
     *            a {@link java.lang.Object} object.
     * @return a {@link forge.Card} object.
     */
    public static Card copyStats(final Card sim) {
        final Card c = new Card();

        c.setFlipCard(sim.isFlipCard());
        c.setDoubleFaced(sim.isDoubleFaced());
        c.setCurSetCode(sim.getCurSetCode());

        final CardCharacteristicName origState = sim.getCurState();
        for (final CardCharacteristicName state : sim.getStates()) {
            c.addAlternateState(state);
            c.setState(state);
            sim.setState(state);
            CardFactoryUtil.copyCharacteristics(sim, c);
        }

        sim.setState(origState);
        c.setState(origState);

        return c;
    } // copyStats()

    /**
     * Copy characteristics.
     * 
     * @param from
     *            the from
     * @param to
     *            the to
     */
    public static void copyCharacteristics(final Card from, final Card to) {
        to.setBaseAttack(from.getBaseAttack());
        to.setBaseDefense(from.getBaseDefense());
        to.setBaseLoyalty(from.getBaseLoyalty());
        to.setBaseAttackString(from.getBaseAttackString());
        to.setBaseDefenseString(from.getBaseDefenseString());
        to.setIntrinsicKeyword(from.getIntrinsicKeyword());
        to.setName(from.getName());
        to.setType(from.getCharacteristics().getType());
        to.setText(from.getSpellText());
        to.setManaCost(from.getManaCost());
        to.setColor(from.getColor());
        to.setCardColorsOverridden(from.isCardColorsOverridden());
        to.setSVars(from.getSVars());
        to.setSets(from.getSets());
        to.setIntrinsicAbilities(from.getIntrinsicAbilities());

        to.setImageName(from.getImageName());
        to.setImageFilename(from.getImageFilename());
        to.setTriggers(from.getTriggers());
        to.setReplacementEffects(from.getReplacementEffects());
        to.setStaticAbilityStrings(from.getStaticAbilityStrings());

    }

    /**
     * Copy characteristics.
     * 
     * @param from
     *            the from
     * @param stateToCopy
     *            the state to copy
     * @param to
     *            the to
     */
    public static void copyState(final Card from, final CardCharacteristicName stateToCopy, final Card to) {

        // copy characteristics not associated with a state
        to.setBaseLoyalty(from.getBaseLoyalty());
        to.setBaseAttackString(from.getBaseAttackString());
        to.setBaseDefenseString(from.getBaseDefenseString());
        to.setText(from.getSpellText());

        // get CardCharacteristics for desired state
        CardCharacteristics characteristics = from.getState(stateToCopy);
        to.getCharacteristics().copy(characteristics);
        // handle triggers and replacement effect through Card class interface
        to.setTriggers(characteristics.getTriggers());
        to.setReplacementEffects(characteristics.getReplacementEffects());
    }

    public static void copySpellAbility(SpellAbility from, SpellAbility to) {
        to.setDescription(from.getDescription());
        to.setStackDescription(from.getDescription());

        if (from.getSubAbility() != null) {
            to.setSubAbility(from.getSubAbility().getCopy());
        }
        if (from.getRestrictions() != null) {
            to.setRestrictions(from.getRestrictions());
        }
        if (from.getConditions() != null) {
            to.setConditions(from.getConditions());
        }

        for (String sVar : from.getSVars()) {
            to.setSVar(sVar, from.getSVar(sVar));
        }
    }

    public static void correctAbilityChainSourceCard(final SpellAbility sa, final Card card) {

        sa.setSourceCard(card);

        if (sa.getSubAbility() != null) {
            correctAbilityChainSourceCard(sa.getSubAbility(), card);
        }
    }

    /**
     * Adds the ability factory abilities.
     * 
     * @param card
     *            the card
     */
    public static final void addAbilityFactoryAbilities(final Card card) {
        // **************************************************
        // AbilityFactory cards
        final ArrayList<String> ia = card.getIntrinsicAbilities();
        if (ia.size() > 0) {
            for (int i = 0; i < ia.size(); i++) {
                final AbilityFactory af = new AbilityFactory();
                // System.out.println(cardName);
                final SpellAbility sa = af.getAbility(ia.get(i), card);
                if (sa.hasParam("SetAsKicked")) {
                    sa.addOptionalAdditionalCosts("Kicker");
                }
                card.addSpellAbility(sa);
            }
        }
    }

    /**
     * <p>
     * postFactoryKeywords.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public static void postFactoryKeywords(final Card card) {
        // this function should handle any keywords that need to be added after
        // a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found

        // TODO - certain cards have two different kicker types, kicker will
        // need
        // to be written differently to handle this
        // TODO - kicker costs can only be mana right now i think?
        // TODO - this kicker only works for pemanents. maybe we can create an
        // optional cost class for buyback, kicker, that type of thing

        if (CardFactoryUtil.hasKeyword(card, "Multikicker") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Multikicker");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("kicker ");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsMultiKicker(true);
                sa.setMultiKickerManaCost(k[1]);
            }
        }

        if (CardFactoryUtil.hasKeyword(card, "Replicate") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Replicate");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                final String[] k = parse.split("cate ");

                final SpellAbility sa = card.getSpellAbility()[0];
                sa.setIsReplicate(true);
                sa.setReplicateManaCost(k[1]);
            }
        }

        final int evokeKeyword = CardFactoryUtil.hasKeyword(card, "Evoke");
        if (evokeKeyword != -1) {
            final SpellAbility evokedSpell = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;

                @Override
                public void resolve() {
                    card.setEvoked(true);
                    Singletons.getModel().getGame().getAction().moveToPlay(card);
                }

                @Override
                public boolean canPlayAI() {
                    if (!SpellPermanent.checkETBEffects(card, this.getActivatingPlayer())) {
                        return false;
                    }
                    return super.canPlayAI();
                }
            };
            final String parse = card.getKeyword().get(evokeKeyword).toString();
            card.removeIntrinsicKeyword(parse);

            final String[] k = parse.split(":");
            final String evokedCost = k[1];

            evokedSpell.setManaCost(evokedCost);

            final StringBuilder desc = new StringBuilder();
            desc.append("Evoke ").append(evokedCost);
            desc.append(" (You may cast this spell for its evoke cost. ");
            desc.append("If you do, when it enters the battlefield, sacrifice it.)");

            evokedSpell.setDescription(desc.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Evoked)");
            evokedSpell.setStackDescription(sb.toString());

            card.addSpellAbility(evokedSpell);
        }

        if (CardFactoryUtil.hasKeyword(card, "Cycling") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Cycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                card.addSpellAbility(CardFactoryUtil.abilityCycle(card, manacost));
            }
        } // Cycling

        while (CardFactoryUtil.hasKeyword(card, "TypeCycling") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "TypeCycling");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String type = k[1];
                final String manacost = k[2];

                card.addSpellAbility(CardFactoryUtil.abilityTypecycle(card, manacost, type));
            }
        } // TypeCycling

        if (CardFactoryUtil.hasKeyword(card, "Transmute") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Transmute");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                card.addSpellAbility(CardFactoryUtil.abilityTransmute(card, manacost));
            }
        } // transmute

        int shiftPos = CardFactoryUtil.hasKeyword(card, "Soulshift");
        while (shiftPos != -1) {
            final int n = shiftPos;
            final String parse = card.getKeyword().get(n).toString();
            final String[] k = parse.split(" ");
            final int manacost = Integer.parseInt(k[1]);

            final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                    + "| OptionalDecider$ You | ValidCard$ Card.Self | Execute$ SoulshiftAbility"
                    + "| TriggerController$ TriggeredCardController | TriggerDescription$ " + parse
                    + " (When this creature dies, you may return target Spirit card with converted mana cost "
                    + manacost + " or less from your graveyard to your hand.)";
            final String abString = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                    + "| ValidTgts$ Spirit.YouOwn+cmcLE" + manacost;
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("SoulshiftAbility", abString);
            shiftPos = CardFactoryUtil.hasKeyword(card, "Soulshift", n + 1);
        } // Soulshift

        if (CardFactoryUtil.hasKeyword(card, "Echo") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Echo");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String manacost = k[1];

                card.setEchoCost(manacost);

                final Command intoPlay = new Command() {

                    private static final long serialVersionUID = -7913835645603984242L;

                    @Override
                    public void execute() {
                        card.addExtrinsicKeyword("(Echo unpaid)");
                    }
                };
                card.addComesIntoPlayCommand(intoPlay);

            }
        } // echo

        if (CardFactoryUtil.hasKeyword(card, "Suspend") != -1) {
            // Suspend:<TimeCounters>:<Cost>
            final int n = CardFactoryUtil.hasKeyword(card, "Suspend");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                card.setSuspend(true);
                final String[] k = parse.split(":");

                final String timeCounters = k[1];
                final String cost = k[2];
                card.addSpellAbility(CardFactoryUtil.abilitySuspend(card, cost, timeCounters));
            }
        } // Suspend

        int xCount = card.getManaCost().getShardCount(ManaCostShard.X);
        if (xCount > 0) {
            final SpellAbility sa = card.getSpellAbility()[0];
            sa.setIsXCost(true);
            sa.setXManaCost(Integer.toString(xCount));
        } // X

        int cardnameSpot = CardFactoryUtil.hasKeyword(card, "CARDNAME is ");
        if (cardnameSpot != -1) {
            String color = "1";
            while (cardnameSpot != -1) {
                if (cardnameSpot != -1) {
                    final String parse = card.getKeyword().get(cardnameSpot).toString();
                    card.removeIntrinsicKeyword(parse);
                    color += " "
                            + InputPayManaCostUtil.getShortColorString(parse.replace("CARDNAME is ", "").replace(".",
                                    ""));
                    cardnameSpot = CardFactoryUtil.hasKeyword(card, "CARDNAME is ");
                }
            }
            card.addColor(color);
        }

        if (CardFactoryUtil.hasKeyword(card, "Fading") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Fading");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();

                final String[] k = parse.split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(CardFactoryUtil.fading(card, power));
            }
        } // Fading

        if (CardFactoryUtil.hasKeyword(card, "Vanishing") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Vanishing");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();

                final String[] k = parse.split(":");
                final int power = Integer.parseInt(k[1]);

                card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
            }
        } // Vanishing

        // AddCost
        if (!card.getSVar("FullCost").equals("")) {
            final SpellAbility[] abilities = card.getSpellAbility();
            if ((abilities.length > 0) && abilities[0].isSpell()) {
                final String altCost = card.getSVar("FullCost");
                final Cost abCost = new Cost(card, altCost, abilities[0].isAbility());
                abilities[0].setPayCosts(abCost);
            }
        }

        // AltCost
        if (!card.getSVar("AltCost").equals("")) {
            final SpellAbility[] abilities = card.getSpellAbility();
            if ((abilities.length > 0) && abilities[0].isSpell()) {
                String altCost = card.getSVar("AltCost");
                final HashMap<String, String> mapParams = new HashMap<String, String>();
                String altCostDescription = "";
                final String[] altCosts = altCost.split("\\|");

                for (int aCnt = 0; aCnt < altCosts.length; aCnt++) {
                    altCosts[aCnt] = altCosts[aCnt].trim();
                }

                for (final String altCost2 : altCosts) {
                    final String[] aa = altCost2.split("\\$");

                    for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                        aa[aaCnt] = aa[aaCnt].trim();
                    }

                    if (aa.length != 2) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("StaticEffectFactory Parsing Error: Split length of ");
                        sb.append(altCost2).append(" in ").append(card.getName()).append(" is not 2.");
                        throw new RuntimeException(sb.toString());
                    }

                    mapParams.put(aa[0], aa[1]);
                }

                altCost = mapParams.get("Cost");

                if (mapParams.containsKey("Description")) {
                    altCostDescription = mapParams.get("Description");
                }

                final SpellAbility sa = abilities[0];
                final SpellAbility altCostSA = sa.copy();

                final Cost abCost = new Cost(card, altCost, altCostSA.isAbility());
                altCostSA.setPayCosts(abCost);

                final StringBuilder sb = new StringBuilder();

                if (!altCostDescription.equals("")) {
                    sb.append(altCostDescription);
                } else {
                    sb.append("You may ").append(abCost.toStringAlt());
                    sb.append(" rather than pay ").append(card.getName()).append("'s mana cost.");
                }

                final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
                restriction.setRestrictions(mapParams);
                if (!mapParams.containsKey("ActivationZone")) {
                    restriction.setZone(ZoneType.Hand);
                }
                altCostSA.setRestrictions(restriction);
                altCostSA.setDescription(sb.toString());
                altCostSA.setBasicSpell(false);

                card.addSpellAbility(altCostSA);
            }
        }

        if (card.hasKeyword("Delve")) {
            card.getSpellAbilities().get(0).setDelve(true);
        }

        if (card.hasStartOfKeyword("Haunt")) {
            final int hauntPos = card.getKeywordPosition("Haunt");
            final String[] splitKeyword = card.getKeyword().get(hauntPos).split(":");
            final String hauntSVarName = splitKeyword[1];
            final String abilityDescription = splitKeyword[2];
            final String hauntAbilityDescription = abilityDescription.substring(0, 1).toLowerCase()
                    + abilityDescription.substring(1);
            String hauntDescription;
            if (card.isCreature()) {
                final StringBuilder sb = new StringBuilder();
                sb.append("When ").append(card.getName());
                sb.append(" enters the battlefield or the creature it haunts dies, ");
                sb.append(hauntAbilityDescription);
                hauntDescription = sb.toString();
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("When the creature ").append(card.getName());
                sb.append(" haunts dies, ").append(hauntAbilityDescription);
                hauntDescription = sb.toString();
            }

            card.getKeyword().remove(hauntPos);

            // First, create trigger that runs when the haunter goes to the
            // graveyard
            final StringBuilder sbHaunter = new StringBuilder();
            sbHaunter.append("Mode$ ChangesZone | Origin$ Battlefield | ");
            sbHaunter.append("Destination$ Graveyard | ValidCard$ Card.Self | ");
            sbHaunter.append("Static$ True | Secondary$ True | TriggerDescription$ Blank");

            final Trigger haunterDies = forge.card.trigger.TriggerHandler
                    .parseTrigger(sbHaunter.toString(), card, true);

            final Ability haunterDiesWork = new Ability(card, "0") {
                @Override
                public void resolve() {
                    this.getTargetCard().addHauntedBy(card);
                    Singletons.getModel().getGame().getAction().exile(card);
                }
            };
            haunterDiesWork.setDescription(hauntDescription);

            final Input target = new Input() {
                private static final long serialVersionUID = 1981791992623774490L;

                @Override
                public void showMessage() {
                    CMatchUI.SINGLETON_INSTANCE.showMessage("Choose target creature to haunt.");
                    ButtonUtil.disableAll();
                }

                @Override
                public void selectCard(final Card c) {
                    Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                    if (!zone.is(ZoneType.Battlefield) || !c.isCreature()) {
                        return;
                    }
                    if (c.canBeTargetedBy(haunterDiesWork)) {
                        haunterDiesWork.setTargetCard(c);
                        Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                        this.stop();
                    } else {
                        CMatchUI.SINGLETON_INSTANCE
                                .showMessage("Cannot target this card (Shroud? Protection?).");
                    }
                }
            };

            final Ability haunterDiesSetup = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final List<Card> creats = CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
                    for (int i = 0; i < creats.size(); i++) {
                        if (!creats.get(i).canBeTargetedBy(this)) {
                            creats.remove(i);
                            i--;
                        }
                    }
                    if (creats.size() == 0) {
                        return;
                    }

                    // need to do it this way because I don't know quite how to
                    // make TriggerHandler respect BeforePayMana.
                    if (card.getController().isHuman()) {
                        Singletons.getModel().getMatch().getInput().setInput(target);
                    } else {
                        // AI choosing what to haunt
                        final List<Card> oppCreats = CardLists.filterControlledBy(creats, card.getController().getOpponent());
                        if (oppCreats.size() != 0) {
                            haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(oppCreats));
                        } else {
                            haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(creats));
                        }
                        Singletons.getModel().getGame().getStack().add(haunterDiesWork);
                    }
                }
            };

            haunterDies.setOverridingAbility(haunterDiesSetup);

            // Second, create the trigger that runs when the haunted creature
            // dies
            final StringBuilder sbDies = new StringBuilder();
            sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ");
            sbDies.append("ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
            sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

            final Trigger hauntedDies = forge.card.trigger.TriggerHandler.parseTrigger(sbDies.toString(), card, true);

            // Third, create the trigger that runs when the haunting creature
            // enters the battlefield
            final StringBuilder sbETB = new StringBuilder();
            sbETB.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ ");
            sbETB.append(hauntSVarName).append(" | Secondary$ True | TriggerDescription$ ");
            sbETB.append(hauntDescription);

            final Trigger haunterETB = forge.card.trigger.TriggerHandler.parseTrigger(sbETB.toString(), card, true);

            // Fourth, create a trigger that removes the haunting status if the
            // haunter leaves the exile
            final StringBuilder sbUnExiled = new StringBuilder();
            sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | Destination$ Any | ");
            sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
            sbUnExiled.append("TriggerDescription$ Blank");

            final Trigger haunterUnExiled = forge.card.trigger.TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
                    true);

            final Ability haunterUnExiledWork = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getHaunting() != null) {
                        card.getHaunting().removeHauntedBy(card);
                        card.setHaunting(null);
                    }
                }
            };

            haunterUnExiled.setOverridingAbility(haunterUnExiledWork);

            // Fifth, add all triggers and abilities to the card.
            if (card.isCreature()) {
                card.addTrigger(haunterETB);
                card.addTrigger(haunterDies);
            } else {
                final AbilityFactory af = new AbilityFactory();
                final String abString = card.getSVar(hauntSVarName).replace("AB$", "SP$")
                        .replace("Cost$ 0", "Cost$ " + card.getManaCost())
                        + " | SpellDescription$ " + abilityDescription;

                final SpellAbility sa = af.getAbility(abString, card);
                card.addSpellAbility(sa);
            }

            card.addTrigger(hauntedDies);
            card.addTrigger(haunterUnExiled);
        }

        if (card.hasKeyword("Provoke")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | "
                    + "OptionalDecider$ You | Execute$ ProvokeAbility | Secondary$ True | TriggerDescription$ "
                    + "When this attacks, you may have target creature defending player "
                    + "controls untap and block it if able.";
            final String abString = "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | "
                    + "TgtPrompt$ Select target creature defending player controls | SubAbility$ DBUntap";
            final String dbString = "DB$ Untap | Defined$ Targeted";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("ProvokeAbility", abString);
            card.setSVar("DBUntap", dbString);
        }

        if (card.hasKeyword("Epic")) {
            final SpellAbility origSA = card.getSpellAbilities().get(0);

            final SpellAbility newSA = new Spell(card, origSA.getPayCosts(), origSA.getTarget()) {
                private static final long serialVersionUID = -7934420043356101045L;

                @Override
                public void resolve() {

                    String name = card.toString() + " Epic";
                    if (card.getController().getCardsIn(ZoneType.Battlefield, name).isEmpty()) {
                        // Create Epic emblem
                        final Card eff = new Card();
                        eff.setName(card.toString() + " Epic");
                        eff.addType("Effect"); // Or Emblem
                        eff.setToken(true); // Set token to true, so when leaving
                                            // play it gets nuked
                        eff.addController(card.getController());
                        eff.setOwner(card.getController());
                        eff.setImageName(card.getImageName());
                        eff.setColor(card.getColor());
                        eff.setImmutable(true);
                        eff.setEffectSource(card);

                        eff.addStaticAbility("Mode$ CantBeCast | ValidCard$ Card | Caster$ You "
                                + "| Description$ For the rest of the game, you can't cast spells.");

                        eff.setSVar("EpicCopy", "AB$ CopySpell | Cost$ 0 | Defined$ EffectSource");

                        final Trigger copyTrigger = forge.card.trigger.TriggerHandler.parseTrigger(
                                "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | Execute$ EpicCopy | TriggerDescription$ "
                                        + "At the beginning of each of your upkeeps, copy " + card.toString()
                                        + " except for its epic ability.", eff, false);

                        eff.addTrigger(copyTrigger);

                        Singletons.getModel().getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                        Singletons.getModel().getGame().getAction().moveToPlay(eff);
                        Singletons.getModel().getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
                    }

                    if (card.getController().isHuman()) {
                        Singletons.getModel().getGame().getAction().playSpellAbilityNoStack(origSA, false);
                    } else {
                        ComputerUtil.playNoStack(card.getController(), origSA);
                    }
                }
            };
            newSA.setDescription(origSA.getDescription());

            origSA.setPayCosts(null);
            origSA.setManaCost("0");

            card.clearSpellAbility();
            card.addSpellAbility(newSA);
        }

        if (card.hasKeyword("Soulbond")) {
            // Setup ETB trigger for card with Soulbond keyword
            final String actualTriggerSelf = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ TrigBondOther | OptionalDecider$ You | "
                    + "IsPresent$ Creature.Other+YouCtrl+NotPaired | Secondary$ True | "
                    + "TriggerDescription$ When CARDNAME enters the battlefield, "
                    + "you may pair CARDNAME with another unpaired creature you control";
            final String abStringSelf = "AB$ Bond | Cost$ 0 | Defined$ Self | ValidCards$ Creature.Other+YouCtrl+NotPaired";
            final Trigger parsedTriggerSelf = TriggerHandler.parseTrigger(actualTriggerSelf, card, true);
            card.addTrigger(parsedTriggerSelf);
            card.setSVar("TrigBondOther", abStringSelf);
            // Setup ETB trigger for other creatures you control
            final String actualTriggerOther = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | OptionalDecider$ You | "
                    + "Execute$ TrigBondSelf | IsPresent$ Creature.Self+NotPaired | Secondary$ True | "
                    + " TriggerDescription$ When another unpaired creature you control enters the battlefield, "
                    + "you may pair it with CARDNAME";
            final String abStringOther = "AB$ Bond | Cost$ 0 | Defined$ TriggeredCard | ValidCards$ Creature.Self+NotPaired";
            final Trigger parsedTriggerOther = TriggerHandler.parseTrigger(actualTriggerOther, card, true);
            card.addTrigger(parsedTriggerOther);
            card.setSVar("TrigBondSelf", abStringOther);
        }

        if (card.hasStartOfKeyword("Amplify")) {
            // find position of Equip keyword
            final int equipPos = card.getKeywordPosition("Amplify");
            final String[] ampString = card.getKeyword().get(equipPos).split(":");
            final String amplifyMagnitude = ampString[1];
            final String suffix = !amplifyMagnitude.equals("1") ? "s" : "";
            final String ampTypes = ampString[2];
            String[] refinedTypes = ampTypes.split(",");
            final StringBuilder types = new StringBuilder();
            for (int i = 0; i < refinedTypes.length; i++) {
                types.append("Card.").append(refinedTypes[i]).append("+YouCtrl");
                if (i + 1 != refinedTypes.length) {
                    types.append(",");
                }
            }
            // Setup ETB trigger for card with Amplify keyword
            final String actualTrigger = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | Execute$ AmplifyReveal | Static$ True | Secondary$ True | "
                    + "TriggerDescription$ As this creature enters the battlefield, put "
                    + amplifyMagnitude + " +1/+1 counter" + suffix + " on it for each "
                    + ampTypes.replace(",", " and/or ") + " card you reveal in your hand.)";
            final String abString = "AB$ Reveal | Cost$ 0 | AnyNumber$ True | RevealValid$ "
                    + types.toString() + " | RememberRevealed$ True | SubAbility$ Amplify";
            final String dbString = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ AmpMagnitude | References$ Revealed,AmpMagnitude | SubAbility$ DBCleanup";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, true);
            card.addTrigger(parsedTrigger);
            card.setSVar("AmplifyReveal", abString);
            card.setSVar("Amplify", dbString);
            card.setSVar("DBCleanup", "DB$ Cleanup | ClearRemembered$ True");
            card.setSVar("AmpMagnitude", "SVar$Revealed/Times." + amplifyMagnitude);
            card.setSVar("Revealed", "Remembered$Amount");
        }

        if (card.hasStartOfKeyword("Equip")) {
            // find position of Equip keyword
            final int equipPos = card.getKeywordPosition("Equip");
            // Check for additional params such as preferred AI targets
            final String equipString = card.getKeyword().get(equipPos).substring(5);
            final String[] equipExtras = equipString.contains("\\|") ? equipString.split("\\|", 2) : null;
            // Get cost string
            String equipCost = "";
            if (equipExtras != null) {
                equipCost = equipExtras[0].trim();
            } else {
                equipCost = equipString.trim();
            }
           // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append(" | ValidTgts$ Creature.YouCtrl | TgtPrompt$ Select target creature you control ");
            abilityStr.append("| SorcerySpeed$ True | Equip$ True | AILogic$ Pump | IsPresent$ Card.Self+nonCreature ");
            if (equipExtras != null) {
                abilityStr.append("| ").append(equipExtras[1]).append(" ");
            }
            if (equipCost.matches(".+<.+>")) { //Something other than a mana cost
                abilityStr.append("| PrecostDesc$ Equip - | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            else {
                abilityStr.append("| PrecostDesc$ Equip | SpellDescription$ (Attach to target creature you control. Equip only as a sorcery.)");
            }
            // instantiate attach ability
            final AbilityFactory af = new AbilityFactory();
            final SpellAbility sa = af.getAbility(abilityStr.toString(), card);
            card.addSpellAbility(sa);
            // add ability to instrinic strings so copies/clones create the ability also
            card.getIntrinsicAbilities().add(abilityStr.toString());
        }

        for (String kw : card.getKeyword()) {

            if (kw.startsWith("ETBReplacement")) {
                String[] splitkw = kw.split(":");
                ReplacementLayer layer = ReplacementLayer.smartValueOf(splitkw[1]);
                AbilityFactory af = new AbilityFactory();
                SpellAbility repAb = af.getAbility(card.getSVar(splitkw[2]), card);
                String desc = repAb.getDescription();
                setupETBReplacementAbility(repAb);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Description$ " + desc;
                if (splitkw.length == 4) {
                    if (splitkw[3].contains("Optional")) {
                        repeffstr += " | Optional$ True";
                    }
                }

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(layer);
                re.setOverridingAbility(repAb);

                card.addReplacementEffect(re);
            } else if (kw.startsWith("etbCounter")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String[] splitkw = parse.split(":");

                String desc = "CARDNAME enters the battlefield with " + splitkw[2] + " "
                        + CounterType.valueOf(splitkw[1]).getName() + " counters on it.";
                String extraparams = "";
                String amount = splitkw[2];
                if (splitkw.length > 3) {
                    if (!splitkw[3].equals("no Condition")) {
                        extraparams = splitkw[3];
                    }
                }
                if (splitkw.length > 4) {
                    desc = splitkw[4];
                }
                String abStr = "AB$ ChangeZone | Cost$ 0 | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard | SubAbility$ ETBCounterDBSVar";
                String dbStr = "DB$ PutCounter | Defined$ Self | CounterType$ " + splitkw[1] + " | CounterNum$ " + amount;
                try {
                    Integer.parseInt(amount);
                }
                catch (NumberFormatException ignored) {
                    dbStr += " | References$ " + amount;
                }
                card.setSVar("ETBCounterSVar", abStr);
                card.setSVar("ETBCounterDBSVar", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBCounterSVar | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            } else if (kw.equals("CARDNAME enters the battlefield tapped.")) {
                String parse = kw;
                card.removeIntrinsicKeyword(parse);

                String abStr = "AB$ Tap | Cost$ 0 | Defined$ Self | ETB$ True | SubAbility$ MoveETB";
                String dbStr = "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Battlefield"
                        + "| Defined$ ReplacedCard";

                card.setSVar("ETBTappedSVar", abStr);
                card.setSVar("MoveETB", dbStr);

                String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                        + "| ReplaceWith$ ETBTappedSVar | Description$ CARDNAME enters the battlefield tapped.";

                ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card);
                re.setLayer(ReplacementLayer.Other);

                card.addReplacementEffect(re);
            }
        }
    }

    public static void setupETBReplacementAbility(SpellAbility sa) {
        SpellAbility tailend = sa;
        while (tailend.getSubAbility() != null) {
            tailend = tailend.getSubAbility();
        }

        class ETBReplacementEffect extends SpellEffect {
            @Override
            public void resolve(SpellAbility sa) {
                forge.Singletons.getModel().getGame().getAction().moveToPlay(((Card) sa.getReplacingObject("Card")));
            }
        }

        tailend.setSubAbility(new CommonDrawback(null, sa.getSourceCard(), null, null, new ETBReplacementEffect(), new CanPlayAsDrawbackAi()));
        // ETBReplacementMove(sa.getSourceCard(), null));
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static final int hasKeyword(final Card c, final String k) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(k)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param k
     *            a {@link java.lang.String} object.
     * @param startPos
     *            a int.
     * @return a int.
     */
    static final int hasKeyword(final Card c, final String k, final int startPos) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = startPos; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(k)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * parseKeywords.
     * </p>
     * Pulling out the parsing of keywords so it can be used by the token
     * generator
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * 
     */
    public static final void parseKeywords(final Card card, final String cardName) {
        if (card.hasKeyword("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6436821515525468682L;

                @Override
                public void execute() {
                    final List<Card> lands = card.getController().getLandsInPlay();
                    lands.remove(card);
                    if (!(lands.size() <= 2)) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (CardFactoryUtil.hasKeyword(card, "CARDNAME enters the battlefield tapped unless you control a") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card,
                    "CARDNAME enters the battlefield tapped unless you control a");
            final String parse = card.getKeyword().get(n).toString();

            String splitString;
            if (parse.contains(" or a ")) {
                splitString = " or a ";
            } else {
                splitString = " or an ";
            }

            final String[] types = parse.substring(60, parse.length() - 1).split(splitString);

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 403635232455049834L;

                @Override
                public void execute() {
                    final List<Card> clICtrl = card.getOwner().getCardsIn(ZoneType.Battlefield);

                    boolean fnd = false;

                    for (int i = 0; i < clICtrl.size(); i++) {
                        final Card c = clICtrl.get(i);
                        for (final String type : types) {
                            if (c.isType(type.trim())) {
                                fnd = true;
                            }
                        }
                    }

                    if (!fnd) {
                        // it enters the battlefield this way, and should not
                        // fire triggers
                        card.setTapped(true);
                    }
                }
            });
        }
        if (CardFactoryUtil.hasKeyword(card, "Sunburst") != -1) {
            final Command sunburstCIP = new Command() {
                private static final long serialVersionUID = 1489845860231758299L;

                @Override
                public void execute() {
                    if (card.isCreature()) {
                        card.addCounter(CounterType.P1P1, card.getSunburstValue(), true);
                    } else {
                        card.addCounter(CounterType.CHARGE, card.getSunburstValue(), true);
                    }

                }
            };

            final Command sunburstLP = new Command() {
                private static final long serialVersionUID = -7564420917490677427L;

                @Override
                public void execute() {
                    card.setSunburstValue(0);
                }
            };

            card.addComesIntoPlayCommand(sunburstCIP);
            card.addLeavesPlayCommand(sunburstLP);
        }

        // Enforce the "World rule"
        if (card.isType("World")) {
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 6536398032388958127L;

                @Override
                public void execute() {
                    final List<Card> cardsInPlay = CardLists.getType(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), "World");
                    cardsInPlay.remove(card);
                    for (int i = 0; i < cardsInPlay.size(); i++) {
                        Singletons.getModel().getGame().getAction().sacrificeDestroy(cardsInPlay.get(i));
                    }
                } // execute()
            }; // Command
            card.addComesIntoPlayCommand(intoPlay);
        }

        if (CardFactoryUtil.hasKeyword(card, "Morph") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Morph");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                card.setCanMorph(true);
                Map<String, String> sVars = card.getSVars();

                final String[] k = parse.split(":");
                final Cost cost = new Cost(card, k[1], true);

                card.addSpellAbility(CardFactoryUtil.abilityMorphDown(card));

                card.turnFaceDown();

                card.addSpellAbility(CardFactoryUtil.abilityMorphUp(card, cost));
                card.setSVars(sVars); // for Warbreak Trumpeter.

                card.turnFaceUp();
            }
        } // Morph

        if (CardFactoryUtil.hasKeyword(card, "Unearth") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Unearth");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");

                final String manacost = k[1];

                card.addSpellAbility(CardFactoryUtil.abilityUnearth(card, manacost));
                card.setUnearth(true);
            }
        } // unearth

        if (CardFactoryUtil.hasKeyword(card, "Madness") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Madness");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMadnessCost(k[1]);
            }
        } // madness

        if (CardFactoryUtil.hasKeyword(card, "Miracle") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Miracle");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                card.setMiracleCost(k[1]);
            }
        } // miracle

        if (CardFactoryUtil.hasKeyword(card, "Devour") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Devour");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String magnitude = k[1];

                // final String player = card.getController();
                final int[] numCreatures = new int[1];

                final Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;

                    @Override
                    public void execute() {
                        final List<Card> creats = card.getController().getCreaturesInPlay();
                        creats.remove(card);
                        // System.out.println("Creats size: " + creats.size());

                        card.clearDevoured();
                        if (card.getController().isHuman()) {
                            if (creats.size() > 0) {
                                final List<Card> selection = GuiChoose.getOrderChoices("Devour", "Devouring", -1, creats, null, card);
                                numCreatures[0] = selection.size();

                                for (Object o : selection) {
                                    Card dinner = (Card) o;
                                    card.addDevoured(dinner);
                                    Singletons.getModel().getGame().getAction().sacrifice(dinner, null);
                                }
                            }
                        } // human
                        else {
                            int count = 0;
                            for (int i = 0; i < creats.size(); i++) {
                                final Card c = creats.get(i);
                                if ((c.getNetAttack() <= 1) && ((c.getNetAttack() + c.getNetDefense()) <= 3)) {
                                    card.addDevoured(c);
                                    Singletons.getModel().getGame().getAction().sacrifice(c, null);
                                    count++;
                                }
                            }
                            numCreatures[0] = count;
                        }
                        final int multiplier = magnitude.equals("X") ? AbilityFactory.calculateAmount(card, magnitude, null)
                                : Integer.parseInt(magnitude);
                        final int totalCounters = numCreatures[0] * multiplier;

                        card.addCounter(CounterType.P1P1, totalCounters, true);

                    }
                };
                card.addComesIntoPlayCommand(intoPlay);
            }
        } // Devour

        if (CardFactoryUtil.hasKeyword(card, "Modular") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Modular");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.getKeyword().remove(parse);

                final int m = Integer.parseInt(parse.substring(8));

                card.addIntrinsicKeyword("etbCounter:P1P1:" + m + ":no Condition:"
                        + "Modular " + m + " (This enters the battlefield with " + m + " +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");

                final SpellAbility ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        final Card card2 = this.getTargetCard();
                        card2.addCounter(CounterType.P1P1, this.getSourceCard().getCounters(CounterType.P1P1), true);
                    } // resolve()
                };

                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;

                    @Override
                    public void execute() {
                        // Target as Modular is Destroyed
                        if (card.getController().isComputer()) {
                            List<Card> choices =
                                    CardLists.filter(card.getController().getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                                @Override
                                public boolean apply(final Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if (choices.size() != 0) {
                                ability.setTargetCard(CardFactoryUtil.getBestCreatureAI(choices));

                                if (ability.getTargetCard() != null) {
                                    ability.setStackDescription("Put " + card.getCounters(CounterType.P1P1)
                                            + " +1/+1 counter/s from " + card + " on " + ability.getTargetCard());
                                    Singletons.getModel().getGame().getStack().addSimultaneousStackEntry(ability);

                                }
                            }
                        } else {
                            Singletons.getModel().getMatch().getInput().setInput(CardFactoryUtil.modularInput(ability, card));
                        }
                    }
                });
            }
        } // Modular

        /*
         * WARNING: must keep this keyword processing before etbCounter keyword
         * processing.
         */
        final int graft = CardFactoryUtil.hasKeyword(card, "Graft");
        if (graft != -1) {
            final String parse = card.getKeyword().get(graft).toString();

            final int m = Integer.parseInt(parse.substring(6));
            final String abStr = "AB$ MoveCounter | Cost$ 0 | Source$ Self | "
                    + "Defined$ TriggeredCard | CounterType$ P1P1 | CounterNum$ 1";
            card.setSVar("GraftTrig", abStr);

            String trigStr = "Mode$ ChangesZone | ValidCard$ Creature.Other | "
                + "Origin$ Any | Destination$ Battlefield"
                + " | TriggerZones$ Battlefield | OptionalDecider$ You | "
                + "IsPresent$ Card.Self+counters_GE1_P1P1 | "
                + "Execute$ GraftTrig | TriggerDescription$ "
                + "Whenever another creature enters the battlefield, you "
                + "may move a +1/+1 counter from this creature onto it.";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
            card.addTrigger(myTrigger);

            card.addIntrinsicKeyword("etbCounter:P1P1:" + m);
        }

        final int bloodthirst = CardFactoryUtil.hasKeyword(card, "Bloodthirst");
        if (bloodthirst != -1) {
            final String numCounters = card.getKeyword().get(bloodthirst).split(" ")[1];
            String desc = "Bloodthirst "
                    + numCounters + " (If an opponent was dealt damage this turn, this creature enters the battlefield with "
                    + numCounters + " +1/+1 counters on it.)";
            if (numCounters.equals("X")) {
                desc = "Bloodthirst X (This creature enters the battlefield with X +1/+1 counters on it, "
                        + "where X is the damage dealt to your opponents this turn.)";
                card.setSVar("X", "Count$BloodthirstAmount");
            }
            card.setSVar("X", "Count$BloodthirstAmount");

            card.addIntrinsicKeyword("etbCounter:P1P1:" + numCounters + ":Bloodthirst$ True:" + desc);
        } // bloodthirst

        final int storm = card.getKeywordAmount("Storm");
        for (int i = 0; i < storm; i++) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ SpellCast | ValidCard$ Card.Self | Execute$ Storm "
                            + "| TriggerDescription$ Storm (When you cast this spell, "
                            + "copy it for each spell cast before it this turn.)");

            card.setSVar("Storm", "AB$CopySpell | Cost$ 0 | Defined$ TriggeredSpellAbility | Amount$ StormCount");
            card.setSVar("StormCount", "Count$StormCount");
            final Trigger stormTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, true);

            card.addTrigger(stormTrigger);
        } // Storm
    }

} // end class CardFactoryUtil
