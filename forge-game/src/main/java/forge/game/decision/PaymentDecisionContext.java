package forge.game.decision;

import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Immutable public metadata and private live state for one atomic PAYMENT request. */
public final class PaymentDecisionContext {
    private final int payerId;
    private final PaymentStage paymentStage;
    private final String remainingCostSummary;
    private final Long decisionSequenceId;
    private final Integer subdecisionIndex;
    private final Player payer;
    private final SpellAbility ability;
    private final ManaCostBeingPaid remainingCost;
    private final ManaConversionMatrix matrix;
    private final ActionContinuation continuation;

    PaymentDecisionContext(final Player payer, final SpellAbility ability, final ManaCostBeingPaid remainingCost,
            final ManaConversionMatrix matrix, final ActionContinuation continuation,
            final Integer subdecisionIndex) {
        this.payer = payer;
        this.ability = ability;
        this.remainingCost = remainingCost;
        this.matrix = matrix;
        this.continuation = continuation;
        this.payerId = payer.getId();
        this.paymentStage = PaymentStage.SOURCE;
        this.remainingCostSummary = remainingCost.toString(false, payer.getManaPool());
        this.decisionSequenceId = continuation == null ? null : continuation.getDecisionSequenceId();
        this.subdecisionIndex = subdecisionIndex;
    }

    public int getPayerId() {
        return payerId;
    }

    public PaymentStage getPaymentStage() {
        return paymentStage;
    }

    public String getRemainingCostSummary() {
        return remainingCostSummary;
    }

    public Long getDecisionSequenceId() {
        return decisionSequenceId;
    }

    public Integer getSubdecisionIndex() {
        return subdecisionIndex;
    }

    public boolean hasActionContinuation() {
        return continuation != null;
    }

    Player getPayer() {
        return payer;
    }

    SpellAbility getAbility() {
        return ability;
    }

    ManaCostBeingPaid getRemainingCost() {
        return remainingCost;
    }

    ManaConversionMatrix getMatrix() {
        return matrix;
    }

    ActionContinuation getContinuation() {
        return continuation;
    }
}
