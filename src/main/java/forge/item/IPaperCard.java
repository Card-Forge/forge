package forge.item;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;

import forge.Card;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.player.Player;
import forge.util.PredicateString;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IPaperCard {

    /**
     * Number of filters based on CardPrinted values.
     */
    public abstract static class Predicates {
    
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
                return (card.getRarity() == this.operand) == this.shouldBeEqual;
            }
    
            public PredicateRarity(final CardRarity type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }
    
        private static class PredicateSets implements Predicate<CardPrinted> {
            private final Set<String> sets;
            private final boolean mustContain;
    
            @Override
            public boolean apply(final CardPrinted card) {
                return this.sets.contains(card.getEdition()) == this.mustContain;
            }
    
            public PredicateSets(final List<String> wantSets, final boolean shouldContain) {
                this.sets = new HashSet<String>(wantSets);
                this.mustContain = shouldContain;
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
        }
    }


    public abstract String getName();
    public abstract String getEdition();
    public abstract int getArtIndex();
    public abstract boolean isFoil();
    public abstract boolean isToken();
    public abstract CardRules getRules();
    public abstract CardRarity getRarity();
    public abstract String getImageFilename();

    public abstract String getItemType();

    public abstract Card getMatchingForgeCard();
    public abstract Card toForgeCard(Player owner);

}