package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardPlayOption;
import forge.game.ability.AbilityUtils;
import forge.game.GameActionUtil;
import forge.game.ability.ApiType;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces the algorithm-neutral, top-level alternatives available to a player holding priority.
 *
 * <p>This v0 provider deliberately supports the player-visible Constructed action zones only. It does not
 * inspect opponent hidden zones and it fails loudly if a discovered action requires an unsupported payment
 * feasibility check.</p>
 */
public final class PriorityActionProvider {
    private static final List<ZoneType> ACTION_ZONES = List.of(
            ZoneType.Hand, ZoneType.Battlefield, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Command);

    private final PriorityCostFeasibility costFeasibility = new PriorityCostFeasibility();
    private long nextRequestId;

    public DecisionRequest createPriorityRequest(final Player player) {
        return generatePriorityRequest(player).getRequest();
    }

    /**
     * Package-visible diagnostic entry point. It exposes only per-assessment outcomes and timing, never a
     * preferred payment or a selected action.
     */
    Generation generatePriorityRequest(final Player player) {
        final Map<String, CandidatePrototype> actionsByKey = new LinkedHashMap<>();
        final List<FeasibilityMeasurement> feasibilityMeasurements = new ArrayList<>();
        for (final Card card : actionSources(player)) {
            for (final SpellAbility ability : topLevelAbilities(card, player)) {
                final PriorityActionKind kind = classify(ability);
                final long feasibilityStartedAtNanos = System.nanoTime();
                final PriorityCostFeasibility.Assessment feasibility = costFeasibility.assessPayment(player, ability);
                feasibilityMeasurements.add(new FeasibilityMeasurement(feasibility.getResult(),
                        feasibility.getUnsupportedReason(), System.nanoTime() - feasibilityStartedAtNanos,
                        feasibility.getAdjustmentStatus(), feasibility.getAdjustmentReason(),
                        feasibility.getAdjustmentPreviewNanos()));
                if (feasibility.getResult() == PriorityCostFeasibility.Result.UNPAYABLE) {
                    continue;
                }
                if (feasibility.getResult() == PriorityCostFeasibility.Result.UNSUPPORTED) {
                    throw new UnsupportedPriorityActionException(ability,
                            "cost feasibility is unsupported: " + feasibility.getUnsupportedReason());
                }
                final String key = semanticKey(kind, card, ability);
                actionsByKey.putIfAbsent(key, new CandidatePrototype(kind, card, ability, key));
            }
        }

        final List<CandidatePrototype> orderedActions = new ArrayList<>(actionsByKey.values());
        orderedActions.sort(Comparator.comparing(CandidatePrototype::semanticKey));

        final List<LegalCandidate> candidates = new ArrayList<>();
        candidates.add(LegalCandidate.pass(0));
        for (final CandidatePrototype action : orderedActions) {
            candidates.add(LegalCandidate.action(candidates.size(), action.kind(), action.source(), action.ability(),
                    action.semanticKey()));
        }
        return new Generation(new DecisionRequest(nextRequestId++, DecisionType.PRIORITY_ACTION, candidates),
                List.copyOf(feasibilityMeasurements));
    }

    /**
     * Expands individually exposed Forge optional costs such as Jump-start into their actual cast variants.
     * Cost combinations remain deferred: this boundary describes one top-level cast option and never chooses
     * the later discard, sacrifice, target, or payment resource.
     */
    private static List<SpellAbility> topLevelAbilities(final Card card, final Player player) {
        final Map<Integer, SpellAbility> liveAbilities = new LinkedHashMap<>();
        for (final SpellAbility ability : card.getAllSpellAbilities()) {
            liveAbilities.putIfAbsent(ability.getId(), ability);
        }
        for (final SpellAbility ability : card.getSpellAbilities()) {
            liveAbilities.putIfAbsent(ability.getId(), ability);
        }
        final List<SpellAbility> abilities = new ArrayList<>();
        for (final SpellAbility liveAbility : liveAbilities.values()) {
            final SpellAbility ability = liveAbility.copy(card, player, true);
            if (ability.canPlay()) {
                abilities.add(ability);
            }
            for (final SpellAbility mayPlay : auditMayPlayOptions(ability, card, player)) {
                if (mayPlay.canPlay()) {
                    abilities.add(mayPlay);
                }
            }
            for (final OptionalCostValue optionalCost : GameActionUtil.getOptionalCostValues(ability)) {
                final SpellAbility alternative = addAuditOptionalCost(ability, optionalCost);
                if (alternative.canPlay()) {
                    abilities.add(alternative);
                }
            }
        }
        return abilities;
    }

