package forge.game.decision;

import java.util.List;

/** Public metadata for one atomic step in a turn-based BLOCK session. */
public final class BlockDeclarationContext {
    private final long blockSessionId;
    private final int gameId;
    private final int blockStepIndex;
    private final BlockDeclarationStage blockStage;
    private final int defendingPlayerId;
    private final int declaringPlayerId;
    private final List<BlockDeclarationAssignment> selectedAssignments;
    private final BlockDeclarationSession session;

    BlockDeclarationContext(final BlockDeclarationSession session, final int blockStepIndex,
            final BlockDeclarationStage blockStage) {
        this.session = session;
        this.blockSessionId = session.getBlockSessionId();
        this.gameId = session.getGameId();
        this.blockStepIndex = blockStepIndex;
        this.blockStage = blockStage;
        this.defendingPlayerId = session.getDefendingPlayer().getId();
        this.declaringPlayerId = session.getWhoDeclares().getId();
        this.selectedAssignments = List.copyOf(session.getSelectedAssignments());
    }

    public long getBlockSessionId() {
        return blockSessionId;
    }

    public int getGameId() {
        return gameId;
    }

    public int getBlockStepIndex() {
        return blockStepIndex;
    }

    public BlockDeclarationStage getBlockStage() {
        return blockStage;
    }

    public Long getDecisionSequenceId() {
        return null;
    }

    public Integer getActionSubdecisionIndex() {
        return null;
    }

    public int getDefendingPlayerId() {
        return defendingPlayerId;
    }

    public int getDeclaringPlayerId() {
        return declaringPlayerId;
    }

    public List<BlockDeclarationAssignment> getSelectedAssignments() {
        return selectedAssignments;
    }

    BlockDeclarationSession getSession() {
        return session;
    }
}
