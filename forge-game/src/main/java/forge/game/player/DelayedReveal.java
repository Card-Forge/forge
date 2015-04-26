package forge.game.player;

import java.io.Serializable;
import java.util.Collection;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;

/**
 * Stores information to reveal cards after a delay unless those cards can be
 * revealed in the same dialog as cards being selected
 */
public class DelayedReveal implements Serializable {
    private static final long serialVersionUID = 5516713460440436615L;

    private final Collection<CardView> cards;
    private final ZoneType zone;
    private final PlayerView owner;
    private final String messagePrefix;

    public DelayedReveal(Iterable<Card> cards0, ZoneType zone0, PlayerView owner0) {
        this(cards0, zone0, owner0, null);
    }
    public DelayedReveal(Iterable<Card> cards0, ZoneType zone0, PlayerView owner0, String messagePrefix0) {
        cards = CardView.getCollection(cards0);
        zone = zone0;
        owner = owner0;
        messagePrefix = messagePrefix0;
    }

    public Collection<CardView> getCards() {
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

    public void remove(CardView card) {
        cards.remove(card);
    }

}
