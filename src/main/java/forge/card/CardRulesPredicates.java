package forge.card;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.util.ComparableOp;
import forge.util.PredicateString;

/**
 * Filtering conditions specific for CardRules class, defined here along with
 * some presets.
 */
public final class CardRulesPredicates {

    /** The Constant isKeptInAiDecks. */
    public static final Predicate<CardRules> IS_KEPT_IN_AI_DECKS = new Predicate<CardRules>() {
        @Override
        public boolean apply(final CardRules card) {
            return !card.isRemovedFromAIDecks;
        }
    };

    /** The Constant isKeptInRandomDecks. */
    public static final Predicate<CardRules> IS_KEPT_IN_RANDOM_DECKS = new Predicate<CardRules>() {
        @Override
        public boolean apply(final CardRules card) {
            return !card.isRemovedFromRandomDecks;
        }
    };

    // Static builder methods - they choose concrete implementation by
    // themselves
    /**
     * Cmc.
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> cmc(final ComparableOp op, final int what) {
        return new LeafNumber(LeafNumber.CardField.CMC, op, what);
    }

    /**
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> power(final ComparableOp op, final int what) {
        return new LeafNumber(LeafNumber.CardField.POWER, op, what);
    }

    /**
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> toughness(final ComparableOp op, final int what) {
        return new LeafNumber(LeafNumber.CardField.TOUGHNESS, op, what);
    }

    // P/T
    /**
     * Rules.
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> rules(final PredicateString.StringOp op, final String what) {
        return new LeafString(LeafString.CardField.RULES, op, what);
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
    public static Predicate<CardRules> name(final PredicateString.StringOp op, final String what) {
        return new LeafString(LeafString.CardField.NAME, op, what);
    }

    /**
     * Sub type.
     * 
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> subType(final String what) {
        return new LeafString(LeafString.CardField.SUBTYPE, PredicateString.StringOp.CONTAINS, what);
    }

    /**
     * Sub type.
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> subType(final PredicateString.StringOp op, final String what) {
        return new LeafString(LeafString.CardField.SUBTYPE, op, what);
    }

    /**
     * Joined type.
     * 
     * @param op
     *            the op
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> joinedType(final PredicateString.StringOp op, final String what) {
        return new LeafString(LeafString.CardField.JOINED_TYPE, op, what);
    }

    /**
     * Has Keyword.
     * 
     * @param keyword
     *            the keyword
     * @return the predicate
     */
    public static Predicate<CardRules> hasKeyword(final String keyword) {
        return new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules card) {
                return card.getKeywords().contains(keyword);
            }
        };
    }

    /**
     * Was printed in sets.
     * 
     * @param setCodes
     *            the set codes
     * @return the predicate
     */
    public static Predicate<CardRules> wasPrintedInSets(final List<String> setCodes) {
        return new PredicateExistsInSets(setCodes);
    }

    /**
     * Core type.
     * 
     * @param isEqual
     *            the is equal
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> coreType(final boolean isEqual, final String what) {
        try {
            return CardRulesPredicates.coreType(isEqual, Enum.valueOf(CardCoreType.class, what));
        } catch (final Exception e) {
            return com.google.common.base.Predicates.alwaysFalse();
        }
    }

    /**
     * Core type.
     * 
     * @param isEqual
     *            the is equal
     * @param type
     *            the type
     * @return the predicate
     */
    public static Predicate<CardRules> coreType(final boolean isEqual, final CardCoreType type) {
        return new PredicateCoreType(type, isEqual);
    }

    /**
     * Super type.
     * 
     * @param isEqual
     *            the is equal
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> superType(final boolean isEqual, final String what) {
        try {
            return CardRulesPredicates.superType(isEqual, Enum.valueOf(CardSuperType.class, what));
        } catch (final Exception e) {
            return com.google.common.base.Predicates.alwaysFalse();
        }
    }

    /**
     * Super type.
     * 
     * @param isEqual
     *            the is equal
     * @param type
     *            the type
     * @return the predicate
     */
    public static Predicate<CardRules> superType(final boolean isEqual, final CardSuperType type) {
        return new PredicateSuperType(type, isEqual);
    }

    /**
     * Rarity in cards latest set.
     * 
     * @param isEqual
     *            the is equal
     * @param value
     *            the value
     * @return the predicate
     */
    public static Predicate<CardRules> rarityInCardsLatestSet(final boolean isEqual, final CardRarity value) {
        return new PredicateLastesSetRarity(value, isEqual);
    }

    /**
     * Checks for color.
     * 
     * @param thatColor
     *            the that color
     * @return the predicate
     */
    public static Predicate<CardRules> hasColor(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.HasAllOf, thatColor);
    }

    /**
     * Checks if is color.
     * 
     * @param thatColor
     *            the that color
     * @return the predicate
     */
    public static Predicate<CardRules> isColor(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.HasAnyOf, thatColor);
    }

    /**
     * Checks if is exactly that color.
     * 
     * @param thatColor
     *            color to check
     * @return the predicate
     */
    public static Predicate<CardRules> isMonoColor(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.Equals, thatColor);
    }

    /**
     * Checks for cnt colors.
     * 
     * @param cntColors
     *            the cnt colors
     * @return the predicate
     */
    public static Predicate<CardRules> hasCntColors(final byte cntColors) {
        return new LeafColor(LeafColor.ColorOperator.CountColors, cntColors);
    }

    /**
     * Checks for at least cnt colors.
     * 
     * @param cntColors
     *            the cnt colors
     * @return the predicate
     */
    public static Predicate<CardRules> hasAtLeastCntColors(final byte cntColors) {
        return new LeafColor(LeafColor.ColorOperator.CountColorsGreaterOrEqual, cntColors);
    }

    private static class LeafString extends PredicateString<CardRules> {
        public enum CardField {
            RULES, NAME, SUBTYPE, JOINED_TYPE
        }

        private final String operand;
        private final LeafString.CardField field;

        @Override
        public boolean apply(final CardRules card) {
            boolean shouldContain;
            switch (this.field) {
            case NAME:
                return this.op(card.getName(), this.operand);
            case SUBTYPE:
                shouldContain = (this.getOperator() == StringOp.CONTAINS) || (this.getOperator() == StringOp.EQUALS);
                return shouldContain == card.getType().subTypeContains(this.operand);
            case RULES:
                shouldContain = (this.getOperator() == StringOp.CONTAINS) || (this.getOperator() == StringOp.EQUALS);
                return shouldContain == card.rulesContain(this.operand);
            case JOINED_TYPE:
                return this.op(card.getType().toString(), this.operand);
            default:
                return false;
            }
        }

        public LeafString(final LeafString.CardField field, final StringOp operator, final String operand) {
            super(operator);
            this.field = field;
            this.operand = operand;
        }
    }

    private static class LeafColor implements Predicate<CardRules> {
        public enum ColorOperator {
            CountColors, CountColorsGreaterOrEqual, HasAnyOf, HasAllOf, Equals
        }

        private final LeafColor.ColorOperator op;
        private final byte color;

        public LeafColor(final LeafColor.ColorOperator operator, final byte thatColor) {
            this.op = operator;
            this.color = thatColor;
        }

        @Override
        public boolean apply(final CardRules subject) {
            switch (this.op) {
            case CountColors:
                return subject.getColor().countColors() == this.color;
            case CountColorsGreaterOrEqual:
                return subject.getColor().countColors() >= this.color;
            case Equals:
                return subject.getColor().isEqual(this.color);
            case HasAllOf:
                return subject.getColor().hasAllColors(this.color);
            case HasAnyOf:
                return subject.getColor().hasAnyColor(this.color);
            default:
                return false;
            }
        }
    }

    public static class LeafNumber implements Predicate<CardRules> {
        public enum CardField {
            CMC, POWER, TOUGHNESS,
        }

        private final LeafNumber.CardField field;
        private final ComparableOp operator;
        private final int operand;

        public LeafNumber(final LeafNumber.CardField field, final ComparableOp op, final int what) {
            this.field = field;
            this.operand = what;
            this.operator = op;
        }

        @Override
        public boolean apply(final CardRules card) {
            int value;
            switch (this.field) {
            case CMC:
                return this.op(card.getManaCost().getCMC(), this.operand);
            case POWER:
                value = card.getIntPower();
                return value >= 0 ? this.op(value, this.operand) : false;
            case TOUGHNESS:
                value = card.getIntToughness();
                return value >= 0 ? this.op(value, this.operand) : false;
            default:
                return false;
            }
        }

        private boolean op(final int op1, final int op2) {
            switch (this.operator) {
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

    private static class PredicateCoreType implements Predicate<CardRules> {
        private final CardCoreType operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final CardRules card) {
            return this.shouldBeEqual == card.getType().typeContains(this.operand);
        }

        public PredicateCoreType(final CardCoreType type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateSuperType implements Predicate<CardRules> {
        private final CardSuperType operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final CardRules card) {
            return this.shouldBeEqual == card.getType().superTypeContains(this.operand);
        }

        public PredicateSuperType(final CardSuperType type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateLastesSetRarity implements Predicate<CardRules> {
        private final CardRarity operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final CardRules card) {
            return card.getRarityFromLatestSet().equals(this.operand) == this.shouldBeEqual;
        }

        public PredicateLastesSetRarity(final CardRarity type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateExistsInSets implements Predicate<CardRules> {
        private final List<String> sets;

        public PredicateExistsInSets(final List<String> wantSets) {
            this.sets = wantSets; // maybe should make a copy here?
        }

        @Override
        public boolean apply(final CardRules subject) {
            for (final String s : this.sets) {
                if (subject.setsPrinted.containsKey(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The Class Presets.
     */
    public static class Presets {

        /** The Constant isCreature. */
        public static final Predicate<CardRules> IS_CREATURE = CardRulesPredicates
                .coreType(true, CardCoreType.Creature);

        /** The Constant isArtifact. */
        public static final Predicate<CardRules> IS_ARTIFACT = CardRulesPredicates
                .coreType(true, CardCoreType.Artifact);

        /** The Constant isLand. */
        public static final Predicate<CardRules> IS_LAND = CardRulesPredicates.coreType(true, CardCoreType.Land);

        /** The Constant isBasicLand. */
        public static final Predicate<CardRules> IS_BASIC_LAND = new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules subject) {
                return subject.getType().isBasicLand();
            }
        };

        /** The Constant isNonBasicLand. */
        public static final Predicate<CardRules> IS_NONBASIC_LAND = new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules subject) {
                return subject.getType().isLand() && !subject.getType().isBasicLand();
            }
        };

        /** The Constant isPlaneswalker. */
        public static final Predicate<CardRules> IS_PLANESWALKER = CardRulesPredicates.coreType(true,
                CardCoreType.Planeswalker);

        /** The Constant isInstant. */
        public static final Predicate<CardRules> IS_INSTANT = CardRulesPredicates.coreType(true, CardCoreType.Instant);

        /** The Constant isSorcery. */
        public static final Predicate<CardRules> IS_SORCERY = CardRulesPredicates.coreType(true, CardCoreType.Sorcery);

        /** The Constant isEnchantment. */
        public static final Predicate<CardRules> IS_ENCHANTMENT = CardRulesPredicates.coreType(true,
                CardCoreType.Enchantment);

        /** The Constant isNonLand. */
        public static final Predicate<CardRules> IS_NON_LAND = CardRulesPredicates.coreType(false, CardCoreType.Land);

        /** The Constant isNonCreatureSpell. */
        public static final Predicate<CardRules> IS_NON_CREATURE_SPELL = Predicates.not(Predicates.or(
                Presets.IS_CREATURE, Presets.IS_LAND));

        /** The Constant IS_NONCREATURE_SPELL_FOR_GENERATOR. **/
        @SuppressWarnings("unchecked")
        public static final Predicate<CardRules> IS_NONCREATURE_SPELL_FOR_GENERATOR = com.google.common.base.Predicates
                .or(Presets.IS_SORCERY, Presets.IS_INSTANT, Presets.IS_PLANESWALKER, Presets.IS_ENCHANTMENT,
                        Predicates.and(Presets.IS_ARTIFACT, Predicates.not(Presets.IS_CREATURE)));

        /** The Constant isWhite. */
        public static final Predicate<CardRules> IS_WHITE = CardRulesPredicates.isColor(CardColor.WHITE);

        /** The Constant isBlue. */
        public static final Predicate<CardRules> IS_BLUE = CardRulesPredicates.isColor(CardColor.BLUE);

        /** The Constant isBlack. */
        public static final Predicate<CardRules> IS_BLACK = CardRulesPredicates.isColor(CardColor.BLACK);

        /** The Constant isRed. */
        public static final Predicate<CardRules> IS_RED = CardRulesPredicates.isColor(CardColor.RED);

        /** The Constant isGreen. */
        public static final Predicate<CardRules> IS_GREEN = CardRulesPredicates.isColor(CardColor.GREEN);

        /** The Constant isColorless. */
        public static final Predicate<CardRules> IS_COLORLESS = CardRulesPredicates.hasCntColors((byte) 0);

        /** The Constant isMulticolor. */
        public static final Predicate<CardRules> IS_MULTICOLOR = CardRulesPredicates.hasAtLeastCntColors((byte) 2);

        /** The Constant colors. */
        public static final List<Predicate<CardRules>> COLORS = new ArrayList<Predicate<CardRules>>();
        static {
            Presets.COLORS.add(Presets.IS_WHITE);
            Presets.COLORS.add(Presets.IS_BLUE);
            Presets.COLORS.add(Presets.IS_BLACK);
            Presets.COLORS.add(Presets.IS_RED);
            Presets.COLORS.add(Presets.IS_GREEN);
            Presets.COLORS.add(Presets.IS_COLORLESS);
        }

        // Think twice before using these, since rarity is a prop of printed
        // card.
        /** The Constant isInLatestSetCommon. */
        public static final Predicate<CardRules> IS_IN_LATEST_SET_COMMON = CardRulesPredicates.rarityInCardsLatestSet(
                true, CardRarity.Common);

        /** The Constant isInLatestSetUncommon. */
        public static final Predicate<CardRules> IS_IN_LATEST_SET_UNCOMMON = CardRulesPredicates
                .rarityInCardsLatestSet(true, CardRarity.Uncommon);

        /** The Constant isInLatestSetRare. */
        public static final Predicate<CardRules> IS_IN_LATEST_SET_RARE = CardRulesPredicates.rarityInCardsLatestSet(
                true, CardRarity.Rare);

        /** The Constant isInLatestSetMythicRare. */
        public static final Predicate<CardRules> IS_IN_LATEST_SET_MYTHIC_RARE = CardRulesPredicates
                .rarityInCardsLatestSet(true, CardRarity.MythicRare);

        /** The Constant isInLatestSetSpecial. */
        public static final Predicate<CardRules> IS_IN_LATEST_SET_SPECIAL = CardRulesPredicates.rarityInCardsLatestSet(
                true, CardRarity.Special);
    }
}