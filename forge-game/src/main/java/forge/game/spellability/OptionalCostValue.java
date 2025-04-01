package forge.game.spellability;

import java.io.Serializable;

import forge.game.cost.Cost;

public class OptionalCostValue implements Serializable {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    private OptionalCost type;
    private Cost cost;
    private boolean addType = true;

    public OptionalCostValue(OptionalCost type, Cost cost) {
        this.type = type;
        this.cost = cost;
    }
    public OptionalCostValue(OptionalCost type, Cost cost, boolean addType) {
        this.type = type;
        this.cost = cost;
        this.addType = addType;
    }

    /**
     * @return the type
     */
    public boolean addsType() {
        return addType;
    }

    /**
     * @return the type
     */
    public OptionalCost getType() {
        return type;
    }

    /**
     * @return the cost
     */
    public Cost getCost() {
        return cost;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean isTag = type.getName().startsWith("(");
        if (type != OptionalCost.Generic && !isTag) {
            sb.append(type.getName());
            sb.append(" – ");
        }
        sb.append(cost.toSimpleString());
        sb.append(isTag ? " " + type.getName() : "");
        return sb.toString();
    }
}
