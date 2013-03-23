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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import forge.Card;
import forge.CardLists;
import forge.FThreads;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputBase;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostReturn.
 */
public class CostReturn extends CostPartWithList {
    // Return<Num/Type{/TypeDescription}>

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputPayReturnType extends InputBase {
        private final SpellAbility sa;
        private final CostReturn part;
        private final int nNeeded;
        private final String type;
        private final CostPayment payment;
        private static final long serialVersionUID = 2685832214519141903L;
        private List<Card> typeList;
        private int nReturns = 0;
        private final CountDownLatch cdlDone;

        /**
         * TODO: Write javadoc for Constructor.
         * @param sa
         * @param part
         * @param nNeeded
         * @param cdl
         * @param type
         * @param payment
         */
        public InputPayReturnType(CountDownLatch cdl, SpellAbility sa, CostReturn part, int nNeeded, String type,
                CostPayment payment) {
            this.sa = sa;
            this.part = part;
            this.nNeeded = nNeeded;
            this.type = type;
            this.payment = payment;
            cdlDone = cdl;
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            final StringBuilder msg = new StringBuilder("Return ");
            final int nLeft = nNeeded - this.nReturns;
            msg.append(nLeft).append(" ");
            msg.append(type);
            if (nLeft > 1) {
                msg.append("s");
            }

            this.typeList = new ArrayList<Card>(sa.getActivatingPlayer().getCardsIn(ZoneType.Battlefield));
            this.typeList = CardLists.getValidCards(this.typeList, type.split(";"), sa.getActivatingPlayer(), sa.getSourceCard());
            CMatchUI.SINGLETON_INSTANCE.showMessage(msg.toString());
            ButtonUtil.enableOnlyCancel();
        }

        @Override
        public void selectButtonCancel() {
            this.cancel();
        }

        @Override
        public void selectCard(final Card card) {
            if (this.typeList.contains(card)) {
                this.nReturns++;
                part.addToList(card);
                Singletons.getModel().getGame().getAction().moveToHand(card);
                this.typeList.remove(card);
                // in case nothing else to return
                if (this.nReturns == nNeeded) {
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
            cdlDone.countDown();
        }

        public void cancel() {
            this.stop();
            payment.cancelCost();
            cdlDone.countDown();
        }
    }

    /**
     * Instantiates a new cost return.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostReturn(final String amount, final String type, final String description) {
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
        sb.append("Return ");

        final Integer i = this.convertAmount();
        String pronoun = "its";

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else {
            final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
            if (i != null) {
                sb.append(Cost.convertIntAndTypeToWords(i, desc));
                if (i > 1) {
                    pronoun = "their";
                }
            } else {
                sb.append(Cost.convertAmountTypeToWords(this.getAmount(), desc));
            }

            sb.append(" you control");
        }
        sb.append(" to ").append(pronoun).append(" owner's hand");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Card source, final Player activator, final Cost cost, final GameState game) {
        if (!this.payCostFromSource()) {
            boolean needsAnnoucement = ability.hasParam("Announce") && this.getType().contains(ability.getParam("Announce"));
            
            List<Card> typeList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Battlefield));
            typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);

            final Integer amount = this.convertAmount();
            if (!needsAnnoucement && amount != null && typeList.size() < amount) {
                return false;
            }
        } else if (!source.isInPlay()) {
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
    public final void payAI(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        for (final Card c : this.getList()) {
            Singletons.getModel().getGame().getAction().moveToHand(c);
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
    public final void payHuman(final SpellAbility ability, final Card source, final CostPayment payment, final GameState game) {
        final String amount = this.getAmount();
        Integer c = this.convertAmount();
        final Player activator = ability.getActivatingPlayer();
        final List<Card> list = activator.getCardsIn(ZoneType.Battlefield);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, ability, list.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        if (this.payCostFromSource()) {
            final Card card = ability.getSourceCard();
            if (card.getController() == ability.getActivatingPlayer() && card.isInPlay()) {
                boolean confirm = GuiDialog.confirm(card, card.getName() + " - Return to Hand?");
                if (confirm) {
                    addToList(card);
                    Singletons.getModel().getGame().getAction().moveToHand(card);
                } else {
                    payment.cancelCost();
                }
            }
        } else {
            CountDownLatch cdl = new CountDownLatch(1);
            final InputBase target = new InputPayReturnType(cdl, ability, this, c, this.getType(), payment);
            final InputBase inp = target;
            FThreads.setInputAndWait(inp, cdl);
        }

        if (!payment.isCanceled())
            addListToHash(ability, "Returned");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean decideAIPayment(final AIPlayer ai, final SpellAbility ability, final Card source, final CostPayment payment) {
        this.resetList();
        if (this.payCostFromSource()) {
            this.getList().add(source);
        } else {
            Integer c = this.convertAmount();
            if (c == null) {
                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }

            this.setList(ComputerUtil.chooseReturnType(ai, this.getType(), source, ability.getTargetCard(), c));
            if (this.getList() == null) {
                return false;
            }
        }
        return true;
    }

    // Inputs


}
