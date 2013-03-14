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
package forge.game.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPart;
import forge.card.cost.CostPartMana;
import forge.card.cost.CostPayment;
import forge.card.cost.CostUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.error.BugReporter;
import forge.game.GameState;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.Aggregates;
import forge.util.MyRandom;


/**
 * <p>
 * ComputerUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerUtil {

    /**
     * <p>
     * handlePlayingSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean handlePlayingSpellAbility(final AIPlayer ai, final SpellAbility sa, final GameState game) {
        
        if (sa instanceof AbilityStatic) {
            final Cost cost = sa.getPayCosts();
            if (cost == null && ComputerUtilMana.payManaCost(ai, sa)) {
                sa.resolve();
            } else {
                final CostPayment pay = new CostPayment(cost, sa, game);
                if (pay.payComputerCosts(ai, game)) {
                    sa.resolve();
                }
            }
            game.getPhaseHandler().setPriority(ai);
            return false;
        }

        game.getStack().freezeStack();
        final Card source = sa.getSourceCard();

        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(game.getAction().moveToStack(source));
        }

        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        final Cost cost = sa.getPayCosts();

        if (cost == null) {
            ComputerUtilMana.payManaCost(ai, sa);
            game.getStack().addAndUnfreeze(sa);
            return true;
        } else {
            final CostPayment pay = new CostPayment(cost, sa, game);
            if (pay.payComputerCosts(ai, game)) {
                game.getStack().addAndUnfreeze(sa);
                if (sa.getSplicedCards() != null && !sa.getSplicedCards().isEmpty()) {
                    GuiChoose.oneOrNone("Computer reveals spliced cards:", sa.getSplicedCards());
                }
                return true;
                // TODO: solve problems with TapsForMana triggers by adding
                // sources tapped here if possible (ArsenalNut)
            }
        }
        //Should not arrive here
        System.out.println("AI failed to play " + sa.getSourceCard());
        return false;
    }

    /**
     * <p>
     * counterSpellRestriction.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int counterSpellRestriction(final Player ai, final SpellAbility sa) {
        // Move this to AF?
        // Restriction Level is Based off a handful of factors

        int restrict = 0;

        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();


        // Play higher costing spells first?
        final Cost cost = sa.getPayCosts();
        // Convert cost to CMC
        // String totalMana = source.getSVar("PayX"); // + cost.getCMC()

        // Consider the costs here for relative "scoring"
        if (CostUtil.hasDiscardHandCost(cost)) {
            // Null Brooch aid
            restrict -= (ai.getCardsIn(ZoneType.Hand).size() * 20);
        }

        // Abilities before Spells (card advantage)
        if (sa.isAbility()) {
            restrict += 40;
        }

        // TargetValidTargeting gets biggest bonus
        if (tgt.getSAValidTargeting() != null) {
            restrict += 35;
        }

        // Unless Cost gets significant bonus + 10-Payment Amount
        final String unless = sa.getParam("UnlessCost");
        if (unless != null && !unless.endsWith(">")) {
            final int amount = AbilityUtils.calculateAmount(source, unless, sa);

            final int usableManaSources = ComputerUtilCard.getUsableManaSources(ai.getOpponent());

            // If the Unless isn't enough, this should be less likely to be used
            if (amount > usableManaSources) {
                restrict += 20 - (2 * amount);
            } else {
                restrict -= (10 - (2 * amount));
            }
        }

        // Then base on Targeting Restriction
        final String[] validTgts = tgt.getValidTgts();
        if ((validTgts.length != 1) || !validTgts[0].equals("Card")) {
            restrict += 10;
        }

        // And lastly give some bonus points to least restrictive TargetType
        // (Spell,Ability,Triggered)
        final String tgtType = tgt.getTargetSpellAbilityType();
        restrict -= (5 * tgtType.split(",").length);

        return restrict;
    }

    // this is used for AI's counterspells
    /**
     * <p>
     * playStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playStack(final SpellAbility sa, final AIPlayer ai, final GameState game) {
        sa.setActivatingPlayer(ai);
        if (ComputerUtilCost.canPayCost(sa, ai)) {
            final Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(game.getAction().moveToStack(source));
            }
            final Cost cost = sa.getPayCosts();
            if (cost == null) {
                ComputerUtilMana.payManaCost(ai, sa);
                game.getStack().add(sa);
            } else {
                final CostPayment pay = new CostPayment(cost, sa, game);
                if (pay.payComputerCosts(ai, game)) {
                    game.getStack().add(sa);
                }
            }
        }
    }

    /**
     * <p>
     * playStackFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playStackFree(final Player ai, final SpellAbility sa) {
        sa.setActivatingPlayer(ai);

        final Card source = sa.getSourceCard();
        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(Singletons.getModel().getGame().getAction().moveToStack(source));
        }

        Singletons.getModel().getGame().getStack().add(sa);
    }

    /**
     * <p>
     * playSpellAbilityWithoutPayingManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playSpellAbilityWithoutPayingManaCost(final AIPlayer ai, final SpellAbility sa, final GameState game) {
        final SpellAbility newSA = sa.copy();
        final Cost cost = new Cost(sa.getSourceCard(), "", false);
        if (newSA.getPayCosts() != null) {
            for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                if (!(part instanceof CostPartMana)) {
                    cost.getCostParts().add(part);
                }
            }
        }
        newSA.setPayCosts(cost);
        newSA.setManaCost(ManaCost.ZERO);
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getDescription()).append(" (without paying its mana cost)");
        newSA.setDescription(sb.toString());
        newSA.setActivatingPlayer(ai);

        if (!ComputerUtilCost.canPayAdditionalCosts(newSA, ai, game)) {
            return;
        }

        final Card source = newSA.getSourceCard();
        if (newSA.isSpell() && !source.isCopiedSpell()) {
            newSA.setSourceCard(game.getAction().moveToStack(source));
        }

        final CostPayment pay = new CostPayment(cost, newSA, game);
        pay.payComputerCosts(ai, game);

        game.getStack().add(newSA);
    }

    /**
     * <p>
     * playNoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playNoStack(final AIPlayer ai, final SpellAbility sa, final GameState game) {
        // TODO: We should really restrict what doesn't use the Stack
        if (ComputerUtilCost.canPayCost(sa, ai)) {
            final Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(game.getAction().moveToStack(source));
            }

            sa.setActivatingPlayer(ai);

            final Cost cost = sa.getPayCosts();
            if (cost == null) {
                ComputerUtilMana.payManaCost(ai, sa);
            } else {
                final CostPayment pay = new CostPayment(cost, sa, game);
                pay.payComputerCosts((AIPlayer)ai, game);
            }

            AbilityUtils.resolve(sa, false);

            // destroys creatures if they have lethal damage, etc..
            game.getAction().checkStateEffects();
        }
    } // play()

    /**
     * <p>
     * getCardPreference.
     * </p>
     * 
     * @param activate
     *            a {@link forge.Card} object.
     * @param pref
     *            a {@link java.lang.String} object.
     * @param typeList
     *            a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCardPreference(final Player ai, final Card activate, final String pref, final List<Card> typeList) {

        if (activate != null) {
            final String[] prefValid = activate.getSVar("AIPreference").split("\\$");
            if (prefValid[0].equals(pref)) {
                final List<Card> prefList = CardLists.getValidCards(typeList, prefValid[1].split(","), activate.getController(), activate);
                if (prefList.size() != 0) {
                    CardLists.shuffle(prefList);
                    return prefList.get(0);
                }
            }
        }
        if (pref.contains("SacCost")) { // search for permanents with SacMe
            for (int ip = 0; ip < 6; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 6 - ip;
                final List<Card> sacMeList = CardLists.filter(typeList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return (!c.getSVar("SacMe").equals("") && (Integer.parseInt(c.getSVar("SacMe")) == priority));
                    }
                });
                if (sacMeList.size() != 0) {
                    CardLists.shuffle(sacMeList);
                    return sacMeList.get(0);
                }
            }
        }

        else if (pref.contains("DiscardCost")) { // search for permanents with
                                            // DiscardMe
            for (int ip = 0; ip < 6; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 6 - ip;
                for (Card c : typeList) {
                    if (priority == 3 && c.isLand()
                            && !ai.getCardsIn(ZoneType.Battlefield, "Crucible of Worlds").isEmpty()) {
                        return c;
                    }
                    if (!c.getSVar("DiscardMe").equals("") && (Integer.parseInt(c.getSVar("DiscardMe")) == priority)) {
                        return c;
                    }
                }
            }

            // Discard lands
            final List<Card> landsInHand = CardLists.getType(typeList, "Land");
            if (!landsInHand.isEmpty()) {
                final List<Card> landsInPlay = CardLists.getType(ai.getCardsIn(ZoneType.Battlefield), "Land");
                final List<Card> nonLandsInHand = CardLists.getNotType(ai.getCardsIn(ZoneType.Hand), "Land");
                final int highestCMC = Math.max(6, Aggregates.max(nonLandsInHand, CardPredicates.Accessors.fnGetCmc));
                if (landsInPlay.size() >= highestCMC
                        || (landsInPlay.size() + landsInHand.size() > 6 && landsInHand.size() > 1)) {
                    // Don't need more land.
                    return ComputerUtilCard.getWorstLand(landsInHand);
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * chooseSacrificeType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> chooseSacrificeType(final Player ai, final String type, final Card source, final Card target, final int amount) {
        List<Card> typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source);
        if (ai.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            typeList = CardLists.getNotType(typeList, "Creature");
        }

        if ((target != null) && target.getController() == ai && typeList.contains(target)) {
            typeList.remove(target); // don't sacrifice the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        final List<Card> sacList = new ArrayList<Card>();
        int count = 0;

        while (count < amount) {
            Card prefCard = ComputerUtil.getCardPreference(ai, source, "SacCost", typeList);
            if (prefCard == null) {
                prefCard = ComputerUtilCard.getWorstAI(typeList);
            }
            if (prefCard == null) {
                return null;
            }
            sacList.add(prefCard);
            typeList.remove(prefCard);
            count++;
        }
        return sacList;
    }

    /**
     * <p>
     * chooseExileFrom.
     * </p>
     * 
     * @param zone
     *            a {@link java.lang.String} object.
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> chooseExileFrom(final Player ai, final ZoneType zone, final String type, final Card activate,
            final Card target, final int amount) {
        List<Card> typeList = new ArrayList<Card>();
        if (zone.equals(ZoneType.Stack)) {
            for (int i = 0; i < Singletons.getModel().getGame().getStack().size(); i++) {
                typeList.add(Singletons.getModel().getGame().getStack().peekAbility(i).getSourceCard());
                typeList = CardLists.getValidCards(typeList, type.split(","), activate.getController(), activate);
            }
        } else {
            typeList = CardLists.getValidCards(ai.getCardsIn(zone), type.split(","), activate.getController(), activate);
        }
        if ((target != null) && target.getController() == ai && typeList.contains(target)) {
            typeList.remove(target); // don't exile the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        final List<Card> exileList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            exileList.add(typeList.get(i));
        }
        return exileList;
    }

    /**
     * <p>
     * chooseTapType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param tap
     *            a boolean.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> chooseTapType(final Player ai, final String type, final Card activate, final boolean tap, final int amount) {
        List<Card> typeList =
                CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), activate.getController(), activate);

        // is this needed?
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);

        if (tap) {
            typeList.remove(activate);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);

        final List<Card> tapList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            tapList.add(typeList.get(i));
        }
        return tapList;
    }

    /**
     * <p>
     * chooseUntapType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param untap
     *            a boolean.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> chooseUntapType(final Player ai, final String type, final Card activate, final boolean untap, final int amount) {
        List<Card> typeList =
                CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), activate.getController(), activate);

        // is this needed?
        typeList = CardLists.filter(typeList, Presets.TAPPED);

        if (untap) {
            typeList.remove(activate);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardLists.sortByPowerDesc(typeList);

        final List<Card> untapList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            untapList.add(typeList.get(i));
        }
        return untapList;
    }

    /**
     * <p>
     * chooseReturnType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static List<Card> chooseReturnType(final Player ai, final String type, final Card activate, final Card target, final int amount) {
        final List<Card> typeList =
                CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), activate.getController(), activate);
        if ((target != null) && target.getController() == ai && typeList.contains(target)) {
            // don't bounce the card we're pumping
            typeList.remove(target);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        final List<Card> returnList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            returnList.add(typeList.get(i));
        }
        return returnList;
    }

    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @return a {@link forge.game.phase.Combat} object.
     */
    public static Combat getBlockers(final Player ai) {
        final List<Card> blockers = ai.getCardsIn(ZoneType.Battlefield);

        return ComputerUtilBlock.getBlockers(ai, Singletons.getModel().getGame().getCombat(), blockers);
    }



    /**
     * <p>
     * sacrificePermanents.
     * </p>
     * @param amount
     *            a int.
     * @param source
     *            the source SpellAbility
     * @param destroy
     *            the destroy
     * @param list
     *            a {@link forge.CardList} object.
     * 
     * @return the card list
     */
    public static List<Card> choosePermanentsToSacrifice(final Player ai, final List<Card> cardlist, final int amount, SpellAbility source, 
            final boolean destroy, final boolean isOptional) {
        final List<Card> remaining = new ArrayList<Card>(cardlist);
        final List<Card> sacrificed = new ArrayList<Card>();

        if (isOptional && source.getActivatingPlayer().isOpponentOf(ai)) { 
            return sacrificed; // sacrifice none 
        }
        
        CardLists.sortByCmcDesc(remaining);
        Collections.reverse(remaining);

        final int max = Math.min(remaining.size(), amount);

        for (int i = 0; i < max; i++) {
            Card c = chooseCardToSacrifice(remaining, ai, destroy);
            remaining.remove(c);
            sacrificed.add(c);
        }
        return sacrificed;
    }

    // Precondition it wants: remaining are reverse-sorted by CMC
    private static Card chooseCardToSacrifice(final List<Card> remaining, final Player ai, final boolean destroy) {
        if (destroy) {
            final List<Card> indestructibles = CardLists.getKeyword(remaining, "Indestructible");
            if (!indestructibles.isEmpty()) {
                return indestructibles.get(0);
            }
        }
        for (int ip = 0; ip < 6; ip++) { // priority 0 is the lowest, priority 5 the highest
            final int priority = 6 - ip;
            for (Card card : remaining) {
                if (!card.getSVar("SacMe").equals("") && Integer.parseInt(card.getSVar("SacMe")) == priority) {
                    return card;
                }
            }
        }

        Card c;

        if (CardLists.getNotType(remaining, "Creature").size() == 0) {
            c = ComputerUtilCard.getWorstCreatureAI(remaining);
        } else if (CardLists.getNotType(remaining, "Land").size() == 0) {
            c = ComputerUtilCard.getWorstLand(CardLists.filter(remaining, CardPredicates.Presets.LANDS));
        } else {
            c = ComputerUtilCard.getWorstPermanentAI(remaining, false, false, false, false);
        }

        final ArrayList<Card> auras = c.getEnchantedBy();

        if (auras.size() > 0) {
            // TODO: choose "worst" controlled enchanting Aura
            for (int j = 0; j < auras.size(); j++) {
                final Card aura = auras.get(j);
                if (aura.getController().equals(c.getController()) && remaining.contains(aura)) {
                    return aura;
                }
            }
        }
        return c;
    }

    /**
     * <p>
     * canRegenerate.
     * </p>
     * @param ai 
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canRegenerate(Player ai, final Card card) {

        if (card.hasKeyword("CARDNAME can't be regenerated.")) {
            return false;
        }

        final Player controller = card.getController();
        final List<Card> l = controller.getCardsIn(ZoneType.Battlefield);
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbility()) {
                // This try/catch should fix the "computer is thinking" bug
                try {

                    if (!sa.isAbility() || sa.getApi() != ApiType.Regenerate) {
                        continue; // Not a Regenerate ability
                    }

                    // sa.setActivatingPlayer(controller);
                    if (!(sa.canPlay() && ComputerUtilCost.canPayCost(sa, controller))) {
                        continue; // Can't play ability
                    }

                    if (controller == ai) {
                        final Cost abCost = sa.getPayCosts();
                        if (abCost != null) {
                            // AI currently disabled for these costs
                            if (!ComputerUtilCost.checkLifeCost(controller, abCost, c, 4, null)) {
                                continue; // Won't play ability
                            }

                            if (!ComputerUtilCost.checkSacrificeCost(controller, abCost, c)) {
                                continue; // Won't play ability
                            }

                            if (!ComputerUtilCost.checkCreatureSacrificeCost(controller, abCost, c)) {
                                continue; // Won't play ability
                            }
                        }
                    }

                    final Target tgt = sa.getTarget();
                    if (tgt != null) {
                        if (CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), controller, sa.getSourceCard()).contains(card)) {
                            return true;
                        }
                    } else if (AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa)
                            .contains(card)) {
                        return true;
                    }

                } catch (final Exception ex) {
                    BugReporter.reportException(ex, "There is an error in the card code for %s:%n", c.getName(),
                            ex.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * possibleDamagePrevention.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int possibleDamagePrevention(final Card card) {

        int prevented = 0;

        final Player controller = card.getController();
        final List<Card> l = controller.getCardsIn(ZoneType.Battlefield);
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbility()) {
                // if SA is from AF_Counter don't add to getPlayable
                // This try/catch should fix the "computer is thinking" bug
                try {
                    if (sa.getApi() == null || !sa.isAbility()) {
                        continue;
                    }

                    if (sa.getApi() == ApiType.PreventDamage && sa.canPlay()
                            && ComputerUtilCost.canPayCost(sa, controller)) {
                        if (AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa).contains(card)) {
                            prevented += AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa);
                        }
                        final Target tgt = sa.getTarget();
                        if (tgt != null) {
                            if (CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), controller, sa.getSourceCard()).contains(card)) {
                                prevented += AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa);
                            }

                        }
                    }
                } catch (final Exception ex) {
                    BugReporter.reportException(ex, "There is an error in the card code for %s:%n", c.getName(),
                            ex.getMessage());
                }
            }
        }
        return prevented;
    }

    /**
     * <p>
     * castPermanentInMain1.
     * </p>
     * 
     * @param sa
     *            a SpellAbility object.
     * @return a boolean.
     */
    public static boolean castPermanentInMain1(final Player ai, final SpellAbility sa) {
        final Card card = sa.getSourceCard();
        if (card.getSVar("PlayMain1").equals("TRUE")) {
            return true;
        }
        if ((card.isCreature() && (ComputerUtil.hasACardGivingHaste(ai)
                || card.hasKeyword("Haste"))) || card.hasKeyword("Exalted")) {
            return true;
        }

        // get all cards the computer controls with BuffedBy
        final List<Card> buffed = ai.getCardsIn(ZoneType.Battlefield);
        for (int j = 0; j < buffed.size(); j++) {
            final Card buffedcard = buffed.get(j);
            if (buffedcard.getSVar("BuffedBy").length() > 0) {
                final String buffedby = buffedcard.getSVar("BuffedBy");
                final String[] bffdby = buffedby.split(",");
                if (card.isValid(bffdby, buffedcard.getController(), buffedcard)) {
                    return true;
                }
            }
            if (card.isEquipment() && buffedcard.isCreature() && CombatUtil.canAttack(buffedcard, ai.getOpponent())) {
                return true;
            }
            if (card.isCreature() && buffedcard.hasKeyword("Soulbond") && !buffedcard.isPaired()) {
                return true;
            }
            if (card.hasKeyword("Soulbond") && buffedcard.isCreature() && !buffedcard.isPaired()) {
                return true;
            }
        } // BuffedBy

        // get all cards the human controls with AntiBuffedBy
        final List<Card> antibuffed = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
        for (int k = 0; k < antibuffed.size(); k++) {
            final Card buffedcard = antibuffed.get(k);
            if (buffedcard.getSVar("AntiBuffedBy").length() > 0) {
                final String buffedby = buffedcard.getSVar("AntiBuffedBy");
                final String[] bffdby = buffedby.split(",");
                if (card.isValid(bffdby, buffedcard.getController(), buffedcard)) {
                    return true;
                }
            }
        } // AntiBuffedBy
        final List<Card> vengevines = ai.getCardsIn(ZoneType.Graveyard, "Vengevine");
        if (vengevines.size() > 0) {
            final List<Card> creatures = ai.getCardsIn(ZoneType.Hand);
            final List<Card> creatures2 = new ArrayList<Card>();
            for (int i = 0; i < creatures.size(); i++) {
                if (creatures.get(i).isCreature() && creatures.get(i).getManaCost().getCMC() <= 3) {
                    creatures2.add(creatures.get(i));
                }
            }
            if (((creatures2.size() + CardUtil.getThisTurnCast("Creature.YouCtrl", vengevines.get(0)).size()) > 1)
                    && card.isCreature() && card.getManaCost().getCMC() <= 3) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is it OK to cast this for less than the Max Targets?
     * @param source the source Card
     * @return true if it's OK to cast this Card for less than the max targets
     */
    public static boolean shouldCastLessThanMax(final Player ai, final Card source) {
        boolean ret = true;
        if (source.getManaCost().countX() > 0) {
            // If TargetMax is MaxTgts (i.e., an "X" cost), this is fine because AI is limited by mana available.
        } else {
            // Otherwise, if life is possibly in danger, then this is fine.
            Combat combat = new Combat();
            combat.initiatePossibleDefenders(ai);
            List<Card> attackers = ai.getOpponent().getCreaturesInPlay();
            for (Card att : attackers) {
                if (CombatUtil.canAttackNextTurn(att)) {
                    combat.addAttacker(att);
                }
            }
            combat = ComputerUtilBlock.getBlockers(ai, combat, ai.getCreaturesInPlay());
            if (!ComputerUtilCombat.lifeInDanger(ai, combat)) {
                // Otherwise, return false. Do not play now.
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Is this discard probably worse than a random draw?
     * @param discard Card to discard
     * @return boolean
     */
    public static boolean isWorseThanDraw(final Player ai, Card discard) {
        if (!discard.getSVar("DiscardMe").equals("")) {
            return true;
        }
        final List<Card> landsInPlay = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS);
        final List<Card> landsInHand = CardLists.filter(ai.getCardsIn(ZoneType.Hand), CardPredicates.Presets.LANDS);
        final List<Card> nonLandsInHand = CardLists.getNotType(ai.getCardsIn(ZoneType.Hand), "Land");
        final int highestCMC = Math.max(6, Aggregates.max(nonLandsInHand, CardPredicates.Accessors.fnGetCmc));
        final int discardCMC = discard.getCMC();
        if (discard.isLand()) {
            if (landsInPlay.size() >= highestCMC
                    || (landsInPlay.size() + landsInHand.size() > 6 && landsInHand.size() > 1)
                    || (landsInPlay.size() > 3 && nonLandsInHand.size() == 0)) {
                // Don't need more land.
                return true;
            }
        } else { //non-land
            if (discardCMC > landsInPlay.size() + landsInHand.size() + 2) {
                // not castable for some time.
                return true;
            } else if (!Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)
                    && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.MAIN2)
                    && discardCMC > landsInPlay.size() + landsInHand.size()
                    && discardCMC > landsInPlay.size() + 1
                    && nonLandsInHand.size() > 1) {
                // not castable for at least one other turn.
                return true;
            } else if (landsInPlay.size() > 5 && discard.getCMC() <= 1
                    && !discard.hasProperty("hasXCost", ai, null)) {
                // Probably don't need small stuff now.
                return true;
            }
        }
        return false;
    }

    // returns true if it's better to wait until blockers are declared
    /**
     * <p>
     * waitForBlocking.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean (returns true if it's better to wait until blockers are declared).
     */
    public static boolean waitForBlocking(final SpellAbility sa) {
        final PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler();

        return (sa.getSourceCard().isCreature()
                && sa.getPayCosts().hasTapCost()
                && (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                 || !ph.getNextTurn().equals(sa.getActivatingPlayer())));
    }

    // returns true if the AI should stop using the ability
    /**
     * <p>
     * preventRunAwayActivations.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean (returns true if the AI should stop using the ability).
     */
    public static boolean preventRunAwayActivations(final SpellAbility sa) {
        final int activations = sa.getRestrictions().getNumberTurnActivations();
        if (activations < 10) { //10 activations per turn should still be acceptable
            return false;
        }
        final Random r = MyRandom.getRandom();

        return r.nextFloat() <= Math.pow(.95, activations);
    }

    /**
     * <p>
     * hasACardGivingHaste.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean hasACardGivingHaste(final Player ai) {
        final List<Card> all = new ArrayList<Card>(ai.getCardsIn(ZoneType.Battlefield));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(ai));
        all.addAll(ai.getCardsIn(ZoneType.Hand));
    
        for (final Card c : all) {
            for (final SpellAbility sa : c.getSpellAbility()) {
    
                if (sa.getApi() == null) {
                    continue;
                }
    
                /// ????
                // if ( sa.isAbility() || sa.isSpell() && sa.getApi() != ApiType.Pump ) continue
                if (sa.hasParam("AB") && !sa.getParam("AB").equals("Pump")) {
                    continue;
                }
                if (sa.hasParam("SP") && !sa.getParam("SP").equals("Pump")) {
                    continue;
                }
    
                if (sa.hasParam("KW") && sa.getParam("KW").contains("Haste")) {
                    return true;
                }
            }
        }
    
        return false;
    } // hasACardGivingHaste

    /**
     * <p>
     * predictThreatenedObjects.
     * </p>
     * 
     * @param saviourAf
     *            a AbilityFactory object
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(final Player aiPlayer, final SpellAbility sa) {
        final ArrayList<Object> objects = new ArrayList<Object>();
        if (Singletons.getModel().getGame().getStack().isEmpty()) {
            return objects;
        }
    
        // check stack for something that will kill this
        final SpellAbility topStack = Singletons.getModel().getGame().getStack().peekAbility();
        objects.addAll(ComputerUtil.predictThreatenedObjects(aiPlayer, sa, topStack));
    
        return objects;
    }

    /**
     * <p>
     * predictThreatenedObjects.
     * </p>
     * 
     * @param saviourAf
     *            a AbilityFactory object
     * @param topStack
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public static ArrayList<Object> predictThreatenedObjects(final Player aiPlayer, final SpellAbility saviour,
            final SpellAbility topStack) {
        ArrayList<Object> objects = new ArrayList<Object>();
        final ArrayList<Object> threatened = new ArrayList<Object>();
        ApiType saviourApi = saviour.getApi();
    
        if (topStack == null) {
            return objects;
        }
    
        final Card source = topStack.getSourceCard();
        final ApiType threatApi = topStack.getApi();
    
        // Can only Predict things from AFs
        if (threatApi != null) {
            final Target tgt = topStack.getTarget();
    
            if (tgt == null) {
                if (topStack.hasParam("Defined")) {
                    objects = AbilityUtils.getDefinedObjects(source, topStack.getParam("Defined"), topStack);
                } else if (topStack.hasParam("ValidCards")) {
                    List<Card> battleField = aiPlayer.getCardsIn(ZoneType.Battlefield);
                    List<Card> cards = CardLists.getValidCards(battleField, topStack.getParam("ValidCards").split(","), source.getController(), source);
                    for (Card card : cards) {
                        objects.add(card);
                    }
                }
            } else {
                objects = tgt.getTargets();
            }
    
            // Determine if Defined Objects are "threatened" will be destroyed
            // due to this SA
    
            // Lethal Damage => prevent damage/regeneration/bounce/shroud
            if (threatApi == ApiType.DealDamage || threatApi == ApiType.DamageAll) {
                // If PredictDamage is >= Lethal Damage
                final int dmg = AbilityUtils.calculateAmount(topStack.getSourceCard(),
                        topStack.getParam("NumDmg"), topStack);
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
    
                        // indestructible
                        if (c.hasKeyword("Indestructible")) {
                            continue;
                        }
    
                        // already regenerated
                        if (c.getShield() > 0) {
                            continue;
                        }
    
                        // don't use it on creatures that can't be regenerated
                        if ((saviourApi == ApiType.Regenerate || saviourApi == ApiType.RegenerateAll) && !c.canBeShielded()) {
                            continue;
                        }
    
                        // give Shroud to targeted creatures
                        if (saviourApi == ApiType.Pump && tgt == null && saviour.hasParam("KW")
                                && (saviour.getParam("KW").endsWith("Shroud")
                                        || saviour.getParam("KW").endsWith("Hexproof"))) {
                            continue;
                        }
    
                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi == ApiType.ChangeZone && (c.getOwner().isOpponentOf(aiPlayer) || c.isToken())) {
                            continue;
                        }
    
                        if (ComputerUtilCombat.predictDamageTo(c, dmg, source, false) >= ComputerUtilCombat.getDamageToKill(c)) {
                            threatened.add(c);
                        }
                    } else if (o instanceof Player) {
                        final Player p = (Player) o;
    
                        if (source.hasKeyword("Infect")) {
                            if (ComputerUtilCombat.predictDamageTo(p, dmg, source, false) >= p.getPoisonCounters()) {
                                threatened.add(p);
                            }
                        } else if (ComputerUtilCombat.predictDamageTo(p, dmg, source, false) >= p.getLife()) {
                            threatened.add(p);
                        }
                    }
                }
            }
            // Destroy => regeneration/bounce/shroud
            else if ((threatApi == ApiType.Destroy || threatApi == ApiType.DestroyAll)
                    && (((saviourApi == ApiType.Regenerate || saviourApi == ApiType.RegenerateAll)
                    && !topStack.hasParam("NoRegen")) || saviourApi == ApiType.ChangeZone || saviourApi == ApiType.Pump)) {
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
                        // indestructible
                        if (c.hasKeyword("Indestructible")) {
                            continue;
                        }
    
                        // already regenerated
                        if (c.getShield() > 0) {
                            continue;
                        }
    
                        // give Shroud to targeted creatures
                        if (saviourApi == ApiType.Pump && tgt == null && saviour.hasParam("KW")
                                && (saviour.getParam("KW").endsWith("Shroud")
                                        || saviour.getParam("KW").endsWith("Hexproof"))) {
                            continue;
                        }
    
                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi == ApiType.ChangeZone && (c.getOwner().isOpponentOf(aiPlayer) || c.isToken())) {
                            continue;
                        }
    
                        // don't use it on creatures that can't be regenerated
                        if (saviourApi == ApiType.Regenerate && !c.canBeShielded()) {
                            continue;
                        }
                        threatened.add(c);
                    }
                }
            }
            // Exiling => bounce/shroud
            else if ((threatApi == ApiType.ChangeZone || threatApi == ApiType.ChangeZoneAll)
                    && (saviourApi == ApiType.ChangeZone || saviourApi == ApiType.Pump)
                    && topStack.hasParam("Destination")
                    && topStack.getParam("Destination").equals("Exile")) {
                for (final Object o : objects) {
                    if (o instanceof Card) {
                        final Card c = (Card) o;
                        // give Shroud to targeted creatures
                        if (saviourApi == ApiType.Pump && tgt == null && saviour.hasParam("KW")
                                && (saviour.getParam("KW").endsWith("Shroud") || saviour.getParam("KW").endsWith("Hexproof"))) {
                            continue;
                        }
    
                        // don't bounce or blink a permanent that the human
                        // player owns or is a token
                        if (saviourApi == ApiType.ChangeZone && (c.getOwner().isOpponentOf(aiPlayer) || c.isToken())) {
                            continue;
                        }
    
                        threatened.add(c);
                    }
                }
            }
        }
    
        threatened.addAll(ComputerUtil.predictThreatenedObjects(aiPlayer, saviour, topStack.getSubAbility()));
        return threatened;
    }

    // Computer mulligans if there are no cards with converted mana cost of
    // 0 in its hand
    public static boolean wantMulligan(AIPlayer ai) {
        final int AI_MULLIGAN_THRESHOLD = 5;
        
        final List<Card> handList = ai.getCardsIn(ZoneType.Hand);
        final boolean hasLittleCmc0Cards = CardLists.getValidCards(handList, "Card.cmcEQ0", ai, null).size() < 2;
        return (handList.size() > AI_MULLIGAN_THRESHOLD) && hasLittleCmc0Cards;

    }
}
