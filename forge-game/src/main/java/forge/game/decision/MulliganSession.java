package forge.game.decision;

import forge.game.Game;
import forge.game.GameStage;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Internal parent lifecycle for one player's game-level mulligan process. */
public final class MulliganSession {
    private static final long NO_ACTIVE_REQUEST = -1L;

    private final long mulliganSessionId;
    private final Game game;
    private final Player actingPlayer;
    private final Player startingPlayer;
    private int nextRoundIndex;
    private int nextStepIndex;
    private MulliganContext context;
    private long activeRequestId = NO_ACTIVE_REQUEST;
    private boolean awaitingForgeCallback;
    private boolean terminal;

    MulliganSession(final long mulliganSessionId, final Player actingPlayer, final Player startingPlayer) {
        this.mulliganSessionId = mulliganSessionId;
        this.game = Objects.requireNonNull(actingPlayer).getGame();
        this.actingPlayer = actingPlayer;
        this.startingPlayer = Objects.requireNonNull(startingPlayer);
    }

    public long getMulliganSessionId() {
        return mulliganSessionId;
    }

    public int getGameId() {
        return game.getId();
    }

    public int getActingPlayerId() {
        return actingPlayer.getId();
    }

    public int getStartingPlayerId() {
        return startingPlayer.getId();
    }

    boolean ownsForgeParticipants(final Game currentGame, final Player currentStartingPlayer) {
        return game == currentGame && startingPlayer == currentStartingPlayer;
    }

    MulliganContext getContext() {
        return context;
    }

    public boolean isTerminal() {
        return terminal;
    }

    boolean isAwaitingForgeCallback() {
        return awaitingForgeCallback;
    }

    boolean hasActiveRequest() {
        return activeRequestId != NO_ACTIVE_REQUEST;
    }

    void beginCallback(final CardCollectionView hand, final int cardsToReturn) {
        final List<CardSelectionCard> identities = new ArrayList<>();
        for (final Card card : hand) {
            identities.add(new CardSelectionCard(card));
        }
        final int roundIndex = nextRoundIndex++;
        context = new MulliganContext(game.getId(), mulliganSessionId, roundIndex, nextStepIndex++,
                actingPlayer.getId(), startingPlayer.getId(), cardsToReturn, hand.size(),
                MulliganStage.KEEP_OR_REDRAW, identities);
        awaitingForgeCallback = false;
    }

    boolean revalidate() {
        if (terminal || context == null || game.getAge() != GameStage.Mulligan
                || actingPlayer.getId() != context.getActingPlayerId()
                || startingPlayer.getId() != context.getStartingPlayerId()
                || actingPlayer.getGame() != game
                || actingPlayer.getGame().getId() != context.getGameId()
                || startingPlayer.getGame() != game
                || context.getCardsToReturn() < 0
                || context.getCardsToReturn() > context.getHandSize()
                || actingPlayer.getCardsIn(ZoneType.Hand).size() != context.getHandSize()) {
            return false;
        }
        for (final CardSelectionCard identity : context.getHandCards()) {
            if (liveCard(identity) == null) {
                return false;
            }
        }
        return true;
    }

    boolean ownsContext(final MulliganContext expected) {
        return expected != null && context != null
                && context.getGameId() == expected.getGameId()
                && context.getMulliganSessionId() == expected.getMulliganSessionId()
                && context.getMulliganRoundIndex() == expected.getMulliganRoundIndex()
                && context.getMulliganStepIndex() == expected.getMulliganStepIndex()
                && context.getActingPlayerId() == expected.getActingPlayerId()
                && context.getStartingPlayerId() == expected.getStartingPlayerId()
                && context.getCardsToReturn() == expected.getCardsToReturn()
                && context.getHandSize() == expected.getHandSize()
                && context.getStage() == expected.getStage()
                && context.getHandCards().equals(expected.getHandCards());
    }

    private Card liveCard(final CardSelectionCard identity) {
        for (final Card card : actingPlayer.getCardsIn(ZoneType.Hand)) {
            if (card.getId() == identity.getCardId()
                    && card.getGameTimestamp() == identity.getGameTimestamp()) {
                return card;
            }
        }
        return null;
    }

    void setActiveRequestId(final long requestId) {
        activeRequestId = requestId;
    }

    boolean ownsActiveRequest(final long requestId) {
        return activeRequestId == requestId;
    }

    void consumeActiveRequest(final long requestId) {
        if (!ownsActiveRequest(requestId)) {
            throw new IllegalStateException("Mulligan request is not active");
        }
        activeRequestId = NO_ACTIVE_REQUEST;
    }

    void markAwaitingForgeCallback() {
        awaitingForgeCallback = true;
    }

    void markTerminal() {
        terminal = true;
        awaitingForgeCallback = false;
        activeRequestId = NO_ACTIVE_REQUEST;
    }
}
