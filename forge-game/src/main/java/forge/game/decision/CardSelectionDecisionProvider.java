package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Produces completion-safe atomic card choices from one immutable Forge callback domain. */
public final class CardSelectionDecisionProvider {
    public enum Status {
        READY,
        DECISION,
        COMPLETE,
        INVALID_DOMAIN,
        UNSUPPORTED_HIDDEN_CARD_SELECTION,
        STALE_SELECTION
    }

    public enum Reason {
        NEGATIVE_BOUNDS,
        MIN_EXCEEDS_MAX,
        IMPOSSIBLE_MINIMUM,
        HIDDEN_SELECTABLE_CARD,
        LIVE_STATE_CHANGED,
        REQUEST_OWNERSHIP,
        ILLEGAL_CANDIDATE
    }

    private long nextRequestId = 1;
    private long nextSessionId = 1;

    public SessionStart beginSession(final Player chooser, final Player affectedPlayer, final SpellAbility source,
            final CardCollection validCards, final int min, final int max,
            final CardCollectionView visibleToChooser) {
        Objects.requireNonNull(chooser);
        Objects.requireNonNull(affectedPlayer);
        Objects.requireNonNull(source);
        Objects.requireNonNull(validCards);
        Objects.requireNonNull(visibleToChooser);
        if (min < 0 || max < 0) {
            return SessionStart.failure(Status.INVALID_DOMAIN, Reason.NEGATIVE_BOUNDS);
        }
        if (min > max) {
            return SessionStart.failure(Status.INVALID_DOMAIN, Reason.MIN_EXCEEDS_MAX);
        }
        if (validCards.size() < min) {
            return SessionStart.failure(Status.INVALID_DOMAIN, Reason.IMPOSSIBLE_MINIMUM);
        }

        final Map<String, CardSelectionCard> visible = new LinkedHashMap<>();
        for (final Card card : visibleToChooser) {
            final CardSelectionCard identity = new CardSelectionCard(card);
            visible.put(identity.identityKey(), identity);
        }
        for (final Card card : validCards) {
            if (!visible.containsKey(new CardSelectionCard(card).identityKey())) {
                return SessionStart.failure(Status.UNSUPPORTED_HIDDEN_CARD_SELECTION,
                        Reason.HIDDEN_SELECTABLE_CARD);
            }
        }
        final CardSelectionSession session = new CardSelectionSession(nextSessionId++, chooser, affectedPlayer,
                source, min, max, validCards, new ArrayList<>(visible.values()));
        return SessionStart.ready(session);
    }

