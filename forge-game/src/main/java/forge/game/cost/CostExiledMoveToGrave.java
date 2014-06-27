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

import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

/**
 * This is for the "ExiledMoveToGrave" Cost.
 */
public class CostExiledMoveToGrave extends CostPartWithList {
    // ExiledMoveToGrave<Num/Type{/TypeDescription}>

    /**
     * Instantiates a new cost CostExiledMoveToGrave.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostExiledMoveToGrave(final String amount, final String type, final String description) {
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
        final Integer i = this.convertAmount();
        sb.append("Put ");

        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        sb.append(" into its owner's graveyard");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "MovedToGrave";
    }
    @Override
    public String getHashForCardList() {
    	return "MovedToGraveCards";
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getHostCard();

        Integer i = this.convertAmount();

        if (i == null) {
            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }


        List<Card> typeList = activator.getGame().getCardsIn(ZoneType.Exile);

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
        if (typeList.size() < i) {
            return false;
        }

        return true;

    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        return targetCard.getGame().getAction().moveToGraveyard(targetCard);
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
