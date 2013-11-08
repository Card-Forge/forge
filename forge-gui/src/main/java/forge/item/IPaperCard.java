package forge.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import forge.Card;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.player.Player;
import forge.util.PredicateString;

public interface IPaperCard extends InventoryItem {

    /**
     * Number of filters based on CardPrinted values.
     */
    public abstract static class Predicates {
    
        public static Predicate<PaperCard> rarity(final boolean isEqual, final CardRarity value) {
            return new PredicateRarity(value, isEqual);
        }
        public static Predicate<PaperCard> printedInSets(final String[] sets) {
            return printedInSets(Lists.newArrayList(sets), true);
        }
    
        public static Predicate<PaperCard> printedInSets(final List<String> value, final boolean shouldContain) {
            if ((value == null) || value.isEmpty()) {
                return com.google.common.base.Predicates.alwaysTrue();
            }
            return new PredicateSets(value, shouldContain);
        }
    
        public static Predicate<PaperCard> printedInSet(final String value) {
            if (StringUtils.isEmpty(value)) {
                return com.google.common.base.Predicates.alwaysTrue();
            }
            return new PredicateSets(Lists.newArrayList(value), true);
        }
    
        public static Predicate<PaperCard> name(final String what) {
            return new PredicateName(PredicateString.StringOp.EQUALS_IC, what);
        }
    
        public static Predicate<PaperCard> name(final PredicateString.StringOp op, final String what) {
            return new PredicateName(op, what);
        }
    
        public static Predicate<PaperCard> names(final List<String> what) {
            return new PredicateNames(what);
        }
    
        private static class PredicateRarity implements Predicate<PaperCard> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;
    
            @Override
            public boolean apply(final PaperCard card) {
                return (card.getRarity() == this.operand) == this.shouldBeEqual;
            }
    
            public PredicateRarity(final CardRarity type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }
    
        private static class PredicateSets implements Predicate<PaperCard> {
            private final Set<String> sets;
            private final boolean mustContain;
    
            @Override
            public boolean apply(final PaperCard card) {
                return this.sets.contains(card.getEdition()) == this.mustContain;
            }
    
            public PredicateSets(final List<String> wantSets, final boolean shouldContain) {
                this.sets = new HashSet<String>(wantSets);
                this.mustContain = shouldContain;
            }
        }
    
        private static class PredicateName extends PredicateString<PaperCard> {
            private final String operand;
    
            @Override
            public boolean apply(final PaperCard card) {
                return this.op(card.getName(), this.operand);
            }
    
            public PredicateName(final PredicateString.StringOp operator, final String operand) {
                super(operator);
                this.operand = operand;
            }
        }
    
        private static class PredicateNames extends PredicateString<PaperCard> {
            private final List<String> operand;
    
            @Override
            public boolean apply(final PaperCard card) {
                final String cardName = card.getName();
                for (final String element : this.operand) {
                    if (this.op(cardName, element)) {
                        return true;
                    }
                }
                return false;
            }
    
            public PredicateNames(final List<String> operand) {
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
            public static final Predicate<PaperCard> IS_COMMON = Predicates.rarity(true, CardRarity.Common);
    
            /** The Constant isUncommon. */
            public static final Predicate<PaperCard> IS_UNCOMMON = Predicates.rarity(true, CardRarity.Uncommon);
    
            /** The Constant isRare. */
            public static final Predicate<PaperCard> IS_RARE = Predicates.rarity(true, CardRarity.Rare);
    
            /** The Constant isMythicRare. */
            public static final Predicate<PaperCard> IS_MYTHIC_RARE = Predicates.rarity(true, CardRarity.MythicRare);
    
            /** The Constant isRareOrMythic. */
            public static final Predicate<PaperCard> IS_RARE_OR_MYTHIC = com.google.common.base.Predicates.or(Presets.IS_RARE,
                    Presets.IS_MYTHIC_RARE);
    
            /** The Constant isSpecial. */
            public static final Predicate<PaperCard> IS_SPECIAL = Predicates.rarity(true, CardRarity.Special);
    
            /** The Constant exceptLands. */
            public static final Predicate<PaperCard> IS_BASIC_LAND = Predicates.rarity(true, CardRarity.BasicLand);
        }
    }


    public abstract String getName();
    public abstract String getEdition();
    public abstract int getArtIndex();
    public abstract boolean isFoil();
    public abstract boolean isToken();
    public abstract CardRules getRules();
    public abstract CardRarity getRarity();

    public abstract String getItemType();

    public abstract Card getMatchingForgeCard();
    public abstract Card toForgeCard(Player owner);

}