package forge.card.cost;

import forge.AllZone;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * The Class CostReveal.
 */
public class CostReveal extends CostPartWithList {
    // Reveal<Num/Type/TypeDescription>

    /**
     * Instantiates a new cost reveal.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostReveal(final String amount, final String type, final String description) {
        super(amount, type, description);
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
        CardList handList = activator.getCardsIn(Zone.Hand);
        String type = getType();
        Integer amount = convertAmount();

        if (getThis()) {
            if (!AllZone.getZoneOf(source).is(Constant.Zone.Hand)) {
                return false;
            }
        } else {
            handList = handList.getValidCards(type.split(";"), activator, source);
            if (amount != null && amount > handList.size()) {
                // not enough cards in hand to pay
                return false;
            }
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
        String type = getType();
        Player activator = ability.getActivatingPlayer();
        CardList hand = activator.getCardsIn(Zone.Hand);
        resetList();

        if (getThis()) {
            if (!hand.contains(source)) {
                return false;
            }

            list.add(source);
        } else {
            hand = hand.getValidCards(type.split(";"), activator, source);
            Integer c = convertAmount();
            if (c == null) {
                String sVar = source.getSVar(amount);
                if (sVar.equals("XChoice")) {
                    c = hand.size();
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }

            list = ComputerUtil.AI_discardNumType(c, type.split(";"), ability);
        }
        return list != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final void payAI(final SpellAbility ability, final Card source, final Cost_Payment payment) {
        GuiUtils.getChoiceOptional("Revealed cards:", list.toArray());
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
        Player activator = ability.getActivatingPlayer();
        String amount = getAmount();
        resetList();

        if (getThis()) {
            addToList(source);
            payment.setPaidManaPart(this, true);
        } else {
            Integer c = convertAmount();

            CardList handList = activator.getCardsIn(Zone.Hand);
            handList = handList.getValidCards(type.split(";"), activator, ability.getSourceCard());

            if (c == null) {
                String sVar = source.getSVar(amount);
                if (sVar.equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, handList.size());
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }

            CostUtil.setInput(input_revealCost(type, handList, payment, this, ability, c));
            return false;
        }
        addListToHash(ability, "Revealed");
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Reveal ");

        Integer i = convertAmount();

        if (getThis()) {
            sb.append(type);
        } else {
            StringBuilder desc = new StringBuilder();

            if (type.equals("Card")) {
                desc.append("Card");
            } else {
                desc.append(typeDescription == null ? type : typeDescription).append(" card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, amount, desc.toString()));
        }
        sb.append(" from your hand");

        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public void refund(final Card source) {

    }

    // Inputs

    /**
     * <p>
     * input_discardCost.
     * </p>
     * 
     * @param discType
     *            a {@link java.lang.String} object.
     * @param handList
     *            a {@link forge.CardList} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param part
     *            TODO
     * @param sa
     *            TODO
     * @param nNeeded
     *            a int.
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input input_revealCost(final String discType, final CardList handList, final Cost_Payment payment,
            final CostReveal part, final SpellAbility sa, final int nNeeded) {
        Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;

            int nReveal = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    done();
                }

                if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() < nNeeded) {
                    stop();
                }
                StringBuilder type = new StringBuilder("");
                if (!discType.equals("Card")) {
                    type.append(" ").append(discType);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Select a ");
                sb.append(part.getDescriptiveType());
                sb.append(" to reveal.");
                if (nNeeded > 1) {
                    sb.append(" You have ");
                    sb.append(nNeeded - nReveal);
                    sb.append(" remaining.");
                }
                AllZone.getDisplay().showMessage(sb.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (zone.is(Constant.Zone.Hand) && handList.contains(card)) {
                    // send in CardList for Typing
                    handList.remove(card);
                    part.addToList(card);
                    nReveal++;

                    // in case no more cards in hand
                    if (nReveal == nNeeded) {
                        done();
                    } else if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                        // really
                                                                                      // shouldn't
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
                // "Inform" AI of the revealed cards
                part.addListToHash(sa, "Revealed");
                payment.paidCost(part);
            }
        };

        return target;
    }// input_discard()

}
