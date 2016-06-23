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
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostUntapType.
 */
public class CostUntapType extends CostPartWithList {
    public final boolean canUntapSource;

    public CostUntapType(final String amount, final String type, final String description, boolean hasUntapInPrice) {
        super(amount, type, description);
        canUntapSource = !hasUntapInPrice;
    }

    @Override
    public int paymentOrder() { return 18; }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Untap ");

        final Integer i = convertAmount();
        final String desc = getDescriptiveType();

        sb.append(Cost.convertAmountTypeToWords(i, getAmount(), " tapped " + desc));

        if (getType().contains("OppCtrl")) {
            sb.append(" an opponent controls");
        }
        else if (getType().contains("YouCtrl")) {
            sb.append(" you control");
        }
        return sb.toString();
    }

    @Override
    public final void refund(final Card source) {
        for (final Card c : getCardList()) {
            c.setTapped(true);
        }
        resetLists();
    }

    @Override
    public final boolean canPay(final SpellAbility ability) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getHostCard();

        CardCollection typeList = CardLists.getValidCards(activator.getGame().getCardsIn(ZoneType.Battlefield), getType().split(";"), activator, source, ability);

        if (!canUntapSource) {
            typeList.remove(source);
        }
        typeList = CardLists.filter(typeList, Presets.TAPPED);

        final Integer amount = convertAmount();
        if ((typeList.size() == 0) || ((amount != null) && (typeList.size() < amount))) {
            return false;
        }
        return true;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        targetCard.untap();
        return targetCard;
    }

    @Override
    public String getHashForLKIList() {
        return "Untapped";
    }
    @Override
    public String getHashForCardList() {
    	return "UntappedCards";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
