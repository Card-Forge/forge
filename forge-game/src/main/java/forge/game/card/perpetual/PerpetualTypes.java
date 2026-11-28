package forge.game.card.perpetual;

import java.util.Set;

import forge.card.CardType;
import forge.card.RemoveType;
import forge.game.card.Card;

public record PerpetualTypes(long timestamp, CardType addTypes, CardType removeTypes, Set<RemoveType> removeXTypes) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedCardTypesEffect(addTypes, removeTypes, false, removeXTypes, timestamp);
    }

}
