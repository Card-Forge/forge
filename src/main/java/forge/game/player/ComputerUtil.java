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
package forge.game.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostMana;
import forge.card.cost.CostPart;
import forge.card.cost.CostPayLife;
import forge.card.cost.CostPayment;
import forge.card.cost.CostUtil;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.InputPayManaCostUtil;
import forge.error.ErrorViewer;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;

/**
 * <p>
 * ComputerUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerUtil {

    // if return true, go to next phase
    /**
     * <p>
     * playSpellAbilities.
     * </p>
     * 
     * @param all
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     * @return a boolean.
     */
    public static boolean playSpellAbilities(final SpellAbility[] all) {
        Player computer = AllZone.getComputerPlayer();
        // not sure "playing biggest spell" matters?
        ComputerUtil.sortSpellAbilityByCost(all);
        final ArrayList<SpellAbility> abilities = new ArrayList<SpellAbility>();
        final ArrayList<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : all) {
            abilities.add(0, sa);
            sa.setActivatingPlayer(computer);
            //add alternative costs as additional spell abilities
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa));
        }
        for (SpellAbility sa : abilities) {
            sa.setActivatingPlayer(computer);
            newAbilities.addAll(GameActionUtil.getSpliceAbilities(sa));
        }
        abilities.addAll(0, newAbilities);
        for (final SpellAbility sa : abilities) {
            // Don't add Counterspells to the "normal" playcard lookups
            final AbilityFactory af = sa.getAbilityFactory();
            if ((af != null) && af.getAPI().equals("Counter")) {
                continue;
            }
            sa.setActivatingPlayer(computer);

            if (ComputerUtil.canBePlayedAndPayedByAI(sa) && ComputerUtil.handlePlayingSpellAbility(sa)) {
                return false;
            }
        }
        return true;
    } // playCards()

    /**
     * <p>
     * playAbilities.
     * </p>
     * 
     * @param all
     *            a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    public static boolean playSpellAbilities(final ArrayList<SpellAbility> all) {
        final SpellAbility[] sas = new SpellAbility[all.size()];
        for (int i = 0; i < sas.length; i++) {
            sas[i] = all.get(i);
        }
        return ComputerUtil.playSpellAbilities(sas);
    } // playCards()

    /**
     * <p>
     * handlePlayingSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean handlePlayingSpellAbility(final SpellAbility sa) {

        if (sa instanceof AbilityStatic) {
            final Cost cost = sa.getPayCosts();
            if (cost == null && ComputerUtil.payManaCost(sa, AllZone.getComputerPlayer(), false, 0, true)) {
                sa.resolve();
            } else {
                final CostPayment pay = new CostPayment(cost, sa);
                if (pay.payComputerCosts()) {
                    sa.resolve();
                }
            }
            return false;
        }

        AllZone.getStack().freezeStack();
        final Card source = sa.getSourceCard();

        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
        }

        final Cost cost = sa.getPayCosts();
        final Target tgt = sa.getTarget();

        if (cost == null) {
            ComputerUtil.payManaCost(sa);
            sa.chooseTargetAI();
            sa.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(sa);
            return true;
        } else {
            if ((tgt != null) && tgt.doesTarget()) {
                sa.chooseTargetAI();
            }

            final CostPayment pay = new CostPayment(cost, sa);
            if (pay.payComputerCosts()) {
                AllZone.getStack().addAndUnfreeze(sa);
                if (sa.getSplicedCards() != null && !sa.getSplicedCards().isEmpty()) {
                    GuiUtils.chooseOneOrNone("Computer reveals spliced cards:", sa.getSplicedCards().toArray());
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
    public static int counterSpellRestriction(final SpellAbility sa) {
        // Move this to AF?
        // Restriction Level is Based off a handful of factors

        int restrict = 0;

        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        final AbilityFactory af = sa.getAbilityFactory();
        final HashMap<String, String> params = af.getMapParams();

        // Play higher costing spells first?
        final Cost cost = sa.getPayCosts();
        // Convert cost to CMC
        // String totalMana = source.getSVar("PayX"); // + cost.getCMC()

        // Consider the costs here for relative "scoring"
        if (CostUtil.hasDiscardHandCost(cost)) {
            // Null Brooch aid
            restrict -= (AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand).size() * 20);
        }

        // Abilities before Spells (card advantage)
        if (af.isAbility()) {
            restrict += 40;
        }

        // TargetValidTargeting gets biggest bonus
        if (tgt.getSAValidTargeting() != null) {
            restrict += 35;
        }

        // Unless Cost gets significant bonus + 10-Payment Amount
        final String unless = params.get("UnlessCost");
        if (unless != null) {
            final int amount = AbilityFactory.calculateAmount(source, unless, sa);

            final int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.getHumanPlayer());

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

    // if return true, go to next phase
    /**
     * <p>
     * playCounterSpell.
     * </p>
     * 
     * @param possibleCounters
     *            a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    public static boolean playCounterSpell(final ArrayList<SpellAbility> possibleCounters) {
        Player computer = AllZone.getComputerPlayer();
        SpellAbility bestSA = null;
        int bestRestriction = Integer.MIN_VALUE;
        final ArrayList<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : possibleCounters) {
            sa.setActivatingPlayer(computer);
            //add alternative costs as additional spell abilities
            newAbilities.addAll(GameActionUtil.getAlternativeCosts(sa));
        }
        possibleCounters.addAll(newAbilities);
        newAbilities.clear();
        for (SpellAbility sa : possibleCounters) {
            sa.setActivatingPlayer(computer);
            newAbilities.addAll(GameActionUtil.getSpliceAbilities(sa));
        }
        possibleCounters.addAll(newAbilities);
        for (final SpellAbility sa : possibleCounters) {
            SpellAbility currentSA = sa;
            sa.setActivatingPlayer(computer);
            // check everything necessary
            if (ComputerUtil.canBePlayedAndPayedByAI(currentSA)) {
                if (bestSA == null) {
                    bestSA = currentSA;
                    bestRestriction = ComputerUtil.counterSpellRestriction(currentSA);
                } else {
                    // Compare bestSA with this SA
                    final int restrictionLevel = ComputerUtil.counterSpellRestriction(currentSA);

                    if (restrictionLevel > bestRestriction) {
                        bestRestriction = restrictionLevel;
                        bestSA = currentSA;
                    }
                }
            }
        }

        if (bestSA == null) {
            return false;
        }

        // TODO - "Look" at Targeted SA and "calculate" the threshold
        // if (bestRestriction < targetedThreshold) return false;

        AllZone.getStack().freezeStack();
        final Card source = bestSA.getSourceCard();

        if (bestSA.isSpell() && !source.isCopiedSpell()) {
            bestSA.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
        }

        final Cost cost = bestSA.getPayCosts();

        if (cost == null) {
            // Honestly Counterspells shouldn't use this branch
            ComputerUtil.payManaCost(bestSA);
            bestSA.chooseTargetAI();
            bestSA.getBeforePayManaAI().execute();
            AllZone.getStack().addAndUnfreeze(bestSA);
        } else {
            final CostPayment pay = new CostPayment(cost, bestSA);
            if (pay.payComputerCosts()) {
                AllZone.getStack().addAndUnfreeze(bestSA);
            }
        }

        return true;
    } // playCounterSpell()

    // this is used for AI's counterspells
    /**
     * <p>
     * playStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playStack(final SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getComputerPlayer());
        if (ComputerUtil.canPayCost(sa)) {
            final Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
            }
            final Cost cost = sa.getPayCosts();
            if (cost == null) {
                ComputerUtil.payManaCost(sa);
                AllZone.getStack().add(sa);
            } else {
                final CostPayment pay = new CostPayment(cost, sa);
                if (pay.payComputerCosts()) {
                    AllZone.getStack().add(sa);
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
    public static final void playStackFree(final SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getComputerPlayer());

        final Card source = sa.getSourceCard();
        if (sa.isSpell() && !source.isCopiedSpell()) {
            sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
        }

        AllZone.getStack().add(sa);
    }

    /**
     * <p>
     * playSpellAbilityWithoutPayingManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playSpellAbilityWithoutPayingManaCost(final SpellAbility sa) {
        final SpellAbility newSA = sa.copy();
        final Cost cost = new Cost(sa.getSourceCard(), "", false);
        if (newSA.getPayCosts() != null) {
            for (final CostPart part : newSA.getPayCosts().getCostParts()) {
                if (!(part instanceof CostMana)) {
                    cost.getCostParts().add(part);
                }
            }
        }
        newSA.setPayCosts(cost);
        newSA.setManaCost("0");
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getDescription()).append(" (without paying its mana cost)");
        newSA.setDescription(sb.toString());
        newSA.setActivatingPlayer(AllZone.getComputerPlayer());

        if (!ComputerUtil.canPayAdditionalCosts(newSA)) {
            return;
        }

        final Card source = newSA.getSourceCard();
        if (newSA.isSpell() && !source.isCopiedSpell()) {
            newSA.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
        }

        final CostPayment pay = new CostPayment(cost, newSA);
        pay.payComputerCosts();

        AllZone.getStack().add(newSA);
    }

    /**
     * <p>
     * playNoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playNoStack(final SpellAbility sa) {
        // TODO: We should really restrict what doesn't use the Stack

        if (ComputerUtil.canPayCost(sa)) {
            final Card source = sa.getSourceCard();
            if (sa.isSpell() && !source.isCopiedSpell()) {
                sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
            }

            sa.setActivatingPlayer(AllZone.getComputerPlayer());

            final Cost cost = sa.getPayCosts();
            if (cost == null) {
                ComputerUtil.payManaCost(sa);
            } else {
                final CostPayment pay = new CostPayment(cost, sa);
                pay.payComputerCosts();
            }

            AbilityFactory.resolve(sa, false);

            // destroys creatures if they have lethal damage, etc..
            Singletons.getModel().getGameAction().checkStateEffects();
        }
    } // play()

    // This is for playing spells regularly (no Cascade/Ripple etc.)
    /**
     * <p>
     * canBePlayedAndPayedByAI.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean canBePlayedAndPayedByAI(final SpellAbility sa) {
        return sa.canPlay() && sa.canPlayAI() && ComputerUtil.canPayCost(sa);
    }

    /**
     * <p>
     * shouldPayCost.
     * </p>
     * 
     * @param hostCard
     *            a {@link forge.Card} object.
     * @param costString
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean shouldPayCost(final Card hostCard, final String costString) {
        final Cost cost = new Cost(hostCard, costString, false);

        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife) {
                final int remainingLife = AllZone.getComputerPlayer().getLife();
                final int lifeCost = ((CostPayLife) part).convertAmount();
                if ((remainingLife - lifeCost) < 10) {
                    return false; //Don't pay life if it would put AI under 10 life
                } else if ((remainingLife / lifeCost) < 4) {
                    return false; //Don't pay life if it is more than 25% of current life
                }
            }
        }

        return true;
    } // shouldPayCost()

    /**
     * <p>
     * canPayCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa) {
        return ComputerUtil.canPayCost(sa, AllZone.getComputerPlayer());
    } // canPayCost()

    /**
     * <p>
     * canPayCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa, final Player player) {
        if (!ComputerUtil.payManaCost(sa, player, true, 0, true)) {
            return false;
        }

        return ComputerUtil.canPayAdditionalCosts(sa, player);
    } // canPayCost()

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int determineLeftoverMana(final SpellAbility sa) {
        return ComputerUtil.determineLeftoverMana(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player) {

        int xMana = 0;

        for (int i = 1; i < 99; i++) {
            if (!ComputerUtil.payManaCost(sa, player, true, i, true)) {
                break;
            }
            xMana = i;
        }

        return xMana;
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final SpellAbility sa) {
        return ComputerUtil.canPayAdditionalCosts(sa, AllZone.getComputerPlayer());
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final SpellAbility sa, final Player player) {
        if (sa.getActivatingPlayer() == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getSourceCard());
            sb.append(" in ComputerUtil.canPayAdditionalCosts() without an activating player");
            System.out.println(sb.toString());
            sa.setActivatingPlayer(player);
        }
        return CostPayment.canPayAdditionalCosts(sa.getPayCosts(), sa);
    }

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void payManaCost(final SpellAbility sa) {
        ComputerUtil.payManaCost(sa, AllZone.getComputerPlayer(), false, 0, true);
    }

    /**
     * <p>
     * payManaCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param test
     *            (is for canPayCost, if true does not change the game state)
     * @param extraMana
     *            a int.
     * @param checkPlayable
     *            should we check if playable? use for hypothetical "can AI play this"
     * @return a boolean.
     * @since 1.0.15
     */
    public static boolean payManaCost(final SpellAbility sa, final Player player, final boolean test,
            final int extraMana, boolean checkPlayable) {
        ManaCost cost = calculateManaCost(sa, test, extraMana);

        final ManaPool manapool = player.getManaPool();

        cost = manapool.payManaFromPool(sa, cost);

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            manapool.clearManaPaid(sa, test);
            return true;
        }

        // get map of mana abilities
        final HashMap<String, ArrayList<AbilityMana>> manaAbilityMap = ComputerUtil.mapManaSources(player, checkPlayable);
        // initialize ArrayList list for mana needed
        final ArrayList<ArrayList<AbilityMana>> partSources = new ArrayList<ArrayList<AbilityMana>>();
        final ArrayList<Integer> partPriority = new ArrayList<Integer>();
        final String[] costParts = cost.toString().replace("X ", "").replace("P", "").split(" ");
        Boolean foundAllSources = findManaSources(manaAbilityMap, partSources, partPriority, costParts);
        if (!foundAllSources) {
            if (!test) {
                // real payment should not arrive here
                throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for "
                        + sa.getSourceCard().getName());
            }
            manapool.clearManaPaid(sa, test); // refund any mana taken from mana pool
            return false;
        }

        // Create array to keep track of sources used
        final ArrayList<Card> usedSources = new ArrayList<Card>();
        // this is to prevent errors for mana sources that have abilities that
        // cost mana.
        usedSources.add(sa.getSourceCard());
        // Loop over mana needed
        int nPriority = 0;
        while (nPriority < partPriority.size()) {
            final int nPart = partPriority.get(nPriority);
            final ArrayList<AbilityMana> manaAbilities = partSources.get(nPart);
            final ManaCost costPart = new ManaCost(costParts[nPart]);
            // Loop over mana abilities that can be used to current mana cost part
            for (final AbilityMana m : manaAbilities) {
                final Card sourceCard = m.getSourceCard();

                // Check if source has already been used
                if (usedSources.contains(sourceCard)) {
                    continue;
                }

                // Check if AI can still play this mana ability
                m.setActivatingPlayer(player);
                // if the AI can't pay the additional costs skip the mana ability
                if (m.getPayCosts() != null && checkPlayable) {
                    if (!ComputerUtil.canPayAdditionalCosts(m, player)) {
                        continue;
                    }
                } else if (sourceCard.isTapped() && checkPlayable) {
                    continue;
                }

                // Check for mana restrictions
                if (!m.meetsManaRestrictions(sa)) {
                    continue;
                }

                String manaProduced;
                // Check if paying snow mana
                if ("S".equals(costParts[nPart])) {
                    manaProduced = "S";
                } else {
                    // check if ability produces any color
                    if (m.isAnyMana()) {
                        String colorChoice = costParts[nPart];
                        final ArrayList<String> negEffect = cost.getManaNeededToAvoidNegativeEffect();
                        final ArrayList<String> negEffectPaid = cost.getManaPaidToAvoidNegativeEffect();
                        // Check for
                        // 1) Colorless
                        // 2) Split e.g. 2/G
                        // 3) Hybrid e.g. UG
                        if (costParts[nPart].matches("[0-9]+")) {
                            colorChoice = "W";
                            for (int n = 0; n < negEffect.size(); n++) {
                                if (!negEffectPaid.contains(negEffect.get(n))) {
                                    colorChoice = negEffect.get(n);
                                    break;
                                }
                            }
                        } else if (costParts[nPart].contains("/")) {
                            colorChoice = costParts[nPart].replace("2/", "");
                        } else if (costParts[nPart].length() > 1) {
                            colorChoice = costParts[nPart].substring(0, 1);
                            for (int n = 0; n < negEffect.size(); n++) {
                                if (costParts[nPart].contains(negEffect.get(n))
                                        && !negEffectPaid.contains(negEffect.get(n))) {
                                    colorChoice = negEffect.get(n);
                                    break;
                                }
                            }
                        }
                        m.setExpressChoice(colorChoice);
                    }
                    // get produced mana
                    manaProduced = m.getManaProduced();
                    if (manaProduced.matches("0")) {
                        continue;
                    }
                }

                // add source card to used list
                usedSources.add(sourceCard);

                // TODO: Change this if AI is able use to mana abilities that
                // produce more than one mana (111230 - ArsenalNut)
                final String color = InputPayManaCostUtil.getLongColorString(manaProduced.split(" ")[0]);
                costPart.payMana(color);

                if (!test) {
                    // Pay additional costs
                    if (m.getPayCosts() != null) {
                        final CostPayment pay = new CostPayment(m.getPayCosts(), m);
                        if (!pay.payComputerCosts()) {
                            continue;
                        }
                    } else {
                        sourceCard.tap();
                    }
                    // resolve mana ability
                    m.resolve();
                    // subtract mana from mana pool
                    cost = manapool.payManaFromAbility(sa, cost, m);
                } else {
                    cost.payMana(color);
                }
                // check if cost part is paid
                if (costPart.isPaid() || cost.isPaid()) {
                    break;
                }
            } // end of mana ability loop
            if (!costPart.isPaid() || cost.isPaid()) {
                break;
            } else {
                nPriority++;
            }

        } // end of cost parts loop

        manapool.clearManaPaid(sa, test);
        // check if paid
        if (cost.isPaid()) {
            // if (sa instanceof Spell_Permanent) // should probably add this
            sa.getSourceCard().setColorsPaid(cost.getColorsPaid());
            sa.getSourceCard().setSunburstValue(cost.getSunburst());
            return true;
        }

        if (!test) {
            final StringBuilder sb = new StringBuilder();
            sb.append("ComputerUtil : payManaCost() cost was not paid for ");
            sb.append(sa.getSourceCard().getName());
            throw new RuntimeException(sb.toString());
        }

        return false;

    } // payManaCost()

    /**
     * Find all mana sources.
     * @param manaAbilityMap
     * @param partSources
     * @param partPriority
     * @param costParts
     * @param foundAllSources
     * @return Were all mana sources found?
     */
    private static Boolean findManaSources(final HashMap<String, ArrayList<AbilityMana>> manaAbilityMap,
            final ArrayList<ArrayList<AbilityMana>> partSources, final ArrayList<Integer> partPriority,
            final String[] costParts) {
        final String[] shortColors = { "W", "U", "B", "R", "G" };
        Boolean foundAllSources;
        if (manaAbilityMap.isEmpty()) {
            foundAllSources = false;
        } else {
            foundAllSources = true;
            // loop over cost parts
            for (int nPart = 0; nPart < costParts.length; nPart++) {
                final ArrayList<AbilityMana> srcFound = new ArrayList<AbilityMana>();
                // Test for:
                // 1) Colorless
                // 2) Split e.g. 2/G
                // 3) Hybrid e.g. U/G
                // defaults to single short color
                if (costParts[nPart].matches("[0-9]+")) { // Colorless
                    srcFound.addAll(manaAbilityMap.get("1"));
                } else if (costParts[nPart].contains("2/")) { // Split
                    final String colorKey = costParts[nPart].replace("2/", "");
                    // add specified color sources first
                    if (manaAbilityMap.containsKey(colorKey)) {
                        srcFound.addAll(manaAbilityMap.get(colorKey));
                    }
                    // add other available colors
                    for (final String color : shortColors) {
                        if (!colorKey.contains(color)) {
                            // Is source available?
                            if (manaAbilityMap.containsKey(color)) {
                                srcFound.addAll(manaAbilityMap.get(color));
                            }
                        }
                    }
                } else if (costParts[nPart].length() > 1) { // Hybrid
                    final String firstColor = costParts[nPart].substring(0, 1);
                    final String secondColor = costParts[nPart].substring(2);
                    final Boolean foundFirst = manaAbilityMap.containsKey(firstColor);
                    final Boolean foundSecond = manaAbilityMap.containsKey(secondColor);
                    if (foundFirst || foundSecond) {
                        if (!foundFirst) {
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                        } else if (!foundSecond) {
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                        } else if (manaAbilityMap.get(firstColor).size() > manaAbilityMap.get(secondColor).size()) {
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                        } else {
                            srcFound.addAll(manaAbilityMap.get(secondColor));
                            srcFound.addAll(manaAbilityMap.get(firstColor));
                        }
                    }
                } else { // single color
                    if (manaAbilityMap.containsKey(costParts[nPart])) {
                        srcFound.addAll(manaAbilityMap.get(costParts[nPart]));
                    }
                }

                // add sources to array lists
                partSources.add(nPart, srcFound);
                // add to sorted priority list
                if (srcFound.size() > 0) {
                    int i;
                    for (i = 0; i < partPriority.size(); i++) {
                        if (srcFound.size() <= partSources.get(i).size()) {
                            break;
                        }
                    }
                    partPriority.add(i, nPart);
                } else {
                    foundAllSources = false;
                    break;
                }
            }
        }
        return foundAllSources;
    }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa
     * @param test
     * @param extraMana
     * @return ManaCost
     */
    private static ManaCost calculateManaCost(final SpellAbility sa, final boolean test, final int extraMana) {
        final String mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();

        ManaCost cost = new ManaCost(mana);

        cost = Singletons.getModel().getGameAction().getSpellCostChange(sa, cost);

        final Card card = sa.getSourceCard();
        // Tack xMana Payments into mana here if X is a set value
        if ((sa.getPayCosts() != null) && (cost.getXcounter() > 0)) {

            int manaToAdd = 0;
            if (test && (extraMana > 0)) {
                manaToAdd = extraMana * cost.getXcounter();
            } else {
                // For Count$xPaid set PayX in the AFs then use that here
                // Else calculate it as appropriate.
                final String xSvar = card.getSVar("X").startsWith("Count$xPaid") ? "PayX" : "X";
                if (!card.getSVar(xSvar).equals("")) {
                    if (xSvar.equals("PayX")) {
                        manaToAdd = Integer.parseInt(card.getSVar(xSvar)) * cost.getXcounter(); // X
                    } else {
                        manaToAdd = AbilityFactory.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }

            cost.increaseColorlessMana(manaToAdd);
            if (!test) {
                card.setXManaCostPaid(manaToAdd / cost.getXcounter());
            }
        }

        // Make mana needed to avoid negative effect a mandatory cost for the AI
        if (!card.getSVar("ManaNeededToAvoidNegativeEffect").equals("")) {
            final String[] negEffects = card.getSVar("ManaNeededToAvoidNegativeEffect").split(",");
            int amountAdded = 0;
            for (int nStr = 0; nStr < negEffects.length; nStr++) {
                // convert long color strings to short color strings
                if (negEffects[nStr].length() > 1) {
                    negEffects[nStr] = InputPayManaCostUtil.getShortColorString(negEffects[nStr]);
                }
                // make mana mandatory for AI
                if (!cost.isColor(negEffects[nStr])) {
                    cost.combineManaCost(negEffects[nStr]);
                    amountAdded++;
                }
            }
            cost.setManaNeededToAvoidNegativeEffect(negEffects);
            // TODO: should it be an error condition if amountAdded is greater
            // than the colorless in the original cost? (ArsenalNut - 120102)
            // adjust colorless amount to account for added mana
            cost.decreaseColorlessMana(amountAdded);
        }
        return cost;
    }

    /**
     * <p>
     * getProduceableColors.
     * </p>
     * 
     * @param m
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    /*
     * public static ArrayList<String> getProduceableColors(final AbilityMana m,
     * final Player player) { final ArrayList<String> colors = new
     * ArrayList<String>();
     * 
     * // if the mana ability is not avaiable move to the next one
     * m.setActivatingPlayer(player); if (!m.canPlay()) { return colors; }
     * 
     * if (!colors.contains(Constant.Color.BLACK) && m.isBasic() &&
     * m.mana().equals("B")) { colors.add(Constant.Color.BLACK); } if
     * (!colors.contains(Constant.Color.WHITE) && m.isBasic() &&
     * m.mana().equals("W")) { colors.add(Constant.Color.WHITE); } if
     * (!colors.contains(Constant.Color.GREEN) && m.isBasic() &&
     * m.mana().equals("G")) { colors.add(Constant.Color.GREEN); } if
     * (!colors.contains(Constant.Color.RED) && m.isBasic() &&
     * m.mana().equals("R")) { colors.add(Constant.Color.RED); } if
     * (!colors.contains(Constant.Color.BLUE) && m.isBasic() &&
     * m.mana().equals("U")) { colors.add(Constant.Color.BLUE); } if
     * (!colors.contains(Constant.Color.COLORLESS) && m.isBasic() &&
     * m.mana().equals("1")) { colors.add(Constant.Color.COLORLESS); }
     * 
     * return colors; }
     */

    /**
     * <p>
     * getAvailableMana.
     * </p>
     * @param checkPlayable
     * 
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAvailableMana(boolean checkPlayable) {
        return ComputerUtil.getAvailableMana(AllZone.getComputerPlayer(), checkPlayable);
    } // getAvailableMana()

    // gets available mana sources and sorts them
    /**
     * <p>
     * getAvailableMana.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param checkPlayable
     * @return a {@link forge.CardList} object.
     */
    public static CardList getAvailableMana(final Player player, final boolean checkPlayable) {
        final CardList list = player.getCardsIn(ZoneType.Battlefield);
        final CardList manaSources = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (checkPlayable) {
                    for (final AbilityMana am : c.getAIPlayableMana()) {
                        am.setActivatingPlayer(player);
                        if (am.canPlay()) {
                            return true;
                        }
                    }

                    return false;
                } else {
                    return true;
                }
            }
        }); // CardListFilter

        final CardList sortedManaSources = new CardList();
        final CardList otherManaSources = new CardList();
        final CardList colorlessManaSources = new CardList();
        final CardList oneManaSources = new CardList();
        final CardList twoManaSources = new CardList();
        final CardList threeManaSources = new CardList();
        final CardList fourManaSources = new CardList();
        final CardList fiveManaSources = new CardList();
        final CardList anyColorManaSources = new CardList();

        // Sort mana sources
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color many
        // 4. all other sources (creature, costs, drawback, etc.)
        for (int i = 0; i < manaSources.size(); i++) {
            final Card card = manaSources.get(i);

            if (card.isCreature() || card.isEnchanted()) {
                otherManaSources.add(card);
                continue; // don't use creatures before other permanents
            }

            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean producesAnyColor = false;
            final ArrayList<AbilityMana> manaAbilities = card.getAIPlayableMana();

            for (final AbilityMana m : manaAbilities) {

                if (m.isAnyMana()) {
                    producesAnyColor = true;
                }

                final Cost cost = m.getPayCosts();
                if (cost != null) {
                    needsLimitedResources |= !cost.isReusuableResource();
                }

                // if the AI can't pay the additional costs skip the mana
                // ability
                m.setActivatingPlayer(AllZone.getComputerPlayer());
                if (cost != null) {
                    if (!ComputerUtil.canPayAdditionalCosts(m, player)) {
                        continue;
                    }
                }

                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAIDrawback()) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good
                                                  // drawbacks (gainLife)
                }
                usableManaAbilities++;
            }

            if (needsLimitedResources) {
                otherManaSources.add(card);
            } else if (producesAnyColor) {
                anyColorManaSources.add(card);
            } else if (usableManaAbilities == 1) {
                if (manaAbilities.get(0).mana().equals("1")) {
                    colorlessManaSources.add(card);
                } else {
                    oneManaSources.add(card);
                }
            } else if (usableManaAbilities == 2) {
                twoManaSources.add(card);
            } else if (usableManaAbilities == 3) {
                threeManaSources.add(card);
            } else if (usableManaAbilities == 4) {
                fourManaSources.add(card);
            } else {
                fiveManaSources.add(card);
            }

        }
        sortedManaSources.addAll(colorlessManaSources);
        sortedManaSources.addAll(oneManaSources);
        sortedManaSources.addAll(twoManaSources);
        sortedManaSources.addAll(threeManaSources);
        sortedManaSources.addAll(fourManaSources);
        sortedManaSources.addAll(fiveManaSources);
        sortedManaSources.addAll(anyColorManaSources);
        sortedManaSources.addAll(otherManaSources);

        return sortedManaSources;
    } // getAvailableMana()

    // sorts the most needed mana abilities to come first
    /**
     * <p>
     * sortForNeeded.
     * </p>
     * 
     * @param cost
     *            a {@link forge.card.mana.ManaCost} object.
     * @param manaAbilities
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.Player} object.
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    /*
     * public static ArrayList<AbilityMana> sortForNeeded(final ManaCost cost,
     * final ArrayList<AbilityMana> manaAbilities, final Player player) {
     * 
     * ArrayList<String> colors;
     * 
     * final ArrayList<String> colorsNeededToAvoidNegativeEffect =
     * cost.getManaNeededToAvoidNegativeEffect();
     * 
     * final ArrayList<AbilityMana> res = new ArrayList<AbilityMana>();
     * 
     * final ManaCost onlyColored = new ManaCost(cost.toString());
     * 
     * onlyColored.removeColorlessMana();
     * 
     * for (final AbilityMana am : manaAbilities) { colors =
     * ComputerUtil.getProduceableColors(am, player); for (int j = 0; j <
     * colors.size(); j++) { if (onlyColored.isNeeded(colors.get(j))) {
     * res.add(am); break; } for (final String col :
     * colorsNeededToAvoidNegativeEffect) { if
     * (col.equalsIgnoreCase(colors.get(j)) ||
     * CardUtil.getShortColor(col).equalsIgnoreCase(colors.get(j))) {
     * res.add(am); } } } }
     * 
     * for (final AbilityMana am : manaAbilities) {
     * 
     * if (res.contains(am)) { break; }
     * 
     * colors = ComputerUtil.getProduceableColors(am, player); for (int j = 0; j
     * < colors.size(); j++) { if (cost.isNeeded(colors.get(j))) { res.add(am);
     * break; } for (final String col : colorsNeededToAvoidNegativeEffect) { if
     * (col.equalsIgnoreCase(colors.get(j)) ||
     * CardUtil.getShortColor(col).equalsIgnoreCase(colors.get(j))) {
     * res.add(am); } } } }
     * 
     * return res; }
     */

    /**
     * <p>
     * mapManaSources.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param checkPlayable TODO
     * @return HashMap<String, CardList>
     */
    public static HashMap<String, ArrayList<AbilityMana>> mapManaSources(final Player player, boolean checkPlayable) {
        final HashMap<String, ArrayList<AbilityMana>> manaMap = new HashMap<String, ArrayList<AbilityMana>>();

        final ArrayList<AbilityMana> whiteSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> blueSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> blackSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> redSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> greenSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> colorlessSources = new ArrayList<AbilityMana>();
        final ArrayList<AbilityMana> snowSources = new ArrayList<AbilityMana>();

        // Get list of current available mana sources
        final CardList manaSources = ComputerUtil.getAvailableMana(checkPlayable);

        // Loop over all mana sources
        for (int i = 0; i < manaSources.size(); i++) {
            final Card sourceCard = manaSources.get(i);
            final ArrayList<AbilityMana> manaAbilities = sourceCard.getAIPlayableMana();

            // Loop over all mana abilities for a source
            for (final AbilityMana m : manaAbilities) {
                m.setActivatingPlayer(AllZone.getComputerPlayer());
                if (!m.canPlay() && checkPlayable) {
                    continue;
                }

                // don't use abilities with dangerous drawbacks
                if (m.getSubAbility() != null) {
                    if (!m.getSubAbility().chkAIDrawback()) {
                        continue;
                    }
                }

                // add to colorless source list
                colorlessSources.add(m);

                // find possible colors
                if (m.canProduce("W")) {
                    whiteSources.add(m);
                }
                if (m.canProduce("U")) {
                    blueSources.add(m);
                }
                if (m.canProduce("B")) {
                    blackSources.add(m);
                }
                if (m.canProduce("R")) {
                    redSources.add(m);
                }
                if (m.canProduce("G")) {
                    greenSources.add(m);
                }
                if (m.isSnow()) {
                    snowSources.add(m);
                }
            } // end of mana abilities loop
        } // end of mana sources loop

        // Add sources
        if (!whiteSources.isEmpty()) {
            manaMap.put("W", whiteSources);
        }
        if (!blueSources.isEmpty()) {
            manaMap.put("U", blueSources);
        }
        if (!blackSources.isEmpty()) {
            manaMap.put("B", blackSources);
        }
        if (!redSources.isEmpty()) {
            manaMap.put("R", redSources);
        }
        if (!greenSources.isEmpty()) {
            manaMap.put("G", greenSources);
        }
        if (!colorlessSources.isEmpty()) {
            manaMap.put("1", colorlessSources);
        }
        if (!snowSources.isEmpty()) {
            manaMap.put("S", snowSources);
        }

        return manaMap;
    }

    // plays a land if one is available
    /**
     * <p>
     * chooseLandsToPlay.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean chooseLandsToPlay() {
        final Player computer = AllZone.getComputerPlayer();
        if (!computer.canPlayLand()) {
            return false;
        }
        CardList landList = computer.getCardsIn(ZoneType.Hand);
        landList = landList.filter(CardListFilter.LANDS);

        final CardList lands = computer.getCardsIn(ZoneType.Graveyard);
        for (final Card crd : lands) {
            if (crd.isLand() && crd.hasKeyword("May be played")) {
                landList.add(crd);
            }
        }

        landList = landList.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (c.getSVar("NeedsToPlay").length() > 0) {
                    final String needsToPlay = c.getSVar("NeedsToPlay");
                    CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);

                    list = list.getValidCards(needsToPlay.split(","), c.getController(), c);
                    if (list.isEmpty()) {
                        return false;
                    }
                }
                if (c.isType("Legendary") && !c.getName().equals("Flagstones of Trokair")) {
                    final CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                    if (list.containsName(c.getName())) {
                        return false;
                    }
                }

                // don't play the land if it has cycling and enough lands are
                // available
                final ArrayList<SpellAbility> spellAbilities = c.getSpellAbilities();
                for (final SpellAbility sa : spellAbilities) {
                    if (sa.isCycling()) {
                        final CardList hand = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
                        CardList lands = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                        lands.addAll(hand);
                        lands = lands.getType("Land");

                        if (lands.size() >= Math.max(hand.getHighestConvertedManaCost(), 6)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });

        while (!landList.isEmpty() && computer.canPlayLand()) {
            // play as many lands as you can
            int ix = 0;
            while (landList.get(ix).isReflectedLand() && ((ix + 1) < landList.size())) {
                // Skip through reflected lands. Choose last if they are all
                // reflected.
                ix++;
            }

            Card land = landList.get(ix);
            //play basic lands that are needed the most
            if (landList.getNotType("Basic").isEmpty()) {
                final CardList combined = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);

                final String[] names = "Plains,Island,Swamp,Mountain,Forest".split(",");
                final ArrayList<String> basics = new ArrayList<String>();

                // what types can I go get?
                for (final String name : names) {
                    if (landList.getType(name).size() != 0) {
                        basics.add(name);
                    }
                }

                // Which basic land is least available from hand and play, that I still
                // have in my deck
                int minSize = Integer.MAX_VALUE;
                String minType = null;

                for (int i = 0; i < basics.size(); i++) {
                    final String b = basics.get(i);
                    final int num = combined.getType(b).size();
                    if (num < minSize) {
                        minType = b;
                        minSize = num;
                    }
                }

                if (minType != null) {
                    landList = landList.getType(minType);
                }

                land = landList.get(0);
            }
            landList.remove(land);
            computer.playLand(land);

            if (AllZone.getStack().size() != 0) {
                return true;
            }
        }
        return false;
    }

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
    public static Card getCardPreference(final Card activate, final String pref, final CardList typeList) {

        if (activate != null) {
            final String[] prefValid = activate.getSVar("AIPreference").split("\\$");
            if (prefValid[0].equals(pref)) {
                final CardList prefList = typeList.getValidCards(prefValid[1].split(","), activate.getController(),
                        activate);
                if (prefList.size() != 0) {
                    prefList.shuffle();
                    return prefList.get(0);
                }
            }
        }
        if (pref.contains("SacCost")) { // search for permanents with SacMe
            for (int ip = 0; ip < 9; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 9 - ip;
                final CardList sacMeList = typeList.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return (!c.getSVar("SacMe").equals("") && (Integer.parseInt(c.getSVar("SacMe")) == priority));
                    }
                });
                if (sacMeList.size() != 0) {
                    sacMeList.shuffle();
                    return sacMeList.get(0);
                }
            }
        }

        if (pref.contains("DiscardCost")) { // search for permanents with
                                            // DiscardMe
            for (int ip = 0; ip < 9; ip++) { // priority 0 is the lowest,
                                             // priority 5 the highest
                final int priority = 9 - ip;
                final CardList sacMeList = typeList.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return (!c.getSVar("DiscardMe").equals("") && (Integer.parseInt(c.getSVar("DiscardMe")) == priority));
                    }
                });
                if (sacMeList.size() != 0) {
                    sacMeList.shuffle();
                    return sacMeList.get(0);
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
     * @param activate
     *            a {@link forge.Card} object.
     * @param target
     *            a {@link forge.Card} object.
     * @param amount
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    public static CardList chooseSacrificeType(final String type, final Card activate, final Card target,
            final int amount) {
        Player activator = AllZone.getComputerPlayer();
        CardList typeList = activator.getCardsIn(ZoneType.Battlefield);
        typeList = typeList.getValidCards(type.split(";"), activate.getController(), activate);
        if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            typeList = typeList.getNotType("Creature");
        }

        if ((target != null) && target.getController().isComputer() && typeList.contains(target)) {
            typeList.remove(target); // don't sacrifice the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        final CardList sacList = new CardList();
        int count = 0;

        while (count < amount) {
            final Card prefCard = ComputerUtil.getCardPreference(activate, "SacCost", typeList);
            if (prefCard != null) {
                sacList.add(prefCard);
                typeList.remove(prefCard);
                count++;
            } else {
                break;
            }
        }

        CardListUtil.sortAttackLowFirst(typeList);

        for (int i = count; i < amount; i++) {
            sacList.add(typeList.get(i));
        }
        return sacList;
    }

    /**
     * <p>
     * AI_discardNumType.
     * </p>
     * 
     * @param numDiscard
     *            a int.
     * @param uTypes
     *            an array of {@link java.lang.String} objects. May be null for
     *            no restrictions.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a CardList of discarded cards.
     */
    public static CardList discardNumTypeAI(final int numDiscard, final String[] uTypes, final SpellAbility sa) {
        CardList hand = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
        Card sourceCard = null;

        if ((uTypes != null) && (sa != null)) {
            hand = hand.getValidCards(uTypes, sa.getActivatingPlayer(), sa.getSourceCard());
        }
        if (sa != null) {
            sourceCard = sa.getSourceCard();
        }

        if (hand.size() < numDiscard) {
            return null;
        }

        final CardList discardList = new CardList();
        int count = 0;

        // look for good discards
        while (count < numDiscard) {
            final Card prefCard = ComputerUtil.getCardPreference(sourceCard, "DiscardCost", hand);
            if (prefCard != null) {
                discardList.add(prefCard);
                hand.remove(prefCard);
                count++;
            } else {
                break;
            }
        }

        final int discardsLeft = numDiscard - count;

        // choose rest
        for (int i = 0; i < discardsLeft; i++) {
            if (hand.size() <= 0) {
                continue;
            }
            final int numLandsInPlay = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield).getType("Land").size();
            final CardList landsInHand = hand.getType("Land");
            final int numLandsInHand = landsInHand.size();

            // Discard a land
            if (numLandsInHand > 3 || (numLandsInHand > 2 && numLandsInPlay > 0)
                    || (numLandsInHand > 1 && numLandsInPlay > 2)
                    || (numLandsInHand > 0 && numLandsInPlay > 5)) {
                discardList.add(landsInHand.get(0));
                hand.remove(landsInHand.get(0));
            } else { // Discard other stuff 
                CardListUtil.sortCMC(hand);
                int numLandsAvailable = numLandsInPlay;
                if (numLandsInHand > 0) {
                    numLandsAvailable++;
                }
                //Discard unplayable card
                if (hand.get(0).getCMC() > numLandsAvailable) {
                    discardList.add(hand.get(0));
                    hand.remove(hand.get(0));
                } else { //Discard worst card
                    Card worst = CardFactoryUtil.getWorstAI(hand);
                    discardList.add(worst);
                    hand.remove(worst);
                }
            }
        }

        return discardList;
    }

    /**
     * <p>
     * chooseExileType.
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
    public static CardList chooseExileType(final String type, final Card activate, final Card target, final int amount) {
        return ComputerUtil.chooseExileFrom(ZoneType.Battlefield, type, activate, target, amount);
    }

    /**
     * <p>
     * chooseExileFromHandType.
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
    public static CardList chooseExileFromHandType(final String type, final Card activate, final Card target,
            final int amount) {
        return ComputerUtil.chooseExileFrom(ZoneType.Hand, type, activate, target, amount);
    }

    /**
     * <p>
     * chooseExileFromGraveType.
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
    public static CardList chooseExileFromGraveType(final String type, final Card activate, final Card target,
            final int amount) {
        return ComputerUtil.chooseExileFrom(ZoneType.Graveyard, type, activate, target, amount);
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
    public static CardList chooseExileFrom(final ZoneType zone, final String type, final Card activate,
            final Card target, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(zone);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if ((target != null) && target.getController().isComputer() && typeList.contains(target)) {
            typeList.remove(target); // don't exile the card we're pumping
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);
        final CardList exileList = new CardList();

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
    public static CardList chooseTapType(final String type, final Card activate, final boolean tap, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);

        // is this needed?
        typeList = typeList.filter(CardListFilter.UNTAPPED);

        if (tap) {
            typeList.remove(activate);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);

        final CardList tapList = new CardList();

        for (int i = 0; i < amount; i++) {
            tapList.add(typeList.get(i));
        }
        return tapList;
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
    public static CardList chooseReturnType(final String type, final Card activate, final Card target, final int amount) {
        CardList typeList = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        typeList = typeList.getValidCards(type.split(","), activate.getController(), activate);
        if ((target != null) && target.getController().isComputer() && typeList.contains(target)) {
            // bounce
            // the
            // card
            // we're
            // pumping
            typeList.remove(target);
        }

        if (typeList.size() < amount) {
            return null;
        }

        CardListUtil.sortAttackLowFirst(typeList);
        final CardList returnList = new CardList();

        for (int i = 0; i < amount; i++) {
            returnList.add(typeList.get(i));
        }
        return returnList;
    }

    /**
     * <p>
     * getPossibleAttackers.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public static CardList getPossibleAttackers() {
        CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CombatUtil.canAttack(c);
            }
        });
        return list;
    }

    /**
     * <p>
     * getAttackers.
     * </p>
     * 
     * @return a {@link forge.game.phase.Combat} object.
     */
    public static Combat getAttackers() {
        final ComputerUtilAttack att = new ComputerUtilAttack(AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield),
                AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield));

        return att.getAttackers();
    }

    /**
     * <p>
     * getBlockers.
     * </p>
     * 
     * @return a {@link forge.game.phase.Combat} object.
     */
    public static Combat getBlockers() {
        final CardList blockers = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);

        return ComputerUtilBlock.getBlockers(AllZone.getCombat(), blockers);
    }

    /**
     * <p>
     * sortSpellAbilityByCost.
     * </p>
     * 
     * @param sa
     *            an array of {@link forge.card.spellability.SpellAbility}
     *            objects.
     */
    static void sortSpellAbilityByCost(final SpellAbility[] sa) {
        // sort from highest cost to lowest
        // we want the highest costs first
        final Comparator<SpellAbility> c = new Comparator<SpellAbility>() {
            @Override
            public int compare(final SpellAbility a, final SpellAbility b) {
                int a1 = CardUtil.getConvertedManaCost(a);
                int b1 = CardUtil.getConvertedManaCost(b);
                
                a1 += getSpellAbilityPriority(a);
                b1 += getSpellAbilityPriority(b);

                return b1 - a1;
            }
        }; // Comparator
        Arrays.sort(sa, c);
    } // sortSpellAbilityByCost()
    
    public static int getSpellAbilityPriority(SpellAbility sa) {
        int p = 0;
        // puts creatures in front of spells
        if (sa.getSourceCard().isCreature()) {
            p += 1;
        }
        // sort planeswalker abilities for ultimate
        if (sa.getRestrictions().getPlaneswalker()) {
            if ((sa.getAbilityFactory() != null) && sa.getAbilityFactory().getMapParams().containsKey("Ultimate")) {
                p += 9;
            }
        }
        AbilityFactory af = sa.getAbilityFactory();
        if (af == null) {
            return p;
        }

        String mode = af.getAPI();
        if ("DestroyAll".equals(mode)) {
            p += 4;
        }
        
        return p;
    }

    /**
     * <p>
     * sacrificePermanents.
     * </p>
     * 
     * @param amount
     *            a int.
     * @param list
     *            a {@link forge.CardList} object.
     * @param destroy
     *            the destroy
     * @param source
     *            the source SpellAbility
     * @return the card list
     */
    public static CardList sacrificePermanents(final int amount, final CardList list, final boolean destroy,
            SpellAbility source) {
        final CardList sacList = new CardList();
        // used in Annihilator and AF_Sacrifice
        int max = list.size();
        if (max > amount) {
            max = amount;
        }

        CardListUtil.sortCMC(list);
        list.reverse();

        for (int i = 0; i < max; i++) {
            Card c = null;

            if (destroy) {
                final CardList indestructibles = list.getKeyword("Indestructible");
                if (!indestructibles.isEmpty()) {
                    c = indestructibles.get(0);
                }
            }

            if (c == null) {
                if (list.getNotType("Creature").size() == 0) {
                    c = CardFactoryUtil.getWorstCreatureAI(list);
                } else if (list.getNotType("Land").size() == 0) {
                    c = CardFactoryUtil.getWorstLand(AllZone.getComputerPlayer());
                } else {
                    c = CardFactoryUtil.getWorstPermanentAI(list, true, true, true, true);
                }

                final ArrayList<Card> auras = c.getEnchantedBy();

                if (auras.size() > 0) {
                    // TODO: choose "worst" controlled enchanting Aura
                    for (int j = 0; j < auras.size(); j++) {
                        final Card aura = auras.get(j);
                        if (aura.getController().isPlayer(c.getController()) && list.contains(aura)) {
                            c = aura;
                            break;
                        }
                    }
                }
                if (destroy) {
                    if (!Singletons.getModel().getGameAction().destroy(c)) {
                        continue;
                    }
                } else {
                    if (!Singletons.getModel().getGameAction().sacrifice(c, source)) {
                        continue;
                    }
                }
            }
            list.remove(c);
            sacList.add(c);
        }
        return sacList;
    }

    /**
     * <p>
     * canRegenerate.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canRegenerate(final Card card) {

        if (card.hasKeyword("CARDNAME can't be regenerated.")) {
            return false;
        }

        final Player controller = card.getController();
        final CardList l = controller.getCardsIn(ZoneType.Battlefield);
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbility()) {
                // This try/catch should fix the "computer is thinking" bug
                try {
                    final AbilityFactory af = sa.getAbilityFactory();

                    if (!sa.isAbility() || (af == null) || !af.getAPI().equals("Regenerate")) {
                        continue; // Not a Regenerate ability
                    }

                    // sa.setActivatingPlayer(controller);
                    if (!(sa.canPlay() && ComputerUtil.canPayCost(sa, controller))) {
                        continue; // Can't play ability
                    }

                    final HashMap<String, String> mapParams = af.getMapParams();

                    final Target tgt = sa.getTarget();
                    if (tgt != null) {
                        if (AllZoneUtil.getCardsIn(ZoneType.Battlefield)
                                .getValidCards(tgt.getValidTgts(), controller, sa.getSourceCard()).contains(card)) {
                            return true;
                        }
                    } else if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa)
                            .contains(card)) {
                        return true;
                    }

                } catch (final Exception ex) {
                    ErrorViewer.showError(ex, "There is an error in the card code for %s:%n", c.getName(),
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
        final CardList l = controller.getCardsIn(ZoneType.Battlefield);
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbility()) {
                // if SA is from AF_Counter don't add to getPlayable
                // This try/catch should fix the "computer is thinking" bug
                try {
                    if ((sa.getAbilityFactory() != null) && sa.isAbility()) {
                        final AbilityFactory af = sa.getAbilityFactory();
                        final HashMap<String, String> mapParams = af.getMapParams();
                        if (mapParams.get("AB").equals("PreventDamage") && sa.canPlay()
                                && ComputerUtil.canPayCost(sa, controller)) {
                            if (AbilityFactory.getDefinedCards(sa.getSourceCard(), mapParams.get("Defined"), sa)
                                    .contains(card)) {
                                prevented += AbilityFactory.calculateAmount(af.getHostCard(), mapParams.get("Amount"),
                                        sa);
                            }
                            final Target tgt = sa.getTarget();
                            if (tgt != null) {
                                if (AllZoneUtil.getCardsIn(ZoneType.Battlefield)
                                        .getValidCards(tgt.getValidTgts(), controller, af.getHostCard()).contains(card)) {
                                    prevented += AbilityFactory.calculateAmount(af.getHostCard(),
                                            mapParams.get("Amount"), sa);
                                }

                            }
                        }
                    }
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex, "There is an error in the card code for %s:%n", c.getName(),
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
    public static boolean castPermanentInMain1(final SpellAbility sa) {
        final Card card = sa.getSourceCard();
        if (card.getSVar("PlayMain1").equals("TRUE")) {
            return true;
        }
        if ((card.isCreature() && (ComputerAIGeneral.hasACardGivingHaste()
                || card.hasKeyword("Haste"))) || card.hasKeyword("Exalted")) {
            return true;
        }

        // get all cards the computer controls with BuffedBy
        final CardList buffed = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
        for (int j = 0; j < buffed.size(); j++) {
            final Card buffedcard = buffed.get(j);
            if (buffedcard.getSVar("BuffedBy").length() > 0) {
                final String buffedby = buffedcard.getSVar("BuffedBy");
                final String[] bffdby = buffedby.split(",");
                if (card.isValid(bffdby, buffedcard.getController(), buffedcard)) {
                    return true;
                }
            }
            if (card.isEquipment() && buffedcard.isCreature() && CombatUtil.canAttack(buffedcard)) {
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
        final CardList antibuffed = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
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
        final CardList vengevines = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard, "Vengevine");
        if (vengevines.size() > 0) {
            final CardList creatures = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
            final CardList creatures2 = new CardList();
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
    public static boolean shouldCastLessThanMax(final Card source) {
        boolean ret = true;
        if (source.getManaCost().countX() > 0) {
            // If TargetMax is MaxTgts (i.e., an "X" cost), this is fine because AI is limited by mana available.
        } else {
            // Otherwise, if life is possibly in danger, then this is fine.
            Combat combat = new Combat();
            combat.initiatePossibleDefenders(AllZone.getComputerPlayer());
            CardList attackers = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
            for (Card att : attackers) {
                if (CombatUtil.canAttackNextTurn(att)) {
                    combat.addAttacker(att);
                }
            }
            combat = ComputerUtilBlock.getBlockers(combat, AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer()));
            if (!CombatUtil.lifeInDanger(combat)) {
                // Otherwise, return false. Do not play now.
                ret = false;
            }
        }
        return ret;
    }
}
