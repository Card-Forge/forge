package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Narrow diagnostic adapter for the own-hand TgtChoose discard callback used by Izzet Charm. */
public final class DiscardCardSelectionAdapter {
    public enum Status {
        SUPPORTED,
        UNSUPPORTED_MODE,
        UNSUPPORTED_CHOOSER,
        UNSUPPORTED_DEPENDENCY,
        UNSUPPORTED_ZONE,
        UNSUPPORTED_HIDDEN_CARD_SELECTION,
        INVALID_DOMAIN
    }

    public enum ReplayStatus {
        COMPLETE,
        NOT_SUPPORTED,
        INVALID_CONTROLLER_RESULT,
        MAPPING_FAILED
    }

    private final CardSelectionDecisionProvider provider = new CardSelectionDecisionProvider();

    public Capture begin(final Player chooser, final Player affectedPlayer, final SpellAbility source,
            final CardCollection validCards, final int min, final int max,
            final CardCollectionView visibleToChooser) {
        if (!"TgtChoose".equals(source.getParam("Mode"))) {
            return Capture.unsupported(Status.UNSUPPORTED_MODE, "MODE_NOT_TGT_CHOOSE");
        }
        if (!chooser.equals(affectedPlayer)) {
            return Capture.unsupported(Status.UNSUPPORTED_CHOOSER, "CHOOSER_NOT_AFFECTED_PLAYER");
        }
        if (source.hasParam("RevealNumber") || source.hasParam("UnlessType")) {
            return Capture.unsupported(Status.UNSUPPORTED_DEPENDENCY,
                    source.hasParam("RevealNumber") ? "REVEAL_NUMBER" : "UNLESS_TYPE");
        }
        for (final Card card : validCards) {
            if (!containsIdentity(affectedPlayer.getCardsIn(ZoneType.Hand), card)) {
                return Capture.unsupported(Status.UNSUPPORTED_ZONE, "VALID_CARD_NOT_IN_AFFECTED_HAND");
            }
        }

        final CardSelectionDecisionProvider.SessionStart start = provider.beginSession(chooser, affectedPlayer,
                source, validCards, min, max, visibleToChooser);
        if (start.getStatus() == CardSelectionDecisionProvider.Status.UNSUPPORTED_HIDDEN_CARD_SELECTION) {
            return Capture.unsupported(Status.UNSUPPORTED_HIDDEN_CARD_SELECTION, start.getReason().name());
        }
        if (start.getStatus() != CardSelectionDecisionProvider.Status.READY) {
            return Capture.unsupported(Status.INVALID_DOMAIN,
                    start.getReason() == null ? start.getStatus().name() : start.getReason().name());
        }
        return Capture.supported(start.getSession(), validCards, min, max);
    }

    public Replay replay(final Capture capture, final CardCollectionView controllerResult) {
        if (capture == null || capture.status != Status.SUPPORTED) {
            return Replay.failure(ReplayStatus.NOT_SUPPORTED, "ADAPTER_NOT_SUPPORTED", List.of());
        }
        if (controllerResult == null || controllerResult.size() < capture.min
                || controllerResult.size() > capture.max) {
            return Replay.failure(ReplayStatus.INVALID_CONTROLLER_RESULT, "RESULT_SIZE_OUT_OF_BOUNDS", List.of());
        }

        final List<CardSelectionCard> selected = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final Card card : controllerResult) {
            final CardSelectionCard identity = new CardSelectionCard(card);
            if (!seen.add(identity.identityKey())) {
                return Replay.failure(ReplayStatus.INVALID_CONTROLLER_RESULT, "DUPLICATE_RESULT_CARD", List.of());
            }
            if (!containsIdentity(capture.validCards, card)) {
                return Replay.failure(ReplayStatus.INVALID_CONTROLLER_RESULT, "RESULT_CARD_NOT_VALID", List.of());
            }
            selected.add(identity);
        }
        selected.sort(Comparator.comparing(CardSelectionCard::selectionSemanticKey));

        final List<ReplayStep> steps = new ArrayList<>();
        CardSelectionDecisionProvider.Generation generation = provider.generateNext(capture.session, null);
        int selectedIndex = 0;
        while (generation.getStatus() == CardSelectionDecisionProvider.Status.DECISION) {
            final DecisionRequest request = generation.getRequest();
            steps.add(new ReplayStep(request, generation.getGenerationNanos()));
            final LegalCandidate mapped;
            if (selectedIndex < selected.size()) {
                final CardSelectionCard wanted = selected.get(selectedIndex++);
                mapped = request.getCandidates().stream()
                        .filter(candidate -> wanted.equals(candidate.getCardSelectionCard()))
                        .findFirst().orElse(null);
            } else {
                mapped = request.getCandidates().stream()
                        .filter(candidate -> candidate.getCardSelectionKind() == CardSelectionCandidateKind.DONE)
                        .findFirst().orElse(null);
            }
            if (mapped == null) {
                return Replay.failure(ReplayStatus.MAPPING_FAILED, "RESULT_CARD_NOT_IN_ATOMIC_REQUEST", steps);
            }
            generation = provider.apply(request, mapped);
        }

        if (generation.getStatus() != CardSelectionDecisionProvider.Status.COMPLETE
                || selectedIndex != selected.size() || generation.getSelectedCards() == null
                || !sameIdentitySet(generation.getSelectedCards(), controllerResult)) {
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

    private static boolean sameIdentitySet(final CardCollectionView first, final CardCollectionView second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (final Card card : first) {
            if (!containsIdentity(second, card)) {
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
        private final int min;
        private final int max;

        private Capture(final Status status, final String reason, final CardSelectionSession session,
                final CardCollection validCards, final int min, final int max) {
            this.status = status;
            this.reason = reason;
            this.session = session;
            this.validCards = validCards;
            this.min = min;
            this.max = max;
        }

        static Capture supported(final CardSelectionSession session, final CardCollection validCards,
                final int min, final int max) {
            return new Capture(Status.SUPPORTED, null, session, new CardCollection(validCards), min, max);
        }

        static Capture unsupported(final Status status, final String reason) {
            return new Capture(status, reason, null, null, 0, 0);
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

        public int getInitialCandidateCount() {
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
