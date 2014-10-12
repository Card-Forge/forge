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
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The Class CostPartWithList.
 */
public abstract class CostPartWithList extends CostPart {
    /** The lists: one for LKI, one for the actual cards. */
    private final CardCollection lkiList = new CardCollection();
    protected final CardCollection cardList = new CardCollection();
    // set is here because executePayment() adds card to list, while ai's decide payment does the same thing.
    // set allows to avoid duplication

    public final CardCollectionView getLKIList() {
        return lkiList;
    }
    
    public final CardCollectionView getCardList() {
    	return cardList;
    }

    /**
     * Reset list.
     */
    public final void resetLists() {
        lkiList.clear();
        cardList.clear();
    }

    /**
     * Adds the list to hash.
     * 
     * @param sa
     *            the sa
     * @param hash
     *            the hash
     */
    public final void reportPaidCardsTo(final SpellAbility sa) {
        final String lkiPaymentMethod = getHashForLKIList();
        for (final Card card : lkiList) {
            sa.addCostToHashList(card, lkiPaymentMethod);
        }
        final String cardPaymentMethod = getHashForCardList();
        for (final Card card : cardList) {
            sa.addCostToHashList(card, cardPaymentMethod);
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

    public final boolean executePayment(SpellAbility ability, Card targetCard) {
        lkiList.add(CardUtil.getLKICopy(targetCard));
        final Card newCard = doPayment(ability, targetCard);
        cardList.add(newCard);
        return true;
    }

    // always returns true, made this to inline with return
    public final boolean executePayment(SpellAbility ability, CardCollectionView targetCards) {
        if (canPayListAtOnce()) { // This is used by reveal. Without it when opponent would reveal hand, you'll get N message boxes. 
            lkiList.addAll(targetCards);
            cardList.addAll(doListPayment(ability, targetCards));
            return true;
        }
        for (Card c: targetCards) {
            executePayment(ability, c);
        }
        return true;
    }

    /**
     * Do a payment with a single card.
     * @param ability the {@link SpellAbility} to pay for.
     * @param targetCard the {@link Card} to pay with.
     * @return The physical card after the payment.
     */
    protected abstract Card doPayment(SpellAbility ability, Card targetCard);
    // Overload these two only together, set to true and perform payment on list
    protected boolean canPayListAtOnce() { return false; }
    protected CardCollectionView doListPayment(SpellAbility ability, CardCollectionView targetCards) { return CardCollection.EMPTY; };

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public abstract String getHashForLKIList();
    public abstract String getHashForCardList();
    
    @Override
    public boolean payAsDecided(Player ai, PaymentDecision decision, SpellAbility ability) {
        executePayment(ability, decision.cards);
        reportPaidCardsTo(ability);
        return true;
    }

}
