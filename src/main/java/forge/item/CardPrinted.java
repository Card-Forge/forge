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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.Card;
import forge.CardUtil;
import forge.Singletons;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.player.Player;
import forge.util.PredicateString;


/**
 * A viciously lightweight version of a card, for instances
 * where a full set of meta and rules is not needed.
 * <br><br>
 * The full set of rules is in the CardRules class.
 * 
 * @author Forge
 * @version $Id: CardReference.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardPrinted implements Comparable<CardPrinted>, InventoryItemFromSet {
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

    // need this to be sure that different cased names won't break the system
    // (and create uniqie cardref entries)
    private final transient String nameLcase;

    // image filename is calculated only after someone request it
    private transient String imageFilename = null;

    // field RO accessors
    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getName()
     */
    /**
     * Gets the name.
     * 
     * @return String
     */
    @Override
    public String getName() {
        return this.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    /**
     * Gets the edition code of the card.
     * 
     * @return String
     */
    @Override
    public String getEdition() {
        return this.edition;
    }

    /**
     * Gets the art index.
     * 
     * @return the art index
     */
    public int getArtIndex() {
        return this.artIndex;
    }

    /**
     * Checks if is foil.
     * 
     * @return true, if is foil
     */
    public boolean isFoil() {
        return this.foiled;
    }
    
    public boolean isTraditional() {
        return !(getCard().getType().isVanguard() 
                || getCard().getType().isScheme());
    }

    /**
     * Gets the card.
     * 
     * @return the card
     */
    public CardRules getCard() {
        return this.card;
    }

    /**
     * Gets the rarity.
     * 
     * @return the rarity
     */
    public CardRarity getRarity() {
        return this.rarity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    /**
     * Gets the image filename.
     * 
     * @return String
     */
    @Override
    public String getImageFilename() {
        if (this.imageFilename == null) {
            this.imageFilename = CardUtil.buildFilename(this);
        }
        return this.imageFilename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItem#getType()
     */
    /**
     * Gets the type.
     * 
     * @return String
     */
    @Override
    public String getType() {
        return this.card.getType().toString();
    }

    /**
     * Lambda to get rules for selects from list of printed cards.
     * 
     */
    public static final Function<CardPrinted, CardRules> FN_GET_RULES = new Function<CardPrinted, CardRules>() {
        @Override
        public CardRules apply(final CardPrinted from) {
            return from.card;
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
        this.nameLcase = this.name.toLowerCase();
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
        final int code = (this.nameLcase.hashCode() * 11) + (this.edition.hashCode() * 59) + (this.artIndex * 2);
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
    public Card toForgeCard() {
        return toForgeCard(null);
    }

    /**
     * To forge card.
     *
     * @param owner the owner
     * @return the card
     */
    public Card toForgeCard(Player owner) {
        final Card c = Singletons.getModel().getCardFactory().getCard(this, owner);
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CardPrinted o) {
        final int nameCmp = this.nameLcase.compareTo(o.nameLcase);
        if (0 != nameCmp) {
            return nameCmp;
        }
        // TODO compare sets properly
        return this.edition.compareTo(o.edition);
    }

    /**
     * Number of filters based on CardPrinted values.
     */
    public abstract static class Predicates {
        
        /**
         * Type.
         * @param t
         *      the type to search for
         * @return
         */
        public static Predicate<CardPrinted> type(final String t) {
            return new PredicateType(t);
        }

        /**
         * Rarity.
         * 
         * @param isEqual
         *            the is equal
         * @param value
         *            the value
         * @return the predicate
         */
        public static Predicate<CardPrinted> rarity(final boolean isEqual, final CardRarity value) {
            return new PredicateRarity(value, isEqual);
        }

        /**
         * Printed in sets.
         * 
         * @param value
         *            the value
         * @param shouldContain
         *            the should contain
         * @return the predicate
         */
        public static Predicate<CardPrinted> printedInSets(final List<String> value, final boolean shouldContain) {
            if ((value == null) || value.isEmpty()) {
                return com.google.common.base.Predicates.alwaysTrue();
            }
            return new PredicateSets(value, shouldContain);
        }

        /**
         * Printed in sets.
         * 
         * @param value
         *            the value
         * @return the predicate
         */
        public static Predicate<CardPrinted> printedInSets(final String value) {
            if ((value == null) || value.isEmpty()) {
                return com.google.common.base.Predicates.alwaysTrue();
            }
            return new PredicateSets(Arrays.asList(new String[] { value }), true);
        }

        /**
         * Name.
         * 
         * @param what
         *            the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> name(final String what) {
            return new PredicateName(PredicateString.StringOp.EQUALS_IC, what);
        }

        /**
         * Name.
         * 
         * @param op
         *            the op
         * @param what
         *            the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> name(final PredicateString.StringOp op, final String what) {
            return new PredicateName(op, what);
        }

        /**
         * Names except.
         * 
         * @param what
         *            the what
         * @return the predicate
         */
        public static Predicate<CardPrinted> namesExcept(final List<String> what) {
            return new PredicateNamesExcept(what);
        }

        private static class PredicateRarity implements Predicate<CardPrinted> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean apply(final CardPrinted card) {
                return card.rarity.equals(this.operand) == this.shouldBeEqual;
            }

            public PredicateRarity(final CardRarity type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateSets implements Predicate<CardPrinted> {
            private final List<String> sets;
            private final boolean mustContain;

            @Override
            public boolean apply(final CardPrinted card) {
                return this.sets.contains(card.edition) == this.mustContain;
            }

            public PredicateSets(final List<String> wantSets, final boolean shouldContain) {
                this.sets = wantSets; // maybe should make a copy here?
                this.mustContain = shouldContain;
            }
        }
        
        private static class PredicateType implements Predicate<CardPrinted> {
            private final String operand;
            
            @Override
            public boolean apply(final CardPrinted card) {
                return card.getType().contains(operand);
            }
            
            
            public PredicateType(final String op) {
                operand = op;
            }
            
        }

        private static class PredicateName extends PredicateString<CardPrinted> {
            private final String operand;

            @Override
            public boolean apply(final CardPrinted card) {
                return this.op(card.getName(), this.operand);
            }

            public PredicateName(final PredicateString.StringOp operator, final String operand) {
                super(operator);
                this.operand = operand;
            }
        }

        private static class PredicateNamesExcept extends PredicateString<CardPrinted> {
            private final List<String> operand;

            @Override
            public boolean apply(final CardPrinted card) {
                final String cardName = card.getName();
                for (final String element : this.operand) {
                    if (this.op(cardName, element)) {
                        return false;
                    }
                }
                return true;
            }

            public PredicateNamesExcept(final List<String> operand) {
                super(StringOp.EQUALS);
                this.operand = operand;
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
            public static final Predicate<CardPrinted> IS_COMMON = Predicates.rarity(true, CardRarity.Common);

            /** The Constant isUncommon. */
            public static final Predicate<CardPrinted> IS_UNCOMMON = Predicates.rarity(true, CardRarity.Uncommon);

            /** The Constant isRare. */
            public static final Predicate<CardPrinted> IS_RARE = Predicates.rarity(true, CardRarity.Rare);

            /** The Constant isMythicRare. */
            public static final Predicate<CardPrinted> IS_MYTHIC_RARE = Predicates.rarity(true, CardRarity.MythicRare);

            /** The Constant isRareOrMythic. */
            public static final Predicate<CardPrinted> IS_RARE_OR_MYTHIC = com.google.common.base.Predicates.or(Presets.IS_RARE,
                    Presets.IS_MYTHIC_RARE);

            /** The Constant isSpecial. */
            public static final Predicate<CardPrinted> IS_SPECIAL = Predicates.rarity(true, CardRarity.Special);

            /** The Constant exceptLands. */
            public static final Predicate<CardPrinted> EXCEPT_LANDS = Predicates.rarity(false, CardRarity.BasicLand);
            
            public static final Predicate<CardPrinted> NONTRADITIONAL = com.google.common.base.Predicates.or(Predicates.type("Vanguard"),Predicates.type("Scheme"),Predicates.type("Plane"));
            
            public static final Predicate<CardPrinted> TRADITIONAL = com.google.common.base.Predicates.not(Presets.NONTRADITIONAL);
        }
    }
}
