/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cost;

import java.util.Iterator;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostExile.
 */
public class CostExile extends CostPartWithList {
    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGraveyard<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)

    private ZoneType from = ZoneType.Battlefield;

    /**
     * Gets the from.
     * 
     * @return the from
     */
    public final ZoneType getFrom() {
        return this.from;
    }

    /**
     * Instantiates a new cost exile.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     * @param from
     *            the from
     */
    public CostExile(final String amount, final String type, final String description, final ZoneType from) {
        super(amount, type, description);
        if (from != null) {
            this.from = from;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Exile ");

        if (this.getThis()) {
            sb.append(this.getType());
            if (!this.from.equals(ZoneType.Battlefield)) {
                sb.append(" from your ").append(this.from);
            }
            return sb.toString();
        }

        if (this.from.equals(ZoneType.Battlefield)) {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));
            if (!this.getThis()) {
                sb.append(" you control");
            }
            return sb.toString();
        }

        if (i != null) {
            sb.append(i);
        } else {
            sb.append(this.getAmount());
        }
        if (!this.getType().equals("Card")) {
            sb.append(" " + this.getType());
        }
        sb.append(" card");
        if ((i == null) || (i > 1)) {
            sb.append("s");
        }
        sb.append(" from your ").append(this.from);

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
        CardList typeList = activator.getCardsIn(this.getFrom());
        if (!this.getThis()) {
            typeList = typeList.getValidCards(this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();
            if ((amount != null) && (typeList.size() < amount)) {
                return false;
            }
        } else if (!typeList.contains(source)) {
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
        for (final Card c : this.getList()) {
            Singletons.getModel().getGameAction().exile(c);
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
    public final boolean payHuman(final SpellAbility ability, final Card source, final CostPayment payment) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();
        CardList list = activator.getCardsIn(this.getFrom());
        list = list.getValidCards(this.getType().split(";"), activator, source);
        if (c == null) {
            final String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, list.size());
            }
            if (sVar.equals("YChoice")) {
                c = CostUtil.chooseYValue(source, list.size());
            }
            else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        if (this.getThis()) {
            CostUtil.setInput(CostExile.exileThis(ability, payment, this));
        } else if (this.from.equals(ZoneType.Battlefield) || this.from.equals(ZoneType.Hand)) {
            CostUtil.setInput(CostExile.exileType(ability, this, this.getType(), payment, c));
        } else if (this.from.equals(ZoneType.Library)) {
            CostExile.exileFromTop(ability, this, payment, c);
        } else {
            CostUtil.setInput(CostExile.exileFrom(ability, this, this.getType(), payment, c));
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
    public final boolean decideAIPayment(final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.getThis()) {
            this.getList().add(source);
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                final String sVar = source.getSVar(this.getAmount());
                // Generalize this
                if (sVar.equals("XChoice")) {
                    return false;
                }

                if (sVar.equals("YChoice")) {
                        return false;
                }

                c = AbilityFactory.calculateAmount(source, this.getAmount(), ability);
            }

            if (this.from.equals(ZoneType.Library)) {
                this.setList(AllZone.getComputerPlayer().getCardsIn(ZoneType.Library, c));
            } else {
                this.setList(ComputerUtil.chooseExileFrom(this.getFrom(), this.getType(), source,
                        ability.getTargetCard(), c));
            }
            if ((this.getList() == null) || (this.getList().size() < c)) {
                return false;
            }
        }
        return true;
    }

    // Inputs

    /**
     * Exile from top.
     * 
     * @param sa
     *            the sa
     * @param part
     *            the part
     * @param payment
     *            the payment
     * @param nNeeded
     *            the n needed
     */
    public static void exileFromTop(final SpellAbility sa, final CostExile part, final CostPayment payment,
            final int nNeeded) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        final CardList list = sa.getActivatingPlayer().getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded) {
            // I don't believe this is possible
            payment.cancelCost();
            return;
        }

        final boolean doExile = GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString());
        if (doExile) {
            final Iterator<Card> itr = list.iterator();
            while (itr.hasNext()) {
                final Card c = itr.next();
                part.addToList(c);
                Singletons.getModel().getGameAction().exile(c);
            }
            part.addListToHash(sa, "Exiled");
            payment.paidCost(part);
        } else {
            payment.cancelCost();
        }
    }

    /**
     * Exile from.
     * 
     * @param sa
     *            the sa
     * @param part
     *            the part
     * @param type
     *            the type
     * @param payment
     *            the payment
     * @param nNeeded
     *            the n needed
     * @return the input
     */
    public static Input exileFrom(final SpellAbility sa, final CostExile part, final String type,
            final CostPayment payment, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 734256837615635021L;
            private CardList typeList;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                this.typeList = sa.getActivatingPlayer().getCardsIn(part.getFrom());
                this.typeList = this.typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(),
                        sa.getSourceCard());

                for (int i = 0; i < nNeeded; i++) {
                    if (this.typeList.size() == 0) {
                        this.cancel();
                    }

                    final Object o = GuiUtils
                            .chooseOneOrNone("Exile from " + part.getFrom(), this.typeList.toArray());

                    if (o != null) {
                        final Card c = (Card) o;
                        this.typeList.remove(c);
                        part.addToList(c);
                        Singletons.getModel().getGameAction().exile(c);
                        if (i == (nNeeded - 1)) {
                            this.done();
                        }
                    } else {
                        this.cancel();
                        break;
                    }
                }
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            public void done() {
                this.stop();
                part.addListToHash(sa, "Exiled");
                payment.paidCost(part);
            }

            public void cancel() {
                this.stop();
                payment.cancelCost();
            }
        };
        return target;
    } // exileFrom()

    /**
     * <p>
     * exileType.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param part
     *            the part
     * @param type
     *            a {@link java.lang.String} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param nNeeded
     *            the n needed
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input exileType(final SpellAbility sa, final CostExile part, final String type,
            final CostPayment payment, final int nNeeded) {
        final Input target = new Input() {
            private static final long serialVersionUID = 1403915758082824694L;

            private CardList typeList;
            private int nExiles = 0;

            @Override
            public void showMessage() {
                if (nNeeded == 0) {
                    this.done();
                }

                final StringBuilder msg = new StringBuilder("Exile ");
                final int nLeft = nNeeded - this.nExiles;
                msg.append(nLeft).append(" ");
                msg.append(type);
                if (nLeft > 1) {
                    msg.append("s");
                }

                if (part.getFrom().equals(ZoneType.Hand)) {
                    msg.append(" from your Hand");
                }
                this.typeList = sa.getActivatingPlayer().getCardsIn(part.getFrom());
                this.typeList = this.typeList.getValidCards(type.split(";"), sa.getActivatingPlayer(),
                        sa.getSourceCard());
                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }

            @Override
            public void selectButtonCancel() {
                this.cancel();
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (this.typeList.contains(card)) {
                    this.nExiles++;
                    part.addToList(card);
                    Singletons.getModel().getGameAction().exile(card);
                    this.typeList.remove(card);
                    // in case nothing else to exile
                    if (this.nExiles == nNeeded) {
                        this.done();
                    } else if (this.typeList.size() == 0) {
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }

            public void done() {
                this.stop();
                part.addListToHash(sa, "Exiled");
                payment.paidCost(part);
            }

            public void cancel() {
                this.stop();
                payment.cancelCost();
            }
        };

        return target;
    } // exileType()

    /**
     * <p>
     * exileThis.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param part
     *            the part
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input exileThis(final SpellAbility sa, final CostPayment payment, final CostExile part) {
        final Input target = new Input() {
            private static final long serialVersionUID = 678668673002725001L;

            @Override
            public void showMessage() {
                final Card card = sa.getSourceCard();
                if (sa.getActivatingPlayer().isHuman()
                        && sa.getActivatingPlayer().getZone(part.getFrom()).contains(card)) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - Exile?");
                    final Object[] possibleValues = { "Yes", "No" };
                    final Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (choice.equals(0)) {
                        payment.getAbility().addCostToHashList(card, "Exiled");
                        Singletons.getModel().getGameAction().exile(card);
                        part.addToList(card);
                        this.stop();
                        part.addListToHash(sa, "Exiled");
                        payment.paidCost(part);
                    } else {
                        this.stop();
                        payment.cancelCost();
                    }
                }
            }
        };

        return target;
    } // input_exile()
}
