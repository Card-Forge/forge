package forge.game.card.perpetual;

import forge.game.card.Card;

public record PerpetualPTBoost(long timestamp, Integer power, Integer toughness) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyCard(Card c) {
        c.addPTBoost(power, toughness, this.getTimestamp(), (long) 0);
    }
}
