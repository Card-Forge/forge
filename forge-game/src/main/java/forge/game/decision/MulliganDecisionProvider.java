package forge.game.decision;

import forge.MulliganDefs;
import forge.StaticData;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Produces neutral KEEP/REDRAW requests around Forge's existing mulligan callback. */
public final class MulliganDecisionProvider {
    public enum Status {
        READY,
        DECISION,
        COMPLETE,
        AWAITING_FORGE_CALLBACK,
        STALE_MULLIGAN,
        REQUEST_OUTSTANDING,
        UNSUPPORTED_EMPTY_HAND_MULLIGAN,
        UNSUPPORTED_MULLIGAN_RULE,
        UNSUPPORTED_MULLIGAN_STATE
    }

    private long nextSessionId = 1;
    private long nextRequestId = 1;
    private final Map<SessionKey, MulliganSession> sessions = new LinkedHashMap<>();
    private final Set<SessionKey> closedSessions = new HashSet<>();
    private final Supplier<MulliganDefs.MulliganRule> activeRuleSupplier;

    public MulliganDecisionProvider() {
        this(() -> StaticData.instance().getMulliganRule());
    }

    public MulliganDecisionProvider(final MulliganDefs.MulliganRule activeRule) {
        this(() -> activeRule);
    }

    private MulliganDecisionProvider(final Supplier<MulliganDefs.MulliganRule> activeRuleSupplier) {
        this.activeRuleSupplier = Objects.requireNonNull(activeRuleSupplier);
    }

    public SessionStart beginCallback(final Player actingPlayer, final Player startingPlayer,
            final CardCollectionView hand, final int cardsToReturn) {
        Objects.requireNonNull(actingPlayer);
        Objects.requireNonNull(startingPlayer);
        Objects.requireNonNull(hand);
        if (activeRuleSupplier.get() != MulliganDefs.MulliganRule.London) {
            return SessionStart.failure(Status.UNSUPPORTED_MULLIGAN_RULE);
        }
        final Game game = actingPlayer.getGame();
        if (startingPlayer.getGame() != game || game.getPlayers().size() != 2
                || game.getRules().getGameType() != GameType.Constructed || game.getRules().hasCommander()) {
            return SessionStart.failure(Status.UNSUPPORTED_MULLIGAN_STATE);
        }
        if (actingPlayer.isCardInCommand("Backup Plan")
                || containsCardNamed(hand, "Serum Powder")) {
            return SessionStart.failure(Status.UNSUPPORTED_MULLIGAN_STATE);
        }
        if (hand.isEmpty()) {
            return SessionStart.failure(Status.UNSUPPORTED_EMPTY_HAND_MULLIGAN);
        }
        if (cardsToReturn < 0 || cardsToReturn > hand.size()) {
            return SessionStart.failure(Status.UNSUPPORTED_MULLIGAN_STATE);
        }
        if (actingPlayer.getGame().getAge() != GameStage.Mulligan) {
            return SessionStart.failure(Status.STALE_MULLIGAN);
        }
        final SessionKey key = new SessionKey(actingPlayer.getGame().getId(), actingPlayer.getId());
        if (closedSessions.contains(key)) {
            return SessionStart.failure(Status.STALE_MULLIGAN);
        }
        MulliganSession session = sessions.get(key);
        if (session == null) {
            session = new MulliganSession(nextSessionId++, actingPlayer, startingPlayer);
            sessions.put(key, session);
        } else if (!session.ownsForgeParticipants(game, startingPlayer)
                || session.getStartingPlayerId() != startingPlayer.getId()) {
            return SessionStart.failure(Status.STALE_MULLIGAN);
        } else if (session.isTerminal() || !session.isAwaitingForgeCallback()) {
            return SessionStart.failure(Status.STALE_MULLIGAN);
        }
        session.beginCallback(hand, cardsToReturn);
        return SessionStart.ready(session);
    }

