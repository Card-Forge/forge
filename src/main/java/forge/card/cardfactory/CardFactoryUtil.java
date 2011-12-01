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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Combat;
import forge.CombatUtil;
import forge.Command;
import forge.CommandArgs;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Counters;
import forge.GameActionUtil;
import forge.HandSizeOp;
import forge.MyRandom;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.InputPayManaCost;
import forge.gui.input.InputPayManaCostUtil;

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
     * AI_getMostExpensivePermanent.
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
    public static Card getMostExpensivePermanentAI(final CardList list, final SpellAbility spell, final boolean targeted) {
        CardList all = list;
        if (targeted) {
            all = all.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }

        return CardFactoryUtil.getMostExpensivePermanentAI(all);
    }

    /**
     * A i_get most expensive permanent.
     * 
     * @param all
     *            the all
     * @return the card
     */
    public static Card getMostExpensivePermanentAI(final CardList all) {
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
            final CardList auras = new CardList(card.getEnchantedBy().toArray());
            auras.getController(card.getController());
            curCMC += auras.getTotalConvertedManaCost() + auras.size();

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
     * AI_getCheapestCreature.
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
    public static Card getCheapestCreatureAI(CardList list, final SpellAbility spell, final boolean targeted) {
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.isCreature();
            }
        });
        return CardFactoryUtil.getCheapestPermanentAI(list, spell, targeted);
    }

    /**
     * <p>
     * AI_getCheapestPermanent.
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
    public static Card getCheapestPermanentAI(final CardList list, final SpellAbility spell, final boolean targeted) {
        CardList all = list;
        if (targeted) {
            all = all.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
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
            if (CardUtil.getConvertedManaCost(cheapest.getManaCost()) <= CardUtil.getConvertedManaCost(cheapest
                    .getManaCost())) {
                cheapest = all.get(i);
            }
        }

        return cheapest;

    }

    /**
     * <p>
     * AI_getBestLand.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestLandAI(final CardList list) {
        final CardList land = list.getType("Land");
        if (!(land.size() > 0)) {
            return null;
        }

        // prefer to target non basic lands
        final CardList nbLand = land.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return (!c.isBasicLand());
            }
        });

        if (nbLand.size() > 0) {
            // TODO - Rank non basics?

            final Random r = MyRandom.getRandom();
            return nbLand.get(r.nextInt(nbLand.size()));
        }

        // if no non-basic lands, target the least represented basic land type
        final String[] names = { "Plains", "Island", "Swamp", "Mountain", "Forest" };
        String sminBL = "";
        int iminBL = 20000; // hopefully no one will ever have more than 20000
                            // lands of one type....
        int n = 0;
        for (int i = 0; i < 5; i++) {
            n = land.getType(names[i]).size();
            if ((n < iminBL) && (n > 0)) {
                // if two or more are tied, only the
                // first
                // one checked will be used
                iminBL = n;
                sminBL = names[i];
            }
        }
        if (iminBL == 20000) {
            return null; // no basic land was a minimum
        }

        final CardList bLand = land.getType(sminBL);
        for (int i = 0; i < bLand.size(); i++) {
            if (!bLand.get(i).isTapped()) {
                // prefer untapped lands
                return bLand.get(i);
            }
        }

        final Random r = MyRandom.getRandom();
        return bLand.get(r.nextInt(bLand.size())); // random tapped land of
                                                   // least represented type
    }

    // The AI doesn't really pick the best enchantment, just the most expensive.
    /**
     * <p>
     * AI_getBestEnchantment.
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
    public static Card getBestEnchantmentAI(final CardList list, final SpellAbility spell, final boolean targeted) {
        CardList all = list;
        all = all.getType("Enchantment");
        if (targeted) {
            all = all.filter(new CardListFilter() {

                @Override
                public boolean addCard(final Card c) {
                    return c.canBeTargetedBy(spell);
                }
            });
        }
        if (all.size() == 0) {
            return null;
        }

        // get biggest Enchantment
        Card biggest = null;
        biggest = all.get(0);

        int bigCMC = 0;
        for (int i = 0; i < all.size(); i++) {
            final int curCMC = CardUtil.getConvertedManaCost(all.get(i).getManaCost());

            if (curCMC > bigCMC) {
                bigCMC = curCMC;
                biggest = all.get(i);
            }
        }

        return biggest;
    }

    // The AI doesn't really pick the best artifact, just the most expensive.
    /**
     * <p>
     * AI_getBestArtifact.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestArtifactAI(final CardList list) {
        CardList all = list;
        all = all.getType("Artifact");
        if (all.size() == 0) {
            return null;
        }

        // get biggest Artifact
        Card biggest = null;
        biggest = all.get(0);

        int bigCMC = 0;
        for (int i = 0; i < all.size(); i++) {
            final int curCMC = CardUtil.getConvertedManaCost(all.get(i).getManaCost());

            if (curCMC > bigCMC) {
                bigCMC = curCMC;
                biggest = all.get(i);
            }
        }

        return biggest;
    }

    /**
     * <p>
     * AI_doesCreatureAttack.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean doesCreatureAttackAI(final Card card) {
        final Combat combat = ComputerUtil.getAttackers();
        final Card[] att = combat.getAttackers();
        for (final Card element : att) {
            if (element.equals(card)) {
                return true;
            }
        }

        return false;
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
    public static int evaluateCreatureList(final CardList list) {
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
    public static int evaluatePermanentList(final CardList list) {
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
        int power = c.getNetAttack();
        final int toughness = c.getNetDefense();

        // Doran
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            power = toughness;
        }

        if (c.hasKeyword("Prevent all combat damage that would be dealt by CARDNAME.")
                || c.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")
                || c.hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                || c.hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
            power = 0;
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

        // Battle stats increasing keywords
        if (c.hasKeyword("Double Strike")) {
            value += 10 + (power * 15);
        }
        value += c.getKeywordMagnitude("Bushido") * 16;
        value += c.getAmountOfKeyword("Flanking") * 15;

        // Other good keywords
        if (c.hasKeyword("Deathtouch") && (power > 0)) {
            value += 25;
        }
        value += c.getAmountOfKeyword("Exalted") * 15;
        if (c.hasKeyword("First Strike") && !c.hasKeyword("Double Strike") && (power > 0)) {
            value += 10 + (power * 5);
        }
        if (c.hasKeyword("Lifelink")) {
            value += power * 10;
        }
        if (c.hasKeyword("Trample") && (power > 1)) {
            value += power * 3;
        }
        if (c.hasKeyword("Vigilance")) {
            value += (power * 5) + (toughness * 5);
        }
        if (c.hasKeyword("Wither")) {
            value += power * 10;
        }
        value += c.getKeywordMagnitude("Rampage");
        value += c.getKeywordMagnitude("Annihilator") * 50;
        if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                + "put into a graveyard, put a +1/+1 counter on CARDNAME.")
                && (power > 0)) {
            value += 2;
        }
        if (c.hasKeyword("Whenever a creature dealt damage by CARDNAME this turn is "
                + "put into a graveyard, put a +2/+2 counter on CARDNAME.")
                && (power > 0)) {
            value += 4;
        }
        if (c.hasKeyword("Whenever CARDNAME is dealt damage, put a +1/+1 counter on it.")) {
            value += 10;
        }

        // Defensive Keywords
        if (c.hasKeyword("Reach")) {
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
        }
        if (c.hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) {
            value += 50;
        }
        if (c.hasKeyword("Shroud")) {
            value += 30;
        }
        if (c.hasKeyword("Hexproof")) {
            value += 35;
        }
        if (c.hasStartOfKeyword("Protection")) {
            value += 20;
        }
        if (c.hasStartOfKeyword("PreventAllDamageBy")) {
            value += 10;
        }
        value += c.getKeywordMagnitude("Absorb") * 11;

        // Activated Abilities
        if (c.hasStartOfKeyword("ab")) {
            value += 10;
        }

        // Bad keywords
        if (c.hasKeyword("Defender") || c.hasKeyword("CARDNAME can't attack.")) {
            value -= (power * 9) + 40;
        }
        if (c.hasKeyword("CARDNAME can't block.")) {
            value -= 10;
        }
        if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
            value -= 10;
        }
        if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= toughness * 5;
        }

        if (c.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            value -= (toughness - 1) * 9;
        }

        if (c.hasKeyword("CARDNAME can't attack or block.")) {
            value = 50 + (c.getCMC() * 5); // reset everything - useless
        }
        if (c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")) {
            value -= 50;
        }
        if (c.hasKeyword("At the beginning of the end step, exile CARDNAME.")) {
            value -= 50;
        }
        if (c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
            value -= 50;
        }
        if (c.hasStartOfKeyword("At the beginning of your upkeep, CARDNAME deals")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("At the beginning of your upkeep, destroy CARDNAME unless you pay")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("Upkeep:")) {
            value -= 20;
        }
        if (c.hasStartOfKeyword("Cumulative upkeep")) {
            value -= 30;
        }
        if (c.hasStartOfKeyword("(Echo unpaid)")) {
            value -= 10;
        }
        if (c.hasStartOfKeyword("Fading")) {
            value -= 20; // not used atm
        }
        if (c.hasStartOfKeyword("Vanishing")) {
            value -= 20; // not used atm
        }

        if (c.isUntapped()) {
            value += 1;
        }

        return value;

    } // evaluateCreature

    // returns null if list.size() == 0
    /**
     * <p>
     * AI_getBestCreature.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */

    public static Card getBestAI(final CardList list) {
        // Get Best will filter by appropriate getBest list if ALL of the list
        // is of that type
        if (list.getNotType("Creature").size() == 0) {
            return CardFactoryUtil.getBestCreatureAI(list);
        }

        if (list.getNotType("Land").size() == 0) {
            return CardFactoryUtil.getBestLandAI(list);
        }

        // TODO - Once we get an EvaluatePermanent this should call
        // getBestPermanent()
        return CardFactoryUtil.getMostExpensivePermanentAI(list);
    }

    /**
     * A i_get best creature.
     * 
     * @param list
     *            the list
     * @return the card
     */
    public static Card getBestCreatureAI(final CardList list) {
        CardList all = list;
        all = all.getType("Creature");
        Card biggest = null;

        if (all.size() != 0) {
            biggest = all.get(0);

            for (int i = 0; i < all.size(); i++) {
                if (CardFactoryUtil.evaluateCreature(biggest) < CardFactoryUtil.evaluateCreature(all.get(i))) {
                    biggest = all.get(i);
                }
            }
        }
        return biggest;
    }

    // This selection rates tokens higher
    /**
     * <p>
     * AI_getBestCreatureToBounce.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final CardList list) {
        final int tokenBonus = 40;
        CardList all = list;
        all = all.getType("Creature");
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

    // returns null if list.size() == 0
    /**
     * <p>
     * AI_getWorstCreature.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getWorstCreatureAI(final CardList list) {
        CardList all = list;
        all = all.getType("Creature");
        // get smallest creature
        Card smallest = null;

        if (all.size() != 0) {
            smallest = all.get(0);

            for (int i = 0; i < all.size(); i++) {
                if (CardFactoryUtil.evaluateCreature(smallest) > CardFactoryUtil.evaluateCreature(all.get(i))) {
                    smallest = all.get(i);
                }
            }
        }
        return smallest;
    }

    /**
     * <p>
     * AI_getWorstPermanent.
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
    public static Card getWorstPermanentAI(final CardList list, final boolean biasEnch, final boolean biasLand,
            final boolean biasArt, final boolean biasCreature) {
        if (list.size() == 0) {
            return null;
        }

        if (biasEnch && (list.getType("Enchantment").size() > 0)) {
            return CardFactoryUtil.getCheapestPermanentAI(list.getType("Enchantment"), null, false);
        }

        if (biasArt && (list.getType("Artifact").size() > 0)) {
            return CardFactoryUtil.getCheapestPermanentAI(list.getType("Artifact"), null, false);
        }

        if (biasLand && (list.getType("Land").size() > 0)) {
            return CardFactoryUtil.getWorstLand(list.getType("Land"));
        }

        if (biasCreature && (list.getType("Creature").size() > 0)) {
            return CardFactoryUtil.getWorstCreatureAI(list.getType("Creature"));
        }

        if (list.getType("Land").size() > 6) {
            return CardFactoryUtil.getWorstLand(list.getType("Land"));
        }

        if ((list.getType("Artifact").size() > 0) || (list.getType("Enchantment").size() > 0)) {
            return CardFactoryUtil.getCheapestPermanentAI(list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return c.isArtifact() || c.isEnchantment();
                }
            }), null, false);
        }

        if (list.getType("Creature").size() > 0) {
            return CardFactoryUtil.getWorstCreatureAI(list.getType("Creature"));
        }

        // Planeswalkers fall through to here, lands will fall through if there
        // aren't very many
        return CardFactoryUtil.getCheapestPermanentAI(list, null, false);
    }

    /**
     * <p>
     * input_Spell.
     * </p>
     * 
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param free
     *            a boolean.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputSpell(final SpellAbility spell, final CardList choices, final boolean free) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;

            @Override
            public void showMessage() {
                if (choices.size() == 0) {
                    this.stop();
                }
                if (spell.getTargetCard() != null) {
                    this.stop();
                }
                AllZone.getDisplay().showMessage("Select target Spell: ");
                final Card choice = GuiUtils.getChoiceOptional("Choose a Spell", choices.toArray());
                if (choice != null) {
                    spell.setTargetCard(choice);
                    this.done();
                } else {
                    this.stop();
                }

            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            void done() {
                choices.clear();
                if (spell.getManaCost().equals("0") || this.isFree()) {
                    if (spell.getTargetCard() != null) {
                        AllZone.getStack().add(spell);
                    }
                    this.stop();
                } else {
                    this.stopSetNext(new InputPayManaCost(spell));
                }
            }
        };
        return target;
    } // input_targetSpell()

    /**
     * <p>
     * input_destroyNoRegeneration.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputDestroyNoRegeneration(final CardList choices, final String message) {
        final Input target = new Input() {
            private static final long serialVersionUID = -6637588517573573232L;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage(message);
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (choices.contains(card)) {
                    AllZone.getGameAction().destroyNoRegeneration(card);
                    this.stop();
                }
            }
        };
        return target;
    } // input_destroyNoRegeneration()

    /**
     * <p>
     * ability_Unearth.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityActivated abilityUnearth(final Card sourceCard, final String manaCost) {

        final Cost cost = new Cost(manaCost, sourceCard.getName(), true);
        final AbilityActivated unearth = new AbilityActivated(sourceCard, cost, null) {
            private static final long serialVersionUID = -5633945565395478009L;

            @Override
            public void resolve() {
                final Card card = AllZone.getGameAction().moveToPlay(sourceCard);

                card.addIntrinsicKeyword("At the beginning of the end step, exile CARDNAME.");
                card.addIntrinsicKeyword("Haste");
                card.setUnearthed(true);
            }

            @Override
            public boolean canPlayAI() {
                if (AllZone.getPhase().isAfter(Constant.Phase.MAIN1)
                        || AllZone.getPhase().isPlayerTurn(AllZone.getHumanPlayer())) {
                    return false;
                }
                return ComputerUtil.canPayCost(this);
            }
        };
        final SpellAbilityRestriction restrict = new SpellAbilityRestriction();
        restrict.setZone(Zone.Graveyard);
        restrict.setSorcerySpeed(true);
        unearth.setRestrictions(restrict);

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append("Unearth: ").append(sourceCard.getName());
        unearth.setStackDescription(sbStack.toString());

        return unearth;
    } // ability_Unearth()

    /**
     * <p>
     * ability_Morph_Down.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final Card sourceCard) {
        final SpellAbility morphDown = new Spell(sourceCard) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                sourceCard.turnFaceDown();

                sourceCard.comesIntoPlay();

                AllZone.getGameAction().moveToPlay(sourceCard);
            }

            @Override
            public boolean canPlay() {
                return Phase.canCastSorcery(sourceCard.getController()) && !AllZoneUtil.isCardInPlay(sourceCard);
            }

        };

        morphDown.setManaCost("3");
        morphDown.setDescription("(You may cast this face down as a 2/2 creature for 3.)");
        morphDown.setStackDescription("Morph - Creature 2/2");

        return morphDown;
    }

    /**
     * <p>
     * ability_Morph_Up.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param orgManaCost
     *            a {@link java.lang.String} object.
     * @param a
     *            a int.
     * @param d
     *            a int.
     * @return a {@link forge.card.spellability.AbilityActivated} object.
     */
    public static AbilityActivated abilityMorphUp(final Card sourceCard, final Cost cost, final String orgManaCost,
            final int a, final int d) {
        final AbilityActivated morphUp = new AbilityActivated(sourceCard, cost, null) {
            private static final long serialVersionUID = -3663857013937085953L;

            @Override
            public void resolve() {
                sourceCard.turnFaceUp();

                // Run triggers
                final Map<String, Object> runParams = new TreeMap<String, Object>();
                runParams.put("Card", sourceCard);
                AllZone.getTriggerHandler().runTrigger("TurnFaceUp", runParams);
            }

            @Override
            public boolean canPlay() {
                return sourceCard.getController().equals(this.getActivatingPlayer()) && sourceCard.isFaceDown()
                        && AllZoneUtil.isCardInPlay(sourceCard);
            }

        }; // morph_up

        // morph_up.setManaCost(cost);
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
     * ability_cycle.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param cycleCost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityCycle(final Card sourceCard, String cycleCost) {
        cycleCost += " Discard<1/CARDNAME>";
        final Cost abCost = new Cost(cycleCost, sourceCard.getName(), true);

        final SpellAbility cycle = new AbilityActivated(sourceCard, abCost, null) {
            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {

                if (AllZone.getPhase().isBefore(Constant.Phase.MAIN2)) {
                    return false;
                }

                // The AI should cycle lands if it has 6 already and no cards in
                // hand with higher CMC
                final CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                CardList lands = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                lands.addAll(hand);
                lands = lands.getType("Land");

                if (sourceCard.isLand() && (lands.size() >= Math.max(hand.getHighestConvertedManaCost(), 6))) {
                    return true;
                }

                // TODO - When else should AI Cycle?
                return false;
            }

            @Override
            public boolean canPlay() {
                if (AllZoneUtil.isCardInPlay("Stabilizer")) {
                    return false;
                }
                return super.canPlay();
            }

            @Override
            public void resolve() {
                sourceCard.getController().drawCard();
            }
        };
        cycle.setIsCycling(true);
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Cycling ").append(cycle.getManaCost()).append(" (");
        sbDesc.append(abCost.toString()).append(" Draw a card.)");
        cycle.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" Cycling: Draw a card");
        cycle.setStackDescription(sbStack.toString());

        cycle.getRestrictions().setZone(Constant.Zone.Hand);
        return cycle;
    } // ability_cycle()

    /**
     * <p>
     * ability_typecycle.
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
        String description;
        cycleCost += " Discard<1/CARDNAME>";
        final Cost abCost = new Cost(cycleCost, sourceCard.getName(), true);

        final SpellAbility cycle = new AbilityActivated(sourceCard, abCost, null) {
            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {
                return false;
            }

            // some AI code could be added (certain colored mana needs analyze
            // method maybe)

            @Override
            public boolean canPlay() {
                if (AllZoneUtil.isCardInPlay("Stabilizer")) {
                    return false;
                }
                return super.canPlay();
            }

            @Override
            public void resolve() {
                final CardList cards = sourceCard.getController().getCardsIn(Zone.Library);
                final CardList sameType = new CardList();

                for (int i = 0; i < cards.size(); i++) {
                    if (cards.get(i).isType(type)) {
                        sameType.add(cards.get(i));
                    }
                }

                if (sameType.size() == 0) {
                    sourceCard.getController().discard(sourceCard, this);
                    return;
                }

                final Object o = GuiUtils.getChoiceOptional("Select a card", sameType.toArray());
                if (o != null) {
                    // ability.setTargetCard((Card)o);

                    sourceCard.getController().discard(sourceCard, this);
                    final Card c1 = (Card) o;
                    AllZone.getGameAction().moveToHand(c1);

                }
                sourceCard.getController().shuffle();
            }
        };
        if (type.contains("Basic")) {
            description = "Basic land";
        } else {
            description = type;
        }

        cycle.setIsCycling(true);
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append(description).append("cycling (").append(abCost.toString());
        sbDesc.append(" Search your library for a ").append(description);
        sbDesc.append(" card, reveal it, and put it into your hand. Then shuffle your library.)");
        cycle.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" ").append(description);
        sbStack.append("cycling: Search your library for a ").append(description).append(" card.)");
        cycle.setStackDescription(sbStack.toString());

        cycle.getRestrictions().setZone(Constant.Zone.Hand);

        return cycle;
    } // ability_typecycle()

    /**
     * <p>
     * ability_transmute.
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
        final Cost abCost = new Cost(transmuteCost, sourceCard.getName(), true);

        final SpellAbility transmute = new AbilityActivated(sourceCard, abCost, null) {
            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean canPlay() {
                return super.canPlay() && Phase.canCastSorcery(sourceCard.getController());
            }

            @Override
            public void resolve() {
                final CardList cards = sourceCard.getController().getCardsIn(Zone.Library);
                final CardList sameCost = new CardList();

                for (int i = 0; i < cards.size(); i++) {
                    if (CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) == CardUtil
                            .getConvertedManaCost(sourceCard.getManaCost())) {
                        sameCost.add(cards.get(i));
                    }
                }

                if (sameCost.size() == 0) {
                    return;
                }

                final Object o = GuiUtils.getChoiceOptional("Select a card", sameCost.toArray());
                if (o != null) {
                    // ability.setTargetCard((Card)o);

                    sourceCard.getController().discard(sourceCard, this);
                    final Card c1 = (Card) o;

                    AllZone.getGameAction().moveToHand(c1);

                }
                sourceCard.getController().shuffle();
            }

        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Transmute (").append(abCost.toString());
        sbDesc.append("Search your library for a card with the same converted mana cost as this card, reveal it, ");
        sbDesc.append("and put it into your hand. Then shuffle your library. Transmute only as a sorcery.)");
        transmute.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard).append(" Transmute: Search your library ");
        sbStack.append("for a card with the same converted mana cost.)");
        transmute.setStackDescription(sbStack.toString());

        transmute.getRestrictions().setZone(Constant.Zone.Hand);
        return transmute;
    } // ability_transmute()

    /**
     * <p>
     * ability_suspend.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param suspendCost
     *            a {@link java.lang.String} object.
     * @param suspendCounters
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility abilitySuspend(final Card sourceCard, final String suspendCost, final int suspendCounters) {
        // be careful with Suspend ability, it will not hit the stack
        final SpellAbility suspend = new AbilityStatic(sourceCard, suspendCost) {
            @Override
            public boolean canPlay() {
                if (!(this.getRestrictions().canPlay(sourceCard, this))) {
                    return false;
                }

                if (sourceCard.isInstant()) {
                    return true;
                }

                return Phase.canCastSorcery(sourceCard.getOwner());
            }

            @Override
            public boolean canPlayAI() {
                return false;
                // Suspend currently not functional for the AI,
                // seems to be an issue with regaining Priority after Suspension
            }

            @Override
            public void resolve() {
                final Card c = AllZone.getGameAction().exile(sourceCard);
                c.addCounter(Counters.TIME, suspendCounters);
            }
        };
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Suspend ").append(suspendCounters).append(": ").append(suspendCost);
        suspend.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" suspending for ");
        sbStack.append(suspendCounters).append(" turns.)");
        suspend.setStackDescription(sbStack.toString());

        suspend.getRestrictions().setZone(Constant.Zone.Hand);
        return suspend;
    } // ability_suspend()

    /**
     * <p>
     * eqPump_Equip.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @param tough
     *            a int.
     * @param extrinsicKeywords
     *            an array of {@link java.lang.String} objects.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility eqPumpEquip(final Card sourceCard, final int power, final int tough,
            final String[] extrinsicKeywords, final Cost abCost) {
        final Target target = new Target(sourceCard, "Select target creature you control",
                "Creature.YouCtrl".split(","));
        final SpellAbility equip = new AbilityActivated(sourceCard, abCost, target) {
            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public void resolve() {
                final Card targetCard = this.getTargetCard();
                if (AllZoneUtil.isCardInPlay(targetCard) && targetCard.canBeTargetedBy(this)) {

                    if (sourceCard.isEquipping()) {
                        final Card crd = sourceCard.getEquipping().get(0);
                        if (crd.equals(targetCard)) {
                            return;
                        }

                        sourceCard.unEquipCard(crd);
                    }
                    sourceCard.equipCard(targetCard);
                }
            }

            // An animated artifact equipmemt can't equip a creature
            @Override
            public boolean canPlay() {
                return super.canPlay() && !sourceCard.isCreature() && Phase.canCastSorcery(sourceCard.getController());
            }

            @Override
            public boolean canPlayAI() {
                return (this.getCreature().size() != 0) && !sourceCard.isEquipping();
            }

            @Override
            public void chooseTargetAI() {
                final Card target = CardFactoryUtil.getBestCreatureAI(this.getCreature());
                this.setTargetCard(target);
            }

            CardList getCreature() {
                CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                list = list.getTargetableCards(this).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return c.isCreature()
                                && (CombatUtil.canAttack(c) || (CombatUtil.canAttackNextTurn(c) && AllZone.getPhase()
                                        .is(Constant.Phase.MAIN2)))
                                && (((c.getNetDefense() + tough) > 0) || sourceCard.getName().equals("Skullclamp"));
                    }
                });

                // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer
                // to target
                CardList equipMagnetList = list;
                equipMagnetList = equipMagnetList.getEquipMagnets();

                if (!equipMagnetList.isEmpty() && (tough >= 0)) {
                    return equipMagnetList;
                }

                // This equipment is keyword only
                if ((power == 0) && (tough == 0)) {
                    list = list.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            final ArrayList<String> extKeywords = new ArrayList<String>(Arrays
                                    .asList(extrinsicKeywords));
                            for (final String s : extKeywords) {

                                // We want to give a new keyword
                                if (!c.hasKeyword(s)) {
                                    return true;
                                }
                            }
                            // no new keywords:
                            return false;
                        }
                    });
                }

                return list;
            } // getCreature()
        }; // equip ability

        String costDesc = abCost.toString();
        // get rid of the ": " at the end
        costDesc = costDesc.substring(0, costDesc.length() - 2);

        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Equip");
        if (!abCost.isOnlyManaCost()) {
            sbDesc.append(" -");
        }
        sbDesc.append(" ").append(costDesc);
        if (!abCost.isOnlyManaCost()) {
            sbDesc.append(".");
        }
        equip.setDescription(sbDesc.toString());

        return equip;
    } // eqPump_Equip() ( was vanila_equip() )

    /**
     * <p>
     * eqPump_onEquip.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @param tough
     *            a int.
     * @param extrinsicKeywords
     *            an array of {@link java.lang.String} objects.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @return a {@link forge.Command} object.
     */
    public static Command eqPumpOnEquip(final Card sourceCard, final int power, final int tough,
            final String[] extrinsicKeywords, final Cost abCost) {

        final Command onEquip = new Command() {

            private static final long serialVersionUID = 8130682765214560887L;

            @Override
            public void execute() {
                if (sourceCard.isEquipping()) {
                    final Card crd = sourceCard.getEquipping().get(0);

                    for (int i = 0; i < extrinsicKeywords.length; i++) {
                        // prevent Flying, Flying
                        if (!(extrinsicKeywords[i].equals("none")) && (!crd.hasKeyword(extrinsicKeywords[i]))) {
                            crd.addExtrinsicKeyword(extrinsicKeywords[i]);
                        }
                    }

                    crd.addSemiPermanentAttackBoost(power);
                    crd.addSemiPermanentDefenseBoost(tough);
                }
            } // execute()
        }; // Command

        return onEquip;
    } // eqPump_onEquip ( was vanila_onequip() )

    /**
     * <p>
     * eqPump_unEquip.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param power
     *            a int.
     * @param tough
     *            a int.
     * @param extrinsicKeywords
     *            an array of {@link java.lang.String} objects.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @return a {@link forge.Command} object.
     */
    public static Command eqPumpUnEquip(final Card sourceCard, final int power, final int tough,
            final String[] extrinsicKeywords, final Cost abCost) {

        final Command onUnEquip = new Command() {

            private static final long serialVersionUID = 5783423127748320501L;

            @Override
            public void execute() {
                if (sourceCard.isEquipping()) {
                    final Card crd = sourceCard.getEquipping().get(0);

                    for (final String extrinsicKeyword : extrinsicKeywords) {
                        crd.removeExtrinsicKeyword(extrinsicKeyword);
                    }

                    crd.addSemiPermanentAttackBoost(-1 * power);
                    crd.addSemiPermanentDefenseBoost(-1 * tough);

                }

            } // execute()
        }; // Command

        return onUnEquip;
    } // eqPump_unEquip ( was vanila_unequip() )

    /**
     * <p>
     * getEldraziSpawnAbility.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.card.spellability.AbilityMana} object.
     */
    public static AbilityMana getEldraziSpawnAbility(final Card c) {
        final Cost cost = new Cost("Sac<1/CARDNAME>", c.getName(), true);
        final AbilityMana mana = new AbilityMana(c, cost, "1") {
            private static final long serialVersionUID = -2478676548112738019L;
        };
        mana.setDescription("Sacrifice CARDNAME: Add 1 to your mana pool.");
        return mana;
    }

    /**
     * <p>
     * entersBattleFieldWithCounters.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param type
     *            a {@link forge.Counters} object.
     * @param n
     *            a int.
     * @return a {@link forge.Command} object.
     */
    public static Command entersBattleFieldWithCounters(final Card c, final Counters type, final int n) {
        final Command addCounters = new Command() {
            private static final long serialVersionUID = 4825430555490333062L;

            @Override
            public void execute() {
                c.addCounter(type, n);
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
        final Command fade = new Command() {
            private static final long serialVersionUID = 431920157968451817L;
            private boolean firstTime = true;

            @Override
            public void execute() {

                // testAndSet - only needed when enters the battlefield.
                if (this.firstTime) {
                    sourceCard.addCounter(Counters.FADE, power);
                }
                this.firstTime = false;
            }
        };
        return fade;
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
        final Command age = new Command() {
            private static final long serialVersionUID = 431920157968451817L;
            private boolean firstTime = true;

            @Override
            public void execute() {

                // testAndSet - only needed when enters the battlefield
                if (this.firstTime) {
                    sourceCard.addCounter(Counters.TIME, power);
                }
                this.firstTime = false;
            }
        };
        return age;
    } // vanishing

    /**
     * <p>
     * ability_Soulshift.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manacost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.Command} object.
     */
    public static Command abilitySoulshift(final Card sourceCard, final String manacost) {
        final Command soulshift = new Command() {
            private static final long serialVersionUID = -4960704261761785512L;

            @Override
            public void execute() {
                AllZone.getStack().add(CardFactoryUtil.soulshiftTrigger(sourceCard, manacost));
            }

        };

        return soulshift;
    } // ability_Soulshift()

    /**
     * <p>
     * soulshiftTrigger.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param manacost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility soulshiftTrigger(final Card sourceCard, final String manacost) {
        final SpellAbility desc = new Ability(sourceCard, "0") {
            @Override
            public void resolve() {
                final CardList cards = sourceCard.getController().getCardsIn(Zone.Graveyard);
                final CardList sameCost = new CardList();
                final int cost = CardUtil.getConvertedManaCost(manacost);
                for (int i = 0; i < cards.size(); i++) {
                    if ((CardUtil.getConvertedManaCost(cards.get(i).getManaCost()) <= cost)
                            && cards.get(i).isType("Spirit")) {
                        sameCost.add(cards.get(i));
                    }
                }

                if (sameCost.size() == 0) {
                    return;
                }

                if (sourceCard.getController().isHuman()) {
                    final StringBuilder question = new StringBuilder();
                    question.append("Return target Spirit card with converted mana cost ");
                    question.append(manacost).append(" or less from your graveyard to your hand?");

                    if (GameActionUtil.showYesNoDialog(sourceCard, question.toString())) {
                        final Object o = GuiUtils.getChoiceOptional("Select a card", sameCost.toArray());
                        if (o != null) {

                            final Card c1 = (Card) o;
                            AllZone.getGameAction().moveToHand(c1);
                        }
                    }
                } else {
                    // Wiser choice should be here
                    Card choice = null;
                    sameCost.shuffle();
                    choice = sameCost.getCard(0);

                    if (!(choice == null)) {
                        AllZone.getGameAction().moveToHand(choice);
                    }
                }
            } // resolve()
        }; // SpellAbility desc

        // The spell description below fails to appear in the card detail panel
        final StringBuilder sbDesc = new StringBuilder();
        sbDesc.append("Soulshift ").append(manacost);
        sbDesc.append(" - When this permanent is put into a graveyard from play, ");
        sbDesc.append("you may return target Spirit card with converted mana cost ");
        sbDesc.append(manacost).append(" or less from your graveyard to your hand.");
        desc.setDescription(sbDesc.toString());

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - Soulshift ").append(manacost);
        desc.setStackDescription(sbStack.toString());

        return desc;
    } // soul_desc()

    // CardList choices are the only cards the user can successful select
    /**
     * <p>
     * input_targetSpecific.
     * </p>
     * 
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
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputTargetSpecific(final SpellAbility spell, final CardList choices, final String message,
            final boolean targeted, final boolean free) {
        return CardFactoryUtil.inputTargetSpecific(spell, choices, message, Command.BLANK, targeted, free);
    }

    // CardList choices are the only cards the user can successful select
    /**
     * <p>
     * input_targetSpecific.
     * </p>
     * 
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param message
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param targeted
     *            a boolean.
     * @param free
     *            a boolean.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputTargetSpecific(final SpellAbility spell, final CardList choices, final String message,
            final Command paid, final boolean targeted, final boolean free) {
        final Input target = new Input() {
            private static final long serialVersionUID = -1779224307654698954L;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (targeted && !card.canBeTargetedBy(spell)) {
                    AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection?).");
                } else if (choices.contains(card)) {
                    spell.setTargetCard(card);
                    if (spell.getManaCost().equals("0") || free) {
                        this.setFree(false);
                        AllZone.getStack().add(spell);
                        this.stop();
                    } else {
                        this.stopSetNext(new InputPayManaCost(spell));
                    }

                    paid.execute();
                }
            } // selectCard()
        };
        return target;
    } // input_targetSpecific()

    // CardList choices are the only cards the user can successful select
    /**
     * <p>
     * input_targetChampionSac.
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
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputTargetChampionSac(final Card crd, final SpellAbility spell, final CardList choices,
            final String message, final boolean targeted, final boolean free) {
        final Input target = new Input() {
            private static final long serialVersionUID = -3320425330743678663L;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                AllZone.getGameAction().sacrifice(crd);
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (choices.contains(card)) {
                    if (card == spell.getSourceCard()) {
                        AllZone.getGameAction().sacrifice(spell.getSourceCard());
                        this.stop();
                    } else {
                        spell.getSourceCard().setChampionedCard(card);
                        AllZone.getGameAction().exile(card);

                        this.stop();

                        // Run triggers
                        final HashMap<String, Object> runParams = new HashMap<String, Object>();
                        runParams.put("Card", spell.getSourceCard());
                        runParams.put("Championed", card);
                        AllZone.getTriggerHandler().runTrigger("Championed", runParams);
                    }
                }
            } // selectCard()
        };
        return target;
    } // input_targetSpecific()

    /**
     * <p>
     * input_equipCreature.
     * </p>
     * 
     * @param equip
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputEquipCreature(final SpellAbility equip) {
        final Input runtime = new Input() {
            private static final long serialVersionUID = 2029801495067540196L;

            @Override
            public void showMessage() {
                // get all creatures you control
                final CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

                this.stopSetNext(CardFactoryUtil.inputTargetSpecific(equip, list, "Select target creature to equip",
                        true, false));
            }
        }; // Input
        return runtime;
    }

    /**
     * custom input method only for use in Recall.
     * 
     * @param numCards
     *            a int.
     * @param recall
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return input
     */
    public static Input inputDiscardRecall(final int numCards, final Card recall, final SpellAbility sa) {
        final Input target = new Input() {
            private static final long serialVersionUID = 1942999595292561944L;
            private int n = 0;

            @Override
            public void showMessage() {
                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                    this.stop();
                }

                AllZone.getDisplay().showMessage("Select a card to discard");
                ButtonUtil.disableAll();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand)) {
                    card.getController().discard(card, sa);
                    this.n++;

                    // in case no more cards in hand
                    if ((this.n == numCards) || (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0)) {
                        this.done();
                    } else {
                        this.showMessage();
                    }
                }
            }

            void done() {
                AllZone.getDisplay().showMessage("Returning cards to hand.");
                AllZone.getGameAction().exile(recall);
                final CardList grave = AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard);
                for (int i = 1; i <= this.n; i++) {
                    final String title = "Return card from grave to hand";
                    final Object o = GuiUtils.getChoice(title, grave.toArray());
                    if (o == null) {
                        break;
                    }
                    final Card toHand = (Card) o;
                    grave.remove(toHand);
                    AllZone.getGameAction().moveToHand(toHand);
                }
                this.stop();
            }
        };
        return target;
    } // input_discardRecall()

    /**
     * <p>
     * MasteroftheWildHunt_input_targetCreature.
     * </p>
     * 
     * @param spell
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param choices
     *            a {@link forge.CardList} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input masterOfTheWildHuntInputTargetCreature(final SpellAbility spell, final CardList choices,
            final Command paid) {
        final Input target = new Input() {
            private static final long serialVersionUID = -1779224307654698954L;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select target wolf to damage for ").append(spell.getSourceCard());
                AllZone.getDisplay().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
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
    } // input_MasteroftheWildHunt_input_targetCreature()

    /**
     * <p>
     * modularInput.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input modularInput(final SpellAbility ability, final Card card) {
        final Input modularInput = new Input() {

            private static final long serialVersionUID = 2322926875771867901L;

            @Override
            public void showMessage() {
                AllZone.getDisplay().showMessage("Select target artifact creature");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card2, final PlayerZone zone) {
                if (card2.isCreature() && card2.isArtifact() && zone.is(Constant.Zone.Battlefield)
                        && card.canBeTargetedBy(ability)) {
                    ability.setTargetCard(card2);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Put ").append(card.getCounters(Counters.P1P1));
                    sb.append(" +1/+1 counter/s from ").append(card);
                    sb.append(" on ").append(card2);
                    ability.setStackDescription(sb.toString());
                    AllZone.getStack().add(ability);
                    this.stop();
                }
            }
        };
        return modularInput;
    }

    /**
     * <p>
     * AI_getHumanCreature.
     * </p>
     * 
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanCreatureAI(final SpellAbility spell, final boolean targeted) {
        CardList creature = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
        if (targeted) {
            creature = creature.getTargetableCards(spell);
        }
        return creature;
    }

    /**
     * <p>
     * AI_getHumanCreature.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanCreatureAI(final String keyword, final SpellAbility spell, final boolean targeted) {
        CardList creature = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        creature = creature.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (targeted) {
                    return c.isCreature() && c.hasKeyword(keyword) && c.canBeTargetedBy(spell);
                } else {
                    return c.isCreature() && c.hasKeyword(keyword);
                }
            }
        });
        return creature;
    } // AI_getHumanCreature()

    /**
     * <p>
     * AI_getHumanCreature.
     * </p>
     * 
     * @param toughness
     *            a int.
     * @param spell
     *            a {@link forge.Card} object.
     * @param targeted
     *            a boolean.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getHumanCreatureAI(final int toughness, final SpellAbility spell, final boolean targeted) {
        CardList creature = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
        creature = creature.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (targeted) {
                    return c.isCreature() && (c.getNetDefense() <= toughness) && c.canBeTargetedBy(spell);
                } else {
                    return c.isCreature() && (c.getNetDefense() <= toughness);
                }
            }
        });
        return creature;
    } // AI_getHumanCreature()

    /**
     * <p>
     * AI_targetHuman.
     * </p>
     * 
     * @return a {@link forge.CommandArgs} object.
     */
    public static CommandArgs targetHumanAI() {
        return new CommandArgs() {
            private static final long serialVersionUID = 8406907523134006697L;

            @Override
            public void execute(final Object o) {
                final SpellAbility sa = (SpellAbility) o;
                sa.setTargetPlayer(AllZone.getHumanPlayer());
            }
        };
    } // targetHuman()

    /**
     * <p>
     * getNumberOfPermanentsByColor.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getNumberOfPermanentsByColor(final String color) {
        final CardList cards = AllZoneUtil.getCardsIn(Zone.Battlefield);

        final CardList coloredPerms = new CardList();

        for (int i = 0; i < cards.size(); i++) {
            if (CardUtil.getColors(cards.get(i)).contains(color)) {
                coloredPerms.add(cards.get(i));
            }
        }
        return coloredPerms.size();
    }

    /**
     * <p>
     * multipleControlled.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean multipleControlled(final Card c) {
        final CardList list = c.getController().getCardsIn(Zone.Battlefield);
        list.remove(c);

        return list.containsName(c.getName());
    }

    /**
     * <p>
     * oppHasKismet.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean oppHasKismet(final Player player) {
        final Player opp = player.getOpponent();
        CardList list = opp.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.getName().equals("Kismet") || c.getName().equals("Frozen AEther")
                        || c.getName().equals("Loxodon Gatekeeper");
            }
        });
        return list.size() > 0;
    }

    /**
     * <p>
     * getNumberOfManaSymbolsControlledByColor.
     * </p>
     * 
     * @param colorAbb
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a int.
     */
    public static int getNumberOfManaSymbolsControlledByColor(final String colorAbb, final Player player) {
        final CardList cards = player.getCardsIn(Zone.Battlefield);
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
    public static int getNumberOfManaSymbolsByColor(final String colorAbb, final CardList cards) {
        int count = 0;
        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);
            if (!c.isToken()) {
                String manaCost = c.getManaCost();
                manaCost = manaCost.trim();
                count += CardFactoryUtil.countOccurrences(manaCost, colorAbb);
            }
        }
        return count;
    }

    /**
     * <p>
     * multiplyManaCost.
     * </p>
     * 
     * @param manacost
     *            a {@link java.lang.String} object.
     * @param multiplier
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    public static String multiplyManaCost(final String manacost, final int multiplier) {
        if (multiplier == 0) {
            return "";
        }
        if (multiplier == 1) {
            return manacost;
        }

        final String[] tokenized = manacost.split("\\s");
        final StringBuilder sb = new StringBuilder();

        if (Character.isDigit(tokenized[0].charAt(0))) {
            // manacost starts with
            // "colorless" number
            // cost
            int cost = Integer.parseInt(tokenized[0]);
            cost = multiplier * cost;
            tokenized[0] = "" + cost;
            sb.append(tokenized[0]);
        } else {
            for (int i = 0; i < multiplier; i++) {
                // tokenized[0] = tokenized[0] + " " + tokenized[0];
                sb.append((" "));
                sb.append(tokenized[0]);
            }
        }

        for (int i = 1; i < tokenized.length; i++) {
            for (int j = 0; j < multiplier; j++) {
                // tokenized[i] = tokenized[i] + " " + tokenized[i];
                sb.append((" "));
                sb.append(tokenized[i]);

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

        if (AllZone.getZoneOf(target) == null) {
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
            if (!AllZone.getZoneOf(target).is(tgt.getZone())) {
                return false;
            }
        } else {
            // If an Aura's target is removed before it resolves, the Aura
            // fizzles
            if (source.isAura() && !target.isInZone(Constant.Zone.Battlefield)) {
                return false;
            }
        }

        // Make sure it's still targetable as well
        return target.canBeTargetedBy(ability);
    }

    /**
     * <p>
     * canBeTargetedBy.
     * </p>
     * 
     * @param c
     *            the c
     * @return a boolean.
     */
    /*
     * public static boolean canBeTargetedBy(final SpellAbility ability, final
     * Card target) { return target.canBeTargetedBy(ability); }
     */

    /**
     * <p>
     * isColored.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean isColored(final Card c) {
        return c.isWhite() || c.isBlue() || c.isBlack() || c.isRed() || c.isGreen();
    }

    /**
     * <p>
     * canBeTargetedBy.
     * </p>
     * 
     * @param card
     *            the card
     * @param target
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    /*
     * public static boolean canBeTargetedBy(final Card spell, final Card
     * target) { if (target == null) { return true; }
     * 
     * if (target.isImmutable()) { return false; }
     * 
     * final PlayerZone zone = AllZone.getZoneOf(target); // if zone is null, it
     * means its on the stack if ((zone == null) ||
     * !zone.is(Constant.Zone.Battlefield)) { // targets not in play, can
     * normally be targeted return true; }
     * 
     * if (AllZoneUtil.isCardInPlay("Spellbane Centaur", target.getController())
     * && target.isCreature() && spell.isBlue()) { return false; }
     * 
     * if (target.getName().equals("Gaea's Revenge") && !spell.isGreen()) {
     * return false; }
     * 
     * if (CardFactoryUtil.hasProtectionFrom(spell, target)) { return false; }
     * 
     * if (target.getKeyword() != null) { final ArrayList<String> list =
     * target.getKeyword();
     * 
     * String kw = ""; for (int i = 0; i < list.size(); i++) { kw = list.get(i);
     * if (kw.equals("Shroud")) { return false; }
     * 
     * if (kw.equals("Hexproof")) { if
     * (!spell.getController().equals(target.getController())) { return false; }
     * }
     * 
     * if (kw.equals("CARDNAME can't be the target of Aura spells.") ||
     * kw.equals("CARDNAME can't be enchanted.")) { if (spell.isAura() &&
     * spell.isSpell()) { return false; } }
     * 
     * if (kw.equals(
     * "CARDNAME can't be the target of red spells or abilities from red sources."
     * )) { if (spell.isRed()) { return false; } }
     * 
     * if (kw.equals("CARDNAME can't be the target of black spells.")) { if
     * (spell.isBlack() && spell.isSpell()) { return false; } }
     * 
     * if (kw.equals("CARDNAME can't be the target of blue spells.")) { if
     * (spell.isBlue() && spell.isSpell()) { return false; } }
     * 
     * if (kw.equals("CARDNAME can't be the target of spells.")) { if
     * (spell.isSpell()) { return false; } } } } return true; }
     */

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
        if (c.hasKeyword("CARDNAME can't be countered.")) {
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

        return true;
    }

    // returns the number of equipments named "e" card c is equipped by
    /**
     * <p>
     * hasNumberEquipments.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param e
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int hasNumberEquipments(final Card c, final String e) {
        if (!c.isEquipped()) {
            return 0;
        }

        final String equipmentName = e;
        CardList list = new CardList(c.getEquippedBy());
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.getName().equals(equipmentName);
            }

        });

        return list.size();

    }

    /**
     * <p>
     * getGraveyardActivationCards.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList getExternalZoneActivationCards(final Player player) {

        final List<Zone> sb = new ArrayList<Constant.Zone>(3);
        sb.add(Constant.Zone.Graveyard);
        sb.add(Constant.Zone.Exile);
        sb.add(Constant.Zone.Library);
        sb.add(Constant.Zone.Command);
        CardList cl = player.getCardsIn(sb);
        cl.addAll(AllZone.getStackZone().getCards());
        cl = cl.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CardFactoryUtil.activateFromExternalZones(c, player);
            }
        });
        return cl;
    }

    /**
     * <p>
     * activateFromGrave.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public static boolean activateFromExternalZones(final Card c, final Player player) {
        final PlayerZone zone = AllZone.getZoneOf(c);
        if (zone.is(Constant.Zone.Graveyard)) {
            if (c.hasUnearth()) {
                return true;
            }

        }

        if (c.isLand() && !zone.is(Constant.Zone.Battlefield) && c.hasStartOfKeyword("May be played")) {
            return true;
        }

        for (final SpellAbility sa : c.getSpellAbility()) {
            final Zone restrictZone = sa.getRestrictions().getZone();
            if (zone.is(restrictZone)) {
                return true;
            }

            if (sa.isSpell()
                    && !zone.is(Zone.Battlefield)
                    && (c.hasStartOfKeyword("May be played") || (c.hasStartOfKeyword("Flashback") && zone
                            .is(Zone.Graveyard))) && restrictZone.equals(Zone.Hand)) {
                return true;
            }
        }

        return false;
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

    // parser for player targeted X variables
    /**
     * <p>
     * playerXCount.
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
            CardList cardsonbattlefield = AllZoneUtil.getCardsIn(Zone.Battlefield);
            cardsonbattlefield = cardsonbattlefield.getValidCards(rest, players.get(0), source);

            n = cardsonbattlefield.size();

            return CardFactoryUtil.doXMath(n, m, source);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("CardsInHand")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Hand).size(), m, source);
            }
        }

        if (sq[0].contains("DomainPlayer")) {
            final CardList someCards = new CardList();
            someCards.addAll(players.get(0).getCardsIn(Zone.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!someCards.getType(basic[i]).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, source);
        }

        if (sq[0].contains("CardsInLibrary")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Library).size(), m, source);
            }
        }

        if (sq[0].contains("CardsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Graveyard).size(), m, source);
            }
        }
        if (sq[0].contains("LandsInGraveyard")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Graveyard).getType("Land").size(), m,
                        source);
            }
        }

        if (sq[0].contains("CreaturesInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(AllZoneUtil.getCreaturesInPlay(players.get(0)).size(), m, source);
            }
        }

        if (sq[0].contains("CardsInPlay")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Battlefield).size(), m, source);
            }
        }

        if (sq[0].contains("LifeTotal")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getLife(), m, source);
            }
        }

        if (sq[0].contains("TopOfLibraryCMC")) {
            if (players.size() > 0) {
                return CardFactoryUtil.doXMath(players.get(0).getCardsIn(Zone.Library, 1).getTotalConvertedManaCost(),
                        m, source);
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

    // parser for non-mana X variables
    /**
     * <p>
     * xCount.
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

        final String[] l = s.split("/");
        final String[] m = CardFactoryUtil.parseMath(l);

        // count total number of aura enchanting card that aura is attached to
        if (l[0].contains("AllAurasEnchanting")) {
            int numAuras = 0;
            final Card aura = c.getEnchantingCard();
            if (aura != null) {
                numAuras = aura.getEnchantedBy().size();
            }
            return CardFactoryUtil.doXMath(numAuras, m, c);
        }

        // accept straight numbers
        if (l[0].contains("Number$")) {
            final String number = l[0].replace("Number$", "");
            if (number.equals("ChosenNumber")) {
                return CardFactoryUtil.doXMath(c.getChosenNumber(), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(number), m, c);
            }
        }

        // Manapool
        if (l[0].contains("ManaPool")) {
            final String color = l[0].split(":")[1];
            return AllZone.getHumanPlayer().getManaPool().getAmountOfColor(color);
        }

        // count valid cards on the battlefield
        if (l[0].contains("Valid")) {
            String restrictions = l[0].replace("Valid ", "");
            restrictions = restrictions.replace("Count$", "");
            final String[] rest = restrictions.split(",");
            CardList cardsonbattlefield = AllZoneUtil.getCardsIn(Zone.Battlefield);
            cardsonbattlefield = cardsonbattlefield.getValidCards(rest, cardController, c);

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

        if (l[0].contains("GreatestPowerYouControl")) {
            final CardList list = AllZoneUtil.getCreaturesInPlay(c.getController());
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetAttack() > highest) {
                    highest = crd.getNetAttack();
                }
            }
            return highest;
        }

        if (l[0].contains("RememberedSumPower")) {
            final CardList list = new CardList();
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    list.add(AllZoneUtil.getCardState((Card) o));
                }
            }
            return list.getTotalCreaturePower();
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid")) {
            return CardFactoryUtil.doXMath(c.getXManaCostPaid(), m, c);
        }

        if (sq[0].contains("xLifePaid")) {
            if (c.getController().isHuman()) {
                return c.getXLifePaid();
            } else {
                // copied for xPaid
                // not implemented for Compy
                // int dam = ComputerUtil.getAvailableMana().size()-
                // CardUtil.getConvertedManaCost(c);
                // if (dam < 0) dam = 0;
                // return dam;
                return 0;
            }
        }

        if (sq[0].equals("StormCount")) {
            return CardFactoryUtil.doXMath(AllZone.getStack().getCardsCastThisTurn().size() - 1, m, c);
        }

        if (sq[0].equals("DamageDoneThisTurn")) {
            return CardFactoryUtil.doXMath(c.getDamageDoneThisTurn(), m, c);
        }

        if (sq[0].contains("RegeneratedThisTurn")) {
            return CardFactoryUtil.doXMath(c.getRegeneratedThisTurn(), m, c);
        }

        CardList someCards = new CardList();

        // Complex counting methods

        // TriggeringObjects
        if (sq[0].startsWith("Triggered")) {
            return CardFactoryUtil.doXMath((Integer) c.getTriggeringObject(sq[0].substring(9)), m, c);
        }

        // Count$Domain
        if (sq[0].contains("Domain")) {
            someCards.addAll(cardController.getCardsIn(Zone.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!someCards.getType(basic[i]).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$OpponentDom
        if (sq[0].contains("OpponentDom")) {
            someCards.addAll(cardController.getOpponent().getCardsIn(Zone.Battlefield));
            final String[] basic = { "Forest", "Plains", "Mountain", "Island", "Swamp" };

            for (int i = 0; i < basic.length; i++) {
                if (!someCards.getType(basic[i]).isEmpty()) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$ColoredCreatures *a DOMAIN for creatures*
        if (sq[0].contains("ColoredCreatures")) {
            someCards.addAll(cardController.getCardsIn(Zone.Battlefield));
            someCards = someCards.filter(CardListFilter.CREATURES);

            final String[] colors = { "green", "white", "red", "blue", "black" };

            for (final String color : colors) {
                if (someCards.getColor(color).size() > 0) {
                    n++;
                }
            }
            return CardFactoryUtil.doXMath(n, m, c);
        }

        // Count$YourLifeTotal
        if (sq[0].contains("YourLifeTotal")) {
            if (cardController.isComputer()) {
                return CardFactoryUtil.doXMath(AllZone.getComputerPlayer().getLife(), m, c);
            } else if (cardController.isHuman()) {
                return CardFactoryUtil.doXMath(AllZone.getHumanPlayer().getLife(), m, c);
            }

            return 0;
        }

        // Count$OppLifeTotal
        if (sq[0].contains("OppLifeTotal")) {
            if (oppController.isComputer()) {
                return CardFactoryUtil.doXMath(AllZone.getComputerPlayer().getLife(), m, c);
            } else if (oppController.isHuman()) {
                return CardFactoryUtil.doXMath(AllZone.getHumanPlayer().getLife(), m, c);
            }

            return 0;
        }

        // Count$YourPoisonCounters
        if (sq[0].contains("YourPoisonCounters")) {
            if (cardController.isComputer()) {
                return CardFactoryUtil.doXMath(AllZone.getComputerPlayer().getPoisonCounters(), m, c);
            } else if (cardController.isHuman()) {
                return CardFactoryUtil.doXMath(AllZone.getHumanPlayer().getPoisonCounters(), m, c);
            }

            return 0;
        }

        // Count$OppPoisonCounters
        if (sq[0].contains("OppPoisonCounters")) {
            if (oppController.isComputer()) {
                return CardFactoryUtil.doXMath(AllZone.getComputerPlayer().getPoisonCounters(), m, c);
            } else if (oppController.isHuman()) {
                return CardFactoryUtil.doXMath(AllZone.getHumanPlayer().getPoisonCounters(), m, c);
            }

            return 0;
        }

        // Count$OppDamageThisTurn
        if (sq[0].contains("OppDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getOpponent().getAssignedDamage(), m, c);
        }

        // Count$YourDamageThisTurn
        if (sq[0].contains("YourDamageThisTurn")) {
            return CardFactoryUtil.doXMath(c.getController().getAssignedDamage(), m, c);
        }

        // Count$HighestLifeTotal
        if (sq[0].contains("HighestLifeTotal")) {
            return CardFactoryUtil.doXMath(
                    Math.max(AllZone.getHumanPlayer().getLife(), AllZone.getComputerPlayer().getLife()), m, c);
        }

        // Count$LowestLifeTotal
        if (sq[0].contains("LowestLifeTotal")) {
            return CardFactoryUtil.doXMath(
                    Math.min(AllZone.getHumanPlayer().getLife(), AllZone.getComputerPlayer().getLife()), m, c);
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            final CardList topcard = cardController.getCardsIn(Zone.Library, 1);
            return CardFactoryUtil.doXMath(topcard.getTotalConvertedManaCost(), m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            CardList enchantedControllerInPlay = c.getEnchantingCard().getController().getCardsIn(Zone.Battlefield);
            enchantedControllerInPlay = enchantedControllerInPlay.getType("Creature");
            return enchantedControllerInPlay.size();
        }

        // Count$LowestLibrary
        if (sq[0].contains("LowestLibrary")) {
            return Math.min(AllZone.getHumanPlayer().getZone(Zone.Library).size(),
                    AllZone.getComputerPlayer().getZone(Zone.Library).size());
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

        if (sq[0].contains("Threshold")) {
            if (cardController.hasThreshold()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Have
                                                                               // Threshold
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not
                                                                               // Threshold
            }
        }

        if (sq[0].contains("Landfall")) {
            if (cardController.hasLandfall()) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c); // Have
                                                                               // Landfall
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not
                                                                               // Landfall
            }
        }

        if (sq[0].contains("GraveyardWithGE20Cards")) {
            if (Math.max(AllZone.getHumanPlayer().getZone(Zone.Graveyard).size(),
                    AllZone.getComputerPlayer().getZone(Zone.Graveyard).size()) >= 20) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c);
            }
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            final Card csource = c;
            CardList cl = c.getDevoured();

            cl = cl.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card cdev) {
                    return cdev.isValid(validDevoured.split(","), csource.getController(), csource);
                }
            });

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
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            return CardFactoryUtil.doXMath(CardUtil.getConvertedManaCost(c), m, c);
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
            return CardFactoryUtil.doXMath(c.getCounters(Counters.getType(sq[1])), m, c);
        }
        // Count$TimesKicked
        if (sq[0].contains("TimesKicked")) {
            return CardFactoryUtil.doXMath(c.getMultiKickerMagnitude(), m, c);
        }
        if (sq[0].contains("NumCounters")) {
            final int num = c.getCounters(Counters.getType(sq[1]));
            return CardFactoryUtil.doXMath(num, m, c);
        }
        if (sq[0].contains("NumBlockingMe")) {
            return CardFactoryUtil.doXMath(AllZone.getCombat().getBlockers(c).size(), m, c);
        }

        // Count$IfMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfMainPhase")) {
            final String cPhase = AllZone.getPhase().getPhase();
            if ((cPhase.equals(Constant.Phase.MAIN1) || cPhase.equals(Constant.Phase.MAIN2))
                    && AllZone.getPhase().getPlayerTurn().equals(cardController)) {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[1]), m, c);
            } else {
                return CardFactoryUtil.doXMath(Integer.parseInt(sq[2]), m, c); // not
                                                                               // Main
                                                                               // Phase
            }
        }

        // Count$M12Empires.<numIf>.<numIfNot>
        if (sq[0].contains("AllM12Empires")) {
            boolean has = AllZoneUtil.isCardInPlay("Crown of Empires", c.getController());
            has &= AllZoneUtil.isCardInPlay("Scepter of Empires", c.getController());
            has &= AllZoneUtil.isCardInPlay("Throne of Empires", c.getController());
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
            Zone destination, origin;
            String validFilter;

            destination = Zone.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = Zone.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }

            final CardList res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);

            return CardFactoryUtil.doXMath(res.size(), m, c);
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        if (sq[0].contains("ThisTurnCast") || sq[0].contains("LastTurnCast")) {

            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];

            CardList res;

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
            final CardList res = CardUtil.getThisTurnEntered(Zone.Graveyard, Zone.Battlefield, "Creature", c);
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
                someCards.addAll(cardController.getCardsIn(Zone.Battlefield));
                mf = true;
            }
        }

        if (sq[0].contains("InYourYard")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(Zone.Graveyard));
                my = true;
            }
        }

        if (sq[0].contains("InYourLibrary")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(Zone.Library));
                my = true;
            }
        }

        if (sq[0].contains("InYourHand")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(Zone.Hand));
                mh = true;
            }
        }

        if (sq[0].contains("OppCtrl")) {
            if (!of) {
                someCards.addAll(oppController.getCardsIn(Zone.Battlefield));
                of = true;
            }
        }

        if (sq[0].contains("InOppYard")) {
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(Zone.Graveyard));
                oy = true;
            }
        }

        if (sq[0].contains("InOppHand")) {
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(Zone.Hand));
                oh = true;
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            if (!mf) {
                someCards.addAll(cardController.getCardsIn(Zone.Battlefield));
            }
            if (!of) {
                someCards.addAll(oppController.getCardsIn(Zone.Battlefield));
            }
        }

        if (sq[0].contains("InAllYards")) {
            if (!my) {
                someCards.addAll(cardController.getCardsIn(Zone.Graveyard));
            }
            if (!oy) {
                someCards.addAll(oppController.getCardsIn(Zone.Graveyard));
            }
        }

        if (sq[0].contains("InAllHands")) {
            if (!mh) {
                someCards.addAll(cardController.getCardsIn(Zone.Hand));
            }
            if (!oh) {
                someCards.addAll(oppController.getCardsIn(Zone.Hand));
            }
        }

        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = someCards.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    if (c.isType(sq[1])) {
                        return true;
                    }

                    return false;
                }
            });
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }

            someCards = someCards.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    if (c.getName().equals(sq[1])) {
                        return true;
                    }

                    return false;
                }
            });
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        if (sq[0].contains("Untapped")) {
            someCards = someCards.filter(CardListFilter.UNTAPPED);
        }

        if (sq[0].contains("Tapped")) {
            someCards = someCards.filter(CardListFilter.TAPPED);
        }

        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        if (sq[0].contains("White")) {
            someCards = someCards.filter(CardListFilter.WHITE);
        }

        if (sq[0].contains("Blue")) {
            someCards = someCards.filter(CardListFilter.BLUE);
        }

        if (sq[0].contains("Black")) {
            someCards = someCards.filter(CardListFilter.BLACK);
        }

        if (sq[0].contains("Red")) {
            someCards = someCards.filter(CardListFilter.RED);
        }

        if (sq[0].contains("Green")) {
            someCards = someCards.filter(CardListFilter.GREEN);
        }

        if (sq[0].contains("Multicolor")) {
            someCards = someCards.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return (CardUtil.getColors(c).size() > 1);
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = someCards.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
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
                cmc = CardUtil.getConvertedManaCost(someCards.getCard(i).getManaCost());
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
    public static int handlePaid(final CardList paidList, final String string, final Card source) {
        if (paidList == null) {
            return 0;
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
            final CardList list = paidList.getValidCards(valid, source.getController(), source);
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
     * input_UntapUpToNType.
     * </p>
     * 
     * @param n
     *            a int.
     * @param type
     *            a {@link java.lang.String} object.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input inputUntapUpToNType(final int n, final String type) {
        final Input untap = new Input() {
            private static final long serialVersionUID = -2167059918040912025L;

            private final int stop = n;
            private int count = 0;

            @Override
            public void showMessage() {
                final StringBuilder sb = new StringBuilder();
                sb.append("Select a ").append(type).append(" to untap");
                AllZone.getDisplay().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (card.isType(type) && zone.is(Constant.Zone.Battlefield)) {
                    card.untap();
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
     * getMostProminentCreatureType.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentCreatureType(final CardList list) {

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
     * getMostProminentColor.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentColor(final CardList list) {

        final Map<String, Integer> map = new HashMap<String, Integer>();

        for (final Card c : list) {
            final ArrayList<String> colorList = CardUtil.getColors(c);

            for (final String color : colorList) {
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

    // total cost to pay for an attacker c, cards like Propaganda, Ghostly
    // Prison, Collective Restraint, ...
    /**
     * <p>
     * getPropagandaCost.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getPropagandaCost(final Card c) {
        int cost = 0;

        final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (final Card card : list) {
            if (card.hasStartOfKeyword("Creatures can't attack unless their controller pays")) {
                final int keywordPosition = card
                        .getKeywordPosition("Creatures can't attack unless their controller pays");
                final String parse = card.getKeyword().get(keywordPosition).toString();
                final String[] k = parse.split(":");

                final String[] restrictions = k[1].split(",");
                if (!c.isValid(restrictions, card.getController(), card)) {
                    continue;
                }

                final String costString = k[2];
                if (costString.equals("X")) {
                    cost += CardFactoryUtil.xCount(card, card.getSVar("X"));
                } else if (costString.equals("Y")) {
                    cost += CardFactoryUtil.xCount(card, card.getSVar("Y"));
                } else {
                    cost += Integer.parseInt(k[2]);
                }
            }
        }

        final String s = Integer.toString(cost);

        return s;
    }

    /**
     * <p>
     * getUsableManaSources.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a int.
     */
    public static int getUsableManaSources(final Player player) {
        CardList list = player.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                for (final AbilityMana am : c.getAIPlayableMana()) {
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
     * makeTokenSaproling.
     * </p>
     * 
     * @param controller
     *            a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList makeTokenSaproling(final Player controller) {
        return CardFactoryUtil.makeToken("Saproling", "G 1 1 Saproling", controller, "G", new String[] { "Creature",
                "Saproling" }, 1, 1, new String[] { "" });
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
     *            a {@link forge.Player} object.
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
    public static CardList makeToken(final String name, final String imageName, final Player controller,
            final String manaCost, final String[] types, final int baseAttack, final int baseDefense,
            final String[] intrinsicKeywords) {
        final CardList list = new CardList();
        final Card c = new Card();
        c.setName(name);
        c.setImageName(imageName);

        // c.setController(controller);
        // c.setOwner(controller);

        // TODO - most tokens mana cost is 0, this needs to be fixed
        // c.setManaCost(manaCost);
        c.addColor(manaCost);
        c.setToken(true);

        for (final String t : types) {
            c.addType(t);
        }

        c.setBaseAttack(baseAttack);
        c.setBaseDefense(baseDefense);

        final int multiplier = AllZoneUtil.getTokenDoublersMagnitude(controller);
        // TODO - does this need to set
        // PlayerZone_ComesIntoPlay.SimultaneousEntry like Rite of Replication
        // does?
        for (int i = 0; i < multiplier; i++) {
            Card temp = CardFactoryUtil.copyStats(c);
            
            for (final String kw : intrinsicKeywords) {
                if (kw.startsWith("HIDDEN")) {
                    temp.addExtrinsicKeyword(kw);//extrinsic keywords won't survive the copyStats treatment 
                } else {
                    temp.addIntrinsicKeyword(kw);
                }
            }
            temp.setOwner(controller);
            temp.setToken(true);
            CardFactoryUtil.parseKeywords(temp, temp.getName());
            temp = CardFactoryUtil.postFactoryKeywords(temp);
            AllZone.getGameAction().moveToPlay(temp);
            list.add(temp);
        }
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
    public static CardList copyTokens(final CardList tokenList) {
        final CardList list = new CardList();

        for (int tokenAdd = 0; tokenAdd < tokenList.size(); tokenAdd++) {
            final Card thisToken = tokenList.getCard(tokenAdd);

            final ArrayList<String> tal = thisToken.getType();
            final String[] tokenTypes = new String[tal.size()];
            tal.toArray(tokenTypes);

            final ArrayList<String> kal = thisToken.getIntrinsicKeyword();
            final String[] tokenKeywords = new String[kal.size()];
            kal.toArray(tokenKeywords);
            final CardList tokens = CardFactoryUtil.makeToken(thisToken.getName(), thisToken.getImageName(),
                    thisToken.getController(), thisToken.getManaCost(), tokenTypes, thisToken.getBaseAttack(),
                    thisToken.getBaseDefense(), tokenKeywords);

            for (final Card token : tokens) {
                token.setColor(thisToken.getColor());
            }

            list.addAll(tokens);
        }

        return list;
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
                                if (AllZoneUtil.isCardInPlay(crd)) {
                                    crd.addTempAttackBoost(-1 * magnitude);
                                    crd.addTempDefenseBoost(-1 * magnitude);
                                }
                            }
                        };

                        AllZone.getEndOfTurn().addUntil(untilEOT);

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

        final Card c = ability.getSourceCard();

        if ((target != null) && c.getText().contains("deals X damage to target") && !c.getName().equals("Death Grasp")) {
            neededDamage = target.getNetDefense() - target.getDamage();
        }

        return neededDamage;
    }

    /**
     * getWorstLand(String)
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
        final CardList lands = AllZoneUtil.getPlayerLandsInPlay(player);
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
    public static Card getWorstLand(final CardList lands) {
        Card worstLand = null;
        // first, check for tapped, basic lands
        for (int i = 0; i < lands.size(); i++) {
            final Card tmp = lands.get(i);
            if (tmp.isTapped() && tmp.isBasicLand()) {
                worstLand = tmp;
            }
        }
        // next, check for tapped, non-basic lands
        if (worstLand == null) {
            for (int i = 0; i < lands.size(); i++) {
                final Card tmp = lands.get(i);
                if (tmp.isTapped()) {
                    worstLand = tmp;
                }
            }
        }
        // next, untapped, basic lands
        if (worstLand == null) {
            for (int i = 0; i < lands.size(); i++) {
                final Card tmp = lands.get(i);
                if (tmp.isUntapped() && tmp.isBasicLand()) {
                    worstLand = tmp;
                }
            }
        }
        // next, untapped, non-basic lands
        if (worstLand == null) {
            for (int i = 0; i < lands.size(); i++) {
                final Card tmp = lands.get(i);
                if (tmp.isUntapped()) {
                    worstLand = tmp;
                }
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
    public static Card getRandomCard(final CardList list) {
        if (list.size() == 0) {
            return null;
        }

        final int index = CardFactoryUtil.random.nextInt(list.size());
        return list.get(index);
    }

    /**
     * <p>
     * revertManland.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param removeTypes
     *            an array of {@link java.lang.String} objects.
     * @param removeKeywords
     *            an array of {@link java.lang.String} objects.
     * @param cost
     *            a {@link java.lang.String} object.
     * @param timeStamp
     *            a long.
     */
    public static void revertManland(final Card c, final String[] removeTypes, final String[] removeKeywords,
            final String cost, final long timeStamp) {
        c.setBaseAttack(0);
        c.setBaseDefense(0);
        for (final String r : removeTypes) {
            c.removeType(r);
        }

        for (final String k : removeKeywords) {
            c.removeIntrinsicKeyword(k);
        }

        // c.setManaCost(cost);
        c.removeColor(cost, c, false, timeStamp);
        c.unEquipAllCards();
    }

    /**
     * <p>
     * activateManland.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param attack
     *            a int.
     * @param defense
     *            a int.
     * @param addTypes
     *            an array of {@link java.lang.String} objects.
     * @param addKeywords
     *            an array of {@link java.lang.String} objects.
     * @param cost
     *            a {@link java.lang.String} object.
     * @return a long.
     */
    public static long activateManland(final Card c, final int attack, final int defense, final String[] addTypes,
            final String[] addKeywords, String cost) {
        c.setBaseAttack(attack);
        c.setBaseDefense(defense);

        for (final String r : addTypes) {
            // if the card doesn't have that type, add it
            if (!c.isType(r)) {
                c.addType(r);
            }
        }
        for (final String k : addKeywords) {
            // if the card doesn't have that keyword, add it (careful about
            // stackable keywords)
            if (!c.getIntrinsicKeyword().contains(k)) {
                c.addIntrinsicKeyword(k);
            }
        }

        // c.setManaCost(cost);
        if (cost.equals("")) {
            cost = "0";
        }

        final long timestamp = c.addColor(cost, c, false, true);
        return timestamp;
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
            final CardList fastbonds = player.getCardsIn(Zone.Battlefield, "Fastbond");
            for (final Card f : fastbonds) {
                final SpellAbility ability = new Ability(f, "0") {
                    @Override
                    public void resolve() {
                        f.getController().addDamage(1, f);
                    }
                };
                ability.setStackDescription("Fastbond - Deals 1 damage to you.");

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * isNegativeCounter.
     * </p>
     * 
     * @param c
     *            a {@link forge.Counters} object.
     * @return a boolean.
     */
    public static boolean isNegativeCounter(final Counters c) {
        return (c == Counters.AGE) || (c == Counters.BLAZE) || (c == Counters.BRIBERY) || (c == Counters.DOOM)
                || (c == Counters.ICE) || (c == Counters.M1M1) || (c == Counters.M0M2) || (c == Counters.M0M1)
                || (c == Counters.TIME);
    }

    /**
     * <p>
     * checkEmblemKeyword.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    public static String checkEmblemKeyword(final Card c) {
        if (c.hasKeyword("Artifacts, creatures, enchantments, and lands you control are indestructible.")) {
            return "Elspeth_Emblem";
        }

        if (c.hasKeyword("Mountains you control have 'tap: This land deals 1 damage to target creature or player.'")) {
            return "Koth_Emblem";
        }

        return "";
    }

    /*
     * //whenever CARDNAME becomes the target of a spell or ability, ... :
     * public static void checkTargetingEffects(SpellAbility sa, final Card c) {
     * 
     * //if (AllZoneUtil.isCardInPlay(c)) //{ if (c.hasKeyword(
     * "When CARDNAME becomes the target of a spell or ability, return CARDNAME to its owner's hand."
     * ) ) { // || (c.isCreature() && AllZoneUtil.isCardInPlay("Cowardice"))
     * SpellAbility ability = new Ability(c, "0") { public void resolve() {
     * AllZone.getGameAction().moveToHand(c); } }; StringBuilder sb = new
     * StringBuilder();
     * sb.append(c).append(" - return CARDNAME to its owner's hand.");
     * ability.setStackDescription(sb.toString());
     * 
     * AllZone.getStack().add(ability); } if (c.hasKeyword(
     * "When CARDNAME becomes the target of a spell or ability, destroy CARDNAME."
     * ) || AllZoneUtil.isCardInPlay("Horobi, Death's Wail")) {
     * 
     * SpellAbility ability = new Ability(c, "0") { public void resolve() {
     * AllZone.getGameAction().destroy(c); } }; StringBuilder sb = new
     * StringBuilder(); sb.append(c).append(" - destroy CARDNAME.");
     * ability.setStackDescription(sb.toString());
     * 
     * AllZone.getStack().add(ability); } if (c.hasKeyword(
     * "When CARDNAME becomes the target of a spell or ability, sacrifice it."))
     * { SpellAbility ability = new Ability(c, "0") { public void resolve() {
     * AllZone.getGameAction().sacrifice(c); } }; StringBuilder sb = new
     * StringBuilder(); sb.append(c).append(" - sacrifice CARDNAME.");
     * ability.setStackDescription(sb.toString());
     * 
     * AllZone.getStack().add(ability); }
     * 
     * //When enchanted creature becomes the target of a spell or ability,
     * <destroy/exile/sacrifice> <that creature/CARDNAME>. (It can't be
     * regenerated.) ArrayList<Card> auras = c.getEnchantedBy(); for(int
     * a=0;a<auras.size();a++) { final Card aura = auras.get(a);
     * ArrayList<String> keywords = aura.getKeyword(); for(int
     * i=0;i<keywords.size();i++) { final String keyword = keywords.get(i);
     * if(keyword.startsWith(
     * "When enchanted creature becomes the target of a spell or ability, ")) {
     * final String action[] = new String[1]; action[0] = keyword.substring(66);
     * String stackDesc = action[0]; stackDesc = stackDesc.replace("that",
     * "enchanted"); stackDesc =
     * stackDesc.substring(0,1).toUpperCase().concat(stackDesc.substring(1));
     * stackDesc =
     * aura.getName().concat(" (").concat(Integer.toString(aura.getUniqueNumber
     * ())).concat(") - ").concat(stackDesc);
     * 
     * Ability saTrigger = new Ability(aura,"0") { public void resolve() { Card
     * target = null; boolean noRegen = false;
     * if(action[0].endsWith(" It can't be regenerated.")) { noRegen = true;
     * action[0] = action[0].substring(0,action[0].length()-25); }
     * 
     * if(action[0].endsWith("CARDNAME.")) { target = aura; } else
     * if(action[0].endsWith("that creature.")) { target = c; } else { throw new
     * IllegalArgumentException("There is a problem in the keyword " + keyword +
     * "for card \"" + c.getName() + "\""); }
     * 
     * if(action[0].startsWith("exile")) {
     * AllZone.getGameAction().exile(target); } else
     * if(action[0].startsWith("destroy")) { if(noRegen) {
     * AllZone.getGameAction().destroyNoRegeneration(target); } else {
     * AllZone.getGameAction().destroy(target); } } else
     * if(action[0].startsWith("sacrifice")) {
     * AllZone.getGameAction().sacrifice(target); } else { throw new
     * IllegalArgumentException("There is a problem in the keyword " + keyword +
     * "for card \"" + c.getName() + "\""); } } };
     * 
     * saTrigger.setStackDescription(stackDesc);
     * 
     * AllZone.getStack().add(saTrigger); } } } //} }
     */

    // copies stats like attack, defense, etc..
    /**
     * <p>
     * copyStats.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @return a {@link forge.Card} object.
     */
    public static Card copyStats(final Object o) {
        final Card sim = (Card) o;
        final Card c = new Card();

        c.setFlip(sim.isFlip());
        c.setDoubleFaced(sim.isDoubleFaced());
        c.setCurSetCode(sim.getCurSetCode());

        final String origState = sim.getCurState();
        for (final String state : sim.getStates()) {
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
        to.setSVars(from.getSVars());
        to.setSets(from.getSets());
        to.setIntrinsicAbilities(from.getIntrinsicAbilities());

        to.setImageName(from.getImageName());
        to.setImageFilename(from.getImageFilename());
        to.setTriggers(from.getTriggers());
        to.setStaticAbilityStrings(from.getStaticAbilityStrings());

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

                card.addSpellAbility(sa);

                final String bbCost = card.getSVar("Buyback");
                if (!bbCost.equals("")) {
                    final SpellAbility bbSA = sa.copy();
                    final String newCost = CardUtil.addManaCosts(card.getManaCost(), bbCost);
                    if (bbSA.getPayCosts() != null) {
                        bbSA.setPayCosts(new Cost(newCost, sa.getSourceCard().getName(), false)); // create
                                                                                                  // new
                                                                                                  // abCost
                    }
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
                    sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
                    bbSA.setDescription(sb.toString());
                    bbSA.setIsBuyBackAbility(true);

                    card.addSpellAbility(bbSA);
                }
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
     * @return a {@link forge.Card} object.
     */
    public static Card postFactoryKeywords(final Card card) {
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
        final int kicker = CardFactoryUtil.hasKeyword(card, "Kicker");
        if (kicker != -1) {
            final SpellAbility kickedSpell = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;

                @Override
                public void resolve() {
                    card.setKicked(true);
                    AllZone.getGameAction().moveToPlay(card);
                }
            };
            final String parse = card.getKeyword().get(kicker).toString();
            card.removeIntrinsicKeyword(parse);

            final String[] k = parse.split(":");
            final String kickerCost = k[1];

            final ManaCost mc = new ManaCost(card.getManaCost());
            mc.combineManaCost(kickerCost);

            kickedSpell.setKickerAbility(true);
            kickedSpell.setManaCost(mc.toString());
            kickedSpell.setAdditionalManaCost(kickerCost);

            final StringBuilder desc = new StringBuilder();
            desc.append("Kicker ").append(kickerCost).append(" (You may pay an additional ");
            desc.append(kickerCost).append(" as you cast this spell.)");

            kickedSpell.setDescription(desc.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Kicked)");
            kickedSpell.setStackDescription(sb.toString());

            card.addSpellAbility(kickedSpell);
        }

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
                    AllZone.getGameAction().moveToPlay(card);
                }

                @Override
                public boolean canPlayAI() {
                    if (!SpellPermanent.checkETBEffects(card, this, null)) {
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

        // Sol's Soulshift fix
        int shiftPos = CardFactoryUtil.hasKeyword(card, "Soulshift");
        while (shiftPos != -1) {
            final int n = shiftPos;
            final String parse = card.getKeyword().get(n).toString();

            final String[] k = parse.split(":");
            final String manacost = k[1];

            card.addDestroyCommand(CardFactoryUtil.abilitySoulshift(card, manacost));
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
                        card.addIntrinsicKeyword("(Echo unpaid)");
                    }
                };
                card.addComesIntoPlayCommand(intoPlay);

            }
        } // echo

        if (CardFactoryUtil.hasKeyword(card, "HandSize") != -1) {
            final String toParse = card.getKeyword().get(CardFactoryUtil.hasKeyword(card, "HandSize"));
            card.removeIntrinsicKeyword(toParse);

            final String[] parts = toParse.split(" ");
            final String mode = parts[1];
            final int amount;
            if (parts[2].equals("INF")) {
                amount = -1;
            } else {
                amount = Integer.parseInt(parts[2]);
            }
            final String target = parts[3];

            final Command entersPlay, leavesPlay, controllerChanges;

            entersPlay = new Command() {
                private static final long serialVersionUID = 98743547743456L;

                @Override
                public void execute() {
                    card.setSVar("HSStamp", "" + Player.getHandSizeStamp());
                    if (target.equals("Self") || target.equals("All")) {
                        card.getController().addHandSizeOperation(
                                new HandSizeOp(mode, amount, Integer.parseInt(card.getSVar("HSStamp"))));
                    }
                    if (target.equals("Opponent") || target.equals("All")) {
                        card.getController()
                                .getOpponent()
                                .addHandSizeOperation(
                                        new HandSizeOp(mode, amount, Integer.parseInt(card.getSVar("HSStamp"))));
                    }
                }
            };

            leavesPlay = new Command() {
                private static final long serialVersionUID = -6843545358873L;

                @Override
                public void execute() {
                    if (target.equals("Self") || target.equals("All")) {
                        card.getController().removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                    }
                    if (target.equals("Opponent") || target.equals("All")) {
                        card.getController().getOpponent()
                                .removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                    }
                }
            };

            controllerChanges = new Command() {
                private static final long serialVersionUID = 778987998465463L;

                @Override
                public void execute() {
                    Log.debug("HandSize", "Control changed: " + card.getController());
                    if (card.getController().isHuman()) {
                        AllZone.getHumanPlayer().removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                        AllZone.getComputerPlayer().addHandSizeOperation(
                                new HandSizeOp(mode, amount, Integer.parseInt(card.getSVar("HSStamp"))));

                        AllZone.getComputerPlayer().sortHandSizeOperations();
                    } else if (card.getController().isComputer()) {
                        AllZone.getComputerPlayer().removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                        AllZone.getHumanPlayer().addHandSizeOperation(
                                new HandSizeOp(mode, amount, Integer.parseInt(card.getSVar("HSStamp"))));

                        AllZone.getHumanPlayer().sortHandSizeOperations();
                    }
                }
            };

            card.addComesIntoPlayCommand(entersPlay);
            card.addLeavesPlayCommand(leavesPlay);
            card.addChangeControllerCommand(controllerChanges);
        } // HandSize

        if (CardFactoryUtil.hasKeyword(card, "Suspend") != -1) {
            // Suspend:<TimeCounters>:<Cost>
            final int n = CardFactoryUtil.hasKeyword(card, "Suspend");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                card.setSuspend(true);
                final String[] k = parse.split(":");

                final int timeCounters = Integer.parseInt(k[1]);
                final String cost = k[2];
                card.addSpellAbility(CardFactoryUtil.abilitySuspend(card, cost, timeCounters));
            }
        } // Suspend

        if (card.getManaCost().contains("X")) {
            final SpellAbility sa = card.getSpellAbility()[0];
            sa.setIsXCost(true);

            if (card.getManaCost().startsWith("X X")) {
                sa.setXManaCost("2");
            } else if (card.getManaCost().startsWith("X")) {
                sa.setXManaCost("1");
            }
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
                final Cost abCost = new Cost(altCost, card.getName(), abilities[0].isAbility());
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

                final Cost abCost = new Cost(altCost, card.getName(), altCostSA.isAbility());
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
                    restriction.setZone(Constant.Zone.Hand);
                }
                altCostSA.setRestrictions(restriction);

                altCostSA.setDescription(sb.toString());

                card.addSpellAbility(altCostSA);
            }
        }

        if (card.hasKeyword("Delve")) {
            card.getSpellAbilities().get(0).setIsDelve(true);
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
                    AllZone.getGameAction().exile(card);
                }
            };
            haunterDiesWork.setDescription(hauntDescription);

            final Input target = new Input() {
                private static final long serialVersionUID = 1981791992623774490L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Choose target creature to haunt.");
                    ButtonUtil.disableAll();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (!zone.is(Constant.Zone.Battlefield) || !c.isCreature()) {
                        return;
                    }
                    if (c.canBeTargetedBy(haunterDiesWork)) {
                        haunterDiesWork.setTargetCard(c);
                        AllZone.getStack().add(haunterDiesWork);
                        this.stop();
                    } else {
                        AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection?).");
                    }
                }
            };

            final Ability haunterDiesSetup = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final CardList creats = AllZoneUtil.getCreaturesInPlay();
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
                        AllZone.getInputControl().setInput(target);
                    } else {
                        // AI choosing what to haunt
                        final CardList oppCreats = creats.getController(AllZone.getHumanPlayer());
                        if (oppCreats.size() != 0) {
                            haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(oppCreats));
                        } else {
                            haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(creats));
                        }
                        AllZone.getStack().add(haunterDiesWork);
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
            final String abString = "DB$ MustBlock | ValidTgts$ Creature.YouDontCtrl | "
                    + "TgtPrompt$ Select target creature defending player controls | SubAbility$ DBUntap";
            final String dbString = "DB$ Untap | Defined$ Targeted";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, false);
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

                    eff.addStaticAbility("Mode$ CantBeCast | ValidCard$ Card | Caster$ You "
                            + "| Description$ For the rest of the game, you can't cast spells.");

                    final Trigger copyTrigger = forge.card.trigger.TriggerHandler.parseTrigger(
                            "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerDescription$ "
                                    + "At the beginning of each of your upkeeps, copy " + card.toString()
                                    + " except for its epic ability.", card, false);

                    copyTrigger.setOverridingAbility(origSA);

                    eff.addTrigger(copyTrigger);

                    AllZone.getTriggerHandler().suppressMode("ChangesZone");
                    AllZone.getGameAction().moveToPlay(eff);
                    AllZone.getTriggerHandler().clearSuppression("ChangesZone");

                    if (card.getController().isHuman()) {
                        AllZone.getGameAction().playSpellAbilityNoStack(origSA, false);
                    } else {
                        ComputerUtil.playNoStack(origSA);
                    }
                }
            };
            newSA.setDescription(origSA.getDescription());

            origSA.setPayCosts(null);
            origSA.setManaCost("0");

            card.clearSpellAbility();
            card.addSpellAbility(newSA);
        }

        return card;
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

    // Sol's Soulshift fix
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
        /*
         * if (card.hasKeyword("CARDNAME enters the battlefield tapped.")) {
         * card.addComesIntoPlayCommand(new Command() { private static final
         * long serialVersionUID = 203335252453049234L;
         * 
         * @Override public void execute() { // it enters the battlefield this
         * way, and should not fire // triggers card.setTapped(true); } }); }
         */// if "Comes into play tapped."
        if (card.hasKeyword("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 6436821515525468682L;

                @Override
                public void execute() {
                    final CardList lands = AllZoneUtil.getPlayerLandsInPlay(card.getController());
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
                    final CardList clICtrl = card.getOwner().getCardsIn(Zone.Battlefield);

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
                        card.addCounter(Counters.P1P1, card.getSunburstValue());
                    } else {
                        card.addCounter(Counters.CHARGE, card.getSunburstValue());
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
                    final CardList cardsInPlay = AllZoneUtil.getCardsIn(Zone.Battlefield).getType("World");
                    cardsInPlay.remove(card);
                    for (int i = 0; i < cardsInPlay.size(); i++) {
                        AllZone.getGameAction().sacrificeDestroy(cardsInPlay.get(i));
                    }
                } // execute()
            }; // Command
            card.addComesIntoPlayCommand(intoPlay);
        }

        if (CardFactoryUtil.hasKeyword(card, "Morph") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Morph");
            if (n != -1) {
                card.setPrevIntrinsicKeyword(card.getIntrinsicKeyword());
                card.setPrevType(card.getType());

                final String parse = card.getKeyword().get(n).toString();
                card.setCanMorph(true);

                final String[] k = parse.split(":");
                final Cost cost = new Cost(k[1], cardName, true);

                final int attack = card.getBaseAttack();
                final int defense = card.getBaseDefense();

                final String orgManaCost = card.getManaCost();

                card.addSpellAbility(CardFactoryUtil.abilityMorphDown(card));

                card.turnFaceDown();

                card.addSpellAbility(CardFactoryUtil.abilityMorphUp(card, cost, orgManaCost, attack, defense));

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

                card.setMadness(true);
                card.setMadnessCost(k[1]);
            }
        } // madness

        if (CardFactoryUtil.hasKeyword(card, "Devour") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Devour");
            if (n != -1) {

                final String parse = card.getKeyword().get(n).toString();
                // card.removeIntrinsicKeyword(parse);

                final String[] k = parse.split(":");
                final String magnitude = k[1];

                final int multiplier = Integer.parseInt(magnitude);
                // final String player = card.getController();
                final int[] numCreatures = new int[1];

                final Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;

                    @Override
                    public void execute() {
                        final CardList creats = AllZoneUtil.getCreaturesInPlay(card.getController());
                        creats.remove(card);
                        // System.out.println("Creats size: " + creats.size());

                        card.clearDevoured();
                        if (card.getController().isHuman()) {
                            if (creats.size() > 0) {
                                final List<Card> selection = GuiUtils.getChoicesOptional(
                                        "Select creatures to sacrifice", creats.toArray());

                                numCreatures[0] = selection.size();
                                for (int m = 0; m < selection.size(); m++) {
                                    card.addDevoured(selection.get(m));
                                    AllZone.getGameAction().sacrifice(selection.get(m));
                                }
                            }

                        } // human
                        else {
                            int count = 0;
                            for (int i = 0; i < creats.size(); i++) {
                                final Card c = creats.get(i);
                                if ((c.getNetAttack() <= 1) && ((c.getNetAttack() + c.getNetDefense()) <= 3)) {
                                    card.addDevoured(c);
                                    AllZone.getGameAction().sacrifice(c);
                                    count++;
                                }
                                // is this needed?
                                AllZone.getComputerPlayer().getZone(Zone.Battlefield).updateObservers();
                            }
                            numCreatures[0] = count;
                        }
                        final int totalCounters = numCreatures[0] * multiplier;

                        card.addCounter(Counters.P1P1, totalCounters);

                    }
                };
                card.addComesIntoPlayCommand(intoPlay);
            }
        } // Devour

        if (CardFactoryUtil.hasKeyword(card, "Modular") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Modular");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();

                final int m = Integer.parseInt(parse.substring(8));

                card.addComesIntoPlayCommand(new Command() {
                    private static final long serialVersionUID = 339412525059881775L;

                    @Override
                    public void execute() {
                        card.addCounter(Counters.P1P1, m);
                    }
                });

                final SpellAbility ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        final Card card2 = this.getTargetCard();
                        card2.addCounter(Counters.P1P1, this.getSourceCard().getCounters(Counters.P1P1));
                    } // resolve()
                };

                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;

                    @Override
                    public void execute() {
                        // Target as Modular is Destroyed
                        if (card.getController().isComputer()) {
                            CardList choices = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                            choices = choices.filter(new CardListFilter() {
                                @Override
                                public boolean addCard(final Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if (choices.size() != 0) {
                                ability.setTargetCard(CardFactoryUtil.getBestCreatureAI(choices));

                                if (ability.getTargetCard() != null) {
                                    ability.setStackDescription("Put " + card.getCounters(Counters.P1P1)
                                            + " +1/+1 counter/s from " + card + " on " + ability.getTargetCard());
                                    AllZone.getStack().addSimultaneousStackEntry(ability);

                                }
                            }
                        } else {
                            AllZone.getInputControl().setInput(CardFactoryUtil.modularInput(ability, card));
                        }
                    }
                });

            }

        } // Modular

        /*
         * WARNING: must keep this keyword processing before etbCounter keyword
         * processing.
         */
        if (CardFactoryUtil.hasKeyword(card, "Graft") != -1) {
            final int n = CardFactoryUtil.hasKeyword(card, "Graft");
            if (n != -1) {
                final String parse = card.getKeyword().get(n).toString();

                final int m = Integer.parseInt(parse.substring(6));
                final String abStr = "AB$ MoveCounter | Cost$ 0 | Source$ Self | "
                        + "Defined$ TriggeredCard | CounterType$ P1P1 | CounterNum$ 1";
                card.setSVar("GraftTrig", abStr);

                String trigStr = "Mode$ ChangesZone | ValidCard$ Creature.Other | "
                        + "Origin$ Any | Destination$ Battlefield";
                trigStr += " | TriggerZones$ Battlefield | OptionalDecider$ You | "
                        + "Execute$ GraftTrig | TriggerDescription$ ";
                trigStr += "Whenever another creature enters the battlefield, you "
                        + "may move a +1/+1 counter from this creature onto it.";
                final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, true);
                card.addTrigger(myTrigger);

                card.addIntrinsicKeyword("etbCounter:P1P1:" + m);
            }

        }

        final int etbCounter = CardFactoryUtil.hasKeyword(card, "etbCounter");
        // etbCounter:CounterType:CounterAmount:Condition:Description
        // enters the battlefield with CounterAmount of CounterType
        if (etbCounter != -1) {
            final String parse = card.getKeyword().get(etbCounter).toString();
            card.removeIntrinsicKeyword(parse);

            final String[] p = parse.split(":");
            final Counters counter = Counters.valueOf(p[1]);
            final String numCounters = p[2];
            final String condition = p.length > 3 ? p[3] : "";

            final StringBuilder sb = new StringBuilder(card.getSpellText());
            if (sb.length() != 0) {
                sb.append("\n");
            }
            if (p.length > 4) {
                sb.append(p[4]);
            } else {
                sb.append(card.getName());
                sb.append(" enters the battlefield with ");
                sb.append(numCounters);
                sb.append(" ");
                sb.append(counter.getName());
                sb.append(" counter");
                if ("1" != numCounters) {
                    sb.append("s");
                }
                sb.append(" on it.");
            }

            card.setText(sb.toString());

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2292898970576123040L;

                @Override
                public void execute() {
                    if (GameActionUtil.specialConditionsMet(card, condition)) {
                        int toAdd = -1;
                        if (numCounters.equals("X")) {
                            toAdd = CardFactoryUtil.xCount(card, card.getSVar("X"));
                        } else {
                            toAdd = Integer.parseInt(numCounters);
                        }

                        card.addCounter(counter, toAdd);
                    }

                }
            }); // ComesIntoPlayCommand
        } // if etbCounter

        final int bloodthirst = CardFactoryUtil.hasKeyword(card, "Bloodthirst");
        if (bloodthirst != -1) {
            final String numCounters = card.getKeyword().get(bloodthirst).split(" ")[1];

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -1849308549161972508L;

                @Override
                public void execute() {
                    if (card.getController().getOpponent().getAssignedDamage() > 0) {
                        int toAdd = -1;
                        if (numCounters.equals("X")) {
                            toAdd = card.getController().getOpponent().getAssignedDamage();
                        } else {
                            toAdd = Integer.parseInt(numCounters);
                        }
                        card.addCounter(Counters.P1P1, toAdd);
                    }
                }

            });
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
