package forge.game.cost;

import forge.game.card.CounterType;
import java.io.Serial;

/**
 * The Class CostBlight.
 */
public class CostBlight extends CostPutCounter {
    @Serial
    private static final long serialVersionUID = 1L;

    public CostBlight(final String amount, final String counters) {
        // Selection count, M1M1 counters, Target type, Target description
        super(amount, CounterType.getType("M1M1"), "Creature.YouCtrl", "a creature you control");
        this.setAmount(counters);
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}