package forge.game.decision;

import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Produces and applies one atomic, algorithm-neutral target choice from Forge's live targeting state.
 *
 * <p>Forge remains the legality oracle: this provider uses the current {@link TargetRestrictions},
 * {@link SpellAbility#canTarget(GameObject)}, {@link SpellAbility#canTargetSpellAbility(SpellAbility)}, and
 * {@code TargetChoices}. It does not score, simulate, or invoke Forge AI.</p>
 */
public final class TargetDecisionProvider {
    public enum Status {
        DECISION,
        COMPLETE,
        INVALID_TARGETING
    }

    public enum CompletionStatus {
        COMPLETE,
        INVALID_TARGETING,
        UNSUPPORTED
    }

    private long nextRequestId;

    /**
     * Purely checks whether every currently known target group can reach its mandatory minimum.
     * It deliberately does not create requests, allocate continuation indices, choose a targeting
     * player, change TargetChoices, or revisit cost adjustment.
     */
    public CompletionAssessment assessCompletion(final SpellAbility ability) {
        Objects.requireNonNull(ability);
        return assessChainCompletion(ability.getRootAbility(), null);
    }

    /**
     * Purely checks a detached candidate mode and its own sub-chain without climbing to or attaching it to the root.
     */
    public CompletionAssessment assessBranchCompletion(final AbilitySub candidateMode,
            final Player rootActivatingPlayer) {
        Objects.requireNonNull(candidateMode);
        Objects.requireNonNull(rootActivatingPlayer);
        return assessChainCompletion(candidateMode, rootActivatingPlayer);
    }

    private CompletionAssessment assessChainCompletion(final SpellAbility first,
            final Player defaultChoosingPlayer) {
        SpellAbility current = first;
        while (current != null) {
            if (current.usesTargeting()) {
                if (current.hasParam("TargetingPlayer") && current.getTargetingPlayer() == null) {
                    return CompletionAssessment.unsupported("TARGETING_PLAYER_CHOICE_REQUIRED");
                }
                final Player choosingPlayer = current.getTargetingPlayer() == null
                        ? defaultChoosingPlayer == null ? current.getActivatingPlayer() : defaultChoosingPlayer
                        : current.getTargetingPlayer();
                if (choosingPlayer == null) {
                    return CompletionAssessment.unsupported("TARGET_CHOOSER_UNKNOWN");
                }
                if (current.isDividedAsYouChoose()) {
                    return CompletionAssessment.unsupported("DIVIDED_TARGET_ALLOCATION");
                }
                if (current.getTargetRestrictions().isRandomTarget()) {
                    return CompletionAssessment.unsupported("RANDOM_TARGETING");
                }
                try {
                    final int minTargets = current.getMinTargets();
                    rejectRequiredCoupledMultiTargetingWithoutCompletionOracle(current,
                            current.getTargets().size(), minTargets);
                    if (!canCompleteMinimumTargets(current, legalTargetPrototypes(current, choosingPlayer,
                            defaultChoosingPlayer == null))) {
                        return CompletionAssessment.invalidTargeting();
                    }
                } catch (final UnsupportedTargetDecisionException ex) {
                    return CompletionAssessment.unsupported("TARGET_COMPLETION_NOT_PROVABLE");
                }
            }
            current = current.getSubAbility();
        }
        return CompletionAssessment.complete();
    }

    /**
     * Creates the next atomic target request for one live target group. A {@code null} continuation explicitly
     * represents target selection that did not originate from a selected priority action.
     */
    public Generation generateTargetRequest(final SpellAbility ability, final Player choosingPlayer,
            final ActionContinuation continuation) {
        final long startedAtNanos = System.nanoTime();
        Objects.requireNonNull(ability);
        Objects.requireNonNull(choosingPlayer);
        if (!ability.usesTargeting()) {
            throw new UnsupportedTargetDecisionException(ability, "ability does not use targeting");
        }
        if (ability.isDividedAsYouChoose()) {
            throw new UnsupportedTargetDecisionException(ability,
                    "DividedAsYouChoose requires a later allocation decision family");
        }
        if (ability.getTargetRestrictions().isRandomTarget()) {
            throw new UnsupportedTargetDecisionException(ability,
                    "random target selection is owned by Forge, not a player decision");
        }
        if (ability.getTargetingPlayer() != null && !ability.getTargetingPlayer().equals(choosingPlayer)) {
            throw new UnsupportedTargetDecisionException(ability,
                    "request chooser does not match Forge's targeting player");
        }

        final int minTargets = ability.getMinTargets();
        final int maxTargets = ability.getMaxTargets();
        final int selectedTargetCount = ability.getTargets().size();
        if (selectedTargetCount > maxTargets) {
            throw new UnsupportedTargetDecisionException(ability, "current target count exceeds Forge maximum");
        }
        rejectRequiredCoupledMultiTargetingWithoutCompletionOracle(ability, selectedTargetCount, minTargets);
        if (selectedTargetCount == maxTargets) {
            return Generation.complete(reassessCost(ability), System.nanoTime() - startedAtNanos);
        }

        final List<TargetPrototype> prototypes = legalTargetPrototypes(ability, choosingPlayer, true);
        if (!canCompleteMinimumTargets(ability, prototypes)) {
            return Generation.invalidTargeting(System.nanoTime() - startedAtNanos);
        }
        final List<LegalCandidate> candidates = new ArrayList<>();
        for (final TargetPrototype prototype : prototypes) {
            candidates.add(LegalCandidate.target(candidates.size(), prototype.kind(), prototype.target(),
                    prototype.entityId(), prototype.name(), prototype.zone(), prototype.semanticKey()));
        }
        if (ability.isMinTargetChosen()) {
            candidates.add(LegalCandidate.done(candidates.size()));
        }
        if (candidates.isEmpty()) {
            return Generation.invalidTargeting(System.nanoTime() - startedAtNanos);
        }

        final Integer subdecisionIndex = continuation == null ? null : continuation.nextSubdecisionIndex();
        final TargetDecisionContext context = new TargetDecisionContext(ability, choosingPlayer,
                targetGroupIndex(ability), selectedTargetCount, minTargets, maxTargets, continuation,
                subdecisionIndex);
        return Generation.decision(new DecisionRequest(nextRequestId++, DecisionType.TARGET, candidates, context),
                System.nanoTime() - startedAtNanos);
    }

    /** Applies a chosen candidate through Forge's existing TargetChoices structure and generates the next step. */
    public Generation apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.TARGET || request.getTargetContext() == null) {
            throw new IllegalArgumentException("Only a TARGET DecisionRequest can be applied as a target choice");
        }
        if (!request.getCandidates().contains(candidate)) {
            throw new IllegalArgumentException("Target candidate does not belong to this live request");
        }

        final TargetDecisionContext context = request.getTargetContext();
        final SpellAbility ability = context.getAbility();
        if (candidate.getTargetKind() == TargetCandidateKind.DONE) {
            if (!ability.isMinTargetChosen() || ability.isMaxTargetChosen()) {
                throw new IllegalArgumentException("DONE is not legal for the current Forge target state");
            }
            return Generation.complete(reassessCost(ability), 0L);
        }

        final GameObject target = candidate.getTarget();
        if (!isStillLegal(ability, context.getChoosingPlayer(), candidate, target)) {
            throw new IllegalArgumentException("Target candidate is no longer legal in Forge's current state");
        }
        if (!ability.getTargets().add(target)) {
            throw new UnsupportedTargetDecisionException(ability, "Forge TargetChoices cannot represent target kind "
                    + candidate.getTargetKind());
        }
        return generateTargetRequest(ability, context.getChoosingPlayer(), context.getContinuation());
    }

    private static boolean isStillLegal(final SpellAbility ability, final Player choosingPlayer,
            final LegalCandidate candidate, final GameObject target) {
        if (target == null) {
            return false;
        }
        if (candidate.getTargetKind() == TargetCandidateKind.TARGET_STACK_OBJECT) {
            if (ability.getTargets().contains(target)) {
                return false;
            }
            for (final SpellAbilityStackInstance instance : ability.getHostCard().getGame().getStack()) {
                if (instance.getId() == candidate.getTargetEntityId() && instance.getSpellAbility() == target) {
                    return ability.canTargetSpellAbility(instance.getSpellAbility());
                }
            }
            return false;
        }
        if (target instanceof Card card && !legalCardCandidates(ability, choosingPlayer).cards().contains(card)) {
            return false;
        }
        if (target instanceof Player && (ability.getTargets().contains(target)
                || legalCardCandidates(ability, choosingPlayer).mustTargetObligationActive())) {
            return false;
        }
        return ability.canTarget(target);
    }

    private static List<TargetPrototype> legalTargetPrototypes(final SpellAbility ability,
            final Player choosingPlayer, final boolean applyTargetTextChanges) {
        final TargetRestrictions restrictions = ability.getTargetRestrictions();
        final Map<String, TargetPrototype> candidates = new LinkedHashMap<>();
        final CardCandidates cardCandidates = legalCardCandidates(ability, choosingPlayer);

        final List<? extends GameObject> forgeCandidates = applyTargetTextChanges
                ? restrictions.getAllCandidates(ability) : currentCandidatesWithoutTextMutation(ability);
        for (final GameObject target : forgeCandidates) {
            if (target instanceof Card card && card.getZone() != null && card.getZone().is(ZoneType.Stack)) {
                continue;
            }
            if (target instanceof Card card && !cardCandidates.cards().contains(card)) {
                continue;
            }
            // The generic human target input toggles a player already chosen for this group; it cannot add it twice.
            if (target instanceof Player && (ability.getTargets().contains(target)
                    || cardCandidates.mustTargetObligationActive())) {
                continue;
            }
            final TargetPrototype prototype = cardOrPlayerPrototype(ability, choosingPlayer, target);
            candidates.putIfAbsent(prototype.semanticKey(), prototype);
        }
        if (restrictions.getZone().contains(ZoneType.Stack)) {
            for (final SpellAbilityStackInstance instance : ability.getHostCard().getGame().getStack()) {
                final SpellAbility target = instance.getSpellAbility();
                if (ability.getTargets().contains(target)) {
                    continue;
                }
                if (!ability.canTargetSpellAbility(target)) {
                    continue;
                }
                final Card source = instance.getSourceCard();
                requireVisible(source, choosingPlayer, ability);
                final String name = visibleCardName(source, choosingPlayer);
                final String semanticKey = TargetCandidateKind.TARGET_STACK_OBJECT.ordinal() + "|"
                        + instance.getId() + "|" + source.getId() + "|" + source.getGameTimestamp();
                candidates.putIfAbsent(semanticKey, new TargetPrototype(TargetCandidateKind.TARGET_STACK_OBJECT,
                        target, instance.getId(), name, ZoneType.Stack, semanticKey));
            }
        }

        final List<TargetPrototype> result = new ArrayList<>(candidates.values());
        result.sort(Comparator.comparing(TargetPrototype::semanticKey));
        return result;
    }

    /** The MODE callback has already applied Forge's target text changes while filtering {@code possible}. */
    private static List<GameObject> currentCandidatesWithoutTextMutation(final SpellAbility ability) {
        final List<GameObject> candidates = new ArrayList<>();
        for (final Player player : ability.getHostCard().getGame().getPlayers()) {
            if (ability.canTarget(player)) {
                candidates.add(player);
            }
        }
        for (final Card card : ability.getHostCard().getGame().getCardsIn(
                ability.getTargetRestrictions().getZone())) {
            if (ability.canTarget(card)) {
                candidates.add(card);
            }
        }
        return candidates;
    }

    /** Uses the same MustTarget filtering boundary as PlayerControllerHuman for an isolated target group. */
    private static CardCandidates legalCardCandidates(final SpellAbility ability, final Player choosingPlayer) {
        final List<Card> cards = new ArrayList<>(CardUtil.getValidCardsToTarget(ability));
        final boolean canFilter = canFilterMustTarget(ability);
        if (canFilter) {
            StaticAbilityMustTarget.filterMustTargetCards(choosingPlayer, cards, ability);
        }
        final boolean mustTargetObligationActive = canFilter
                && choosingPlayer.equals(ability.getHostCard().getController())
                && !StaticAbilityMustTarget.meetsMustTargetRestriction(ability.getRootAbility());
        return new CardCandidates(new HashSet<>(cards), mustTargetObligationActive);
    }

    /** PlayerControllerHuman defers MustTarget filtering until final validation when another group targets. */
    private static boolean canFilterMustTarget(final SpellAbility ability) {
        SpellAbility relatedAbility = ability.getParent();
        while (relatedAbility != null) {
            if (relatedAbility.usesTargeting()) {
                return false;
            }
            relatedAbility = relatedAbility.getParent();
        }
        relatedAbility = ability.getSubAbility();
        while (relatedAbility != null) {
            if (relatedAbility.usesTargeting()) {
                return false;
            }
            relatedAbility = relatedAbility.getSubAbility();
        }
        return true;
    }

    private static TargetPrototype cardOrPlayerPrototype(final SpellAbility ability, final Player choosingPlayer,
            final GameObject target) {
        if (target instanceof Card card) {
            requireVisible(card, choosingPlayer, ability);
            final ZoneType zone = card.getZone() == null ? null : card.getZone().getZoneType();
            final String semanticKey = TargetCandidateKind.TARGET_CARD.ordinal() + "|"
                    + (zone == null ? "" : zone.ordinal()) + "|" + card.getId() + "|" + card.getGameTimestamp();
            return new TargetPrototype(TargetCandidateKind.TARGET_CARD, card, card.getId(),
                    visibleCardName(card, choosingPlayer), zone, semanticKey);
        }
        if (target instanceof Player player) {
            final String semanticKey = TargetCandidateKind.TARGET_PLAYER.ordinal() + "|" + player.getId();
            return new TargetPrototype(TargetCandidateKind.TARGET_PLAYER, player, player.getId(), player.getName(),
                    null, semanticKey);
        }
        throw new UnsupportedTargetDecisionException(ability,
                "unsupported Forge target entity class " + target.getClass().getName());
    }

    private static void requireVisible(final Card card, final Player choosingPlayer, final SpellAbility ability) {
        if (!card.getView().canBeShownTo(choosingPlayer.getView())) {
            throw new UnsupportedTargetDecisionException(ability,
                    "target is not legally identifiable by the choosing player");
        }
    }

    private static String visibleCardName(final Card card, final Player choosingPlayer) {
        return card.isFaceDown() && !card.getView().canFaceDownBeShownTo(choosingPlayer.getView())
                ? "" : card.getName();
    }

    private static int targetGroupIndex(final SpellAbility ability) {
        int targetGroupIndex = 0;
        SpellAbility current = ability.getRootAbility();
        while (current != null) {
            if (current == ability) {
                return targetGroupIndex;
            }
            if (current.usesTargeting()) {
                targetGroupIndex++;
            }
            current = current.getSubAbility();
        }
        throw new UnsupportedTargetDecisionException(ability, "target group is not reachable from its root ability");
    }

    /** Mirrors TargetSelection's minimum-target preflight before exposing an atomic decision. */
    private static boolean canCompleteMinimumTargets(final SpellAbility ability,
            final List<TargetPrototype> prototypes) {
        final int remainingMinimum = ability.getMinTargets() - ability.getTargets().size();
        if (remainingMinimum <= 0) {
            return true;
        }
        if (prototypes.size() < remainingMinimum) {
            return false;
        }
        final TargetRestrictions restrictions = ability.getTargetRestrictions();
        if (!restrictions.isDifferentControllers() && !restrictions.isForEachPlayer()) {
            return true;
        }
        final Set<Player> controllers = new HashSet<>();
        for (final Card selected : ability.getTargets().getTargetCards()) {
            controllers.add(selected.getController());
        }
        for (final TargetPrototype prototype : prototypes) {
            if (prototype.target() instanceof Card card) {
                controllers.add(card.getController());
            }
        }
        return controllers.size() >= ability.getMinTargets();
    }

    /**
     * Forge exposes no side-effect-free completion oracle for these restrictions. Counting current candidates
     * cannot prove that every atomic choice can reach the required minimum, so a policy request would be unsound.
     */
    private static void rejectRequiredCoupledMultiTargetingWithoutCompletionOracle(final SpellAbility ability,
            final int selectedTargetCount, final int minTargets) {
        if (minTargets <= 1 || selectedTargetCount >= minTargets) {
            return;
        }
        final TargetRestrictions restrictions = ability.getTargetRestrictions();
        if (restrictions.isSameController()
                || restrictions.isWithoutSameCreatureType()
                || restrictions.isWithSameCreatureType()
                || restrictions.isWithSameCardType()
                || restrictions.isDifferentCMC()
                || restrictions.isDifferentNames()
                || restrictions.isEqualToughness()
                || ability.hasParam("MaxTotalTargetCMC")
                || ability.hasParam("MaxTotalTargetPower")) {
            throw new UnsupportedTargetDecisionException(ability,
                    "required coupled multi-target restriction has no side-effect-free Forge completion oracle");
        }
    }

    private static PriorityCostFeasibility.Assessment reassessCost(final SpellAbility ability) {
        final SpellAbility rootAbility = ability.getRootAbility();
        return new PriorityCostFeasibility().assessPayment(rootAbility.getActivatingPlayer(), rootAbility);
    }

    private record TargetPrototype(TargetCandidateKind kind, GameObject target, int entityId, String name,
            ZoneType zone, String semanticKey) {
    }

    private record CardCandidates(Set<Card> cards, boolean mustTargetObligationActive) {
    }

    public static final class CompletionAssessment {
        private final CompletionStatus status;
        private final String unsupportedReason;

        private CompletionAssessment(final CompletionStatus status, final String unsupportedReason) {
            this.status = status;
            this.unsupportedReason = unsupportedReason;
        }

        private static CompletionAssessment complete() {
            return new CompletionAssessment(CompletionStatus.COMPLETE, null);
        }

        private static CompletionAssessment invalidTargeting() {
            return new CompletionAssessment(CompletionStatus.INVALID_TARGETING, null);
        }

        private static CompletionAssessment unsupported(final String reason) {
            return new CompletionAssessment(CompletionStatus.UNSUPPORTED, reason);
        }

        public CompletionStatus getStatus() {
            return status;
        }

        public String getUnsupportedReason() {
            return unsupportedReason;
        }
    }

    /** Result for one atomic target operation. Only {@link Status#DECISION} contains a request. */
    public static final class Generation {
        private final Status status;
        private final DecisionRequest request;
        private final PriorityCostFeasibility.Assessment costFeasibility;
        private final long requestGenerationNanos;

        private Generation(final Status status, final DecisionRequest request,
                final PriorityCostFeasibility.Assessment costFeasibility, final long requestGenerationNanos) {
            this.status = status;
            this.request = request;
            this.costFeasibility = costFeasibility;
            this.requestGenerationNanos = requestGenerationNanos;
        }

        private static Generation decision(final DecisionRequest request, final long requestGenerationNanos) {
            return new Generation(Status.DECISION, request, null, requestGenerationNanos);
        }

        private static Generation complete(final PriorityCostFeasibility.Assessment costFeasibility,
                final long requestGenerationNanos) {
            return new Generation(Status.COMPLETE, null, costFeasibility, requestGenerationNanos);
        }

        private static Generation invalidTargeting(final long requestGenerationNanos) {
            return new Generation(Status.INVALID_TARGETING, null, null, requestGenerationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        /**
         * A fresh post-target feasibility preview after a completed target group. Forge recalculates the actual
         * adjusted cost again in CostPayment immediately before payment.
         */
        public PriorityCostFeasibility.Assessment getCostFeasibility() {
            return costFeasibility;
        }

        /** Time spent creating this request from Forge's live target state. */
        public long getRequestGenerationNanos() {
            return requestGenerationNanos;
        }
    }
}
