package forge.card;

import forge.util.CardTranslation;
import forge.util.ComparableOp;
import forge.util.IterableUtil;
import forge.util.PredicateString;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Filtering conditions specific for CardRules class, defined here along with
 * some presets.
 */
public final class CardRulesPredicates {

    public static final Predicate<CardRules> IS_KEPT_IN_AI_DECKS = card -> !card.getAiHints().getRemAIDecks();
    public static final Predicate<CardRules> IS_KEPT_IN_AI_LIMITED_DECKS = card -> !card.getAiHints().getRemAIDecks() && !card.getAiHints().getRemNonCommanderDecks();
    public static final Predicate<CardRules> IS_KEPT_IN_RANDOM_DECKS = card -> !card.getAiHints().getRemRandomDecks();

    // Static builder methods - they choose concrete implementation by themselves
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
     * Sub type.
     *
     * @param what
     *            the what
     * @return the predicate
     */
    public static Predicate<CardRules> subType(final String what) {
        return new LeafString(LeafString.CardField.SUBTYPE, PredicateString.StringOp.CONTAINS, what);
    }

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
        return card -> {
            if (!card.getType().isCreature()) { return false; }
            return !Collections.disjoint(card.getType().getCreatureTypes(), Arrays.asList(creatureTypes));
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
        return card -> IterableUtil.any(card.getAllFaces(), cf -> cf != null && card.hasStartOfKeyword(keyword, cf));
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
        return card -> {
            DeckHints deckHas = card.getAiHints().getDeckHas();
            return deckHas != null && deckHas.isValid() && deckHas.contains(type, has);
        };
    }

    public static Predicate<CardRules> deckHasExactly(final DeckHints.Type type, final String has[]) {
        return card -> {
            DeckHints deckHas = card.getAiHints().getDeckHas();
            return deckHas != null && deckHas.isValid() && deckHas.is(type, has);
        };
    }

    public static Predicate<CardRules> coreType(final String what) {
        try {
            return CardRulesPredicates.coreType(Enum.valueOf(CardType.CoreType.class, what));
        } catch (final Exception e) {
            return x -> false;
        }
    }

    /**
     * @return a Predicate that matches cards that have the specified core type.
     */
    public static Predicate<CardRules> coreType(final CardType.CoreType type) {
        return card -> card.getType().hasType(type);
    }

    /**
     * @return a Predicate that matches cards that have the specified supertype.
     */
    public static Predicate<CardRules> superType(final CardType.Supertype type) {
        return card -> card.getType().hasSupertype(type);
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
        return rules -> rules.getColorIdentity().hasNoColorsExcept(colormask);
    }

    public static Predicate<CardRules> canBePartnerCommanderWith(final CardRules commander) {
        return rules -> rules.canBePartnerCommanders(commander);
    }

    private static class LeafString extends PredicateString<CardRules> {
        public enum CardField {
            ORACLE_TEXT, NAME, SUBTYPE, JOINED_TYPE, COST
        }

        private final String operand;
        private final LeafString.CardField field;

        protected boolean checkName(String name) {
            return op(name, this.operand)
            || op(CardTranslation.getTranslatedName(name), this.operand)
            || op(StringUtils.stripAccents(name), this.operand);
        }
        protected boolean checkOracle(ICardFace face) {
            if (face == null) {
                return false;
            }
            if (face.hasFunctionalVariants()) {
                for (Map.Entry<String, ? extends ICardFace> v : face.getFunctionalVariants().entrySet()) {
                    //Not a very pretty implementation, but an ICardFace doesn't have a specific variant, so they all need to be checked.
                    String origOracle = v.getValue().getOracleText();
                    if(op(origOracle, operand))
                        return true;
                    String name = v.getValue().getName() + " $" + v.getKey();
                    if(op(CardTranslation.getTranslatedOracle(name), operand))
                        return true;
                }
            }
            if (op(face.getOracleText(), operand) || op(CardTranslation.getTranslatedOracle(face.getName()), operand)) {
                return true;
            }
            return false;
        }
        protected boolean checkType(ICardFace face) {
            if (face == null) {
                return false;
            }
            if (face.hasFunctionalVariants()) {
                for (Map.Entry<String, ? extends ICardFace> v : face.getFunctionalVariants().entrySet()) {
                    String origType = v.getValue().getType().toString();
                    if(op(origType, operand))
                        return true;
                    String name = v.getValue().getName() + " $" + v.getKey();
                    if(op(CardTranslation.getTranslatedType(name, origType), operand))
                        return true;
                }
            }
            return (op(CardTranslation.getTranslatedType(face.getName(), face.getType().toString()), operand) || op(face.getType().toString(), operand));
        }

