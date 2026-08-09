package forge.game.decision;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Callback-local state for composing atomic card choices without mutating Forge game state. */
public final class CardSelectionSession {
    private final long selectionSessionId;
    private final int gameId;
    private final Player chooser;
    private final Player affectedPlayer;
    private final SpellAbility source;
    private final int min;
    private final int max;
    private final Map<String, Card> validCards;
    private final Map<String, CardSelectionCard> validIdentities;
    private final List<CardSelectionCard> visibleCards;
    private final List<CardSelectionCard> selectedIdentities = new ArrayList<>();
    private int nextStepIndex;
    private long activeRequestId = -1;

    CardSelectionSession(final long selectionSessionId, final Player chooser, final Player affectedPlayer,
            final SpellAbility source, final int min, final int max, final Iterable<Card> validCards,
            final List<CardSelectionCard> visibleCards) {
        this.selectionSessionId = selectionSessionId;
        this.gameId = chooser.getGame().getId();
        this.chooser = chooser;
        this.affectedPlayer = affectedPlayer;
        this.source = source;
        this.min = min;
        this.max = max;
        this.visibleCards = List.copyOf(visibleCards);
        this.validCards = new LinkedHashMap<>();
        this.validIdentities = new LinkedHashMap<>();
        for (final Card card : validCards) {
            final CardSelectionCard identity = new CardSelectionCard(card);
            this.validCards.put(identity.identityKey(), card);
            this.validIdentities.put(identity.identityKey(), identity);
        }
    }

    long getSelectionSessionId() {
        return selectionSessionId;
    }

    int getGameId() {
        return gameId;
    }

    Player getChooser() {
        return chooser;
    }

    Player getAffectedPlayer() {
        return affectedPlayer;
    }

    SpellAbility getSource() {
        return source;
    }

    int getMin() {
        return min;
    }

    int getMax() {
        return max;
    }

    List<CardSelectionCard> getVisibleCards() {
        return visibleCards;
    }

    List<CardSelectionCard> getSelectedIdentities() {
        return selectedIdentities;
    }

    List<CardSelectionCard> remainingIdentities() {
        final List<CardSelectionCard> remaining = new ArrayList<>();
        for (final CardSelectionCard identity : validIdentities.values()) {
            if (!selectedIdentities.contains(identity)) {
                remaining.add(identity);
            }
        }
        return remaining;
    }

    Card liveCard(final CardSelectionCard identity) {
        for (final Card card : affectedPlayer.getCardsIn(ZoneType.Hand)) {
            if (card.getId() == identity.getCardId()
                    && card.getGameTimestamp() == identity.getGameTimestamp()) {
                return card;
            }
        }
        return null;
    }

    boolean revalidate() {
        if (!chooser.isInGame() || !affectedPlayer.isInGame() || chooser.getGame().getId() != gameId
                || affectedPlayer.getGame().getId() != gameId || selectedIdentities.size() > max) {
            return false;
        }
        for (final CardSelectionCard identity : validIdentities.values()) {
            if (liveCard(identity) == null || !validCards.containsKey(identity.identityKey())) {
                return false;
            }
        }
        return true;
    }

    boolean select(final CardSelectionCard identity) {
        if (!validIdentities.containsKey(identity.identityKey()) || selectedIdentities.contains(identity)
                || liveCard(identity) == null) {
            return false;
        }
        selectedIdentities.add(identity);
        return true;
    }

    CardCollection selectedLiveCards() {
        final CardCollection result = new CardCollection();
        for (final CardSelectionCard identity : selectedIdentities) {
            final Card card = liveCard(identity);
            if (card == null) {
                return null;
            }
            result.add(card);
        }
        return result;
    }

    int allocateStepIndex() {
        return nextStepIndex++;
    }

    void setActiveRequestId(final long requestId) {
        activeRequestId = requestId;
    }

    boolean ownsActiveRequest(final long requestId) {
        return activeRequestId == requestId;
    }
}
