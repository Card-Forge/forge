package forge.game.player;

import java.io.Serializable;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;

/**
 * Stores information to reveal cards after a delay unless those cards can be
 * revealed in the same dialog as cards being selected
 */
public class DelayedReveal implements Serializable {
    private static final long serialVersionUID = 5516713460440436615L;

    private final TrackableCollection<CardView> cards;
    private final ZoneType zone;
    private final PlayerView owner;
    private final String messagePrefix;

    public DelayedReveal(final Iterable<Card> cards0, final ZoneType zone0, final PlayerView owner0) {
        this(cards0, zone0, owner0, "");
    }
    public DelayedReveal(final Iterable<Card> cards0, final ZoneType zone0, final PlayerView owner0, final String messagePrefix0) {
        cards = CardView.getCollection(cards0);
        zone = zone0;
        owner = owner0;
        messagePrefix = messagePrefix0;
    }

    public TrackableCollection<CardView> getCards() {
        return cards;
    }

    public ZoneType getZone() {
        return zone;
    }

    public PlayerView getOwner() {
        return owner;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public void remove(final CardView card) {
        cards.remove(card);
    }

}