    public Generation generateNext(final MulliganSession session) {
        Objects.requireNonNull(session);
        final long startedAtNanos = System.nanoTime();
        if (session.isTerminal()) {
            return Generation.complete(null, null, System.nanoTime() - startedAtNanos);
        }
        if (session.isAwaitingForgeCallback()) {
            return Generation.failure(Status.AWAITING_FORGE_CALLBACK, System.nanoTime() - startedAtNanos);
        }
        if (session.hasActiveRequest()) {
            return Generation.failure(Status.REQUEST_OUTSTANDING, System.nanoTime() - startedAtNanos);
        }
        if (!session.revalidate()) {
            return Generation.failure(Status.STALE_MULLIGAN, System.nanoTime() - startedAtNanos);
        }
        final List<LegalCandidate> candidates = new ArrayList<>();
        candidates.add(LegalCandidate.mulligan(0, MulliganCandidateKind.KEEP));
        candidates.add(LegalCandidate.mulligan(1, MulliganCandidateKind.REDRAW));
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.MULLIGAN,
                candidates, session.getContext());
        session.setActiveRequestId(request.getRequestId());
        return Generation.decision(request, System.nanoTime() - startedAtNanos);
    }

    public Generation apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.MULLIGAN || request.getMulliganContext() == null
                || !request.getCandidates().contains(candidate)
                || candidate.getMulliganKind() == null) {
            return Generation.failure(Status.STALE_MULLIGAN);
        }
        final MulliganContext context = request.getMulliganContext();
        final MulliganSession session = findSession(context);
        if (session == null || !session.ownsContext(context)
                || !session.ownsActiveRequest(request.getRequestId()) || !session.revalidate()) {
            return Generation.failure(Status.STALE_MULLIGAN);
        }
        session.consumeActiveRequest(request.getRequestId());
        if (candidate.getMulliganKind() == MulliganCandidateKind.KEEP) {
            session.markTerminal();
            closedSessions.add(new SessionKey(session.getGameId(), session.getActingPlayerId()));
            sessions.remove(new SessionKey(session.getGameId(), session.getActingPlayerId()));
            return Generation.complete(candidate, MulliganCandidateKind.KEEP);
        }
        session.markAwaitingForgeCallback();
        return Generation.awaitingForgeCallback(candidate);
    }

    private MulliganSession findSession(final MulliganContext context) {
        return sessions.get(new SessionKey(context.getGameId(), context.getActingPlayerId()));
    }

    private static boolean containsCardNamed(final Iterable<Card> cards, final String name) {
        for (final Card card : cards) {
            if (name.equals(card.getName())) {
                return true;
            }
        }
        return false;
    }

    MulliganSession currentSession(final Player actingPlayer) {
        return sessions.get(new SessionKey(actingPlayer.getGame().getId(), actingPlayer.getId()));
    }

    int activeSessionCount() {
        return sessions.size();
    }

    void endGame(final Game game) {
        final int gameId = game.getId();
        sessions.values().stream().filter(session -> session.getGameId() == gameId)
                .forEach(MulliganSession::markTerminal);
        sessions.keySet().removeIf(key -> key.gameId() == gameId);
        closedSessions.removeIf(key -> key.gameId() == gameId);
    }

    private record SessionKey(int gameId, int actingPlayerId) {
    }

    public static final class SessionStart {
        private final Status status;
        private final MulliganSession session;

        private SessionStart(final Status status, final MulliganSession session) {
            this.status = status;
            this.session = session;
        }

        static SessionStart ready(final MulliganSession session) {
            return new SessionStart(Status.READY, session);
        }

        static SessionStart failure(final Status status) {
            return new SessionStart(status, null);
        }

        public Status getStatus() {
            return status;
        }

        public MulliganSession getSession() {
            return session;
        }
    }

    public static final class Generation {
        private final Status status;
        private final DecisionRequest request;
        private final LegalCandidate selectedCandidate;
        private final MulliganCandidateKind selectedKind;
        private final long generationNanos;

        private Generation(final Status status, final DecisionRequest request,
                final LegalCandidate selectedCandidate, final MulliganCandidateKind selectedKind,
                final long generationNanos) {
            this.status = status;
            this.request = request;
            this.selectedCandidate = selectedCandidate;
            this.selectedKind = selectedKind;
            this.generationNanos = generationNanos;
        }

        static Generation decision(final DecisionRequest request, final long generationNanos) {
            return new Generation(Status.DECISION, request, null, null, generationNanos);
        }

        static Generation complete(final LegalCandidate selectedCandidate, final MulliganCandidateKind selectedKind) {
            return complete(selectedCandidate, selectedKind, 0L);
        }

        static Generation complete(final LegalCandidate selectedCandidate, final MulliganCandidateKind selectedKind,
                final long generationNanos) {
            return new Generation(Status.COMPLETE, null, selectedCandidate, selectedKind, generationNanos);
        }

        static Generation awaitingForgeCallback(final LegalCandidate selectedCandidate) {
            return new Generation(Status.AWAITING_FORGE_CALLBACK, null, selectedCandidate,
                    selectedCandidate.getMulliganKind(), 0L);
        }

        static Generation failure(final Status status) {
            return failure(status, 0L);
        }

        static Generation failure(final Status status, final long generationNanos) {
            return new Generation(status, null, null, null, generationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public LegalCandidate getSelectedCandidate() {
            return selectedCandidate;
        }

        public MulliganCandidateKind getSelectedKind() {
            return selectedKind;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }
}
