package forge.item;

import com.google.common.collect.Lists;
import forge.card.*;
import forge.util.PredicateString;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Filters based on PaperCard values.
 */
public abstract class PaperCardPredicates {

    public static Predicate<PaperCard> printedInSets(final String[] sets) {
        return printedInSets(Lists.newArrayList(sets), true);
    }

    public static Predicate<PaperCard> printedInSets(final List<String> value, final boolean shouldContain) {
        if ((value == null) || value.isEmpty()) {
            return x -> true;
        }
        return new PredicateSets(value, shouldContain);
    }

    public static Predicate<PaperCard> printedInSet(final String value) {
        if (StringUtils.isEmpty(value)) {
            return x -> true;
        }
        return new PredicateSets(Lists.newArrayList(value), true);
    }

    public static Predicate<PaperCard> name(final String what) {
        return new PredicateName(what);
    }

    public static Predicate<PaperCard> names(final List<String> what) {
        return new PredicateNames(what);
    }

    private static final class PredicateColor implements Predicate<PaperCard> {

        private final byte operand;

        private PredicateColor(final byte color) {
            this.operand = color;
        }

        @Override
        public boolean test(final PaperCard card) {
            for (final byte color : card.getRules().getColor()) {
                if (color == operand) {
                    return true;
                }
            }
            if (card.getRules().getType().hasType(CardType.CoreType.Land)) {
                for (final byte color : card.getRules().getColorIdentity()) {
                    if (color == operand) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    private static final class PredicateRarity implements Predicate<PaperCard> {
        private final CardRarity operand;

        @Override
        public boolean test(final PaperCard card) {
            return (card.getRarity() == this.operand);
        }

        private PredicateRarity(final CardRarity type) {
            this.operand = type;
        }
    }

    private static final class PredicateSets implements Predicate<PaperCard> {
        private final Set<String> sets;
        private final boolean mustContain;

        @Override
        public boolean test(final PaperCard card) {
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
        public boolean test(final PaperCard card) {
            return this.op(card.getName(), this.operand);
        }

        private PredicateName(final String operand) {
            super(StringOp.EQUALS_IC);
            this.operand = operand;
        }
    }

    private static final class PredicateNames extends PredicateString<PaperCard> {
        private final List<String> operand;

        @Override
        public boolean test(final PaperCard card) {
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

    public static Predicate<PaperCard> fromRules(Predicate<CardRules> cardRulesPredicate) {
        return paperCard -> cardRulesPredicate.test(paperCard.getRules());
    }

    public static final Predicate<PaperCard> IS_COMMON = new PredicateRarity(CardRarity.Common);
    public static final Predicate<PaperCard> IS_UNCOMMON = new PredicateRarity(CardRarity.Uncommon);
    public static final Predicate<PaperCard> IS_RARE = new PredicateRarity(CardRarity.Rare);
    public static final Predicate<PaperCard> IS_MYTHIC_RARE = new PredicateRarity(CardRarity.MythicRare);
    public static final Predicate<PaperCard> IS_RARE_OR_MYTHIC = PaperCardPredicates.IS_RARE.or(PaperCardPredicates.IS_MYTHIC_RARE);
    public static final Predicate<PaperCard> IS_SPECIAL = new PredicateRarity(CardRarity.Special);
    public static final Predicate<PaperCard> IS_BASIC_LAND_RARITY = new PredicateRarity(CardRarity.BasicLand);
    public static final Predicate<PaperCard> IS_BLACK = new PredicateColor(MagicColor.BLACK);
    public static final Predicate<PaperCard> IS_BLUE = new PredicateColor(MagicColor.BLUE);
    public static final Predicate<PaperCard> IS_GREEN = new PredicateColor(MagicColor.GREEN);
    public static final Predicate<PaperCard> IS_RED = new PredicateColor(MagicColor.RED);
    public static final Predicate<PaperCard> IS_WHITE = new PredicateColor(MagicColor.WHITE);
    public static final Predicate<PaperCard> IS_COLORLESS = paperCard -> paperCard.getRules().getColor().isColorless();
    public static final Predicate<PaperCard> IS_UNREBALANCED = PaperCard::isUnRebalanced;
    public static final Predicate<PaperCard> IS_REBALANCED = PaperCard::isRebalanced;

    //Common rules-based predicates.
    public static final Predicate<PaperCard> IS_LAND = fromRules(CardRulesPredicates.IS_LAND);
    public static final Predicate<PaperCard> IS_NON_LAND = fromRules(CardRulesPredicates.IS_NON_LAND);
    public static final Predicate<PaperCard> IS_BASIC_LAND = fromRules(CardRulesPredicates.IS_BASIC_LAND);
    /** Matches any card except Plains, Island, Swamp, Mountain, Forest, or Wastes. */
    public static final Predicate<PaperCard> NOT_BASIC_LAND = fromRules(CardRulesPredicates.NOT_BASIC_LAND);
    /** Matches any card except Plains, Island, Swamp, Mountain, or Forest. */
    public static final Predicate<PaperCard> NOT_TRUE_BASIC_LAND = fromRules(CardRulesPredicates.NOT_TRUE_BASIC_LAND);
    public static final Predicate<PaperCard> IS_NONBASIC_LAND = fromRules(CardRulesPredicates.IS_NONBASIC_LAND);
    public static final Predicate<PaperCard> IS_CREATURE = fromRules(CardRulesPredicates.IS_CREATURE);
    public static final Predicate<PaperCard> CAN_BE_COMMANDER = fromRules(CardRulesPredicates.CAN_BE_COMMANDER);
}
