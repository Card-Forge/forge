package forge.ai;

import com.google.common.collect.ListMultimap;
import forge.ai.AiCardMemory.MemorySet;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.cost.CostPartMana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Castability-aware mana source selection: counts hand/command spells still castable after each
 * candidate source is reserved. Can be disabled for performance on low-end devices.
 */
public final class CastabilityProbe {
    /** Max candidates evaluated per castability probe on large boards. */
    static final int CANDIDATE_CAP = 5;
    /** Soft CMC cap: skip castability dry-runs above this CMC when total mana budget is insufficient. */
    private static final int SOFT_CMC_CAP = 3;

    /** System property {@code forge.manaPayment.castabilityProbe} overrides preference when set. */
    private static final String SYS_PROP = "forge.manaPayment.castabilityProbe";

    private static boolean defaultEnabled = true;
    private static int dryRunCountForTests;

    private CastabilityProbe() {
    }

    /** Whether castability probing is active (preference / system property). */
    public static boolean isEnabled() {
        final String prop = System.getProperty(SYS_PROP);
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        return defaultEnabled;
    }

    /** Set default from Forge preferences at startup (see {@link forge.model.FModel}). */
    public static void setDefaultEnabled(final boolean enabled) {
        defaultEnabled = enabled;
    }

    /** Test hook: enable probe and clear JVM override (see {@link forge.ai.controller.AutoPaymentTest}). */
    public static void enableForTests() {
        System.clearProperty(SYS_PROP);
        defaultEnabled = true;
    }

    /**
     * Castability-aware source comparison during payment. Enabled for human payment-prompt Auto preview
     * and for AI production payment of hand/command spells only.
     */
    static boolean shouldUse(final SpellAbility sa, final boolean test,
            final ComputerUtilMana.ManaPaymentContext ctx) {
        if (!isEnabled()) {
            return false;
        }
        final Card host = sa.getHostCard();
        if (host == null) {
            return false;
        }
        if (host.isInZone(ZoneType.Hand) || host.isInZone(ZoneType.Command)) {
            return test ? ctx != null && ctx.paymentPromptPreview : true;
        }
        return false;
    }

    /** Nested filter activations use castability only when the spell being paid is in hand/command. */
    static boolean shouldUseForNestedActivation(final SpellAbility sa, final boolean test,
            final ComputerUtilMana.ManaPaymentContext ctx) {
        final Card host = sa.getHostCard();
        if (host == null || (!host.isInZone(ZoneType.Hand) && !host.isInZone(ZoneType.Command))) {
            return false;
        }
        return shouldUse(sa, test, ctx);
    }

    @FunctionalInterface
    interface ConsumedBuilder {
        Set<Card> build(SpellAbility cand, SpellAbility sa, Player ai, ComputerUtilMana.ManaPaymentContext ctx);
    }

