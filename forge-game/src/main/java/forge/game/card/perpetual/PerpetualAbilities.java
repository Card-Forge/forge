package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.card.CardTraitChanges;
import forge.game.card.ICardTraitChanges;

public record PerpetualAbilities(long timestamp, ICardTraitChanges changes) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedCardTraits(changes.copy(c, false), timestamp, (long) 0, true);
        if (changes instanceof CardTraitChanges ctc && ctc.containsCostChange()) {
            c.calculatePerpetualAdjustedManaCost();
        }
    }
}
