package forge.game.decision;

import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.cost.Cost;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostAdjustmentPreview;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostTap;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bounded, side-effect-free existential mana-payment query for priority legality.
 *
 * <p>This class answers only whether a supported payment exists. It deliberately does not select a
 * payment, invoke a controller, mutate a mana pool, or activate a real mana ability. Unsupported
 * payment structures are reported explicitly rather than being treated as unpayable.</p>
 */
public final class PriorityCostFeasibility {
    private static final int SEARCH_NODE_LIMIT = 50_000;

    public enum Result {
        PAYABLE,
        UNPAYABLE,
        UNSUPPORTED
    }

    public enum UnsupportedReason {
        COST_TYPE,
        X_SEMANTICS,
        COST_ADJUSTMENT,
        COST_ADJUSTMENT_CHOICE_REQUIRED,
        COST_ADJUSTMENT_UNSUPPORTED,
        MANA_SOURCE_COST,
        DYNAMIC_MANA_PRODUCTION,
        MANA_RESTRICTION,
        MANA_SOURCE_ALTERNATIVE_COST,
        MANA_PRODUCTION_REPLACEMENT,
        SEARCH_LIMIT_EXCEEDED
    }

    public enum CapacityResult {
        SUPPORTED,
        UNSUPPORTED
    }

    /** Complete, cost-independent mana capacity for the fixed-output support slice. */
    public static final class CapacityAssessment {
        private final CapacityResult result;
        private final long maximumManaUnits;
        private final UnsupportedReason unsupportedReason;

        private CapacityAssessment(final CapacityResult result, final long maximumManaUnits,
                final UnsupportedReason unsupportedReason) {
            this.result = result;
            this.maximumManaUnits = maximumManaUnits;
            this.unsupportedReason = unsupportedReason;
        }

        public CapacityResult getResult() {
            return result;
        }

        public long getMaximumManaUnits() {
            return maximumManaUnits;
        }

        public UnsupportedReason getUnsupportedReason() {
            return unsupportedReason;
        }

        private static CapacityAssessment supported(final long maximumManaUnits) {
            return new CapacityAssessment(CapacityResult.SUPPORTED, maximumManaUnits, null);
        }

        private static CapacityAssessment unsupported(final UnsupportedReason reason) {
            return new CapacityAssessment(CapacityResult.UNSUPPORTED, 0L, reason);
        }
    }

    /** Immutable tri-state result of one feasibility query. */
    public static final class Assessment {
        private final Result result;
        private final UnsupportedReason unsupportedReason;
        private final CostAdjustmentPreview.Status adjustmentStatus;
        private final CostAdjustmentPreview.Reason adjustmentReason;
        private final long adjustmentPreviewNanos;

        private Assessment(final Result result, final UnsupportedReason unsupportedReason) {
            this(result, unsupportedReason, null, null, 0L);
        }

        private Assessment(final Result result, final UnsupportedReason unsupportedReason,
                final CostAdjustmentPreview.Status adjustmentStatus,
                final CostAdjustmentPreview.Reason adjustmentReason, final long adjustmentPreviewNanos) {
            this.result = result;
            this.unsupportedReason = unsupportedReason;
            this.adjustmentStatus = adjustmentStatus;
            this.adjustmentReason = adjustmentReason;
            this.adjustmentPreviewNanos = adjustmentPreviewNanos;
        }

        public Result getResult() {
            return result;
        }

        public UnsupportedReason getUnsupportedReason() {
            return unsupportedReason;
        }

        public CostAdjustmentPreview.Status getAdjustmentStatus() {
            return adjustmentStatus;
        }

        public CostAdjustmentPreview.Reason getAdjustmentReason() {
            return adjustmentReason;
        }