    /**
     * Among candidates, pick the source that leaves the most hand/command spells castable afterwards.
     */
    static SpellAbility pickBest(final ManaCostBeingPaid cost, final List<SpellAbility> candidates,
            final SpellAbility sa, final Player ai, final ManaCostShard toPay, final ConsumedBuilder consumedBuilder,
            final boolean preferMultiForGeneric, final boolean test, final ComputerUtilMana.ManaPaymentContext ctx) {
        SpellAbility best = null;
        int bestCastable = -1;
        int bestEfficiency = Integer.MAX_VALUE;
        final boolean multicolorHand = ComputerUtilMana.handHasMulticolorManaSpells(ai, sa, ctx);
        final ComputerUtilMana.ManaPaymentContext probeCtx = ctx.withFilterProbe();
        for (final SpellAbility cand : capCandidates(candidates)) {
            final Set<Card> sacSnapshot = ComputerUtilMana.snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
            final Set<Card> tapSnapshot = ComputerUtilMana.snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
            final Set<Card> consumed = consumedBuilder.build(cand, sa, ai, probeCtx);
            if (consumed == null) {
                ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
                ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
                continue;
            }
            final int castable = countCastableSpellsAfterPayment(ai, sa, consumed, probeCtx);
            ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
            ComputerUtilMana.debugLog(test, "    castability " + cand.getHostCard() + " -> " + castable
                    + " hand/command spells remain");
            final boolean preferCand = multicolorHand && ComputerUtilMana.isAnyMultiManaProducer(cand)
                    && preferMultiForGeneric;
            final boolean preferBest = multicolorHand && best != null && ComputerUtilMana.isAnyMultiManaProducer(best)
                    && preferMultiForGeneric;
            boolean takeCand = false;
            if (castable > bestCastable) {
                takeCand = true;
                bestEfficiency = Integer.MAX_VALUE;
            } else if (castable == bestCastable) {
                if (preferCand && !preferBest) {
                    takeCand = true;
                    bestEfficiency = Integer.MAX_VALUE;
                } else if (!preferCand && !preferBest) {
                    final ComputerUtilMana.PaymentImpact impact = ComputerUtilMana.evaluatePaymentImpact(cost, sa, ai,
                            toPay, cand, candidates, probeCtx);
                    final int candEfficiency = impact.efficiencyScore;
                    if (best == null) {
                        takeCand = true;
                        bestEfficiency = candEfficiency;
                    } else {
                        if (bestEfficiency == Integer.MAX_VALUE) {
                            bestEfficiency = ComputerUtilMana.evaluatePaymentImpact(cost, sa, ai, toPay, best,
                                    candidates, probeCtx).efficiencyScore;
                        }
                        if (tieBreakPrefers(cand, best, candEfficiency, bestEfficiency, toPay, cost, ai, sa)) {
                            takeCand = true;
                            bestEfficiency = candEfficiency;
                        }
                    }
                }
            }
            if (takeCand) {
                bestCastable = castable;
                best = cand;
            }
        }
        return best;
    }

    /** Record a colored-shard failure with zero candidates during a castability nested dry-run. */
    static void recordNoSourceColoredShardFailure(final ComputerUtilMana.ManaPaymentContext ctx,
            final ManaCostShard toPay, final Collection<SpellAbility> saList) {
        if (ctx == null || ctx.caches.castabilityProbe.availableManaAfterReservation < 0
                || toPay == null || toPay.isGeneric() || toPay.isPhyrexian()
                || toPay == ManaCostShard.COLORLESS || toPay == ManaCostShard.X
                || toPay == ManaCostShard.COLORED_X || saList == null || !saList.isEmpty()) {
            return;
        }
        ctx.caches.castabilityProbe.lastFailedColoredShard = toPay;
        ctx.caches.castabilityProbe.lastFailureWasNoSources = true;
    }

    /** Test hook: reset nested castability dry-run counter. */
    public static void resetDryRunCountForTests() {
        dryRunCountForTests = 0;
    }

    /** Test hook: nested castability dry-runs since last reset. */
    public static int getDryRunCountForTests() {
        return dryRunCountForTests;
    }

    static int countCastableSpellsAfterPayment(final Player ai, final SpellAbility spellBeingPaid,
            final Set<Card> consumed, final ComputerUtilMana.ManaPaymentContext ctx) {
        final Set<Card> reserved = new HashSet<>(consumed);
        final Set<Card> tapCost = AiCardMemory.getMemorySet(ai, MemorySet.PAYS_TAP_COST);
        if (tapCost != null) {
            reserved.addAll(tapCost);
        }
        final Set<Card> sacCost = AiCardMemory.getMemorySet(ai, MemorySet.PAYS_SAC_COST);
        if (sacCost != null) {
            reserved.addAll(sacCost);
        }
        final ComputerUtilMana.CastabilityProbeScratch probe = ctx.caches.castabilityProbe;
        probe.resetForProbe();
        probe.unavailableColoredShards.addAll(computeUnavailableColoredShards(ai, reserved, ctx));
        probe.availableManaAfterReservation = computeAvailableManaAfterReservation(ai, reserved, ctx);
        int count = countCastableSpellsInZone(ai, spellBeingPaid, reserved, ZoneType.Hand, ctx);
        count += countCastableSpellsInZone(ai, spellBeingPaid, reserved, ZoneType.Command, ctx);
        probe.resetForProbe();
        return count;
    }

