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

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.FThreads;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
import forge.view.ButtonUtil;

/**
 * The Class CostDiscard.
 */
public class CostDiscard extends CostPartWithList {
    // Discard<Num/Type{/TypeDescription}>

    // Inputs
    
    /** 
     * TODO: Write javadoc for this type.
     *
     */
    public static final class InputPayCostDiscard extends InputPayCostBase {
        private final List<Card> handList;
        private final CostDiscard part;
        private final int nNeeded;
        private final String discType;
        private static final long serialVersionUID = -329993322080934435L;
        private int nDiscard = 0;
        private boolean sameName;

        private final SpellAbility sa;

        /**
         * TODO: Write javadoc for Constructor.
         * @param sa
         * @param handList
         * @param part
         * @param payment
         * @param nNeeded
         * @param sp
         * @param discType
         */
        public InputPayCostDiscard(CountDownLatch cdl, SpellAbility sa, List<Card> handList, CostDiscard part, CostPayment payment, int nNeeded, String discType) {
            super(cdl, payment);
            this.sa = sa;
            this.handList = handList;
            this.part = part;
            this.nNeeded = nNeeded;
            this.discType = discType;
            sameName = discType.contains("WithSameName");
        }

        @Override
        public void showMessage() {
            if (nNeeded == 0) {
                this.done();
            }

            if (sa.getActivatingPlayer().getZone(ZoneType.Hand).isEmpty()) {
                this.stop();
            }
            final StringBuilder type = new StringBuilder("");
            if (!discType.equals("Card")) {
                type.append(" ").append(discType);
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("Select a ");
            sb.append(part.getDescriptiveType());
            sb.append(" to discard.");
            if (nNeeded > 1) {
                sb.append(" You have ");
                sb.append(nNeeded - this.nDiscard);
                sb.append(" remaining.");
            }
            CMatchUI.SINGLETON_INSTANCE.showMessage(sb.toString());
            if (nNeeded > 0) {
                ButtonUtil.enableOnlyCancel();
            }
        }

        @Override
        public void selectCard(final Card card) {
            Zone zone = Singletons.getModel().getGame().getZoneOf(card);
            if (zone.is(ZoneType.Hand) && handList.contains(card)) {
                if (!sameName || part.getList().isEmpty()
                        || part.getList().get(0).getName().equals(card.getName())) {
                    // send in List<Card> for Typing
                    card.getController().discard(card, sa);
                    part.addToList(card);
                    handList.remove(card);
                    this.nDiscard++;

                    // in case no more cards in hand
                    if (this.nDiscard == nNeeded) {
                        this.done();
                    } else if (sa.getActivatingPlayer().getZone(ZoneType.Hand).size() == 0) {
                        // really
                        // shouldn't
                        // happen
                        this.cancel();
                    } else {
                        this.showMessage();
                    }
                }
            }
        }
    }

