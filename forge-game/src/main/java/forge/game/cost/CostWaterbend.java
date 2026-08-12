package forge.game.cost;

import forge.card.mana.ManaCost;

public class CostWaterbend extends CostPartMana {

    public CostWaterbend(final String mana) {
        super(new ManaCost(mana), null);

        maxWaterbend = mana;
    }

    @Override
    public final String toString() {
        return "Waterbend " + getMana().toString();
    }
}
