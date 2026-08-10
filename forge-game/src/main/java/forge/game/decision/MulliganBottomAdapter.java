package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Narrow adapter for the ordered own-hand bottom callback of the London mulligan. */
public final class MulliganBottomAdapter {
    public enum Status {
        SUPPORTED,
        INVALID_DOMAIN,
        STALE_CALLBACK
    }

    public enum ReplayStatus {
        COMPLETE,
        NOT_SUPPORTED,
        MAPPING_FAILED
    }

    private final CardSelectionDecisionProvider provider = new CardSelectionDecisionProvider();

    public Capture begin(final Player actingPlayer, final CardCollectionView callbackHand,
            final int cardsToReturn) {
        if (actingPlayer == null || callbackHand == null) {
            return Capture.unsupported(Status.INVALID_DOMAIN, "NULL_CALLBACK_ARGUMENT");
        }
        if (cardsToReturn < 0 || cardsToReturn > callbackHand.size()) {
            return Capture.unsupported(Status.INVALID_DOMAIN, "CARDS_TO_RETURN_OUT_OF_BOUNDS");
        }

        final CardCollection authoritativeHand = new CardCollection(callbackHand);
        for (final Card card : authoritativeHand) {
            if (!containsIdentity(actingPlayer.getCardsIn(ZoneType.Hand), card)) {
                return Capture.unsupported(Status.STALE_CALLBACK, "CALLBACK_HAND_IDENTITY_NOT_LIVE");
            }
        }

        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(actingPlayer, actingPlayer,
                CardSelectionAdapter.MULLIGAN_BOTTOM, authoritativeHand, cardsToReturn, cardsToReturn,
                authoritativeHand);
        if (start.getStatus() != CardSelectionDecisionProvider.Status.READY) {
            final String reason = start.getReason() == null ? start.getStatus().name() : start.getReason().name();
            return Capture.unsupported(Status.INVALID_DOMAIN, reason);
        }
        return Capture.supported(start.getSession(), authoritativeHand, cardsToReturn);
    }

