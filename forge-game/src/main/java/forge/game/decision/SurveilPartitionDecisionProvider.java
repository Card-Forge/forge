package forge.game.decision;

import forge.game.card.Card;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SurveilPartitionDecisionProvider {
    private long nextSurveilSessionId = 1L;
    private long nextRequestId = 1L;
    private final Map<Long, SurveilPartitionSession> activeSessions = new HashMap<>();

    public SurveilPartitionDecisionProvider() {
    }

    long nextSurveilSessionId() {
        return nextSurveilSessionId++;
    }

    long nextRequestId() {
        return nextRequestId++;
    }

    SurveilPartitionSession admit(final Player chooser, final List<Card> privateSnapshot) {
        final List<Card> immutableSnapshot = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(privateSnapshot, "privateSnapshot")));
        final SurveilPartitionSession session = new SurveilPartitionSession(nextSurveilSessionId(), chooser,
                immutableSnapshot);
        if (!session.isComplete()) {
            activeSessions.put(session.surveilSessionId(), session);
        }
        return session;
    }

    DecisionRequest createMembershipRequest(final SurveilPartitionSession session) {
        Objects.requireNonNull(session, "session");
        if (session.isEmptySnapshot() && session.isComplete()) {
            return null;
        }
        requireRegistered(session);
        if (!session.isIdentityStable()) {
            throw new IllegalStateException("Surveil session identity is stale");
        }
        if (session.isComplete()) {
            throw new IllegalStateException("Surveil session is complete");
        }
        if (session.hasOpenRequest()) {
            throw new IllegalStateException("Surveil session already has an open request");
        }
        return session.createMembershipRequest(nextRequestId());
    }

    void applyMembershipCandidate(final SurveilPartitionSession session, final LegalCandidate candidate) {
        Objects.requireNonNull(session, "session");
        requireRegistered(session);
        if (!session.isIdentityStable()) {
            throw new IllegalStateException("Surveil session identity is stale");
        }
        session.applyMembershipCandidate(candidate);
    }

    boolean isComplete(final SurveilPartitionSession session) {
        return Objects.requireNonNull(session, "session").isComplete();
    }

    void closeSession(final SurveilPartitionSession session) {
        if (session == null) {
            return;
        }
        final SurveilPartitionSession registered = activeSessions.get(session.surveilSessionId());
        if (registered == session) {
            activeSessions.remove(session.surveilSessionId());
            session.markClosed("CLOSED");
        }
    }

    int activeSessionCount() {
        return activeSessions.size();
    }

    private void requireRegistered(final SurveilPartitionSession session) {
        if (activeSessions.get(session.surveilSessionId()) != session || session.isClosed()) {
            throw new IllegalStateException("Surveil session is stale or not registered");
        }
    }
}
