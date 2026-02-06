package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.staticability.StaticAbility;

public interface PerpetualInterface {
    default long getTimestamp() { return -1; }
    default void applyEffect(Card c) {}

    default StaticAbility createEffect(Card c) {return null;}
}
