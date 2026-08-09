package forge.game.decision;

import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Diagnostic-only adapter that replays an existing complete Forge block declaration neutrally. */
public final class BlockDeclarationAdapter {
    private final BlockDeclarationDecisionProvider provider = new BlockDeclarationDecisionProvider();

    public Capture begin(final Player whoDeclares, final Player defendingPlayer, final Combat combat) {
        final BlockDeclarationDecisionProvider.SessionStart start = provider.beginSession(defendingPlayer,
                whoDeclares, combat);
        if (start.getStatus() != BlockDeclarationDecisionProvider.Status.READY) {
            return Capture.unsupported(start.getReason());
        }
        return Capture.supported(start.getSession());
    }

    public Replay replay(final Capture capture, final Combat combat) {
        if (capture == null || capture.getStatus() != Status.SUPPORTED || combat == null) {
            return Replay.failure(ReplayStatus.UNSUPPORTED, "UNSUPPORTED_CAPTURE");
        }

        final BlockDeclarationSession captured = capture.getSession();
        if (captured.getCombat() != combat || !captured.capturedAttackDeclarationStillPresent()
                || !captured.capturedBlockerDeclarationStillPresent()) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "STALE_BLOCK_DECLARATION");
        }

        final List<BlockDeclarationAssignment> actualAssignments = mapAssignments(captured, combat);
        if (actualAssignments == null) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "MAPPING_FAILED");
        }
        actualAssignments.sort(Comparator.comparing(BlockDeclarationAssignment::semanticKey));

        final BlockDeclarationDecisionProvider.SessionStart replayStart = provider.replaySession(captured);
        final BlockDeclarationSession replaySession = replayStart.getSession();
        final List<ReplayStep> steps = new ArrayList<>();
        BlockDeclarationDecisionProvider.Generation next = provider.generateNext(replaySession);

        for (final BlockDeclarationAssignment wanted : actualAssignments) {
            if (next.getStatus() != BlockDeclarationDecisionProvider.Status.DECISION
                    || next.getRequest() == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "REPLAY_GENERATION_FAILED");
            }
            final LegalCandidate blockerCandidate = next.getRequest().getCandidates().stream()
                    .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.CHOOSE_BLOCKER
                            && wanted.getBlocker().equals(candidate.getBlockerCard()))
                    .findFirst().orElse(null);
            if (blockerCandidate == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "BLOCKER_NOT_REPLAYABLE");
            }
            steps.add(new ReplayStep(next.getRequest(), blockerCandidate, next.getGenerationNanos()));
            next = provider.apply(next.getRequest(), blockerCandidate);
            if (next.getStatus() != BlockDeclarationDecisionProvider.Status.DECISION
                    || next.getRequest() == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "ATTACKER_STAGE_NOT_REPLAYABLE");
            }

            final LegalCandidate attackerCandidate = next.getRequest().getCandidates().stream()
                    .filter(candidate -> candidate.getBlockKind()
                            == BlockDeclarationCandidateKind.CHOOSE_ATTACKER_FOR_BLOCKER
                            && wanted.getBlocker().equals(candidate.getBlockerCard())
                            && wanted.getAttacker().equals(candidate.getBlockAttackerCard()))
                    .findFirst().orElse(null);
            if (attackerCandidate == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "PAIR_NOT_REPLAYABLE");
            }
            steps.add(new ReplayStep(next.getRequest(), attackerCandidate, next.getGenerationNanos()));
            next = provider.apply(next.getRequest(), attackerCandidate);
        }

        if (next.getStatus() != BlockDeclarationDecisionProvider.Status.DECISION || next.getRequest() == null) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "DONE_NOT_REPLAYABLE");
        }
        final LegalCandidate done = next.getRequest().getCandidates().stream()
                .filter(candidate -> candidate.getBlockKind() == BlockDeclarationCandidateKind.DONE)
                .findFirst().orElse(null);
        if (done == null) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "DONE_NOT_REPLAYABLE");
        }
        steps.add(new ReplayStep(next.getRequest(), done, next.getGenerationNanos()));
        final BlockDeclarationDecisionProvider.Generation complete = provider.apply(next.getRequest(), done);
        if (complete.getStatus() != BlockDeclarationDecisionProvider.Status.COMPLETE
                || !assignmentKeys(replaySession.getSelectedAssignments())
                        .equals(assignmentKeys(actualAssignments))) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "ASSIGNMENT_SET_MISMATCH");
        }
        return Replay.complete(steps, replaySession.getSelectedAssignments());
    }

    private static List<BlockDeclarationAssignment> mapAssignments(final BlockDeclarationSession captured,
            final Combat combat) {
        final List<BlockDeclarationAssignment> result = new ArrayList<>();
        final Set<String> usedBlockers = new HashSet<>();
        final Set<String> assignments = new HashSet<>();
        for (final Card attacker : combat.getAttackers()) {
            final BlockDeclarationCard attackerIdentity = new BlockDeclarationCard(attacker);
            if (captured.liveAttacker(attackerIdentity) != attacker
                    || combat.getDefenderByAttacker(attacker) != captured.getDefendingPlayer()) {
                return null;
            }
            for (final Card blocker : combat.getBlockers(attacker)) {
                final BlockDeclarationCard blockerIdentity = new BlockDeclarationCard(blocker);
                if (captured.liveBlocker(blockerIdentity) != blocker) {
                    return null;
                }
                if (!captured.capturedPairExists(blockerIdentity, attackerIdentity)) {
                    return null;
                }
                if (!usedBlockers.add(blockerIdentity.identityKey())) {
                    return null;
                }
                final BlockDeclarationAssignment assignment = new BlockDeclarationAssignment(blockerIdentity,
                        attackerIdentity);
                if (!assignments.add(assignment.semanticKey())) {
                    return null;
                }
                result.add(assignment);
            }
        }
        return result;
    }

    private static Set<String> assignmentKeys(final List<BlockDeclarationAssignment> assignments) {
        final Set<String> result = new HashSet<>();
        for (final BlockDeclarationAssignment assignment : assignments) {
            result.add(assignment.semanticKey());
        }
        return result;
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
        private final BlockDeclarationDecisionProvider.Reason reason;
        private final BlockDeclarationSession session;

        private Capture(final Status status, final BlockDeclarationDecisionProvider.Reason reason,
                final BlockDeclarationSession session) {
            this.status = status;
            this.reason = reason;
            this.session = session;
        }

        private static Capture supported(final BlockDeclarationSession session) {
            return new Capture(Status.SUPPORTED, null, session);
        }

        private static Capture unsupported(final BlockDeclarationDecisionProvider.Reason reason) {
            return new Capture(Status.UNSUPPORTED, reason, null);
        }

        public Status getStatus() {
            return status;
        }

        public BlockDeclarationDecisionProvider.Reason getReason() {
            return reason;
        }

        public BlockDeclarationSession getSession() {
            return session;
        }
    }

    public static final class ReplayStep {
        private final DecisionRequest request;
        private final LegalCandidate candidate;
        private final long generationNanos;

        private ReplayStep(final DecisionRequest request, final LegalCandidate candidate,
                final long generationNanos) {
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
        private final List<BlockDeclarationAssignment> completedAssignments;

        private Replay(final ReplayStatus status, final String reason, final List<ReplayStep> steps,
                final List<BlockDeclarationAssignment> completedAssignments) {
            this.status = status;
            this.reason = reason;
            this.steps = List.copyOf(steps);
            this.completedAssignments = List.copyOf(completedAssignments);
        }

        private static Replay complete(final List<ReplayStep> steps,
                final List<BlockDeclarationAssignment> assignments) {
            return new Replay(ReplayStatus.COMPLETE, null, steps, assignments);
        }

        private static Replay failure(final ReplayStatus status, final String reason) {
            return new Replay(status, reason, List.of(), List.of());
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

        public List<BlockDeclarationAssignment> getCompletedAssignments() {
            return completedAssignments;
        }
    }
}
