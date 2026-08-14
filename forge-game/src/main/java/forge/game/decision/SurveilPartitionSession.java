package forge.game.decision;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class SurveilPartitionSession {
    private final long surveilSessionId;
    private final Game game;
    private final int gameId;
    private final Player chooser;
    private final int choosingPlayerId;
    private final List<Card> nativeSnapshot;
    private final IdentityHashMap<Card, SurveilItem> nativeItems;
    private final List<SurveilItem> canonicalPolicyItems;
    private final List<SurveilPartitionCard> visibleItems;
    private final SurveilPartitionCandidateKind[] labels;
    private SurveilPartitionCandidateKind[] nativeMembershipVector;
    private final Map<String, EnumSet<SurveilPartitionCandidateKind>> symmetryLabels;
    private final Map<String, Boolean> symmetryConflicts;
    private List<Card> retainedNativeList;
    private int currentStep;
    private DecisionRequest openRequest;
    private boolean complete;
    private boolean mappingFailed;
    private boolean closed;
    private String closeReason;

    SurveilPartitionSession(final long surveilSessionId, final Player chooser,
            final List<Card> privateSnapshot) {
        this.surveilSessionId = surveilSessionId;
        this.chooser = Objects.requireNonNull(chooser, "chooser");
        this.game = Objects.requireNonNull(chooser.getGame(), "chooser game");
        this.gameId = game.getId();
        this.choosingPlayerId = chooser.getId();
        this.nativeSnapshot = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(privateSnapshot, "privateSnapshot")));
        this.nativeItems = new IdentityHashMap<>();
        this.symmetryLabels = new HashMap<>();
        this.symmetryConflicts = new HashMap<>();

        final Set<StableIdentity> stableIdentities = new HashSet<>();
        final List<SurveilItem> capturedItems = new ArrayList<>(nativeSnapshot.size());
        for (int nativeOrdinal = 0; nativeOrdinal < nativeSnapshot.size(); nativeOrdinal++) {
            final Card card = Objects.requireNonNull(nativeSnapshot.get(nativeOrdinal), "privateSnapshot card");
            if (nativeItems.containsKey(card)) {
                throw new IllegalArgumentException("Surveil snapshot contains the same native card twice");
            }
            if (!isVisibleToChooser(card, chooser)) {
                throw new IllegalArgumentException("Surveil snapshot contains a card hidden from the chooser");
            }
            final StableIdentity stableIdentity = new StableIdentity(card.getId(), card.getGameTimestamp());
            if (!stableIdentities.add(stableIdentity)) {
                throw new IllegalArgumentException("Surveil snapshot contains duplicate stable card identity");
            }

            final String visibleName = Objects.requireNonNull(card.getName(), "card visible name");
            final SurveilPartitionCard projection = new SurveilPartitionCard(0L, visibleName);
            final SurveilItem item = new SurveilItem(card, nativeOrdinal, stableIdentity, projection);
            nativeItems.put(card, item);
            capturedItems.add(item);
        }

        capturedItems.sort(Comparator
                .comparing((SurveilItem item) -> item.symmetryKey)
                .thenComparingInt(item -> item.stableIdentity.cardId())
                .thenComparingLong(item -> item.stableIdentity.gameTimestamp()));
        final List<SurveilItem> canonicalItems = new ArrayList<>(capturedItems.size());
        final List<SurveilPartitionCard> publicItems = new ArrayList<>(capturedItems.size());
        int canonicalRank = 1;
        for (final SurveilItem item : capturedItems) {
            item.itemId = SurveilPartitionItemId.opaqueItemId(canonicalRank++);
            item.projection = new SurveilPartitionCard(item.itemId, item.symmetryKey);
            canonicalItems.add(item);
            publicItems.add(item.projection);
        }
        this.canonicalPolicyItems = List.copyOf(canonicalItems);
        this.visibleItems = List.copyOf(publicItems);
        this.labels = new SurveilPartitionCandidateKind[canonicalPolicyItems.size()];
        this.retainedNativeList = List.of();
        this.complete = canonicalPolicyItems.isEmpty();
    }

    long surveilSessionId() {
        return surveilSessionId;
    }

    boolean isEmptySnapshot() {
        return nativeSnapshot.isEmpty();
    }

    boolean isComplete() {
        return complete;
    }

    boolean isClosed() {
        return closed;
    }

    boolean hasOpenRequest() {
        return openRequest != null;
    }

    boolean isIdentityStable() {
        try {
            return chooser.getId() == choosingPlayerId
                    && chooser.getGame() == game
                    && chooser.getGame().getId() == gameId;
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    DecisionRequest createMembershipRequest(final long requestId) {
        if (complete) {
            if (isEmptySnapshot()) {
                return null;
            }
            throw new IllegalStateException("Surveil session is complete");
        }
        if (closed) {
            throw new IllegalStateException("Surveil session is closed: " + closeReason);
        }
        requireNativeMembershipVector();
        if (!isIdentityStable()) {
            throw new IllegalStateException("Surveil session identity is stale");
        }
        if (openRequest != null) {
            throw new IllegalStateException("Surveil session already has an open request");
        }

        final SurveilItem currentItem = canonicalPolicyItems.get(currentStep);
        final SurveilPartitionContext context = new SurveilPartitionContext(
                SurveilPartitionProfile.SURVEIL_PARTITION, surveilSessionId, currentStep,
                choosingPlayerId, canonicalPolicyItems.size(), visibleItems, currentItem.itemId);
        final DecisionRequest request = new DecisionRequest(requestId, DecisionType.CARD_SELECTION,
                List.of(
                        LegalCandidate.surveilPartition(0,
                                SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD, currentItem.projection),
                        LegalCandidate.surveilPartition(1,
                                SurveilPartitionCandidateKind.CLASSIFY_RETAIN, currentItem.projection)),
                context);
        openRequest = request;
        return request;
    }

    void applyMembershipCandidate(final LegalCandidate candidate) {
        if (closed) {
            throw new IllegalStateException("Surveil session is closed: " + closeReason);
        }
        requireNativeMembershipVector();
        if (!isIdentityStable()) {
            throw new IllegalStateException("Surveil session identity is stale");
        }
        if (complete || openRequest == null) {
            throw new IllegalArgumentException("Surveil session has no outstanding membership request");
        }
        if (candidate == null || !belongsToOpenRequest(candidate)) {
            throw new IllegalArgumentException("Candidate does not belong to the outstanding request");
        }

        final SurveilPartitionContext context = openRequest.getSurveilPartitionContext();
        if (context == null || context.getProfile() != SurveilPartitionProfile.SURVEIL_PARTITION
                || context.getSurveilSessionId() != surveilSessionId
                || context.getChoosingPlayerId() != choosingPlayerId
                || context.getOriginalItemCount() != canonicalPolicyItems.size()
                || context.getDecisionStepIndex() != currentStep) {
            throw new IllegalArgumentException("Surveil request context does not match the session");
        }
        if (context.getVisibleItems() != visibleItems
                && !context.getVisibleItems().equals(visibleItems)) {
            throw new IllegalArgumentException("Surveil request projection does not match the session");
        }

        final SurveilItem currentItem = canonicalPolicyItems.get(currentStep);
        if (context.getCurrentItemId() != currentItem.itemId) {
            throw new IllegalArgumentException("Surveil request item does not match the session cursor");
        }
        final SurveilPartitionCandidateKind kind = candidate.getSurveilPartitionCandidateKind();
        final SurveilPartitionCard item = candidate.getSurveilPartitionCard();
        if ((kind != SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                && kind != SurveilPartitionCandidateKind.CLASSIFY_RETAIN)
                || item == null
                || item.getItemId() != currentItem.itemId
                || !candidate.getSemanticKey().equals(semanticKey(kind, currentItem.itemId))
                || candidate.getKind() != null
                || candidate.getTargetKind() != null
                || candidate.getPaymentKind() != null
                || candidate.getXValue() != null
                || candidate.getModeOrdinal() != null
                || !candidate.getModeDescription().isEmpty()
                || candidate.isModeUsesTargeting()
                || candidate.getCardSelectionKind() != null
                || candidate.getCardSelectionCard() != null
                || candidate.getAttackKind() != null
                || candidate.getAttackCard() != null
                || candidate.getAttackDefender() != null
                || candidate.getBlockKind() != null
                || candidate.getBlockerCard() != null
                || candidate.getBlockAttackerCard() != null
                || candidate.getMulliganKind() != null
                || candidate.getConfirmationKind() != null
                || candidate.getOrderKind() != null
                || candidate.getOrderItem() != null
                || candidate.getCopySpellResolveFirstOrderKind() != null
                || candidate.getCopySpellResolveFirstOrderItem() != null
                || candidate.getTargetEntityId() != -1
                || !candidate.getTargetName().isEmpty()
                || candidate.getTargetZone() != null
                || candidate.getSourceCardId() != -1
                || !candidate.getSourceName().isEmpty()
                || candidate.getSourceZone() != null
                || candidate.getSourceState() != null
                || !candidate.getAbilityDescription().isEmpty()
                || candidate.getSpellAbility() != null
                || candidate.getTarget() != null
                || candidate.getMana() != null) {
            throw new IllegalArgumentException("Candidate is not an exact Surveil membership choice");
        }
        if (labels[currentStep] != null) {
            throw new IllegalArgumentException("Surveil session step was already classified");
        }

        labels[currentStep] = kind;
        currentItem.label = kind;
        final EnumSet<SurveilPartitionCandidateKind> labelsForKey = symmetryLabels.computeIfAbsent(
                currentItem.symmetryKey,
                ignored -> EnumSet.noneOf(SurveilPartitionCandidateKind.class));
        labelsForKey.add(kind);
        symmetryConflicts.put(currentItem.symmetryKey, labelsForKey.size() > 1);
        currentStep++;
        openRequest = null;
        if (currentStep == canonicalPolicyItems.size()) {
            completeMapping();
        }
    }

    void recordNativeMembershipVector(final List<SurveilPartitionCandidateKind> canonicalLabels) {
        recordNativeMembershipVector(canonicalLabels, nativeSnapshot);
    }

    List<SurveilPartitionCandidateKind> canonicalMembershipVector(final List<Card> graveyardCards) {
        Objects.requireNonNull(graveyardCards, "graveyardCards");
        final Set<Card> graveyard = Collections.newSetFromMap(new IdentityHashMap<>());
        graveyard.addAll(graveyardCards);
        final List<SurveilPartitionCandidateKind> canonicalLabels = new ArrayList<>(
                canonicalPolicyItems.size());
        for (final SurveilItem item : canonicalPolicyItems) {
            canonicalLabels.add(graveyard.contains(item.nativeCard)
                    ? SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                    : SurveilPartitionCandidateKind.CLASSIFY_RETAIN);
        }
        return List.copyOf(canonicalLabels);
    }

    void recordSymmetryConflicts(final List<SurveilPartitionCandidateKind> canonicalLabels) {
        Objects.requireNonNull(canonicalLabels, "canonicalLabels");
        if (canonicalLabels.size() != canonicalPolicyItems.size()) {
            throw new IllegalArgumentException("Surveil symmetry labels have the wrong cardinality");
        }
        final Map<String, EnumSet<SurveilPartitionCandidateKind>> labelsByKey = new HashMap<>();
        for (int index = 0; index < canonicalLabels.size(); index++) {
            final SurveilPartitionCandidateKind kind = Objects.requireNonNull(
                    canonicalLabels.get(index), "canonical symmetry label");
            if (kind != SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                    && kind != SurveilPartitionCandidateKind.CLASSIFY_RETAIN) {
                throw new IllegalArgumentException("Surveil symmetry labels contain an unapproved kind");
            }
            labelsByKey.computeIfAbsent(canonicalPolicyItems.get(index).symmetryKey,
                    ignored -> EnumSet.noneOf(SurveilPartitionCandidateKind.class)).add(kind);
        }
        labelsByKey.values().stream()
                .filter(labelsForKey -> labelsForKey.size() > 1)
                .forEach(ignored -> SurveilPartitionDiagnostics.recordSymmetryConflict());
    }

    void recordNativeMembershipVector(final List<SurveilPartitionCandidateKind> canonicalLabels,
            final List<Card> retainedNativeOrder) {
        Objects.requireNonNull(canonicalLabels, "canonicalLabels");
        Objects.requireNonNull(retainedNativeOrder, "retainedNativeOrder");
        if (closed) {
            throw new IllegalStateException("Surveil session is closed: " + closeReason);
        }
        if (complete && !isEmptySnapshot()) {
            throw new IllegalStateException("Surveil session is complete");
        }
        if (openRequest != null || currentStep != 0) {
            throw new IllegalStateException("Native membership vector must be recorded before a request");
        }
        if (nativeMembershipVector != null) {
            throw new IllegalStateException("Native membership vector was already recorded");
        }
        if (canonicalLabels.size() != labels.length) {
            throw new IllegalArgumentException("Native membership vector has the wrong cardinality");
        }

        final SurveilPartitionCandidateKind[] validated =
                new SurveilPartitionCandidateKind[canonicalLabels.size()];
        for (int index = 0; index < canonicalLabels.size(); index++) {
            final SurveilPartitionCandidateKind kind = Objects.requireNonNull(
                    canonicalLabels.get(index), "native membership kind");
            if (kind != SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                    && kind != SurveilPartitionCandidateKind.CLASSIFY_RETAIN) {
                throw new IllegalArgumentException("Native membership vector contains an unapproved kind");
            }
            validated[index] = kind;
        }
        this.retainedNativeList = List.copyOf(retainedNativeOrder);
        nativeMembershipVector = validated;
    }

    SurveilPartitionCandidateKind nativeMembershipKindAt(final int canonicalStep) {
        if (closed) {
            throw new IllegalStateException("Surveil session is closed: " + closeReason);
        }
        if (canonicalStep < 0 || canonicalStep >= labels.length) {
            throw new IllegalArgumentException("canonicalStep must be within the item range");
        }
        if (nativeMembershipVector == null) {
            throw new IllegalStateException("Surveil native membership vector has not been recorded");
        }
        final SurveilPartitionCandidateKind kind = nativeMembershipVector[canonicalStep];
        if (kind == null) {
            throw new IllegalStateException("Surveil membership step has not been classified");
        }
        return kind;
    }

    boolean isMappingFailed() {
        return mappingFailed;
    }

    void markClosed(final String reason) {
        closed = true;
        closeReason = Objects.requireNonNull(reason, "reason");
        openRequest = null;
    }

    private boolean belongsToOpenRequest(final LegalCandidate candidate) {
        for (final LegalCandidate openCandidate : openRequest.getCandidates()) {
            if (openCandidate == candidate) {
                return true;
            }
        }
        return false;
    }

    private void requireNativeMembershipVector() {
        if (!isEmptySnapshot() && nativeMembershipVector == null) {
            throw new IllegalStateException("Surveil native membership vector has not been recorded");
        }
    }

    private void completeMapping() {
        try {
            for (int index = 0; index < canonicalPolicyItems.size(); index++) {
                if (labels[index] == null || canonicalPolicyItems.get(index).label == null) {
                    throw new IllegalStateException("Surveil native mapping is incomplete");
                }
            }
            this.complete = true;
        } catch (final RuntimeException exception) {
            mappingFailed = true;
            throw exception;
        }
    }

    private static boolean isVisibleToChooser(final Card card, final Player chooser) {
        try {
            // Surveil privately reveals the chooser's own library cards to the native chooser
            // before the public projection is emitted; CardView intentionally hides libraries.
            if (card.getZone() != null && card.getZone().getZoneType() == ZoneType.Library
                    && card.getController() == chooser) {
                return true;
            }
            return !card.isFaceDown()
                    && card.getView() != null
                    && chooser.getView() != null
                    && card.getView().canBeShownTo(chooser.getView());
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    private static String semanticKey(final SurveilPartitionCandidateKind kind, final long itemId) {
        return "SURVEIL_PARTITION|" + kind.name() + "|" + itemId;
    }

    private static final class SurveilItem {
        private final Card nativeCard;
        private final int nativeOrdinal;
        private final StableIdentity stableIdentity;
        private final String symmetryKey;
        private long itemId;
        private SurveilPartitionCard projection;
        private SurveilPartitionCandidateKind label;

        private SurveilItem(final Card nativeCard, final int nativeOrdinal,
                final StableIdentity stableIdentity, final SurveilPartitionCard projection) {
            this.nativeCard = nativeCard;
            this.nativeOrdinal = nativeOrdinal;
            this.stableIdentity = stableIdentity;
            this.symmetryKey = projection.getVisibleName();
            this.projection = projection;
        }
    }

    private record StableIdentity(int cardId, long gameTimestamp) {
    }
}
