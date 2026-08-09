package forge.game.decision;

import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Produces sequential, mutation-free BLOCK choices for the approved constraint-free v0 slice. */
public final class BlockDeclarationDecisionProvider {
    public enum Status {
        READY,
        DECISION,
        COMPLETE,
        UNSUPPORTED,
        STALE_BLOCK_DECLARATION,
        APPLY_FAILED
    }

    public enum Reason {
        NOT_ONE_V_ONE,
        UNSUPPORTED_DEFENDER_SHAPE,
        EXTERNAL_DECLARER,
        PREMUTATED_COMBAT,
        NO_ATTACKERS,
        NO_LEGAL_BLOCK_PAIR,
        UNSUPPORTED_ATTACKING_BAND,
        UNSUPPORTED_MULTI_BLOCKER_ASSIGNMENT,
        BLOCKER_GROUP_RESTRICTION,
        ATTACKER_BLOCK_COUNT_RESTRICTION,
        GLOBAL_BLOCK_RESTRICTION,
        BLOCK_REQUIREMENT,
        UNSUPPORTED_BLOCK_COST,
        STALE_ATTACK_DECLARATION,
        STALE_BLOCK_DECLARATION,
        LIVE_STATE_CHANGED,
        REQUEST_OWNERSHIP,
        REQUEST_OUTSTANDING,
        ILLEGAL_CANDIDATE,
        APPLY_FAILED,
        MAPPING_FAILED
    }

    private long nextRequestId = 1L;
    private long nextSessionId = 1L;

    public SessionStart beginSession(final Player defendingPlayer, final Player whoDeclares,
            final Combat combat) {
        Objects.requireNonNull(defendingPlayer);
        Objects.requireNonNull(whoDeclares);
        Objects.requireNonNull(combat);

        if (whoDeclares != defendingPlayer) {
            return SessionStart.failure(Reason.EXTERNAL_DECLARER);
        }

        final BlockDeclarationSession session = new BlockDeclarationSession(nextSessionId++, defendingPlayer,
                whoDeclares, combat);
        if (!session.initializePairs()) {
            return SessionStart.failure(Reason.NO_LEGAL_BLOCK_PAIR);
        }
        final Reason admissionReason = session.admissionReason();
        if (admissionReason != null) {
            return SessionStart.failure(admissionReason);
        }
        return SessionStart.ready(session);
    }

    public Generation generateNext(final BlockDeclarationSession session) {
        Objects.requireNonNull(session);
        final long startedAtNanos = System.nanoTime();
        if (session.isCompleted()) {
            return Generation.complete(session, System.nanoTime() - startedAtNanos);
        }
        if (session.hasActiveRequest()) {
            return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.REQUEST_OUTSTANDING,
                    System.nanoTime() - startedAtNanos);
        }
        final Reason admissionReason = session.admissionReason();
        if (admissionReason != null) {
            final Status status = admissionReason == Reason.APPLY_FAILED ? Status.APPLY_FAILED
                    : Status.STALE_BLOCK_DECLARATION;
            return Generation.failure(session, status, admissionReason, System.nanoTime() - startedAtNanos);
        }

