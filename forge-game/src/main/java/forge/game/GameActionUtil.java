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
package forge.game;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.cost.Cost;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.*;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * <p>
 * GameActionUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {

    private GameActionUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * Find the alternative costs to a {@link SpellAbility}.
     * </p>
     * 
     * @param sa
     *            a {@link SpellAbility}.
     * @param activator
     *            the {@link Player} for which to calculate available
     * @return a {@link List} of {@link SpellAbility} objects, each representing
     *         a possible alternative cost the provided activator can use to pay
     *         the provided {@link SpellAbility}.
     */
    public static final List<SpellAbility> getAlternativeCosts(final SpellAbility sa, final Player activator) {
        final List<SpellAbility> alternatives = Lists.newArrayList();

        Card source = sa.getHostCard();
        final Game game = source.getGame();

        if (sa.isSpell()) {
            boolean lkicheck = false;

            // need to be done before so it works with Vivien and Zoetic Cavern
            if (source.isFaceDown() && source.isInZone(ZoneType.Exile)) {
                if (!source.isLKI()) {
                    source = CardUtil.getLKICopy(source);
                }

                source.turnFaceUp(false, false);
                lkicheck = true;
            }

            if (sa.hasParam("Bestow") && !source.isBestowed() && !source.isInZone(ZoneType.Battlefield)) {
                if (!source.isLKI()) {
                    source = CardUtil.getLKICopy(source);
                }

                source.animateBestow(false);
                lkicheck = true;
            } else if (sa.isCastFaceDown()) {
                // need a copy of the card to turn facedown without trigger anything
                if (!source.isLKI()) {
                    source = CardUtil.getLKICopy(source);
                }
                source.turnFaceDownNoUpdate();
                lkicheck = true;
            } else if (sa.isAdventure() && !source.isInZone(ZoneType.Battlefield)) {
                if (!source.isLKI()) {
                    source = CardUtil.getLKICopy(source);
                }

                source.setState(CardStateName.Adventure, false);

                // need to reset CMC
                source.setLKICMC(-1);
                source.setLKICMC(source.getCMC());
                lkicheck = true;
            } else if (source.isSplitCard() && (sa.isLeftSplit() || sa.isRightSplit())) {
                if (!source.isLKI()) {
                    source = CardUtil.getLKICopy(source);
                }
                if (sa.isLeftSplit()) {
                    if (!source.hasState(CardStateName.LeftSplit)) {
                        source.addAlternateState(CardStateName.LeftSplit, false);
                        source.getState(CardStateName.LeftSplit).copyFrom(
                                sa.getHostCard().getState(CardStateName.LeftSplit), true);
                    }

                    source.setState(CardStateName.LeftSplit, false);
                }

                if (sa.isRightSplit()) {
                    if (!source.hasState(CardStateName.RightSplit)) {
                        source.addAlternateState(CardStateName.RightSplit, false);
                        source.getState(CardStateName.RightSplit).copyFrom(
                                sa.getHostCard().getState(CardStateName.RightSplit), true);
                    }

                    source.setState(CardStateName.RightSplit, false);
                }

                // need to reset CMC
                source.setLKICMC(-1);
                source.setLKICMC(source.getCMC());
                lkicheck = true;
            }

            if (lkicheck) {
                // double freeze tracker, so it doesn't update view
                game.getTracker().freeze();
                CardCollection preList = new CardCollection(source);
                game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
            }

            for (CardPlayOption o : source.mayPlay(activator)) {
                // do not appear if it can be cast with SorcerySpeed
                if (o.getAbility().hasParam("MayPlayNotSorcerySpeed") && activator.couldCastSorcery(sa)) {
                    continue;
                }
                // non basic are only allowed if PayManaCost is yes
                if (!sa.isBasicSpell() && o.getPayManaCost() == PayManaCost.NO) {
                    continue;
                }
                final Card host = o.getHost();

                SpellAbility newSA = null;

                boolean changedManaCost = false;
                if (o.getPayManaCost() == PayManaCost.NO) {
                    newSA = sa.copyWithNoManaCost(activator);
                    newSA.setBasicSpell(false);
                    changedManaCost = true;
                } else if (o.getAltManaCost() != null) {
                    newSA = sa.copyWithManaCostReplaced(activator, o.getAltManaCost());
                    newSA.setBasicSpell(false);
                    changedManaCost = true;
                } else {
                    newSA = sa.copy(activator);
                }
                final SpellAbilityRestriction sar = newSA.getRestrictions();
                if (o.isWithFlash()) {
                    sar.setInstantSpeed(true);
                }
                sar.setZone(null);
                newSA.setMayPlay(o.getAbility());
                newSA.setMayPlayOriginal(sa);

                if (changedManaCost) {
                    if ("0".equals(sa.getParam("ActivationLimit")) && sa.getHostCard().getManaCost().isNoCost()) {
                        sar.setLimitToCheck(null);
                    }
                }

                final StringBuilder sb = new StringBuilder(sa.getDescription());
                if (!source.equals(host)) {
                    sb.append(" by ");
                    if ((host.isEmblem() || host.getType().hasSubtype("Effect"))
                            && host.getEffectSource() != null) {
                        sb.append(host.getEffectSource());
                    } else {
                        sb.append(host);
                    }
                }
                if (o.getAbility().hasParam("MayPlayText")) {
                    sb.append(" (").append(o.getAbility().getParam("MayPlayText")).append(")");
                }
                sb.append(o.toString(false));
                newSA.setDescription(sb.toString());
                alternatives.add(newSA);
            }

            // reset static abilities
            if (lkicheck) {
                game.getAction().checkStaticAbilities(false);
                // clear delayed changes, this check should not have updated the view
                game.getTracker().clearDelayed();
                // need to unfreeze tracker
                game.getTracker().unfreeze();
            }
        }

        if (!sa.isBasicSpell()) {
            return alternatives;
        }

        if (sa.isCycling() && activator.hasKeyword("CyclingForZero")) {
            // set the cost to this directly to buypass non mana cost
            final SpellAbility newSA = sa.copyWithDefinedCost("Discard<1/CARDNAME>");
            newSA.setBasicSpell(false);
            newSA.getMapParams().put("CostDesc", ManaCostParser.parse("0"));
            // makes new SpellDescription
            final StringBuilder sb = new StringBuilder();
            sb.append(newSA.getCostDescription());
            sb.append(newSA.getParam("SpellDescription"));
            newSA.setDescription(sb.toString());
            
            alternatives.add(newSA);
        }

        if (sa.hasParam("Equip") && activator.hasKeyword("EquipInstantSpeed")) {
            final SpellAbility newSA = sa.copy(activator);
            SpellAbilityRestriction sar = newSA.getRestrictions();
            sar.setSorcerySpeed(false);
            sar.setInstantSpeed(true);
            newSA.setDescription(sa.getDescription() + " (you may activate any time you could cast an instant )");
            alternatives.add(newSA);
        }

        for (final KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (sa.isSpell() && keyword.startsWith("Flashback")) {
                // if source has No Mana cost, and flashback doesn't have own one,
                // flashback can't work
                if (keyword.equals("Flashback") && source.getManaCost().isNoCost()) {
                    continue;
                }

                final SpellAbility flashback = sa.copy(activator);
                flashback.setFlashBackAbility(true);

                flashback.getRestrictions().setZone(ZoneType.Graveyard);

                // there is a flashback cost (and not the cards cost)
                if (keyword.contains(":")) {
                    final String[] k = keyword.split(":");
                    flashback.setPayCosts(new Cost(k[1], false));
                }
                alternatives.add(flashback);
            }
        }
        return alternatives;
    }

    public static List<OptionalCostValue> getOptionalCostValues(final SpellAbility sa) {
        final List<OptionalCostValue> costs = Lists.newArrayList();
        if (sa == null || !sa.isSpell()) {
            return costs;
        }
        final Card source = sa.getHostCard();
        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("Buyback")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                costs.add(new OptionalCostValue(OptionalCost.Buyback, cost));
            } else if (keyword.startsWith("Entwine")) {
                String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                costs.add(new OptionalCostValue(OptionalCost.Entwine, cost));
            } else if (keyword.startsWith("Kicker")) {
                String[] sCosts = TextUtil.split(keyword.substring(6), ':');
                boolean generic = "Generic".equals(sCosts[sCosts.length - 1]);
                // If this is a "generic kicker" (Undergrowth), ignore value for kicker creations
                int numKickers = sCosts.length - (generic ? 1 : 0);
                for (int j = 0; j < numKickers; j++) {
                    final Cost cost = new Cost(sCosts[j], false);
                    OptionalCost type = null;
                    if (!generic) {
                        type = j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2;
                    } else {
                        type = OptionalCost.Generic;
                    }
                    costs.add(new OptionalCostValue(type, cost));
                }
            } else if (keyword.equals("Retrace")) {
                if (source.getZone().is(ZoneType.Graveyard)) {
                    final Cost cost = new Cost("Discard<1/Land>", false);
                    costs.add(new OptionalCostValue(OptionalCost.Retrace, cost));
                }
            } else if (keyword.equals("Jump-start")) {
                if (source.getZone().is(ZoneType.Graveyard)) {
                    final Cost cost = new Cost("Discard<1/Card>", false);
                    costs.add(new OptionalCostValue(OptionalCost.Jumpstart, cost));
                }
            } else if (keyword.startsWith("MayFlashCost")) {
                String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                costs.add(new OptionalCostValue(OptionalCost.Flash, cost));
            }
            
            // Surge while having OptionalCost is none of them
        }
        return costs;
    }
    
    public static SpellAbility addOptionalCosts(final SpellAbility sa, List<OptionalCostValue> list) {
        if (sa == null || list.isEmpty()) {
            return sa;
        }
        final SpellAbility result = sa.copy();
        for (OptionalCostValue v : list) {
            result.getPayCosts().add(v.getCost());
            result.addOptionalCost(v.getType());
            
            // add some extra logic, try to move it to other parts
            switch (v.getType()) {
            case Retrace:
            case Jumpstart:
                result.getRestrictions().setZone(ZoneType.Graveyard);
                break;
            case Flash:
                result.getRestrictions().setInstantSpeed(true);
                break;
            default:
                break;
            }
        }
        return result;
    }
    
    public static List<SpellAbility> getAdditionalCostSpell(final SpellAbility sa) {
        final List<SpellAbility> abilities = Lists.newArrayList(sa);
        if (!sa.isSpell()) {
            return abilities;
        }
        final Card source = sa.getHostCard();
        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("AlternateAdditionalCost")) {
                final List<SpellAbility> newAbilities = Lists.newArrayList();
                String[] costs = TextUtil.split(keyword, ':');

                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);

                final Cost cost1 = new Cost(costs[1], false);
                newSA.setDescription(sa.getDescription() + " (Additional cost " + cost1.toSimpleString() + ")");
                newSA.setPayCosts(cost1.add(sa.getPayCosts()));
                if (newSA.canPlay()) {
                    newAbilities.add(newSA);
                }

                //second option
                final SpellAbility newSA2 = sa.copy();
                newSA2.setBasicSpell(false);

                final Cost cost2 = new Cost(costs[2], false);
                newSA2.setDescription(sa.getDescription() + " (Additional cost " + cost2.toSimpleString() + ")");
                newSA2.setPayCosts(cost2.add(sa.getPayCosts()));
                if (newSA2.canPlay()) {
                    newAbilities.add(newSA2);
                }
                
                abilities.clear();
                abilities.addAll(newAbilities);
            }
        }
        return abilities;
    }
    
    public static SpellAbility addExtraKeywordCost(final SpellAbility sa) {
        if (!sa.isSpell() || sa.isCopied()) {
            return sa;
        }
        SpellAbility result = null;
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        host.getGame().getAction().checkStaticAbilities(false);

        boolean reset = false;

        for (KeywordInterface ki : host.getKeywords()) {
            final String o = ki.getOriginal();
            if (o.equals("Conspire")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/" +
                        "untapped creature you control that shares a color with " + host.getName() + ">";
                    final Cost cost = new Cost(conspireCost, false);
                    String str = "Pay for Conspire? " + cost.toSimpleString();

                    boolean v = pc.addKeywordCost(sa, cost, ki, str);
                    tr.setSVar("Conspire", v ? "1" : "0");

                    if (v) {
                        if (result == null) {
                            result = sa.copy();
                        }
                        result.getPayCosts().add(cost);
                        reset = true;
                    }
                }
            } else if (o.startsWith("Replicate")) {
                Trigger tr = Iterables.getFirst(ki.getTriggers(), null);
                if (tr != null) {
                    String costStr = o.split(":")[1];
                    final Cost cost = new Cost(costStr, false);

                    String str = "Choose Amount for Replicate: " + cost.toSimpleString();

                    int v = pc.chooseNumberForKeywordCost(sa, cost, ki, str, Integer.MAX_VALUE);

                    tr.setSVar("ReplicateAmount", String.valueOf(v));
                    tr.getOverridingAbility().setSVar("ReplicateAmount", String.valueOf(v));

                    for (int i = 0; i < v; i++) {
                        if (result == null) {
                            result = sa.copy();
                        }
                        result.getPayCosts().add(cost);
                        reset = true;
                    }
                }
            }
        }

        if (host.isCreature()) {
            String kw = "As an additional cost to cast creature spells," +
                    " you may pay any amount of mana. If you do, that creature enters " +
                    "the battlefield with that many additional +1/+1 counters on it.";

            for (final Card c : activator.getZone(ZoneType.Battlefield)) {
                for (KeywordInterface ki : c.getKeywords()) {
                    if (kw.equals(ki.getOriginal())) {
                        final Cost cost = new Cost(ManaCost.ONE, false);
                        String str = "Choose Amount for " + c.getName() + ": " + cost.toSimpleString();

                        int v = pc.chooseNumberForKeywordCost(sa, cost, ki, str, Integer.MAX_VALUE);

                        if (v > 0) {
                            host.addReplacementEffect(CardFactoryUtil.makeEtbCounter("etbCounter:P1P1:" + v, host, false));
                            if (result == null) {
                                result = sa.copy();
                            }
                            for (int i = 0; i < v; i++) {
                                result.getPayCosts().add(cost);
                            }
                        }
                    }
                }
            }
        }

        // reset active Trigger
        if (reset) {
            host.getGame().getTriggerHandler().resetActiveTriggers(false);
        }

        return result != null ? result : sa;
    }

    private static boolean hasUrzaLands(final Player p) {
        final CardCollectionView landsControlled = p.getCardsIn(ZoneType.Battlefield);
        return Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Mine")))
                && Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Power-Plant")))
                && Iterables.any(landsControlled, Predicates.and(CardPredicates.isType("Urza's"), CardPredicates.isType("Tower")));
    }

    public static int amountOfManaGenerated(final SpellAbility sa, boolean multiply) {
        // Calculate generated mana here for stack description and resolving

        int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;
        AbilityManaPart abMana = sa.getManaPartRecursive();

        if (sa.hasParam("Bonus")) {
            // For mana abilities that get a bonus
            // Bonus currently MULTIPLIES the base amount. Base Amounts should
            // ALWAYS be Base
            int bonus = 0;
            if (sa.getParam("Bonus").equals("UrzaLands")) {
                if (hasUrzaLands(sa.getActivatingPlayer())) {
                    bonus = Integer.parseInt(sa.getParam("BonusProduced"));
                }
            }

            amount += bonus;
        }

        if (!multiply || abMana.isAnyMana() || abMana.isComboMana() || abMana.isSpecialMana()) {
            return amount;
        } else {
            // For cards that produce like {C}{R} vs cards that produce {R}{R}.
            return abMana.mana().split(" ").length * amount;
        }
    }


    public static String generatedMana(final SpellAbility sa) {
        int amount = amountOfManaGenerated(sa, false);
        AbilityManaPart abMana = sa.getManaPart();
        String baseMana;

        if (abMana.isComboMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = abMana.getOrigProduced();
            }
        } else if (abMana.isAnyMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = "Any";
            }
        } else if (sa.getApi() == ApiType.ManaReflected) {
            baseMana = abMana.getExpressChoice();
        } else if (abMana.isSpecialMana()) {
            baseMana = abMana.getExpressChoice();
        } else {
            baseMana = abMana.mana();
        }

        if (sa.getSubAbility() != null) {
            // Mark SAs with subAbilities as undoable. These are generally things like damage, and other stuff
            // that's hard to track and remove
            sa.setUndoable(false);
        } else {
            try {
                if ((sa.getParam("Amount") != null) && (amount != Integer.parseInt(sa.getParam("Amount")))) {
                    sa.setUndoable(false);
                }
            } catch (final NumberFormatException n) {
                sa.setUndoable(false);
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        } else if (abMana.isComboMana()) {
            // amount is already taken care of in resolve method for combination mana, just append baseMana
            sb.append(baseMana);
        } else {
            if (StringUtils.isNumeric(baseMana)) {
                sb.append(amount * Integer.parseInt(baseMana));
            } else {
                sb.append(baseMana);
                for (int i = 1; i < amount; i++) {
                    sb.append(" ").append(baseMana);
                }
            }
        }
        return sb.toString();
    }

    public static CardCollectionView orderCardsByTheirOwners(Game game, CardCollectionView list, ZoneType dest) {
        CardCollection completeList = new CardCollection();
        for (Player p : game.getPlayers()) {
            CardCollection subList = new CardCollection();
            for (Card c : list) {
                if (c.getOwner().equals(p)) {
                    subList.add(c);
                }
            }
            CardCollectionView subListView = subList;
            if (subList.size() > 1) {
                subListView = p.getController().orderMoveToZoneList(subList, dest);
            }
            completeList.addAll(subListView);
        }
        return completeList;
    }

} // end class GameActionUtil
