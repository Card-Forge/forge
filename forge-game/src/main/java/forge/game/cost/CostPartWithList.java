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
package forge.game.cost;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;

/**
 * The Class CostPartWithList.
 */
public abstract class CostPartWithList extends CostPart {
    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    /** The lists: one for LKI, one for the actual cards. */
    private final CardCollection lkiList = new CardCollection();
    protected final CardCollection cardList = new CardCollection();

    private boolean intrinsic = true;

    protected final CardZoneTable table = new CardZoneTable();
    // set is here because executePayment() adds card to list, while ai's decide payment does the same thing.
    // set allows to avoid duplication

    public final CardCollectionView getLKIList() {
        return lkiList;
    }

    public final CardCollectionView getCardList() {
    	return cardList;
    }

    public final void setIntrinsic(boolean b) {
        intrinsic = b;
    }

    /**
     * Reset list.
     */
    public void resetLists() {
        lkiList.clear();
        cardList.clear();
        table.clear();
    }

    /**
     * Adds the list to hash.
     *
     * @param sa
     *            the sa
     */
    public final void reportPaidCardsTo(final SpellAbility sa) {
        if (sa == null) {
            return;
        }
        final String lkiPaymentMethod = getHashForLKIList();
        for (final Card card : lkiList) {
            sa.addCostToHashList(card, lkiPaymentMethod, intrinsic);
        }
        final String cardPaymentMethod = getHashForCardList();
        for (final Card card : cardList) {
            sa.addCostToHashList(card, cardPaymentMethod, intrinsic);
        }
    }

    // public abstract List<Card> getValidCards();

    /**
     * Instantiates a new cost part with list.
     */
    public CostPartWithList() {
    }

    /**
     * Instantiates a new cost part with list.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostPartWithList(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public final boolean executePayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        lkiList.add(CardUtil.getLKICopy(targetCard));
        final Zone origin = targetCard.getZone();
        final Card newCard = doPayment(payer, ability, targetCard, effect);

        // need to update the LKI info to ensure correct interaction with cards which may trigger on this
        // (e.g. Necroskitter + a creature dying from a -1/-1 counter on a cost payment).
        targetCard.getGame().updateLastStateForCard(targetCard);

        if (newCard != null) {
            final Zone newZone = newCard.getZone();
            cardList.add(newCard);

            if (!origin.equals(newZone)) {
                table.put(origin.getZoneType(), newZone.getZoneType(), newCard);
            }
        }
        return true;
    }

    // always returns true, made this to inline with return
    protected boolean executePayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        handleBeforePayment(payer, ability, targetCards);
        if (canPayListAtOnce()) { // This is used by reveal. Without it when opponent would reveal hand, you'll get N message boxes.
            for (Card c: targetCards) {
                lkiList.add(CardUtil.getLKICopy(c));
            }
            cardList.addAll(doListPayment(payer, ability, targetCards, effect));
        } else {
            for (Card c : targetCards) {
                executePayment(payer, ability, c, effect);
            }
        }
        handleChangeZoneTrigger(payer, ability, targetCards);
        return true;
    }

    /**
     * Do a payment with a single card.
     * @param ability the {@link SpellAbility} to pay for.
     * @param targetCard the {@link Card} to pay with.
     * @return The physical card after the payment.
     */
    protected abstract Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect);
    // Overload these two only together, set to true and perform payment on list
    protected boolean canPayListAtOnce() { return false; }
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) { return CardCollection.EMPTY; }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public abstract String getHashForLKIList();
    public abstract String getHashForCardList();

    @Override
    public boolean payAsDecided(Player payer, PaymentDecision decision, SpellAbility ability, final boolean effect) {
        executePayment(payer, ability, decision.cards, effect);
        reportPaidCardsTo(ability);
        return true;
    }

    protected void handleBeforePayment(Player payer, SpellAbility ability, CardCollectionView targetCards) {

    }

    protected void handleChangeZoneTrigger(Player payer, SpellAbility ability, CardCollectionView targetCards) {
        if (table.isEmpty()) {
            return;
        }

        // copy table because the original get cleaned after the cost is done
        final CardZoneTable copyTable = new CardZoneTable(table);
        copyTable.triggerChangesZoneAll(payer.getGame(), ability);
    }

}
