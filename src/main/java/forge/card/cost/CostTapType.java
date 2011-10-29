package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

/**
 * The Class CostTapType.
 */
public class CostTapType extends CostPartWithList {

    /**
     * Instantiates a new cost tap type.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostTapType(final String amount, final String type, final String description) {
        super(amount, type, description);
        isReusable = true;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public final String getDescription() {
        return typeDescription == null ? type : typeDescription;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tap ");

        Integer i = convertAmount();
        String desc = getDescription();

        sb.append(Cost.convertAmountTypeToWords(i, amount, "untapped " + desc));

        sb.append(" you control");

        return sb.toString();
    }

    /**
     * Adds the to tapped list.
     * 
     * @param c
     *            the c
     */
    public final void addToTappedList(final Card c) {
        list.add(c);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public final void refund(final Card source) {
        for (Card c : list) {
            c.untap();
        }

        list.clear();
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
        CardList typeList = activator.getCardsIn(Zone.Battlefield);

        typeList = typeList.getValidCards(getType().split(";"), activator, source);

        if (cost.getTap()) {
            typeList.remove(source);
        }
        typeList = typeList.filter(CardListFilter.UNTAPPED);

        Integer amount = convertAmount();
        if (typeList.size() == 0 || (amount != null && typeList.size() < amount)) {
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
        for (Card c : list)
            c.tap();
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
        CardList typeList = ability.getActivatingPlayer().getCardsIn(Zone.Battlefield);
        typeList = typeList.getValidCards(getType().split(";"), ability.getActivatingPlayer(), ability.getSourceCard());
        typeList = typeList.filter(CardListFilter.UNTAPPED);
        String amount = getAmount();
        Integer c = convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, typeList.size());
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }

        CostUtil.setInput(CostTapType.input_tapXCost(this, typeList, ability, payment, c));
        return false;
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
        boolean tap = payment.getCost().getTap();
        Integer c = convertAmount();
        if (c == null) {
            // Determine Amount
        }

        list = ComputerUtil.chooseTapType(getType(), source, tap, c);

        if (list == null) {
            System.out.println("Couldn't find a valid card to tap for: " + source.getName());
            return false;
        }

        return true;
    }

    // Inputs

    /**
     * <p>
     * input_tapXCost.
     * </p>
     * 
     * @param tapType
     *            the tap type
     * @param cardList
     *            a {@link forge.CardList} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param nCards
     *            a int.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_tapXCost(final CostTapType tapType, final CardList cardList, final SpellAbility sa,
            final Cost_Payment payment, final int nCards) {
        Input target = new Input() {

            private static final long serialVersionUID = 6438988130447851042L;
            int nTapped = 0;

            @Override
            public void showMessage() {
                if (nCards == 0) {
                    done();
                }

                if (cardList.size() == 0) {
                    stop();
                }

                int left = nCards - nTapped;
                AllZone.getDisplay()
                        .showMessage("Select a " + tapType.getDescription() + " to tap (" + left + " left)");
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Battlefield) && cardList.contains(card) && card.isUntapped()) {
                    // send in CardList for Typing
                    card.tap();
                    tapType.addToList(card);
                    cardList.remove(card);

                    nTapped++;

                    if (nTapped == nCards) {
                        done();
                    } else if (cardList.size() == 0) {
                        // happen
                        cancel();
                    } else {
                        showMessage();
                    }
                }
            }

            public void cancel() {
                stop();
                payment.cancelCost();
            }

            public void done() {
                stop();
                payment.paidCost(tapType);
                tapType.addListToHash(sa, "Tapped");
            }
        };

        return target;
    }// input_tapXCost()
}
