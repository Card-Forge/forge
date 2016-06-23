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
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * This is for the "ExiledMoveToGrave" Cost.
 */
public class CostExiledMoveToGrave extends CostPartWithList {
    // ExiledMoveToGrave<Num/Type{/TypeDescription}>
    public CostExiledMoveToGrave(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    @Override
    public int paymentOrder() { return 15; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = convertAmount();
        sb.append("Put ");

        final String desc = getTypeDescription() == null ? getType() : getTypeDescription();
        sb.append(Cost.convertAmountTypeToWords(i, getAmount(), desc));

        sb.append(" from exile into that player's graveyard");

        return sb.toString();
    }

    @Override
    public String getHashForLKIList() {
        return "MovedToGrave";
    }
    @Override
    public String getHashForCardList() {
    	return "MovedToGraveCards";
    }

    @Override
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getHostCard();

        Integer i = convertAmount();

        if (i == null) {
            i = AbilityUtils.calculateAmount(source, getAmount(), ability);
        }

        CardCollectionView typeList = activator.getGame().getCardsIn(ZoneType.Exile);

        typeList = CardLists.getValidCards(typeList, getType().split(";"), activator, source, ability);
        return typeList.size() >= i;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        return targetCard.getGame().getAction().moveToGraveyard(targetCard);
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
