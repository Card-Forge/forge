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
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

/**
 * This is for the "PutCardToLib" Cost. 
 */
public class CostPutCardToLib extends CostPartWithList {
    // PutCardToLibFromHand<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromSameGrave<Num/LibPos/Type{/TypeDescription}>
    // PutCardToLibFromGrave<Num/LibPos/Type{/TypeDescription}>

    public final ZoneType from;
    public final boolean sameZone;
    private String libPosition = "0";
    
    /**
     * Gets the from.
     * 
     * @return the from
     */
    public final ZoneType getFrom() {
        return this.from;
    }
    
    /**
     * Gets the libposition.
     * 
     * @return the libposition
     */
    public final String getLibPos() {
        return this.libPosition;
    }

    /**
     * isSameZone.
     * 
     * @return a boolean
     */
    public final boolean isSameZone() {
        return this.sameZone;
    }

    /**
     * Instantiates a new cost CostPutCardToLib.
     * 
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     * @param from
     *            the from
     */
    public CostPutCardToLib(final String amount, final String libpos, final String type, final String description, final ZoneType from) {
        this(amount, libpos, type, description, from, false);
    }
    
    public CostPutCardToLib(final String amount, final String libpos, final String type, final String description, final ZoneType from, final boolean sameZone) {
        super(amount, type, description);
        this.from = from == null ? ZoneType.Hand : from;
        this.libPosition = libpos;
        this.sameZone = sameZone;
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

        if (this.sameZone) {
            sb.append(" from the same ");
        } else {
            sb.append(" from your ");
        }

        sb.append(this.from).append(" on ");
        
        if (this.libPosition.equals("0")) {
            sb.append("top of");
        } else {
            sb.append("the bottom of");
        }
        
        if (this.sameZone) {
            sb.append(" their owner's library");
        } else {
            sb.append(" your library");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "CardPutToLib";
    }
    @Override
    public String getHashForCardList() {
    	return "CardPutToLibCards";
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
        final Game game = activator.getGame();

        Integer i = this.convertAmount();

        if (i == null) {
            final String sVar = ability.getSVar(this.getAmount());
            if (sVar.equals("XChoice")) {
                return true;
            }
            i = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }
        
        List<Card> typeList = new ArrayList<Card>();
        if (this.sameZone) {
            typeList = new ArrayList<Card>(game.getCardsIn(this.getFrom()));
        } else {
            typeList = new ArrayList<Card>(activator.getCardsIn(this.getFrom()));
        }

        typeList = CardLists.getValidCards(typeList, this.getType().split(";"), activator, source);
        
        if (typeList.size() < i) {
            return false;
        }

        if (this.sameZone) {
            boolean foundPayable = false;
            List<Player> players = game.getPlayers();
            for (Player p : players) {
                if (CardLists.filter(typeList, CardPredicates.isController(p)).size() >= i) {
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

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(SpellAbility ability, Card targetCard) {
        return targetCard.getGame().getAction().moveToLibrary(targetCard, Integer.parseInt(getLibPos()));
    }


    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