    private static SpellAbility addAuditOptionalCost(final SpellAbility ability,
            final OptionalCostValue optionalCost) {
        final SpellAbility result = ability.copy(ability.getHostCard(), ability.getActivatingPlayer(), true);
        if (ability.hasParam("ReduceCost")) {
            result.putParam("ReduceCost", ability.getParam("ReduceCost"));
        }
        if (ability.hasParam("RaiseCost")) {
            result.putParam("RaiseCost", ability.getParam("RaiseCost"));
        }
        if (optionalCost.getType() != OptionalCost.Offering) {
            result.getPayCosts().add(optionalCost.getCost());
        }
        result.addOptionalCost(optionalCost.getType());
        switch (optionalCost.getType()) {
        case Retrace:
        case Jumpstart:
            result.getRestrictions().setZone(ZoneType.Graveyard);
            break;
        case Flash:
        case Offering:
            result.getRestrictions().setInstantSpeed(true);
            break;
        default:
            break;
        }
        return result;
    }

    private static List<SpellAbility> auditMayPlayOptions(final SpellAbility ability, final Card source,
            final Player player) {
        final List<SpellAbility> result = new ArrayList<>();
        for (final CardPlayOption option : source.mayPlay(player)) {
            if (option.getAbility().hasParam("MayPlayNotSorcerySpeed") && player.canCastSorcery()) {
                continue;
            }
            if ((!ability.isBasicSpell() || ability.costHasManaX() && ability.getPayCosts().getCostMana() != null
                    && ability.getPayCosts().getCostMana().getXMin() > 0)
                    && option.getPayManaCost() == CardPlayOption.PayManaCost.NO) {
                continue;
            }
            if (ability.isKeyword(Keyword.WARP) && !ability.getHostCard().equals(option.getHost())) {
                continue;
            }
            final SpellAbility mayPlay = ability.copy(source, player, true);
            if (option.getPayManaCost() == CardPlayOption.PayManaCost.NO) {
                mayPlay.setPayCosts(mayPlay.getPayCosts().copyWithNoMana());
                mayPlay.putParam("WithoutManaCost", "True");
                mayPlay.setBasicSpell(false);
            } else if (option.getAltManaCost() != null) {
                final Cost replacement = mayPlay.getPayCosts().copyWithNoMana();
                replacement.add(option.getAltManaCost());
                mayPlay.setPayCosts(replacement);
                mayPlay.setBasicSpell(false);
            }
            if (option.getAbility().hasParam("ValidAfterStack")) {
                mayPlay.putParam("ValidAfterStack", option.getAbility().getParam("ValidAfterStack"));
            }
            if (option.getAbility().hasParam("RaiseCost")) {
                String raise = option.getAbility().getParam("RaiseCost");
                if (option.getAbility().hasSVar(raise)) {
                    raise = Integer.toString(AbilityUtils.calculateAmount(option.getHost(), raise,
                            option.getAbility()));
                }
                mayPlay.putParam("RaiseCost", raise);
            }
            if (option.isWithFlash()) {
                mayPlay.getRestrictions().setInstantSpeed(true);
            }
            mayPlay.getRestrictions().setZone(null);
            mayPlay.setMayPlay(option);
            result.add(mayPlay);
        }
        return result;
    }

    /**
     * Checks whether a Forge-selected top-level ability is represented by a request created for the same state.
     * This is a diagnostic coverage check; it does not apply the ability.
     */
    public boolean contains(final DecisionRequest request, final SpellAbility ability) {
        return findCandidate(request, ability) != null;
    }