    public Generation generateNext(final CardSelectionSession session, final ActionContinuation continuation) {
        Objects.requireNonNull(session);
        final long startedAtNanos = System.nanoTime();
        if (!session.revalidate()) {
            return Generation.failure(Status.STALE_SELECTION, Reason.LIVE_STATE_CHANGED,
                    System.nanoTime() - startedAtNanos);
        }
        final int selectedCount = session.getSelectedIdentities().size();
        if (selectedCount == session.getMax()) {
            return complete(session, startedAtNanos);
        }
        final List<CardSelectionCard> remaining = session.remainingIdentities();
        if (selectedCount < session.getMin() && remaining.size() < session.getMin() - selectedCount) {
            return Generation.failure(Status.INVALID_DOMAIN, Reason.IMPOSSIBLE_MINIMUM,
                    System.nanoTime() - startedAtNanos);
        }

        remaining.sort(Comparator.comparing(CardSelectionCard::selectionSemanticKey));
        final List<LegalCandidate> candidates = new ArrayList<>();
        for (final CardSelectionCard identity : remaining) {
            candidates.add(LegalCandidate.selectCard(candidates.size(), identity));
        }
        if (selectedCount >= session.getMin()) {
            candidates.add(LegalCandidate.cardSelectionDone(candidates.size()));
        }
        if (candidates.isEmpty()) {
            return Generation.failure(Status.INVALID_DOMAIN, Reason.IMPOSSIBLE_MINIMUM,
                    System.nanoTime() - startedAtNanos);
        }

        final Integer actionSubdecisionIndex = continuation == null ? null : continuation.nextSubdecisionIndex();
        final CardSelectionContext context = new CardSelectionContext(session, session.allocateStepIndex(),
                continuation, actionSubdecisionIndex);
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.CARD_SELECTION,
                candidates, context);
        session.setActiveRequestId(request.getRequestId());
        return Generation.decision(request, System.nanoTime() - startedAtNanos);
    }

    public Generation apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.CARD_SELECTION || request.getCardSelectionContext() == null
                || !request.getCandidates().contains(candidate)) {
            return Generation.failure(Status.STALE_SELECTION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        final CardSelectionContext context = request.getCardSelectionContext();
        final CardSelectionSession session = context.getSession();
        if (!session.ownsActiveRequest(request.getRequestId())) {
            return Generation.failure(Status.STALE_SELECTION, Reason.REQUEST_OWNERSHIP, 0L);
        }
        if (!session.revalidate()) {
            return Generation.failure(Status.STALE_SELECTION, Reason.LIVE_STATE_CHANGED, 0L);
        }
        if (candidate.getCardSelectionKind() == CardSelectionCandidateKind.DONE) {
            if (session.getSelectedIdentities().size() < session.getMin()) {
                return Generation.failure(Status.STALE_SELECTION, Reason.ILLEGAL_CANDIDATE, 0L);
            }
            return complete(session, System.nanoTime());
        }
        if (candidate.getCardSelectionKind() != CardSelectionCandidateKind.SELECT_CARD
                || candidate.getCardSelectionCard() == null
                || !session.select(candidate.getCardSelectionCard())) {
            return Generation.failure(Status.STALE_SELECTION, Reason.ILLEGAL_CANDIDATE, 0L);
        }
        return generateNext(session, context.getContinuation());
    }

    private static Generation complete(final CardSelectionSession session, final long startedAtNanos) {
        final CardCollection selected = session.selectedLiveCards();
        if (selected == null) {
            return Generation.failure(Status.STALE_SELECTION, Reason.LIVE_STATE_CHANGED,
                    System.nanoTime() - startedAtNanos);
        }
        return Generation.complete(selected, System.nanoTime() - startedAtNanos);
    }

    public static final class SessionStart {
        private final Status status;
        private final Reason reason;
        private final CardSelectionSession session;

        private SessionStart(final Status status, final Reason reason, final CardSelectionSession session) {
            this.status = status;
            this.reason = reason;
            this.session = session;
        }

        static SessionStart ready(final CardSelectionSession session) {
            return new SessionStart(Status.READY, null, session);
        }

        static SessionStart failure(final Status status, final Reason reason) {
            return new SessionStart(status, reason, null);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public CardSelectionSession getSession() {
            return session;
        }
    }

    public static final class Generation {
        private final Status status;
        private final Reason reason;
        private final DecisionRequest request;
        private final CardCollection selectedCards;
        private final long generationNanos;

        private Generation(final Status status, final Reason reason, final DecisionRequest request,
                final CardCollection selectedCards, final long generationNanos) {
            this.status = status;
            this.reason = reason;
            this.request = request;
            this.selectedCards = selectedCards;
            this.generationNanos = generationNanos;
        }

        static Generation decision(final DecisionRequest request, final long generationNanos) {
            return new Generation(Status.DECISION, null, request, null, generationNanos);
        }

        static Generation complete(final CardCollection selectedCards, final long generationNanos) {
            return new Generation(Status.COMPLETE, null, null, selectedCards, generationNanos);
        }

        static Generation failure(final Status status, final Reason reason, final long generationNanos) {
            return new Generation(status, reason, null, null, generationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public Reason getReason() {
            return reason;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public CardCollection getSelectedCards() {
            return selectedCards;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }
}
