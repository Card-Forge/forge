package forge.game.cost;

import forge.card.mana.ManaCost;

public class CostWaterbend extends CostPartMana {
    private int maxWaterbend;

    public CostWaterbend(final ManaCost cost) {
        super(cost, null);

        maxWaterbend = cost.getGenericCost();
    }

    public int getMaxWaterbend() {
        return maxWaterbend;
    }

    @Override
    public final String toString() {
        return "Waterbend " + cost.toString();
    }
}
