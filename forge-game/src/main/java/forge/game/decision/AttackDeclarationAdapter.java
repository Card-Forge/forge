package forge.game.decision;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Diagnostic-only adapter that replays an existing complete Forge declaration neutrally. */
public final class AttackDeclarationAdapter {
    private final AttackDeclarationDecisionProvider provider = new AttackDeclarationDecisionProvider();

    public Capture begin(final Player whoDeclares, final Player attackingPlayer, final Combat combat) {
        final AttackDeclarationDecisionProvider.SessionStart start = provider.beginSession(attackingPlayer,
                whoDeclares, combat);
        if (start.getStatus() != AttackDeclarationDecisionProvider.Status.READY) {
            return Capture.unsupported(start.getReason());
        }
        return Capture.supported(start.getSession());
    }

    public Replay replay(final Capture capture, final Map<Card, GameEntity> actualAssignments) {
        if (capture == null || capture.getStatus() != Status.SUPPORTED || actualAssignments == null) {
            return Replay.failure(ReplayStatus.UNSUPPORTED);
        }

        final AttackDeclarationSession captured = capture.getSession();
        final Map<String, AttackDeclarationCard> identities = new LinkedHashMap<>();
        for (final AttackDeclarationCard identity : captured.getEligibleIdentities()) {
            identities.put(identity.identityKey(), identity);
        }
        final List<AttackDeclarationCard> selected = new ArrayList<>();
        for (final Map.Entry<Card, GameEntity> entry : actualAssignments.entrySet()) {
            final AttackDeclarationCard identity = identities.get(new AttackDeclarationCard(entry.getKey()).identityKey());
            if (identity == null || captured.liveCard(identity) != entry.getKey()
                    || entry.getValue() != captured.getSoleDefenderIdentity().getLiveEntity()) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED);
            }
            selected.add(identity);
        }
        selected.sort(Comparator.comparing(AttackDeclarationCard::semanticKey));

        final AttackDeclarationDecisionProvider.SessionStart replayStart = provider.replaySession(captured);
        final AttackDeclarationSession replaySession = replayStart.getSession();
        final List<ReplayStep> steps = new ArrayList<>();
        AttackDeclarationDecisionProvider.Generation next = provider.generateNext(replaySession);
        for (final AttackDeclarationCard wanted : selected) {
            if (next.getStatus() != AttackDeclarationDecisionProvider.Status.DECISION) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED);
            }
            final LegalCandidate candidate = next.getRequest().getCandidates().stream()
                    .filter(value -> value.getAttackKind() == AttackDeclarationCandidateKind.ADD_ATTACKER
                            && wanted.equals(value.getAttackCard()))
                    .findFirst().orElse(null);
            if (candidate == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED);
            }
            steps.add(new ReplayStep(next.getRequest(), candidate, next.getGenerationNanos()));
            next = provider.apply(next.getRequest(), candidate);
            if (next.getStatus() != AttackDeclarationDecisionProvider.Status.DECISION) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED);
            }
        }

        if (next.getStatus() != AttackDeclarationDecisionProvider.Status.DECISION) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED);
        }
        final LegalCandidate done = next.getRequest().getCandidates().stream()
                .filter(value -> value.getAttackKind() == AttackDeclarationCandidateKind.DONE)
                .findFirst().orElse(null);
        if (done == null) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED);
        }
        steps.add(new ReplayStep(next.getRequest(), done, next.getGenerationNanos()));
        final AttackDeclarationDecisionProvider.Generation complete = provider.apply(next.getRequest(), done);
        if (complete.getStatus() != AttackDeclarationDecisionProvider.Status.COMPLETE
                || replaySession.getSelectedAssignments().size() != actualAssignments.size()) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED);
        }
        return Replay.complete(steps, replaySession.getSelectedAssignments());
    }

    public enum Status {
        SUPPORTED,
        UNSUPPORTED
    }

    public enum ReplayStatus {
        COMPLETE,
        MAPPING_FAILED,
        UNSUPPORTED
    }

    public static final class Capture {
        private final Status status;
        private final AttackDeclarationDecisionProvider.Reason reason;
        private final AttackDeclarationSession session;

        private Capture(final Status status, final AttackDeclarationDecisionProvider.Reason reason,
                final AttackDeclarationSession session) {
            this.status = status;
            this.reason = reason;
            this.session = session;
        }

        private static Capture supported(final AttackDeclarationSession session) {
            return new Capture(Status.SUPPORTED, null, session);
        }

        private static Capture unsupported(final AttackDeclarationDecisionProvider.Reason reason) {
            return new Capture(Status.UNSUPPORTED, reason, null);
        }

        public Status getStatus() {
            return status;
        }

        public AttackDeclarationDecisionProvider.Reason getReason() {
            return reason;
        }

        public AttackDeclarationSession getSession() {
            return session;
        }
    }

    public static final class ReplayStep {
        private final DecisionRequest request;
        private final LegalCandidate candidate;
        private final long generationNanos;

        private ReplayStep(final DecisionRequest request, final LegalCandidate candidate, final long generationNanos) {
            this.request = request;
            this.candidate = candidate;
            this.generationNanos = generationNanos;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public LegalCandidate getCandidate() {
            return candidate;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }

    public static final class Replay {
        private final ReplayStatus status;
        private final String reason;
        private final List<ReplayStep> steps;
        private final List<AttackDeclarationAssignment> completedAssignments;

        private Replay(final ReplayStatus status, final List<ReplayStep> steps,
                final List<AttackDeclarationAssignment> completedAssignments, final String reason) {
            this.status = status;
            this.reason = reason;
            this.steps = List.copyOf(steps);
            this.completedAssignments = List.copyOf(completedAssignments);
        }

        private static Replay complete(final List<ReplayStep> steps,
                final List<AttackDeclarationAssignment> assignments) {
            return new Replay(ReplayStatus.COMPLETE, steps, assignments, null);
        }

        private static Replay failure(final ReplayStatus status) {
            return new Replay(status, List.of(), List.of(), status.name());
        }

        public ReplayStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public List<ReplayStep> getSteps() {
            return steps;
        }

        public List<AttackDeclarationAssignment> getCompletedAssignments() {
            return completedAssignments;
        }
    }
}
