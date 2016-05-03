package forge.game.cost;

import forge.game.IIdentifiable;

public class IndividualCostPaymentInstance implements IIdentifiable {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    private final int id;
    private final CostPart cost;

    public IndividualCostPaymentInstance(final CostPart cost) {
        id = nextId();
        this.cost = cost;
    }

    public int getId() { return id; }

    public CostPart getCost() { return cost; }

}
