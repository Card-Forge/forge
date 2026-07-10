package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.ai.AiCardMemory.MemorySet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPayment;
import forge.game.cost.CostTap;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * Plans payments that the greedy mana payer cannot safely evaluate, especially mana abilities with
 * reusable mana activation costs (signets, filter lands) and mana with spell restrictions.
 *
 * The search state is deliberately virtual: it records floating mana, sources already consumed,
 * and the mana abilities to replay later. It does not tap cards while searching. A found Plan
 * replays those choices against the real game state after the search has finished.
 */
final class ManaPaymentPlanner {
    private static final int DEFAULT_MAX_STATES = 10000;
    private static final int FUTURE_CASTABILITY_PENALTY = 2000;
    private static final int MAX_FUTURE_SCORING_CANDIDATES = 16;
    static int maxStates = DEFAULT_MAX_STATES;

    private ManaPaymentPlanner() {
    }

    static boolean shouldUse(final Player ai, final boolean checkPlayable, final ManaCostBeingPaid cost,
            final SpellAbility paidFor) {
        return hasManaAbility(ai, checkPlayable, (ma, abilityCost) -> isPlannableManaAbility(ma, abilityCost)
                && canHelpPayCurrentCost(ma, ai, cost, paidFor, checkPlayable));
    }

    static boolean hasCostedManaAbility(final Player ai, final boolean checkPlayable) {
        return hasManaAbility(ai, checkPlayable, (ma, cost) -> isSupportedActivationCost(cost)
                && cost.isReusuableResource() && cost.hasManaCost() && cost.hasTapCost());
    }