        public long getAdjustmentPreviewNanos() {
            return adjustmentPreviewNanos;
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Assessment)) {
                return false;
            }
            final Assessment assessment = (Assessment) other;
            return result == assessment.result && unsupportedReason == assessment.unsupportedReason;
        }

        @Override
        public int hashCode() {
            return 31 * result.hashCode() + (unsupportedReason == null ? 0 : unsupportedReason.hashCode());
        }

        private static Assessment payable() {
            return new Assessment(Result.PAYABLE, null);
        }

        private static Assessment unpayable() {
            return new Assessment(Result.UNPAYABLE, null);
        }

        private static Assessment unsupported(final UnsupportedReason reason) {
            return new Assessment(Result.UNSUPPORTED, reason);
        }

        private Assessment withAdjustmentPreview(final CostAdjustmentPreview preview, final long durationNanos) {
            return new Assessment(result, unsupportedReason, preview.getStatus(), preview.getReason(), durationNanos);
        }
    }

    /**
     * Compatibility adapter for the FRL-01A caller. New callers should retain the structured result.
     */
    public Result assess(final Player payer, final SpellAbility ability) {
        return assessPayment(payer, ability).getResult();
    }

    /**
     * Determines whether at least one complete payment exists under this deliberately bounded model.
     */
    public Assessment assessPayment(final Player payer, final SpellAbility ability) {
        final SpellAbility root = ability.getRootAbility();
        final SpellAbility probe = preparedProbe(root, payer);
        final Cost originalCost = probe.getPayCosts();
        final XValue xValue = determineExistentialX(originalCost, probe);
        return assessPayment(payer, probe, originalCost, xValue);
    }

    /** Determines whether the prepared root action has a complete supported payment for exactly {@code x}. */
    public Assessment assessPaymentAtX(final Player payer, final SpellAbility ability, final int x) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be non-negative");
        }
        final SpellAbility root = ability.getRootAbility();
        final SpellAbility probe = preparedProbe(root, payer);
        final Cost originalCost = probe.getPayCosts();
        return assessPayment(payer, probe, originalCost, new XValue(x, null));
    }

    /** Returns the complete cost-independent capacity of all currently available fixed-output resources. */
    public CapacityAssessment assessManaCapacity(final Player payer, final SpellAbility ability) {
        final SpellAbility probe = preparedProbe(ability.getRootAbility(), payer);
        if (hasPlayableAlternativeManaActivation(payer)) {
            return CapacityAssessment.unsupported(UnsupportedReason.MANA_SOURCE_ALTERNATIVE_COST);
        }
        final ManaInventory inventory = getManaInventory(payer, probe, true);
        if (inventory.unsupportedReason() != null) {
            return CapacityAssessment.unsupported(inventory.unsupportedReason());
        }
        final Map<String, Integer> largestBundleBySource = new HashMap<>();
        for (final ShadowManaSource source : inventory.sources()) {
            largestBundleBySource.merge(source.sourceKey(), source.bundle().size(), Math::max);
        }
        long maximum = inventory.floatingMana().size();
        for (final int bundleSize : largestBundleBySource.values()) {
            maximum += bundleSize;
        }
        return CapacityAssessment.supported(maximum);
    }

    private Assessment assessPayment(final Player payer, final SpellAbility ability, final Cost originalCost,
            final XValue xValue) {
        final long adjustmentStartedAtNanos = System.nanoTime();
        final CostAdjustmentPreview adjustment = CostAdjustment.preview(originalCost, ability, payer, false,
                xValue.value(), ability.getXColor());
        final long adjustmentDurationNanos = System.nanoTime() - adjustmentStartedAtNanos;
        if (adjustment.getStatus() != CostAdjustmentPreview.Status.ADJUSTED) {
            return Assessment.unsupported(adjustment.getStatus() == CostAdjustmentPreview.Status.CHOICE_REQUIRED
                    ? UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED
                    : UnsupportedReason.COST_ADJUSTMENT_UNSUPPORTED)
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        final Cost cost = adjustment.getAdjustedCost();
        if (cost == null || !cost.hasManaCost()) {
            return (cost == null || cost.canPay(ability, payer, false)
                    ? Assessment.payable() : Assessment.unpayable()).withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        if (hasNonManaXCost(cost)) {
            return Assessment.unsupported(UnsupportedReason.X_SEMANTICS)
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        if (xValue.unsupportedReason() != null) {
            return Assessment.unsupported(xValue.unsupportedReason())
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        if (!cost.canPay(ability, payer, false)) {
            return Assessment.unpayable().withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }

        final List<CostPartMana> manaParts = cost.getCostParts().stream()
                .filter(CostPartMana.class::isInstance)
                .map(CostPartMana.class::cast)
                .toList();
        if (manaParts.size() != 1 || hasDynamicManaCost(manaParts.get(0))) {
            return Assessment.unsupported(UnsupportedReason.COST_TYPE)
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }

        final ManaCost manaCost = manaParts.get(0).getManaCostFor(ability);
        if (!supportsManaCost(manaCost)) {
            return Assessment.unsupported(UnsupportedReason.COST_TYPE)
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }

        final ManaCostBeingPaid unpaid = adjustment.getAdjustedManaCost();

        final ManaInventory inventory = getManaInventory(payer, ability, false);
        final Search search = new Search(payer, ability, inventory);
        if (search.findAnyPayment(unpaid, inventory.floatingMana(), new HashSet<>())) {
            return Assessment.payable().withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        if (search.didReachLimit()) {
            return Assessment.unsupported(UnsupportedReason.SEARCH_LIMIT_EXCEEDED)
                    .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
        }
        return (inventory.unsupportedReason() == null
                ? Assessment.unpayable() : Assessment.unsupported(inventory.unsupportedReason()))
                .withAdjustmentPreview(adjustment, adjustmentDurationNanos);
    }

    private static SpellAbility preparedProbe(final SpellAbility root, final Player payer) {
        if (root.getActivatingPlayer() != null) {
            return root;
        }
        final SpellAbility copy = root.copy(payer);
        if (copy == null) {
            throw new IllegalArgumentException("Unable to create a side-effect-free prepared ability probe");
        }
        return copy;
    }

    private static boolean hasNonManaXCost(final Cost cost) {
        return cost.getCostParts().stream().anyMatch(part -> !(part instanceof CostPartMana)
                && "X".equals(part.getAmount()));
    }

    private static boolean hasDynamicManaCost(final CostPartMana manaPart) {
        return manaPart.isExiledCreatureCost() || manaPart.isEnchantedCreatureCost();
    }

    private static boolean supportsManaCost(final ManaCost manaCost) {
        for (final ManaCostShard shard : manaCost) {
            if (shard.isPhyrexian() || shard.isSnow() || shard.isOr2Generic()) {
                return false;
            }
        }
        return true;
    }

    private static XValue determineExistentialX(final Cost cost, final SpellAbility ability) {
        if (cost == null || !cost.hasManaCost() || cost.getCostMana().getAmountOfX() == 0) {
            return new XValue(null, null);
        }
        final CostPartMana manaPart = cost.getCostMana();
        final Integer announcedX = ability.getXManaCostPaid();
        if (announcedX != null) {
            return new XValue(announcedX, null);
        }
        final String declaredX = ability.getParamOrDefault("XAlternative", ability.getSVar("X"));
        if (!declaredX.isEmpty() && !"Count$xPaid".equals(declaredX)) {
            return new XValue(null, UnsupportedReason.X_SEMANTICS);
        }
        return new XValue(manaPart.getXMin(), null);
    }

    private static ManaInventory getManaInventory(final Player payer, final SpellAbility ability,
            final boolean requireCompleteFixedOutput) {
        final List<ShadowMana> floating = new ArrayList<>();
        for (final Mana mana : payer.getManaPool()) {
            floating.add(ShadowMana.fromFloating(mana));
        }

        final List<ShadowManaSource> sources = new ArrayList<>();
        UnsupportedReason unsupportedReason = null;
        for (final Card card : payer.getCardsIn(ZoneType.Battlefield)) {
            if (!payer.equals(card.getController()) || card.isTapped()) {
                continue;
            }
            final SourceOptions sourceOptions = getSourceOptions(card, payer, ability, requireCompleteFixedOutput);
            sources.addAll(sourceOptions.sources());
            if (unsupportedReason == null) {
                unsupportedReason = sourceOptions.unsupportedReason();
            }
        }
        floating.sort(Comparator.comparing(ShadowMana::semanticKey));
        sources.sort(Comparator.comparing(ShadowManaSource::semanticKey));
        return new ManaInventory(List.copyOf(floating), List.copyOf(sources), unsupportedReason);
    }

    private static SourceOptions getSourceOptions(final Card card, final Player payer, final SpellAbility ability,
            final boolean requireCompleteFixedOutput) {
        final List<ShadowManaSource> result = new ArrayList<>();
        UnsupportedReason unsupportedReason = null;
        int index = 0;
        for (final SpellAbility manaAbility : card.getManaAbilities()) {
            final SpellAbility probeAbility = manaAbility.copy(payer);
            if (probeAbility == null) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                index++;
                continue;
            }
            if (!probeAbility.canPlay()) {
                index++;
                continue;
            }
            if (!isTapOnlyManaAbility(probeAbility)) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.MANA_SOURCE_COST);
                index++;
                continue;
            }
            if (requireCompleteFixedOutput && probeAbility.getSubAbility() != null) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                index++;
                continue;
            }
            final List<AbilityManaPart> manaParts = probeAbility.getAllManaParts();
            if (manaParts.size() != 1) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                index++;
                continue;
            }
            if (requireCompleteFixedOutput && hasProduceManaReplacement(probeAbility, manaParts.get(0), payer)) {
                unsupportedReason = chooseReason(unsupportedReason,
                        UnsupportedReason.MANA_PRODUCTION_REPLACEMENT);
                index++;
                continue;
            }
            if (requireCompleteFixedOutput && manaParts.get(0).isComboMana()) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                index++;
                continue;
            }
            final List<List<ShadowMana>> bundles = staticBundles(card, manaParts.get(0));
            if (bundles == null) {
                unsupportedReason = chooseReason(unsupportedReason, UnsupportedReason.DYNAMIC_MANA_PRODUCTION);
                index++;
                continue;
            }
            final String sourceKey = sourceKey(card);
            final String sourceEquivalenceKey = card.getName() + "|" + card.getCurrentStateName();
            for (int bundleIndex = 0; bundleIndex < bundles.size(); bundleIndex++) {
                result.add(new ShadowManaSource(sourceKey, sourceEquivalenceKey, index, bundleIndex,
                        bundles.get(bundleIndex)));
            }
            index++;
        }
        return new SourceOptions(List.copyOf(result), unsupportedReason);
    }

    private static boolean hasPlayableAlternativeManaActivation(final Player payer) {
        for (final Card card : payer.getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final SpellAbility liveAbility : card.getManaAbilities()) {
                final SpellAbility baseProbe = liveAbility.copy(payer);
                if (baseProbe == null) {
                    continue;
                }
                for (final SpellAbility alternative
                        : GameActionUtil.getAlternativeCosts(baseProbe, payer, false)) {
                    if (alternative.isManaAbility() && alternative.canPlay(true)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasProduceManaReplacement(final SpellAbility manaAbility,
            final AbilityManaPart part, final Player payer) {
        final Card source = manaAbility.getHostCard();
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(source);
        runParams.put(AbilityKey.Mana, part.getOrigProduced());
        runParams.put(AbilityKey.Player, payer);
        runParams.put(AbilityKey.AbilityMana, manaAbility.getRootAbility());
        runParams.put(AbilityKey.Activator, payer);
        return !source.getGame().getReplacementHandler().getReplacementList(
                ReplacementType.ProduceMana, runParams, ReplacementLayer.Other).isEmpty();
    }

    private static UnsupportedReason chooseReason(final UnsupportedReason current, final UnsupportedReason candidate) {
        return current == null ? candidate : current;
    }

    private static boolean isTapOnlyManaAbility(final SpellAbility ability) {
        final Cost cost = ability.getPayCosts();
        return cost != null && cost.getCostParts().size() == 1 && cost.getCostParts().get(0) instanceof CostTap;
    }

    /**
     * Returns static production bundles, or null for a choice/dynamic production that must remain deferred.
     */
    private static List<List<ShadowMana>> staticBundles(final Card source, final AbilityManaPart manaPart) {
        final String produced = manaPart.getOrigProduced();
        if (produced.isEmpty() || produced.contains("Any") || produced.contains("Chosen")
                || produced.contains("Special") || produced.contains("ColorID") || produced.contains("Noted")) {
            return null;
        }
        if (manaPart.isComboMana()) {
            final String values = produced.substring("Combo".length()).trim();
            final List<List<ShadowMana>> bundles = new ArrayList<>();
            for (final String value : values.split(" ")) {
                final ShadowMana mana = parseStaticMana(value, source, manaPart);
                if (mana == null) {
                    return null;
                }
                bundles.add(List.of(mana));
            }
            return bundles.isEmpty() ? null : bundles;
        }

        final List<ShadowMana> bundle = new ArrayList<>();
        for (final String value : produced.split(" ")) {
            if (value.isEmpty()) {
                continue;
            }
            if (value.chars().allMatch(Character::isDigit)) {
                final int amount = Integer.parseInt(value);
                for (int i = 0; i < amount; i++) {
                    bundle.add(new ShadowMana((byte) ManaAtom.COLORLESS, source.isSnow(), source, manaPart));
                }
            } else {
                final ShadowMana mana = parseStaticMana(value, source, manaPart);
                if (mana == null) {
                    return null;
                }
                bundle.add(mana);
            }
        }
        return bundle.isEmpty() ? null : List.of(List.copyOf(bundle));
    }

    private static ShadowMana parseStaticMana(final String value, final Card source, final AbilityManaPart manaPart) {
        final byte color = ManaAtom.fromName(value);
        return color == 0 ? null : new ShadowMana(color, source.isSnow(), source, manaPart);
    }

    private static String sourceKey(final Card card) {
        return card.getId() + "|" + card.getGameTimestamp();
    }

    private record ShadowMana(byte color, boolean snow, Card source, AbilityManaPart manaAbility) {
        private static ShadowMana fromFloating(final Mana mana) {
            return new ShadowMana(mana.getColor(), mana.isSnow(), mana.getSourceCard(), mana.getManaAbility());
        }

        private String semanticKey() {
            return sourceKey(source) + "|" + color + "|" + (manaAbility == null ? "" : manaAbility.getOrigProduced());
        }

        private String equivalenceKey() {
            final String restrictions = manaAbility == null ? "" : manaAbility.getManaRestrictions()
                    + "|" + manaAbility.getExtraManaRestriction();
            return source.getName() + "|" + source.getCurrentStateName() + "|" + color + "|" + snow
                    + "|" + (manaAbility == null ? "" : manaAbility.getOrigProduced()) + "|" + restrictions;
        }

    }

    private record ShadowManaSource(String sourceKey, String equivalenceKey, int abilityIndex, int bundleIndex,
            List<ShadowMana> bundle) {
        private String semanticKey() {
            return sourceKey + "|" + abilityIndex + "|" + bundleIndex;
        }
    }

    private record XValue(Integer value, UnsupportedReason unsupportedReason) {
    }

    private record ManaInventory(List<ShadowMana> floatingMana, List<ShadowManaSource> sources,
            UnsupportedReason unsupportedReason) {
    }

    private record SourceOptions(List<ShadowManaSource> sources, UnsupportedReason unsupportedReason) {
    }

    private static final class Search {
        private final Player payer;
        private final SpellAbility ability;
        private final ManaInventory inventory;
        private final Set<String> visitedStates = new HashSet<>();
        private int visitedNodes;
        private boolean reachedLimit;

        private Search(final Player payer, final SpellAbility ability, final ManaInventory inventory) {
            this.payer = payer;
            this.ability = ability;
            this.inventory = inventory;
        }

        private boolean findAnyPayment(final ManaCostBeingPaid unpaid, final List<ShadowMana> available,
                final Set<String> activatedSources) {
            if (!visitedStates.add(stateKey(unpaid, available, activatedSources))) {
                return false;
            }
            if (++visitedNodes > SEARCH_NODE_LIMIT) {
                reachedLimit = true;
                return false;
            }
            if (unpaid.isPaid()) {
                return true;
            }
            for (int index = 0; index < available.size(); index++) {
                final ShadowMana mana = available.get(index);
                if (!canApply(mana, unpaid)) {
                    continue;
                }
                final List<ShadowMana> remaining = new ArrayList<>(available);
                remaining.remove(index);
                for (final ManaCostBeingPaid variant : paymentVariants(mana, unpaid)) {
                    if (findAnyPayment(variant, List.copyOf(remaining), activatedSources)) {
                        return true;
                    }
                }
            }
            for (final ShadowManaSource source : inventory.sources()) {
                if (activatedSources.contains(source.sourceKey())) {
                    continue;
                }
                final List<ShadowMana> withBundle = new ArrayList<>(available);
                withBundle.addAll(source.bundle());
                withBundle.sort(Comparator.comparing(ShadowMana::semanticKey));
                final Set<String> nextActivatedSources = new HashSet<>(activatedSources);
                nextActivatedSources.add(source.sourceKey());
                if (findAnyPayment(unpaid, List.copyOf(withBundle), nextActivatedSources)) {
                    return true;
                }
            }
            return false;
        }

        private String stateKey(final ManaCostBeingPaid unpaid, final List<ShadowMana> available,
                final Set<String> activatedSources) {
            final StringBuilder key = new StringBuilder(unpaid.toString(true, payer.getManaPool()));
            key.append('|');
            for (final ShadowMana mana : available) {
                key.append(mana.equivalenceKey()).append(';');
            }
            key.append('|');
            activatedSources.stream().map(this::sourceEquivalenceKey).sorted()
                    .forEach(source -> key.append(source).append(';'));
            return key.toString();
        }

        private String sourceEquivalenceKey(final String sourceKey) {
            for (final ShadowManaSource source : inventory.sources()) {
                if (source.sourceKey().equals(sourceKey)) {
                    return source.equivalenceKey();
                }
            }
            return sourceKey;
        }

        private boolean canApply(final ShadowMana mana, final ManaCostBeingPaid unpaid) {
            if (mana.manaAbility() != null && !mana.manaAbility().meetsManaRestrictions(ability)) {
                return false;
            }
            if (!ability.allowsPayingWithShard(mana.source(), mana.color())) {
                return false;
            }
            return !unpaid.getPaymentVariants(mana.color(), mana.snow(), mana.manaAbility(), payer.getManaPool()).isEmpty();
        }

        private List<ManaCostBeingPaid> paymentVariants(final ShadowMana mana, final ManaCostBeingPaid unpaid) {
            return unpaid.getPaymentVariants(mana.color(), mana.snow(), mana.manaAbility(), payer.getManaPool());
        }

        private boolean didReachLimit() {
            return reachedLimit;
        }
    }
}
