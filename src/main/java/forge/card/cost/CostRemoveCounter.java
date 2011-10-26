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

    private Counters counter;
    private int lastPaidAmount = 0;

    /**
     * Gets the counter.
     * 
     * @return the counter
     */
    public final Counters getCounter() {
        return counter;
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
        isReusable = true;

        this.counter = counter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        if (counter.getName().equals("Loyalty")) {
            sb.append("-").append(amount);
        } else {
            sb.append("Remove ");
            Integer i = convertAmount();
            sb.append(Cost.convertAmountTypeToWords(i, amount, counter.getName() + " counter"));

            if (amount.equals("All")) {
                sb.append("s");
            }

            sb.append(" from ").append(typeDescription == null ? type : typeDescription);
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
        source.addCounterFromNonEffect(counter, lastPaidAmount);
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
        Counters c = getCounter();

        Integer amount = convertAmount();
        if (amount != null && source.getCounters(c) - amount < 0) {
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
        Integer c = convertAmount();
        if (c == null) {
            c = AbilityFactory.calculateAmount(source, amount, ability);
        }
        source.subtractCounter(getCounter(), c);
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
        Counters type = getCounter();
        Integer c = convertAmount();
        int maxCounters = source.getCounters(type);

        if (amount.equals("All")) {
            c = maxCounters;
        } else {
            if (c == null) {
                String sVar = source.getSVar(amount);
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
        Integer c = convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            if (sVar.equals("XChoice")) {
                return false;
            }

            c = AbilityFactory.calculateAmount(source, amount, ability);
        }
        if (c > source.getCounters(getCounter())) {
            System.out.println("Not enough " + getCounter() + " on " + source.getName());
            return false;
        }
        return true;
    }
}
