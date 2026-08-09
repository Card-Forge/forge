package forge.game.decision;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.AttackConstraints;
import forge.game.combat.AttackRequirement;
import forge.game.combat.AttackRestriction;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.combat.GlobalAttackRestrictions;
import forge.game.cost.CostExert;
import forge.game.cost.CostEnlist;
import forge.game.keyword.Keyword;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Produces sequential, mutation-free ATTACK choices for the approved constraint-free v0 slice. */
public final class AttackDeclarationDecisionProvider {
    public enum Status {
        READY,
        DECISION,
        COMPLETE,
        UNSUPPORTED,
        STALE_ATTACK_DECLARATION
    }

    public enum Reason {
        MULTIPLE_DEFENDERS,
        NO_OPPOSING_PLAYER_DEFENDER,
        EXTERNAL_DECLARER,
        PREMUTATED_COMBAT,
        GLOBAL_ATTACK_RESTRICTION,
        ATTACK_REQUIREMENT,
        GROUP_ATTACK_RESTRICTION,
        ATTACK_COST,
        EXERT,
        ENLIST,
        BANDING,
        NO_ELIGIBLE_ATTACKERS,
        LIVE_STATE_CHANGED,
        REQUEST_OWNERSHIP,
        REQUEST_OUTSTANDING,
        ILLEGAL_CANDIDATE,
        APPLY_FAILED
    }

    private long nextRequestId = 1;
    private long nextSessionId = 1;

    public SessionStart beginSession(final Player attackingPlayer, final Player whoDeclares, final Combat combat) {
        Objects.requireNonNull(attackingPlayer);
        Objects.requireNonNull(whoDeclares);
        Objects.requireNonNull(combat);

        if (whoDeclares != attackingPlayer) {
            return SessionStart.failure(Reason.EXTERNAL_DECLARER);
        }
        if (combat.getAttackingPlayer() != attackingPlayer) {
            return SessionStart.failure(Reason.LIVE_STATE_CHANGED);
        }
        if (!combat.getAttackers().isEmpty()) {
            return SessionStart.failure(Reason.PREMUTATED_COMBAT);
        }
        if (combat.getDefenders().size() != 1) {
            return SessionStart.failure(Reason.MULTIPLE_DEFENDERS);
        }
        final GameEntity defender = combat.getDefenders().getFirst();
        if (!(defender instanceof Player) || !((Player) defender).isOpponentOf(attackingPlayer)) {
            return SessionStart.failure(Reason.NO_OPPOSING_PLAYER_DEFENDER);
        }

        final AttackConstraints constraints = combat.getAttackConstraints();
        final GlobalAttackRestrictions global = constraints.getGlobalRestrictions();
        if (global.getMax() != null || !global.getDefenderMax().isEmpty()) {
            return SessionStart.failure(Reason.GLOBAL_ATTACK_RESTRICTION);
        }
        for (final AttackRestriction restriction : constraints.getRestrictions().values()) {
            if (!restriction.getTypes().isEmpty()) {
                return SessionStart.failure(Reason.GROUP_ATTACK_RESTRICTION);
            }
        }
        for (final AttackRequirement requirement : constraints.getRequirements().values()) {
            if (requirement.hasRequirement()) {
                return SessionStart.failure(Reason.ATTACK_REQUIREMENT);
            }
        }
        if (constraints.countViolations(Collections.emptyMap()) != 0) {
            return SessionStart.failure(Reason.ATTACK_REQUIREMENT);
        }

        final CardCollection possible = new CardCollection();
        for (final Card card : attackingPlayer.getCreaturesInPlay()) {
            if (CombatUtil.canAttack(card, defender)) {
                possible.add(card);
            }
        }
        if (possible.isEmpty()) {
            return SessionStart.failure(Reason.NO_ELIGIBLE_ATTACKERS);
        }
        if (!CombatUtil.getOptionalAttackCostCreatures(possible, CostExert.class).isEmpty()) {
            return SessionStart.failure(Reason.EXERT);
        }
        if (!CombatUtil.getOptionalAttackCostCreatures(possible, CostEnlist.class).isEmpty()) {
            return SessionStart.failure(Reason.ENLIST);
        }
        for (final Card card : possible) {
            if (CombatUtil.getAttackCost(attackingPlayer.getGame(), card, defender) != null) {
                return SessionStart.failure(Reason.ATTACK_COST);
            }
            if (card.hasKeyword(Keyword.BANDING) || card.hasKeyword(Keyword.BANDSWITH)) {
                return SessionStart.failure(Reason.BANDING);
            }
        }

        final AttackDeclarationDefender defenderIdentity = new AttackDeclarationDefender(defender.getId(),
                defender.getName(), defender instanceof Player ? "PLAYER" : defender.getClass().getSimpleName());
        final AttackDeclarationSession session = new AttackDeclarationSession(nextSessionId++, attackingPlayer,
                whoDeclares, combat, defenderIdentity, defender, possible);
        return SessionStart.ready(session);
    }