    /**
     * Instantiates a new cost discard.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostDiscard(final String amount, final String type, final String description) {
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
        sb.append("Discard ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else if (this.getType().equals("Hand")) {
            sb.append("your hand");
        } else if (this.getType().equals("LastDrawn")) {
            sb.append("the last card you drew this turn");
        } else {
            final StringBuilder desc = new StringBuilder();

            if (this.getType().equals("Card") || this.getType().equals("Random")) {
                desc.append("card");
            } else {
                desc.append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription()).append(
                        " card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));

            if (this.getType().equals("Random")) {
                sb.append(" at random");
            }
        }
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
        List<Card> handList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Hand));
        String type = this.getType();
        final Integer amount = this.convertAmount();

        if (this.payCostFromSource()) {
            if (!source.isInZone(ZoneType.Hand)) {
                return false;
            }
        } else if (type.equals("Hand")) {
            // this will always work
        } else if (type.equals("LastDrawn")) {
            final Card c = activator.getLastDrawnCard();
            return handList.contains(c);
        } else {
            if (ability.isSpell()) {
                handList.remove(source); // can't pay for itself
            }
            boolean sameName = false;
            if (type.contains("+WithSameName")) {
                sameName = true;
                type = type.replace("+WithSameName", "");
            }
            if (!type.equals("Random")) {
                handList = CardLists.getValidCards(handList, type.split(";"), activator, source);
            }
            if (sameName) {
                for (Card c : handList) {
                    if (CardLists.filter(handList, CardPredicates.nameEquals(c.getName())).size() > 1) {
                        return true;
                    }
                }
                return false;
            }
            if ((amount != null) && (amount > handList.size())) {
                // not enough cards in hand to pay
                return false;
            }
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
            ai.discard(c, ability);
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
        final Player activator = ability.getActivatingPlayer();
        List<Card> handList = new ArrayList<Card>(activator.getCardsIn(ZoneType.Hand));
        String discardType = this.getType();
        final String amount = this.getAmount();
        this.resetList();

        if (this.payCostFromSource()) {
            if (!handList.contains(source)) {
                payment.setCancel(true);
            }
            activator.discard(source, ability);
            payment.setPaidPart(this);
            //this.addToList(source);
        } else if (discardType.equals("Hand")) {
            this.setList(handList);
            activator.discardHand(ability);
            payment.setPaidPart(this);
        } else if (discardType.equals("LastDrawn")) {
            final Card lastDrawn = activator.getLastDrawnCard();
            this.addToList(lastDrawn);
            if (!handList.contains(lastDrawn)) {
                payment.setCancel(true);
            }
            activator.discard(lastDrawn, ability);
            payment.setPaidPart(this);
        } else {
            Integer c = this.convertAmount();

            if (discardType.equals("Random")) {
                if (c == null) {
                    final String sVar = ability.getSVar(amount);
                    // Generalize this
                    if (sVar.equals("XChoice")) {
                        c = CostUtil.chooseXValue(source, ability,  handList.size());
                    } else {
                        c = AbilityUtils.calculateAmount(source, amount, ability);
                    }
                }

                this.setList(activator.discardRandom(c, ability));
                payment.setPaidPart(this);
            } else {
                String type = new String(discardType);
                boolean sameName = false;
                if (type.contains("+WithSameName")) {
                    sameName = true;
                    type = type.replace("+WithSameName", "");
                }
                final String[] validType = type.split(";");
                handList = CardLists.getValidCards(handList, validType, activator, ability.getSourceCard());
                final List<Card> landList2 = handList;
                if (sameName) {
                    handList = CardLists.filter(handList, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            for (Card card : landList2) {
                                if (!card.equals(c) && card.getName().equals(c.getName())) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }

                if (c == null) {
                    final String sVar = ability.getSVar(amount);
                    // Generalize this
                    if (sVar.equals("XChoice")) {
                        c = CostUtil.chooseXValue(source, ability, handList.size());
                    } else {
                        c = AbilityUtils.calculateAmount(source, amount, ability);
                    }
                }

                CountDownLatch cdl = new CountDownLatch(1);
                final Input inp = new InputPayCostDiscard(cdl, ability, handList, this, payment, c, discardType);
                FThreads.setInputAndWait(inp, cdl);
            }
        }
        if ( !payment.isCanceled())
            this.addListToHash(ability, "Discarded");
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
        final String type = this.getType();

        final List<Card> hand = ai.getCardsIn(ZoneType.Hand);
        this.resetList();
        if (type.equals("LastDrawn")) {
            if (!hand.contains(ai.getLastDrawnCard())) {
                return false;
            }
            this.addToList(ai.getLastDrawnCard());
        }

        else if (this.payCostFromSource()) {
            if (!hand.contains(source)) {
                return false;
            }

            this.addToList(source);
        }

        else if (type.equals("Hand")) {
            this.getList().addAll(hand);
        }

        else {
            if (type.contains("WithSameName")) {
                return false;
            }
            Integer c = this.convertAmount();
            if (c == null) {
                final String sVar = ability.getSVar(this.getAmount());
                if (sVar.equals("XChoice")) {
                    return false;
                }
                c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
            }

            if (type.equals("Random")) {
                this.setList(CardLists.getRandomSubList(hand, c));
            } else {
                this.setList(ai.getAi().getCardsToDiscard(c, type.split(";"), ability));
            }
        }
        return this.getList() != null;
    }

    // Inputs

}
