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

import forge.card.mana.ManaCostParser;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.*;
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
            }

            if (lkicheck) {
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

                final SpellAbility newSA = sa.copy(activator);
                final SpellAbilityRestriction sar = newSA.getRestrictions();
                if (o.isWithFlash()) {
                    sar.setInstantSpeed(true);
                }
                sar.setZone(null);
                newSA.setMayPlay(o.getAbility());
                newSA.setMayPlayOriginal(sa);

                boolean changedManaCost = false;
                if (o.getPayManaCost() == PayManaCost.NO) {
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
                    changedManaCost = true;
                } else if (o.getAltManaCost() != null) {
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(newSA.getPayCosts().copyWithDefinedMana(o.getAltManaCost()));
                    changedManaCost = true;
                    if (host.hasSVar("AsForetoldSplitCMCHack")) {
                        // TODO: This is a temporary workaround for As Foretold interaction with split cards, better solution needed.
                        if (sa.isLeftSplit()) {
                            int leftCMC = sa.getHostCard().getCMC(Card.SplitCMCMode.LeftSplitCMC);
                            if (leftCMC > host.getCounters(CounterType.TIME)) {
                                continue;
                            }
                        } else if (sa.isRightSplit()) {
                            int rightCMC = sa.getHostCard().getCMC(Card.SplitCMCMode.RightSplitCMC);
                            if (rightCMC > host.getCounters(CounterType.TIME)) {
                                continue;
                            }
                        }
                    }
                }
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
                    final String k[] = keyword.split(":"); 
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
            } else if (keyword.equals("Conspire")) {
                final String conspireCost = "tapXType<2/Creature.SharesColorWith/" +
            "untapped creature you control that shares a color with " + source.getName() + ">";
                final Cost cost = new Cost(conspireCost, false);
                costs.add(new OptionalCostValue(OptionalCost.Conspire, cost));
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
            // need to copy cost, otherwise it does alter the original
            result.setPayCosts(result.getPayCosts().copy().add(v.getCost()));
            result.addOptionalCost(v.getType());
            
            // add some extra logic, try to move it to other parts
            switch (v.getType()) {
            case Conspire:
                result.addConspireInstance();
                break;
            case Retrace:
            case Jumpstart:
                result.getRestrictions().setZone(ZoneType.Graveyard);
                break;
            case Flash:
                result.getRestrictions().setInstantSpeed(true);
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
    
    
    /**
     * get optional additional costs.
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     * 
     * @deprecated only used by AI, replace it with new functions in AI
     */
    @Deprecated public static List<SpellAbility> getOptionalCosts(final SpellAbility original) {
        final List<SpellAbility> abilities = getAdditionalCostSpell(original);

        final Card source = original.getHostCard();

        if (!original.isSpell()) {
            return abilities;
        }

        // Buyback, Kicker
        for (KeywordInterface inst : source.getKeywords()) {
            final String keyword = inst.getOriginal();
            if (keyword.startsWith("Buyback")) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(new Cost(keyword.substring(8), false).add(newSA.getPayCosts()));
                    newSA.setDescription(newSA.getDescription() + " (with Buyback)");
                    newSA.addOptionalCost(OptionalCost.Buyback);
                    if (newSA.canPlay()) {
                        abilities.add(i, newSA);
                        i++;
                    }
                }
            } else if (keyword.startsWith("MayFlashCost")) {
                // this is there for the AI
                if ( source.getGame().getPhaseHandler().isPlayerTurn(source.getController())) {
                    continue; // don't cast it with additional flash cost during AI's own turn, commonly a waste of mana
                }
                final String[] k = keyword.split(":");
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    newSA.setPayCosts(new Cost(k[1], false).add(newSA.getPayCosts()));
                    newSA.setDescription(newSA.getDescription() + " (as though it had flash)");
                    newSA.getRestrictions().setInstantSpeed(true);
                    if (newSA.canPlay()) {
                        abilities.add(i, newSA);
                        i++;
                    }
                }
            } else if (keyword.startsWith("Kicker")) {
                String[] sCosts = TextUtil.split(keyword.substring(6), ':');
                boolean generic = "Generic".equals(sCosts[sCosts.length - 1]);
                // If this is a "generic kicker" (Undergrowth), ignore value for kicker creations
                int numKickers = sCosts.length - (generic ? 1 : 0);
                for (int i = 0; i < abilities.size(); i++) {
                    int iUnKicked = i;
                    for (int j = 0; j < numKickers; j++) {
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost = new Cost(sCosts[j], false);
                        newSA.setPayCosts(cost.add(newSA.getPayCosts()));
                        if (!generic) {
                            newSA.setDescription(newSA.getDescription() + " (Kicker " + cost.toSimpleString() + ")");
                            newSA.addOptionalCost(j == 0 ? OptionalCost.Kicker1 : OptionalCost.Kicker2);
                        } else {
                            newSA.setDescription(newSA.getDescription() + " (Optional " + cost.toSimpleString() + ")");
                            newSA.addOptionalCost(OptionalCost.Generic);
                        }
                        if (newSA.canPlay()) {
                            abilities.add(i, newSA);
                            i++;
                            iUnKicked++;
                        }
                    }
                    if (numKickers == 2) { // case for both kickers - it's hardcoded since they never have more than 2 kickers
                        final SpellAbility newSA = abilities.get(iUnKicked).copy();
                        newSA.setBasicSpell(false);
                        final Cost cost1 = new Cost(sCosts[0], false);
                        final Cost cost2 = new Cost(sCosts[1], false);
                        newSA.setDescription(TextUtil.addSuffix(newSA.getDescription(), TextUtil.concatWithSpace(" (Both kickers:", cost1.toSimpleString(),"and",TextUtil.addSuffix(cost2.toSimpleString(),")"))));
                        newSA.setPayCosts(cost2.add(cost1.add(newSA.getPayCosts())));
                        newSA.addOptionalCost(OptionalCost.Kicker1);
                        newSA.addOptionalCost(OptionalCost.Kicker2);
                        if (newSA.canPlay()) {
                            abilities.add(i, newSA);
                            i++;
                        }
                    }
                }
            }
        }

        if (source.hasKeyword(Keyword.CONSPIRE)) {
            int amount = source.getAmountOfKeyword(Keyword.CONSPIRE);
            for (int kwInstance = 1; kwInstance <= amount; kwInstance++) {
                for (int i = 0; i < abilities.size(); i++) {
                    final SpellAbility newSA = abilities.get(i).copy();
                    newSA.setBasicSpell(false);
                    final String conspireCost = "tapXType<2/Creature.SharesColorWith/untapped creature you control that shares a color with " + source.getName() + ">";
                    newSA.setPayCosts(new Cost(conspireCost, false).add(newSA.getPayCosts()));
                    final String tag = kwInstance > 1 ? " (Conspire " + kwInstance + ")" : " (Conspire)";
                    newSA.setDescription(newSA.getDescription() + tag);
                    newSA.addOptionalCost(OptionalCost.Conspire);
                    newSA.addConspireInstance();
                    if (newSA.canPlay()) {
                        abilities.add(++i, newSA);
                    }
                }
            }
        }
        return abilities;
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
