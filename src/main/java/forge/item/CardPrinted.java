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
import forge.Singletons;
import forge.card.CardInSet;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.cardfactory.CardFactory;
import forge.game.player.Player;
import forge.util.Base64Coder;


/**
 * A viciously lightweight version of a card, for instances
 * where a full set of meta and rules is not needed.
 * <br><br>
 * The full set of rules is in the CardRules class.
 * 
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardPrinted implements Comparable<IPaperCard>, InventoryItemFromSet, IPaperCard {
    // Reference to rules
    private final transient CardRules card;

    // These fields are kinda PK for PrintedCard
    private final String name;
    private final String edition;
    private final int artIndex;
    private final boolean foiled;

    // Calculated fields are below:
    private final transient CardRarity rarity; // rarity is given in ctor when
                                               // set is assigned

    // field RO accessors
    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getName()
     */
    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public String getDescription() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getEdition()
     */
    @Override
    public String getEdition() {
        return this.edition;
    }

    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getArtIndex()
     */
    @Override
    public int getArtIndex() {
        return this.artIndex;
    }

    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#isFoil()
     */
    @Override
    public boolean isFoil() {
        return this.foiled;
    }

    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getRules()
     */
    @Override
    public CardRules getRules() {
        return this.card;
    }

    /* (non-Javadoc)
     * @see forge.item.ICardPrinted#getRarity()
     */
    @Override
    public CardRarity getRarity() {
        return this.rarity;
    }

    private static String toMWSFilename(String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/') || (c == ':') || (c == '?')) {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }    

    private String getImageName() {
        return CardSplitType.Split != card.getSplitType() ? name : card.getMainPart().getName() + card.getOtherPart().getName();
    }
    
    @Override
    public String getImageFilename() {
        return getImageLocator(getImageName(), getArtIndex(), true, false);
    }
    
    public String getImageFilename(boolean backFace) {
        return getImageFilename(backFace, getArtIndex(), true);
    }
    
    public String getImageFilename(boolean backFace, int artIdx, boolean includeSet) {
        final String nameToUse;
        if (backFace) {
            if (null == card.getOtherPart()) {
                return null;
            }
            switch (card.getSplitType()) {
            case Transform: case Flip: case Licid:
                break;
            default:
                return null;
            }
            nameToUse = card.getOtherPart().getName();
        } else {
            nameToUse = getImageName();
        }

        return getImageLocator(nameToUse, artIdx, includeSet, false);
    }
    
    public String getImageUrlPath(boolean backFace) {
        return getImageLocator(backFace ? card.getOtherPart().getName() : getImageName(), getArtIndex(), true, true);
    }
    
    private String getImageLocator(String nameToUse, int artIdx, boolean includeSet, boolean base64encode) {
        StringBuilder s = new StringBuilder();
        
        s.append(toMWSFilename(nameToUse));
        
        final int cntPictures;
        if (includeSet) {
            cntPictures = card.getEditionInfo(edition).getCopiesCount();
        } else {
            // raise the art index limit to the maximum of the sets this card was printed in
            int maxCntPictures = 1;
            for (String set : card.getSets()) {
                CardInSet setInfo = card.getEditionInfo(set);
                if (maxCntPictures < setInfo.getCopiesCount()) {
                    maxCntPictures = setInfo.getCopiesCount();
                }
            }
            cntPictures = maxCntPictures;
        }
        if (cntPictures > 1  && cntPictures > artIdx) {
            s.append(artIdx + 1);
        }
        s.append(".full");
        
        final String fname;
        if (base64encode) {
            s.append(".jpg");
            fname = Base64Coder.encodeString(s.toString(), true);
        } else {
            fname = s.toString();
        }
        
        if (includeSet) {
            return String.format("%s/%s", Singletons.getModel().getEditions().getCode2ByCode(edition), fname);
        } else {
            return fname;
        }
    }
    
    @Override
    public String getItemType() {
        return "Card";
    }

    /**
     * Lambda to get rules for selects from list of printed cards.
     */
    public static final Function<CardPrinted, CardRules> FN_GET_RULES = new Function<CardPrinted, CardRules>() {
        @Override
        public CardRules apply(final CardPrinted from) {
            return from.card;
        }
    };
    public static final Function<CardPrinted, String> FN_GET_NAME = new Function<CardPrinted, String>() {
        @Override
        public String apply(final CardPrinted from) {
            return from.getName();
        }
    };    

    public static final Function<CardPrinted, Integer> FN_GET_EDITION_INDEX = new Function<CardPrinted, Integer>() {
        @Override
        public Integer apply(final CardPrinted from) {
            return Integer.valueOf(Singletons.getModel().getEditions().get(from.getEdition()).getIndex());
        }
    };

    // Constructor is private. All non-foiled instances are stored in CardDb
    private CardPrinted(final CardRules c, final String edition0, final CardRarity rare, final int index, final boolean foil) {
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;
        this.artIndex = index;
        this.foiled = foil;
        this.rarity = rare;
    }

    /* package visibility */
    /**
     * Builds the.
     * 
     * @param c
     *            the c
     * @param edition
     *            the set
     * @param rare
     *            the rare
     * @param index
     *            the index
     * @return the card printed
     */
    static CardPrinted build(final CardRules c, final String edition, final CardRarity rare, final int index) {
        return new CardPrinted(c, edition, rare, index, false);
    }

    /* foiled don't need to stay in CardDb's structures, so u'r free to create */
    /**
     * Make foiled.
     * 
     * @param c
     *            the c
     * @return the card printed
     */
    public static CardPrinted makeFoiled(final CardPrinted c) {
        return new CardPrinted(c.card, c.edition, c.rarity, c.artIndex, true);
    }

    // Want this class to be a key for HashTable
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

        final CardPrinted other = (CardPrinted) obj;
        if (!this.name.equals(other.name)) {
            return false;
        }
        if (!this.edition.equals(other.edition)) {
            return false;
        }
        if ((other.foiled != this.foiled) || (other.artIndex != this.artIndex)) {
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
        if (this.foiled) {
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
    private static final Map<CardPrinted, Card> cp2card = new HashMap<CardPrinted, Card>();
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

    @Override
    public boolean isToken() {
        return false;
    }
}
