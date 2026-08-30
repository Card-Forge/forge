package forge.game.card.perpetual;

import forge.card.mana.ManaCost;
import forge.game.card.Card;

public record PerpetualManaCost(long timestamp, ManaCost manaCost) implements PerpetualInterface {
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedManaCost(manaCost, false, timestamp, (long) 0);
    }
}