    private static List<SpellAbility> capCandidates(final List<SpellAbility> candidates) {
        if (candidates.size() <= CANDIDATE_CAP) {
            return candidates;
        }
        return candidates.subList(0, CANDIDATE_CAP);
    }

    private static int countCastableSpellsInZone(final Player ai, final SpellAbility spellBeingPaid,
            final Set<Card> consumed, final ZoneType zone, final ComputerUtilMana.ManaPaymentContext ctx) {
        int count = 0;
        final Card being = spellBeingPaid.getHostCard();
        final ComputerUtilMana.CastabilityProbeScratch probe = ctx.caches.castabilityProbe;
        for (Card c : ai.getCardsIn(zone)) {
            if (c == being) {
                continue;
            }
            for (SpellAbility candSa : c.getSpellAbilities()) {
                if (!candSa.isSpell() || candSa.getPayCosts() == null || !candSa.getPayCosts().hasManaCost()) {
                    continue;
                }
                candSa.setActivatingPlayer(ai);
                if (isUncastableByTotalManaBudget(candSa, probe.availableManaAfterReservation)) {
                    continue;
                }
                final CostPartMana costMana = candSa.getPayCosts().getCostMana();
                if (costMana == null) {
                    continue;
                }
                final ManaCost mc = costMana.getMana();
                if (spellRequiresUnavailableColoredShard(mc, probe.unavailableColoredShards)) {
                    continue;
                }
                if (canPayManaCostExcluding(candSa, ai, consumed, ctx)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean spellRequiresUnavailableColoredShard(final ManaCost mc,
            final Set<ManaCostShard> unavailableColoredShards) {
        if (mc == null || unavailableColoredShards == null || unavailableColoredShards.isEmpty()) {
            return false;
        }
        final ManaCostBeingPaid probe = new ManaCostBeingPaid(mc);
        for (final ManaCostShard shard : probe.getDistinctShards()) {
            if (shard.isGeneric() || shard == ManaCostShard.COLORLESS || shard.isPhyrexian()
                    || shard == ManaCostShard.X || shard == ManaCostShard.COLORED_X) {
                continue;
            }
            if (unavailableColoredShards.contains(shard)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUncastableByTotalManaBudget(final SpellAbility candSa, final int availableMana) {
        if (availableMana < 0 || candSa == null || candSa.getPayCosts() == null
                || !candSa.getPayCosts().hasManaCost()) {
            return false;
        }
        final int cmc = candSa.getPayCosts().getCostMana().getMana().getCMC();
        return cmc > SOFT_CMC_CAP && cmc > availableMana;
    }

    private static Set<ManaCostShard> computeUnavailableColoredShards(final Player ai, final Set<Card> reserved,
            final ComputerUtilMana.ManaPaymentContext ctx) {
        final Set<ManaCostShard> unavailable = new HashSet<>();
        final ListMultimap<Integer, SpellAbility> map = ComputerUtilMana.getOrBuildManaAbilityMap(ai, true, ctx);
        for (final ManaCostShard shard : ManaCostShard.values()) {
            if (shard.isGeneric() || shard == ManaCostShard.COLORLESS || shard.isPhyrexian()
                    || shard == ManaCostShard.X || shard == ManaCostShard.COLORED_X) {
                continue;
            }
            if (!hasAvailableProducerForShard(ai, map, shard, reserved)) {
                unavailable.add(shard);
            }
        }
        return unavailable;
    }

    private static boolean hasAvailableProducerForShard(final Player ai,
            final ListMultimap<Integer, SpellAbility> manaAbilityMap, final ManaCostShard shard,
            final Set<Card> reserved) {
        for (final byte color : ManaAtom.MANATYPES) {
            if (!shard.canBePaidWithManaOfColor(color)) {
                continue;
            }
            for (final SpellAbility candidate : manaAbilityMap.get((int) color)) {
                if (isManaSourceAvailableAfterReservation(ai, candidate, reserved)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isManaSourceAvailableAfterReservation(final Player ai, final SpellAbility ma,
            final Set<Card> reserved) {
        if (ma == null || ai == null) {
            return false;
        }
        final Card host = ma.getHostCard();
        if (host == null || reserved.contains(host)) {
            return false;
        }
        if (AiCardMemory.isRememberedCard(ai, host, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
            return false;
        }
        if (AiCardMemory.isRememberedCard(ai, host, MemorySet.PAYS_SAC_COST)) {
            return false;
        }
        if (ma.getPayCosts() != null && ma.getPayCosts().hasTapCost()) {
            if (AiCardMemory.isRememberedCard(ai, host, MemorySet.PAYS_TAP_COST) || host.isTapped()) {
                return false;
            }
        }
        ma.setActivatingPlayer(ai);
        return ma.canPlay();
    }

    private static int computeAvailableManaAfterReservation(final Player ai, final Set<Card> reserved,
            final ComputerUtilMana.ManaPaymentContext ctx) {
        int available = ai.getManaPool().totalMana();
        final ListMultimap<Integer, SpellAbility> map = ComputerUtilMana.getOrBuildManaAbilityMap(ai, true, ctx);
        final Set<Card> seenHosts = new HashSet<>();
        for (final SpellAbility ma : map.values()) {
            final Card host = ma.getHostCard();
            if (host == null || seenHosts.contains(host)) {
                continue;
            }
            if (!isManaSourceAvailableAfterReservation(ai, ma, reserved)) {
                continue;
            }
            int maxForHost = 0;
            for (final SpellAbility ma2 : host.getManaAbilities()) {
                if (!isManaSourceAvailableAfterReservation(ai, ma2, reserved)) {
                    continue;
                }
                ma2.setActivatingPlayer(ai);
                if (!ma2.canPlay()) {
                    continue;
                }
                maxForHost = Math.max(maxForHost, ma2.amountOfManaGenerated(true));
            }
            seenHosts.add(host);
            available += maxForHost;
        }
        return available;
    }

    private static boolean canPayManaCostExcluding(final SpellAbility candSa, final Player ai, final Set<Card> consumed,
            final ComputerUtilMana.ManaPaymentContext ctx) {
        final ComputerUtilMana.CastabilityProbeScratch probe = ctx.caches.castabilityProbe;
        probe.clearLastFailure();
        final List<Card> reserved = new ArrayList<>();
        for (Card c : consumed) {
            if (!AiCardMemory.isRememberedCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
                AiCardMemory.rememberCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
                reserved.add(c);
            }
        }
        final Set<Card> sacSnapshot = ComputerUtilMana.snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
        final Set<Card> tapSnapshot = ComputerUtilMana.snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
        try {
            dryRunCountForTests++;
            final boolean result = ComputerUtilMana.payManaCostForCastabilityProbe(candSa.getPayCosts(), candSa, ai,
                    ctx);
            if (!result && probe.lastFailureWasNoSources && probe.lastFailedColoredShard != null) {
                probe.unavailableColoredShards.add(probe.lastFailedColoredShard);
            }
            return result;
        } finally {
            probe.clearLastFailure();
            for (Card c : reserved) {
                AiCardMemory.forgetCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
            }
            ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            ComputerUtilMana.restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
        }
    }

    private static boolean tieBreakPrefers(final SpellAbility cand, final SpellAbility best,
            final int candEfficiency, final int bestEfficiency, final ManaCostShard toPay,
            final ManaCostBeingPaid cost, final Player ai, final SpellAbility sa) {
        if (candEfficiency < bestEfficiency) {
            return true;
        }
        if (candEfficiency > bestEfficiency || best == null) {
            return false;
        }
        if (toPay != ManaCostShard.GENERIC && toPay != ManaCostShard.X) {
            return false;
        }
        final ComputerUtilMana.GenericColorPreference pref = ComputerUtilMana
                .genericColorPreferenceForNestedActivation(ai, sa, cost);
        return ComputerUtilMana.rankGenericManaSource(cand, pref)
                < ComputerUtilMana.rankGenericManaSource(best, pref);
    }
}
