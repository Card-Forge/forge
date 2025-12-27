package forge.game.cost;

import forge.game.card.CounterEnumType;

public class CostBlight extends CostPutCounter {
    public CostBlight(final String counters) {
        super("1", CounterEnumType.M1M1, "Creature.YouCtrl", "a creature you control");
        this.setAmount(counters);
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}