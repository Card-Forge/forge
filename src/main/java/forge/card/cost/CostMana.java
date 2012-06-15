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

import com.google.common.base.Strings;

import forge.AllZone;
import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputMana;
import forge.control.input.InputPayManaCostUtil;
import forge.game.phase.PhaseHandler;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostMana.
 */
public class CostMana extends CostPart {
    // "Leftover"
    private String mana = "";
    private int amountX = 0;
    private String adjustedMana = "";

    /**
     * Gets the mana.
     * 
     * @return the mana
     */
    public final String getMana() {
        // Only used for Human to pay for non-X cost first
        return this.mana;
    }

    /**
     * Sets the mana.
     * 
     * @param sCost
     *            the new mana
     */
    public final void setMana(final String sCost) {
        this.mana = sCost;
    }

    /**
     * Checks for no x mana cost.
     * 
     * @return true, if successful
     */
    public final boolean hasNoXManaCost() {
        return this.amountX == 0;
    }

    /**
     * Gets the x mana.
     * 
     * @return the x mana
     */
    public final int getXMana() {
        return this.amountX;
    }

    /**
     * Sets the x mana.
     * 
     * @param xCost
     *            the new x mana
     */
    public final void setXMana(final int xCost) {
        this.amountX = xCost;
    }

    /**
     * Gets the adjusted mana.
     * 
     * @return the adjusted mana
     */
    public final String getAdjustedMana() {
        return this.adjustedMana;
    }

    /**
     * Sets the adjusted mana.
     * 
     * @param adjustedMana
     *            the new adjusted mana
     */
    public final void setAdjustedMana(final String adjustedMana) {
        this.adjustedMana = adjustedMana;
    }

    /**
     * Gets the mana to pay.
     * 
     * @return the mana to pay
     */
    public final String getManaToPay() {
        // Only used for Human to pay for non-X cost first
        if (!this.adjustedMana.equals("")) {
            return this.adjustedMana;
        }

        return this.mana;
    }

