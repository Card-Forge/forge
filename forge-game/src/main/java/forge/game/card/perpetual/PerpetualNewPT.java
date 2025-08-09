package forge.game.card.perpetual;

import forge.game.card.Card;

public record PerpetualNewPT(long timestamp, Integer power, Integer toughness) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addNewPT(power, toughness, timestamp, (long) 0);
    }
}
