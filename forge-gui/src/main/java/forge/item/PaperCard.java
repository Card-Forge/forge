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
package forge.item;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Function;

import forge.Card;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.cardfactory.CardFactory;
import forge.game.player.Player;


/**
 * A viciously lightweight version of a card, for instances
 * where a full set of meta and rules is not needed.
 * <br><br>
 * The full set of rules is in the CardRules class.
 * 
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class PaperCard implements Comparable<IPaperCard>, InventoryItemFromSet, IPaperCard {
    // Reference to rules
    private final transient CardRules card;

    // These fields are kinda PK for PrintedCard
    public final String name;
    public final String edition;
    public final int artIndex;
    public final boolean foil;

    // Calculated fields are below:
    private final transient CardRarity rarity; // rarity is given in ctor when set is assigned

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getEdition() {
        return this.edition;
    }

    @Override
    public int getArtIndex() {
        return this.artIndex;
    }

    @Override
    public boolean isFoil() {
        return this.foil;
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public CardRules getRules() {
        return this.card;
    }

    @Override
    public CardRarity getRarity() {
        return this.rarity;
    }


    
    
//    @Override
//    public String getImageKey() {
//        return getImageLocator(getImageName(), getArtIndex(), true, false);
//    }
    
    
    @Override
    public String getItemType() {
        return "Card";
    }

    /**
     * Lambda to get rules for selects from list of printed cards.
     */
    public static final Function<PaperCard, CardRules> FN_GET_RULES = new Function<PaperCard, CardRules>() {
        @Override
        public CardRules apply(final PaperCard from) {
            return from.card;
        }
    };
    public static final Function<PaperCard, String> FN_GET_NAME = new Function<PaperCard, String>() {
        @Override
        public String apply(final PaperCard from) {
            return from.getName();
        }
    };    

    public PaperCard(final CardRules c, final String edition0, final CardRarity rare, final int index) {
        this(c, edition0, rare, index, false);
    }
    
    public PaperCard(final CardRules c, final String edition0, final CardRarity rare, final int index, final boolean foil) {
        if ( edition0 == null || c == null || rare == null )
            throw new IllegalArgumentException("Cannot create card without rules, edition or rarity");
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;
        this.artIndex = index;
        this.foil = foil;
        this.rarity = rare;
    }

    // Want this class to be a key for HashTable
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final PaperCard other = (PaperCard) obj;
        if (!this.name.equals(other.name)) {
            return false;
        }
        if (!this.edition.equals(other.edition)) {
            return false;
        }
        if ((other.foil != this.foil) || (other.artIndex != this.artIndex)) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int code = (this.name.hashCode() * 11) + (this.edition.hashCode() * 59) + (this.artIndex * 2);
        if (this.foil) {
            return code + 1;
        }
        return code;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name;
        // cannot still decide, if this "name|set" format is needed anymore
        // return String.format("%s|%s", name, cardSet);
    }

    /**
     * To forge card.
     * 
     * @return the card
     */
    private static final Map<PaperCard, Card> cp2card = new HashMap<PaperCard, Card>();
    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getMatchingForgeCard()
     */
    @Override
    public Card getMatchingForgeCard() {
        Card res = cp2card.get(this);
        if (null == res) { 
            res = toForgeCard(null); 
            cp2card.put(this, res);
        }
        return res;
    }

    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#toForgeCard(forge.game.player.Player)
     */
    @Override
    public Card toForgeCard(Player owner) {
        final Card c = CardFactory.getCard(this, owner);
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final IPaperCard o) {
        final int nameCmp = this.getName().compareToIgnoreCase(o.getName());
        if (0 != nameCmp) {
            return nameCmp;
        }
        // TODO compare sets properly
        return this.edition.compareTo(o.getEdition());
    }
}
