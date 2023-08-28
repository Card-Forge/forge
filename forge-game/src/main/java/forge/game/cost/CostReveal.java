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

import java.util.Arrays;
import java.util.List;

import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Class CostReveal.
 */
public class CostReveal extends CostPartWithList {
    // Reveal<Num/Type/TypeDescription>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;

    private List<ZoneType> revealFrom = Arrays.asList(ZoneType.Hand);

    public CostReveal(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public CostReveal(final String amount, final String type, final String description, final String zoneType) {
        super(amount, type, description);
        this.revealFrom = ZoneType.listValueOf(zoneType);
    }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }

    public List<ZoneType> getRevealFrom() {
        return revealFrom;
    }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        CardCollectionView handList = payer.getCardsIn(revealFrom);
        if (ability.isSpell()) {
            CardCollection modifiedHand = new CardCollection(handList);
            modifiedHand.remove(source); // can't pay for itself
            handList = modifiedHand;
        }
        handList = CardLists.getValidCards(handList, getType(), payer, source, ability);

        return handList.size();
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();

        CardCollectionView handList = payer.getCardsIn(revealFrom);
        final int amount = this.getAbilityAmount(ability);

        if (this.payCostFromSource()) {
            return revealFrom.contains(source.getLastKnownZone().getZoneType());
        } else if (this.getType().equals("Hand")) {
            return true;
        } else if (this.getType().equals("SameColor")) {
            for (final Card card : handList) {
                if (CardLists.count(handList, CardPredicates.sharesColorWith(card)) >= amount) {
                    return true;
                }
            }
            return false;
        } else {
            return amount <= getMaxAmountX(ability, payer, effect);
        }
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Reveal ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else if (this.getType().equals("Hand")) {
            return ("Reveal your hand");
        } else if (this.getType().equals("SameColor")) {
            return ("Reveal " + i + " cards from your hand that share a color");
        } else {
            final StringBuilder desc = new StringBuilder();

            if (this.getType().equals("Card")) {
                desc.append("Card");
            } else {
                desc.append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription()).append(
                        " card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));
        }

        sb.append(" from your ");
        sb.append(revealFrom.get(0).getTranslatedName());

        if (revealFrom.size() > 1) {
            final StringBuilder desc = new StringBuilder();
            desc.append(this.getTypeDescription() == null ? this.getType() : this.getTypeDescription());
            sb.append(" or choose ");
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));
            sb.append(" you control");
        }

        return sb.toString();
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        targetCard.getGame().getAction().reveal(new CardCollection(targetCard), payer);
        StringBuilder sb = new StringBuilder();
        sb.append(ability.getActivatingPlayer());
        if (targetCard.isInZone(ZoneType.Hand)) {
            sb.append(" reveals ");
        } else {
            sb.append(" chooses ");
        }
        sb.append(targetCard).append(" to pay a cost for ");
        sb.append(ability);
        targetCard.getGame().getGameLog().add(GameLogEntryType.INFORMATION, sb.toString());
        return targetCard;
    }

    @Override
    protected boolean canPayListAtOnce() {
        return true;
    }

    @Override
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        ability.getActivatingPlayer().getGame().getAction().reveal(targetCards, payer);
        return targetCards;
    }

    @Override
    public String getHashForLKIList() {
        return "Revealed";
    }
    @Override
    public String getHashForCardList() {
    	return "RevealedCards";
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int paymentOrder() {
        // Caller of the Untamed needs the reveal to happen before the mana cost
        if (!revealFrom.get(0).equals(ZoneType.Hand)) { return -1; }
        return 5;
    }
}
