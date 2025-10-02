package forge.game.card.perpetual;

import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.cost.Cost;

public record PerpetualIncorporate(long timestamp, ManaCost incorporate) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        ColorSet colors = ColorSet.fromMask(incorporate.getColorProfile());
        final ManaCost newCost = ManaCost.combine(c.getManaCost(), incorporate);
        c.addChangedManaCost(newCost, timestamp, (long) 0);
        c.addColor(colors, true, timestamp, null);
        c.updateManaCostForView();

        if (c.getFirstSpellAbility() != null) {
            c.getFirstSpellAbility().getPayCosts().add(new Cost(incorporate, false));
        }
    }

}
