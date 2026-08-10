package forge.game.decision;

import java.util.List;

/** Immutable training-contract view of one decision trace request. */
public final class DecisionTraceRequestRecord {
    private final long traceRequestIndex;
    private final int turn;
    private final String phase;
    private final int actingPlayerSeat;
    private final DecisionType decisionType;
    private final String adapterOrStage;
    private final int decisionStepIndex;
    private final boolean forced;
    private final List<String> legalCandidates;
    private final String candidateSetHash;

    DecisionTraceRequestRecord(final long traceRequestIndex, final int turn, final String phase,
            final int actingPlayerSeat, final DecisionType decisionType, final String adapterOrStage,
            final int decisionStepIndex, final boolean forced, final List<String> legalCandidates,
            final String candidateSetHash) {
        this.traceRequestIndex = traceRequestIndex;
        this.turn = turn;
        this.phase = phase;
        this.actingPlayerSeat = actingPlayerSeat;
        this.decisionType = decisionType;
        this.adapterOrStage = adapterOrStage;
        this.decisionStepIndex = decisionStepIndex;
        this.forced = forced;
        this.legalCandidates = List.copyOf(legalCandidates);
        this.candidateSetHash = candidateSetHash;
    }

    public long getTraceRequestIndex() {
        return traceRequestIndex;
    }

    public int getTurn() {
        return turn;
    }

    public String getPhase() {
        return phase;
    }

    public int getActingPlayerSeat() {
        return actingPlayerSeat;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public String getAdapterOrStage() {
        return adapterOrStage;
    }

    public int getDecisionStepIndex() {
        return decisionStepIndex;
    }

    public boolean isForced() {
        return forced;
    }

    public List<String> getLegalCandidates() {
        return legalCandidates;
    }

    public String getCandidateSetHash() {
        return candidateSetHash;
    }
}
