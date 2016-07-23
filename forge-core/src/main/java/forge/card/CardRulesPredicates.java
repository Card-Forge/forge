package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.util.ComparableOp;
import forge.util.PredicateString;
import forge.util.PredicateString.StringOp;

/**
 * Filtering conditions specific for CardRules class, defined here along with
 * some presets.
 */
public final class CardRulesPredicates {

    /** The Constant isKeptInAiDecks. */
    public static final Predicate<CardRules> IS_KEPT_IN_AI_DECKS = new Predicate<CardRules>() {
        @Override
        public boolean apply(final CardRules card) {
            return !card.getAiHints().getRemAIDecks();
        }
    };

    /** The Constant isKeptInRandomDecks. */
    public static final Predicate<CardRules> IS_KEPT_IN_RANDOM_DECKS = new Predicate<CardRules>() {
        @Override
        public boolean apply(final CardRules card) {
            return !card.getAiHints().getRemRandomDecks();
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

    public static Predicate<CardRules> cost(final PredicateString.StringOp op, final String what) {
        return new LeafString(LeafString.CardField.COST, op, what);
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
        return new LeafString(LeafString.CardField.ORACLE_TEXT, op, what);
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
     * TODO: Write javadoc for this method.
     * @param transform
     * @return
     */
    public static Predicate<CardRules> splitType(final CardSplitType transform) {
        return new PredicateSplitType(transform);
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

    public static Predicate<CardRules> hasCreatureType(final String... creatureTypes) {
        return new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules card) {
                if (!card.getType().isCreature()) { return false; }

                final Set<String> set = card.getType().getCreatureTypes();
                for (final String creatureType : creatureTypes) {
                    if (set.contains(creatureType)) {
                        return true;
                    }
                }
                return false;
            }
        };
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
                return Iterables.contains(card.getMainPart().getKeywords(), keyword);
            }
        };
    }

