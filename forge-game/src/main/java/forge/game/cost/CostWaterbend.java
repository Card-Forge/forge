package forge.game.cost;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

public class CostWaterbend extends CostPartMana {

    public CostWaterbend(final String mana) {
        super(new ManaCost(new ManaCostParser(mana)), null);

        maxWaterbend = mana;
    }

    @Override
    public final String toString() {
        return "Waterbend " + getMana().toString();
    }
}
