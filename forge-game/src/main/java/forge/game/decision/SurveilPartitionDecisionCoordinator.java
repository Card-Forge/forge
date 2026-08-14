package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Capture-only boundary around the existing native surveil callback. */
public final class SurveilPartitionDecisionCoordinator {
    private final SurveilPartitionDecisionProvider provider;

    public SurveilPartitionDecisionCoordinator(final SurveilPartitionDecisionProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    SurveilPartitionDecisionProvider provider() {
        return provider;
    }

    public Pair<CardCollection, CardCollection> captureNativeSurveil(final Player chooser,
            final CardCollection topN,
            final Function<CardCollection, Pair<CardCollection, CardCollection>> nativeArrange) {
        Objects.requireNonNull(nativeArrange, "nativeArrange");
        final CardCollection originalTopN = topN;
        SurveilPartitionDiagnostics.recordArrangeCall();
        if (originalTopN == null) {
            SurveilPartitionDiagnostics.recordCaptureAdmissionFailure("NULL_TOP_N");
            return invokeNative(originalTopN, nativeArrange);
        }

        final List<Card> privateSnapshot = Collections.unmodifiableList(new ArrayList<>(originalTopN));
        final SurveilPartitionSession session;
        try {
            session = provider.admit(chooser, privateSnapshot);
        } catch (final RuntimeException admissionFailure) {
            SurveilPartitionDiagnostics.recordCaptureAdmissionFailure(admissionFailure.getClass().getSimpleName());
            return invokeNative(originalTopN, nativeArrange);
        }

        // The coordinator is the sole terminal owner for an admitted capture session.
        // It closes once, after native mapping and all post-callback trace materialization,
        // including every exceptional terminal path.
        try {
            SurveilPartitionDiagnostics.recordSessionSize(privateSnapshot.size());
            final Pair<CardCollection, CardCollection> nativePair = invokeNative(originalTopN, nativeArrange);
            if (!validateNativePair(privateSnapshot, nativePair)) {
                SurveilPartitionDiagnostics.recordMapping(false, "IDENTITY");
                return nativePair;
            }
            final List<Card> graveyard = normalize(nativePair.getRight());
            final List<SurveilPartitionCandidateKind> vector = session.canonicalMembershipVector(graveyard);
            session.recordNativeMembershipVector(vector, normalize(nativePair.getLeft()));
            session.recordSymmetryConflicts(vector);
            SurveilPartitionDiagnostics.recordN2Cardinality(graveyard.size(), privateSnapshot.size() - graveyard.size());
            afterNativeMembershipVectorCaptured(chooser, session, vector);
            SurveilPartitionDiagnostics.recordMapping(true, "VALID");
            return nativePair;
        } finally {
            provider.closeSession(session);
        }
    }

    int activeSessionCount() {
        return provider.activeSessionCount();
    }

    /**
     * Task 5 seam: trace materialization is deliberately inserted after native mapping and before closure.
     * This hook does not create a second request or candidate state machine.
     */
    void afterNativeMembershipVectorCaptured(final Player chooser, final SurveilPartitionSession session,
            final List<SurveilPartitionCandidateKind> vector) {
        for (int step = 0; step < vector.size(); step++) {
            final DecisionRequest request = provider.createMembershipRequest(session);
            final SurveilPartitionCandidateKind expectedKind = session.nativeMembershipKindAt(step);
            final LegalCandidate chosen = request.getCandidates().stream()
                    .filter(candidate -> candidate.getSurveilPartitionCandidateKind() == expectedKind)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("native membership candidate is not legal"));
            final DeterminismTrace.RequestHandle handle = DeterminismTrace.recordRequest(
                    chooser.getGame(), chooser.getId(), request,
                    "SURVEIL_PARTITION", step, DecisionTraceRequestRecord.Profile.SURVEIL_PARTITION,
                    DecisionTraceTeacherLabelEligibility.NOT_APPLICABLE);
            SurveilPartitionDiagnostics.recordMembershipRequest();
            handle.recordNativeMappedResult(chosen);
            SurveilPartitionDiagnostics.recordMembershipResult();
            provider.applyMembershipCandidate(session, chosen);
        }
    }

    private Pair<CardCollection, CardCollection> invokeNative(final CardCollection originalTopN,
            final Function<CardCollection, Pair<CardCollection, CardCollection>> nativeArrange) {
        try {
            final Pair<CardCollection, CardCollection> result = nativeArrange.apply(originalTopN);
            SurveilPartitionDiagnostics.recordCallback(false);
            return result;
        } catch (final RuntimeException failure) {
            SurveilPartitionDiagnostics.recordCallback(true);
            throw failure;
        }
    }

    private static boolean validateNativePair(final List<Card> snapshot,
            final Pair<CardCollection, CardCollection> nativePair) {
        if (nativePair == null) {
            return false;
        }
        final List<Card> graveyard = normalize(nativePair.getRight());
        final List<Card> retained = normalize(nativePair.getLeft());
        final IdentityHashMap<Card, Boolean> expected = new IdentityHashMap<>();
        for (final Card card : snapshot) {
            if (card == null || expected.put(card, Boolean.TRUE) != null) {
                return false;
            }
        }
        final IdentityHashMap<Card, Boolean> seen = new IdentityHashMap<>();
        for (final Card card : graveyard) {
            if (card == null || expected.get(card) == null || seen.put(card, Boolean.TRUE) != null) {
                return false;
            }
        }
        for (final Card card : retained) {
            if (card == null || expected.get(card) == null || seen.put(card, Boolean.TRUE) != null) {
                return false;
            }
        }
        return seen.size() == expected.size();
    }

    private static List<Card> normalize(final CardCollection cards) {
        return cards == null ? List.of() : new ArrayList<>(cards);
    }

}
