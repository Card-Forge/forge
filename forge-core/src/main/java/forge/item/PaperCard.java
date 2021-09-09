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

import com.google.common.base.Function;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.util.CardTranslation;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.TextUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

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
    /* [NEW] Attribute to store reference to CollectorNumber of each PaperCard.
       By default the attribute is marked as "unset" so that it could be retrieved and set.
       (see getCollectorNumber())
    */
    private String collectorNumber;
    private String artist;
    private final int artIndex;
    private final boolean foil;
    private Boolean hasImage;

    // Calculated fields are below:
    private transient CardRarity rarity; // rarity is given in ctor when set is assigned
    // Reference to a new instance of Self, but foiled!
    private transient PaperCard foiledVersion = null;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEdition() {
        return edition;
    }

    @Override
    public String getCollectorNumber() {
        if (collectorNumber == null)
            collectorNumber = IPaperCard.NO_COLLECTOR_NUMBER;
        return collectorNumber;
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

    @Override
    public String getArtist() {
        if (this.artist == null)
            artist = IPaperCard.NO_ARTIST_NAME;
        return artist;
    }

    /* FIXME: At the moment, every card can get Foiled, with no restriction on the
        corresponding Edition - so we could Foil even Alpha cards.
    */
    public PaperCard getFoiled() {
        if (this.foil)
            return this;

        if (this.foiledVersion == null) {
            this.foiledVersion = new PaperCard(this.rules, this.edition, this.rarity,
                    this.artIndex, true, String.valueOf(collectorNumber), this.artist);
        }
        return this.foiledVersion;
    }

//    @Override
//    public String getImageKey() {
//        return getImageLocator(getImageName(), getArtIndex(), true, false);
//    }

    @Override
    public String getItemType() {
        final Localizer localizer = Localizer.getInstance();
        return localizer.getMessage("lblCard");
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

    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0){
        this(rules0, edition0, rarity0, IPaperCard.DEFAULT_ART_INDEX, false,
                IPaperCard.NO_COLLECTOR_NUMBER, IPaperCard.NO_ARTIST_NAME);
    }

    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0,
                     final int artIndex0, final boolean foil0, final String collectorNumber0, final String artist0) {
        if (rules0 == null || edition0 == null || rarity0 == null) {
            throw new IllegalArgumentException("Cannot create card without rules, edition or rarity");
        }
        rules = rules0;
        name = rules0.getName();
        edition = edition0;
        artIndex = Math.max(artIndex0, IPaperCard.DEFAULT_ART_INDEX);
        foil = foil0;
        rarity = rarity0;
        artist = (artist0 != null ? TextUtil.normalizeText(artist0) : IPaperCard.NO_ARTIST_NAME);
        collectorNumber = (collectorNumber0 != null) && (collectorNumber0.length() > 0) ? collectorNumber0 : IPaperCard.NO_COLLECTOR_NUMBER;
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
        if (!getCollectorNumber().equals(other.getCollectorNumber()))
            return false;
        return (other.foil == foil) && (other.artIndex == artIndex);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int code = (name.hashCode() * 11) + (edition.hashCode() * 59) +
                (artIndex * 2) + (getCollectorNumber().hashCode() * 383);
        if (foil) {
            return code + 1;
        }
        return code;
    }

    // FIXME: Check
    @Override
    public String toString() {
        return CardTranslation.getTranslatedName(name);
        // cannot still decide, if this "name|set" format is needed anymore
        // return String.format("%s|%s", name, cardSet);
    }

    /*
     * This (utility) method transform a collectorNumber String into a key string for sorting.
     * This method proxies the same strategy implemented in CardEdition.CardInSet class from which the
     * collectorNumber of PaperCard instances are originally retrieved.
     * This is also to centralise the criterion, whilst avoiding code duplication.
     *
     * Note: The method has been made private as this is for internal API use **only**, to allow
     * for generalised comparison with IPaperCard instances (see compareTo)
     *
     * The public API of PaperCard includes a method (i.e. getCollectorNumberSortingKey) which applies
     * this method on instance's own collector number.
     *
     * @return a zero-padded 5-digits String + any non-numerical content in the input String, properly attached.
     */
    private static String makeCollectorNumberSortingKey(final String collectorNumber0){
        String collectorNumber = collectorNumber0;
        if (collectorNumber.equals(NO_COLLECTOR_NUMBER))
            collectorNumber = null;
        return CardEdition.CardInSet.getSortableCollectorNumber(collectorNumber);
    }

    private String sortableCNKey = null;
    public String getCollectorNumberSortingKey(){
        if (sortableCNKey == null) {
            // Hardly the case, but just invoke getter rather than direct
            // attribute to be sure that collectorNumber has been retrieved already!
            sortableCNKey = makeCollectorNumberSortingKey(getCollectorNumber());
        }
        return sortableCNKey;
    }


    @Override
    public int compareTo(final IPaperCard o) {
        final int nameCmp = name.compareToIgnoreCase(o.getName());
        if (0 != nameCmp) {
            return nameCmp;
        }
        //FIXME: compare sets properly
        int setDiff = edition.compareTo(o.getEdition());
        if (0 != setDiff)
            return setDiff;
        String thisCollNrKey = getCollectorNumberSortingKey();
        String othrCollNrKey = makeCollectorNumberSortingKey(o.getCollectorNumber());
        final int collNrCmp = thisCollNrKey.compareTo(othrCollNrKey);
        if (0 != collNrCmp) {
            return collNrCmp;
        }
        return Integer.compare(artIndex, o.getArtIndex());
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();

        IPaperCard pc = StaticData.instance().getCommonCards().getCard(name, edition, artIndex);
        if (pc == null) {
            pc = StaticData.instance().getVariantCards().getCard(name, edition, artIndex);
            if (pc == null) {
                throw new IOException(TextUtil.concatWithSpace("Card", name, "not found"));
            }
        }
        rules = pc.getRules();
        rarity = pc.getRarity();
    }

    @Override
    public String getImageKey(boolean altState) {
        String imageKey = ImageKeys.CARD_PREFIX + name + CardDb.NameSetSeparator
                + edition + CardDb.NameSetSeparator + artIndex;
        if (altState) {
            imageKey += ImageKeys.BACKFACE_POSTFIX;
        }
        return imageKey;
    }

    private String cardImageKey = null;
    @Override
    public String getCardImageKey() {
        if (this.cardImageKey == null)
            this.cardImageKey = ImageUtil.getImageKey(this, false, true);
        return cardImageKey;
    }

    private String cardAltImageKey = null;
    @Override
    public String getCardAltImageKey() {
        if (this.cardAltImageKey == null){
            if (this.hasBackFace())
                this.cardAltImageKey = ImageUtil.getImageKey(this, true, true);
            else  // altImageKey will be the same as cardImageKey
                this.cardAltImageKey = ImageUtil.getImageKey(this, false, true);
        }
        return cardAltImageKey;
    }

    @Override
    public boolean hasBackFace(){
        CardSplitType cst = this.rules.getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip || cst == CardSplitType.Meld || cst == CardSplitType.Modal;
    }

    // Return true if card is one of the five basic lands that can be added for free
    public boolean isVeryBasicLand() {
        return (this.getName().equals("Swamp"))
                || (this.getName().equals("Plains"))
                || (this.getName().equals("Island"))
                || (this.getName().equals("Forest"))
                || (this.getName().equals("Mountain"));
    }
}
