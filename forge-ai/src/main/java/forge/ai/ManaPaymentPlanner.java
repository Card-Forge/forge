package forge.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

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
    private static final int MAX_PAYMENT_CANDIDATES = 16;
    // Source scores are scaled by ten; preserve a colored option over ordinary 13-point utility.
    private static final int COLOR_FLEXIBILITY_PENALTY = 150;
    static int maxStates = DEFAULT_MAX_STATES;
    private final Player ai;
    private final boolean checkPlayable;
    private final List<SpellAbility> manaAbilities;
    private final Map<AbilityManaPart, String> originalChoices = new HashMap<>();
    private final Map<Integer, AbilityInfo> abilityInfo = new HashMap<>();
    private final Map<Integer, Integer> sourceScores = new HashMap<>();
    private final Map<Integer, Integer> sourceColorMasks = new HashMap<>();
    private final Map<Integer, Integer> sourcePaymentScores = new HashMap<>();
    private final Map<Integer, Integer> sourcePotentials = new HashMap<>();
    private final Map<String, List<Mana>> virtualManaCache = new HashMap<>();
    private int remainingStates = maxStates;

    private ManaPaymentPlanner(final Player ai, final boolean checkPlayable) {
        this.ai = ai;
        this.checkPlayable = checkPlayable;
        manaAbilities = getManaAbilities(ai, checkPlayable);
        for (SpellAbility ma : manaAbilities) {
            for (AbilityManaPart manaPart : ma.getAllManaParts()) {
                originalChoices.putIfAbsent(manaPart, manaPart.getExpressChoice());
            }
        }
    }

    static boolean shouldUsePlanner(final ManaCostBeingPaid cost, final SpellAbility paidFor,
            final Player ai, final boolean checkPlayable) {
        for (Card source : CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand))) {
            for (SpellAbility ma : source.getManaAbilities()) {
                ma.setActivatingPlayer(ai);
                Cost activationCost = ma.getPayCosts();
                if (isSupportedActivationCost(activationCost) && activationCost.isReusuableResource()
                        && activationCost.hasManaCost() && activationCost.hasTapCost()
                        && (!checkPlayable || ma.canPlay())) {
                    return true;
                }
            }
        }
        return getManaAbilities(ai, checkPlayable).stream().anyMatch(ma ->
                ma.totalAmountOfManaGenerated(paidFor, true) == cost.getConvertedManaCost()
                && Integer.bitCount(getManaAbilityColorMask(ma, paidFor) & MagicColor.ALL_COLORS) > 1)
                && Arrays.stream(futureColorPips(ai, paidFor), 0, 5)
                .filter(pip -> pip > 0).count() > 1;
    }

    static Plan findPlan(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai,
            final boolean checkPlayable) {
        return new ManaPaymentPlanner(ai, checkPlayable).findPlan(cost, sa);
    }

    static Plan findBetterConsolidatingPlan(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Player ai, final boolean checkPlayable, final Set<Card> greedySources) {
        return new ManaPaymentPlanner(ai, checkPlayable).findBetterConsolidatingPlan(cost, sa, greedySources);
    }

    private Plan findPlan(final ManaCostBeingPaid cost, final SpellAbility sa) {
        List<State> candidates = findPaymentCandidates(new ManaCostBeingPaid(cost), sa);
        if (candidates == null) {
            return Plan.EXHAUSTED;
        }
        State best = candidates.stream().min(Comparator.comparingInt(State::score)).orElse(null);
        return best == null ? null : new Plan(best.actions, usedPoolSources(best));
    }

    private Plan findBetterConsolidatingPlan(final ManaCostBeingPaid cost, final SpellAbility sa,
            final Set<Card> greedySources) {
        if (greedySources.isEmpty()) {
            return null;
        }
        boolean spendsDisposableMana = greedySources.stream().anyMatch(source -> source.getManaAbilities().stream()
                .anyMatch(ma -> ComputerUtilCost.isSacrificeSelfCost(ma.getPayCosts())));
        if (!spendsDisposableMana && (greedySources.size() < 2 || greedySources.size() <= cost.getConvertedManaCost()
                && manaAbilities.stream().noneMatch(ma -> {
            Cost abilityCost = ma.getPayCosts();
            SpellAbility manaSa = ComputerUtilMana.getManaPartAbility(ma);
            return ma.totalAmountOfManaGenerated(sa, true) == cost.getConvertedManaCost()
                    || (abilityCost.hasManaCost() && abilityCost.hasTapCost()
                    && (manaSa != null && manaSa.getManaPart().isComboMana()
                    || ma.totalAmountOfManaGenerated(sa, true) > abilityCost.getCostMana().getMana().getCMC()));
        }))) {
            return null;
        }
        Set<Integer> greedySourceIds = new HashSet<>();
        greedySources.forEach(source -> greedySourceIds.add(source.getId()));

        List<State> candidates = findPaymentCandidates(new ManaCostBeingPaid(cost), sa);
        if (candidates == null) {
            return null;
        }

        int bestPotential = remainingManaPotential(greedySourceIds);
        int[] desiredPips = futureColorPips(ai, sa);
        int bestColors = remainingColorOptions(greedySourceIds, desiredPips);
        int bestScore = paymentScore(greedySourceIds);
        State best = null;
        for (State candidate : candidates) {
            int potential = remainingManaPotential(candidate.usedSourceIds);
            int colors = remainingColorOptions(candidate.usedSourceIds, desiredPips);
            // Replay an equally cheap disposable payment so the real greedy pass cannot reroute it.
            boolean better = spendsDisposableMana
                    ? candidate.score < bestScore || candidate.score == bestScore && best == null
                    : colors > bestColors || colors == bestColors && (potential > bestPotential
                    || potential == bestPotential && (candidate.score < bestScore
                    || candidate.score == bestScore && best == null));
            if (better) {
                bestColors = colors;
                bestPotential = potential;
                bestScore = candidate.score;
                best = candidate;
            }
        }
        return best == null ? null : new Plan(best.actions, usedPoolSources(best));
    }

    private static int[] futureColorPips(final Player ai, final SpellAbility paidFor) {
        CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));
        hand.remove(paidFor.getHostCard());
        return AiDeckStatistics.fromCards(hand).maxPips;
    }

    private int remainingColorOptions(final Set<Integer> usedSources, final int[] desiredPips) {
        int[] sourcesByColor = new int[MagicColor.WUBRG.length];
        int remainingSources = 0;
        boolean hasFreeSource = false;
        for (Map.Entry<Integer, Integer> source : sourceColorMasks.entrySet()) {
            if (usedSources.contains(source.getKey())) {
                continue;
            }
            remainingSources++;
            hasFreeSource |= sourcePotentials.containsKey(source.getKey());
            for (int i = 0; i < MagicColor.WUBRG.length; i++) {
                sourcesByColor[i] += (source.getValue() & MagicColor.WUBRG[i]) == 0 ? 0 : 1;
            }
        }
        int score = 0;
        for (int i = 0; i < MagicColor.WUBRG.length; i++) {
            score += Math.min(desiredPips[i], sourcesByColor[i]);
        }
        // A flexible source can satisfy only one colored pip per activation.
        return hasFreeSource ? Math.min(score, remainingSources) : 0;
    }

    private int paymentScore(final Set<Integer> usedSources) {
        return usedSources.stream().mapToInt(id -> sourcePaymentScores.getOrDefault(id, 0)).sum();
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

    private List<State> findPaymentCandidates(final ManaCostBeingPaid cost, final SpellAbility paidFor) {
        State start = new State(new VirtualPool(Lists.newArrayList(ai.getManaPool())), new HashSet<>(),
                new ArrayList<>(), 0);
        try {
            return searchPaymentCandidates(cost, start, paidFor);
        } finally {
            originalChoices.forEach(AbilityManaPart::setExpressChoice);
        }
    }

    private Set<Card> usedPoolSources(final State result) {
        Set<Mana> remaining = Collections.newSetFromMap(new IdentityHashMap<>());
        remaining.addAll(result.pool.mana);
        Set<Card> sources = new LinkedHashSet<>();
        for (Mana mana : ai.getManaPool()) {
            if (!remaining.remove(mana) && mana.getSourceCard() != null) {
                sources.add(mana.getSourceCard());
            }
        }
        return sources;
    }

    private List<State> searchPaymentCandidates(final ManaCostBeingPaid cost, final State start,
            final SpellAbility paidFor) {
        // Best-first search prefers lower-score plans, with the priority nudged toward states that
        // leave less of the current cost unpaid. The request-wide hard cap keeps adversarial boards bounded.
        PriorityQueue<Node> search = new PriorityQueue<>(
                Comparator.comparingInt((Node node) -> node.priority)
                        .thenComparingInt(node -> -node.state.actions.size())
                        .thenComparingInt(node -> node.state.score));
        Map<String, Integer> bestScores = new HashMap<>();
        Map<Integer, String> sourceKeys = getSourceEquivalenceKeys(paidFor);
        String startKey = manaPlannerKey(start, sourceKeys);
        bestScores.put(startKey, start.score);
        ManaCostBeingPaid remainingCost = start.pool.remainingCost(cost, paidFor, ai);
        search.add(new Node(remainingCost, start, 250 * remainingCost.getConvertedManaCost(), startKey));
        List<State> candidates = new ArrayList<>();

        while (!search.isEmpty() && remainingStates-- > 0) {
            Node node = search.poll();
            State state = node.state;

            Integer bestScore = bestScores.get(node.key);
            if (bestScore == null || bestScore < state.score) {
                continue;
            }

            if (state.pool.mana.size() >= cost.getConvertedManaCost()) {
                List<State> paid = payCostFromVirtualPool(new ManaCostBeingPaid(cost), state, paidFor, false);
                if (!paid.isEmpty()) {
                    candidates.addAll(paid);
                    if (candidates.size() >= MAX_PAYMENT_CANDIDATES) {
                        return candidates;
                    }
                    continue;
                }
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
                        final List<Mana> virtualMana = getVirtualMana(ma, produced, ai, virtualManaCache);
                        if (!canUseProducedMana(ma, virtualMana, cost, toPay, paidFor, state)) {
                            continue;
                        }
                        AbilityInfo info = abilityInfo.get(ma.getId());
                        if (!triedManaAbilityKeys.add(info.key + "|" + produced)) {
                            continue;
                        }

                        // AbilityManaPart stores chosen/combo output as mutable state. Set it long enough
                        // to let Forge's normal cost code evaluate the activation cost, then snapshot it
                        // in Action so the real payment can replay the same choice.
                        AbilityManaPart manaPart = setExpressChoice(ma, produced);
                        List<State> statesAfterActivationCost = payManaAbilityActivationCost(state, ma);
                        if (statesAfterActivationCost.isEmpty()) {
                            continue;
                        }

                        for (State stateAfterActivationCost : statesAfterActivationCost) {
                            List<Mana> activationMana = consumedMana(state.pool, stateAfterActivationCost.pool);
                            State nextState = stateAfterActivationCost.withManaAbilityResolved(
                                    new Action(ma, manaPart, activationMana), virtualMana,
                                    info.score + activationManaFlexibility(activationMana));

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

    private int remainingManaPotential(final Set<Integer> usedSources) {
        return sourcePotentials.entrySet().stream().filter(e -> !usedSources.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue).sum();
    }

    private List<State> payManaAbilityActivationCost(final State state, final SpellAbility ma) {
        Cost cost = ma.getPayCosts();
        if (!CostPayment.canPayAdditionalCosts(cost, ma, false)) {
            return Collections.emptyList();
        }

        State reservedState = state.withUsedSource(ma.getHostCard());
        if (!cost.hasManaCost()) {
            return Collections.singletonList(reservedState);
        }

        ManaCostBeingPaid activationCost = ComputerUtilMana.calculateManaCost(cost, ma, ai, true, 0, ma.isTrigger());
        return payCostFromVirtualPool(activationCost, reservedState, ma, true);
    }

    private static List<Mana> consumedMana(final VirtualPool before, final VirtualPool after) {
        Set<Mana> remaining = Collections.newSetFromMap(new IdentityHashMap<>());
        remaining.addAll(after.mana);
        List<Mana> consumed = new ArrayList<>();
        for (Mana mana : before.mana) {
            if (!remaining.remove(mana)) {
                consumed.add(mana);
            }
        }
        return consumed;
    }

    private int activationManaFlexibility(final List<Mana> mana) {
        int score = 0;
        for (Mana m : mana) {
            Card source = m.getSourceCard();
            if (source != null && (sourceColorMasks.getOrDefault(source.getId(), 0)
                    & MagicColor.ALL_COLORS) != 0) {
                score += COLOR_FLEXIBILITY_PENALTY;
            }
        }
        return score;
    }

    private List<State> payCostFromVirtualPool(final ManaCostBeingPaid cost, final State start,
            final SpellAbility paidFor, final boolean findAll) {
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

    private boolean canUseProducedMana(final SpellAbility ma, final List<Mana> produced,
            final ManaCostBeingPaid cost, final ManaCostShard toPay, final SpellAbility paidFor, final State state) {
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
                && canPayFutureManaAbilityActivationCost(mana, state);
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

    private boolean canPayFutureManaAbilityActivationCost(final Mana mana, final State state) {
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

    private Map<Integer, String> getSourceEquivalenceKeys(final SpellAbility paidFor) {
        Map<Integer, List<String>> keysBySource = new HashMap<>();
        for (SpellAbility ma : manaAbilities) {
            Card source = ma.getHostCard();
            int amount = Math.max(1, ma.totalAmountOfManaGenerated(paidFor, true));
            int colorMask = getManaAbilityColorMask(ma, paidFor);
            int colors = Integer.bitCount(colorMask & MagicColor.ALL_COLORS);
            sourceColorMasks.merge(source.getId(), colorMask, (a, b) -> a | b);
            int score = 10 * sourceScores.computeIfAbsent(source.getId(), id ->
                    ComputerUtilMana.scoreManaProducingCard(source)) + 25 * amount * amount + colors;
            String abilityKey = manaAbilitySourceKey(ma, amount, score);
            abilityInfo.put(ma.getId(), new AbilityInfo(abilityKey, score));
            sourcePaymentScores.merge(source.getId(), score, Math::min);
            if (!ma.getPayCosts().hasManaCost()) {
                // Preserve mana quantity first; use color breadth only to break equal-output choices.
                sourcePotentials.merge(source.getId(), 10 * amount + colors, Math::max);
            }
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
        private static final Plan EXHAUSTED = new Plan(EXHAUSTED_ACTIONS, Collections.emptySet());
        private final List<Action> actions;
        private final Set<Card> poolSources;

        private Plan(final List<Action> actions, final Set<Card> poolSources) {
            this.actions = actions;
            this.poolSources = poolSources;
        }

        boolean isExhausted() {
            return actions == EXHAUSTED_ACTIONS;
        }

        void addManaSourcesTo(final Set<Card> sources) {
            sources.addAll(poolSources);
            actions.stream().map(action -> action.manaAbility.getHostCard()).forEach(sources::add);
        }

        boolean pay(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai,
                final boolean effect, final List<Mana> manaSpentToPay) {
            ManaPool manaPool = ai.getManaPool();
            Map<Card, SpellAbility> resolvedActions = new HashMap<>();
            for (Action action : actions) {
                SpellAbility manaAbility = action.manaAbility;

                if (manaAbility.getPayCosts().hasTapCost()) {
                    AiCardMemory.rememberCard(ai, manaAbility.getHostCard(), MemorySet.PAYS_TAP_COST);
                }

                final CostPayment pay = new CostPayment(manaAbility.getPayCosts().copyWithNoMana(), manaAbility);
                if (!pay.payComputerCosts(new AiCostDecision(ai, manaAbility, effect, true))) {
                    return false;
                }

                ManaCostBeingPaid activationCost = ComputerUtilMana.calculateManaCost(
                        manaAbility.getPayCosts(), manaAbility, ai, true, 0, manaAbility.isTrigger());
                for (Mana planned : action.activationMana) {
                    Mana actual = findMatchingMana(manaPool, planned);
                    if (actual == null || !manaPool.tryPayCostWithMana(manaAbility, activationCost, actual, false)) {
                        return false;
                    }
                    manaAbility.getPayingMana().add(actual);
                    SpellAbility producer = resolvedActions.get(planned.getSourceCard());
                    if (producer != null && !manaAbility.getPayingManaAbilities().contains(producer)) {
                        manaAbility.getPayingManaAbilities().add(producer);
                    }
                }
                if (!activationCost.isPaid()) {
                    return false;
                }

                action.applyChoice();
                ai.getGame().getStack().addAndUnfreeze(manaAbility);
                resolvedActions.put(manaAbility.getHostCard(), manaAbility);
            }
            return manaPool.payManaCostFromPool(cost, sa, false, manaSpentToPay);
        }

        private static Mana findMatchingMana(final ManaPool manaPool, final Mana planned) {
            for (Mana actual : manaPool) {
                if (actual.equals(planned)) {
                    return actual;
                }
            }
            return null;
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

    private record Action(SpellAbility manaAbility, AbilityManaPart manaPart, String expressChoice,
            List<Mana> activationMana) {
        private Action(final SpellAbility manaAbility, final AbilityManaPart manaPart,
                final List<Mana> activationMana) {
            this(manaAbility, manaPart, manaPart == null ? "" : manaPart.getExpressChoice(), activationMana);
        }

        private void applyChoice() {
            if (manaPart != null && !expressChoice.isEmpty()) {
                manaPart.setExpressChoice(expressChoice);
            }
        }
    }
}
