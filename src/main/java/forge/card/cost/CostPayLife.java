package forge.card.cost;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

/**
 * The Class CostPayLife.
 */
public class CostPayLife extends CostPart {
    private int lastPaidAmount = 0;

    /**
     * Gets the last paid amount.
     * 
     * @return the last paid amount
     */
    public final int getLastPaidAmount() {
        return this.lastPaidAmount;
    }

    /**
     * Sets the last paid amount.
     * 
     * @param paidAmount
     *            the new last paid amount
     */
    public final void setLastPaidAmount(final int paidAmount) {
        this.lastPaidAmount = paidAmount;
    }

    /**
     * Instantiates a new cost pay life.
     * 
     * @param amount
     *            the amount
     */
    public CostPayLife(final String amount) {
        this.setAmount(amount);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pay ").append(this.getAmount()).append(" Life");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        // Really should be activating player
        source.getController().payLife(this.lastPaidAmount * -1, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost) {
        final Integer amount = this.convertAmount();
        if ((amount != null) && !activator.canPayLife(amount)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final SpellAbility ability, final Card source, final CostPayment payment) {
        AllZone.getComputerPlayer().payLife(this.getLastPaidAmount(), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        final Player activator = ability.getActivatingPlayer();
        final int life = activator.getLife();

        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, life);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Pay ").append(c).append(" Life?");

        if (GameActionUtil.showYesNoDialog(source, sb.toString()) && activator.canPayLife(c)) {
            activator.payLife(c, null);
            this.setLastPaidAmount(c);
            payment.setPaidManaPart(this, true);
        } else {
            payment.setCancel(true);
            payment.getRequirements().finishPaying();
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final CostPayment payment) {
        final Player activator = ability.getActivatingPlayer();

        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = source.getSVar(this.getAmount());
            // Generalize this
            if (sVar.equals("XChoice")) {
                return false;
            } else {
                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }
        }
        if (!activator.canPayLife(c)) {
            return false;
        }
        // activator.payLife(c, null);
        this.setLastPaidAmount(c);
        return true;
    }
}
