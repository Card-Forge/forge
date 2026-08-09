package forge.game.decision;

import java.util.List;

/** Public metadata for one atomic step within a callback-local card-selection session. */
public final class CardSelectionContext {
    private final long selectionSessionId;
    private final int gameId;
    private final int selectionStepIndex;
    private final Long decisionSequenceId;
    private final Integer actionSubdecisionIndex;
    private final int chooserId;
    private final int affectedPlayerId;
    private final int sourceCardId;
    private final long sourceCardTimestamp;
    private final int min;
    private final int max;
    private final List<CardSelectionCard> selectedCards;
    private final List<CardSelectionCard> visibleCards;
    private final CardSelectionSession session;
    private final ActionContinuation continuation;

    CardSelectionContext(final CardSelectionSession session, final int selectionStepIndex,
            final ActionContinuation continuation, final Integer actionSubdecisionIndex) {
        this.session = session;
        this.selectionSessionId = session.getSelectionSessionId();
        this.gameId = session.getGameId();
        this.selectionStepIndex = selectionStepIndex;
        this.continuation = continuation;
        this.decisionSequenceId = continuation == null ? null : continuation.getDecisionSequenceId();
        this.actionSubdecisionIndex = actionSubdecisionIndex;
        this.chooserId = session.getChooser().getId();
        this.affectedPlayerId = session.getAffectedPlayer().getId();
        this.sourceCardId = session.getSource().getHostCard().getId();
        this.sourceCardTimestamp = session.getSource().getHostCard().getGameTimestamp();
        this.min = session.getMin();
        this.max = session.getMax();
        this.selectedCards = List.copyOf(session.getSelectedIdentities());
        this.visibleCards = List.copyOf(session.getVisibleCards());
    }

    public long getSelectionSessionId() {
        return selectionSessionId;
    }

    public int getGameId() {
        return gameId;
    }

    public int getSelectionStepIndex() {
        return selectionStepIndex;
    }

    public Long getDecisionSequenceId() {
        return decisionSequenceId;
    }

    public Integer getActionSubdecisionIndex() {
        return actionSubdecisionIndex;
    }

    public int getChooserId() {
        return chooserId;
    }

    public int getAffectedPlayerId() {
        return affectedPlayerId;
    }

    public int getSourceCardId() {
        return sourceCardId;
    }

    public long getSourceCardTimestamp() {
        return sourceCardTimestamp;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public List<CardSelectionCard> getSelectedCards() {
        return selectedCards;
    }

    public List<CardSelectionCard> getVisibleCards() {
        return visibleCards;
    }

    CardSelectionSession getSession() {
        return session;
    }

    ActionContinuation getContinuation() {
        return continuation;
    }
}
