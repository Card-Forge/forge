package forge.game.player;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.zone.ZoneType;

//Stores information to reveal cards after a delay unless those cards can be revealed in the same dialog as cards being selected
public class DelayedReveal {
    private final CardCollection cards;
    private final ZoneType zone;
    private final Player owner;
    private final String messagePrefix;
    private boolean revealed;

    public DelayedReveal(Iterable<Card> cards0, ZoneType zone0, Player owner0) {
        this(cards0, zone0, owner0, null);
    }
    public DelayedReveal(Iterable<Card> cards0, ZoneType zone0, Player owner0, String messagePrefix0) {
        cards = new CardCollection(cards0); //create copy of list to allow modification
        zone = zone0;
        owner = owner0;
        messagePrefix = messagePrefix0;
    }

    public Iterable<Card> getCards() {
        return cards;
    }

    public ZoneType getZone() {
        return zone;
    }

    public Player getOwner() {
        return owner;
    }

    public void remove(Card card) {
        cards.remove(card);
    }

    public void reveal(PlayerController controller) {
        if (revealed) { return; } //avoid revealing more than once
        revealed = true;
        controller.reveal(cards, zone, owner, messagePrefix);
    }
}
