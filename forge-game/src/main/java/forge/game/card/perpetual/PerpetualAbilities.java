package forge.game.card.perpetual;

import forge.game.card.Card;
import forge.game.card.CardTraitChanges;

public record PerpetualAbilities(long timestamp, CardTraitChanges changes) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedCardTraits(changes.copy(c, false), timestamp, (long) 0);
    }

}
