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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Function;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRarity;
import forge.card.CardRules;

/**
 * A lightweight version of a card that matches real-world cards, to use outside of games (eg. inventory, decks, trade).
 * <br><br>
 * The full set of rules is in the CardRules class.
 * 
 * @author Forge
 */
public final class PaperCard implements Comparable<IPaperCard>, InventoryItemFromSet, IPaperCard, Serializable {
    private static final long serialVersionUID = 2942081982620691205L;

    // Reference to rules
    private transient CardRules rules;

    // These fields are kinda PK for PrintedCard
    private final String name;
    private final String edition;
    private final int artIndex;
    private final boolean foil;
    private Boolean hasImage;

    // Calculated fields are below:
    private transient CardRarity rarity; // rarity is given in ctor when set is assigned

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEdition() {
        return edition;
    }

    @Override
    public int getArtIndex() {
        return artIndex;
    }

    @Override
    public boolean isFoil() {
        return foil;
    }

    @Override
    public boolean isToken() {
        return false;
    }

    @Override
    public CardRules getRules() {
        return rules;
    }

    @Override
    public CardRarity getRarity() {
        return rarity;
    }

//    @Override
//    public String getImageKey() {
//        return getImageLocator(getImageName(), getArtIndex(), true, false);
//    }

    @Override
    public String getItemType() {
        return "Card";
    }

    public boolean hasImage() {
        if (hasImage == null) { //cache value since it's not free to calculate
            hasImage = ImageKeys.hasImage(this);
        }
        return hasImage;
    }

    /**
     * Lambda to get rules for selects from list of printed cards.
     */
    public static final Function<PaperCard, CardRules> FN_GET_RULES = new Function<PaperCard, CardRules>() {
        @Override
        public CardRules apply(final PaperCard from) {
            return from.rules;
        }
    };
    public static final Function<PaperCard, String> FN_GET_NAME = new Function<PaperCard, String>() {
        @Override
        public String apply(final PaperCard from) {
            return from.getName();
        }
    };

    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0, final int artIndex0) {
        this(rules0, edition0, rarity0, artIndex0, false);
    }
    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0, final int artIndex0, final boolean foil0) {
        if (rules0 == null || edition0 == null || rarity0 == null) {
            throw new IllegalArgumentException("Cannot create card without rules, edition or rarity");
        }
        rules = rules0;
        name = rules0.getName();
        edition = edition0;
        artIndex = artIndex0;
        foil = foil0;
        rarity = rarity0;
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
        if (getClass() != obj.getClass()) {
            return false;
        }

        final PaperCard other = (PaperCard) obj;
        if (!name.equals(other.name)) {
            return false;
        }
        if (!edition.equals(other.edition)) {
            return false;
        }
        if ((other.foil != foil) || (other.artIndex != artIndex)) {
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
        final int code = (name.hashCode() * 11) + (edition.hashCode() * 59) + (artIndex * 2);
        if (foil) {
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
        return name;
        // cannot still decide, if this "name|set" format is needed anymore
        // return String.format("%s|%s", name, cardSet);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final IPaperCard o) {
        final int nameCmp = getName().compareToIgnoreCase(o.getName());
        if (0 != nameCmp) {
            return nameCmp;
        }
        // TODO compare sets properly
        int setDiff = edition.compareTo(o.getEdition());
        if ( 0 != setDiff )
            return setDiff;
        
        return Integer.compare(artIndex, o.getArtIndex());
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();

        final IPaperCard pc = StaticData.instance().getCommonCards().getCard(name, edition, artIndex);
        if (pc == null) {
            throw new IOException(String.format("Card %s not found", name));
        }
        rules = pc.getRules();
        rarity = pc.getRarity();
    }

    @Override
    public String getImageKey(boolean altState) {
        String imageKey = ImageKeys.CARD_PREFIX + name + CardDb.NameSetSeparator + edition + CardDb.NameSetSeparator + artIndex;
        if (altState) {
            imageKey += ImageKeys.BACKFACE_POSTFIX;
        }
        return imageKey;
    }
}
