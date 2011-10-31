package forge.card.cost;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.input.Input;

/**
 * The Class CostSacrifice.
 */
public class CostSacrifice extends CostPartWithList {

    /**
     * Instantiates a new cost sacrifice.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostSacrifice(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sacrifice ");

        final Integer i = this.convertAmount();

        if (this.getThis()) {
            sb.append(this.getType());
        } else {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
            if (i != null) {
                sb.append(Cost.convertIntAndTypeToWords(i, desc));
            } else {
                sb.append(Cost.convertAmountTypeToWords(this.getAmount(), desc));
            }
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#refund(forge.Card)
     */
    @Override
    public void refund(final Card source) {
        // TODO Auto-generated method stub

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
        // You can always sac all
        if (!this.getThis()) {
            CardList typeList = activator.getCardsIn(Zone.Battlefield);
            typeList = typeList.getValidCards(this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();

            if ((amount != null) && (typeList.size() < amount)) {
                return false;
            }

            // If amount is null, it's either "ALL" or "X"
            // if X is defined, it needs to be calculated and checked, if X is
            // choice, it can be Paid even if it's 0
        } else if (!AllZoneUtil.isCardInPlay(source)) {
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
        for (final Card c : this.list) {
            AllZone.getGameAction().sacrifice(c);
        }
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
        final String amount = this.getAmount();
        final String type = this.getType();
        final Player activator = ability.getActivatingPlayer();
        CardList list = activator.getCardsIn(Zone.Battlefield);
        list = list.getValidCards(type.split(";"), activator, source);

        if (this.getThis()) {
            CostUtil.setInput(CostSacrifice.sacrificeThis(ability, payment, this));
        } else if (amount.equals("All")) {
            this.list = list;
            CostSacrifice.sacrificeAll(ability, payment, this, list);
            this.addListToHash(ability, "Sacrificed");
            return true;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                final String sVar = source.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = CostUtil.chooseXValue(source, list.size());
                } else {
                    c = AbilityFactory.calculateAmount(source, amount, ability);
                }
            }

            CostUtil.setInput(CostSacrifice.sacrificeFromList(ability, payment, this, list, c));
        }

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
        this.resetList();
        final Player activator = ability.getActivatingPlayer();
        if (this.getThis()) {
            this.list.add(source);
        } else if (this.getAmount().equals("All")) {
            CardList typeList = activator.getCardsIn(Zone.Battlefield);
            typeList = typeList.getValidCards(this.getType().split(","), activator, source);
            // Does the AI want to use Sacrifice All?
            return false;
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                if (source.getSVar(this.getAmount()).equals("XChoice")) {
                    return false;
                }

                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }
            this.list = ComputerUtil.chooseSacrificeType(this.getType(), source, ability.getTargetCard(), c);
            if (this.list == null) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * <p>
     * sacrificeAllType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param part
     *            TODO
     * @param typeList
     *            TODO
     */
    public static void sacrificeAll(final SpellAbility sa, final Cost_Payment payment, final CostPart part,
            final CardList typeList) {
        // TODO Ask First
        for (final Card card : typeList) {
            payment.getAbility().addCostToHashList(card, "Sacrificed");
            AllZone.getGameAction().sacrifice(card);
        }

        payment.setPaidManaPart(part, true);
    }

    /**
     * <p>
     * sacrificeFromList.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param part
     *            TODO
     * @param typeList
     *            TODO
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeFromList(final SpellAbility sa, final Cost_Payment payment, final CostSacrifice part,
            final CardList typeList, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private int nSacrifices = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Sacrifice ");
                final int nLeft = nNeeded - this.nSacrifices;
                msg.append(nLeft).append(" ");
                msg.append(part.getDescriptiveType());
                if (nLeft > 1) {
                    msg.append("s");
                }

                AllZone.getDisplay().showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (typeList.contains(card)) {
                    this.nSacrifices++;
                    part.addToList(card);
                    AllZone.getGameAction().sacrifice(card);
                    typeList.remove(card);
                    // in case nothing else to sacrifice
                    if (this.nSacrifices == nNeeded) {
                        this.done();
                    } else if (typeList.size() == 0) {
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void done() {
                this.stop();
                part.addListToHash(sa, "Sacrificed");
                payment.paidCost(part);
            }

            public void cancel() {
                this.stop();

                payment.cancelCost();
            }
        };

        return target;
    } // sacrificeType()

    /**
     * <p>
     * sacrificeThis.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.Cost_Payment} object.
     * @param part
     *            TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public static Input sacrificeThis(final SpellAbility sa, final Cost_Payment payment, final CostSacrifice part) {
        final Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;

            @Override
            public void showMessage() {
                final Card card = sa.getSourceCard();
                if (card.getController().isHuman() && AllZoneUtil.isCardInPlay(card)) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Sacrifice?");
                    final Object[] possibleValues = { "Yes", "No" };
                    final Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (choice.equals(0)) {
                        part.addToList(card);
                        part.addListToHash(sa, "Sacrificed");
                        AllZone.getGameAction().sacrifice(card);
                        this.stop();
                        payment.paidCost(part);
                    } else {
                        this.stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    } // input_sacrifice()
}
