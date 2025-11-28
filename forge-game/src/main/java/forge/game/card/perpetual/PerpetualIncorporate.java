package forge.game.card.perpetual;

import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.Card;

public record PerpetualIncorporate(long timestamp, ManaCost incorporate) implements PerpetualInterface {
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        ColorSet colors = ColorSet.fromMask(incorporate.getColorProfile());
        c.addChangedManaCost(incorporate, true, timestamp, (long) 0);
        c.addColorByText(colors, true, timestamp, null);
    }
}