    /**
     * Has matching DeckHas hint.
     *
     * @param type
     *            the DeckHints.Type
     * @param has
     *            the hint
     * @return the predicate
     */
    public static Predicate<CardRules> deckHas(final DeckHints.Type type, final String has) {
        return new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules card) {
                DeckHints deckHas = card.getAiHints().getDeckHas();
                return deckHas != null && deckHas.isValid() && deckHas.contains(type, has);
            }
        };
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
            return CardRulesPredicates.coreType(isEqual, Enum.valueOf(CardType.CoreType.class, what));
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
    public static Predicate<CardRules> coreType(final boolean isEqual, final CardType.CoreType type) {
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
            return CardRulesPredicates.superType(isEqual, Enum.valueOf(CardType.Supertype.class, what));
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
    public static Predicate<CardRules> superType(final boolean isEqual, final CardType.Supertype type) {
        return new PredicateSuperType(type, isEqual);
    }


    /**
     * Checks for color.
     *
     * @param thatColor
     *            color to check
     * @return the predicate
     */
    public static Predicate<CardRules> hasColor(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.HasAllOf, thatColor);
    }

    /**
     * Checks if is color.
     *
     * @param thatColor
     *            color to check
     * @return the predicate
     */
    public static Predicate<CardRules> isColor(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.HasAnyOf, thatColor);
    }

    /**
     * Checks if card can be cast with unlimited mana of given color set.
     *
     * @param thatColor
     *            color to check
     * @return the predicate
     */
    public static Predicate<CardRules> canCastWithAvailable(final byte thatColor) {
        return new LeafColor(LeafColor.ColorOperator.CanCast, thatColor);
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

    public static Predicate<CardRules> hasColorIdentity(final int colormask) {
        return new Predicate<CardRules>() {
            @Override
            public boolean apply(final CardRules rules) {
                return rules.getColorIdentity().hasNoColorsExcept(colormask);
            }
        };
    }

    private static class LeafString extends PredicateString<CardRules> {
        public enum CardField {
            ORACLE_TEXT, NAME, SUBTYPE, JOINED_TYPE, COST
        }

        private final String operand;
        private final LeafString.CardField field;

        @Override
        public boolean apply(final CardRules card) {
            boolean shouldContain;
            switch (this.field) {
            case NAME:
                return op(card.getName(), this.operand);
            case SUBTYPE:
                shouldContain = (this.getOperator() == StringOp.CONTAINS) || (this.getOperator() == StringOp.EQUALS);
                return shouldContain == card.getType().hasSubtype(this.operand);
            case ORACLE_TEXT:
                return op(card.getOracleText(), operand);
            case JOINED_TYPE:
                return op(card.getType().toString(), operand);
            case COST:
                final String cost = card.getManaCost().toString();
                return op(cost, operand);
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
            CountColors, CountColorsGreaterOrEqual, HasAnyOf, HasAllOf, Equals, CanCast
        }

        private final LeafColor.ColorOperator op;
        private final byte color;

        public LeafColor(final LeafColor.ColorOperator operator, final byte thatColor) {
            this.op = operator;
            this.color = thatColor;
        }

        @Override
        public boolean apply(final CardRules subject) {
            if (null == subject) {
                return false;
            }
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
            case CanCast:
                return subject.canCastWithAvailable(this.color);
            default:
                return false;
            }
        }
    }

    public static class LeafNumber implements Predicate<CardRules> {
        public enum CardField {
            CMC, GENERIC_COST, POWER, TOUGHNESS
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
            case GENERIC_COST:
                return this.op(card.getManaCost().getGenericCost(), this.operand);
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
        private final CardType.CoreType operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final CardRules card) {
            if (null == card) {
                return false;
            }
            return this.shouldBeEqual == card.getType().hasType(this.operand);
        }

        public PredicateCoreType(final CardType.CoreType type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateSuperType implements Predicate<CardRules> {
        private final CardType.Supertype operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final CardRules card) {
            return this.shouldBeEqual == card.getType().hasSupertype(this.operand);
        }

        public PredicateSuperType(final CardType.Supertype type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateSplitType implements Predicate<CardRules> {
        private final CardSplitType cst;

        public PredicateSplitType(final CardSplitType type) {
            cst = type;
        }

        @Override
        public boolean apply(final CardRules subject) {
            return subject.getSplitType() == cst;
        }
    }

    /**
     * The Class Presets.
     */
    public static class Presets {

        /** The Constant isCreature. */
        public static final Predicate<CardRules> IS_CREATURE = CardRulesPredicates
                .coreType(true, CardType.CoreType.Creature);

        public static final Predicate<CardRules> IS_LEGENDARY = CardRulesPredicates
                .superType(true, CardType.Supertype.Legendary);

        /** The Constant isArtifact. */
        public static final Predicate<CardRules> IS_ARTIFACT = CardRulesPredicates
                .coreType(true, CardType.CoreType.Artifact);

        /** The Constant isEquipment. */
        public static final Predicate<CardRules> IS_EQUIPMENT = CardRulesPredicates
                .subType("Equipment");

        /** The Constant isLand. */
        public static final Predicate<CardRules> IS_LAND = CardRulesPredicates.coreType(true, CardType.CoreType.Land);

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

        public static final Predicate<CardRules> IS_PLANESWALKER = CardRulesPredicates.coreType(true, CardType.CoreType.Planeswalker);
        public static final Predicate<CardRules> IS_INSTANT = CardRulesPredicates.coreType(true, CardType.CoreType.Instant);
        public static final Predicate<CardRules> IS_SORCERY = CardRulesPredicates.coreType(true, CardType.CoreType.Sorcery);
        public static final Predicate<CardRules> IS_ENCHANTMENT = CardRulesPredicates.coreType(true, CardType.CoreType.Enchantment);
        public static final Predicate<CardRules> IS_PLANE = CardRulesPredicates.coreType(true, CardType.CoreType.Plane);
        public static final Predicate<CardRules> IS_PHENOMENON = CardRulesPredicates.coreType(true, CardType.CoreType.Phenomenon);
        public static final Predicate<CardRules> IS_PLANE_OR_PHENOMENON = Predicates.or(IS_PLANE, IS_PHENOMENON);
        public static final Predicate<CardRules> IS_SCHEME = CardRulesPredicates.coreType(true, CardType.CoreType.Scheme);
        public static final Predicate<CardRules> IS_VANGUARD = CardRulesPredicates.coreType(true, CardType.CoreType.Vanguard);
        public static final Predicate<CardRules> IS_CONSPIRACY = CardRulesPredicates.coreType(true, CardType.CoreType.Conspiracy);
        public static final Predicate<CardRules> IS_NON_LAND = CardRulesPredicates.coreType(false, CardType.CoreType.Land);
        public static final Predicate<CardRules> IS_NON_CREATURE_SPELL = Predicates.not(Predicates.or(Presets.IS_CREATURE, Presets.IS_LAND));
        public static final Predicate<CardRules> CAN_BE_COMMANDER = Predicates.or(CardRulesPredicates.rules(StringOp.CONTAINS_IC, "can be your commander"),
                Predicates.and(Presets.IS_CREATURE, Presets.IS_LEGENDARY));

        /** The Constant IS_NONCREATURE_SPELL_FOR_GENERATOR. **/
        @SuppressWarnings("unchecked")
        public static final Predicate<CardRules> IS_NONCREATURE_SPELL_FOR_GENERATOR = com.google.common.base.Predicates
                .or(Presets.IS_SORCERY, Presets.IS_INSTANT, Presets.IS_PLANESWALKER, Presets.IS_ENCHANTMENT,
                        Predicates.and(Presets.IS_ARTIFACT, Predicates.not(Presets.IS_CREATURE)));

        /** The Constant isWhite. */
        public static final Predicate<CardRules> IS_WHITE = CardRulesPredicates.isColor(MagicColor.WHITE);

        /** The Constant isBlue. */
        public static final Predicate<CardRules> IS_BLUE = CardRulesPredicates.isColor(MagicColor.BLUE);

        /** The Constant isBlack. */
        public static final Predicate<CardRules> IS_BLACK = CardRulesPredicates.isColor(MagicColor.BLACK);

        /** The Constant isRed. */
        public static final Predicate<CardRules> IS_RED = CardRulesPredicates.isColor(MagicColor.RED);

        /** The Constant isGreen. */
        public static final Predicate<CardRules> IS_GREEN = CardRulesPredicates.isColor(MagicColor.GREEN);

        /** The Constant isColorless. */
        public static final Predicate<CardRules> IS_COLORLESS = CardRulesPredicates.hasCntColors((byte) 0);

        /** The Constant isMulticolor. */
        public static final Predicate<CardRules> IS_MULTICOLOR = CardRulesPredicates.hasAtLeastCntColors((byte) 2);

        /** The Constant isMonocolor. */
        public static final Predicate<CardRules> IS_MONOCOLOR = CardRulesPredicates.hasCntColors((byte) 1);

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
    }
}
