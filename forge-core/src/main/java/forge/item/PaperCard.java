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

import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.util.CardTranslation;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A lightweight version of a card that matches real-world cards, to use outside of games (eg. inventory, decks, trade).
 * <br><br>
 * The full set of rules is in the CardRules class.
 *
 * @author Forge
 */
public class PaperCard implements Comparable<IPaperCard>, InventoryItemFromSet, IPaperCard {
    @Serial
    private static final long serialVersionUID = 2942081982620691205L;

    // Reference to rules
    private transient CardRules rules;

    // These fields are kinda PK for PrintedCard
    private final String name;
    private String edition;
    /* [NEW] Attribute to store reference to CollectorNumber of each PaperCard.
       By default the attribute is marked as "unset" so that it could be retrieved and set.
       (see getCollectorNumber())
    */
    private String collectorNumber;
    private String artist;
    private final int artIndex;
    private final boolean foil;
    private final PaperCardFlags flags;
    private final String sortableName;
    private final String functionalVariant;

    // Calculated fields are below:
    private transient CardRarity rarity; // rarity is given in ctor when set is assigned
    // Reference to a new instance of Self, but foiled!
    private transient PaperCard foiledVersion, noSellVersion, flaglessVersion;
    private transient Boolean hasImage;

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
    public String getFunctionalVariant() {
        return functionalVariant;
    }