        @Override
        public boolean test(final CardRules card) {
            boolean shouldContain;
            switch (this.field) {
            case NAME:
                for (ICardFace face : card.getAllFaces()) {
                    if (face != null && checkName(face.getName())) {
                        return true;
                    }
                }
                return false;
            case SUBTYPE:
                shouldContain = (this.getOperator() == StringOp.CONTAINS) || (this.getOperator() == StringOp.EQUALS);
                return shouldContain == card.getType().hasSubtype(this.operand);
            case ORACLE_TEXT:
                for (ICardFace face : card.getAllFaces()) {
                    if (checkOracle(face)) {
                        return true;
                    }
                }
                return false;
            case JOINED_TYPE:
                if ((op(CardTranslation.getTranslatedType(card.getName(), card.getType().toString()), operand) || op(card.getType().toString(), operand))) {
                    return true;
                }
                for (ICardFace face : card.getAllFaces()) {
                    if (checkType(face)) {
                        return true;
                    }
                }

                return false;
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
        public boolean test(final CardRules subject) {
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
        public boolean test(final CardRules card) {
            int value;
            switch (this.field) {
            case CMC:
                return this.op(card.getManaCost().getCMC(), this.operand);
            case GENERIC_COST:
                return this.op(card.getManaCost().getGenericCost(), this.operand);
            case POWER:
                value = card.getIntPower();
                return value != Integer.MAX_VALUE && this.op(value, this.operand);
            case TOUGHNESS:
                value = card.getIntToughness();
                return value != Integer.MAX_VALUE && this.op(value, this.operand);
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

    public static final Predicate<CardRules> IS_CREATURE = CardRulesPredicates.coreType(CardType.CoreType.Creature);
    public static final Predicate<CardRules> IS_LEGENDARY = CardRulesPredicates.superType(CardType.Supertype.Legendary);
    public static final Predicate<CardRules> IS_ARTIFACT = CardRulesPredicates.coreType(CardType.CoreType.Artifact);
    public static final Predicate<CardRules> IS_ATTRACTION = CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Attraction"));
    public static final Predicate<CardRules> IS_CONTRAPTION = CardRulesPredicates.IS_ARTIFACT.and(CardRulesPredicates.subType("Contraption"));
    public static final Predicate<CardRules> IS_EQUIPMENT = CardRulesPredicates.subType("Equipment");
    public static final Predicate<CardRules> IS_LAND = CardRulesPredicates.coreType(CardType.CoreType.Land);
    public static final Predicate<CardRules> IS_BASIC_LAND = subject -> subject.getType().isBasicLand();
    public static final Predicate<CardRules> NOT_BASIC_LAND = subject -> !subject.getType().isBasicLand();
    /** Matches only Plains, Island, Swamp, Mountain, or Forest. */
    public static final Predicate<CardRules> IS_TRUE_BASIC_LAND = subject -> !subject.getName().equals("Wastes")&&subject.getType().isBasicLand();
    /** Matches any card except Plains, Island, Swamp, Mountain, or Forest. */
    public static final Predicate<CardRules> NOT_TRUE_BASIC_LAND = subject -> !subject.getType().isBasicLand() || subject.getName().equals("Wastes");
    public static final Predicate<CardRules> IS_NONBASIC_LAND = subject -> subject.getType().isLand() && !subject.getType().isBasicLand();
    public static final Predicate<CardRules> CAN_BE_COMMANDER = CardRules::canBeCommander;
    public static final Predicate<CardRules> CAN_BE_PARTNER_COMMANDER = CardRules::canBePartnerCommander;
    public static final Predicate<CardRules> CAN_BE_OATHBREAKER = CardRules::canBeOathbreaker;
    public static final Predicate<CardRules> CAN_BE_SIGNATURE_SPELL = CardRules::canBeSignatureSpell;
    public static final Predicate<CardRules> IS_PLANESWALKER = CardRulesPredicates.coreType(CardType.CoreType.Planeswalker);
    public static final Predicate<CardRules> CAN_BE_TINY_LEADERS_COMMANDER = CardRulesPredicates.IS_LEGENDARY.and(CardRulesPredicates.IS_CREATURE.or(CardRulesPredicates.IS_PLANESWALKER));
    public static final Predicate<CardRules> CAN_BE_BRAWL_COMMANDER = CardRulesPredicates.IS_LEGENDARY.and(CardRulesPredicates.IS_CREATURE.or(CardRulesPredicates.IS_PLANESWALKER));
    public static final Predicate<CardRules> IS_BATTLE = CardRulesPredicates.coreType(CardType.CoreType.Battle);
    public static final Predicate<CardRules> IS_INSTANT = CardRulesPredicates.coreType(CardType.CoreType.Instant);
    public static final Predicate<CardRules> IS_SORCERY = CardRulesPredicates.coreType(CardType.CoreType.Sorcery);
    public static final Predicate<CardRules> IS_ENCHANTMENT = CardRulesPredicates.coreType(CardType.CoreType.Enchantment);
    public static final Predicate<CardRules> IS_NON_CREATURE_SPELL = Predicate.not(
            CardRulesPredicates.IS_CREATURE.or(CardRulesPredicates.IS_LAND).or(CardRules::isVariant)
    );

    public static final Predicate<CardRules> IS_PLANE = CardRulesPredicates.coreType(CardType.CoreType.Plane);
    public static final Predicate<CardRules> IS_PHENOMENON = CardRulesPredicates.coreType(CardType.CoreType.Phenomenon);
    public static final Predicate<CardRules> IS_PLANE_OR_PHENOMENON = IS_PLANE.or(IS_PHENOMENON);
    public static final Predicate<CardRules> IS_SCHEME = CardRulesPredicates.coreType(CardType.CoreType.Scheme);
    public static final Predicate<CardRules> IS_VANGUARD = CardRulesPredicates.coreType(CardType.CoreType.Vanguard);
    public static final Predicate<CardRules> IS_CONSPIRACY = CardRulesPredicates.coreType(CardType.CoreType.Conspiracy);
    public static final Predicate<CardRules> IS_DUNGEON = CardRulesPredicates.coreType(CardType.CoreType.Dungeon);
    public static final Predicate<CardRules> IS_NON_LAND = CardRulesPredicates.coreType(CardType.CoreType.Land);
    public static final Predicate<CardRules> IS_WHITE = CardRulesPredicates.isColor(MagicColor.WHITE);
    public static final Predicate<CardRules> IS_BLUE = CardRulesPredicates.isColor(MagicColor.BLUE);
    public static final Predicate<CardRules> IS_BLACK = CardRulesPredicates.isColor(MagicColor.BLACK);
    public static final Predicate<CardRules> IS_RED = CardRulesPredicates.isColor(MagicColor.RED);
    public static final Predicate<CardRules> IS_GREEN = CardRulesPredicates.isColor(MagicColor.GREEN);
    public static final Predicate<CardRules> IS_COLORLESS = CardRulesPredicates.hasCntColors((byte) 0);
    public static final Predicate<CardRules> IS_MULTICOLOR = CardRulesPredicates.hasAtLeastCntColors((byte) 2);
    public static final Predicate<CardRules> IS_MONOCOLOR = CardRulesPredicates.hasCntColors((byte) 1);
}