    public Generation generateNext(final AttackDeclarationSession session) {
        Objects.requireNonNull(session);
        final long startedAtNanos = System.nanoTime();
        if (session.isCompleted()) {
            return Generation.complete(session, System.nanoTime() - startedAtNanos);
        }
        if (session.hasActiveRequest()) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.REQUEST_OUTSTANDING,
                    System.nanoTime() - startedAtNanos);
        }
        if (!session.revalidate()) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.LIVE_STATE_CHANGED,
                    System.nanoTime() - startedAtNanos);
        }

        final List<AttackDeclarationCard> remaining = session.remainingIdentities();
        remaining.sort(Comparator.comparing(AttackDeclarationCard::semanticKey));
        final List<LegalCandidate> candidates = new ArrayList<>();
        for (final AttackDeclarationCard identity : remaining) {
            candidates.add(LegalCandidate.addAttacker(candidates.size(), identity,
                    session.getSoleDefenderIdentity()));
        }
        candidates.add(LegalCandidate.attackDone(candidates.size()));

        final AttackDeclarationContext context = new AttackDeclarationContext(session,
                session.allocateStepIndex());
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.ATTACK, candidates, context);
        session.setActiveRequestId(request.getRequestId());
        return Generation.decision(session, request, System.nanoTime() - startedAtNanos);
    }

    public Generation apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.ATTACK || request.getAttackContext() == null
                || !request.getCandidates().contains(candidate)) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        final AttackDeclarationSession session = request.getAttackContext().getSession();
        if (!session.ownsActiveRequest(request.getRequestId())) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        if (!session.revalidate()) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.LIVE_STATE_CHANGED, 0L);
        }
        if (candidate.getAttackKind() == AttackDeclarationCandidateKind.DONE) {
            session.consumeActiveRequest(request.getRequestId());
            session.markCompleted();
            return Generation.complete(session, System.nanoTime());
        }
        if (candidate.getAttackKind() != AttackDeclarationCandidateKind.ADD_ATTACKER
                || candidate.getAttackCard() == null
                || !session.isEligible(candidate.getAttackCard())
                || !candidate.getAttackDefender().equals(session.getSoleDefenderIdentity())
                || !session.select(candidate.getAttackCard())) {
            return Generation.failure(Status.STALE_ATTACK_DECLARATION, Reason.ILLEGAL_CANDIDATE, 0L);
        }
        session.consumeActiveRequest(request.getRequestId());
        return generateNext(session);
    }

    public ApplyResult applyCompletedToCombat(final AttackDeclarationSession session) {
        Objects.requireNonNull(session);
        if (!session.isCompleted() || !session.revalidate() || !session.getCombat().getAttackers().isEmpty()) {
            return ApplyResult.failure(Reason.LIVE_STATE_CHANGED);
        }
        final Map<Card, GameEntity> assignments = session.resolveAssignments();
        if (assignments == null) {
            return ApplyResult.failure(Reason.LIVE_STATE_CHANGED);
        }
        for (final Map.Entry<Card, GameEntity> assignment : assignments.entrySet()) {
            session.getCombat().addAttacker(assignment.getKey(), assignment.getValue());
        }
        if (!CombatUtil.validateAttackers(session.getCombat())) {
            session.getCombat().clearAttackers();
            return ApplyResult.failure(Reason.APPLY_FAILED);
        }
        return ApplyResult.complete();
    }

    public SessionStart replaySession(final AttackDeclarationSession captureSession) {
        Objects.requireNonNull(captureSession);
        return SessionStart.ready(captureSession.copyForReplay());
    }

    public static final class SessionStart {
        private final Status status;
        private final Reason reason;
        private final AttackDeclarationSession session;

        private SessionStart(final Status status, final Reason reason, final AttackDeclarationSession session) {
            this.status = status;
            this.reason = reason;
            this.session = session;
        }

        private static SessionStart ready(final AttackDeclarationSession session) {
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

        public AttackDeclarationSession getSession() {
            return session;
        }
    }

    public static final class Generation {
        private final Status status;
        private final Reason reason;
        private final AttackDeclarationSession session;
        private final DecisionRequest request;
        private final long generationNanos;

        private Generation(final Status status, final Reason reason, final AttackDeclarationSession session,
                final DecisionRequest request, final long generationNanos) {
            this.status = status;
            this.reason = reason;
            this.session = session;
            this.request = request;
            this.generationNanos = generationNanos;
        }

        private static Generation decision(final AttackDeclarationSession session, final DecisionRequest request,
                final long generationNanos) {
            return new Generation(Status.DECISION, null, session, request, generationNanos);
        }

        private static Generation complete(final AttackDeclarationSession session, final long generationNanos) {
            return new Generation(Status.COMPLETE, null, session, null, generationNanos);
        }

        private static Generation failure(final Status status, final Reason reason, final long generationNanos) {
            return new Generation(status, reason, null, null, generationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public AttackDeclarationSession getSession() {
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

        private static ApplyResult failure(final Reason reason) {
            return new ApplyResult(Status.STALE_ATTACK_DECLARATION, reason);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }
    }
}