    /**
     * Finds the generated candidate matching Forge's selected ability without regenerating the request.
     *
     * <p>This is diagnostic correlation only. It neither applies the ability nor performs another legality or
     * payment-feasibility assessment.</p>
     */
    LegalCandidate findCandidate(final DecisionRequest request, final SpellAbility ability) {
        if (ability == null) {
            return request.getCandidates().stream()
                    .filter(candidate -> candidate.getKind() == PriorityActionKind.PASS).findFirst().orElse(null);
        }
        final String key = semanticKey(classify(ability), ability.getHostCard(), ability);
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getSemanticKey().equals(key)).findFirst().orElse(null);
    }

    private static List<Card> actionSources(final Player player) {
        final Map<String, Card> sources = new LinkedHashMap<>();
        for (final ZoneType zone : ACTION_ZONES) {
            for (final Card card : player.getCardsIn(zone)) {
                if (zone == ZoneType.Battlefield && !player.equals(card.getController())) {
                    continue;
                }
                sources.putIfAbsent(sourceKey(card), card);
            }
        }
        // Surface an opponent-owned exiled card only when Forge grants an explicit play permission and the
        // acting player is allowed to see it. A face-down permission requires a later opaque representation.
        for (final Card card : player.getGame().getCardsIn(ZoneType.Exile)) {
            if (card.getView().canBeShownTo(player.getView()) && !card.mayPlay(player).isEmpty()) {
                sources.putIfAbsent(sourceKey(card), card);
            }
        }
        final List<Card> result = new ArrayList<>(sources.values());
        result.sort(Comparator.comparing(PriorityActionProvider::sourceKey));
        return result;
    }

    private static PriorityActionKind classify(final SpellAbility ability) {
        if (ability.isLandAbility()) {
            return PriorityActionKind.PLAY_LAND;
        }
        if (ability.isSpell()) {
            return PriorityActionKind.CAST_SPELL;
        }
        if (ability.hasParam("SpecialAction")) {
            return PriorityActionKind.SPECIAL_ACTION;
        }
        if (ability.isManaAbility()) {
            return PriorityActionKind.ACTIVATE_MANA_ABILITY;
        }
        if (ability.isActivatedAbility()) {
            return PriorityActionKind.ACTIVATE_ABILITY;
        }
        if (ability.isAbility() && !ability.isTrigger()) {
            return PriorityActionKind.SPECIAL_ACTION;
        }
        throw new UnsupportedPriorityActionException(ability, "ability kind is not a top-level priority action");
    }

    private static String semanticKey(final PriorityActionKind kind, final Card source, final SpellAbility ability) {
        final ApiType api = ability.getApi();
        return kind.ordinal() + "|" + sourceKey(source) + "|" + source.getCurrentStateName().name() + "|"
                + (api == null ? "" : api.name()) + "|" + ability.getOriginalDescription() + "|"
                + ability.getPayCosts() + "|" + ability.getMayPlay();
    }

    private static String sourceKey(final Card card) {
        final ZoneType zone = card.getZone() == null ? null : card.getZone().getZoneType();
        return (zone == null ? "" : zone.ordinal()) + "|" + card.getId() + "|" + card.getGameTimestamp();
    }

    private record CandidatePrototype(PriorityActionKind kind, Card source, SpellAbility ability, String semanticKey) {
    }

    static final class Generation {
        private final DecisionRequest request;
        private final List<FeasibilityMeasurement> feasibilityMeasurements;

        private Generation(final DecisionRequest request, final List<FeasibilityMeasurement> feasibilityMeasurements) {
            this.request = request;
            this.feasibilityMeasurements = feasibilityMeasurements;
        }

        DecisionRequest getRequest() {
            return request;
        }

        List<FeasibilityMeasurement> getFeasibilityMeasurements() {
            return feasibilityMeasurements;
        }
    }

    static final class FeasibilityMeasurement {
        private final PriorityCostFeasibility.Result result;
        private final PriorityCostFeasibility.UnsupportedReason unsupportedReason;
        private final long durationNanos;
        private final forge.game.cost.CostAdjustmentPreview.Status adjustmentStatus;
        private final forge.game.cost.CostAdjustmentPreview.Reason adjustmentReason;
        private final long adjustmentPreviewNanos;

        private FeasibilityMeasurement(final PriorityCostFeasibility.Result result,
                final PriorityCostFeasibility.UnsupportedReason unsupportedReason, final long durationNanos,
                final forge.game.cost.CostAdjustmentPreview.Status adjustmentStatus,
                final forge.game.cost.CostAdjustmentPreview.Reason adjustmentReason,
                final long adjustmentPreviewNanos) {
            this.result = result;
            this.unsupportedReason = unsupportedReason;
            this.durationNanos = durationNanos;
            this.adjustmentStatus = adjustmentStatus;
            this.adjustmentReason = adjustmentReason;
            this.adjustmentPreviewNanos = adjustmentPreviewNanos;
        }

        PriorityCostFeasibility.Result getResult() {
            return result;
        }

        PriorityCostFeasibility.UnsupportedReason getUnsupportedReason() {
            return unsupportedReason;
        }

        long getDurationNanos() {
            return durationNanos;
        }

        forge.game.cost.CostAdjustmentPreview.Status getAdjustmentStatus() {
            return adjustmentStatus;
        }

        forge.game.cost.CostAdjustmentPreview.Reason getAdjustmentReason() {
            return adjustmentReason;
        }

        long getAdjustmentPreviewNanos() {
            return adjustmentPreviewNanos;
        }
    }
}
