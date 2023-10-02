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
package forge.game.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Cost_Payment class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CostPayment extends ManaConversionMatrix {
    private final Cost cost;
    private Cost adjustedCost;
    private final SpellAbility ability;
    private final List<CostPart> paidCostParts = Lists.newArrayList();

    /**
     * <p>
     * Getter for the field <code>cost</code>.
     * </p>
     * 
     * @return a {@link forge.game.cost.Cost} object.
     */
    public final Cost getCost() {
        return this.cost;
    }

    public final SpellAbility getAbility() {
        return this.ability;
    }

    /**
     * <p>
     * Constructor for Cost_Payment.
     * </p>
     * 
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param abil
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public CostPayment(final Cost cost, final SpellAbility abil) {
        this.cost = cost;
        this.adjustedCost = cost;
        this.ability = abil;
        restoreColorReplacements();
    }

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param ability
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(Cost cost, final SpellAbility ability) {
        if (cost == null) {
            return true;
        }

        cost = CostAdjustment.adjust(cost, ability);
        return cost.canPay(ability, false);
    }

    /**
     * <p>
     * isAllPaid.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFullyPaid() {
        for (final CostPart part : adjustedCost.getCostParts()) {
            if (!this.paidCostParts.contains(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * cancelPayment.
     * </p>
     */
    public final void refundPayment() {
        Card sourceCard = this.ability.getHostCard();
        for (final CostPart part : this.paidCostParts) {
            if (part.isUndoable()) {
                part.refund(sourceCard);
            }
        }

        // Move this to CostMana
        this.ability.getActivatingPlayer().getManaPool().refundManaPaid(this.ability);
    }

    public boolean payCost(final CostDecisionMakerBase decisionMaker) {
        adjustedCost = CostAdjustment.adjust(cost, ability);
        final List<CostPart> costParts = adjustedCost.getCostPartsWithZeroMana();

        final Game game = decisionMaker.getPlayer().getGame();

        for (final CostPart part : costParts) {
            // Wrap the cost and push onto the cost stack
            game.costPaymentStack.push(part, this);

            PaymentDecision pd = part.accept(decisionMaker);

            // Right before we start paying as decided, we need to transfer the CostPayments matrix over?
            if (pd != null) {
                pd.matrix = this;
            }

            if (pd == null || !part.payAsDecided(decisionMaker.getPlayer(), pd, ability, decisionMaker.isEffect())) {
                game.costPaymentStack.pop(); // cost is resolved
                return false;
            }
            this.paidCostParts.add(part);
            game.costPaymentStack.pop(); // cost is resolved
        }

        // this clears lists used for undo. 
        for (final CostPart part1 : this.paidCostParts) {
            if (part1 instanceof CostPartWithList) {
                ((CostPartWithList) part1).resetLists();
            }
        }

        return true;
    }

    public final boolean payComputerCosts(final CostDecisionMakerBase decisionMaker) {
        // Just in case it wasn't set, but honestly it shouldn't have gotten
        // here without being set
        if (this.ability.getActivatingPlayer() == null) {
            this.ability.setActivatingPlayer(decisionMaker.getPlayer());
        }

        Map<CostPart, PaymentDecision> decisions = Maps.newHashMap();
        // for Trinisphere make sure to include Zero
        List<CostPart> parts = CostAdjustment.adjust(cost, ability).getCostPartsWithZeroMana();

        // Set all of the decisions before attempting to pay anything

        final Game game = decisionMaker.getPlayer().getGame();

        for (final CostPart part : parts) {
            PaymentDecision decision = part.accept(decisionMaker);
            if (null == decision) return false;

            // wrap the payment and push onto the cost stack
            game.costPaymentStack.push(part, this);
            if (decisionMaker.paysRightAfterDecision() && !part.payAsDecided(decisionMaker.getPlayer(), decision, ability, decisionMaker.isEffect())) {
                game.costPaymentStack.pop(); // cost is resolved
                return false;
            }

            game.costPaymentStack.pop(); // cost is either paid or deferred
            decisions.put(part, decision);
        }

        for (final CostPart part : parts) {
            // wrap the payment and push onto the cost stack
            game.costPaymentStack.push(part, this);

            if (!part.payAsDecided(decisionMaker.getPlayer(), decisions.get(part), this.ability, decisionMaker.isEffect())) {
                game.costPaymentStack.pop(); // cost is resolved
                return false;
            }
            // abilities care what was used to pay for them
            if (part instanceof CostPartWithList) {
                ((CostPartWithList) part).resetLists();
            }

            game.costPaymentStack.pop(); // cost is resolved
        }
        return true;
    }

    /**
     * <p>
     * getManaFrom.
     * </p>
     *
     * @param saBeingPaidFor
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link forge.game.mana.Mana} object.
     */
    public static Mana getMana(final Player player, final ManaCostShard shard, final SpellAbility saBeingPaidFor,
            final byte colorsPaid, Map<String, Integer> xManaCostPaidByColor) {
        final List<Pair<Mana, Integer>> weightedOptions = selectManaToPayFor(player.getManaPool(), shard,
            saBeingPaidFor, colorsPaid, xManaCostPaidByColor);

        // Exclude border case
        if (weightedOptions.isEmpty()) {
            return null; // There is no matching mana in the pool
        }

        // select equal weight possibilities
        List<Mana> manaChoices = new ArrayList<>();
        int bestWeight = Integer.MIN_VALUE;
        for (Pair<Mana, Integer> option : weightedOptions) {
            int thisWeight = option.getRight();
            Mana thisMana = option.getLeft();

            if (thisWeight > bestWeight) {
                manaChoices.clear();
                bestWeight = thisWeight;
            }

            if (thisWeight == bestWeight) {
                // add only distinct Mana-s
                boolean haveDuplicate = false;
                for (Mana m : manaChoices) {
                    if (m.equals(thisMana)) {
                        haveDuplicate = true;
                        break;
                    }
                }
                if (!haveDuplicate) {
                    manaChoices.add(thisMana);
                }
            }
        }

        // got an only one best option?
        if (manaChoices.size() == 1) {
            return manaChoices.get(0);
        }

        // if we are simulating mana payment for the human controller, use the first mana available (and avoid prompting the human player)
        if (!player.getController().isAI()) {
            return manaChoices.get(0);
        }

        // Let them choose then
        return player.getController().chooseManaFromPool(manaChoices);
    }

    private static List<Pair<Mana, Integer>> selectManaToPayFor(final ManaPool manapool, final ManaCostShard shard,
            final SpellAbility saBeingPaidFor, final byte colorsPaid, Map<String, Integer> xManaCostPaidByColor) {
        final List<Pair<Mana, Integer>> weightedOptions = new ArrayList<>();
        for (final Mana thisMana : manapool) {
            if (shard == ManaCostShard.COLORED_X && !ManaCostBeingPaid.canColoredXShardBePaidByColor(MagicColor.toShortString(thisMana.getColor()), xManaCostPaidByColor)) {
                continue;
            }

            if (!manapool.canPayForShardWithColor(shard, thisMana.getColor())) {
                continue;
            }

            if (shard.isSnow() && !thisMana.isSnow()) {
                continue;
            }

            if (thisMana.getManaAbility() != null && !thisMana.getManaAbility().meetsSpellAndShardRestrictions(saBeingPaidFor, shard, thisMana.getColor())) {
                continue;
            }

            if (!saBeingPaidFor.allowsPayingWithShard(thisMana.getSourceCard(), thisMana.getColor())) {
                continue;
            }

            int weight = 0;
            if (colorsPaid == -1) {
                // prefer colorless mana to spend
                weight += thisMana.isColorless() ? 5 : 0;
            } else {
                // get more colors for converge
                weight += (thisMana.getColor() | colorsPaid) != colorsPaid ? 5 : 0;
            }

            // prefer restricted mana to spend
            if (thisMana.isRestricted()) {
                weight += 2;
            }

            // Spend non-snow mana first
            if (!thisMana.isSnow()) {
                weight += 1;
            }

            weightedOptions.add(Pair.of(thisMana, weight));
        }
        return weightedOptions;
    }

    public static void handleOfferings(final SpellAbility sa, boolean test, boolean costIsPaid) {
        final CardZoneTable table = new CardZoneTable();
        if (sa.isOffering() && sa.getSacrificedAsOffering() != null) {
            final Card offering = sa.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (costIsPaid && !test) {
                sa.getHostCard().getGame().getAction().sacrifice(offering, sa, false, table, null);
            }
            sa.resetSacrificedAsOffering();
        }
        if (sa.isEmerge() && sa.getSacrificedAsEmerge() != null) {
            final Card emerge = sa.getSacrificedAsEmerge();
            emerge.setUsedToPay(false);
            if (costIsPaid && !test) {
                sa.getHostCard().getGame().getAction().sacrifice(emerge, sa, false, table, null);
            }
            sa.resetSacrificedAsEmerge();
        }
        if (!table.isEmpty()) {
            table.triggerChangesZoneAll(sa.getHostCard().getGame(), sa);
        }
    }
}
