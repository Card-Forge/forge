package forge.card.cost;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
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
        return lastPaidAmount;
    }

    /**
     * Sets the last paid amount.
     * 
     * @param paidAmount
     *            the new last paid amount
     */
    public final void setLastPaidAmount(final int paidAmount) {
        lastPaidAmount = paidAmount;
    }

    /**
     * Instantiates a new cost pay life.
     * 
     * @param amount
     *            the amount
     */
    public CostPayLife(final String amount) {
        this.amount = amount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pay ").append(amount).append(" Life");
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
        source.getController().payLife(lastPaidAmount * -1, null);
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
        Integer amount = convertAmount();
        if (amount != null && !activator.canPayLife(amount)) {
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
    public final void payAI(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        AllZone.getComputerPlayer().payLife(getLastPaidAmount(), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        String amount = getAmount();
        Player activator = ability.getActivatingPlayer();
        int life = activator.getLife();

        Integer c = convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, life);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Pay ").append(c).append(" Life?");

        if (GameActionUtil.showYesNoDialog(source, sb.toString()) && activator.canPayLife(c)) {
            activator.payLife(c, null);
            setLastPaidAmount(c);
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
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        Player activator = ability.getActivatingPlayer();

        Integer c = convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                return false;
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (!activator.canPayLife(c)) {
            return false;
        }
        // activator.payLife(c, null);
        setLastPaidAmount(c);
        return true;
    }
}
