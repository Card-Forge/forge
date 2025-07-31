package forge.itemmanager.advancedsearchparsers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import forge.card.CardRarity;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.ComparableOp;

public abstract class RarityParser {
    private static final Map<CardRarity, Integer> RARITY_RANK;
    static {
        RARITY_RANK = new HashMap<>();
        RARITY_RANK.put(CardRarity.BasicLand, 0);
        RARITY_RANK.put(CardRarity.Common, 1);
        RARITY_RANK.put(CardRarity.Uncommon, 2);
        RARITY_RANK.put(CardRarity.Rare, 3);
        RARITY_RANK.put(CardRarity.MythicRare, 4);
    }

    /**
     * Handles exact rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleExact(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        CardRarity rarity = ParseRarityFromStr(tokenValue);

        if (rarity == null) {
            return null;
        }

        if (rarity.equals(CardRarity.Special)) {
            predicate = PaperCardPredicates.IS_SPECIAL;
        } else {
            predicate = new PredicateRarityCheck(ComparableOp.EQUALS, rarity);
        }
        
        return predicate;
    }

    /**
     * Handles greater than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleGreater(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        CardRarity rarity = ParseRarityFromStr(tokenValue);

        if (rarity == null || rarity.equals(CardRarity.Special)) {
            return null;
        }

        if (rarity.equals(CardRarity.MythicRare)) {
            predicate = x -> false;
        } else {
            predicate = new PredicateRarityCheck(ComparableOp.GREATER_THAN, rarity);
        }
        
        return predicate;
    }

    /**
     * Handles greater or equal than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleGreaterOrEqual(String tokenValue) {
        CardRarity rarity = ParseRarityFromStr(tokenValue);

        if (rarity == null || rarity.equals(CardRarity.Special)) {
            return null;
        }
        
        return new PredicateRarityCheck(ComparableOp.GT_OR_EQUAL, rarity);
    }

    /**
     * Handles less than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleLess(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        CardRarity rarity = ParseRarityFromStr(tokenValue);

        if (rarity == null || rarity.equals(CardRarity.Special)) {
            return null;
        }

        if (rarity.equals(CardRarity.BasicLand)) {
            predicate = x -> false;
        } else {
            predicate = new PredicateRarityCheck(ComparableOp.LESS_THAN, rarity);
        }
        
        return predicate;
    }

    /**
     * Handles less or equal than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleLessOrEqual(String tokenValue) {
        CardRarity rarity = ParseRarityFromStr(tokenValue);

        if (rarity == null || rarity.equals(CardRarity.Special)) {
            return null;
        }

        return new PredicateRarityCheck(ComparableOp.LT_OR_EQUAL, rarity);
    }

    public static CardRarity ParseRarityFromStr(String input) {
        CardRarity value = null;

        switch(input) {
            case "l":
            case "land":
                value = CardRarity.BasicLand;
                break;

            case "c":
            case "common":
                value = CardRarity.Common;
                break;

            case "u":
            case "uncommon":
                value = CardRarity.Uncommon;
                break;

            case "r":
            case "rare":
                value = CardRarity.Rare;
                break;

            case "m":
            case "mythic":
                value = CardRarity.MythicRare;
                break;

            case "s":
            case "special":
                value = CardRarity.Special;
                break;
        }

        return value;
    }

    private static final class PredicateRarityCheck implements Predicate<PaperCard> {
        private final ComparableOp op;
        private final CardRarity rarity;

        @Override
        public boolean test(final PaperCard card) {
            return doCheck(RARITY_RANK.get(card.getRarity()), RARITY_RANK.get(rarity));
        }

        private PredicateRarityCheck(final ComparableOp op, final CardRarity rarity) {
            this.op = op;
            this.rarity = rarity;
            
        }

        private boolean doCheck(final Integer op1, final Integer op2) {
            if (op1 == null || op2 == null) {
                return false;
            }

            switch (this.op) {
                case EQUALS:
                    return op1 == op2;
                case GREATER_THAN:
                    return op1 > op2;
                case GT_OR_EQUAL:
                    return op1 >= op2;
                case LESS_THAN:
                    return op1 < op2;
                case LT_OR_EQUAL:
                    return op1 <= op2;
                case NOT_EQUALS:
                    return op1 != op2;
                default:
                    return false;
                }
        }
    }
}