        final List<LegalCandidate> candidates = new ArrayList<>();
        final BlockDeclarationCard pending = session.getPendingBlocker();
        if (pending == null) {
            final List<BlockDeclarationCard> remaining = session.remainingBlockers();
            remaining.sort(Comparator.comparing(BlockDeclarationCard::semanticKey));
            for (final BlockDeclarationCard blocker : remaining) {
                candidates.add(LegalCandidate.chooseBlocker(candidates.size(), blocker));
            }
            candidates.add(LegalCandidate.blockDone(candidates.size()));
        } else {
            final List<BlockDeclarationCard> attackers = session.currentAttackersFor(pending);
            if (attackers.isEmpty()) {
                return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.STALE_BLOCK_DECLARATION,
                        System.nanoTime() - startedAtNanos);
            }
            for (final BlockDeclarationCard attacker : attackers) {
                candidates.add(LegalCandidate.chooseAttacker(candidates.size(), pending, attacker));
            }
        }
        final BlockDeclarationStage stage = pending == null ? BlockDeclarationStage.CHOOSE_BLOCKER
                : BlockDeclarationStage.CHOOSE_ATTACKER_FOR_BLOCKER;
        final BlockDeclarationContext context = new BlockDeclarationContext(session, session.allocateStepIndex(),
                stage);
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.BLOCK, candidates, context);
        session.setActiveRequestId(request.getRequestId());
        return Generation.decision(session, request, System.nanoTime() - startedAtNanos);
    }

    public Generation apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.BLOCK || request.getBlockContext() == null
                || !request.getCandidates().contains(candidate)) {
            return Generation.failure(null, Status.STALE_BLOCK_DECLARATION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        final BlockDeclarationSession session = request.getBlockContext().getSession();
        if (!session.ownsActiveRequest(request.getRequestId())) {
            return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        final Reason admissionReason = session.admissionReason();
        if (admissionReason != null) {
            return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, admissionReason, 0L);
        }

        if (request.getBlockContext().getBlockStage() == BlockDeclarationStage.CHOOSE_BLOCKER) {
            if (candidate.getBlockKind() == BlockDeclarationCandidateKind.DONE) {
                session.consumeActiveRequest(request.getRequestId());
                session.markCompleted();
                return Generation.complete(session, System.nanoTime());
            }
            if (candidate.getBlockKind() != BlockDeclarationCandidateKind.CHOOSE_BLOCKER
                    || !session.isEligibleBlocker(candidate.getBlockerCard())) {
                return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.ILLEGAL_CANDIDATE, 0L);
            }
            session.consumeActiveRequest(request.getRequestId());
            session.setPendingBlocker(candidate.getBlockerCard());
            return generateNext(session);
        }

        if (session.currentAttackersFor(session.getPendingBlocker()).isEmpty()) {
            return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.STALE_BLOCK_DECLARATION, 0L);
        }
        if (candidate.getBlockKind() != BlockDeclarationCandidateKind.CHOOSE_ATTACKER_FOR_BLOCKER
                || !session.select(candidate.getBlockerCard(), candidate.getBlockAttackerCard())) {
            return Generation.failure(session, Status.STALE_BLOCK_DECLARATION, Reason.ILLEGAL_CANDIDATE, 0L);
        }
        session.consumeActiveRequest(request.getRequestId());
        return generateNext(session);
    }

    public ApplyResult applyCompletedToCombat(final BlockDeclarationSession session) {
        Objects.requireNonNull(session);
        if (!session.isCompleted() || session.getCombat().getAllBlockers().size() != 0) {
            return ApplyResult.failure(Status.APPLY_FAILED, Reason.APPLY_FAILED);
        }
        final Reason admissionReason = session.admissionReason();
        if (admissionReason != null) {
            return ApplyResult.failure(Status.APPLY_FAILED, admissionReason);
        }
        final Map<BlockDeclarationAssignment, Pair<Card, Card>> assignments = session.resolveCompletedAssignments();
        if (assignments == null) {
            return ApplyResult.failure(Status.APPLY_FAILED, Reason.STALE_BLOCK_DECLARATION);
        }

        final List<Pair<Card, Card>> added = new ArrayList<>();
        try {
            for (final Pair<Card, Card> assignment : assignments.values()) {
                final Card attacker = assignment.getLeft();
                final Card blocker = assignment.getRight();
                added.add(assignment);
                session.getCombat().addBlocker(attacker, blocker);
                if (!session.getCombat().getAttackersBlockedBy(blocker).contains(attacker)) {
                    rollback(session, added);
                    return ApplyResult.failure(Status.APPLY_FAILED, Reason.APPLY_FAILED);
                }
            }
            for (final Pair<Card, Card> assignment : added) {
                if (CombatUtil.getBlockCost(session.getDefendingPlayer().getGame(), assignment.getRight(),
                        assignment.getLeft()) != null) {
                    rollback(session, added);
                    return ApplyResult.failure(Status.APPLY_FAILED, Reason.UNSUPPORTED_BLOCK_COST);
                }
            }
            if (CombatUtil.validateBlocks(session.getCombat(), session.getDefendingPlayer()) != null) {
                rollback(session, added);
                return ApplyResult.failure(Status.APPLY_FAILED, Reason.APPLY_FAILED);
            }
            return ApplyResult.complete();
        } catch (final RuntimeException ex) {
            rollback(session, added);
            return ApplyResult.failure(Status.APPLY_FAILED, Reason.APPLY_FAILED);
        }
    }

    private static boolean rollback(final BlockDeclarationSession session, final List<Pair<Card, Card>> added) {
        for (int index = added.size() - 1; index >= 0; index--) {
            final Pair<Card, Card> assignment = added.get(index);
            session.getCombat().removeBlockAssignment(assignment.getLeft(), assignment.getRight());
        }
        for (final Pair<Card, Card> assignment : added) {
            if (session.getCombat().getAttackersBlockedBy(assignment.getRight()).contains(assignment.getLeft())
                    || session.getCombat().getBlockers(assignment.getLeft()).contains(assignment.getRight())) {
                return false;
            }
        }
        return true;
    }

    public SessionStart replaySession(final BlockDeclarationSession captureSession) {
        Objects.requireNonNull(captureSession);
        return SessionStart.ready(captureSession.copyForReplay());
    }

    public static final class SessionStart {
        private final Status status;
        private final Reason reason;
        private final BlockDeclarationSession session;

        private SessionStart(final Status status, final Reason reason, final BlockDeclarationSession session) {
            this.status = status;
            this.reason = reason;
            this.session = session;
        }

        private static SessionStart ready(final BlockDeclarationSession session) {
            return new SessionStart(Status.READY, null, session);
        }

        private static SessionStart failure(final Reason reason) {
            return new SessionStart(Status.UNSUPPORTED, reason, null);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public BlockDeclarationSession getSession() {
            return session;
        }
    }

    public static final class Generation {
        private final Status status;
        private final Reason reason;
        private final BlockDeclarationSession session;
        private final DecisionRequest request;
        private final long generationNanos;

        private Generation(final Status status, final Reason reason, final BlockDeclarationSession session,
                final DecisionRequest request, final long generationNanos) {
            this.status = status;
            this.reason = reason;
            this.session = session;
            this.request = request;
            this.generationNanos = generationNanos;
        }

        private static Generation decision(final BlockDeclarationSession session, final DecisionRequest request,
                final long generationNanos) {
            return new Generation(Status.DECISION, null, session, request, generationNanos);
        }

        private static Generation complete(final BlockDeclarationSession session, final long generationNanos) {
            return new Generation(Status.COMPLETE, null, session, null, generationNanos);
        }

        private static Generation failure(final BlockDeclarationSession session, final Status status,
                final Reason reason, final long generationNanos) {
            return new Generation(status, reason, session, null, generationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public BlockDeclarationSession getSession() {
            return session;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }

    public static final class ApplyResult {
        private final Status status;
        private final Reason reason;

        private ApplyResult(final Status status, final Reason reason) {
            this.status = status;
            this.reason = reason;
        }

        private static ApplyResult complete() {
            return new ApplyResult(Status.COMPLETE, null);
        }

        private static ApplyResult failure(final Status status, final Reason reason) {
            return new ApplyResult(status, reason);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }
    }
}
