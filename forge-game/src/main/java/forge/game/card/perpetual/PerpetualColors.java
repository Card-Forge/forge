package forge.game.card.perpetual;

import forge.card.ColorSet;
import forge.game.card.Card;

public record PerpetualColors(long timestamp, ColorSet colors, boolean overwrite) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyCard(Card c) {
        c.addColor(colors, !overwrite, timestamp, (long) 0, false);
    }

}