    private static boolean hasManaAbility(final Player ai, final boolean checkPlayable,
            final BiPredicate<SpellAbility, Cost> predicate) {
        for (final Card source : CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand))) {
            for (final SpellAbility ma : source.getManaAbilities()) {
                ma.setActivatingPlayer(ai);
                Cost cost = ma.getPayCosts();
                if (predicate.test(ma, cost) && (!checkPlayable || ma.canPlay())) {
                    return true;
                }
            }
        }
        return false;
    }

    static Plan findPlan(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai,
            final boolean checkPlayable, final boolean scoreFutureOptions) {
        State start = new State(new VirtualPool(Lists.newArrayList(ai.getManaPool())), new HashSet<>(),
                new ArrayList<>(), 0);
        List<SpellAbility> manaAbilities = getManaAbilities(ai, checkPlayable);
        return findPaymentPlan(new ManaCostBeingPaid(cost), start, sa, ai, checkPlayable, manaAbilities,
                scoreFutureOptions, new SearchContext(manaAbilities, maxStates));
    }

    static boolean greedyPaymentMayStrandFutureSpell(final Player ai, final SpellAbility paidFor,
            final boolean checkPlayable, final Set<Card> usedSources) {
        if (usedSources.isEmpty()) {
            return false;
        }

        for (Card card : ai.getCardsIn(ZoneType.Hand)) {
            if (card == paidFor.getHostCard()) {
                continue;
            }
            SpellAbility futureSpell = card.getFirstSpellAbility();
            if (futureSpell == null) {
                continue;
            }

            futureSpell.setActivatingPlayer(ai);
            ManaCostBeingPaid futureCost = ComputerUtilMana.calculateManaCost(futureSpell.getPayCosts(), futureSpell,
                    ai, true, 0, false);
            if (futureCost.getConvertedManaCost() == 0) {
                continue;
            }

            int[] remainingMana = getRemainingManaAfterGreedyPayment(ai, futureSpell, checkPlayable, usedSources);
            if (remainingMana[0] >= futureCost.getConvertedManaCost()
                    && !canCoverMonoColorPips(futureCost, remainingMana)) {
                return true;
            }
        }
        return false;
    }

    private static int[] getRemainingManaAfterGreedyPayment(final Player ai, final SpellAbility futureSpell,
            final boolean checkPlayable, final Set<Card> usedSources) {
        int[] result = new int[MagicColor.WUBRG.length + 1];
        for (Card source : ComputerUtilMana.getAvailableManaSources(ai, checkPlayable)) {
            if (usedSources.contains(source)) {
                continue;
            }

            int sourceAmount = 0;
            byte sourceColors = 0;
            for (SpellAbility ma : ComputerUtilMana.getAIPlayableMana(source)) {
                ma.setActivatingPlayer(ai);
                if (ma.getPayCosts().hasManaCost() || (checkPlayable && !ma.canPlay())) {
                    continue;
                }

                int amount = ma.totalAmountOfManaGenerated(futureSpell, true);
                if (amount <= 0) {
                    continue;
                }
                sourceAmount = Math.max(sourceAmount, amount);
                sourceColors |= getManaAbilityColorMask(ma, futureSpell);
            }

            if (sourceAmount > 0) {
                result[0] += sourceAmount;
                for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                    if ((sourceColors & MagicColor.WUBRG[i]) != 0) {
                        result[i + 1] += sourceAmount;
                    }
                }
            }
        }
        return result;
    }

    private static boolean canCoverMonoColorPips(final ManaCostBeingPaid cost, final int[] remainingMana) {
        int[] needed = new int[MagicColor.WUBRG.length];
        for (ManaCostShard shard : cost.getUnpaidShards()) {
            if (!shard.isMonoColor()) {
                continue;
            }
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                if (shard.getColorMask() == MagicColor.WUBRG[i] && ++needed[i] > remainingMana[i + 1]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isPlannableManaAbility(final SpellAbility ma, final Cost cost) {
        if (!ma.isManaAbility()) {
            return false;
        }

        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        return isSupportedActivationCost(cost) && cost.isReusuableResource()
                && ((cost.hasManaCost() && cost.hasTapCost())
                || (manaSa != null && !manaSa.getManaPart().getManaRestrictions().isEmpty()
                && (getManaAbilityColorMask(ma) & MagicColor.ALL_COLORS) != 0));
    }

    private static boolean isSupportedActivationCost(final Cost cost) {
        if (cost == null) {
            return false;
        }
        // Virtual state tracks pool mana and one consumed source, but not permanents or cards used
        // to pay another source's activation cost.
        for (CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostPartMana) && !(part instanceof CostTap) && !part.payCostFromSource()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canHelpPayCurrentCost(final SpellAbility ma, final Player ai,
            final ManaCostBeingPaid cost, final SpellAbility paidFor, final boolean checkPlayable) {
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        String originalChoice = manaSa == null ? "" : manaSa.getManaPart().getExpressChoice();
        try {
            for (ManaCostShard shard : getPlannerShards(cost)) {
                if (ComputerUtilMana.canPayShardWithSpellAbility(shard, ai, ma, paidFor, cost, checkPlayable,
                        cost.getXManaCostPaidByColor())) {
                    return true;
                }
            }
            return false;
        } finally {
            if (manaSa != null) {
                manaSa.getManaPart().setExpressChoice(originalChoice);
            }
        }
    }

    private static Plan findPaymentPlan(final ManaCostBeingPaid cost, final State start,
            final SpellAbility paidFor, final Player ai, final boolean checkPlayable,
            final List<SpellAbility> manaAbilities, final boolean scoreFutureOptions, final SearchContext context) {
        List<State> candidates;
        try {
            candidates = findPaymentCandidates(cost, start, paidFor, ai, checkPlayable, manaAbilities,
                    scoreFutureOptions, context);
        } finally {
            context.restoreChoices();
        }
        if (candidates == null) {
            return Plan.EXHAUSTED;
        }
        State state = chooseBestPlan(candidates, paidFor, ai, checkPlayable, manaAbilities, scoreFutureOptions,
                context);
        return state == null ? null : new Plan(state.actions);
    }

    private static List<State> findPaymentCandidates(final ManaCostBeingPaid cost, final State start,
            final SpellAbility paidFor, final Player ai, final boolean checkPlayable,
            final List<SpellAbility> manaAbilities, final boolean scoreFutureOptions, final SearchContext context) {
        // Best-first search prefers lower-score plans, with the priority nudged toward states that
        // leave less of the current cost unpaid. The request-wide hard cap keeps adversarial boards bounded.
        PriorityQueue<Node> search = new PriorityQueue<>(
                Comparator.comparingInt((Node node) -> node.priority)
                        .thenComparingInt(node -> -node.state.actions.size())
                        .thenComparingInt(node -> node.state.score));
        Map<String, Integer> bestScores = new HashMap<>();
        List<State> candidates = new ArrayList<>();
        Map<Integer, String> sourceKeys = getSourceEquivalenceKeys(manaAbilities, paidFor, context);
        String startKey = manaPlannerKey(start, sourceKeys);
        bestScores.put(startKey, start.score);
        ManaCostBeingPaid remainingCost = start.pool.remainingCost(cost, paidFor, ai);
        search.add(new Node(remainingCost, start, 250 * remainingCost.getConvertedManaCost(), startKey));

        while (!search.isEmpty() && context.trySpend()) {
            Node node = search.poll();
            State state = node.state;

            Integer bestScore = bestScores.get(node.key);
            if (bestScore == null || bestScore < state.score) {
                continue;
            }

            if (state.pool.mana.size() >= cost.getConvertedManaCost()) {
                List<State> paid = payCostFromVirtualPool(new ManaCostBeingPaid(cost), state, paidFor, ai,
                        scoreFutureOptions);
                if (!scoreFutureOptions && !paid.isEmpty()) {
                    return paid;
                }
                candidates.addAll(paid);
            }

            List<ManaCostShard> shardsToTry = getPlannerShards(node.cost);
            if (shardsToTry.isEmpty()) {
                continue;
            }

            Set<String> triedManaAbilityKeys = new HashSet<>();
            for (ManaCostShard toPay : shardsToTry) {
                for (final SpellAbility ma : manaAbilities) {
                    Card source = ma.getHostCard();
                    if (source == paidFor.getHostCard() || state.hasUsedSource(source)) {
                        continue;
                    }

                    ma.setActivatingPlayer(ai);
                    if (!ComputerUtilMana.canPayShardWithSpellAbility(toPay, ai, ma, paidFor, cost, checkPlayable,
                            cost.getXManaCostPaidByColor())) {
                        continue;
                    }

                    for (String produced : predictManaChoicesForShard(ma, ai, toPay, paidFor, cost,
                            cost.getXManaCostPaidByColor())) {
                        final List<Mana> virtualMana = getVirtualMana(ma, produced, ai, context.virtualManaCache);
                        if (!canUseProducedMana(ma, virtualMana, cost, toPay, paidFor, state, manaAbilities, ai)) {
                            continue;
                        }
                        AbilityInfo info = context.abilityInfo.get(ma.getId());
                        if (!triedManaAbilityKeys.add(info.key + "|" + produced)) {
                            continue;
                        }

                        // AbilityManaPart stores chosen/combo output as mutable state. Set it long enough
                        // to let Forge's normal cost code evaluate the activation cost, then snapshot it
                        // in Action so the real payment can replay the same choice.
                        AbilityManaPart manaPart = setExpressChoice(ma, produced);
                        List<State> statesAfterActivationCost = payManaAbilityActivationCost(state, ma, ai);
                        if (statesAfterActivationCost.isEmpty()) {
                            continue;
                        }

                        for (State stateAfterActivationCost : statesAfterActivationCost) {
                            State nextState = stateAfterActivationCost.withManaAbilityResolved(
                                    new Action(ma, manaPart), virtualMana, info.score);

                            String nextKey = manaPlannerKey(nextState, sourceKeys);
                            Integer queuedScore = bestScores.get(nextKey);
                            if (queuedScore != null && queuedScore <= nextState.score) {
                                continue;
                            }
                            bestScores.put(nextKey, nextState.score);
                            remainingCost = nextState.pool.remainingCost(cost, paidFor, ai);
                            search.add(new Node(remainingCost, nextState,
                                    nextState.score + 250 * remainingCost.getConvertedManaCost(), nextKey));
                        }
                    }
                }
            }
        }

        // An empty queue proves there is no remaining path; a non-empty queue means the budget expired.
        return !search.isEmpty() && candidates.isEmpty() ? null : candidates;
    }

    private static State chooseBestPlan(final List<State> candidates, final SpellAbility paidFor,
            final Player ai, final boolean checkPlayable, final List<SpellAbility> manaAbilities,
            final boolean scoreFutureOptions, final SearchContext context) {
        List<State> plans = candidates;
        if (scoreFutureOptions && plans.size() > MAX_FUTURE_SCORING_CANDIDATES) {
            // Future scoring recursively replans other spells in hand. Keep it bounded and only apply
            // it to the strongest normal-payment candidates so equivalent sources do not stall the UI.
            plans.sort(Comparator.comparingInt((State state) -> state.score)
                    .thenComparingInt(state -> state.actions.size()));
            plans = plans.subList(0, MAX_FUTURE_SCORING_CANDIDATES);
        }

        State bestPlan = null;
        int bestPlanScore = Integer.MAX_VALUE;
        for (State candidate : plans) {
            int score = candidate.score;
            if (scoreFutureOptions) {
                Integer futureScore = scoreFutureCastability(candidate, paidFor, ai, checkPlayable, manaAbilities,
                        context);
                if (futureScore == null) {
                    return chooseBestPlan(plans, paidFor, ai, checkPlayable, manaAbilities, false, context);
                }
                score += futureScore;
            }
            if (score < bestPlanScore || (score == bestPlanScore
                    && (bestPlan == null || candidate.actions.size() < bestPlan.actions.size()))) {
                bestPlan = candidate;
                bestPlanScore = score;
            }
        }
        return bestPlan;
    }

    private static List<State> payManaAbilityActivationCost(final State state, final SpellAbility ma,
            final Player ai) {
        Cost cost = ma.getPayCosts();
        if (!CostPayment.canPayAdditionalCosts(cost, ma, false)) {
            return Collections.emptyList();
        }

        State reservedState = state.withUsedSource(ma.getHostCard());
        if (!cost.hasManaCost()) {
            return Collections.singletonList(reservedState);
        }

        ManaCostBeingPaid activationCost = ComputerUtilMana.calculateManaCost(cost, ma, ai, true, 0, ma.isTrigger());
        return payCostFromVirtualPool(activationCost, reservedState, ma, ai, true);
    }

    private static List<State> payCostFromVirtualPool(final ManaCostBeingPaid cost, final State start,
            final SpellAbility paidFor, final Player ai, final boolean findAll) {
        List<State> result = new ArrayList<>();
        List<Node> search = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        search.add(new Node(cost, start, 0, null));

        for (int index = 0; index < search.size(); index++) {
            Node node = search.get(index);
            ManaCostBeingPaid nodeCost = node.cost;
            State state = node.state;

            if (nodeCost.isPaid()) {
                result.add(state);
                if (!findAll) {
                    return result;
                }
                continue;
            }

            if (!seen.add(activationPaymentKey(nodeCost, state))) {
                continue;
            }

            Set<String> triedMana = new HashSet<>();
            for (int i = 0; i < state.pool.mana.size(); i++) {
                Mana mana = state.pool.mana.get(i);
                if (!canPayWithVirtualMana(nodeCost, mana, paidFor, ai)) {
                    continue;
                }
                if (!triedMana.add(manaKey(mana, null))) {
                    continue;
                }

                ManaCostBeingPaid nextCost = new ManaCostBeingPaid(nodeCost);
                nextCost.payMana(mana, ai.getManaPool());
                search.add(new Node(nextCost, state.withoutManaAt(i), 0, null));
            }
        }

        return result;
    }

    private static Integer scoreFutureCastability(final State state, final SpellAbility paidFor,
            final Player ai, final boolean checkPlayable, final List<SpellAbility> manaAbilities,
            final SearchContext context) {
        int score = 0;
        for (Card card : ai.getCardsIn(ZoneType.Hand)) {
            if (card == paidFor.getHostCard()) {
                continue;
            }
            SpellAbility spell = card.getFirstSpellAbility();
            if (spell == null) {
                continue;
            }

            spell.setActivatingPlayer(ai);
            ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(spell.getPayCosts(), spell, ai, true, 0, false);
            // This is a shallow future check: recursive scoring is disabled in the nested search, so
            // current plans are preferred when they leave other spells castable without exploding depth.
            Plan futurePlan = findPaymentPlan(cost, state, spell, ai, checkPlayable, manaAbilities, false, context);
            if (futurePlan != null && futurePlan.isExhausted()) {
                return null;
            }
            if (futurePlan == null) {
                score += FUTURE_CASTABILITY_PENALTY;
            }
        }
        return score;
    }

    static int getManaAbilityColorMask(final SpellAbility ma) {
        return getManaAbilityColorMask(ma, null);
    }

    static int getManaAbilityColorMask(final SpellAbility ma, final SpellAbility paidFor) {
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        if (manaSa == null) {
            return 0;
        }

        AbilityManaPart m = manaSa.getManaPart();
        if (paidFor != null && !m.meetsManaRestrictions(paidFor)) {
            return 0;
        }
        if (m.isAnyMana() || "Chosen".equals(m.getOrigProduced())) {
            Card source = ma.getHostCard();
            return source.hasChosenColor() ? MagicColor.fromName(source.getChosenColor()) : MagicColor.ALL_COLORS;
        }

        int mask = 0;
        if (manaSa.getApi() == ApiType.ManaReflected) {
            for (String color : CardUtil.getReflectableManaColors(ma)) {
                mask |= MagicColor.fromName(color);
            }
            return mask;
        }

        if (m.isComboMana()) {
            for (String color : TextUtil.split(m.getComboColors(manaSa), ' ')) {
                mask |= MagicColor.fromName(color);
            }
            return mask;
        }

        for (String mana : TextUtil.split(m.mana(manaSa), ' ')) {
            mask |= MagicColor.fromName(mana);
        }
        return mask;
    }

    static String predictManaForShard(final SpellAbility ma, final Player ai,
            final ManaCostShard toPay, final SpellAbility paidFor, final ManaCostBeingPaid cost,
            final Map<String, Integer> xManaCostPaidByColor) {
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        if (manaSa == null || !"Chosen".equals(manaSa.getManaPart().getOrigProduced())) {
            return ComputerUtilMana.predictManafromSpellAbility(ma, ai, toPay);
        }

        Card source = ma.getHostCard();
        byte[] colors = source.hasChosenColor() ? new byte[] { MagicColor.fromName(source.getChosenColor()) } : MagicColor.WUBRG;
        for (byte color : colors) {
            if (color == 0 || !ai.getManaPool().canPayForShardWithColor(toPay, color)) {
                continue;
            }
            if (toPay == ManaCostShard.COLORED_X
                    && !ManaCostBeingPaid.canColoredXShardBePaidByColor(MagicColor.toShortString(color), xManaCostPaidByColor)) {
                continue;
            }

            String produced = predictManaWithTemporaryChosenColor(ma, ai, toPay, color);
            for (Mana mana : createVirtualMana(ma, produced, ai)) {
                if (canPayWithVirtualMana(cost, mana, paidFor, ai)) {
                    manaSa.getManaPart().setExpressChoice(MagicColor.toShortString(color));
                    return produced;
                }
            }
        }
        return "";
    }

    private static List<String> predictManaChoicesForShard(final SpellAbility ma, final Player ai,
            final ManaCostShard toPay, final SpellAbility paidFor, final ManaCostBeingPaid cost,
            final Map<String, Integer> xManaCostPaidByColor) {
        String predicted = predictManaForShard(ma, ai, toPay, paidFor, cost, xManaCostPaidByColor);
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        if (manaSa == null || !manaSa.getManaPart().isComboMana()) {
            return Collections.singletonList(predicted);
        }

        List<String> result = new ArrayList<>();
        result.add(predicted);
        int amount = Math.max(1, ma.totalAmountOfManaGenerated(paidFor, true));
        if (amount == 1) {
            for (String color : TextUtil.split(manaSa.getManaPart().getComboColors(manaSa), ' ')) {
                if (!result.contains(color)) {
                    result.add(color);
                }
            }
            return result;
        }

        String color = getComboColorToRepeat(manaSa, toPay, cost, ai);
        if (color.isEmpty()) {
            return result;
        }

        String sameColor = String.join(" ", Collections.nCopies(amount, color));
        if (!result.contains(sameColor)) {
            result.add(sameColor);
        }
        return result;
    }

    private static String getComboColorToRepeat(final SpellAbility manaSa, final ManaCostShard toPay,
            final ManaCostBeingPaid cost, final Player ai) {
        if (!toPay.isGeneric()) {
            String color = MagicColor.toShortString(toPay.getColorMask());
            return manaSa.getManaPart().getComboColors(manaSa).contains(color) ? color : "";
        }

        for (String color : TextUtil.split(manaSa.getManaPart().getComboColors(manaSa), ' ')) {
            if (cost.needsColor(ManaAtom.fromName(color), ai.getManaPool())) {
                return color;
            }
        }
        return "";
    }

    private static String predictManaWithTemporaryChosenColor(final SpellAbility ma, final Player ai,
            final ManaCostShard toPay, final byte color) {
        Card source = ma.getHostCard();
        boolean hadChosenColor = source.hasChosenColor();
        List<String> oldChosenColors = hadChosenColor ? Lists.newArrayList(source.getChosenColors()) : null;
        // Chosen-color mana is implemented in Forge by reading the source card's chosen colors.
        // Mutating and restoring the card is the narrowest way to reuse that existing prediction logic.
        source.setChosenColors(Lists.newArrayList(MagicColor.toLongString(color)));
        try {
            return ComputerUtilMana.predictManafromSpellAbility(ma, ai, toPay);
        } finally {
            source.setChosenColors(hadChosenColor ? oldChosenColors : null);
        }
    }

    private static List<Mana> createVirtualMana(final SpellAbility ma, final String produced, final Player ai) {
        List<Mana> result = new ArrayList<>();
        AbilityManaPart manaPart = ComputerUtilMana.getManaPartAbility(ma).getManaPart();
        for (String mana : TextUtil.split(produced, ' ')) {
            if (mana.isEmpty()) {
                continue;
            }
            if (StringUtils.isNumeric(mana)) {
                for (int i = Integer.parseInt(mana); i > 0; i--) {
                    result.add(new Mana((byte) ManaAtom.COLORLESS, ma.getHostCard(), manaPart, ai));
                }
            } else {
                result.add(new Mana(ManaAtom.fromName(mana), ma.getHostCard(), manaPart, ai));
            }
        }
        return result;
    }

    private static List<Mana> getVirtualMana(final SpellAbility ma, final String produced, final Player ai,
            final Map<String, List<Mana>> cache) {
        String key = ma.getId() + "|" + produced;
        return cache.computeIfAbsent(key, k -> createVirtualMana(ma, produced, ai));
    }

    private static boolean canUseProducedMana(final SpellAbility ma, final List<Mana> produced,
            final ManaCostBeingPaid cost, final ManaCostShard toPay, final SpellAbility paidFor, final State state,
            final List<SpellAbility> manaAbilities, final Player ai) {
        if (canPayShardWithProducedMana(toPay, produced, paidFor, ai, cost.getXManaCostPaidByColor())) {
            return true;
        }
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        if (produced.size() != 1 || manaSa == null || manaSa.getManaPart().isComboMana()
                || ma.getHostCard().getManaAbilities().size() != 1) {
            return false;
        }

        Mana mana = produced.get(0);
        return !canPayWithVirtualMana(cost, mana, paidFor, ai)
                && canPayFutureManaAbilityActivationCost(mana, state, manaAbilities, ai);
    }

    private static boolean canPayShardWithProducedMana(final ManaCostShard toPay, final List<Mana> produced,
            final SpellAbility paidFor, final Player ai, final Map<String, Integer> xManaCostPaidByColor) {
        for (Mana mana : produced) {
            String color = MagicColor.toShortString(mana.getColor());
            if (toPay == ManaCostShard.COLORED_X
                    && !ManaCostBeingPaid.canColoredXShardBePaidByColor(color, xManaCostPaidByColor)) {
                continue;
            }
            if (mana.meetsManaRestrictions(paidFor)
                    && paidFor.allowsPayingWithShard(mana.getSourceCard(), mana.getColor())
                    && ai.getManaPool().canPayForShardWithColor(toPay, mana.getColor())) {
                return true;
            }
        }
        return false;
    }

    private static AbilityManaPart setExpressChoice(final SpellAbility ma, final String produced) {
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        if (manaSa == null) {
            return null;
        }
        AbilityManaPart manaPart = manaSa.getManaPart();
        if (manaPart.isComboMana() || manaPart.isAnyMana() || "Chosen".equals(manaPart.getOrigProduced())) {
            manaPart.setExpressChoice(produced);
        }
        return manaPart;
    }

    private static boolean canPayFutureManaAbilityActivationCost(final Mana mana, final State state,
            final List<SpellAbility> manaAbilities, final Player ai) {
        for (SpellAbility ma : manaAbilities) {
            if (state.hasUsedSource(ma.getHostCard()) || !ma.getPayCosts().hasManaCost()) {
                continue;
            }

            ManaCostBeingPaid activationCost = ComputerUtilMana.calculateManaCost(ma.getPayCosts(), ma, ai, true, 0,
                    ma.isTrigger());
            if (canPayWithVirtualMana(activationCost, mana, ma, ai)) {
                return true;
            }
        }
        return false;
    }

    private static List<SpellAbility> getManaAbilities(final Player ai, final boolean checkPlayable) {
        List<SpellAbility> result = new ArrayList<>();
        for (final Card source : ComputerUtilMana.getAvailableManaSources(ai, checkPlayable)) {
            for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(source)) {
                ma.setActivatingPlayer(ai);
                if (isSupportedActivationCost(ma.getPayCosts()) && (!checkPlayable || ma.canPlay())) {
                    result.add(ma);
                }
            }
        }
        return result;
    }

    private static boolean canPayWithVirtualMana(final ManaCostBeingPaid cost, final Mana mana,
            final SpellAbility paidFor, final Player ai) {
        return mana.meetsManaRestrictions(paidFor)
                && paidFor.allowsPayingWithShard(mana.getSourceCard(), mana.getColor())
                && cost.isNeeded(mana, ai.getManaPool());
    }

    private static List<ManaCostShard> getPlannerShards(final ManaCostBeingPaid cost) {
        List<ManaCostShard> shards = Lists.newArrayList(cost.getDistinctShards());
        shards.sort(Comparator.comparingInt(ManaCostShard::getCmc).thenComparing(ManaCostShard::name));
        List<ManaCostShard> result = new ArrayList<>();
        while (!shards.isEmpty()) {
            ManaCostShard shard = cost.getShardToPayByPriority(shards, forge.card.ColorSet.WUBRG.getColor());
            if (shard == null) {
                break;
            }
            result.add(shard);
            shards.remove(shard);
        }
        return result;
    }

    private static Map<Integer, String> getSourceEquivalenceKeys(final List<SpellAbility> manaAbilities,
            final SpellAbility paidFor, final SearchContext context) {
        Map<Integer, List<String>> keysBySource = new HashMap<>();
        for (SpellAbility ma : manaAbilities) {
            Card source = ma.getHostCard();
            int amount = Math.max(1, ma.totalAmountOfManaGenerated(paidFor, true));
            int score = 10 * context.getSourceScore(source) + 25 * amount * amount;
            String abilityKey = manaAbilitySourceKey(ma, amount, score);
            context.abilityInfo.put(ma.getId(), new AbilityInfo(abilityKey, score));
            keysBySource.computeIfAbsent(source.getId(), k -> new ArrayList<>())
                    .add(abilityKey);
        }

        Map<Integer, String> result = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : keysBySource.entrySet()) {
            Collections.sort(entry.getValue());
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    private static String manaAbilitySourceKey(final SpellAbility ma, final int amount, final int score) {
        SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
        AbilityManaPart manaPart = manaSa == null ? null : manaSa.getManaPart();
        return ma.getHostCard().getName()
                + "|" + ma.getApi()
                + "|" + (manaSa == null ? "" : manaSa.getApi())
                + "|" + ma.getPayCosts()
                + "|" + (manaPart == null ? "" : manaPart.getOrigProduced())
                + "|" + (manaPart == null ? "" : manaPart.getManaRestrictions())
                + "|" + (manaPart == null ? "" : manaPart.getExtraManaRestriction())
                + "|" + getManaAbilityColorMask(ma)
                + "|" + amount
                + "|" + score;
    }

    private static String manaPlannerKey(final State state, final Map<Integer, String> sourceKeys) {
        Map<String, Integer> usedCounts = new TreeMap<>();
        for (Integer sourceId : state.usedSourceIds) {
            usedCounts.merge(sourceKeys.getOrDefault(sourceId, "id:" + sourceId), 1, Integer::sum);
        }
        return state.pool.key(sourceKeys) + "|" + usedCounts;
    }

    private static String activationPaymentKey(final ManaCostBeingPaid cost, final State state) {
        List<Integer> used = new ArrayList<>(state.usedSourceIds);
        Collections.sort(used);
        return cost + "|" + state.pool.key(null) + "|" + used;
    }

    private static String sourceKey(final Card source, final Map<Integer, String> sourceKeys) {
        return sourceKeys.getOrDefault(source.getId(), "id:" + source.getId());
    }

    private static String manaKey(final Mana mana, final Map<Integer, String> sourceKeys) {
        String color = MagicColor.toShortString(mana.getColor());
        if (!mana.isRestricted()) {
            return color;
        }
        return color + ":" + (sourceKeys == null ? mana.getSourceCard().getName()
                : sourceKey(mana.getSourceCard(), sourceKeys));
    }

    static final class Plan {
        private static final List<Action> EXHAUSTED_ACTIONS = Collections.emptyList();
        private static final Plan EXHAUSTED = new Plan(EXHAUSTED_ACTIONS);
        private final List<Action> actions;

        private Plan(final List<Action> actions) {
            this.actions = actions;
        }

        boolean isExhausted() {
            return actions == EXHAUSTED_ACTIONS;
        }

        boolean pay(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai,
                final boolean effect, final List<Mana> manaSpentToPay) {
            ManaPool manaPool = ai.getManaPool();
            for (Action action : actions) {
                SpellAbility manaAbility = action.manaAbility;

                if (manaAbility.getPayCosts().hasTapCost()) {
                    AiCardMemory.rememberCard(ai, manaAbility.getHostCard(), MemorySet.PAYS_TAP_COST);
                }

                final CostPayment pay = new CostPayment(manaAbility.getPayCosts(), manaAbility);
                if (!pay.payComputerCosts(new AiCostDecision(ai, manaAbility, effect, true))) {
                    return false;
                }

                action.applyChoice();
                ai.getGame().getStack().addAndUnfreeze(manaAbility);
            }
            return manaPool.payManaCostFromPool(cost, sa, false, manaSpentToPay);
        }
    }

    private record State(VirtualPool pool, Set<Integer> usedSourceIds, List<Action> actions, int score) {
        private State withoutManaAt(final int index) {
            return new State(pool.without(index), usedSourceIds, actions, score);
        }

        private State withUsedSource(final Card source) {
            Set<Integer> nextUsedSources = new HashSet<>(usedSourceIds);
            nextUsedSources.add(source.getId());
            return new State(pool, nextUsedSources, actions, score);
        }

        private State withManaAbilityResolved(final Action action, final List<Mana> produced, final int actionScore) {
            List<Action> nextActions = new ArrayList<>(actions);
            nextActions.add(action);
            return new State(pool.plusAll(produced), usedSourceIds, nextActions, score + actionScore);
        }

        private boolean hasUsedSource(final Card source) {
            return usedSourceIds.contains(source.getId());
        }
    }

    private record VirtualPool(List<Mana> mana) {
        private VirtualPool without(final int index) {
            List<Mana> next = new ArrayList<>(mana);
            next.remove(index);
            return new VirtualPool(next);
        }

        private VirtualPool plusAll(final List<Mana> produced) {
            List<Mana> next = new ArrayList<>(mana);
            next.addAll(produced);
            return new VirtualPool(next);
        }

        private ManaCostBeingPaid remainingCost(final ManaCostBeingPaid cost, final SpellAbility paidFor,
                final Player ai) {
            ManaCostBeingPaid remaining = new ManaCostBeingPaid(cost);
            for (Mana m : mana) {
                if (canPayWithVirtualMana(remaining, m, paidFor, ai)) {
                    remaining.payMana(m, ai.getManaPool());
                }
            }
            return remaining;
        }

        private String key(final Map<Integer, String> sourceKeys) {
            List<String> keys = new ArrayList<>();
            for (Mana m : mana) {
                keys.add(manaKey(m, sourceKeys));
            }
            Collections.sort(keys);
            return keys.toString();
        }
    }

    private record Node(ManaCostBeingPaid cost, State state, int priority, String key) {
    }

    private record AbilityInfo(String key, int score) {
    }

    private static final class SearchContext {
        private final Map<AbilityManaPart, String> originalChoices = new HashMap<>();
        private final Map<Integer, AbilityInfo> abilityInfo = new HashMap<>();
        private final Map<Integer, Integer> sourceScores = new HashMap<>();
        private final Map<String, List<Mana>> virtualManaCache = new HashMap<>();
        private int remaining;

        private SearchContext(final List<SpellAbility> manaAbilities, final int maxStates) {
            remaining = maxStates;
            for (SpellAbility ma : manaAbilities) {
                for (AbilityManaPart manaPart : ma.getAllManaParts()) {
                    originalChoices.putIfAbsent(manaPart, manaPart.getExpressChoice());
                }
            }
        }

        private boolean trySpend() {
            return remaining-- > 0;
        }

        private int getSourceScore(final Card source) {
            return sourceScores.computeIfAbsent(source.getId(), id ->
                    ComputerUtilMana.scoreManaProducingCard(source));
        }

        private void restoreChoices() {
            originalChoices.forEach(AbilityManaPart::setExpressChoice);
        }
    }

    private record Action(SpellAbility manaAbility, AbilityManaPart manaPart, String expressChoice) {
        private Action(final SpellAbility manaAbility, final AbilityManaPart manaPart) {
            this(manaAbility, manaPart, manaPart == null ? "" : manaPart.getExpressChoice());
        }

        private void applyChoice() {
            if (manaPart != null && !expressChoice.isEmpty()) {
                manaPart.setExpressChoice(expressChoice);
            }
        }
    }
}