    /**
     * Instantiates a new cost mana.
     * 
     * @param mana
     *            the mana
     * @param amount
     *            the amount
     */
    public CostMana(final String mana, final int amount) {
        this.mana = mana.trim();
        this.amountX = amount;
        this.setUndoable(true);
        this.setReusable(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(Strings.repeat("X ", this.amountX));
        if (!this.mana.equals("0")) {
            sb.append(this.mana);
        }

        return sb.toString().trim();
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
        // For now, this will always return true. But this should probably be
        // checked at some point
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
        ComputerUtil.payManaCost(ability);
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
        int manaToAdd = 0;
        if (!this.hasNoXManaCost()) {
            // if X cost is a defined value, other than xPaid
            if (!ability.getSVar("X").equals("Count$xPaid")) {
                // this currently only works for things about Targeted object
                manaToAdd = AbilityFactory.calculateAmount(source, "X", ability) * this.getXMana();
            }
        }
        if (!this.getManaToPay().equals("0") || (manaToAdd > 0)) {
            CostUtil.setInput(CostMana.inputPayMana(ability, payment, this, manaToAdd));
        } else if (this.getXMana() > 0) {
            CostUtil.setInput(CostMana.inputPayXMana(ability, payment, this, this.getXMana()));
        } else {
            payment.paidCost(this);
        }

        // We return false here because the Inputs set above should recall
        // payment.payCosts()
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
        return true;
    }

    // Inputs

    /**
     * <p>
     * input_payXMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param costMana
     *            TODO
     * @param numX
     *            a int.
     * 
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputPayXMana(final SpellAbility sa, final CostPayment payment, final CostMana costMana,
            final int numX) {
        final Input payX = new InputMana() {
            private static final long serialVersionUID = -6900234444347364050L;
            private int xPaid = 0;
            private String colorsPaid = "";
            private ManaCost manaCost = new ManaCost(Integer.toString(numX));

            @Override
            public void showMessage() {
                if (this.manaCost.toString().equals(Integer.toString(numX))) {
                    // only
                    // cancel
                    // if
                    // partially
                    // paid
                    // an X
                    // value
                    ButtonUtil.enableAll();
                } else {
                    ButtonUtil.enableOnlyCancel();
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage(
                        "Pay X Mana Cost for " + sa.getSourceCard().getName() + "\n" + this.xPaid + " Paid so far.");
            }

            // selectCard
            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    // this really shouldn't happen but just in case
                    return;
                }

                this.manaCost = InputPayManaCostUtil.activateManaAbility(sa, card, this.manaCost);
                if (this.manaCost.isPaid()) {
                    if (!this.colorsPaid.contains(this.manaCost.getColorsPaid())) {
                        this.colorsPaid += this.manaCost.getColorsPaid();
                    }
                    this.manaCost = new ManaCost(Integer.toString(numX));
                    this.xPaid++;
                }

                if (AllZone.getInputControl().getInput() == this) {
                    this.showMessage();
                }
            }

            @Override
            public void selectButtonCancel() {
                this.stop();
                payment.cancelCost();
                AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
            }

            @Override
            public void selectButtonOK() {
                this.stop();
                payment.getCard().setXManaCostPaid(this.xPaid);
                payment.paidCost(costMana);
                payment.getCard().setColorsPaid(this.colorsPaid);
                payment.getCard().setSunburstValue(this.colorsPaid.length());
            }

            @Override
            public void selectManaPool(String color) {
                this.manaCost = InputPayManaCostUtil.activateManaAbility(color, sa, this.manaCost);
                if (this.manaCost.isPaid()) {
                    if (!this.colorsPaid.contains(this.manaCost.getColorsPaid())) {
                        this.colorsPaid += this.manaCost.getColorsPaid();
                    }
                    this.manaCost = new ManaCost(Integer.toString(numX));
                    this.xPaid++;
                }

                if (AllZone.getInputControl().getInput() == this) {
                    this.showMessage();
                }
            }

        };

        return payX;
    }

    /**
     * <p>
     * input_payMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param payment
     *            a {@link forge.card.cost.CostPayment} object.
     * @param costMana
     *            the cost mana
     * @param manaToAdd
     *            a int.
     * @return a {@link forge.control.input.Input} object.
     */
    public static Input inputPayMana(final SpellAbility sa, final CostPayment payment, final CostMana costMana,
            final int manaToAdd) {
        final ManaCost manaCost;

        if (PhaseHandler.getGameBegins() == 1) {
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {
                final String mana = costMana.getManaToPay();
                manaCost = new ManaCost(mana);
                manaCost.increaseColorlessMana(manaToAdd);
            }
        } else {
            System.out.println("Is input_payMana ever called when the Game isn't in progress?");
            manaCost = new ManaCost(sa.getManaCost());
        }

        final Input payMana = new InputMana() {
            private ManaCost mana = manaCost;
            private static final long serialVersionUID = 3467312982164195091L;

            private final String originalManaCost = costMana.getMana();

            private int phyLifeToLose = 0;

            private void resetManaCost() {
                this.mana = new ManaCost(this.originalManaCost);
                this.phyLifeToLose = 0;
            }

            @Override
            public void selectCard(final Card card, final PlayerZone zone) {
                // prevent cards from tapping themselves if ability is a
                // tapability, although it should already be tapped
                if (sa.getSourceCard().equals(card) && sa.isTapAbility()) {
                    return;
                }

                this.mana = InputPayManaCostUtil.activateManaAbility(sa, card, this.mana);

                if (this.mana.isPaid()) {
                    this.done();
                } else if (AllZone.getInputControl().getInput() == this) {
                    this.showMessage();
                }
            }

            @Override
            public void selectPlayer(final Player player) {
                if (player.isHuman()) {
                    if (manaCost.payPhyrexian()) {
                        this.phyLifeToLose += 2;
                    }

                    this.showMessage();
                }
            }

            private void done() {
                final Card source = sa.getSourceCard();
                if (this.phyLifeToLose > 0) {
                    AllZone.getHumanPlayer().payLife(this.phyLifeToLose, source);
                }
                source.setColorsPaid(this.mana.getColorsPaid());
                source.setSunburstValue(this.mana.getSunburst());
                this.resetManaCost();
                this.stop();

                if (costMana.hasNoXManaCost() || (manaToAdd > 0)) {
                    payment.paidCost(costMana);
                } else {
                    source.setXManaCostPaid(0);
                    CostUtil.setInput(CostMana.inputPayXMana(sa, payment, costMana, costMana.getXMana()));
                }

                // If this is a spell with convoke, re-tap all creatures used
                // for it.
                // This is done to make sure Taps triggers go off at the right
                // time
                // (i.e. AFTER cost payment, they are tapped previously as well
                // so that
                // any mana tapabilities can't be used in payment as well as
                // being tapped for convoke)

                if (sa.getTappedForConvoke() != null) {
                    AllZone.getTriggerHandler().suppressMode(TriggerType.Untaps);
                    for (final Card c : sa.getTappedForConvoke()) {
                        c.untap();
                        c.tap();
                    }
                    AllZone.getTriggerHandler().clearSuppression(TriggerType.Untaps);
                    sa.clearTappedForConvoke();
                }

            }

            @Override
            public void selectButtonCancel() {
                // If we're paying for a spell with convoke, untap all creatures
                // used for it.
                if (sa.getTappedForConvoke() != null) {
                    AllZone.getTriggerHandler().suppressMode(TriggerType.Untaps);
                    for (final Card c : sa.getTappedForConvoke()) {
                        c.untap();
                    }
                    AllZone.getTriggerHandler().clearSuppression(TriggerType.Untaps);
                    sa.clearTappedForConvoke();
                }

                this.stop();
                this.resetManaCost();
                payment.cancelCost();
                AllZone.getHumanPlayer().getZone(ZoneType.Battlefield).updateObservers();
            }

            @Override
            public void showMessage() {
                ButtonUtil.enableOnlyCancel();
                final String displayMana = this.mana.toString().replace("X", "").trim();
                CMatchUI.SINGLETON_INSTANCE.showMessage("Pay Mana Cost: " + displayMana);

                final StringBuilder msg = new StringBuilder("Pay Mana Cost: " + displayMana);
                if (this.phyLifeToLose > 0) {
                    msg.append(" (");
                    msg.append(this.phyLifeToLose);
                    msg.append(" life paid for phyrexian mana)");
                }

                if (this.mana.containsPhyrexianMana()) {
                    msg.append("\n(Click on your life total to pay life for phyrexian mana.)");
                }

                CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
                if (this.mana.isPaid()) {
                    this.done();
                }
            }

            @Override
            public void selectManaPool(String color) {
                this.mana = InputPayManaCostUtil.activateManaAbility(color, sa, this.mana);

                if (this.mana.isPaid()) {
                    this.done();
                } else if (AllZone.getInputControl().getInput() == this) {
                    this.showMessage();
                }
            }
        };
        return payMana;
    }
}
