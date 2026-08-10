package forge.game.decision;

import java.util.List;

/** Value-only public metadata for one KEEP/REDRAW callback. */
public final class MulliganContext {
    private final int gameId;
    private final long mulliganSessionId;
    private final int mulliganRoundIndex;
    private final int mulliganStepIndex;
    private final int actingPlayerId;
    private final int startingPlayerId;
    private final int cardsToReturn;
    private final int handSize;
    private final MulliganStage stage;
    private final List<CardSelectionCard> handCards;

    MulliganContext(final int gameId, final long mulliganSessionId, final int mulliganRoundIndex,
            final int mulliganStepIndex, final int actingPlayerId, final int startingPlayerId,
            final int cardsToReturn, final int handSize, final MulliganStage stage,
            final List<CardSelectionCard> handCards) {
        this.gameId = gameId;
        this.mulliganSessionId = mulliganSessionId;
        this.mulliganRoundIndex = mulliganRoundIndex;
        this.mulliganStepIndex = mulliganStepIndex;
        this.actingPlayerId = actingPlayerId;
        this.startingPlayerId = startingPlayerId;
        this.cardsToReturn = cardsToReturn;
        this.handSize = handSize;
        this.stage = stage;
        this.handCards = List.copyOf(handCards);
    }

    public int getGameId() {
        return gameId;
    }

    public long getMulliganSessionId() {
        return mulliganSessionId;
    }

    public int getMulliganRoundIndex() {
        return mulliganRoundIndex;
    }

    public int getMulliganStepIndex() {
        return mulliganStepIndex;
    }

    public int getActingPlayerId() {
        return actingPlayerId;
    }

    public int getStartingPlayerId() {
        return startingPlayerId;
    }

    public int getCardsToReturn() {
        return cardsToReturn;
    }

    public int getHandSize() {
        return handSize;
    }

    public MulliganStage getStage() {
        return stage;
    }

    public List<CardSelectionCard> getHandCards() {
        return handCards;
    }
}
