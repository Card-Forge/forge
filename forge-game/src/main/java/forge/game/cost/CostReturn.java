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
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostReturn.
 */
public class CostReturn extends CostPartWithList {
    // Return<Num/Type{/TypeDescription}>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

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

    @Override
    public int paymentOrder() { return 10; }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer) {
        final Card source = ability.getHostCard();

        CardCollectionView typeList = payer.getCardsIn(ZoneType.Battlefield);
        typeList = CardLists.getValidCards(typeList, getType().split(";"), payer, source, ability);

        return typeList.size();
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
            final String desc = this.getDescriptiveType();
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
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        final Card source = ability.getHostCard();
        if (payCostFromSource()) {
            return source.isInPlay();
        }

        final Integer amount = this.convertAmount();
        return amount == null || getMaxAmountX(ability, payer) >= amount;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        return targetCard.getGame().getAction().moveToHand(targetCard, null);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "Returned";
    }
    @Override
    public String getHashForCardList() {
    	return "ReturnedCards";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
