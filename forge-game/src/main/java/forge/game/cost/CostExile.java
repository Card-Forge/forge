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

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

/**
 * The Class CostExile.
 */
public class CostExile extends CostPartWithList {
    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGrave<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)
    // ExileSameGrave<Num/Type{/TypeDescription}>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    public final ZoneType from;
    public final boolean sameZone;

    public final ZoneType getFrom() {
        return this.from;
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from) {
        this(amount, type, description, from, false);
    }

    public CostExile(final String amount, final String type, final String description, final ZoneType from, final boolean sameZone) {
        super(amount, type, description);
        this.from = from != null ? from : ZoneType.Battlefield;
        this.sameZone = sameZone;
    }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();

        CardCollectionView typeList;
        if (this.sameZone) {
            typeList = game.getCardsIn(this.from);
        } else {
            typeList = payer.getCardsIn(this.from);
        }

        typeList = CardLists.getValidCards(typeList, getType().split(";"), payer, source, ability);

        return typeList.size();
    }

    @Override
    public int paymentOrder() { return 15; }

    @Override
    public final String toString() {
        final Integer i = this.convertAmount();
        String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();
        String origin = this.from.name().toLowerCase();

        if (this.payCostFromSource()) {
            if (!this.from.equals(ZoneType.Battlefield)) {
                return String.format("Exile %s from your %s", this.getType(), origin);
            }
            return String.format("Exile %s", this.getType());
        } else if (this.getType().equals("All")) {
            return String.format("Exile all cards from your %s", origin);
        }

        if (this.from.equals(ZoneType.Battlefield)) {
            if (!this.payCostFromSource()) {
                return String.format("Exile %s you control", Cost.convertAmountTypeToWords(i, this.getAmount(), desc));
            }
            return String.format("Exile %s", Cost.convertAmountTypeToWords(i, this.getAmount(), desc));
        }

        if (!desc.equals("Card") && !desc.contains("card")) {
            if (this.sameZone) {
                return String.format("Exile card %s from the same %s", Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
            }
            return String.format("Exile card %s from your %s", Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
        }

        if (this.sameZone) {
            return String.format("Exile %s from the same %s", Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
        }

        if (this.getAmount().equals("X")) {
            return String.format ("Exile any number of %s from your %s", desc, origin);
        }

        //for very specific situations like Timothar
        if (desc.startsWith("the")) {
            return String.format("Exile %s from your %s", desc, origin);
        }

        return String.format("Exile %s from your %s", Cost.convertAmountTypeToWords(i, this.getAmount(), desc), origin);
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();

        String type = this.getType();
        if (type.equals("All")) {
            return true; // this will always work
        }
        else if (type.contains("FromTopGrave")) {
            type = TextUtil.fastReplace(type, "FromTopGrave", "");
        }

        CardCollectionView list;
        if (this.sameZone) {
            list = game.getCardsIn(this.from);
        } else {
            list = payer.getCardsIn(this.from);
        }

        if (this.payCostFromSource()) {
            return list.contains(source);
        }

        if (!type.contains("X") || ability.getXManaCostPaid() != null) {
            list = CardLists.getValidCards(list, type.split(";"), payer, source, ability);
        }

        Integer amount = this.convertAmount();

        if (amount == null) { // try to calculate when it's defined.
            amount = AbilityUtils.calculateAmount(ability.getHostCard(), getAmount(), ability);
        }

        // for cards like Allosaurus Rider, do not count it
        if (this.from == ZoneType.Hand && source.isInZone(ZoneType.Hand) && list.contains(source)) {
            amount++;
        }

        if (amount != null && list.size() < amount) {
            return false;
        }

        if (this.sameZone && amount != null) {
            boolean foundPayable = false;
            FCollectionView<Player> players = game.getPlayers();
            for (Player p : players) {
                if (CardLists.filter(list, CardPredicates.isController(p)).size() >= amount) {
                    foundPayable = true;
                    break;
                }
            }
            return foundPayable;
        }
        return true;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        final Game game = targetCard.getGame();
        Card newCard = game.getAction().exile(targetCard, null);
        newCard.setExiledWith(ability.getHostCard());
        newCard.setExiledBy(ability.getActivatingPlayer());
        return newCard;
    }

    public static final String HashLKIListKey = "Exiled";
    public static final String HashCardListKey = "ExiledCards";

    @Override
    public String getHashForLKIList() {
        return HashLKIListKey;
    }
    @Override
    public String getHashForCardList() {
        return HashCardListKey;
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
