package forge.card.cost;

import forge.Card;
import forge.Counters;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

/**
 * The Class CostRemoveCounter.
 */
public class CostRemoveCounter extends CostPart {
    // SubCounter<Num/Counter/{Type/TypeDescription}>

    // Here are the cards that have RemoveCounter<Type>
    // Ion Storm, Noviken Sages, Ghave, Guru of Spores, Power Conduit (any
    // Counter is tough),
    // Quillspike, Rift Elemental, Sage of Fables, Spike Rogue

    private final Counters counter;
    private int lastPaidAmount = 0;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final Counters getCounter() {
        return this.counter;
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
     * Instantiates a new cost remove counter.
     * 
     * @param amount
     *            the amount
     * @param counter
     *            the counter
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostRemoveCounter(final String amount, final Counters counter, final String type, final String description) {
        super(amount, type, description);
        this.setReusable(true);

        this.counter = counter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        if (this.counter.getName().equals("Loyalty")) {
            sb.append("-").append(this.getAmount());
        } else {
            sb.append("Remove ");
            final Integer i = this.convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), this.counter.getName() + " counter"));

            if (this.getAmount().equals("All")) {
                sb.append("s");
            }

            sb.append(" from ").append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription());
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        source.addCounterFromNonEffect(this.counter, this.lastPaidAmount);
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
        final Counters c = this.getCounter();

        final Integer amount = this.convertAmount();
        if ((amount != null) && ((source.getCounters(c) - amount) < 0)) {
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
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
        }
        source.subtractCounter(this.getCounter(), c);
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
        final Counters type = this.getCounter();
        Integer c = this.convertAmount();
        final int maxCounters = source.getCounters(type);

        if (amount.equals("All")) {
            c = maxCounters;
        } else {
            if (c == null) {
                final String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, maxCounters);
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }
        }

        if (maxCounters >= c) {
            source.setSVar("CostCountersRemoved", "Number$" + Integer.toString(c));
            source.subtractCounter(type, c);
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
        Integer c = this.convertAmount();
        if (c == null) {
            final String sVar = source.getSVar(this.getAmount());
            if (sVar.equals("XChoice")) {
                return false;
            }

            c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
        }
        if (c > source.getCounters(this.getCounter())) {
            System.out.println("Not enough " + this.getCounter() + " on " + source.getName());
            return false;
        }
        return true;
    }
}
