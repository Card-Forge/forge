package forge.game.card.perpetual;

import forge.card.mana.ManaCost;
import forge.game.card.Card;
import forge.game.cost.Cost;

public record PerpetualManaCost(long timestamp, ManaCost manaCost) implements PerpetualInterface {

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void applyEffect(Card c) {
        c.addChangedManaCost(manaCost, timestamp, (long) 0);

        c.updateManaCostForView();

        if (c.getFirstSpellAbility() != null) {
            Cost cost = c.getFirstSpellAbility().getPayCosts().copyWithDefinedMana(manaCost);
            c.getFirstSpellAbility().setPayCosts(cost);
        }
    }

}
