package forge.game.player;

import java.util.Collection;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

//Stores information to reveal cards after a delay unless those cards can be revealed in the same dialog as cards being selected
public class DelayedReveal {
    public final Collection<Card> cards;
    public final ZoneType zone;
    public final Player owner;
    public final String messagePrefix;

    public DelayedReveal(Collection<Card> cards0, ZoneType zone0, Player owner0) {
        this(cards0, zone0, owner0, null);
    }
    public DelayedReveal(Collection<Card> cards0, ZoneType zone0, Player owner0, String messagePrefix0) {
        cards = cards0;
        zone = zone0;
        owner = owner0;
        messagePrefix = messagePrefix0;
    }

    public void reveal(PlayerController controller) {
        controller.reveal(cards, zone, owner, messagePrefix);
    }
}
