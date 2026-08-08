package forge.game.cost;

import forge.game.mana.ManaCostBeingPaid;

/**
 * Side-effect-free result of the controller-free subset of cost adjustment.
 */
public final class CostAdjustmentPreview {
    public enum Status {
        ADJUSTED,
        CHOICE_REQUIRED,
        UNSUPPORTED
    }

    public enum Reason {
        FACE_DOWN_STATE,
        REDUCTION_ORDER,
        VARIABLE_REDUCTION,
        DYNAMIC_COST,
        OFFERING,
        EMERGE,
        ASSIST,
        DELVE,
        CONVOKE,
        IMPROVISE,
        WATERBEND,
        TAP_CREATURES_FOR_MANA
    }

    private final Status status;
    private final Reason reason;
    private final Cost adjustedCost;
    private final ManaCostBeingPaid adjustedManaCost;

    private CostAdjustmentPreview(final Status status, final Reason reason, final Cost adjustedCost,
            final ManaCostBeingPaid adjustedManaCost) {
        this.status = status;
        this.reason = reason;
        this.adjustedCost = adjustedCost;
        this.adjustedManaCost = adjustedManaCost;
    }

    static CostAdjustmentPreview adjusted(final Cost cost, final ManaCostBeingPaid manaCost) {
        return new CostAdjustmentPreview(Status.ADJUSTED, null, cost.copy(), new ManaCostBeingPaid(manaCost));
    }

    static CostAdjustmentPreview choiceRequired(final Reason reason) {
        return new CostAdjustmentPreview(Status.CHOICE_REQUIRED, reason, null, null);
    }

    static CostAdjustmentPreview unsupported(final Reason reason) {
        return new CostAdjustmentPreview(Status.UNSUPPORTED, reason, null, null);
    }

    public Status getStatus() {
        return status;
    }

    public Reason getReason() {
        return reason;
    }

    public Cost getAdjustedCost() {
        return adjustedCost == null ? null : adjustedCost.copy();
    }

    public ManaCostBeingPaid getAdjustedManaCost() {
        return adjustedManaCost == null ? null : new ManaCostBeingPaid(adjustedManaCost);
    }

    public boolean hasAdjustedManaCost() {
        return adjustedManaCost != null;
    }
}
