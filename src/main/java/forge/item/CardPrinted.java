package forge.item;

import java.util.Arrays;
import java.util.List;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import net.slightlymagic.maxmtg.PredicateString;

import org.apache.commons.lang3.ArrayUtils;

import forge.AllZone;
import forge.Card;
import forge.CardUtil;
import forge.card.CardRarity;
import forge.card.CardRules;

/**
 * <p>
 * CardReference class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardPrinted implements Comparable<CardPrinted>, InventoryItemFromSet {
    // Reference to rules
    private final transient CardRules card;

    // These fields are kinda PK for PrintedCard
    private final String name;
    private final String cardSet;
    private final int artIndex;
    private final boolean foiled;
    private final boolean isAlternate;
    private final boolean isDoubleFaced;

    // Calculated fields are below:
    private final transient CardRarity rarity; // rarity is given in ctor when
                                               // set is assigned

    // need this to be sure that different cased names won't break the system
    // (and create uniqie cardref entries)
    private final transient String nameLcase;

    // image filename is calculated only after someone request it
    private transient String imageFilename = null;

    // field RO accessors
    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getName()
     */
    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    /**
     * @return String
     */
    public String getSet() {
        return cardSet;
    }

    /**
     * Gets the art index.
     *
     * @return the art index
     */
    public int getArtIndex() {
        return artIndex;
    }

    /**
     * Checks if is foil.
     *
     * @return true, if is foil
     */
    public boolean isFoil() {
        return foiled;
    }

    /**
     * Gets the card.
     *
     * @return the card
     */
    public CardRules getCard() {
        return card;
    }

    /**
     * Gets the rarity.
     *
     * @return the rarity
     */
    public CardRarity getRarity() {
        return rarity;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    /**
     * @return String
     */
    public String getImageFilename() {
        if (imageFilename == null) {
            imageFilename = CardUtil.buildFilename(this);
        }
        return imageFilename;
    }

    /* (non-Javadoc)
     * @see forge.item.InventoryItem#getType()
     */
    /**
     * @return String
     */
    public String getType() {
        return card.getType().toString();
    }

    // Lambda to get rules for selects from list of printed cards
    /** The Constant fnGetRules. */
    public static final Lambda1<CardRules, CardPrinted> fnGetRules = new Lambda1<CardRules, CardPrinted>() {
        @Override
        public CardRules apply(final CardPrinted from) {
            return from.card;
        }
    };

    // Constructor is private. All non-foiled instances are stored in CardDb
    private CardPrinted(final CardRules c, final String set, final CardRarity rare, final int index,
            final boolean foil, final boolean isAlt, final boolean isDF)
    {
        card = c;
        name = c.getName();
        cardSet = set;
        artIndex = index;
        foiled = foil;
        rarity = rare;
        isAlternate = isAlt;
        isDoubleFaced = isDF;
        nameLcase = name.toLowerCase();
    }

    /* package visibility */
    /**
     * Builds the.
     *
     * @param c the c
     * @param set the set
     * @param rare the rare
     * @param index the index
     * @param isAlt the is alt
     * @param isDF the is df
     * @return the card printed
     */
    static CardPrinted build(final CardRules c, final String set, final CardRarity rare, final int index,
            final boolean isAlt, final boolean isDF)
    {
        return new CardPrinted(c, set, rare, index, false, isAlt, isDF);
    }

    /* foiled don't need to stay in CardDb's structures, so u'r free to create */
    /**
     * Make foiled.
     *
     * @param c the c
     * @return the card printed
     */
    public static CardPrinted makeFoiled(final CardPrinted c) {
        return new CardPrinted(c.card, c.cardSet, c.rarity, c.artIndex, true, c.isAlternate, c.isDoubleFaced);
    }

    // Want this class to be a key for HashTable
    /* (non-Javadoc)
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
        if (getClass() != obj.getClass()) {
            return false;
        }

        CardPrinted other = (CardPrinted) obj;
        if (!name.equals(other.name)) {
            return false;
        }
        if (!cardSet.equals(other.cardSet)) {
            return false;
        }
        if (other.foiled != this.foiled || other.artIndex != this.artIndex) {
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int code = nameLcase.hashCode() * 11 + cardSet.hashCode() * 59 + artIndex * 2;
        if (foiled) {
            return code + 1;
        }
        return code;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
        // cannot still decide, if this "name|set" format is needed anymore
        // return String.format("%s|%s", name, cardSet);
    }

    /**
     * To forge card.
     *
     * @return the card
     */
    public Card toForgeCard() {
        Card c = AllZone.getCardFactory().getCard(name, null);
        if (c != null) {
            c.setCurSetCode(getSet());
            c.setRandomPicture(artIndex + 1);
            c.setImageFilename(getImageFilename());
            if (c.hasAlternateState()) {
                c.changeState();
                c.setImageFilename(CardUtil.buildFilename(c));
                c.changeState();
            }
        }
        // else throw "Unsupported card";
        return c;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardPrinted o) {
        int nameCmp = nameLcase.compareTo(o.nameLcase);
        if (0 != nameCmp) {
            return nameCmp;
        }
        // TODO compare sets properly
        return cardSet.compareTo(o.cardSet);
    }

    /**
     * Checks if is alternate.
     *
     * @return true, if is alternate
     */
    public boolean isAlternate() {
        return isAlternate;
    }

    /**
     * Checks if is double faced.
     *
     * @return true, if is double faced
     */
    public boolean isDoubleFaced() {
        return isDoubleFaced;
    }

    /**
     * Number of filters based on CardPrinted values.
     */
    public abstract static class Predicates {

        /**
         * Rarity.
         *
         * @param isEqual the is equal
         * @param value the value
         * @return the predicate
         */
        public static Predicate<CardPrinted> rarity(final boolean isEqual, final CardRarity value) {
            return new PredicateRarity(value, isEqual);
        }

        /**
         * Printed in sets.
         *
         * @param value the value
         * @param shouldContain the should contain
         * @return the predicate
         */
        public static Predicate<CardPrinted> printedInSets(final List<String> value, final boolean shouldContain) {
            if (value == null || value.isEmpty()) {
                return Predicate.getTrue(CardPrinted.class);
            }
            return new PredicateSets(value, shouldContain);
        }

        /**
         * Printed in sets.
         *
         * @param value the value
         * @return the predicate
         */
        public static Predicate<CardPrinted> printedInSets(final String value) {
            if (value == null || value.isEmpty()) {
                return Predicate.getTrue(CardPrinted.class);
            }
            return new PredicateSets(Arrays.asList(new String[] {value}), true);
        }

        /**
         * Name.
         *
         * @param what the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> name(final String what) {
            return new PredicateName(PredicateString.StringOp.EQUALS, what);
        }

        /**
         * Name.
         *
         * @param op the op
         * @param what the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> name(final PredicateString.StringOp op, final String what) {
            return new PredicateName(op, what);
        }

        /**
         * Names except.
         *
         * @param what the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> namesExcept(final List<String> what) {
            return new PredicateNamesExcept(what);
        }

        private static class PredicateNotAlternate extends Predicate<CardPrinted> {
            @Override
            public boolean isTrue(final CardPrinted card) {
                return !card.isAlternate;
            }
        }

        private static class PredicateRarity extends Predicate<CardPrinted> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardPrinted card) {
                return card.rarity.equals(operand) == shouldBeEqual;
            }

            public PredicateRarity(final CardRarity type, final boolean wantEqual) {
                operand = type;
                shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateSets extends Predicate<CardPrinted> {
            private final List<String> sets;
            private final boolean mustContain;

            @Override
            public boolean isTrue(final CardPrinted card) {
                return sets.contains(card.cardSet) == mustContain;
            }

            public PredicateSets(final List<String> wantSets, final boolean shouldContain) {
                sets = wantSets; // maybe should make a copy here?
                mustContain = shouldContain;
            }
        }

        private static class PredicateName extends PredicateString<CardPrinted> {
            private final String operand;

            @Override
            public boolean isTrue(final CardPrinted card) {
                return op(card.getName(), operand);
            }

            public PredicateName(final PredicateString.StringOp operator, final String operand) {
                super(operator);
                this.operand = operand;
            }
        }

        private static class PredicateNamesExcept extends PredicateString<CardPrinted> {
            private final String[] operand;

            @Override
            public boolean isTrue(final CardPrinted card) {
                String cardName = card.getName();
                for (int i = 0; i < operand.length; i++) {
                    if (op(cardName, operand[i])) {
                        return false;
                    }
                }
                return true;
            }

            public PredicateNamesExcept(final List<String> operand) {
                super(StringOp.EQUALS);
                this.operand = operand.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            }
        }

        /**
         * Pre-built predicates are stored here to allow their re-usage and
         * easier access from code.
         */
        public abstract static class Presets {
            // Think twice before using these, since rarity is a prop of printed
            // card.
            /** The Constant isCommon. */
            public static final Predicate<CardPrinted> isCommon = rarity(true, CardRarity.Common);
            
            /** The Constant isUncommon. */
            public static final Predicate<CardPrinted> isUncommon = rarity(true, CardRarity.Uncommon);
            
            /** The Constant isRare. */
            public static final Predicate<CardPrinted> isRare = rarity(true, CardRarity.Rare);
            
            /** The Constant isMythicRare. */
            public static final Predicate<CardPrinted> isMythicRare = rarity(true, CardRarity.MythicRare);
            
            /** The Constant isRareOrMythic. */
            public static final Predicate<CardPrinted> isRareOrMythic = Predicate.or(isRare, isMythicRare);
            
            /** The Constant isSpecial. */
            public static final Predicate<CardPrinted> isSpecial = rarity(true, CardRarity.Special);
            
            /** The Constant exceptLands. */
            public static final Predicate<CardPrinted> exceptLands = rarity(false, CardRarity.BasicLand);

            /** The Constant isTrue. */
            public static final Predicate<CardPrinted> isTrue = Predicate.getTrue(CardPrinted.class);

            /** The Constant nonAlternate. */
            public static final Predicate<CardPrinted> nonAlternate = new PredicateNotAlternate();
        }
    }
}
