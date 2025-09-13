package forge.item;

import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.*;
import forge.card.CardEdition.EditionEntry;
import forge.util.PredicateString;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

    public static Predicate<PaperCard> printedWithRarity(final CardRarity rarity) {
        return new PredicatePrintedWithRarity(rarity);
    }

    public static Predicate<PaperCard> name(final String what) {
        return new PredicateName(what);
    }

    public static Predicate<PaperCard> names(final List<String> what) {
        return new PredicateNames(what);
    }

    /**
     * Filters on a card foil status
     */
    public static Predicate<PaperCard> isFoil(final boolean isFoil) {
        return new PredicateFoil(isFoil);
    }

    private static final class PredicatePrintedWithRarity implements Predicate<PaperCard> {
        private final CardRarity matchingRarity;

        @Override
        public boolean test(final PaperCard card) {
            return StaticData.instance().getEditions().stream()
                .anyMatch(ce -> {
                    List<EditionEntry> entries = ce.getCardInSet(card.getName());
                    return entries != null && entries.stream()
                        .anyMatch(ee -> ee.rarity() == matchingRarity);
                });
        }

        private PredicatePrintedWithRarity(final CardRarity rarity) {
            this.matchingRarity = rarity;
        }
    }

    private static final class PredicateColor implements Predicate<PaperCard> {
        private final MagicColor.Color operand;

        private PredicateColor(final MagicColor.Color color) {
            this.operand = color;
        }

        @Override
        public boolean test(final PaperCard card) {
            if (card.getRules().getColor().toEnumSet().contains(operand)) {
                return true;
            }
            if (card.getRules().getType().hasType(CardType.CoreType.Land)) {
                if (card.getRules().getColorIdentity().toEnumSet().contains(operand)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class PredicateFoil implements Predicate<PaperCard> {
        private final boolean operand;

        @Override
        public boolean test(final PaperCard card) { return card.isFoil() == operand; }

        private PredicateFoil(final boolean isFoil) {
            this.operand = isFoil;
        }
    }

    private static final class PredicateRarity implements Predicate<PaperCard> {
        private final CardRarity operand;

        @Override
        public boolean test(final PaperCard card) {
            return card.getRarity() == this.operand;
        }

        private PredicateRarity(final CardRarity rarity) {
            this.operand = rarity;
        }
    }

    public static final class PredicateRarities implements Predicate<PaperCard> {
        private final HashSet<CardRarity> operand;

        @Override
        public boolean test(final PaperCard card) {
            return this.operand.contains(card.getRarity());
        }

        public PredicateRarities(CardRarity... rarities) {
            this.operand = new HashSet<>(Arrays.asList(rarities));
        }
    }

    private static final class PredicateSets implements Predicate<PaperCard> {
        private final Set<String> sets;
        private final boolean mustContain;

        @Override
        public boolean test(final PaperCard card) {
            return this.sets.contains(card.getEdition()) == this.mustContain &&
                StaticData.instance().getCardEdition(card.getEdition()).isCardObtainable(card.getName());
        }

        private PredicateSets(final List<String> wantSets, final boolean shouldContain) {
            this.sets = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            this.sets.addAll(wantSets);
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
    public static final Predicate<PaperCard> IS_BLACK = new PredicateColor(MagicColor.Color.BLACK);
    public static final Predicate<PaperCard> IS_BLUE = new PredicateColor(MagicColor.Color.BLUE);
    public static final Predicate<PaperCard> IS_GREEN = new PredicateColor(MagicColor.Color.GREEN);
    public static final Predicate<PaperCard> IS_RED = new PredicateColor(MagicColor.Color.RED);
    public static final Predicate<PaperCard> IS_WHITE = new PredicateColor(MagicColor.Color.WHITE);
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