    public Replay replay(final Capture capture, final CardCollectionView controllerResult) {
        if (capture == null || capture.status != Status.SUPPORTED) {
            return Replay.failure(ReplayStatus.NOT_SUPPORTED, "ADAPTER_NOT_SUPPORTED", List.of());
        }
        if (controllerResult == null || controllerResult.size() != capture.cardsToReturn) {
            return Replay.failure(ReplayStatus.MAPPING_FAILED, "RESULT_SIZE_NOT_EXACT", List.of());
        }

        final List<CardSelectionCard> selected = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final Card card : controllerResult) {
            final CardSelectionCard identity = new CardSelectionCard(card);
            if (!seen.add(identity.identityKey())) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "DUPLICATE_RESULT_CARD", List.of());
            }
            if (!containsIdentity(capture.validCards, card)) {
                final String reason = hasCardId(capture.validCards, card)
                        ? "RESULT_CARD_TIMESTAMP_STALE" : "RESULT_CARD_NOT_IN_CALLBACK_HAND";
                return Replay.failure(ReplayStatus.MAPPING_FAILED, reason, List.of());
            }
            selected.add(identity);
        }

        final List<ReplayStep> steps = new ArrayList<>();
        CardSelectionDecisionProvider.Generation generation = provider.generateNext(capture.session, null);
        int selectedIndex = 0;
        while (generation.getStatus() == CardSelectionDecisionProvider.Status.DECISION) {
            final DecisionRequest request = generation.getRequest();
            steps.add(new ReplayStep(request, generation.getGenerationNanos()));
            if (selectedIndex >= selected.size()) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "RESULT_CARD_NOT_IN_ATOMIC_REQUEST", steps);
            }
            final CardSelectionCard wanted = selected.get(selectedIndex++);
            final LegalCandidate mapped = request.getCandidates().stream()
                    .filter(candidate -> wanted.equals(candidate.getCardSelectionCard()))
                    .findFirst().orElse(null);
            if (mapped == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "RESULT_CARD_NOT_IN_ATOMIC_REQUEST", steps);
            }
            generation = provider.apply(request, mapped);
        }

        if (generation.getStatus() != CardSelectionDecisionProvider.Status.COMPLETE
                || selectedIndex != selected.size() || generation.getSelectedCards() == null
                || !sameIdentitySequence(generation.getSelectedCards(), controllerResult)) {
            final String reason = generation.getReason() == null ? generation.getStatus().name()
                    : generation.getReason().name();
            return Replay.failure(ReplayStatus.MAPPING_FAILED, reason, steps);
        }
        return Replay.complete(steps, generation.getSelectedCards());
    }

    private static boolean containsIdentity(final Iterable<Card> cards, final Card wanted) {
        for (final Card card : cards) {
            if (card.getId() == wanted.getId() && card.getGameTimestamp() == wanted.getGameTimestamp()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCardId(final Iterable<Card> cards, final Card wanted) {
        for (final Card card : cards) {
            if (card.getId() == wanted.getId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameIdentitySequence(final CardCollectionView first, final CardCollectionView second) {
        if (first.size() != second.size()) {
            return false;
        }
        final List<Card> firstCards = new ArrayList<>(first);
        final List<Card> secondCards = new ArrayList<>(second);
        for (int index = 0; index < firstCards.size(); index++) {
            if (firstCards.get(index).getId() != secondCards.get(index).getId()
                    || firstCards.get(index).getGameTimestamp() != secondCards.get(index).getGameTimestamp()) {
                return false;
            }
        }
        return true;
    }

    public static final class Capture {
        private final Status status;
        private final String reason;
        private final CardSelectionSession session;
        private final CardCollection validCards;
        private final int cardsToReturn;

        private Capture(final Status status, final String reason, final CardSelectionSession session,
                final CardCollection validCards, final int cardsToReturn) {
            this.status = status;
            this.reason = reason;
            this.session = session;
            this.validCards = validCards;
            this.cardsToReturn = cardsToReturn;
        }

        static Capture supported(final CardSelectionSession session, final CardCollection validCards,
                final int cardsToReturn) {
            return new Capture(Status.SUPPORTED, null, session, new CardCollection(validCards), cardsToReturn);
        }

        static Capture unsupported(final Status status, final String reason) {
            return new Capture(status, reason, null, null, 0);
        }

        public Status getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public long getSelectionSessionId() {
            return session == null ? -1 : session.getSelectionSessionId();
        }

        public int getGameId() {
            return session == null ? -1 : session.getGameId();
        }

        public int getCardsToReturn() {
            return cardsToReturn;
        }

        public int getInitialHandSize() {
            return validCards == null ? 0 : validCards.size();
        }
    }

    public static final class ReplayStep {
        private final DecisionRequest request;
        private final long generationNanos;

        ReplayStep(final DecisionRequest request, final long generationNanos) {
            this.request = request;
            this.generationNanos = generationNanos;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }

    public static final class Replay {
        private final ReplayStatus status;
        private final String reason;
        private final List<ReplayStep> steps;
        private final CardCollection completedCards;

        private Replay(final ReplayStatus status, final String reason, final List<ReplayStep> steps,
                final CardCollection completedCards) {
            this.status = status;
            this.reason = reason;
            this.steps = List.copyOf(steps);
            this.completedCards = completedCards;
        }

        static Replay complete(final List<ReplayStep> steps, final CardCollection completedCards) {
            return new Replay(ReplayStatus.COMPLETE, null, steps, completedCards);
        }

        static Replay failure(final ReplayStatus status, final String reason, final List<ReplayStep> steps) {
            return new Replay(status, reason, steps, null);
        }

        public ReplayStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public List<ReplayStep> getSteps() {
            return steps;
        }

        public CardCollection getCompletedCards() {
            return completedCards;
        }
    }
}
