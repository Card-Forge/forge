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
    public int paymentOrder() { return 15; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Exile ");

        String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
            if (!this.from.equals(ZoneType.Battlefield)) {
                sb.append(" from your ").append(this.from);
            }
            return sb.toString();
        } else if (this.getType().equals("All")) {
            sb.append(" all cards from your ").append(this.from);
            return sb.toString();
        }

        if (this.from.equals(ZoneType.Battlefield)) {
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));
            if (!this.payCostFromSource()) {
                sb.append(" you control");
            }
            return sb.toString();
        }

        if (!desc.equals("Card") && !desc.endsWith("card")) {
            desc += " card";
        }
        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        if (this.sameZone) {
            sb.append(" from the same ");
        } else {
            sb.append(" from your ");
        }

        sb.append(this.from);

        return sb.toString();
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
        }
        else {
            list = payer.getCardsIn(this.from);
        }

        if (this.payCostFromSource()) {
            return list.contains(source);
        }

        list = CardLists.getValidCards(list, type.split(";"), payer, source, ability);

        final Integer amount = this.convertAmount();
        if ((amount != null) && (list.size() < amount)) {
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
            if (!foundPayable) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        final Game game = targetCard.getGame();
        return game.getAction().exile(targetCard, null);
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
