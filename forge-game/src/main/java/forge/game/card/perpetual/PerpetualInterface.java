package forge.game.card.perpetual;

import forge.game.card.Card;

public interface PerpetualInterface {
    long getTimestamp();
    void applyEffect(Card c);
}
