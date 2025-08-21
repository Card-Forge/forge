package forge.game.card.perpetual;

import java.util.List;

import forge.game.card.Card;

public record PerpetualKeywords(long timestamp, List<String> addKeywords, List<String> removeKeywords, boolean removeAll) implements PerpetualInterface {
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedCardKeywords(addKeywords, removeKeywords, removeAll, timestamp, null);
    }
}
