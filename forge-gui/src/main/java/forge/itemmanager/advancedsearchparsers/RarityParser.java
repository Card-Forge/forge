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
        RARITY_RANK.put(CardRarity.Common, 0);
        RARITY_RANK.put(CardRarity.Uncommon, 1);
        RARITY_RANK.put(CardRarity.Rare, 2);
        RARITY_RANK.put(CardRarity.MythicRare, 3);
    }

    /**
     * Handles exact rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleExact(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        switch(tokenValue) {
            case "c":
            case "common":
                predicate = new PredicateRarityCheck(ComparableOp.EQUALS, CardRarity.Common);
                break;

            case "u":
            case "uncommon":
                predicate = new PredicateRarityCheck(ComparableOp.EQUALS, CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = new PredicateRarityCheck(ComparableOp.EQUALS, CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = new PredicateRarityCheck(ComparableOp.EQUALS, CardRarity.MythicRare);
                break;

            case "s":
            case "special":
                predicate = PaperCardPredicates.IS_SPECIAL;
                break;
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
        switch(tokenValue) {
            case "c":
            case "common":
                predicate = new PredicateRarityCheck(ComparableOp.GREATER_THAN, CardRarity.Common);
                break;

            case "u":
            case "uncommon":
                predicate = new PredicateRarityCheck(ComparableOp.GREATER_THAN, CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = new PredicateRarityCheck(ComparableOp.GREATER_THAN, CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = x -> false;
                break;
        }
        
        return predicate;
    }

    /**
     * Handles greater or equal than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleGreaterOrEqual(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        switch(tokenValue) {
            case "c":
            case "common":
                predicate = new PredicateRarityCheck(ComparableOp.GT_OR_EQUAL, CardRarity.Common);
                break;

            case "u":
            case "uncommon":
                predicate = new PredicateRarityCheck(ComparableOp.GT_OR_EQUAL, CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = new PredicateRarityCheck(ComparableOp.GT_OR_EQUAL, CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = new PredicateRarityCheck(ComparableOp.GT_OR_EQUAL, CardRarity.MythicRare);
                break;
        }
        
        return predicate;
    }

    /**
     * Handles less than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleLess(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        switch(tokenValue) {
            case "c":
            case "common":
                predicate = x -> false;
                break;

            case "u":
            case "uncommon":
                predicate = new PredicateRarityCheck(ComparableOp.LESS_THAN, CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = new PredicateRarityCheck(ComparableOp.LESS_THAN, CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = new PredicateRarityCheck(ComparableOp.LESS_THAN, CardRarity.MythicRare);
                break;
        }
        
        return predicate;
    }

    /**
     * Handles less or equal than rarity check of a card
     * @param tokenValue Token value
     * @return Predicate or null
     */
    public static Predicate<PaperCard> handleLessOrEqual(String tokenValue) {
        Predicate<PaperCard> predicate = null;
        switch(tokenValue) {
            case "c":
            case "common":
                predicate = new PredicateRarityCheck(ComparableOp.LT_OR_EQUAL, CardRarity.Common);
                break;

            case "u":
            case "uncommon":
                predicate = new PredicateRarityCheck(ComparableOp.LT_OR_EQUAL, CardRarity.Uncommon);
                break;

            case "r":
            case "rare":
                predicate = new PredicateRarityCheck(ComparableOp.LT_OR_EQUAL, CardRarity.Rare);
                break;

            case "m":
            case "mythic":
                predicate = new PredicateRarityCheck(ComparableOp.LT_OR_EQUAL, CardRarity.MythicRare);
                break;
        }
        
        return predicate;
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
