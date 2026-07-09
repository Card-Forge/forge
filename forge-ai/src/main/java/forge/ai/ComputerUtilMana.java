package forge.ai;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import forge.ai.AiCardMemory.MemorySet;
import forge.ai.ability.AnimateAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ComputerUtilMana {
    private final static boolean DEBUG_MANA_PAYMENT = false;

    // Score penalty applied to a filter (mana ability with a mana activation cost) so plain lands
    // are preferred for single colored shards. Overridden by a consolidation bonus when the filter covers
    // two or more of the unpaid colored shards (see sortManaAbilities).
    private final static int FILTER_SINGLE_SHARD_PENALTY = 8;
    private final static int FILTER_CONSOLIDATION_BONUS = 20;
    // Sacrifice / one-shot mana (Lotus Petal) should lose to a consolidating signet when both can pay a colored pip.
    private final static int DISPOSABLE_MANA_PENALTY = 30;
    /** Index of colorless ({C}) pips in {@link AiDeckStatistics#maxPips} (WUBRGC order). */
    private final static int COLORLESS_PIP_INDEX = 5;

    // Guards against re-entering the castability-aware source selection while it is itself probing hand
    // spells via canPayManaCost (which can hit another filter). When set, filter activation just uses the
    // first free candidate to keep planning bounded.
    private static final ThreadLocal<Boolean> inFilterActivationProbe = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Nesting depth for payManaCost. Depth > 1 means a feasibility probe spawned from inside another
    // payment (e.g. castability checks while paying Calix). Probes must not clear shared tap memory or
    // emit top-level payment traces that look like separate spell checks.
    private static final ThreadLocal<Integer> manaPaymentDepth = ThreadLocal.withInitial(() -> 0);

    /** Per outer payment plan: cache dry-run nested activation taps (Study Hall {1}, etc.). */
    private static final ThreadLocal<Map<Long, CardCollection>> nestedActivationTapCache = ThreadLocal.withInitial(HashMap::new);
    /** Sentinel stored in {@link #nestedActivationTapCache} when nested activation is unpayable. */
    private static final CardCollection NESTED_ACTIVATION_FAILED = new CardCollection();

    // Runtime-toggleable payment tracing, enabled with -Dforge.debugManaPayment=true.
    // By default only test-mode (feasibility/planning) runs are traced so production Auto stays quiet;
    // set -Dforge.debugManaPayment.testOnly=false to also trace actual payment.
    private static boolean debugManaPayment(boolean test) {
        return Boolean.getBoolean("forge.debugManaPayment")
                && (!Boolean.parseBoolean(System.getProperty("forge.debugManaPayment.testOnly", "true")) || test);
    }

    private static void debugLog(boolean test, String msg) {
        if (debugManaPayment(test)) {
            System.out.println("MANA_PAYMENT [" + (test ? "test" : "prod") + "] " + msg);
        }
    }

    /** Payment trace for the outer spell only; nested feasibility probes stay silent. */
    private static void debugLogMain(boolean test, String msg) {
        if (manaPaymentDepth.get() <= 1) {
            debugLog(test, msg);
        }
    }

    /** Nested activation dry-runs: only log during the outer payment plan. */
    private static void debugLogNested(boolean test, String msg) {
        if (manaPaymentDepth.get() <= 1) {
            debugLog(test, msg);
        }
    }

    /** Spell name for payment traces; non-hand zones are tagged so commanders/planeswalkers are obvious. */
    private static String manaPaymentSpellLabel(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder label = new StringBuilder(host.getName());
        if (host.isInZone(ZoneType.Command)) {
            label.append(" [command]");
        } else if (host.isInZone(ZoneType.Stack)) {
            label.append(" [stack]");
        }
        return label.toString();
    }

    private static void clearManaPaymentPlanCache() {
        nestedActivationTapCache.get().clear();
    }

    /** Fingerprint reserved / tapped mana sources so nested activation dry-runs can be cached per plan. */
    private static long manaSourceReservationKey(final Player ai, final Card filterHost) {
        long key = filterHost == null ? 0 : filterHost.getId();
        key = key * 31 + fingerprintMemorySet(ai, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
        key = key * 31 + fingerprintMemorySet(ai, MemorySet.PAYS_TAP_COST);
        key = key * 31 + fingerprintMemorySet(ai, MemorySet.PAYS_SAC_COST);
        return key;
    }

    private static long fingerprintMemorySet(final Player ai, final MemorySet set) {
        final Set<Card> cards = AiCardMemory.getMemorySet(ai, set);
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        long fp = 1;
        for (final Card c : cards) {
            fp = fp * 31 + c.getId();
        }
        return fp;
    }

    public static boolean canPayManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        //check copy of cost so it doesn't modify the exist cost being paid
        cost = new ManaCostBeingPaid(cost);
        return payManaCost(cost, sa, ai, true, true, effect, null);
    }
    public static boolean canPayManaCost(final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        return canPayManaCost(sa.getPayCosts(), sa, ai, extraMana, effect);
    }
    public static boolean canPayManaCost(final Cost cost, final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        return payManaCost(cost, sa, ai, true, extraMana, true, effect);
    }

    public static boolean payManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        return payManaCost(cost, sa, ai, false, true, effect, null);
    }
    public static boolean payManaCost(final Cost cost, final Player ai, final SpellAbility sa, final boolean effect) {
        return payManaCost(cost, sa, ai, false, 0, true, effect);
    }
    private static boolean payManaCost(final Cost cost, final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable, final boolean effect) {
        ManaCostBeingPaid manaCost = calculateManaCost(cost, sa, ai, test, extraMana, effect);
        return payManaCost(manaCost, sa, ai, test, checkPlayable, effect, null);
    }

    /**
     * Return the number of colors used for payment for Converge
     */
    public static int getConvergeCount(final SpellAbility sa, final Player ai) {
        ManaCostBeingPaid cost = calculateManaCost(sa.getPayCosts(), sa, ai, true, 0, false);
        if (payManaCost(cost, sa, ai, true, true, false, null)) {
            return cost.getSunburst();
        }
        return 0;
    }

    // Does not check if mana sources can be used right now, just checks for potential chance.
    public static boolean hasEnoughManaSourcesToCast(final SpellAbility sa, final Player ai) {
        if (ai == null || sa == null)
            return false;
        sa.setActivatingPlayer(ai);
        return payManaCost(sa.getPayCosts(), sa, ai, true, 0, false, false);
    }

    public static CardCollection getManaSourcesToPayCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        final List<Mana> payment = payManaCost(cost, sa, ai, true, true, effect);
        if (payment == null) {
            return null;
        }
        return new CardCollection(payment.stream().map(Mana::getSourceCard).filter(Objects::nonNull));
    }

    private static Integer scoreManaProducingCard(final Card card) {
        int score = 0;

        int maxManaProduced = 0;
        boolean hasManaCostAbility = false;
        for (SpellAbility ability : card.getSpellAbilities()) {
            ability.setActivatingPlayer(card.getController());
            if (ability.isManaAbility()) {
                score += ability.calculateScoreForManaAbility();
                maxManaProduced = Math.max(maxManaProduced, ability.amountOfManaGenerated(true));
                if (ability.getPayCosts() != null && ability.getPayCosts().hasManaCost()) {
                    hasManaCostAbility = true;
                }
                // TODO check TriggersWhenSpent: decrease score depending on context
            }
            else if (!ability.isTrigger() && ability.isPossible()) {
                score += 13; //add 13 for any non-mana activated abilities
            }
        }

        if (card.isCreature()) {
            // Rank mana sources by mana per tap, not card type alone. A creature that makes several
            // mana is as efficient as several lands, so waive the combat "keep me back" penalty for the most
            // efficient dorks (3+ mana) and halve it for 2-mana producers.
            int combatPenalty = 0;
            if (CombatUtil.canAttack(card)) {
                combatPenalty += 13;
            }
            if (CombatUtil.canBlock(card)) {
                combatPenalty += 13;
            }
            if (maxManaProduced >= 3) {
                combatPenalty = 0;
            } else if (maxManaProduced == 2) {
                combatPenalty /= 2;
            }
            score += combatPenalty;
        }

        // Deprioritize filters slightly by default so plain lands win the single-shard case.
        // Multi-shard consolidation is handled separately in sortManaAbilities where the cost is known.
        if (hasManaCostAbility) {
            score += FILTER_SINGLE_SHARD_PENALTY;
        }

        if (isDisposableManaCard(card)) {
            score += DISPOSABLE_MANA_PENALTY;
        }

        return score;
    }

    /** True for sacrifice or other one-shot mana abilities the AI should not spend before a reusable filter. */
    private static boolean isDisposableManaAbility(final SpellAbility ma) {
        if (ma == null || !ma.isManaAbility()) {
            return false;
        }
        if (!ma.isUndoable()) {
            return true;
        }
        final Cost payCosts = ma.getPayCosts();
        if (payCosts == null) {
            return false;
        }
        for (final CostPart part : payCosts.getCostParts()) {
            if (part instanceof CostSacrifice) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDisposableManaCard(final Card card) {
        for (final SpellAbility ma : card.getManaAbilities()) {
            if (isDisposableManaAbility(ma)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A source with no mana activation cost that may pay a filter's nested generic cost ({1}, etc.).
     * Includes sacrifice rocks; {@link #sortFreeSourcesForNestedActivation} ranks them below tap sources.
     */
    private static boolean isFreeManaSourceForNestedActivation(final SpellAbility ma, final Card filterHost) {
        if (ma == null || ma.getHostCard() == filterHost) {
            return false;
        }
        final Cost payCosts = ma.getPayCosts();
        return payCosts == null || !payCosts.hasManaCost();
    }

    /** True when this mana ability produces only colorless mana (e.g. Mind Stone, Wastes). */
    private static boolean producesOnlyColorless(final SpellAbility ma) {
        final AbilityManaPart mp = ma.getManaPart();
        if (mp == null || mp.isAnyMana() || mp.isComboMana()) {
            return false;
        }
        return "C".equals(mp.mana(ma).trim());
    }

    /**
     * True when a free (no mana activation cost), reusable ability natively produces this colored shard
     * (e.g. Plains for {W}, Forest for {G}). Excludes any-mana and one-shot sources.
     */
    private static boolean producesShardDirectly(final SpellAbility ma, final ManaCostShard shard) {
        if (ma == null || shard.isGeneric() || shard == ManaCostShard.COLORLESS || shard.isPhyrexian()) {
            return false;
        }
        if (isDisposableManaAbility(ma)) {
            return false;
        }
        final Cost payCosts = ma.getPayCosts();
        if (payCosts != null && payCosts.hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = ma.getManaPart();
        if (mp == null || mp.isAnyMana() || mp.isComboMana()) {
            return false;
        }
        return mp.mana(ma).contains(shard.toShortString());
    }

    /**
     * True for reusable, free sources that can pay this colored shard: dedicated lands, Arcane Signet
     * (commander color identity), etc. Excludes one-shot mana and any-mana filters (Study Hall {@code {1},{T}:any}).
     */
    private static boolean isReusableFreeManaForShard(final SpellAbility ma, final ManaCostShard shard) {
        if (producesShardDirectly(ma, shard)) {
            return true;
        }
        if (ma == null || shard.isGeneric() || shard == ManaCostShard.COLORLESS || shard.isPhyrexian()) {
            return false;
        }
        if (isDisposableManaAbility(ma) || isAnyManaConsolidatingFilter(ma)) {
            return false;
        }
        final Cost payCosts = ma.getPayCosts();
        if (payCosts != null && payCosts.hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = ma.getManaPart();
        if (mp == null) {
            return false;
        }
        return mp.canProduce(shard.toShortString(), ma);
    }

    /**
     * Preference rank for paying a generic mana pip. Lower is better.
     * By default colorless carries generic costs so colored / any-mana sources stay available for colored pips.
     * When the hand or command zone still needs dedicated {C} pips, colored sources are preferred instead.
     */
    private static int rankGenericManaSource(final SpellAbility ma, final boolean reserveColorless) {
        if (isDisposableManaAbility(ma)) {
            return 50;
        }
        final Cost payCosts = ma.getPayCosts();
        final boolean hasManaCost = payCosts != null && payCosts.hasManaCost();
        if (producesOnlyColorless(ma) && !hasManaCost) {
            // A land's {T}:{C} mode is worse than tapping a colored basic for generic when {C} is needed.
            return ma.getHostCard().isLand() ? (reserveColorless ? 30 : 15) : (reserveColorless ? 30 : 0);
        }
        if (hasManaCost) {
            return 40;
        }
        final AbilityManaPart mp = ma.getManaPart();
        if (mp != null && mp.isAnyMana()) {
            return reserveColorless ? 10 : 20;
        }
        if (mp != null && mp.isComboMana() && !producesOnlyColorless(ma)) {
            // Arcane Signet and similar: save colored mana for colored pips, like unrestricted any-mana rocks.
            return reserveColorless ? 10 : 20;
        }
        return reserveColorless ? 0 : 10;
    }

    /**
     * True when other castable cards in hand or command zone need dedicated {@code {C}} pips, so colorless rocks
     * should be saved for those costs rather than spent on generic mana.
     */
    private static boolean shouldReserveColorlessMana(final Player ai, final SpellAbility sa) {
        if (ai == null) {
            return false;
        }
        final CardCollection remaining = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        remaining.addAll(ai.getCardsIn(ZoneType.Command));
        remaining.remove(sa.getHostCard());
        return AiDeckStatistics.fromCards(remaining).maxPips[COLORLESS_PIP_INDEX] > 0;
    }

    /** Reusable sources first; sacrifice / one-shot mana remains available as a fallback. */
    private static void sortFreeSourcesForNestedActivation(final List<SpellAbility> candidates,
            final ManaCostShard toPay, final boolean reserveColorless) {
        candidates.sort((a1, a2) -> {
            if (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X) {
                final boolean c1 = producesOnlyColorless(a1);
                final boolean c2 = producesOnlyColorless(a2);
                if (c1 != c2) {
                    return reserveColorless ? (c1 ? 1 : -1) : (c1 ? -1 : 1);
                }
            }
            final boolean d1 = isDisposableManaAbility(a1);
            final boolean d2 = isDisposableManaAbility(a2);
            if (d1 != d2) {
                return d1 ? 1 : -1;
            }
            return 0;
        });
    }

    private static int getComboManaAmount(final SpellAbility ability) {
        if (ability == null) {
            return 0;
        }
        final AbilityManaPart mp = ability.getManaPart();
        if (mp == null || !mp.isComboMana()) {
            return 0;
        }
        return ability.hasParam("Amount")
                ? AbilityUtils.calculateAmount(ability.getHostCard(), ability.getParam("Amount"), ability) : 1;
    }

    /**
     * True when one activation produces two or more combo mana (e.g. Cascade Bluffs, Starlight Cairn
     * Metalcraft {@code {T}: Add three mana in any combination of colors}).
     */
    private static boolean isMultiManaComboAbility(final SpellAbility ability) {
        return getComboManaAmount(ability) >= 2;
    }

    /**
     * True when one activation of this combo filter produces two or more mana (e.g. Cascade Bluffs
     * {@code {U/R},{T}: Add {U}{U}, {U}{R}, or {R}{R}}).
     */
    private static boolean isComboConsolidatingFilter(final SpellAbility ability) {
        if (!isMultiManaComboAbility(ability)) {
            return false;
        }
        final Cost payCosts = ability.getPayCosts();
        return payCosts != null && payCosts.hasManaCost();
    }

    /**
     * True when one activation of this filter produces multiple colored mana at once (e.g. Boros Signet
     * {@code Produced$ R W}). {@code Produced$ Any} filters such as Study Hall add only one mana per
     * activation and must not receive the multi-shard consolidation bonus.
     */
    private static boolean isMultiShardConsolidatingFilter(final SpellAbility ability) {
        if (ability.getPayCosts() == null || !ability.getPayCosts().hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = ability.getManaPart();
        if (mp == null || mp.isAnyMana() || mp.isComboMana()) {
            return false;
        }
        return mp.mana(ability).split(" ").length >= 2;
    }

    /**
     * Any-mana filter with a mana activation cost (Study Hall), distinct from multi-mana signets that
     * produce two colored mana per activation.
     */
    private static boolean isAnyManaConsolidatingFilter(final SpellAbility ability) {
        if (ability.getPayCosts() == null || !ability.getPayCosts().hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = ability.getManaPart();
        return mp != null && mp.isAnyMana() && !isMultiShardConsolidatingFilter(ability)
                && !isComboConsolidatingFilter(ability);
    }

    /** Lower is better. Disposable sources are heavily penalized so signets beat Lotus Petal for colored pips. */
    private static int paymentEfficiencyScore(final SpellAbility chosen, final int consumedCount,
            final ManaCostBeingPaid cost, final ManaCostShard toPay, final List<SpellAbility> alternatives,
            final Player ai) {
        int score = consumedCount;
        if (isDisposableManaAbility(chosen)) {
            final boolean multiShardAlternative = alternatives.stream()
                    .anyMatch(ma -> ma != chosen && isMultiShardConsolidatingFilter(ma));
            if (multiShardAlternative || !disposableIsReasonableForShard(chosen, cost, toPay, alternatives, ai)) {
                score += 100;
            }
        }
        if (isAnyManaConsolidatingFilter(chosen) && cost.getGenericManaAmount() > 0 && !toPay.isGeneric()) {
            score += 50;
        }
        return score;
    }

    /**
     * Disposables are a last resort. Sacrificing a Petal/Treasure is only reasonable when no reusable
     * free producer exists for this shard and every any-mana filter alternative either cannot be
     * activated without a disposable or would strand the rest of the spell cost.
     */
    private static boolean disposableIsReasonableForShard(final SpellAbility disposable,
            final ManaCostBeingPaid cost, final ManaCostShard toPay, final List<SpellAbility> alternatives,
            final Player ai) {
        if (!isDisposableManaAbility(disposable) || toPay.isGeneric() || cost.getGenericManaAmount() == 0) {
            return false;
        }
        if (alternatives.stream().anyMatch(ma -> ma != disposable && isReusableFreeManaForShard(ma, toPay))) {
            return false;
        }
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, true);
        final List<SpellAbility> filterAlts = alternatives.stream()
                .filter(ma -> ma != disposable && isAnyManaConsolidatingFilter(ma))
                .collect(Collectors.toList());
        if (filterAlts.isEmpty()) {
            return false;
        }
        return filterAlts.stream().noneMatch(ma -> canActivateFilterWithoutDisposable(ma, manaAbilityMap)
                && !filterActivationCompetesForSpellGeneric(ma, cost, ai));
    }

    /**
     * True when paying this filter's activation would consume reusable generic mana sources needed
     * to pay the spell's own generic pips (e.g. one Plains cannot fund Study Hall's {@code {1}} and
     * the spell's {@code {1}}). {@code {1}} accepts any mana; {@code {C}} sources count too.
     */
    private static boolean filterActivationCompetesForSpellGeneric(final SpellAbility filter,
            final ManaCostBeingPaid cost, final Player ai) {
        if (cost.getGenericManaAmount() <= 0 || ai == null) {
            return false;
        }
        final CostPartMana costMana = filter.getPayCosts().getCostMana();
        if (costMana == null) {
            return false;
        }
        final int activationGeneric = costMana.getMana().getGenericCost();
        if (activationGeneric <= 0) {
            return false;
        }
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, true);
        final Card filterHost = filter.getHostCard();
        int reusableGenericSources = 0;
        for (final SpellAbility candidate : manaAbilityMap.get(ManaAtom.GENERIC)) {
            if (isFreeReusableSourceForNestedActivation(candidate, filterHost)) {
                reusableGenericSources++;
            }
        }
        return reusableGenericSources < cost.getGenericManaAmount() + activationGeneric;
    }

    /** Free nested-activation source that is not a one-shot (Petal, Treasure, etc.). */
    private static boolean isFreeReusableSourceForNestedActivation(final SpellAbility ma, final Card filterHost) {
        return isFreeManaSourceForNestedActivation(ma, filterHost) && !isDisposableManaAbility(ma);
    }

    /**
     * True when this any-mana filter's full activation cost ({@code {1}}, hybrid, etc.) can be paid by
     * reusable sources only. Generic {@code {1}} accepts any mana including colored taps; {@code {C}}
     * is colorless-only production.
     */
    private static boolean canActivateFilterWithoutDisposable(final SpellAbility filter,
            final ListMultimap<Integer, SpellAbility> manaAbilityMap) {
        if (!isAnyManaConsolidatingFilter(filter)) {
            return false;
        }
        final CostPartMana costMana = filter.getPayCosts().getCostMana();
        if (costMana == null) {
            return false;
        }
        final ManaCost activation = costMana.getManaCostFor(filter);
        final Card filterHost = filter.getHostCard();
        if (activation.getGenericCost() > 0
                && !hasFreeReusableSourceForGenericActivation(manaAbilityMap, filterHost)) {
            return false;
        }
        for (final ManaCostShard shard : activation) {
            if (shard.isGeneric() || shard == ManaCostShard.COLORLESS) {
                continue;
            }
            if (!hasFreeReusableSourceForShard(manaAbilityMap, shard, filterHost)) {
                return false;
            }
        }
        return true;
    }

    /** Generic {@code {1}} activation: any reusable tap (Plains, Reliquary {@code {C}}, etc.). */
    private static boolean hasFreeReusableSourceForGenericActivation(
            final ListMultimap<Integer, SpellAbility> manaAbilityMap, final Card filterHost) {
        for (final SpellAbility candidate : manaAbilityMap.get(ManaAtom.GENERIC)) {
            if (isFreeReusableSourceForNestedActivation(candidate, filterHost)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFreeReusableSourceForShard(
            final ListMultimap<Integer, SpellAbility> manaAbilityMap, final ManaCostShard shard,
            final Card filterHost) {
        for (final byte color : ManaAtom.MANATYPES) {
            if (!shard.canBePaidWithManaOfColor(color)) {
                continue;
            }
            for (final SpellAbility candidate : manaAbilityMap.get((int) color)) {
                if (isFreeReusableSourceForNestedActivation(candidate, filterHost)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * True when a filter's activation cost (generic {1} or hybrid {U/R}) can be paid by separate free
     * sources still available on the battlefield. Covers multi-mana signets (Boros Signet), any-mana
     * filters (Study Hall) and combo filter lands (Cascade Bluffs) so a reusable consolidation line
     * beats tapping several basics or sacrificing one-shot mana.
     */
    private static boolean canActivateFilterWithFreeSources(final SpellAbility filter,
            final ListMultimap<Integer, SpellAbility> manaAbilityMap) {
        if (filter.getPayCosts() == null || !filter.getPayCosts().hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = filter.getManaPart();
        if (mp == null) {
            return false;
        }
        if (!isMultiShardConsolidatingFilter(filter) && !isComboConsolidatingFilter(filter) && !mp.isAnyMana()) {
            return false;
        }
        final CostPartMana costMana = filter.getPayCosts().getCostMana();
        if (costMana == null) {
            return false;
        }
        final Card filterHost = filter.getHostCard();
        final ManaCost activation = costMana.getMana();
        if (activation.getGenericCost() > 0) {
            boolean found = false;
            for (final SpellAbility candidate : manaAbilityMap.get(ManaAtom.GENERIC)) {
                if (isFreeManaSourceForNestedActivation(candidate, filterHost)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        for (final ManaCostShard shard : activation) {
            if (!hasFreeSourceForShard(manaAbilityMap, shard, filterHost)) {
                return false;
            }
        }
        return true;
    }

    /** True when a free source (no mana activation cost, not the filter itself) can pay this shard. */
    private static boolean hasFreeSourceForShard(final ListMultimap<Integer, SpellAbility> manaAbilityMap,
            final ManaCostShard shard, final Card filterHost) {
        for (final byte color : ManaAtom.MANATYPES) {
            if (!shard.canBePaidWithManaOfColor(color)) {
                continue;
            }
            for (final SpellAbility candidate : manaAbilityMap.get((int) color)) {
                if (isFreeManaSourceForNestedActivation(candidate, filterHost)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void sortManaAbilities(final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards,
            final ListMultimap<Integer, SpellAbility> manaAbilityMap, final SpellAbility sa,
            final ManaCostBeingPaid cost, final Player ai) {
        final Map<Card, Integer> manaCardMap = Maps.newHashMap();
        final List<Card> orderedCards = Lists.newArrayList();
        final int unpaidGeneric = cost.getGenericManaAmount();
        final boolean reserveColorless = shouldReserveColorlessMana(ai, sa);
        int coloredShardCount = 0;
        for (final ManaCostShard shard : cost.getDistinctShards()) {
            if (!shard.isGeneric() && shard != ManaCostShard.COLORLESS && !shard.isPhyrexian()) {
                coloredShardCount += cost.getUnpaidShards(shard);
            }
        }
        final int unpaidColoredShards = coloredShardCount;

        // count the distinct colored shards each card can pay, so a filter covering 2+ can be prioritized
        final Map<Card, Set<ManaCostShard>> coloredShardsCovered = Maps.newHashMap();
        final Map<Card, Set<ManaCostShard>> comboShardsCovered = Maps.newHashMap();
        for (final ManaCostShard shard : sourcesForShards.keySet()) {
            for (SpellAbility ability : sourcesForShards.get(shard)) {
                final Card hostCard = ability.getHostCard();
                if (!manaCardMap.containsKey(hostCard)) {
                    // TODO +1 when reserved
                    manaCardMap.put(hostCard, scoreManaProducingCard(hostCard));
                    orderedCards.add(hostCard);
                }
                if (!shard.isGeneric()) {
                    if (isMultiShardConsolidatingFilter(ability)) {
                        coloredShardsCovered.computeIfAbsent(hostCard, k -> new HashSet<>()).add(shard);
                    } else if (isMultiManaComboAbility(ability)) {
                        comboShardsCovered.computeIfAbsent(hostCard, k -> new HashSet<>()).add(shard);
                    }
                }
            }
        }

        // Consolidation bonus: a filter that can pay 2+ unpaid colored shards in one activation
        // (e.g. Boros Signet -> {R}{W}) should beat tapping two separate basics.
        for (Map.Entry<Card, Set<ManaCostShard>> e : coloredShardsCovered.entrySet()) {
            if (e.getValue().size() >= 2) {
                manaCardMap.put(e.getKey(), manaCardMap.get(e.getKey()) - FILTER_CONSOLIDATION_BONUS);
            }
        }
        // Combo filters (Cascade Bluffs) choose their output, so unlike fixed signets they can cover two
        // pips of the SAME shard ({U}{U}) as well as two different ones ({U}{R}): count pips, not shards.
        for (Map.Entry<Card, Set<ManaCostShard>> e : comboShardsCovered.entrySet()) {
            if (coloredShardsCovered.containsKey(e.getKey())) {
                continue; // already received the bonus above
            }
            int coverablePips = 0;
            for (final ManaCostShard s : e.getValue()) {
                coverablePips += cost.getUnpaidShards(s);
            }
            if (coverablePips >= 2) {
                manaCardMap.put(e.getKey(), manaCardMap.get(e.getKey()) - FILTER_CONSOLIDATION_BONUS);
            }
        }

        // Consolidation bonus for multi-mana filters paying multiple generic pips at once
        // (e.g. Sungrass Prairie -> {G}{W} for {2}, with Study Hall {T} paying the {1} activation cost).
        if (unpaidGeneric >= 2 && sourcesForShards.containsKey(ManaCostShard.GENERIC)) {
            final Set<Card> genericConsolidators = new HashSet<>();
            for (SpellAbility ability : sourcesForShards.get(ManaCostShard.GENERIC)) {
                final boolean consolidates = (isMultiShardConsolidatingFilter(ability)
                        && canActivateFilterWithFreeSources(ability, manaAbilityMap))
                        || (isMultiManaComboAbility(ability) && !hasManaActivationCost(ability));
                if (consolidates && genericConsolidators.add(ability.getHostCard())) {
                    manaCardMap.put(ability.getHostCard(),
                            manaCardMap.get(ability.getHostCard()) - FILTER_CONSOLIDATION_BONUS);
                }
            }
        }

        // lower value means better choice
        orderedCards.sort(Comparator.comparingInt(manaCardMap::get));

        if (DEBUG_MANA_PAYMENT) {
            System.out.print("Ordered Cards: " + orderedCards.size());
            for (Card card : orderedCards) {
                System.out.print(card.getName() + ", ");
            }
            System.out.println();
        }

        List<Integer> colorsMostCommon;
        if (sourcesForShards.keySet().stream().anyMatch(ManaCostShard::isGeneric)) {
            // early tempo is more important so we only look at hand here
            final Player ap = sa.getActivatingPlayer();
            if (ap != null) {
                CardCollection hand = new CardCollection(ap.getCardsIn(ZoneType.Hand));
                hand.remove(sa.getHostCard());
                AiDeckStatistics stats = AiDeckStatistics.fromCards(hand);
                Integer[] orderedColorsIdx = {0, 1, 2, 3, 4};
                // order common colors to the front, increases chance AI can play a second spell after
                colorsMostCommon = Arrays.stream(orderedColorsIdx).sorted(Comparator.comparingInt(o -> stats.maxPips[(int) o]).reversed())
                        .filter(idx -> stats.maxPips[idx] > 0)
                        .map(idx -> (int) MagicColor.WUBRG[idx])
                        .collect(Collectors.toList());
            } else {
                colorsMostCommon = null;
            }
        } else {
            colorsMostCommon = null;
        }

        for (final ManaCostShard shard : sourcesForShards.keySet()) {
            final List<SpellAbility> abilities = sourcesForShards.get(shard);
            final List<SpellAbility> newAbilities = new ArrayList<>(abilities);

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Unsorted Abilities: " + newAbilities);
            }

            newAbilities.sort((ability1, ability2) -> {
                int preOrder = orderedCards.indexOf(ability1.getHostCard()) - orderedCards.indexOf(ability2.getHostCard());

                if (preOrder != 0) {
                    // Paying one colored shard usually prefers a free source over a filter on another card
                    // (e.g. Canopy Vista {W} over Study Hall {1}{T}: any). Exception: a real multi-mana
                    // filter can also cover an outstanding generic pip after a separate free source activates it
                    // (e.g. Lotus Petal -> Selesnya Signet -> {G}{W} pays {1}{W}).
                    if (shard.isGeneric()) {
                        if (unpaidGeneric >= 2) {
                            boolean ab1Consolidates = isMultiShardConsolidatingFilter(ability1)
                                    && canActivateFilterWithFreeSources(ability1, manaAbilityMap);
                            boolean ab2Consolidates = isMultiShardConsolidatingFilter(ability2)
                                    && canActivateFilterWithFreeSources(ability2, manaAbilityMap);
                            if (ab1Consolidates != ab2Consolidates) {
                                return ab1Consolidates ? -1 : 1;
                            }
                            // Multi-mana signets beat any-mana filters (Study Hall) when several generic pips remain.
                            boolean ab1Multi = isMultiShardConsolidatingFilter(ability1)
                                    && canActivateFilterWithFreeSources(ability1, manaAbilityMap);
                            boolean ab2Multi = isMultiShardConsolidatingFilter(ability2)
                                    && canActivateFilterWithFreeSources(ability2, manaAbilityMap);
                            boolean ab1Any = isAnyManaConsolidatingFilter(ability1)
                                    && canActivateFilterWithFreeSources(ability1, manaAbilityMap);
                            boolean ab2Any = isAnyManaConsolidatingFilter(ability2)
                                    && canActivateFilterWithFreeSources(ability2, manaAbilityMap);
                            if (ab1Multi && ab2Any) {
                                return -1;
                            }
                            if (ab2Multi && ab1Any) {
                                return 1;
                            }
                            if (ab1Any && !ab2Any && !producesOnlyColorless(ability2)) {
                                return -1;
                            }
                            if (ab2Any && !ab1Any && !producesOnlyColorless(ability1)) {
                                return 1;
                            }
                        }
                        final int genericRank1 = rankGenericManaSource(ability1, reserveColorless);
                        final int genericRank2 = rankGenericManaSource(ability2, reserveColorless);
                        if (genericRank1 != genericRank2) {
                            return genericRank1 - genericRank2;
                        }
                    } else if (!shard.isGeneric() && shard != ManaCostShard.COLORLESS) {
                        boolean ab1Filter = ability1.getPayCosts() != null && ability1.getPayCosts().hasManaCost();
                        boolean ab2Filter = ability2.getPayCosts() != null && ability2.getPayCosts().hasManaCost();
                        boolean ab1Consolidates = ab1Filter && canActivateFilterWithFreeSources(ability1, manaAbilityMap);
                        boolean ab2Consolidates = ab2Filter && canActivateFilterWithFreeSources(ability2, manaAbilityMap);
                        if (ab1Consolidates != ab2Consolidates) {
                            // Multi-shard filters consolidate colored pips (e.g. {R}{W} via Boros Signet,
                            // {U}{R} via Cascade Bluffs). Any-mana filters (Study Hall) only beat
                            // disposables — see next checks.
                            if (unpaidColoredShards >= 2
                                    && (isMultiShardConsolidatingFilter(ability1) || isMultiShardConsolidatingFilter(ability2)
                                            || isComboConsolidatingFilter(ability1) || isComboConsolidatingFilter(ability2))) {
                                return ab1Consolidates ? -1 : 1;
                            }
                        }
                        // Multi-mana signets beat disposables even with generic still unpaid (Signet -> {G}{W}
                        // preserves Lotus Petal for later). Any-mana filters (Study Hall) only win when generic
                        // is fully paid — see below.
                        if (ab1Consolidates && isMultiShardConsolidatingFilter(ability1)
                                && isDisposableManaAbility(ability2)) {
                            return -1;
                        }
                        if (ab2Consolidates && isMultiShardConsolidatingFilter(ability2)
                                && isDisposableManaAbility(ability1)) {
                            return 1;
                        }
                        if (ab1Consolidates && isComboConsolidatingFilter(ability1)
                                && isDisposableManaAbility(ability2)) {
                            return -1;
                        }
                        if (ab2Consolidates && isComboConsolidatingFilter(ability2)
                                && isDisposableManaAbility(ability1)) {
                            return 1;
                        }
                        if (ab1Filter != ab2Filter) {
                            // Pay Study Hall (etc.) first for WW so Plains stays for the second pip.
                            if (unpaidGeneric == 0 && unpaidColoredShards >= 2) {
                                if (ab1Consolidates && isAnyManaConsolidatingFilter(ability1) && !ab2Filter) {
                                    return -1;
                                }
                                if (ab2Consolidates && isAnyManaConsolidatingFilter(ability2) && !ab1Filter) {
                                    return 1;
                                }
                            }
                            // Lone colored pip: any-mana filter + activator beats disposable (keep the Petal).
                            if (unpaidGeneric == 0) {
                                if (ab1Consolidates && isAnyManaConsolidatingFilter(ability1)
                                        && isDisposableManaAbility(ability2)) {
                                    return -1;
                                }
                                if (ab2Consolidates && isAnyManaConsolidatingFilter(ability2)
                                        && isDisposableManaAbility(ability1)) {
                                    return 1;
                                }
                            } else if (ab1Consolidates && isAnyManaConsolidatingFilter(ability1)
                                    && isDisposableManaAbility(ability2)) {
                                if (canActivateFilterWithoutDisposable(ability1, manaAbilityMap)
                                        && !filterActivationCompetesForSpellGeneric(ability1, cost, ai)) {
                                    return -1;
                                }
                                return 1;
                            } else if (ab2Consolidates && isAnyManaConsolidatingFilter(ability2)
                                    && isDisposableManaAbility(ability1)) {
                                if (canActivateFilterWithoutDisposable(ability2, manaAbilityMap)
                                        && !filterActivationCompetesForSpellGeneric(ability2, cost, ai)) {
                                    return 1;
                                }
                                return -1;
                            }
                            return ab1Filter ? 1 : -1;
                        }
                        if (isDisposableManaAbility(ability1) != isDisposableManaAbility(ability2)) {
                            final boolean reuse1 = isReusableFreeManaForShard(ability1, shard);
                            final boolean reuse2 = isReusableFreeManaForShard(ability2, shard);
                            if (reuse1 != reuse2) {
                                return reuse1 ? -1 : 1;
                            }
                            final boolean d1 = isDisposableManaAbility(ability1);
                            final boolean d2 = isDisposableManaAbility(ability2);
                            final boolean f1 = ab1Consolidates && isAnyManaConsolidatingFilter(ability1);
                            final boolean f2 = ab2Consolidates && isAnyManaConsolidatingFilter(ability2);
                            if (d1 && f2) {
                                if (canActivateFilterWithoutDisposable(ability2, manaAbilityMap)
                                        && !filterActivationCompetesForSpellGeneric(ability2, cost, ai)) {
                                    return 1;
                                }
                                return unpaidGeneric > 0 ? -1 : 1;
                            }
                            if (d2 && f1) {
                                if (canActivateFilterWithoutDisposable(ability1, manaAbilityMap)
                                        && !filterActivationCompetesForSpellGeneric(ability1, cost, ai)) {
                                    return -1;
                                }
                                return unpaidGeneric > 0 ? 1 : -1;
                            }
                            // Disposables are last resort.
                            return d1 ? 1 : -1;
                        }
                    }

                    // on identical score (most likely basics) try keep access to more colors longer
                    if (shard.isGeneric() && manaCardMap.get(ability1.getHostCard()).equals(manaCardMap.get(ability2.getHostCard()))) {
                        final boolean colorless1 = producesOnlyColorless(ability1);
                        final boolean colorless2 = producesOnlyColorless(ability2);
                        if (colorless1 != colorless2) {
                            return reserveColorless ? (colorless1 ? 1 : -1) : (colorless1 ? -1 : 1);
                        }
                        if (colorsMostCommon != null) {
                            for (Integer col : colorsMostCommon) {
                                boolean fromCommonColorSource1 = manaAbilityMap.get(col).stream().anyMatch(ma -> ma.getHostCard().equals(ability1.getHostCard()));
                                boolean fromCommonColorSource2 = manaAbilityMap.get(col).stream().anyMatch(ma -> ma.getHostCard().equals(ability2.getHostCard()));
                                if (fromCommonColorSource1 && !fromCommonColorSource2) {
                                    return 1;
                                }
                                if (!fromCommonColorSource1 && fromCommonColorSource2) {
                                    return -1;
                                }
                            }
                        }
                    }

                    // sources were previously sorted, so add their index to connect those values to some degree
                    // This has been disabled because it makes the AI more likely to sacrifice lands than use creatures for mana
                    // preOrder += abilities.indexOf(ability1) - abilities.indexOf(ability2);

                    return preOrder;
                }

                // Mana abilities on the same card
                // Prefer multi-mana combo over {T}:{C} when several
                // pips of this shard remain; otherwise prefer the ability without a mana activation cost
                // (e.g. Painted Bluffs {T}:{C} over its {1}{T}: any mode).
                final int unpaidForShard = cost.getUnpaidShards(shard);
                if (unpaidForShard >= 2 || (shard.isGeneric() && unpaidGeneric >= 2)) {
                    final int combo1 = getComboManaAmount(ability1);
                    final int combo2 = getComboManaAmount(ability2);
                    if (combo1 >= 2 || combo2 >= 2) {
                        if (combo1 != combo2) {
                            return Integer.compare(combo2, combo1);
                        }
                        final boolean colorless1 = producesOnlyColorless(ability1);
                        final boolean colorless2 = producesOnlyColorless(ability2);
                        if (colorless1 != colorless2) {
                            return colorless1 ? 1 : -1;
                        }
                    }
                }
                boolean ab1HasManaCost = ability1.getPayCosts() != null && ability1.getPayCosts().hasManaCost();
                boolean ab2HasManaCost = ability2.getPayCosts() != null && ability2.getPayCosts().hasManaCost();
                if (ab1HasManaCost != ab2HasManaCost) {
                    // Study Hall {1},{T}:any + reusable activators beats {T}:{C} when several generic pips remain.
                    if (shard.isGeneric() && unpaidGeneric >= 2) {
                        if (isAnyManaConsolidatingFilter(ability1)
                                && canActivateFilterWithFreeSources(ability1, manaAbilityMap)) {
                            return -1;
                        }
                        if (isAnyManaConsolidatingFilter(ability2)
                                && canActivateFilterWithFreeSources(ability2, manaAbilityMap)) {
                            return 1;
                        }
                    }
                    return ab1HasManaCost ? 1 : -1;
                }

                String shardMana = shard.toShortString();

                boolean payWithAb1 = ability1.getManaPart().mana(ability1).contains(shardMana);
                boolean payWithAb2 = ability2.getManaPart().mana(ability2).contains(shardMana);

                if (payWithAb1 && !payWithAb2) {
                    return -1;
                } else if (payWithAb2 && !payWithAb1) {
                    return 1;
                }

                return ability1.compareTo(ability2);
            });

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Sorted Abilities: " + newAbilities);
            }

            sourcesForShards.replaceValues(shard, newAbilities);

            // Sort the first N abilities so that the preferred shard is selected, e.g. Adamant
            String manaPref = sa.getParamOrDefault("AIManaPref", "");
            if (manaPref.isEmpty() && sa.getHostCard() != null && sa.getHostCard().hasSVar("AIManaPref")) {
                manaPref = sa.getHostCard().getSVar("AIManaPref");
            }

            if (!manaPref.isEmpty()) {
                final String[] prefShardInfo = manaPref.split(":");
                final String preferredShard = prefShardInfo[0];
                final int preferredShardAmount = prefShardInfo.length > 1 ? Integer.parseInt(prefShardInfo[1]) : 3;

                if (!preferredShard.isEmpty()) {
                    final List<SpellAbility> prefSortedAbilities = new ArrayList<>(newAbilities);
                    final List<SpellAbility> otherSortedAbilities = new ArrayList<>(newAbilities);

                    prefSortedAbilities.sort((ability1, ability2) -> {
                        if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                            return -1;
                        else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                            return 1;

                        return 0;
                    });
                    otherSortedAbilities.sort((ability1, ability2) -> {
                        if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                            return 1;
                        else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                            return -1;

                        return 0;
                    });

                    final List<SpellAbility> finalAbilities = new ArrayList<>();
                    for (int i = 0; i < preferredShardAmount && i < prefSortedAbilities.size(); i++) {
                        finalAbilities.add(prefSortedAbilities.get(i));
                    }
                    for (SpellAbility ab : otherSortedAbilities) {
                        if (!finalAbilities.contains(ab))
                            finalAbilities.add(ab);
                    }

                    sourcesForShards.replaceValues(shard, finalAbilities);
                }
            }
        }
    }

    public static SpellAbility chooseManaAbility(ManaCostBeingPaid cost, SpellAbility sa, Player ai, ManaCostShard toPay,
            Collection<SpellAbility> maList, boolean checkCosts) {
        final List<SpellAbility> valid = collectValidManaPaymentChoices(cost, sa, ai, toPay, maList, checkCosts);
        return pickFirstReservedManaChoice(ai, sa, valid);
    }

    /**
     * Reservation checks ({@link ComputerUtilCost#checkForManaSacrificeCost},
     * {@link ComputerUtilCost#checkTapTypeCost}) have side effects and must run only for the ability
     * actually chosen, not while enumerating every candidate (see AutoPaymentTest).
     */
    private static boolean passesManaPaymentReservationChecks(final Player ai, final SpellAbility ma,
            final SpellAbility sa) {
        return ComputerUtilCost.checkForManaSacrificeCost(ai, ma.getPayCosts(), ma, ma.isTrigger())
                && ComputerUtilCost.checkTapTypeCost(ai, ma.getPayCosts(), ma.getHostCard(), sa,
                        AiCardMemory.getMemorySet(ai, MemorySet.PAYS_TAP_COST));
    }

    private static SpellAbility pickFirstReservedManaChoice(final Player ai, final SpellAbility sa,
            final List<SpellAbility> candidates) {
        for (final SpellAbility ma : candidates) {
            final Set<Card> sacSnapshot = snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
            final Set<Card> tapSnapshot = snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
            if (passesManaPaymentReservationChecks(ai, ma, sa)) {
                return ma;
            }
            restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
        }
        return null;
    }

    /**
     * Like {@link #chooseManaAbility} but, when a consolidating filter is in play, may compare
     * candidates by how many hand/command spells remain castable afterwards.
     */
    private static SpellAbility chooseManaAbilityForShard(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, Collection<SpellAbility> maList, final boolean checkCosts) {
        final List<SpellAbility> valid = collectValidManaPaymentChoices(cost, sa, ai, toPay, maList, checkCosts);
        if (valid.isEmpty()) {
            return null;
        }
        if (valid.size() == 1 || inFilterActivationProbe.get() || !shouldUseCastabilityProbeForFilterActivation(sa)
                || valid.stream().noneMatch(ma -> isMultiShardConsolidatingFilter(ma) || isMultiManaComboAbility(ma))
                || !hasOtherHandOrCommandSpells(ai, sa)) {
            return preferSourceThatKeepsRestPayable(cost, sa, ai, toPay, valid);
        }

        SpellAbility best = null;
        int bestCastable = -1;
        inFilterActivationProbe.set(Boolean.TRUE);
        try {
            for (final SpellAbility cand : valid) {
                final Set<Card> consumed = collectCardsConsumedByPayment(cand, sa, ai);
                if (consumed == null) {
                    continue;
                }
                final int castable = countCastableSpellsAfterPayment(ai, sa, consumed);
                debugLog(true, "  castability " + cand.getHostCard() + " -> " + castable + " hand/command spells remain");
                if (castable > bestCastable) {
                    bestCastable = castable;
                    best = cand;
                }
            }
        } finally {
            inFilterActivationProbe.set(Boolean.FALSE);
        }
        return best != null ? best : valid.get(0);
    }

    /**
     * Among equally-ranked candidates, avoid one that would strand the rest of THIS cost. Using a
     * filter with a mana activation cost taps an extra source, which can leave later pips of the same
     * payment unpayable (e.g. tapping Plains to activate Study Hall for {G} strands the {W} and {1} that also
     * needed Plains). Only reorders when the top choice has such a cost and a cheaper alternative keeps the
     * remaining cost payable; otherwise the existing order is preserved.
     */
    private static SpellAbility preferSourceThatKeepsRestPayable(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, final List<SpellAbility> valid) {
        // Nested feasibility probes follow sort order only; stranding / efficiency checks are for the
        // outer spell payment (depth 1) so we don't re-simulate every land on every recursive call.
        if (inFilterActivationProbe.get() || manaPaymentDepth.get() > 1) {
            final SpellAbility first = pickFirstReservedManaChoice(ai, sa, valid);
            return first == null ? null : refreshExpressChoice(cost, sa, ai, toPay, first);
        }

        final boolean preferEfficient = shouldPreferEfficientPayment(cost, toPay);
        SpellAbility first = null;
        SpellAbility best = null;
        int bestEfficiency = Integer.MAX_VALUE;

        for (final SpellAbility cand : valid) {
            if (preferEfficient && bestEfficiency <= 1) {
                break;
            }
            final Set<Card> sacSnapshot = snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
            final Set<Card> tapSnapshot = snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
            if (!passesManaPaymentReservationChecks(ai, cand, sa)) {
                restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
                restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
                continue;
            }
            if (first == null) {
                first = cand;
            }

            final PaymentImpact impact = evaluatePaymentImpact(cost, sa, ai, toPay, cand, valid);
            restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);

            if (!impact.keepsRest) {
                continue;
            }
            if (preferEfficient) {
                if (impact.efficiencyScore < bestEfficiency) {
                    bestEfficiency = impact.efficiencyScore;
                    best = cand;
                }
            } else if (best == null) {
                // Lone colored pip with no generic left: preserve disposables (Study Hall over Lotus Petal).
                best = cand;
                break;
            }
        }
        if (best != null) {
            return refreshExpressChoice(cost, sa, ai, toPay, best);
        }
        return first == null ? null : refreshExpressChoice(cost, sa, ai, toPay, first);
    }

    private static final class PaymentImpact {
        final boolean keepsRest;
        final int consumedCount;
        final int efficiencyScore;

        private PaymentImpact(final boolean keepsRest, final int consumedCount, final SpellAbility chosen,
                final ManaCostBeingPaid cost, final ManaCostShard toPay, final List<SpellAbility> alternatives,
                final Player ai) {
            this.keepsRest = keepsRest;
            this.consumedCount = consumedCount;
            this.efficiencyScore = paymentEfficiencyScore(chosen, consumedCount, cost, toPay, alternatives, ai);
        }
    }

    private static int effectiveCardsConsumedForPayment(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, final SpellAbility chosen, final Set<Card> consumed) {
        if (isMultiShardConsolidatingFilter(chosen)) {
            final ManaCostBeingPaid probe = new ManaCostBeingPaid(cost);
            payMultipleMana(probe, predictManafromSpellAbility(chosen, ai, toPay), ai);
            if (probe.isPaid()) {
                return 1;
            }
        } else if (isMultiManaComboAbility(chosen)) {
            final ManaCostBeingPaid probe = new ManaCostBeingPaid(cost);
            setComboManaChoice(ai, chosen, probe);
            try {
                payMultipleMana(probe,
                        capComboManaProduced(predictManafromSpellAbility(chosen, ai, toPay), getComboManaAmount(chosen)),
                        ai);
            } finally {
                chosen.getManaPart().clearExpressChoice();
            }
            if (probe.isPaid()) {
                return 1;
            }
        }
        return consumed.size();
    }

    /**
     * Single pass: cards consumed by paying with {@code chosen}, and whether the rest of {@code cost}
     * remains payable afterwards.
     */
    private static PaymentImpact evaluatePaymentImpact(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, final SpellAbility chosen,
            final List<SpellAbility> alternatives) {
        final Set<Card> consumed = collectCardsConsumedByPayment(chosen, sa, ai);
        if (consumed == null) {
            return new PaymentImpact(false, Integer.MAX_VALUE, chosen, cost, toPay, alternatives, ai);
        }
        final int consumedCount = effectiveCardsConsumedForPayment(cost, sa, ai, toPay, chosen, consumed);
        if (!hasRemainingCostAfterShard(cost, toPay)) {
            return new PaymentImpact(true, consumedCount, chosen, cost, toPay, alternatives, ai);
        }
        return new PaymentImpact(
                keepsRemainingCostPayableWithConsumed(cost, sa, ai, toPay, chosen, consumed),
                consumedCount, chosen, cost, toPay, alternatives, ai);
    }

    /**
     * Prefer the payment line that burns fewest sources when generic mana is still unpaid (colored pip can
     * use a disposable that would be spent anyway) or when paying a generic pip (direct tap beats a filter).
     */
    private static boolean shouldPreferEfficientPayment(final ManaCostBeingPaid cost, final ManaCostShard toPay) {
        if (toPay.isGeneric() || toPay == ManaCostShard.X) {
            return true;
        }
        return cost.getGenericManaAmount() > 0;
    }

    /**
     * The castability probe runs nested feasibility solves that can leave an "any color" source's express
     * color choice pointing at the wrong shard. Re-validate the finally-chosen source against {@code toPay}
     * so its express choice matches the pip it's about to pay.
     */
    private static SpellAbility refreshExpressChoice(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, final SpellAbility chosen) {
        final AbilityManaPart mp = chosen.getManaPart();
        if (mp != null && (mp.isAnyMana() || mp.isComboMana())) {
            canPayShardWithSpellAbility(toPay, ai, chosen, sa, cost, true, cost.getXManaCostPaidByColor());
        }
        return chosen;
    }

    /** True when the ability has a mana activation cost of its own (filter), so using it taps an extra source. */
    private static boolean hasManaActivationCost(final SpellAbility ma) {
        return ma.getPayCosts() != null && ma.getPayCosts().hasManaCost();
    }

    /** True when the cost still has unpaid shards other than a single copy of the one about to be paid. */
    private static boolean hasRemainingCostAfterShard(final ManaCostBeingPaid cost, final ManaCostShard toPay) {
        int distinct = 0;
        for (@SuppressWarnings("unused") final ManaCostShard s : cost.getDistinctShards()) {
            distinct++;
        }
        if (distinct > 1) {
            return true;
        }
        return cost.getUnpaidShards(toPay) > 1;
    }

    /**
     * Simulate paying {@code toPay} with {@code chosen} (reserving the cards it and any nested activation
     * consume) and check the remaining shards of {@code cost} are still payable from what's left.
     */
    private static boolean keepsRemainingCostPayable(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final ManaCostShard toPay, final SpellAbility chosen) {
        if (inFilterActivationProbe.get()) {
            return true;
        }
        final Set<Card> consumed = collectCardsConsumedByPayment(chosen, sa, ai);
        if (consumed == null) {
            return false;
        }
        return keepsRemainingCostPayableWithConsumed(cost, sa, ai, toPay, chosen, consumed);
    }

    private static boolean keepsRemainingCostPayableWithConsumed(final ManaCostBeingPaid cost,
            final SpellAbility sa, final Player ai, final ManaCostShard toPay, final SpellAbility chosen,
            final Set<Card> consumed) {
        if (inFilterActivationProbe.get()) {
            return true;
        }
        // chosen is a valid payer for toPay, so account for the mana it actually produces.
        final ManaCostBeingPaid remaining = new ManaCostBeingPaid(cost);
        if (isMultiShardConsolidatingFilter(chosen)) {
            payMultipleMana(remaining, predictManafromSpellAbility(chosen, ai, toPay), ai);
        } else if (isMultiManaComboAbility(chosen)) {
            setComboManaChoice(ai, chosen, remaining);
            try {
                payMultipleMana(remaining,
                        capComboManaProduced(predictManafromSpellAbility(chosen, ai, toPay), getComboManaAmount(chosen)),
                        ai);
            } finally {
                chosen.getManaPart().clearExpressChoice();
            }
        } else {
            remaining.decreaseShard(toPay, 1);
        }
        if (remaining.isPaid()) {
            return true;
        }
        final List<Card> reserved = new ArrayList<>();
        for (Card c : consumed) {
            if (!AiCardMemory.isRememberedCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
                AiCardMemory.rememberCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
                reserved.add(c);
            }
        }
        // The nested feasibility solve taps sources in test mode, which marks them PAYS_TAP_COST. That
        // memory must not leak into the real payment (it would hide those sources as already-tapped), so
        // snapshot it and restore afterwards.
        final Set<Card> tapMemorySnapshot = snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
        final Set<Card> sacMemorySnapshot = snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
        inFilterActivationProbe.set(Boolean.TRUE);
        try {
            return canPayManaCost(remaining, sa, ai, false);
        } finally {
            inFilterActivationProbe.set(Boolean.FALSE);
            for (Card c : reserved) {
                AiCardMemory.forgetCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
            }
            restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapMemorySnapshot);
            restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacMemorySnapshot);
        }
    }

    /** Copy the current contents of an AI card memory set (or null if unavailable). */
    private static Set<Card> snapshotMemory(final Player ai, final MemorySet set) {
        final Set<Card> live = AiCardMemory.getMemorySet(ai, set);
        return live == null ? null : new HashSet<>(live);
    }

    /** Reset an AI card memory set back to a previously captured snapshot. */
    private static void restoreMemory(final Player ai, final MemorySet set, final Set<Card> snapshot) {
        if (snapshot == null) {
            return;
        }
        AiCardMemory.clearMemorySet(ai, set);
        for (final Card c : snapshot) {
            AiCardMemory.rememberCard(ai, c, set);
        }
    }

    /** True when hand or command zone contains another spell besides the one being paid for. */
    private static boolean hasOtherHandOrCommandSpells(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        for (final ZoneType zone : new ZoneType[] { ZoneType.Hand, ZoneType.Command }) {
            for (Card c : ai.getCardsIn(zone)) {
                if (c == host) {
                    continue;
                }
                for (SpellAbility candSa : c.getSpellAbilities()) {
                    if (candSa.isSpell() && candSa.getPayCosts() != null && candSa.getPayCosts().hasManaCost()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Cards tapped (including nested activation costs) if this mana ability is chosen. */
    private static Set<Card> collectCardsConsumedByPayment(final SpellAbility saPayment, final SpellAbility sa,
            final Player ai) {
        final Set<Card> consumed = new HashSet<>();
        consumed.add(saPayment.getHostCard());
        if (saPayment.getPayCosts() != null && saPayment.getPayCosts().hasManaCost()) {
            final CardCollection nested = predictNestedActivationTaps(saPayment, sa, ai);
            if (nested == null) {
                return null;
            }
            consumed.addAll(nested);
        }
        return consumed;
    }

    /** Dry-run nested generic activation cost payment; returns tapped cards or null if unpayable. */
    private static CardCollection predictNestedActivationTaps(final SpellAbility filterAb,
            final SpellAbility sa, final Player ai) {
        final Card filterHost = filterAb.getHostCard();
        final long cacheKey = manaSourceReservationKey(ai, filterHost);
        final Map<Long, CardCollection> cache = nestedActivationTapCache.get();
        final CardCollection cached = cache.get(cacheKey);
        if (cached != null) {
            return cached == NESTED_ACTIVATION_FAILED ? null : new CardCollection(cached);
        }
        final CardCollection taps = new CardCollection();
        final Set<Card> sacSnapshot = snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
        final Set<Card> tapSnapshot = snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
        final List<Mana> probeMana = new ArrayList<>();
        try {
            if (!payNestedActivationCost(filterAb, sa, ai, ArrayListMultimap.create(), probeMana, true, false, taps)) {
                return null;
            }
            cache.put(cacheKey, new CardCollection(taps));
            return taps;
        } finally {
            ai.getManaPool().refundMana(probeMana);
            restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
        }
    }

    private static List<SpellAbility> collectValidManaPaymentChoices(final ManaCostBeingPaid cost,
            final SpellAbility sa, final Player ai, final ManaCostShard toPay,
            Collection<SpellAbility> maList, final boolean checkCosts) {
        Card saHost = sa.getHostCard();

        // When paying the activation cost of a filter (the ability being paid for is itself a mana
        // ability with a mana cost), only free sources may pay it — never another filter. Prevents filter chains.
        if (sa.isManaAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasManaCost()) {
            final List<SpellAbility> freeOnly = new ArrayList<>();
            for (SpellAbility ma : maList) {
                if (ma.getPayCosts() == null || !ma.getPayCosts().hasManaCost()) {
                    freeOnly.add(ma);
                }
            }
            maList = freeOnly;
        }

        // CastTotalManaSpent (AIPreference:ManaFrom$Type or AIManaPref$ Type)
        String manaSourceType = "";
        if (saHost.hasSVar("AIPreference")) {
            String condition = saHost.getSVar("AIPreference");
            if (condition.startsWith("ManaFrom")) {
                manaSourceType = TextUtil.split(condition, '$')[1];
            }
        } else if (sa.hasParam("AIManaPref")) {
            manaSourceType = sa.getParam("AIManaPref");
        }
        if (manaSourceType != "") {
            List<SpellAbility> filteredList = Lists.newArrayList(maList);
            switch (manaSourceType) {
                case "Snow":
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().isSnow()
                            && ab2.getHostCard() != null && !ab2.getHostCard().isSnow() ? -1 : 1);
                    maList = filteredList;
                    break;
                case "Treasure":
                    // Try to spend only one Treasure if possible
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                            && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1);
                    SpellAbility first = filteredList.get(0);
                    if (first.getHostCard() != null && first.getHostCard().getType().hasSubtype("Treasure")) {
                        maList.remove(first);
                        List<SpellAbility> updatedList = Lists.newArrayList();
                        updatedList.add(first);
                        updatedList.addAll(maList);
                        maList = updatedList;
                    }
                    break;
                case "TreasureMax":
                    // Ok to spend as many Treasures as possible
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                            && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1);
                    maList = filteredList;
                    break;
                case "NotSameCard":
                    String hostName = sa.getHostCard().getName();
                    maList = filteredList.stream()
                            .filter(saPay -> !saPay.getHostCard().getName().equals(hostName))
                            .collect(Collectors.toList());
                    break;
                default:
                    break;
            }
        }

        final List<SpellAbility> valid = new ArrayList<>();
        for (final SpellAbility ma : maList) {
            // this rarely seems like a good idea
            if (ma.getHostCard() == saHost) {
                continue;
            }

            if (ma.getPayCosts().hasTapCost() && AiCardMemory.isRememberedCard(ai, ma.getHostCard(), MemorySet.PAYS_TAP_COST)) {
                continue;
            }
            if (AiCardMemory.isRememberedCard(ai, ma.getHostCard(), MemorySet.PAYS_SAC_COST)) {
                continue;
            }

            int amount = ma.hasParam("Amount") ? AbilityUtils.calculateAmount(ma.getHostCard(), ma.getParam("Amount"), ma) : 1;
            if (amount <= 0) {
                // wrong gamestate for variable amount
                continue;
            }

            if (sa.getApi() == ApiType.Animate) {
                // For abilities like Genju of the Cedars, make sure that we're not activating the aura ability by tapping the enchanted card for mana
                if (saHost.isAura() && "Enchanted".equals(sa.getParam("Defined"))
                        && ma.getHostCard() == saHost.getEnchantingCard()
                        && ma.getPayCosts().hasTapCost()) {
                    continue;
                }

                // If a manland was previously animated this turn, do not tap it to animate another manland
                if (saHost.isLand() && ma.getHostCard().isLand()
                        && ai.getController().isAI()
                        && AnimateAi.isAnimatedThisTurn(ai, ma.getHostCard())) {
                    continue;
                }
            } else if (sa.getApi() == ApiType.Pump) {
                if ((saHost.isInstant() || saHost.isSorcery())
                        && ma.getHostCard().isCreature()
                        && ai.getController().isAI()
                        && ma.getPayCosts().hasTapCost()
                        && sa.getTargets().getTargetCards().contains(ma.getHostCard())) {
                    // do not activate pump instants/sorceries targeting creatures by tapping targeted
                    // creatures for mana (for example, Servant of the Conduit)
                    continue;
                }
            } else if (sa.getApi() == ApiType.Attach
                    && "AvoidPayingWithAttachTarget".equals(saHost.getSVar("AIPaymentPreference"))) {
                // For cards like Genju of the Cedars, make sure we're not attaching to the same land that will
                // be tapped to pay its own cost if there's another untapped land like that available
                if (ma.getHostCard().equals(sa.getTargetCard())) {
                    if (CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(ma.getHostCard().getName()).and(CardPredicates.UNTAPPED)) > 1) {
                        continue;
                    }
                }
            }

            SpellAbility paymentChoice = ma;

            // Exception: when paying generic mana with Cavern of Souls, prefer the colored mana producing ability
            // to attempt to make the spell uncounterable when possible.
            if (ComputerUtilAbility.getAbilitySourceName(ma).equals("Cavern of Souls")
                    && saHost.getType().hasCreatureType(ma.getHostCard().getChosenType())) {
                if (toPay == ManaCostShard.COLORLESS && cost.getUnpaidShards().contains(ManaCostShard.GENERIC)) {
                    // Deprioritize Cavern of Souls, try to pay generic mana with it instead to use the NoCounter ability
                    continue;
                } else if (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X) {
                    for (SpellAbility ab : maList) {
                        if (ab.isManaAbility() && ab.getManaPart().isAnyMana() && ab.hasParam("AddsNoCounter")) {
                            if (!ab.getHostCard().isTapped()) {
                                paymentChoice = ab;
                                break;
                            }
                        }
                    }
                }
            }

            if (!canPayShardWithSpellAbility(toPay, ai, paymentChoice, sa, cost, checkCosts, cost.getXManaCostPaidByColor())) {
                continue;
            }

            // Skip useless 1:1 filters (e.g. Initiates of the Ebon Hand: {1} -> {B}) when a direct,
            // free source for the same color is available. No net mana profit means the filter is wasteful.
            if (isUselessFilter(paymentChoice, toPay, maList, ai, cost)) {
                continue;
            }
            if (isWastefulSacLandForGeneric(paymentChoice, toPay, ai)) {
                continue;
            }

            valid.add(paymentChoice);
        }
        return valid;
    }

    /**
     * Skip useless filters when a direct source in the candidate pool can pay {@code toPay} without
     * routing through the filter. Covers 1:1 filters (Initiates {1} -> {B}) and any-mana filters
     * (Study Hall) when activation would burn a disposable that could pay the pip directly.
     */
    private static boolean isUselessFilter(final SpellAbility ma, final ManaCostShard toPay,
            final Collection<SpellAbility> maList, final Player ai, final ManaCostBeingPaid cost) {
        final Cost payCosts = ma.getPayCosts();
        if (payCosts == null || !payCosts.hasManaCost()) {
            return false;
        }
        final AbilityManaPart mp = ma.getManaPart();
        if (mp == null) {
            return false;
        }
        if (mp.isComboMana() || mp.mana(ma).split(" ").length > 1) {
            return false;
        }
        if (mp.isAnyMana()) {
            return isUselessAnyManaFilter(ma, maList, ai, cost, toPay);
        }
        final CostPartMana costMana = payCosts.getCostMana();
        final int activationCMC = costMana == null ? 0 : costMana.getMana().getCMC();
        // net mana profit means it's worth using (e.g. mana rocks that add more than they cost)
        if (ma.amountOfManaGenerated(true) > activationCMC) {
            return false;
        }
        // only skip when a free source in the same candidate pool can pay this shard directly
        for (SpellAbility other : maList) {
            if (other == ma || other.getHostCard() == ma.getHostCard()) {
                continue;
            }
            final Cost otherCost = other.getPayCosts();
            if (otherCost != null && !otherCost.hasManaCost()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Any-mana filters (Study Hall) are wasteful for a single pip when a dedicated reusable producer
     * exists, or when a disposable is the only way to pay and the filter's activation cannot be covered
     * by reusable sources alone. Otherwise keep the filter — a reusable activation line preserves the
     * disposable ({@code {1}} accepts any mana; hybrid entry costs need matching reusable taps).
     */
    private static boolean isUselessAnyManaFilter(final SpellAbility filter, final Collection<SpellAbility> maList,
            final Player ai, final ManaCostBeingPaid cost, final ManaCostShard toPay) {
        final Card filterHost = filter.getHostCard();
        if (maList.stream().anyMatch(other -> other != filter && other.getHostCard() != filterHost
                && isReusableFreeManaForShard(other, toPay))) {
            return true;
        }
        if (cost != null && cost.getGenericManaAmount() > 0 && !toPay.isGeneric() && ai != null) {
            final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, true);
            if (canActivateFilterWithoutDisposable(filter, manaAbilityMap)
                    && !filterActivationCompetesForSpellGeneric(filter, cost, ai)) {
                return false;
            }
            for (SpellAbility other : maList) {
                if (other == filter || other.getHostCard() == filterHost) {
                    continue;
                }
                if (isDisposableManaAbility(other)) {
                    return true;
                }
            }
        }
        if (toPay.isGeneric() || toPay == ManaCostShard.X) {
            for (SpellAbility other : maList) {
                if (other == filter || other.getHostCard() == filterHost) {
                    continue;
                }
                final Cost otherCost = other.getPayCosts();
                if (otherCost == null || !otherCost.hasManaCost()) {
                    return true;
                }
            }
        }
        if (hasReusableActivatorForFilter(filter, ai)) {
            return false;
        }
        for (SpellAbility other : maList) {
            if (other == filter || other.getHostCard() == filterHost) {
                continue;
            }
            final Cost otherCost = other.getPayCosts();
            if (otherCost != null && !otherCost.hasManaCost() && isDisposableManaAbility(other)) {
                return true;
            }
        }
        return false;
    }

  /** Skip sacrificing a land for generic when another land can still tap for mana. */
    private static boolean isWastefulSacLandForGeneric(final SpellAbility ma, final ManaCostShard toPay,
            final Player ai) {
        if (!toPay.isGeneric() && toPay != ManaCostShard.X) {
            return false;
        }
        if (!isDisposableManaAbility(ma) || !ma.getHostCard().isLand() || ai == null) {
            return false;
        }
        final Card host = ma.getHostCard();
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c == host || isDisposableManaCard(c)) {
                continue;
            }
            for (SpellAbility other : getAIPlayableMana(c)) {
                if (other.getPayCosts() != null && other.getPayCosts().hasTapCost()
                        && !AiCardMemory.isRememberedCard(ai, c, MemorySet.PAYS_TAP_COST)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Record tap/sacrifice reservation during test-mode planning so it matches production auto-pay. */
    private static void rememberManaSourceConsumed(final Player ai, final SpellAbility ma) {
        if (ma.getPayCosts().hasTapCost()) {
            AiCardMemory.rememberCard(ai, ma.getHostCard(), MemorySet.PAYS_TAP_COST);
        }
        if (isDisposableManaAbility(ma)) {
            AiCardMemory.rememberCard(ai, ma.getHostCard(), MemorySet.PAYS_SAC_COST);
        }
    }

    /**
     * True when an untapped reusable source can still pay a filter's nested activation cost
     * (excludes disposables and sources already reserved for this payment).
     */
    private static boolean hasAvailableReusableActivatorForNestedCost(final Player ai, final Card filterHost) {
        if (ai == null) {
            return false;
        }
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility ma : getAIPlayableMana(c)) {
                if (!isFreeManaSourceForNestedActivation(ma, filterHost) || isDisposableManaAbility(ma)) {
                    continue;
                }
                if (ma.getPayCosts().hasTapCost()
                        && AiCardMemory.isRememberedCard(ai, c, MemorySet.PAYS_TAP_COST)) {
                    continue;
                }
                if (AiCardMemory.isRememberedCard(ai, c, MemorySet.PAYS_SAC_COST)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /** True when some reusable (non-disposable) source on the battlefield can pay this filter's activation cost. */
    private static boolean hasReusableActivatorForFilter(final SpellAbility filter, final Player ai) {
        if (ai == null) {
            return false;
        }
        final Card filterHost = filter.getHostCard();
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility ma : getAIPlayableMana(c)) {
                if (isFreeManaSourceForNestedActivation(ma, filterHost) && !isDisposableManaAbility(ma)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Simulate paying a mana ability's generic activation cost (e.g. a signet's {1}) during test-mode planning.
     * Only free sources (no mana activation cost of their own) may pay it, which blocks filter-for-filter
     * chains. Consumed sources are removed from the shared candidate pool so they can't be reused.
     *
     * @return true if the activation cost was fully paid from free sources.
     */
    private static boolean simulateNestedActivationCost(final SpellAbility filterAb,
            final SpellAbility sa, final Player ai, final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards,
            final List<Mana> manaSpentToPay, final CardCollection outTapped) {
        return payNestedActivationCost(filterAb, sa, ai, sourcesForShards, manaSpentToPay, true, false, outTapped);
    }

    /**
     * Production counterpart to {@link #simulateNestedActivationCost}: physically taps the same sources the
     * planner chose so Auto-pay matches simulation / feasibility checks.
     */
    private static boolean executeNestedActivationCost(final SpellAbility filterAb,
            final SpellAbility sa, final Player ai, final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards,
            final List<Mana> manaSpentToPay, final boolean effect) {
        return payNestedActivationCost(filterAb, sa, ai, sourcesForShards, manaSpentToPay, false, effect, null);
    }

    private static boolean payNestedActivationCost(final SpellAbility filterAb,
            final SpellAbility sa, final Player ai, final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards,
            final List<Mana> manaSpentToPay, final boolean test, final boolean effect,
            final CardCollection outTapped) {
        final CostPartMana costMana = filterAb.getPayCosts().getCostMana();
        if (costMana == null) {
            return true;
        }
        final ManaCost activationMana = costMana.getManaCostFor(filterAb);
        if (activationMana.isNoCost() || activationMana.getCMC() == 0) {
            return true;
        }

        final ManaCostBeingPaid nestedCost = new ManaCostBeingPaid(activationMana);
        final Card filterHost = filterAb.getHostCard();

        // The outer sourcesForShards only lists shards from the spell being cast (e.g. {W}{W} has no
        // GENERIC bucket), so build a dedicated map for paying this activation cost ({1}, etc.).
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, true);
        final ListMultimap<ManaCostShard, SpellAbility> nestedSourcesForShards =
                groupAndOrderToPayShards(ai, manaAbilityMap, nestedCost);
        sortManaAbilities(nestedSourcesForShards, manaAbilityMap, sa, nestedCost, ai);

        // First spend any floating mana in the pool towards the activation cost.
        final ManaPool pool = ai.getManaPool();
        pool.payManaCostFromPool(nestedCost, filterAb, test, manaSpentToPay);

        while (!nestedCost.isPaid()) {
            final String costBefore = nestedCost.toString();
            final ManaCostShard toPay = getNextShardToPay(nestedCost, nestedSourcesForShards);
            if (toPay == null) {
                return false;
            }
            final Collection<SpellAbility> saList = nestedSourcesForShards.get(toPay);
            if (saList == null || saList.isEmpty()) {
                return false;
            }

            // Only free sources may pay a nested activation cost (no other signets/filters),
            // and the filter can never tap itself to pay its own cost.
            final boolean reusableActivatorAvailable = hasAvailableReusableActivatorForNestedCost(ai, filterHost);
            final List<SpellAbility> freeCandidates = new ArrayList<>();
            for (SpellAbility ma : saList) {
                if (!isFreeManaSourceForNestedActivation(ma, filterHost)) {
                    continue;
                }
                if (ma.getPayCosts().hasTapCost()
                        && AiCardMemory.isRememberedCard(ai, ma.getHostCard(), MemorySet.PAYS_TAP_COST)) {
                    continue;
                }
                if (AiCardMemory.isRememberedCard(ai, ma.getHostCard(), MemorySet.PAYS_SAC_COST)) {
                    continue;
                }
                if (reusableActivatorAvailable && isDisposableManaAbility(ma)) {
                    continue;
                }
                freeCandidates.add(ma);
            }
            if (freeCandidates.isEmpty()) {
                return false;
            }
            sortFreeSourcesForNestedActivation(freeCandidates, toPay,
                    shouldReserveColorlessMana(ai, sa));

            final SpellAbility chosen = chooseSourceForFilterActivation(sa, ai, filterAb, toPay, freeCandidates, nestedCost, nestedSourcesForShards);
            if (chosen == null) {
                return false;
            }

            if (outTapped != null) {
                outTapped.add(chosen.getHostCard());
            }

            if (test) {
                final String manaProduced = predictManafromSpellAbility(chosen, ai, toPay);
                debugLogNested(true, "    nested tap " + chosen.getHostCard() + " -> " + manaProduced + " for " + filterHost + " activation");
                final String unused = payMultipleMana(nestedCost, manaProduced, ai);
                depositNestedManaSurplus(unused, chosen.getHostCard(), ai, manaSpentToPay);
                rememberManaSourceConsumed(ai, chosen);
            } else {
                debugLogMain(false, "    nested tap " + chosen.getHostCard() + " for " + filterHost + " activation");
                if (!executeFreeManaSource(chosen, filterAb, ai, nestedCost, effect)) {
                    return false;
                }
            }
            nestedSourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(chosen.getHostCard()));
            sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(chosen.getHostCard()));
            if (costBefore.equals(nestedCost.toString())) {
                return false;
            }
        }
        return true;
    }

    /** Pay tap/sac/etc. on a mana source without re-entering generic mana payment (already handled by nested planner). */
    private static boolean payNonManaAbilityCosts(final SpellAbility ma, final Player ai, final boolean effect) {
        final Cost adjusted = CostAdjustment.adjust(ma.getPayCosts(), ma, effect);
        if (adjusted == null) {
            return true;
        }
        for (final CostPart part : adjusted.getCostParts()) {
            if (part instanceof CostPartMana) {
                continue;
            }
            final PaymentDecision pd = part.accept(new AiCostDecision(ai, ma, effect, true));
            if (pd == null || !part.payAsDecided(ai, pd, ma, effect)) {
                return false;
            }
        }
        return true;
    }

    /** Physically activate a free mana source and apply its mana toward {@code costToPay}. */
    private static boolean executeFreeManaSource(final SpellAbility ma, final SpellAbility saPaidFor,
            final Player ai, final ManaCostBeingPaid costToPay, final boolean effect) {
        ma.setActivatingPlayer(ai);
        if (!ComputerUtilCost.checkForManaSacrificeCost(ai, ma.getPayCosts(), ma, ma.isTrigger())) {
            return false;
        }
        if (!ComputerUtilCost.checkTapTypeCost(ai, ma.getPayCosts(), ma.getHostCard(), saPaidFor, AiCardMemory.getMemorySet(ai, MemorySet.PAYS_TAP_COST))) {
            return false;
        }
        if (!payNonManaAbilityCosts(ma, ai, effect)) {
            return false;
        }
        ai.getGame().getStack().addAndUnfreeze(ma);
        ai.getManaPool().payManaFromAbility(saPaidFor, costToPay, ma);
        return true;
    }

    /**
     * Activate a filter after {@link #executeNestedActivationCost} paid its generic activation cost.
     * Skips {@link ComputerUtilCost#checkTapTypeCost} on the filter host because the outer payment loop
     * already reserved it in {@link MemorySet#PAYS_TAP_COST} before calling here.
     */
    private static boolean executeFilterManaSource(final SpellAbility filterAb, final SpellAbility sa,
            final Player ai, final ManaCostBeingPaid cost, final ManaCostShard toPay,
            final boolean effect, final ManaPool manapool) {
        filterAb.setActivatingPlayer(ai);
        if (!ComputerUtilCost.checkForManaSacrificeCost(ai, filterAb.getPayCosts(), filterAb, filterAb.isTrigger())) {
            return false;
        }
        refreshExpressChoice(cost, sa, ai, toPay, filterAb);
        if (!payNonManaAbilityCosts(filterAb, ai, effect)) {
            return false;
        }
        ai.getGame().getStack().addAndUnfreeze(filterAb);
        manapool.payManaFromAbility(sa, cost, filterAb);
        return true;
    }

    /**
     * Apply a chosen mana source to {@code cost}. Test mode simulates; production executes the same plan
     * (including nested filter activation costs) so Auto-pay matches feasibility / simulation output.
     */
    private static boolean applyChosenManaPayment(final SpellAbility saPayment, final SpellAbility sa,
            final Player ai, final ManaCostBeingPaid cost, final ManaCostShard toPay,
            final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards, final List<Mana> manaSpentToPay,
            final boolean test, final boolean effect, final ManaPool manapool, final CardCollection outTapped) {
        if (test) {
            if (saPayment.getPayCosts() != null && saPayment.getPayCosts().hasManaCost()) {
                if (!simulateNestedActivationCost(saPayment, sa, ai, sourcesForShards, manaSpentToPay, outTapped)) {
                    return false;
                }
            }
            if (isMultiManaComboAbility(saPayment)) {
                setComboManaChoice(ai, saPayment, cost);
            }
            String manaProduced = predictManafromSpellAbility(saPayment, ai, toPay);
            if (isMultiManaComboAbility(saPayment)) {
                manaProduced = capComboManaProduced(manaProduced, getComboManaAmount(saPayment));
            }
            debugLogMain(true, "  tap " + saPayment.getHostCard() + " -> " + manaProduced
                    + " (paying " + toPay + " for " + manaPaymentSpellLabel(sa) + ")");
            payMultipleMana(cost, manaProduced, ai);
        } else if (saPayment.getPayCosts() != null && saPayment.getPayCosts().hasManaCost()) {
            if (!executeNestedActivationCost(saPayment, sa, ai, sourcesForShards, manaSpentToPay, effect)) {
                return false;
            }
            if (!executeFilterManaSource(saPayment, sa, ai, cost, toPay, effect, manapool)) {
                return false;
            }
        } else {
            if (isMultiManaComboAbility(saPayment)) {
                setComboManaChoice(ai, saPayment, cost);
            }
            final CostPayment pay = new CostPayment(saPayment.getPayCosts(), saPayment);
            if (!pay.payComputerCosts(new AiCostDecision(ai, saPayment, effect, true))) {
                return false;
            }
            ai.getGame().getStack().addAndUnfreeze(saPayment);
            manapool.payManaFromAbility(sa, cost, saPayment);
        }
        sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(saPayment.getHostCard()));
        return true;
    }

    /**
     * Choose which free source pays a filter's generic activation cost, preferring the source that keeps
     * the most castable spells in hand and command zone afterwards (castability-aware).
     * Falls back to {@link #chooseManaAbility}.
     */
    private static SpellAbility chooseSourceForFilterActivation(final SpellAbility sa, final Player ai,
            final SpellAbility filterAb, final ManaCostShard toPay, final List<SpellAbility> candidates,
            final ManaCostBeingPaid nestedCost, final ListMultimap<ManaCostShard, SpellAbility> sourcesForShards) {
        if (candidates.size() == 1) {
            // Sole free source for a nested activation; reservation runs when the payment is applied.
            return candidates.get(0);
        }
        // Castability probing runs full canPayManaCost and can recurse heavily; only use it for hand/command casts.
        if (inFilterActivationProbe.get() || !shouldUseCastabilityProbeForFilterActivation(sa)) {
            return chooseManaAbility(nestedCost, sa, ai, toPay, candidates, true);
        }

        SpellAbility best = null;
        int bestCastable = -1;
        inFilterActivationProbe.set(Boolean.TRUE);
        try {
            for (SpellAbility cand : candidates) {
                // simulate tapping this candidate + the filter, then count remaining castable spells
                final Set<Card> consumed = new HashSet<>();
                consumed.add(cand.getHostCard());
                consumed.add(filterAb.getHostCard());
                final int castable = countCastableSpellsAfterPayment(ai, sa, consumed);
                debugLog(true, "    candidate for " + filterAb.getHostCard() + " {1}: " + cand.getHostCard()
                        + " -> " + castable + " spells still castable (hand/command)");
                if (castable > bestCastable) {
                    bestCastable = castable;
                    best = cand;
                }
            }
        } finally {
            inFilterActivationProbe.set(Boolean.FALSE);
        }
        if (best != null) {
            return best;
        }
        // fall back to the standard chooser if the heuristic couldn't decide
        return chooseManaAbility(nestedCost, sa, ai, toPay, candidates, true);
    }

    /** Hand and command-zone casts benefit from castability-aware filter activation payment. */
    private static boolean shouldUseCastabilityProbeForFilterActivation(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        return host.isInZone(ZoneType.Hand) || host.isInZone(ZoneType.Command);
    }

    /**
     * Count how many other spells in hand and command zone could still be cast if the given cards were
     * consumed for the current payment. Used for castability-aware source selection.
     */
    private static int countCastableSpellsAfterPayment(final Player ai, final SpellAbility spellBeingPaid, final Set<Card> consumed) {
        int count = countCastableSpellsInZone(ai, spellBeingPaid, consumed, ZoneType.Hand);
        count += countCastableSpellsInZone(ai, spellBeingPaid, consumed, ZoneType.Command);
        return count;
    }

    private static int countCastableSpellsInZone(final Player ai, final SpellAbility spellBeingPaid,
            final Set<Card> consumed, final ZoneType zone) {
        int count = 0;
        final Card being = spellBeingPaid.getHostCard();
        for (Card c : ai.getCardsIn(zone)) {
            if (c == being) {
                continue;
            }
            for (SpellAbility candSa : c.getSpellAbilities()) {
                if (!candSa.isSpell() || candSa.getPayCosts() == null || !candSa.getPayCosts().hasManaCost()) {
                    continue;
                }
                candSa.setActivatingPlayer(ai);
                if (canPayManaCostExcluding(candSa, ai, consumed)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Feasibility check for a spell while pretending the given cards are already consumed (tapped) for another payment.
     */
    private static boolean canPayManaCostExcluding(final SpellAbility candSa, final Player ai, final Set<Card> consumed) {
        // Reserve the consumed cards so isManaSourceReserved() hides them from the feasibility check.
        // (PAYS_TAP_COST would be cleared at the start of payManaCost, so it can't be used here.)
        final List<Card> reserved = new ArrayList<>();
        for (Card c : consumed) {
            if (!AiCardMemory.isRememberedCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
                AiCardMemory.rememberCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
                reserved.add(c);
            }
        }
        final Set<Card> sacSnapshot = snapshotMemory(ai, MemorySet.PAYS_SAC_COST);
        final Set<Card> tapSnapshot = snapshotMemory(ai, MemorySet.PAYS_TAP_COST);
        try {
            return canPayManaCost(candSa.getPayCosts(), candSa, ai, 0, false);
        } finally {
            for (Card c : reserved) {
                AiCardMemory.forgetCard(ai, c, MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL);
            }
            restoreMemory(ai, MemorySet.PAYS_SAC_COST, sacSnapshot);
            restoreMemory(ai, MemorySet.PAYS_TAP_COST, tapSnapshot);
        }
    }

    public static String predictManaReplacement(SpellAbility saPayment, Player ai, ManaCostShard toPay) {
        Card hostCard = saPayment.getHostCard();
        Game game = hostCard.getGame();
        String manaProduced = toPay.isSnow() && hostCard.isSnow() ? "S" : GameActionUtil.generatedTotalMana(saPayment);

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(hostCard);
        repParams.put(AbilityKey.Mana, manaProduced);
        repParams.put(AbilityKey.Activator, ai);
        repParams.put(AbilityKey.AbilityMana, saPayment); // RootAbility

        // TODO Damping Sphere might replace later?

        // add flags to replacementEffects to filter better?
        List<ReplacementEffect> reList = game.getReplacementHandler().getReplacementList(ReplacementType.ProduceMana, repParams, ReplacementLayer.Other);

        List<SpellAbility> replaceMana = Lists.newArrayList();
        List<SpellAbility> replaceType = Lists.newArrayList();
        List<SpellAbility> replaceAmount = Lists.newArrayList(); // currently only multi

        // try to guess the color the mana gets replaced to
        for (ReplacementEffect re : reList) {
            SpellAbility o = re.getOverridingAbility();

            if (o == null || o.getApi() != ApiType.ReplaceMana) {
                continue;
            }

            // this one does replace the amount too
            if (o.hasParam("ReplaceMana")) {
                replaceMana.add(o);
            } else if (o.hasParam("ReplaceType") || o.hasParam("ReplaceColor")) {
                // this one replaces the color/type
                // check if this one can be replaced into wanted mana shard
                replaceType.add(o);
            } else if (o.hasParam("ReplaceAmount")) {
                replaceAmount.add(o);
            }
        }

        // it is better to apply these ones first
        if (!replaceMana.isEmpty()) {
            for (SpellAbility saMana : replaceMana) {
                // one of then has to Any
                // one of then has to C
                // one of then has to B
                String m = saMana.getParam("ReplaceMana");
                if ("Any".equals(m)) {
                    byte rs = MagicColor.GREEN;
                    for (byte c : MagicColor.WUBRGC) {
                        if (toPay.canBePaidWithManaOfColor(c)) {
                            rs = c;
                            break;
                        }
                    }
                    manaProduced = MagicColor.toShortString(rs);
                } else {
                    manaProduced = m;
                }
            }
        }

        // then apply this one
        if (!replaceType.isEmpty()) {
            for (SpellAbility saMana : replaceAmount) {
                Card card = saMana.getHostCard();
                if (saMana.hasParam("ReplaceType")) {
                    // replace color and colorless
                    String color = saMana.getParam("ReplaceType");
                    if ("Any".equals(color)) {
                        byte rs = MagicColor.GREEN;
                        for (byte c : MagicColor.WUBRGC) {
                            if (toPay.canBePaidWithManaOfColor(c)) {
                                rs = c;
                                break;
                            }
                        }
                        color = MagicColor.toShortString(rs);
                    }
                    for (byte c : MagicColor.WUBRGC) {
                        String s = MagicColor.toShortString(c);
                        manaProduced = manaProduced.replace(s, color);
                    }
                } else if (saMana.hasParam("ReplaceColor")) {
                    String color = saMana.getParam("ReplaceColor");
                    if ("Chosen".equals(color)) {
                        if (card.hasChosenColor()) {
                            color = MagicColor.toShortString(card.getChosenColor());
                        }
                    }
                    if (saMana.hasParam("ReplaceOnly")) {
                        manaProduced = manaProduced.replace(saMana.getParam("ReplaceOnly"), color);
                    } else {
                        for (byte c : MagicColor.WUBRG) {
                            String s = MagicColor.toShortString(c);
                            manaProduced = manaProduced.replace(s, color);
                        }
                    }
                }
            }
        }

        // then multiply if able
        if (!replaceAmount.isEmpty()) {
            int totalAmount = 1;
            for (SpellAbility saMana : replaceAmount) {
                totalAmount *= Integer.parseInt(saMana.getParam("ReplaceAmount"));
            }
            manaProduced = StringUtils.repeat(manaProduced, " ", totalAmount);
        }

        return manaProduced;
    }

    public static String predictManafromSpellAbility(SpellAbility saPayment, Player ai, ManaCostShard toPay) {
        Card hostCard = saPayment.getHostCard();

        StringBuilder manaProduced = new StringBuilder(predictManaReplacement(saPayment, ai, toPay));
        String originalProduced = manaProduced.toString();

        if (originalProduced.isEmpty()) {
            return originalProduced;
        }

        // Run triggers like Nissa
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(hostCard);
        runParams.put(AbilityKey.Activator, ai); // assuming AI would only ever gives itself mana
        runParams.put(AbilityKey.AbilityMana, saPayment);
        runParams.put(AbilityKey.Produced, originalProduced);
        for (Trigger tr : ai.getGame().getTriggerHandler().getActiveTrigger(TriggerType.TapsForMana, runParams)) {
            SpellAbility trSA = tr.ensureAbility();
            if (trSA == null) {
                continue;
            }
            if (ApiType.Mana.equals(trSA.getApi())) {
                int pAmount = AbilityUtils.calculateAmount(trSA.getHostCard(), trSA.getParamOrDefault("Amount", "1"), trSA);
                String produced = trSA.getParam("Produced");
                if (produced.equals("Chosen")) {
                    produced = MagicColor.toShortString(trSA.getHostCard().getChosenColor());
                }
                manaProduced.append(" ").append(StringUtils.repeat(produced, " ", pAmount));
            } else if (ApiType.ManaReflected.equals(trSA.getApi())) {
                final String colorOrType = trSA.getParamOrDefault("ColorOrType", "Color");
                // currently Color or Type, Type is colors + colorless
                final String reflectProperty = trSA.getParam("ReflectProperty");

                if (reflectProperty.equals("Produced") && !originalProduced.isEmpty()) {
                    // check if a colorless shard can be paid from the trigger
                    if (toPay.equals(ManaCostShard.COLORLESS) && colorOrType.equals("Type") && originalProduced.contains("C")) {
                        manaProduced.append(" " + "C");
                    } else if (originalProduced.length() == 1) {
                        // if length is only one, and it either is equal C == Type
                        if (colorOrType.equals("Type") || !originalProduced.equals("C")) {
                            manaProduced.append(" ").append(originalProduced);
                        }
                    } else {
                        // should it look for other shards too?
                        boolean found = false;
                        for (String s : originalProduced.split(" ")) {
                            if (colorOrType.equals("Type") || !s.equals("C") && toPay.canBePaidWithManaOfColor(MagicColor.fromName(s))) {
                                found = true;
                                manaProduced.append(" ").append(s);
                                break;
                            }
                        }
                        // no good mana found? just add the first generated color
                        if (!found) {
                            for (String s : originalProduced.split(" ")) {
                                if (colorOrType.equals("Type") || !s.equals("C")) {
                                    manaProduced.append(" ").append(s);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return manaProduced.toString();
    }

    /**
     * Dry-run the unified mana planner and return the host cards it would consume (including nested
     * activators). Uses the same path as {@link #canPayManaCost} and production {@link #payManaCost}.
     */
    public static CardCollection getManaSourcesToPayCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        final CardCollection plan = new CardCollection();
        payManaCost(cost, sa, ai, true, true, false, plan);
        return plan;
    }

    /** @return cards Auto would tap to pay, or null if the cost can't be paid */
    public static CardCollection getManaSourcesToPayCostIfAble(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        final ManaCostBeingPaid costCopy = new ManaCostBeingPaid(cost);
        final CardCollection sources = getManaSourcesToPayCost(costCopy, sa, ai);
        return costCopy.isPaid() ? sources : null;
    }

    private static boolean payManaCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean test, boolean checkPlayable, boolean effect, final CardCollection planOut) {
        final int depth = manaPaymentDepth.get() + 1;
        manaPaymentDepth.set(depth);
        final boolean outermost = depth == 1;
        try {
        if ((sa.isOffering() && sa.getSacrificedAsOffering() == null) || (sa.isEmerge() && sa.getSacrificedAsEmerge() == null)) {
            // nothing was chosen
            return null;
        }

        if (outermost) {
            AiCardMemory.clearMemorySet(ai, MemorySet.PAYS_TAP_COST);
            AiCardMemory.clearMemorySet(ai, MemorySet.PAYS_SAC_COST);
            clearManaPaymentPlanCache();
        }
        adjustManaCostToAvoidNegEffects(cost, sa.getHostCard(), ai);

        debugLogMain(test, "paying " + cost + " for " + manaPaymentSpellLabel(sa));

        List<Mana> manaSpentToPay = test ? new ArrayList<>() : sa.getPayingMana();
        List<SpellAbility> paymentList = Lists.newArrayList();
        final ManaPool manapool = ai.getManaPool();

        // Apply color/type conversion matrix if necessary (already done via autopay)
        if (ai.getControllingPlayer() == null) {
            manapool.restoreColorReplacements();
            CardPlayOption mayPlay = sa.getMayPlayOption();
            if (!effect) {
                if (sa.isSpell() && mayPlay != null) {
                    mayPlay.applyManaConvert(manapool);
                } else if (sa.isActivatedAbility() && sa.getGrantorStatic() != null && sa.getGrantorStatic().hasParam("ManaConversion")) {
                    AbilityUtils.applyManaColorConversion(manapool, sa.getGrantorStatic().getParam("ManaConversion"));
                }
            }
            if (sa.hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(manapool, sa.getParam("ManaConversion"));
            }
            StaticAbilityManaConvert.manaConvert(manapool, ai, sa.getHostCard(), effect && !sa.isCastFromPlayEffect() ? null : sa);
        }

        // not worth checking if it makes sense to not spend floating first
        if (manapool.payManaCostFromPool(cost, sa, test, manaSpentToPay)) {
            CostPayment.handleOfferings(sa, test, cost.isPaid());
            debugLogMain(test, "  result: PAID (pool)");
            // paid all from floating mana
            return manaSpentToPay;
        }

        int phyLifeToPay = 2;
        boolean purePhyrexian = cost.containsOnlyPhyrexianMana();
        boolean hasConverge = sa.getHostCard().hasConverge();
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = getSourcesForShards(cost, sa, ai, test, checkPlayable, hasConverge);

        int testEnergyPool = ai.getCounters(CounterEnumType.ENERGY);
        ManaCostShard toPay = null;
        List<SpellAbility> saExcludeList = new ArrayList<>();

        // Loop over mana needed
        while (!cost.isPaid()) {
            while (!cost.isPaid() && !manapool.isEmpty()) {
                boolean found = false;
                for (byte color : ManaAtom.MANATYPES) {
                    if (manapool.tryPayCostWithColor(color, sa, cost, manaSpentToPay)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
            if (cost.isPaid()) {
                break;
            }

            if (sourcesForShards == null && !purePhyrexian) {
                // no mana abilities to use for paying
                break;
            }

            toPay = getNextShardToPay(cost, sourcesForShards);

            Collection<SpellAbility> saList = null;
            if (hasConverge &&
                    (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X)) {
                final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
                for (final MagicColor.Color b : ColorSet.fromMask(unpaidColors)) {
                    // try and pay other colors for converge
                    final ManaCostShard shard = ManaCostShard.valueOf(b.getColorMask());
                    saList = sourcesForShards.get(shard);
                    if (saList != null && !saList.isEmpty()) {
                        toPay = shard;
                        break;
                    }
                }
                if (saList == null || saList.isEmpty()) {
                    // failed to converge, revert to paying generic
                    saList = sourcesForShards.get(toPay);
                    hasConverge = false;
                }
            } else if (sourcesForShards == null && purePhyrexian) {
                // Phyrexian mana only: no valid mana sources, but can still pay life
                saList = Lists.newArrayList();
            } else {
                saList = sourcesForShards.get(toPay);
            }

            saList.removeAll(saExcludeList);
            debugLogMain(test, "  shard " + toPay + " candidates: " + saList);

            SpellAbility saPayment = saList.isEmpty() ? null
                    : chooseManaAbilityForShard(cost, sa, ai, toPay, saList, checkPlayable || !test);
            debugLogMain(test, "  chosen for " + toPay + ": " + (saPayment == null ? "(none)" : saPayment.getHostCard()));

            if (saPayment != null && ComputerUtilCost.isSacrificeSelfCost(saPayment.getPayCosts()) && sa.isTargeting(saPayment.getHostCard())) {
                // not a good idea to sac a card that you're targeting with the SA you're paying for
                saExcludeList.add(saPayment);
                continue;
            }

            if (saPayment != null && "BlackLotus".equals(saPayment.getParam("AILogic")) && !SpecialCardAi.BlackLotus.consider(ai, sa, cost)) {
                // since we checked this already, do not loop indefinitely checking again
                saExcludeList.add(saPayment);
                continue;
            }

            if (saPayment == null) {
                boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(phyLifeToPay, false, sa)
                        || (ai.getLife() <= phyLifeToPay && !ai.cantLoseForZeroOrLessLife())) {
                    // cannot pay
                    break;
                }
                if (test) {
                    phyLifeToPay += 2;
                }

                if (sa.hasParam("AIPhyrexianPayment")) {
                    if ("Never".equals(sa.getParam("AIPhyrexianPayment"))) {
                        break; // unwise to pay
                    } else if (sa.getParam("AIPhyrexianPayment").startsWith("OnFatalDamage.")) {
                        int dmg = Integer.parseInt(sa.getParam("AIPhyrexianPayment").substring(14));
                        if (ai.getOpponents().stream().noneMatch(PlayerPredicates.lifeLessOrEqualTo(dmg))) {
                            break; // no one to finish with the gut shot
                        }
                    }
                }

                if (toPay.isPhyrexian()) {
                    cost.payPhyrexian();
                    if (!test) {
                        sa.setSpendPhyrexianMana(true);
                    }
                } else if (lifeInsteadOfBlack) {
                    cost.decreaseShard(ManaCostShard.BLACK, 1);
                }

                if (!test) {
                    ai.payLife(2, sa, false);
                }
                continue;
            }

            paymentList.add(saPayment);
            if (saPayment.getPayCosts().hasTapCost()) {
                AiCardMemory.rememberCard(ai, saPayment.getHostCard(), MemorySet.PAYS_TAP_COST);
            }

            if (test) {
                // Check energy when testing
                CostPayEnergy energyCost = saPayment.getPayCosts().getCostEnergy();
                if (energyCost != null) {
                    testEnergyPool -= Integer.parseInt(energyCost.getAmount());
                    if (testEnergyPool < 0) {
                        // Can't pay energy cost
                        break;
                    }
                }
            }

            if (!applyChosenManaPayment(saPayment, sa, ai, cost, toPay, sourcesForShards, manaSpentToPay, test, effect, manapool, planOut)) {
                if (saPayment.getPayCosts().hasManaCost()) {
                    debugLogMain(test, "  reject " + saPayment.getHostCard() + " (nested activation cost unpayable)");
                }
                saExcludeList.add(saPayment);
                paymentList.remove(saPayment);
                if (saPayment.getPayCosts().hasTapCost()) {
                    AiCardMemory.forgetCard(ai, saPayment.getHostCard(), MemorySet.PAYS_TAP_COST);
                }
                if (!test) {
                    saList.remove(saPayment);
                }
                continue;
            }

            if (planOut != null) {
                planOut.add(saPayment.getHostCard());
            }
            if (test) {
                rememberManaSourceConsumed(ai, saPayment);
            }

            if (!test) {
                // need to consider if another use is now prevented
                if (!cost.isPaid() && saPayment.isActivatedAbility() && !saPayment.getRestrictions().canPlay(saPayment.getHostCard(), saPayment)) {
                    sourcesForShards.values().removeIf(s -> s == saPayment);
                }

                if (hasConverge) {
                    // hack to prevent converge re-using sources
                    sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(saPayment.getHostCard()));
                }
            }
        }

        CostPayment.handleOfferings(sa, test, cost.isPaid());

        // The cost is still unpaid, so refund the mana and report
        if (!cost.isPaid()) {
            debugLogMain(test, "  result: FAILED (unpaid " + toPay + ")");
            manapool.refundMana(manaSpentToPay);
            if (test) {
                resetPayment(paymentList);
            } else {
                System.out.println("ComputerUtilMana: payManaCost() cost was not paid for " + sa + " (" +  sa.getHostCard().getName() + "). Didn't find what to pay for " + toPay);
                sa.setSkip(true);
            }
            return null;
        }

        debugLogMain(test, "  result: PAID");

        if (test) {
            manapool.refundMana(manaSpentToPay);
            resetPayment(paymentList);
        }

        return true;
        } finally {
            manaPaymentDepth.set(depth - 1);
        }
    }

    private static void resetPayment(List<SpellAbility> payments) {
        for (SpellAbility sa : payments) {
            sa.getManaPart().clearExpressChoice();
        }
    }

    /**
     * Creates a mapping between the required mana shards and the available spell abilities to pay for them
     */
    private static ListMultimap<ManaCostShard, SpellAbility> getSourcesForShards(final ManaCostBeingPaid cost,
            final SpellAbility sa, final Player ai, final boolean test, final boolean checkPlayable,
            final boolean hasConverge) {
        // arrange all mana abilities by color produced.
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, checkPlayable);
        debugLogMain(test, "  source colors: " + manaAbilityMap);
        if (manaAbilityMap.isEmpty()) {
            // no mana abilities, bailing out
            debugLogMain(test, "  no playable mana abilities found");
            return null;
        }

        // select which abilities may be used for each shard
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = groupAndOrderToPayShards(ai, manaAbilityMap, cost);
        if (hasConverge) {
            // add extra colors for paying converge
            final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
            for (final MagicColor.Color color : ColorSet.fromMask(unpaidColors)) {
                final byte b = color.getColorMask();
                final ManaCostShard shard = ManaCostShard.valueOf(b);
                if (!sourcesForShards.containsKey(shard)) {
                    if (ai.getManaPool().canPayForShardWithColor(shard, b)) {
                        for (SpellAbility saMana : manaAbilityMap.get((int)b)) {
                            sourcesForShards.get(shard).add(saMana);
                        }
                    }
                }
            }
        }

        sortManaAbilities(sourcesForShards, manaAbilityMap, sa, cost, ai);
        debugLogMain(test, "  sources by shard: " + sourcesForShards);
        return sourcesForShards;
    }

    private static String capComboManaProduced(final String manaProduced, final int maxMana) {
        if (manaProduced == null || manaProduced.isEmpty() || maxMana <= 0) {
            return manaProduced;
        }
        final String[] parts = TextUtil.split(manaProduced, ' ');
        if (parts.length <= maxMana) {
            return manaProduced;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxMana; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static void setComboManaChoice(final Player ai, final SpellAbility manaAb, final ManaCostBeingPaid cost) {
        final StringBuilder choiceString = new StringBuilder();
        final AbilityManaPart comboMana = manaAb.getManaPart();

        int amount = manaAb.hasParam("Amount") ? AbilityUtils.calculateAmount(manaAb.getHostCard(), manaAb.getParam("Amount"), manaAb) : 1;
        final ManaCostBeingPaid testCost = new ManaCostBeingPaid(cost);
        final String[] comboColors = comboMana.getComboColors(manaAb).split(" ");

        // Honor a single-color hint from canPayShardWithSpellAbility; discard stale multi-pip express.
        final String expressHint = comboMana.getExpressChoice();
        comboMana.clearExpressChoice();
        final String preferredColor = expressHint != null && !expressHint.isEmpty() && !expressHint.contains(" ")
                ? expressHint : "";

        for (int nMana = 1; nMana <= amount; nMana++) {
            String choice = "";
            if (nMana == 1 && !preferredColor.isEmpty()
                    && manaAb.canProduce(preferredColor)
                    && satisfiesColorChoice(comboMana, choiceString, preferredColor)
                    && testCost.isAnyPartPayableWith(ManaAtom.fromName(preferredColor), ai.getManaPool())) {
                choice = preferredColor;
            }
            if (choice.isEmpty() && !testCost.isPaid()) {
                for (String color : comboColors) {
                    if (satisfiesColorChoice(comboMana, choiceString, color)
                            && testCost.needsColor(ManaAtom.fromName(color), ai.getManaPool())) {
                        choice = color;
                        break;
                    }
                }
            }
            if (choice.isEmpty()) {
                String commonColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Hand));
                if (!commonColor.isEmpty()
                        && satisfiesColorChoice(comboMana, choiceString, MagicColor.toShortString(commonColor))
                        && comboMana.getComboColors(manaAb).contains(MagicColor.toShortString(commonColor))) {
                    choice = MagicColor.toShortString(commonColor);
                } else {
                    for (String c : comboColors) {
                        if (satisfiesColorChoice(comboMana, choiceString, c)) {
                            choice = c;
                            break;
                        }
                    }
                }
            }
            if (choice.isEmpty()) {
                break;
            }
            payMultipleMana(testCost, choice, ai);
            if (choiceString.length() > 0) {
                choiceString.append(' ');
            }
            choiceString.append(choice);
        }

        comboMana.setExpressChoice(choiceString.length() == 0 ? "0" : choiceString.toString());
    }

    private static boolean satisfiesColorChoice(AbilityManaPart abMana, StringBuilder choices, String choice) {
        return !abMana.getOrigProduced().contains("Different") || !choices.toString().contains(choice);
    }

    private static boolean canPayShardWithSpellAbility(ManaCostShard toPay, Player ai, SpellAbility ma, SpellAbility sa, ManaCostBeingPaid cost, boolean checkCosts, Map<String, Integer> xManaCostPaidByColor) {
        final Card sourceCard = ma.getHostCard();

        if (isManaSourceReserved(ai, sourceCard)) {
            return false;
        }

        if (toPay.isSnow() && !sourceCard.isSnow()) {
            return false;
        }

        AbilityManaPart m = ma.getManaPart();
        if (!m.meetsManaRestrictions(sa)) {
            return false;
        }

        if (checkCosts) {
            // Check if AI can still play this mana ability
            ma.setActivatingPlayer(ai);
            // Filters with only a generic activation cost ({1}) pay that via simulateNestedActivationCost;
            // requiring full Cost.canPay here rejects them when the {1} is not yet assigned.
            if (ma.getPayCosts() != null && ma.getPayCosts().hasManaCost() && hasOnlyGenericManaCost(ma.getPayCosts())) {
                if (ma.getRestrictions() != null && ma.getRestrictions().isInstantSpeed()) {
                    return false;
                }
            } else if (!CostPayment.canPayAdditionalCosts(ma.getPayCosts(), ma, false)) {
                return false;
            } else if (ma.getRestrictions() != null && ma.getRestrictions().isInstantSpeed()) {
                return false;
            }
        }

        if (m.isComboMana()) {
            for (String s : m.getComboColors(ma).split(" ")) {
                if (toPay == ManaCostShard.COLORED_X && !ManaCostBeingPaid.canColoredXShardBePaidByColor(s, xManaCostPaidByColor)) {
                    continue;
                }

                if (!sa.allowsPayingWithShard(sourceCard, ManaAtom.fromName(s))) {
                    continue;
                }

                if (ai.getManaPool().canPayForShardWithColor(toPay, ManaAtom.fromName(s))) {
                    // usually we'll want to produce color that matches the shard
                    ColorSet shared = ColorSet.fromMask(toPay.getColorMask()).getSharedColors(ColorSet.fromNames(m.getComboColors(ma).split(" ")));
                    // but other effects might still lead to a more permissive payment
                    if (!shared.isColorless()) {
                        m.setExpressChoice(shared.iterator().next().getShortName());
                    }
                    setComboManaChoice(ai, ma, cost);
                    return true;
                }
            }
            return false;
        }

        if (ma.getApi() == ApiType.ManaReflected) {
            Set<String> reflected = CardUtil.getReflectableManaColors(ma);

            for (byte c : MagicColor.WUBRGC) {
                if (toPay == ManaCostShard.COLORED_X && !ManaCostBeingPaid.canColoredXShardBePaidByColor(MagicColor.toShortString(c), xManaCostPaidByColor)) {
                    continue;
                }

                if (!sa.allowsPayingWithShard(sourceCard, c)) {
                    continue;
                }

                if (ai.getManaPool().canPayForShardWithColor(toPay, c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return true;
                }
            }
            return false;
        }

        if (m.isAnyMana()) {
            byte colorChoice = 0;
            if (toPay.isOr2Generic()) {
                colorChoice = toPay.getColorMask();
                if (!sa.allowsPayingWithShard(sourceCard, colorChoice)
                        || !ai.getManaPool().canPayForShardWithColor(toPay, colorChoice)) {
                    colorChoice = 0;
                }
            } else {
                for (byte c : MagicColor.WUBRG) {
                    if (sa.allowsPayingWithShard(sourceCard, c)
                            && ai.getManaPool().canPayForShardWithColor(toPay, c)) {
                        colorChoice = c;
                        break;
                    }
                }
            }
            if (colorChoice == 0) {
                return false;
            }
            m.setExpressChoice(MagicColor.toShortString(colorChoice));
            return true;
        }

        // Abilities that add several colors at once (e.g. Boros Signet "Produced$ R W") list each
        // color space-separated. MagicColor.fromName("R W") would resolve to colorless, so iterate the
        // produced colors individually and match against the shard being paid.
        final String[] producedColors = m.mana(ma).split(" ");
        final boolean multiColorProducer = producedColors.length > 1;
        if (multiColorProducer) {
            String payColor = null;
            for (String s : producedColors) {
                final byte c = MagicColor.fromName(s);
                if (c == 0 || !sa.allowsPayingWithShard(sourceCard, c)) {
                    continue;
                }
                if (toPay == ManaCostShard.COLORED_X) {
                    if (ManaCostBeingPaid.canColoredXShardBePaidByColor(s, xManaCostPaidByColor)) {
                        payColor = s;
                        break;
                    }
                } else if (ai.getManaPool().canPayForShardWithColor(toPay, c)) {
                    payColor = s;
                    break;
                }
            }
            if (payColor == null) {
                return false;
            }
            // Only pin the express choice when a single colored shard is being paid; leave multi-color
            // output intact so both pips (e.g. {R}{W}) can be consumed from one activation.
            if (!toPay.isGeneric() && toPay != ManaCostShard.COLORED_X) {
                m.setExpressChoice(payColor);
            }
            return true;
        }

        if (!sa.allowsPayingWithShard(sourceCard, MagicColor.fromName(producedColors[0]))) {
            return false;
        }

        if (toPay == ManaCostShard.COLORED_X) {
            for (String s : producedColors) {
                if (ManaCostBeingPaid.canColoredXShardBePaidByColor(s, xManaCostPaidByColor)) {
                    return true;
                }
            }
            return false;
        }

        final byte producedColor = MagicColor.fromName(producedColors[0]);
        return ai.getManaPool().canPayForShardWithColor(toPay, producedColor);
    }

    // returns true if sourceCard is reserved as a mana source for payment
    // for the future spell to be cast in another phase. However, if the spell ability that is
    // being considered for casting is high priority, then mana source reservation will be ignored.
    private static boolean isManaSourceReserved(Player ai, Card sourceCard) {
        if (!(ai.getController() instanceof PlayerControllerAi)) {
            return false;
        }

        // reserved for spell synchronization
        if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
            return true;
        }

        PhaseType curPhase = ai.getGame().getPhaseHandler().getPhase();
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();

        // For combat tricks, always obey mana reservation
        if (curPhase == PhaseType.COMBAT_DECLARE_BLOCKERS || curPhase == PhaseType.CLEANUP) {
            if (ai.getGame().getPhaseHandler().isPlayerTurn(ai)) {
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK);
            } else {
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK);
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT);
            }
        } else if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK) ||
                AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK)) {
            // This mana source is held elsewhere for a combat trick.
            return true;
        }

        int chanceToReserve = aic.getIntProperty(AiProps.RESERVE_MANA_FOR_MAIN2_CHANCE);
        // TODO use Math.min(100 - AiAbilityDecision.rating(), chanceToReserve)
        if (chanceToReserve == 0 || !MyRandom.percentTrue(chanceToReserve)) {
            // using a reserved source might make rest of reservation pointless, but that's tricky to conclude
            return false;
        }

        if (curPhase == PhaseType.MAIN2 || curPhase == PhaseType.CLEANUP) {
            AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
        } else if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2)) {
            // mana source is held elsewhere for a Main 2 spell
            return true;
        }

        return false;
    }

    private static ManaCostShard getNextShardToPay(ManaCostBeingPaid cost, Multimap<ManaCostShard, SpellAbility> sourcesForShards) {
        List<ManaCostShard> shardsToPay = Lists.newArrayList(cost.getDistinctShards());
        // optimize order so that the shards with less available sources are considered first
        shardsToPay.sort(Comparator.comparingInt(shard -> sourcesForShards.get(shard).size()));
        // mind the priorities
        // * Pay mono-colored first
        // * Pay 2/C with matching colors
        // * pay hybrids
        // * pay phyrexian, keep mana for colorless
        // * pay generic
        return cost.getShardToPayByPriority(shardsToPay, ColorSet.WUBRG.getColor());
    }

    private static void adjustManaCostToAvoidNegEffects(ManaCostBeingPaid cost, final Card card, Player ai) {
        // Make mana needed to avoid negative effect a mandatory cost for the AI
        for (String manaPart : card.getSVar("ManaNeededToAvoidNegativeEffect").split(",")) {
            // convert long color strings to short color strings
            if (manaPart.isEmpty()) {
                continue;
            }

            byte mask = ManaAtom.fromName(manaPart);

            // make mana mandatory for AI
            if (!cost.needsColor(mask, ai.getManaPool()) && cost.getGenericManaAmount() > 0) {
                ManaCostShard shard = ManaCostShard.valueOf(mask);
                cost.increaseShard(shard, 1);
                cost.decreaseGenericMana(1);
            }
        }
    }

    /**
     * <p>
     * payMultipleMana.
     * </p>
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static void depositNestedManaSurplus(final String unusedMana, final Card sourceCard,
            final Player ai, final List<Mana> manaSpentToPay) {
        if (unusedMana == null || sourceCard == null) {
            return;
        }
        final ManaPool pool = ai.getManaPool();
        for (final String manaPart : TextUtil.split(unusedMana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for (int i = Integer.parseInt(manaPart); i > 0; i--) {
                    final Mana surplus = new Mana((byte) ManaAtom.COLORLESS, sourceCard, null, ai);
                    pool.addManaNoEvent(surplus);
                    if (manaSpentToPay != null) {
                        manaSpentToPay.add(surplus);
                    }
                }
            } else if ("C".equalsIgnoreCase(manaPart)) {
                final Mana surplus = new Mana(ManaAtom.fromName(MagicColor.toShortString(manaPart)), sourceCard, null, ai);
                pool.addManaNoEvent(surplus);
                if (manaSpentToPay != null) {
                    manaSpentToPay.add(surplus);
                }
            }
        }
    }

    private static String payMultipleMana(ManaCostBeingPaid testCost, String mana, final Player p) {
        List<String> unused = new ArrayList<>(4);
        for (String manaPart : TextUtil.split(mana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for (int i = Integer.parseInt(manaPart); i > 0; i--) {
                    boolean wasNeeded = testCost.ai_payMana("1", p.getManaPool());
                    if (!wasNeeded) {
                        unused.add(Integer.toString(i));
                        break;
                    }
                }
            } else {
                String color = MagicColor.toShortString(manaPart);
                boolean wasNeeded = testCost.ai_payMana(color, p.getManaPool());
                if (!wasNeeded) {
                    unused.add(color);
                }
            }
        }
        return unused.isEmpty() ? null : StringUtils.join(unused, ' ');
    }

    /**
     * Find all mana sources.
     * @param manaAbilityMap The map of SpellAbilities that produce mana.
     * @return Were all mana sources found?
     */
    private static ListMultimap<ManaCostShard, SpellAbility> groupAndOrderToPayShards(final Player ai, final ListMultimap<Integer, SpellAbility> manaAbilityMap,
            final ManaCostBeingPaid cost) {
        ListMultimap<ManaCostShard, SpellAbility> res = ArrayListMultimap.create();

        if ((cost.getGenericManaAmount() > 0 || cost.hasAnyKind(ManaAtom.OR_2_GENERIC)) && manaAbilityMap.containsKey(ManaAtom.GENERIC)) {
            res.putAll(ManaCostShard.GENERIC, manaAbilityMap.get(ManaAtom.GENERIC));
        }

        // loop over cost parts
        for (ManaCostShard shard : cost.getDistinctShards()) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: shard = " + shard);
            }
            if (shard == ManaCostShard.S) {
                res.putAll(shard, manaAbilityMap.get(ManaAtom.IS_SNOW));
                continue;
            }

            if (shard.isOr2Generic()) {
                Integer colorKey = (int) shard.getColorMask();
                if (manaAbilityMap.containsKey(colorKey))
                    res.putAll(shard, manaAbilityMap.get(colorKey));
                if (manaAbilityMap.containsKey(ManaAtom.GENERIC))
                    res.putAll(shard, manaAbilityMap.get(ManaAtom.GENERIC));
                continue;
            }

            if (shard == ManaCostShard.GENERIC) {
                continue;
            }

            for (Integer colorint : manaAbilityMap.keySet()) {
                // apply mana color change matrix here
                if (ai.getManaPool().canPayForShardWithColor(shard, colorint.byteValue())) {
                    for (SpellAbility sa : manaAbilityMap.get(colorint)) {
                        if (!res.get(shard).contains(sa)) {
                            res.put(shard, sa);
                        }
                    }
                }
            }
        }

        return res;
    }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa The SpellAbility to calculate for.
     * @param test test
     * @param extraMana extraMana
     * @return ManaCost
     */
    public static ManaCostBeingPaid calculateManaCost(final Cost cost, final SpellAbility sa, final Player payer, final boolean test, final int extraMana, final boolean effect) {
        Card host = sa.getHostCard();
        Zone castFromBackup = null;
        if (test && sa.isSpell() && !host.isInZone(ZoneType.Stack)) {
            castFromBackup = host.getCastFrom();
            host.setCastFrom(host.getZone() != null ? host.getZone() : null);
        }

        Cost payCosts;
        if (test) {
            payCosts = CostAdjustment.adjust(cost, sa, effect);
            // prevent asking Human when only predicting
            if (!payer.getController().isAI()) {
                sa.setMaxWaterbend(null);
            }
        } else {
            // when not testing CostPayment already handled raise
            payCosts = cost;
        }
        CostPartMana manapart = payCosts != null ? payCosts.getCostMana() : null;
        final ManaCost mana = payCosts != null ? ( manapart == null ? ManaCost.ZERO : manapart.getManaCostFor(sa) ) : ManaCost.NO_COST;

        ManaCostBeingPaid manaCost = new ManaCostBeingPaid(mana);

        // Tack xMana Payments into mana here if X is a set value
        if (manaCost.getXcounter() > 0 || extraMana > 0) {
            int manaToAdd = 0;
            int xCounter = manaCost.getXcounter();
            if (test && extraMana > 0) {
                final int multiplicator = Math.max(xCounter, 1);
                manaToAdd = extraMana * multiplicator;
            } else {
                manaToAdd = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("XAlternative", "X"), sa) * xCounter;
            }

            if (manaToAdd < 1 && payCosts != null && payCosts.getCostMana().getXMin() > 0) {
                // AI cannot really handle X costs properly but this keeps AI from violating rules
                manaToAdd = 1;
            }

            String xColor = sa.getXColor();
            if (xColor == null) {
                xColor = "1";
            }
            if (host.hasKeyword("Spend only colored mana on X. No more than one mana of each color may be spent this way.")) {
                xColor = "WUBRGX";
            }
            if (xCounter > 0) {
                manaCost.setXManaCostPaid(manaToAdd / xCounter, xColor);
            } else {
                manaCost.increaseShard(ManaCostShard.parseNonGeneric(xColor), manaToAdd);
            }

            if (!test) {
                sa.setXManaCostPaid(manaToAdd / xCounter);
            }
        }

        CostAdjustment.adjust(manaCost, sa, payer, null, test, effect);

        if ("NumTimes".equals(sa.getParam("Announce"))) { // e.g. the Adversary cycle
            ManaCost mkCost = sa.getPayCosts().getTotalMana();
            ManaCost mCost = ManaCost.ZERO;
            for (int i = 0; i < 10; i++) {
                mCost = ManaCost.combine(mCost, mkCost);
                ManaCostBeingPaid mcbp = new ManaCostBeingPaid(mCost);
                if (!canPayManaCost(mcbp, sa, sa.getActivatingPlayer(), true)) {
                    host.setSVar("NumTimes", "Number$" + i);
                    break;
                }
            }
        }

        if (test && sa.isSpell() && !host.isInZone(ZoneType.Stack)) {
            host.setCastFrom(castFromBackup);
        }

        return manaCost;
    }

    // This method can be used to estimate the total amount of mana available to the player,
    // including the mana available in that player's mana pool
    public static int getAvailableManaEstimate(final Player p) {
        return getAvailableManaEstimate(p, true);
    }
    public static int getAvailableManaEstimate(final Player p, final boolean checkPlayable) {
        int availableMana = 0;

        final List<Card> srcs = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> !c.getManaAbilities().isEmpty());

        int maxProduced = 0;
        int producedWithCost = 0;
        boolean hasSourcesWithNoManaCost = false;

        for (Card src : srcs) {
            maxProduced = 0;

            for (SpellAbility ma : src.getManaAbilities()) {
                ma.setActivatingPlayer(p);
                if (!checkPlayable || ma.canPlay()) {
                    int costsToActivate = ma.getPayCosts().getCostMana() != null ? ma.getPayCosts().getCostMana().convertAmount() : 0;
                    int producedMana = ma.getParamOrDefault("Produced", "").split(" ").length;
                    int producedAmount = AbilityUtils.calculateAmount(src, ma.getParamOrDefault("Amount", "1"), ma);

                    int producedTotal = producedMana * producedAmount - costsToActivate;

                    if (costsToActivate > 0) {
                        producedWithCost += producedTotal;
                    } else if (!hasSourcesWithNoManaCost) {
                        hasSourcesWithNoManaCost = true;
                    }

                    if (producedTotal > maxProduced) {
                        maxProduced = producedTotal;
                    }
                }
            }

            availableMana += maxProduced;
        }

        availableMana += p.getManaPool().totalMana();

        if (producedWithCost > 0 && !hasSourcesWithNoManaCost) {
            availableMana -= producedWithCost; // probably can't activate them, no other mana available
        }

        return availableMana;
    }

    public static CardCollection getAvailableManaSources(final Player ai, final boolean checkPlayable) {
        final CardCollectionView list = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));
        final List<Card> manaSources = CardLists.filter(list, c -> {
            for (final SpellAbility am : getAIPlayableMana(c)) {
                am.setActivatingPlayer(ai);
                if (!checkPlayable || (am.canPlay() && am.checkRestrictions(ai))) {
                    return true;
                }
            }
            return false;
        });

        final CardCollection sortedManaSources = new CardCollection();
        final CardCollection otherManaSources = new CardCollection();
        final CardCollection useLastManaSources = new CardCollection();
        final CardCollection colorlessManaSources = new CardCollection();
        final CardCollection oneManaSources = new CardCollection();
        final CardCollection twoManaSources = new CardCollection();
        final CardCollection threeManaSources = new CardCollection();
        final CardCollection fourManaSources = new CardCollection();
        final CardCollection fiveManaSources = new CardCollection();
        final CardCollection anyColorManaSources = new CardCollection();

        // Sort mana sources
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color many
        // 4. all other sources (creature, costs, drawback, etc.)
        for (Card card : manaSources) {
            // exclude creature sources that will tap as a part of an attack declaration
            if (card.isCreature()) {
                if (card.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai)) {
                    Combat combat = card.getGame().getCombat();
                    if (combat.getAttackers().indexOf(card) != -1 && !card.hasKeyword(Keyword.VIGILANCE)) {
                        continue;
                    }
                }
            }
            // exclude cards that will deal lethal damage when tapped
            if (ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife()) {
                boolean dealsLethalOnTap = false;
                for (Trigger t : card.getTriggers()) {
                    if (t.getMode() == TriggerType.Taps || t.getMode() == TriggerType.TapsForMana) {
                        SpellAbility trigSa = t.getOverridingAbility();
                        if (trigSa.getApi() == ApiType.DealDamage && trigSa.getParamOrDefault("Defined", "").equals("You")) {
                            int numDamage = AbilityUtils.calculateAmount(card, trigSa.getParam("NumDmg"), null);
                            numDamage = ai.staticReplaceDamage(numDamage, card, false);
                            if (ai.getLife() <= numDamage) {
                                dealsLethalOnTap = true;
                                break;
                            }
                        }
                    }
                }
                if (dealsLethalOnTap) {
                    continue;
                }
            }

            // High-efficiency mana creatures (2+ mana per tap, e.g. Fyndhorn Elder, Fanatic of
            // Rhonas with ferocious active) are as valuable as multi-mana lands and should compete with
            // them rather than always tapping last. One-mana dorks (Llanowar Elves, Birds) stay behind
            // lands in otherManaSources.
            int maxCreatureMana = 0;
            if (card.isCreature() || card.isEnchanted()) {
                if (card.isCreature()) {
                    for (final SpellAbility m : getAIPlayableMana(card)) {
                        m.setActivatingPlayer(ai);
                        if (checkPlayable && !m.canPlay()) {
                            continue;
                        }
                        maxCreatureMana = Math.max(maxCreatureMana, m.amountOfManaGenerated(true));
                    }
                }
                if (maxCreatureMana < 2) {
                    otherManaSources.add(card);
                    continue; // don't use weak creatures before other permanents
                }
                // else fall through to normal bucketing by mana output tier
            }

            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean unpreferredCost = false;
            boolean producesAnyColor = false;
            final List<SpellAbility> manaAbilities = getAIPlayableMana(card);

            for (final SpellAbility m : manaAbilities) {
                if (m.getManaPart().isAnyMana()) {
                    producesAnyColor = true;
                }

                final Cost cost = m.getPayCosts();

                if (cost != null) {
                    // if the AI can't pay the additional costs skip the mana ability
                    m.setActivatingPlayer(ai);
                    if (!CostPayment.canPayAdditionalCosts(m.getPayCosts(), m, false)) {
                        continue;
                    }

                    if (!cost.isReusuableResource()) {
                        for (CostPart part : cost.getCostParts()) {
                            if (part instanceof CostSacrifice && !part.payCostFromSource()) {
                                unpreferredCost = true;
                            }
                        }
                        needsLimitedResources = !unpreferredCost;
                    }
                }

                AbilitySub sub = m.getSubAbility();
                // We really shouldn't be hardcoding names here. ChkDrawback should just return true for them
                if (sub != null && !card.getName().equals("Pristine Talisman") && !card.getName().equals("Zhur-Taa Druid")) {
                    if (!SpellApiToAi.Converter.get(sub).chkDrawbackWithSubs(ai, sub).willingToPlay()) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good drawbacks (gainLife)
                }
                usableManaAbilities++;
            }

            if (unpreferredCost) {
                useLastManaSources.add(card);
            } else if (needsLimitedResources) {
                otherManaSources.add(card);
            } else if (producesAnyColor) {
                anyColorManaSources.add(card);
            } else {
                // For high-efficiency creatures a single ability can make several mana; bucket them by mana
                // output rather than ability count so they sort alongside comparable multi-mana lands.
                int tier = Math.max(usableManaAbilities, maxCreatureMana);
                if (tier == 1) {
                    if (manaAbilities.get(0).getManaPart().mana(manaAbilities.get(0)).equals("C")) {
                        colorlessManaSources.add(card);
                    } else {
                        oneManaSources.add(card);
                    }
                } else if (tier == 2) {
                    twoManaSources.add(card);
                } else if (tier == 3) {
                    threeManaSources.add(card);
                } else if (tier == 4) {
                    fourManaSources.add(card);
                } else {
                    fiveManaSources.add(card);
                }
            }
        }
        sortedManaSources.addAll(sortedManaSources.size(), colorlessManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), oneManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), twoManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), threeManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fourManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fiveManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), anyColorManaSources);
        //use better creatures later
        ComputerUtilCard.sortByEvaluateCreature(otherManaSources);
        Collections.reverse(otherManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), otherManaSources);
        // This should be things like sacrifice other stuff.
        ComputerUtilCard.sortByEvaluateCreature(useLastManaSources);
        Collections.reverse(useLastManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), useLastManaSources);

        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sortedManaSources = " + sortedManaSources);
        }
        return sortedManaSources;
    }

    private static ListMultimap<Integer, SpellAbility> groupSourcesByManaColor(final Player ai, boolean checkPlayable) {
        final ListMultimap<Integer, SpellAbility> manaMap = ArrayListMultimap.create();
        final Game game = ai.getGame();

        for (final Card sourceCard : getAvailableManaSources(ai, checkPlayable)) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor sourceCard = " + sourceCard);
            }
            for (final SpellAbility m : getAIPlayableMana(sourceCard)) {
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor m = " + m);
                }
                m.setActivatingPlayer(ai);
                if (checkPlayable && !m.canPlay()) {
                    continue;
                }

                // don't kill yourself
                final Cost abCost = m.getPayCosts();
                if (!ComputerUtilCost.checkLifeCost(ai, abCost, sourceCard, 1, m)) {
                    continue;
                }

                // don't use abilities with dangerous drawbacks
                // TODO this has already been checked earlier
                AbilitySub sub = m.getSubAbility();
                if (sub != null && !SpellApiToAi.Converter.get(sub).chkDrawbackWithSubs(ai, sub).willingToPlay()) {
                    continue;
                }

                manaMap.put(ManaAtom.GENERIC, m);

                SpellAbility tail = m;
                while (tail != null) {
                    AbilityManaPart mp = tail.getManaPart();
                    if (mp != null && tail.metConditions()) {
                        // TODO Replacement Check currently doesn't work for reflected colors

                        // setup produce mana replacement effects
                        String origin = mp.getOrigProduced();
                        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(sourceCard);
                        repParams.put(AbilityKey.Mana, origin);
                        repParams.put(AbilityKey.Activator, ai);
                        repParams.put(AbilityKey.AbilityMana, m); // RootAbility

                        List<ReplacementEffect> reList = game.getReplacementHandler().getReplacementList(ReplacementType.ProduceMana, repParams, ReplacementLayer.Other);

                        if (reList.isEmpty()) {
                            Set<String> reflectedColors = CardUtil.getReflectableManaColors(m);
                            // find possible colors
                            for (MagicColor.Color color : MagicColor.Color.values()) {
                                if (mp.canProduce(color.getShortName(), tail) || reflectedColors.contains(color.getName())) {
                                    manaMap.put((int) ManaAtom.fromName(color.getName()), m);
                                }
                            }
                        } else {
                            // try to guess the color the mana gets replaced to
                            for (ReplacementEffect re : reList) {
                                SpellAbility o = re.getOverridingAbility();
                                String replaced = origin;
                                if (o == null || o.getApi() != ApiType.ReplaceMana) {
                                    continue;
                                }
                                if (o.hasParam("ReplaceMana")) {
                                    replaced = o.getParam("ReplaceMana");
                                } else if (o.hasParam("ReplaceType")) {
                                    String color = o.getParam("ReplaceType");
                                    for (byte c : MagicColor.WUBRGC) {
                                        String s = MagicColor.toShortString(c);
                                        replaced = replaced.replace(s, color);
                                    }
                                } else if (o.hasParam("ReplaceColor")) {
                                    String color = o.getParam("ReplaceColor");
                                    if (o.hasParam("ReplaceOnly")) {
                                        replaced = replaced.replace(o.getParam("ReplaceOnly"), color);
                                    } else {
                                        for (byte c : MagicColor.WUBRG) {
                                            String s = MagicColor.toShortString(c);
                                            replaced = replaced.replace(s, color);
                                        }
                                    }
                                }

                                for (byte color : MagicColor.WUBRG) {
                                    if ("Any".equals(replaced) || replaced.contains(MagicColor.toShortString(color))) {
                                        manaMap.put((int)color, m);
                                    }
                                }

                                if (replaced.contains("C")) {
                                    manaMap.put(ManaAtom.COLORLESS, m);
                                }
                            }
                        }
                    }
                    tail = tail.getSubAbility();
                }

                if (m.getHostCard().isSnow()) {
                    manaMap.put(ManaAtom.IS_SNOW, m);
                }
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor manaMap  = " + manaMap);
                }
            } // end of mana abilities loop
        } // end of mana sources loop

        return manaMap;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player, final boolean effect) {
        int max = 99;
        if (sa.hasParam("XMax")) {
            max = Math.min(max, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("XMax"), sa));
        }
        if (sa.hasParam("AIXMax")) {
            // when maximum depends on X calculate once before to avoid running more expensive checks for higher limit
            sa.setXManaCostPaid(max);
            max = Math.min(max, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("AIXMax"), sa));
        }
        for (int i = 1; i <= max; i++) {
            if (!canPayManaCost(sa.getRootAbility(), player, i, effect)) {
                return i - 1;
            }
        }
        return max;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param shardColor
     *            a mana shard to specifically test for.
     * @return a int.
     * @since 1.5.59
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player, final String shardColor, final boolean effect) {
        ManaCost origCost = sa.getRootAbility().getPayCosts().getTotalMana();

        String shardSurplus = shardColor;
        for (int i = 1; i < 100; i++) {
            ManaCost extra = new ManaCost(shardSurplus);
            if (!canPayManaCost(new ManaCostBeingPaid(ManaCost.combine(origCost, extra)), sa, player, effect)) {
                return i - 1;
            }
            shardSurplus += " " + shardColor;
        }
        return 99;
    }

    // Returns basic mana abilities plus "reflected mana" abilities
    /**
     * <p>
     * getAIPlayableMana.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<SpellAbility> getAIPlayableMana(Card c) {
        final List<SpellAbility> res = new ArrayList<>();
        for (final SpellAbility a : c.getManaAbilities()) {
            // if there is a parent ability the AI can't use it
            if (a.getApi() != ApiType.Mana && a.getApi() != ApiType.ManaReflected) {
                continue;
            }

            final Cost cost = a.getPayCosts();
            // Generic ({1}) and hybrid-only ({U/R}) activation costs are supported via nested payment
            // planning. Single-colored, X, and phyrexian activation costs are still excluded.
            if (cost.hasManaCost() && !hasPlannableManaActivationCost(cost)) {
                continue;
            }

            if (a.getRestrictions() != null && a.getRestrictions().isInstantSpeed()) {
                continue;
            }

            if (!res.contains(a)) {
                if (cost.isReusuableResource()) {
                    res.add(0, a);
                } else {
                    res.add(res.size(), a);
                }
            }
        }
        return res;
    }

    // True when the ability's mana cost is only generic mana (e.g. {1}, {2}) with no colored, hybrid, or X pips.
    private static boolean hasOnlyGenericManaCost(final Cost cost) {
        final CostPartMana manaCost = cost.getCostMana();
        if (manaCost == null) {
            return true;
        }
        final ManaCost mc = manaCost.getMana();
        return mc.getColorProfile() == 0 && mc.countX() == 0;
    }

    /**
     * True when nested payment planning can pay this ability's mana activation cost: generic-only
     * (signets, Skycloud Expanse) or hybrid-only (Cascade Bluffs, Flooded Grove).
     */
    private static boolean hasPlannableManaActivationCost(final Cost cost) {
        if (hasOnlyGenericManaCost(cost)) {
            return true;
        }
        final CostPartMana costMana = cost.getCostMana();
        if (costMana == null) {
            return true;
        }
        final ManaCost mc = costMana.getMana();
        if (mc.countX() > 0 || mc.getGenericCost() > 0) {
            return false;
        }
        boolean hasHybridShard = false;
        for (final ManaCostShard shard : mc) {
            hasHybridShard = true;
            if (shard.isPhyrexian() || shard.isOr2Generic() || shard.isMonoColor() || !shard.isMultiColor()) {
                return false;
            }
        }
        return hasHybridShard;
    }

    /**
     * Matches list of creatures to shards in mana cost for convoking.
     *
     * @param cost      cost of convoked ability
     * @param list      creatures to be evaluated
     * @param artifacts
     * @param creatures
     * @return map between creatures and shards to convoke
     */
    public static Map<Card, ManaCostShard> getConvokeOrImproviseFromList(final ManaCost cost, List<Card> list, boolean artifacts, boolean creatures) {
        final Map<Card, ManaCostShard> convoke = new HashMap<>();
        Card convoked = null;
        if (creatures && !artifacts) {
            // Run for convoke but not improvise or waterbending
            for (ManaCostShard toPay : cost) {
                if (toPay.isSnow() || toPay.isColorless()) {
                    continue;
                }
                for (Card c : list) {
                    final int mask = c.getColor().getColor() & toPay.getColorMask();
                    if (mask != 0) {
                        convoked = c;
                        convoke.put(c, toPay);
                        break;
                    }
                }
                if (convoked != null) {
                    list.remove(convoked);
                }
                convoked = null;
            }
        }
        for (int i = 0; i < list.size() && i < cost.getGenericCost(); i++) {
            convoke.put(list.get(i), ManaCostShard.GENERIC);
        }
        return convoke;
    }
}
