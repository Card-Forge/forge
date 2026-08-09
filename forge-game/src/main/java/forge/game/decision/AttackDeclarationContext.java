package forge.game.decision;

import java.util.List;

/** Public metadata for one atomic step in a turn-based ATTACK session. */
public final class AttackDeclarationContext {
    private final long attackSessionId;
    private final int gameId;
    private final int attackStepIndex;
    private final int attackingPlayerId;
    private final int declaringPlayerId;
    private final AttackDeclarationDefender soleDefender;
    private final List<AttackDeclarationAssignment> selectedAssignments;
    private final AttackDeclarationSession session;

    AttackDeclarationContext(final AttackDeclarationSession session, final int attackStepIndex) {
        this.session = session;
        this.attackSessionId = session.getAttackSessionId();
        this.gameId = session.getGameId();
        this.attackStepIndex = attackStepIndex;
        this.attackingPlayerId = session.getAttackingPlayer().getId();
        this.declaringPlayerId = session.getWhoDeclares().getId();
        this.soleDefender = session.getSoleDefenderIdentity();
        this.selectedAssignments = List.copyOf(session.getSelectedAssignments());
    }

    public long getAttackSessionId() {
        return attackSessionId;
    }

    public int getGameId() {
        return gameId;
    }

    public int getAttackStepIndex() {
        return attackStepIndex;
    }

    public Long getDecisionSequenceId() {
        return null;
    }

    public Integer getActionSubdecisionIndex() {
        return null;
    }

    public int getAttackingPlayerId() {
        return attackingPlayerId;
    }

    public int getDeclaringPlayerId() {
        return declaringPlayerId;
    }

    public AttackDeclarationDefender getSoleDefender() {
        return soleDefender;
    }

    public List<AttackDeclarationAssignment> getSelectedAssignments() {
        return selectedAssignments;
    }

    AttackDeclarationSession getSession() {
        return session;
    }
}