    @Override
    public ColorSet getMarkedColors() {
        return this.flags.markedColors;
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
                    this.artIndex, true, String.valueOf(collectorNumber), this.artist, this.functionalVariant);
        }
        return this.foiledVersion;
    }
    public PaperCard getUnFoiled() {
        if (!this.foil)
            return this;

        PaperCard unFoiledVersion = new PaperCard(this.rules, this.edition, this.rarity,
                this.artIndex, false, String.valueOf(collectorNumber), this.artist, this.functionalVariant);
        return unFoiledVersion;
    }
    public PaperCard getNoSellVersion() {
        if (this.flags.noSellValue)
            return this;

        if (this.noSellVersion == null)
            this.noSellVersion = new PaperCard(this, this.flags.withNoSellValueFlag(true));
        return this.noSellVersion;
    }

    public PaperCard getMeldBaseCard() {
        if (getRules().getSplitType() != CardSplitType.Meld) {
            return null;
        }

        // This is the base part of the meld duo
        if (getRules().getOtherPart() == null) {
            return this;
        }

        String meldWith = getRules().getMeldWith();
        if (meldWith == null) {
            return null;
        }
        
        List<PrintSheet> sheets = StaticData.instance().getCardEdition(this.edition).getPrintSheetsBySection();
        for (PrintSheet sheet : sheets) {
            if (sheet.contains(this)) {
                return sheet.find(PaperCardPredicates.name(meldWith));
            }
        }

        return null;
    }

    public PaperCard copyWithoutFlags() {
        if(this.flaglessVersion == null) {
            if(this.flags == PaperCardFlags.IDENTITY_FLAGS)
                this.flaglessVersion = this;
            else
                this.flaglessVersion = new PaperCard(this, null);
        }
        return flaglessVersion;
    }
    public PaperCard copyWithFlags(Map<String, String> flags) {
        if(flags == null || flags.isEmpty())
            return this.copyWithoutFlags();
        return new PaperCard(this, new PaperCardFlags(flags));
    }
    public PaperCard copyWithMarkedColors(ColorSet colors) {
        if(Objects.equals(colors, this.flags.markedColors))
            return this;
        return new PaperCard(this, this.flags.withMarkedColors(colors));
    }
    @Override
    public String getItemType() {
        final Localizer localizer = Localizer.getInstance();
        return localizer.getMessage("lblCard");
    }

    public PaperCardFlags getMarkedFlags() {
        return this.flags;
    }

    public boolean hasNoSellValue() {
        return this.flags.noSellValue;
    }
    public boolean hasImage() {
        return hasImage(false);
    }
    public boolean hasImage(boolean update) {
        if (hasImage == null || update) { //cache value since it's not free to calculate
            hasImage = ImageKeys.hasImage(this, update);
        }
        return hasImage;
    }

    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0) {
        this(rules0, edition0, rarity0, IPaperCard.DEFAULT_ART_INDEX, false,
                IPaperCard.NO_COLLECTOR_NUMBER, IPaperCard.NO_ARTIST_NAME, IPaperCard.NO_FUNCTIONAL_VARIANT);
    }

    public PaperCard(final PaperCard copyFrom, final PaperCardFlags flags) {
        this(copyFrom.rules, copyFrom.edition, copyFrom.rarity, copyFrom.artIndex, copyFrom.foil, copyFrom.collectorNumber,
                copyFrom.artist, copyFrom.functionalVariant, flags);
        this.flaglessVersion = copyFrom.flaglessVersion;
    }

    public PaperCard(final CardRules rules0, final String edition0, final CardRarity rarity0,
                     final int artIndex0, final boolean foil0, final String collectorNumber0,
                     final String artist0, final String functionalVariant) {
        this(rules0, edition0, rarity0, artIndex0, foil0, collectorNumber0, artist0, functionalVariant, null);
    }

    protected PaperCard(final CardRules rules, final String edition, final CardRarity rarity,
                     final int artIndex, final boolean foil, final String collectorNumber,
                     final String artist, final String functionalVariant, final PaperCardFlags flags) {
        if (rules == null || edition == null || rarity == null) {
            throw new IllegalArgumentException("Cannot create card without rules, edition or rarity");
        }
        this.rules = rules;
        name = rules.getName();
        this.edition = edition;
        this.artIndex = Math.max(artIndex, IPaperCard.DEFAULT_ART_INDEX);
        this.foil = foil;
        this.rarity = rarity;
        this.artist = artist;
        this.collectorNumber = (collectorNumber != null && !collectorNumber.isEmpty()) ? collectorNumber : IPaperCard.NO_COLLECTOR_NUMBER;
        // If the user changes the language this will make cards sort by the old language until they restart the game.
        // This is a good tradeoff
        sortableName = TextUtil.toSortableName(CardTranslation.getTranslatedName(rules.getName()));
        this.functionalVariant = functionalVariant != null ? functionalVariant : IPaperCard.NO_FUNCTIONAL_VARIANT;

        if(flags == null || flags.equals(PaperCardFlags.IDENTITY_FLAGS))
            this.flags = PaperCardFlags.IDENTITY_FLAGS;
        else
            this.flags = flags;
    }

    public static PaperCard FAKE_CARD = new PaperCard(CardRules.getUnsupportedCardNamed("Fake Card"), "Fake Edition", CardRarity.Common);

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
        if (!Objects.equals(flags, other.flags))
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
        return Objects.hash(name, edition, collectorNumber, artIndex, foil, flags);
    }

    // FIXME: Check
    @Override
    public String toString() {
        return CardTranslation.getTranslatedName(name);
        // cannot still decide, if this "name|set" format is needed anymore
        // return String.format("%s|%s", name, cardSet);
    }
    public String getCardName() {
        return name;
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
        return CardEdition.getSortableCollectorNumber(collectorNumber);
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

    @Serial
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();

        IPaperCard pc = StaticData.instance().getCommonCards().getCard(name, edition, artIndex);
        if (pc == null) {
            pc = StaticData.instance().getVariantCards().getCard(name, edition, artIndex);
            if (pc == null) {
                System.out.println("PaperCard: " + name + " not found with set and index " + edition + ", " + artIndex);
                pc = readObjectAlternate(name, edition);
                if (pc == null) {
                    pc = StaticData.instance().getCommonCards().createUnsupportedCard(name);
                    //throw new IOException(TextUtil.concatWithSpace("Card", name, "not found with set and index", edition, Integer.toString(artIndex)));
                }
                System.out.println("Alternate object found: " + pc.getName() + ", " + pc.getEdition() + ", " + pc.getArtIndex());
            }
        }
        rules = pc.getRules();
        rarity = pc.getRarity();
    }

    private IPaperCard readObjectAlternate(String name, String edition) throws ClassNotFoundException, IOException {
        IPaperCard pc = StaticData.instance().getCommonCards().getCard(name, edition);
        if (pc == null) {
            pc = StaticData.instance().getVariantCards().getCard(name, edition);
        }

        if (pc == null) {
            pc = StaticData.instance().getCommonCards().getCard(name);
            if (pc == null) {
                pc = StaticData.instance().getVariantCards().getCard(name);
            }
        }

        return pc;
    }

    @Serial
    private Object readResolve() throws ObjectStreamException {
        //If we deserialize an old PaperCard with no flags, reinitialize as a fresh copy to set default flags.
        if(this.flags == null)
            return new PaperCard(this, null);
        return this;
    }

    @Override
    public String getImageKey(boolean altState) {
        String normalizedName = StringUtils.stripAccents(name);
        String imageKey = ImageKeys.CARD_PREFIX + normalizedName + CardDb.NameSetSeparator
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
            this.cardImageKey = ImageUtil.getImageKey(this, "", true);
        return cardImageKey;
    }

    private String cardAltImageKey = null;
    @Override
    public String getCardAltImageKey() {
        if (this.cardAltImageKey == null){
            if (this.hasBackFace())
                this.cardAltImageKey = ImageUtil.getImageKey(this, "back", true);
            else  // altImageKey will be the same as cardImageKey
                this.cardAltImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardAltImageKey;
    }

    private String cardWSpecImageKey = null;
    @Override
    public String getCardWSpecImageKey() {
        if (this.cardWSpecImageKey == null) {
            if (this.rules.getSplitType() == CardSplitType.Specialize)
                this.cardWSpecImageKey = ImageUtil.getImageKey(this, "white", true);
            else  // just use cardImageKey
                this.cardWSpecImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardWSpecImageKey;
    }

    private String cardUSpecImageKey = null;
    @Override
    public String getCardUSpecImageKey() {
        if (this.cardUSpecImageKey == null) {
            if (this.rules.getSplitType() == CardSplitType.Specialize)
                this.cardUSpecImageKey = ImageUtil.getImageKey(this, "blue", true);
            else  // just use cardImageKey
                this.cardUSpecImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardUSpecImageKey;
    }

    private String cardBSpecImageKey = null;
    @Override
    public String getCardBSpecImageKey() {
        if (this.cardBSpecImageKey == null) {
            if (this.rules.getSplitType() == CardSplitType.Specialize)
                this.cardBSpecImageKey = ImageUtil.getImageKey(this, "black", true);
            else  // just use cardImageKey
                this.cardBSpecImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardBSpecImageKey;
    }

    private String cardRSpecImageKey = null;
    @Override
    public String getCardRSpecImageKey() {
        if (this.cardRSpecImageKey == null) {
            if (this.rules.getSplitType() == CardSplitType.Specialize)
                this.cardRSpecImageKey = ImageUtil.getImageKey(this, "red", true);
            else  // just use cardImageKey
                this.cardRSpecImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardRSpecImageKey;
    }

    private String cardGSpecImageKey = null;
    @Override
    public String getCardGSpecImageKey() {
        if (this.cardGSpecImageKey == null) {
            if (this.rules.getSplitType() == CardSplitType.Specialize)
                this.cardGSpecImageKey = ImageUtil.getImageKey(this, "green", true);
            else  // just use cardImageKey
                this.cardGSpecImageKey = ImageUtil.getImageKey(this, "", true);
        }
        return cardGSpecImageKey;
    }

    @Override
    public boolean hasBackFace(){
        CardSplitType cst = this.rules.getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip || cst == CardSplitType.Meld
                || cst == CardSplitType.Modal;
    }

    @Override
    public ICardFace getMainFace() {
        ICardFace face = this.rules.getMainPart();
        return this.getVariantForFace(face);
    }

    @Override
    public ICardFace getOtherFace() {
        ICardFace face = this.rules.getOtherPart();
        return this.getVariantForFace(face);
    }

    private ICardFace getVariantForFace(ICardFace face) {
        if(!face.hasFunctionalVariants() || this.functionalVariant.equals(NO_FUNCTIONAL_VARIANT))
            return face;
        ICardFace variant = face.getFunctionalVariant(this.functionalVariant);
        if(variant == null) {
            System.err.printf("Tried to apply unknown or unsupported variant - Card: \"%s\"; Variant: %s\n", face.getName(), this.functionalVariant);
            return face;
        }
        return variant;
    }

    // Return true if card is one of the five basic lands that can be added for free
    public boolean isVeryBasicLand() {
        return (this.getName().equals("Swamp"))
                || (this.getName().equals("Plains"))
                || (this.getName().equals("Island"))
                || (this.getName().equals("Forest"))
                || (this.getName().equals("Mountain"));
    }

    public String getSortableName() {
        return sortableName;
    }
    public boolean isUnRebalanced() {
        return StaticData.instance().isRebalanced("A-" + name);
    }
    public boolean isRebalanced() {
        return StaticData.instance().isRebalanced(name);
    }

    /**
     * Contains properties of a card which distinguish it from an otherwise identical copy of the card with the same
     * name, edition, and collector number. Examples include permanent markings on the card, and flags for Adventure
     * mode.
     */
    public static class PaperCardFlags implements Serializable {
        @Serial
        private static final long serialVersionUID = -3924720485840169336L;

        /**
         * Chosen colors, for cards like Cryptic Spires.
         */
        public final ColorSet markedColors;
        /**
         * Removes the sell value of the card in Adventure mode.
         */
        public final boolean noSellValue;

        //TODO: Could probably move foil here.

        static final PaperCardFlags IDENTITY_FLAGS = new PaperCardFlags(Map.of());

        protected PaperCardFlags(Map<String, String> flags) {
            if(flags.containsKey("markedColors"))
                markedColors = ColorSet.fromNames(flags.get("markedColors").split(""));
            else
                markedColors = null;
            noSellValue = flags.containsKey("noSellValue");
        }

        //Copy constructor. There are some better ways to do this, and they should be explored once we have more than 4
        //or 5 fields here. Just need to ensure it's impossible to accidentally change a field while the PaperCardFlags
        //object is in use.
        private PaperCardFlags(PaperCardFlags copyFrom, ColorSet markedColors, Boolean noSellValue) {
            if(markedColors == null)
                markedColors = copyFrom.markedColors;
            else if(markedColors.isColorless())
                markedColors = null;
            this.markedColors = markedColors;
            this.noSellValue = noSellValue != null ? noSellValue : copyFrom.noSellValue;
        }

        public PaperCardFlags withMarkedColors(ColorSet markedColors) {
            if(markedColors == null)
                markedColors = ColorSet.NO_COLORS;
            return new PaperCardFlags(this, markedColors, null);
        }

        public PaperCardFlags withNoSellValueFlag(boolean noSellValue) {
            return new PaperCardFlags(this, null, noSellValue);
        }

        private Map<String, String> asMap;
        public Map<String, String> toMap() {
            if(asMap != null)
                return asMap;
            Map<String, String> out = new HashMap<>();
            if(markedColors != null && !markedColors.isColorless())
                out.put("markedColors", markedColors.toString());
            if(noSellValue)
                out.put("noSellValue", "true");
            asMap = out;
            return out;
        }

        @Override
        public String toString() {
            return this.toMap().entrySet().stream()
                    .map((e) -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\t"));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PaperCardFlags that)) return false;
            return noSellValue == that.noSellValue && Objects.equals(markedColors, that.markedColors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(markedColors, noSellValue);
        }
    }
}
