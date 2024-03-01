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

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * This is for the "PutCardToLib" Cost. 
 */
public class CostPutCardToLib extends CostPartWithList {
    // PutCardToLibFromHand<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromSameGrave<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromGrave<Num/LibPos/Type{/TypeDescription}>

    /**
     * Serializables need a version ID.
     */
    private static final long serialVersionUID = 1L;
    public final ZoneType from;
    public final boolean sameZone;
    private String libPosition = "0";

    public final ZoneType getFrom() {
        return from;
    }

    public final String getLibPos() {
        return libPosition;
    }

    public final boolean isSameZone() {
        return sameZone;
    }

    public CostPutCardToLib(final String amount, final String libpos, final String type, final String description, final ZoneType from) {
        this(amount, libpos, type, description, from, false);
    }
    
    public CostPutCardToLib(final String amount, final String libpos, final String type, final String description, final ZoneType from0, final boolean sameZone0) {
        super(amount, type, description);
        from = from0 == null ? ZoneType.Hand : from0;
        libPosition = libpos;
        sameZone = sameZone0;
    }

    @Override
    public int paymentOrder() { return 10; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = convertAmount();
        sb.append("Put ");
        
        final String desc = getTypeDescription() == null ? getType() : getTypeDescription();
        if (this.payCostFromSource()) {
            sb.append(this.getType());
        } else {
            sb.append(Cost.convertAmountTypeToWords(i, getAmount(), desc));
        }

        if (sameZone) {
            sb.append(" from the same ").append(from);
        } else if (!this.payCostFromSource()) {
            sb.append(" from your ").append(from);
        }

        sb.append(" on ");
        
        if (libPosition.equals("0")) {
            sb.append("top of");
        } else {
            sb.append("the bottom of");
        }
        
        if (sameZone) {
            sb.append(" their owner's library");
        } else if (this.payCostFromSource()) {
            sb.append(" its owner's library");
        } else {
            sb.append(" your library");
        }

        return sb.toString();
    }

    @Override
    public String getHashForLKIList() {
        return "CardPutToLib";
    }
    @Override
    public String getHashForCardList() {
    	return "CardPutToLibCards";
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        final Game game = source.getGame();

        int i = getAbilityAmount(ability);

        CardCollectionView typeList;
        if (sameZone) {
            typeList = game.getCardsIn(getFrom());
        }
        else {
            typeList = payer.getCardsIn(getFrom());
        }

        if (this.payCostFromSource()) {
            return typeList.contains(source);
        }

        typeList = CardLists.getValidCards(typeList, getType().split(";"), payer, source, ability);

        if (typeList.size() < i) {
            return false;
        }

        if (sameZone) {
            boolean foundPayable = false;
            FCollectionView<Player> players = game.getPlayers();
            for (Player p : players) {
                if (CardLists.count(typeList, CardPredicates.isController(p)) >= i) {
                    foundPayable = true;
                    break;
                }
            }
            return foundPayable;
        }
        return true;
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(moveParams, table);
        return targetCard.getGame().getAction().moveToLibrary(targetCard, Integer.parseInt(getLibPos()), null, moveParams);
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
