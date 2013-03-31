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
import java.util.Collection;
import java.util.List;
import forge.Card;
import forge.CardUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

/**
 * The Class CostPartWithList.
 */
public abstract class CostPartWithList extends CostPart {

    /** The list. */
    private final List<Card> list = new ArrayList<Card>(); 
    // set is here because executePayment() adds card to list, while ai's decide payment does the same thing.
    // set allows to avoid duplication

    /**
     * Gets the list.
     * 
     * @return the list
     */
    public final List<Card> getList() {
        return this.list;
    }

    /**
     * Reset list.
     */
    public final void resetList() {
        this.list.clear();
    }

    protected final void addToList(final Card c) {
        this.list.add(c);
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
        final String paymentMethod = getHashForList();
        for (final Card card : this.list) {
            sa.addCostToHashList(CardUtil.getLKICopy(card), paymentMethod);
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
        this.list.add(targetCard);
        doPayment(ability, targetCard);
        return true;
    }

    // always returns true, made this to inline with return
    public final boolean executePayment(SpellAbility ability, Collection<Card> targetCards) {
        if(canPayListAtOnce()) { // This is used by reveal. Without it when opponent would reveal hand, you'll get N message boxes. 
            this.list.addAll(targetCards);
            doListPayment(ability, targetCards);
            return true;
        }
        for(Card c: targetCards)
            executePayment(ability, c);
        return true;
    }

    protected abstract void doPayment(SpellAbility ability, Card targetCard);
    // Overload these two only together, set to true and perform payment on list
    protected boolean canPayListAtOnce() { return false; }
    protected void doListPayment(SpellAbility ability, Collection<Card> targetCards) { };

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public abstract String getHashForList();
    
    @Override
    public void payAI(PaymentDecision decision, AIPlayer ai, SpellAbility ability, Card source) {
        executePayment(ability, decision.cards);
        reportPaidCardsTo(ability);
    }

}
