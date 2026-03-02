package forge.game.cost;

import forge.game.card.CounterEnumType;

public class CostBlight extends CostPutCounter {
    public CostBlight(final String counters) {
        super(counters, CounterEnumType.M1M1, "Creature.YouCtrl", "a creature you control");
    }

    public String toString() {
        return "Blight " + getAmount();
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}