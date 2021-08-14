package forge.item;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType.CoreType;
import forge.card.MagicColor;
import forge.util.PredicateCard;
import forge.util.PredicateString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IPaperCard extends InventoryItem, Serializable {

    String NO_COLLECTOR_NUMBER = "N.A.";  // Placeholder for No-Collection number available
    int DEFAULT_ART_INDEX = 1;
    int NO_ART_INDEX = -1;  // Placeholder when NO ArtIndex is Specified

    /**
     * Number of filters based on CardPrinted values.
     */
    abstract class Predicates {

        public static Predicate<PaperCard> rarity(final boolean isEqual, final CardRarity value) {
            return new PredicateRarity(value, isEqual);
        }

        public static Predicate<PaperCard> color(final boolean isEqual, final boolean noColor, final byte value) {
            return new PredicateColor(value, noColor, isEqual);
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

        public static PredicateCards cards(final List<PaperCard> what) { return new PredicateCards(what); }

        private static final class PredicateColor implements Predicate<PaperCard> {

            private final byte operand;
            private final boolean noColor;
            private final boolean shouldBeEqual;

            private PredicateColor(final byte color, final boolean noColor, final boolean wantEqual) {
                this.operand = color;
                this.noColor = noColor;
                this.shouldBeEqual = wantEqual;
            }

            @Override
            public boolean apply(final PaperCard card) {
                boolean colorFound = false;
                if (noColor) {
                    return card.getRules().getColor().isColorless() == shouldBeEqual;
                }
                for (final byte color : card.getRules().getColor()) {
                    if (color == operand) {
                        colorFound = true;
                        break;
                    }
                }
                if (card.getRules().getType().hasType(CoreType.Land)) {
                    for (final byte color : card.getRules().getColorIdentity()) {
                        if (color == operand) {
                            colorFound = true;
                            break;
                        }
                    }
                }
                return colorFound == shouldBeEqual;
            }

        }

        private static final class PredicateRarity implements Predicate<PaperCard> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean apply(final PaperCard card) {
                return (card.getRarity() == this.operand) == this.shouldBeEqual;
            }

            private PredicateRarity(final CardRarity type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }

        private static final class PredicateSets implements Predicate<PaperCard> {
            private final Set<String> sets;
            private final boolean mustContain;

            @Override
            public boolean apply(final PaperCard card) {
                return this.sets.contains(card.getEdition()) == this.mustContain;
            }

            private PredicateSets(final List<String> wantSets, final boolean shouldContain) {
                this.sets = new HashSet<>(wantSets);
                this.mustContain = shouldContain;
            }
        }

        private static final class PredicateName extends PredicateString<PaperCard> {
            private final String operand;

            @Override
            public boolean apply(final PaperCard card) {
                return this.op(card.getName(), this.operand);
            }

            private PredicateName(final PredicateString.StringOp operator, final String operand) {
                super(operator);
                this.operand = operand;
            }
        }

        private static final class PredicateNames extends PredicateString<PaperCard> {
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

            private PredicateNames(final List<String> operand) {
                super(StringOp.EQUALS);
                this.operand = operand;
            }
        }

        private static final class PredicateCards extends PredicateCard<PaperCard> {
            private final List<PaperCard> operand;

            @Override
            public boolean apply(final PaperCard card) {
                for (final PaperCard element : this.operand) {
                    if (this.op(card, element)) {
                        return true;
                    }
                }
                return false;
            }

            private PredicateCards(final List<PaperCard> operand) {
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

            public static final Predicate<PaperCard> IS_BLACK = Predicates.color(true, false, MagicColor.BLACK);
            public static final Predicate<PaperCard> IS_BLUE = Predicates.color(true, false, MagicColor.BLUE);
            public static final Predicate<PaperCard> IS_GREEN = Predicates.color(true, false, MagicColor.GREEN);
            public static final Predicate<PaperCard> IS_RED = Predicates.color(true, false, MagicColor.RED);
            public static final Predicate<PaperCard> IS_WHITE = Predicates.color(true, false, MagicColor.WHITE);
            public static final Predicate<PaperCard> IS_COLORLESS = Predicates.color(true, true, MagicColor.COLORLESS);

        }
    }


    String getName();
    String getEdition();
    String getCollectorNumber();
    int getArtIndex();
    boolean isFoil();
    boolean isToken();
    CardRules getRules();
    CardRarity getRarity();
    String getArtist();

    String getItemType();

}