package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.slightlymagic.maxmtg.Predicate;
import net.slightlymagic.maxmtg.Predicate.ComparableOp;
import net.slightlymagic.maxmtg.Predicate.PredicatesOp;
import net.slightlymagic.maxmtg.PredicateString;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * CardOracle class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardOracle.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardRules {

    private final CardRuleCharacteristics characteristics;

    private int iPower = -1;
    private int iToughness = -1;
    private String power = null;
    private String toughness = null;

    private String loyalty = null;

    private Map<String, CardInSet> setsPrinted = null;

    private boolean isRemovedFromAIDecks = false;
    private boolean isRemovedFromRandomDecks = false;

    // Ctor and builders are needed here
    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return characteristics.getCardName();
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public CardType getType() {
        return characteristics.getCardType();
    }

    /**
     * Gets the mana cost.
     * 
     * @return the mana cost
     */
    public CardManaCost getManaCost() {
        return characteristics.getManaCost();
    }

    /**
     * Gets the color.
     * 
     * @return the color
     */
    public CardColor getColor() {
        return characteristics.getColor();
    }

    /**
     * Gets the rules.
     * 
     * @return the rules
     */
    public String[] getRules() {
        return characteristics.getCardRules();
    }

    /**
     * Gets the sets printed.
     * 
     * @return the sets printed
     */
    public Set<Entry<String, CardInSet>> getSetsPrinted() {
        return characteristics.getSetsData().entrySet();
    }

    /**
     * Gets the power.
     * 
     * @return the power
     */
    public String getPower() {
        return power;
    }

    /**
     * Gets the int power.
     * 
     * @return the int power
     */
    public int getIntPower() {
        return iPower;
    }

    /**
     * Gets the toughness.
     * 
     * @return the toughness
     */
    public String getToughness() {
        return toughness;
    }

    /**
     * Gets the int toughness.
     * 
     * @return the int toughness
     */
    public int getIntToughness() {
        return iToughness;
    }

    /**
     * Gets the loyalty.
     * 
     * @return the loyalty
     */
    public String getLoyalty() {
        return loyalty;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return isRemovedFromAIDecks;
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    public boolean getRemRandomDecks() {
        return isRemovedFromRandomDecks;
    }

    /**
     * Gets the p tor loyalty.
     * 
     * @return the p tor loyalty
     */
    public String getPTorLoyalty() {
        if (getType().isCreature()) {
            return power + "/" + toughness;
        }
        if (getType().isPlaneswalker()) {
            return loyalty;
        }
        return "";
    }

    private final boolean isAlt;

    /**
     * Checks if is alt state.
     * 
     * @return true, if is alt state
     */
    public boolean isAltState() {
        return isAlt;
    }

    private final boolean isDFC;

    /**
     * Checks if is double faced.
     * 
     * @return true, if is double faced
     */
    public boolean isDoubleFaced() {
        return isDFC;
    }

    /**
     * Instantiates a new card rules.
     * 
     * @param chars
     *            the chars
     * @param isDoubleFacedCard
     *            the is double faced card
     * @param isAlt0
     *            the is alt0
     * @param removedFromRandomDecks
     *            the removed from random decks
     * @param removedFromAIDecks
     *            the removed from ai decks
     */
    public CardRules(final CardRuleCharacteristics chars, final boolean isDoubleFacedCard, final boolean isAlt0,
            final boolean removedFromRandomDecks, final boolean removedFromAIDecks) {
        characteristics = chars;
        isAlt = isAlt0;
        isDFC = isDoubleFacedCard;
        this.isRemovedFromAIDecks = removedFromAIDecks;
        this.isRemovedFromRandomDecks = removedFromRandomDecks;

        // System.out.println(cardName);

        if (getType().isCreature()) {
            int slashPos = characteristics.getPtLine() == null ? -1 : characteristics.getPtLine().indexOf('/');
            if (slashPos == -1) {
                throw new RuntimeException(String.format("Creature '%s' has bad p/t stats", getName()));
            }
            this.power = characteristics.getPtLine().substring(0, slashPos);
            this.toughness = characteristics.getPtLine().substring(slashPos + 1, characteristics.getPtLine().length());
            this.iPower = StringUtils.isNumeric(power) ? Integer.parseInt(power) : 0;
            this.iToughness = StringUtils.isNumeric(toughness) ? Integer.parseInt(toughness) : 0;
        } else if (getType().isPlaneswalker()) {
            this.loyalty = characteristics.getPtLine();
        }

        if (characteristics.getSetsData().isEmpty()) {
            characteristics.getSetsData().put("???", new CardInSet(CardRarity.Unknown, 1));
        }
        setsPrinted = characteristics.getSetsData();
    }

    /**
     * Rules contain.
     * 
     * @param text
     *            the text
     * @return true, if successful
     */
    public boolean rulesContain(final String text) {
        if (characteristics.getCardRules() == null) {
            return false;
        }
        for (String r : characteristics.getCardRules()) {
            if (StringUtils.containsIgnoreCase(r, text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the latest set printed.
     * 
     * @return the latest set printed
     */
    public String getLatestSetPrinted() {
        String lastSet = null;
        // TODO: Make a true release-date based sorting
        for (String cs : setsPrinted.keySet()) {
            lastSet = cs;
        }
        return lastSet;
    }

    /**
     * Gets the sets the info.
     * 
     * @param setCode
     *            the set code
     * @return the sets the info
     */
    public CardInSet getSetInfo(final String setCode) {
        CardInSet result = setsPrinted.get(setCode);
        if (result != null) {
            return result;
        }
        throw new RuntimeException(String.format("Card '%s' was never printed in set '%s'", getName(), setCode));

    }

    /**
     * Gets the rarity from latest set.
     * 
     * @return the rarity from latest set
     */
    public CardRarity getRarityFromLatestSet() {
        CardInSet cis = setsPrinted.get(getLatestSetPrinted());
        return cis.getRarity();
    }

    /**
     * Gets the ai status.
     * 
     * @return the ai status
     */
    public String getAiStatus() {
        return isRemovedFromAIDecks ? (isRemovedFromRandomDecks ? "AI ?" : "AI")
                : (isRemovedFromRandomDecks ? "?" : "");
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (isRemovedFromAIDecks && isRemovedFromRandomDecks) {
            return Integer.valueOf(3);
        } else if (isRemovedFromAIDecks) {
            return Integer.valueOf(4);
        } else if (isRemovedFromRandomDecks) {
            return Integer.valueOf(2);
        } else {
            return Integer.valueOf(1);
        }
    }

    /**
     * Filtering conditions specific for CardRules class, defined here along
     * with some presets.
     */
    public abstract static class Predicates {

        /** The Constant isKeptInAiDecks. */
        public static final Predicate<CardRules> IS_KEPT_IN_AI_DECKS = new Predicate<CardRules>() {
            @Override
            public boolean isTrue(final CardRules card) {
                return !card.isRemovedFromAIDecks;
            }
        };

        /** The Constant isKeptInRandomDecks. */
        public static final Predicate<CardRules> IS_KEPT_IN_RANDOM_DECKS = new Predicate<CardRules>() {
            @Override
            public boolean isTrue(final CardRules card) {
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

        // Power
        // Toughness
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
         * Was printed in sets.
         * 
         * @param setCodes
         *            the set codes
         * @return the predicate
         */
        public static Predicate<CardRules> wasPrintedInSets(final List<String> setCodes) {
            return new PredicateExitsInSets(setCodes);
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
                return coreType(isEqual, CardCoreType.valueOf(CardCoreType.class, what));
            } catch (Exception e) {
                return Predicate.getFalse(CardRules.class);
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
                return superType(isEqual, CardSuperType.valueOf(CardSuperType.class, what));
            } catch (Exception e) {
                return Predicate.getFalse(CardRules.class);
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
         * Checks for cnt colors.
         * 
         * @param cntColors
         *            the cnt colors
         * @return the predicate
         */
        public static Predicate<CardRules> hasCntColors(final byte cntColors) {
            return new LeafColor(LeafColor.ColorOperator.Equals, cntColors);
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
            private final CardField field;

            @Override
            public boolean isTrue(final CardRules card) {
                boolean shouldConatin;
                switch (field) {
                case NAME:
                    return op(card.getName(), operand);
                case SUBTYPE:
                    shouldConatin = getOperator() == StringOp.CONTAINS || getOperator() == StringOp.EQUALS;
                    return shouldConatin == card.getType().subTypeContains(operand);
                case RULES:
                    shouldConatin = getOperator() == StringOp.CONTAINS || getOperator() == StringOp.EQUALS;
                    return shouldConatin == card.rulesContain(operand);
                case JOINED_TYPE:
                    return op(card.getType().toString(), operand);
                default:
                    return false;
                }
            }

            public LeafString(final CardField field, final StringOp operator, final String operand) {
                super(operator);
                this.field = field;
                this.operand = operand;
            }
        }

        private static class LeafColor extends Predicate<CardRules> {
            public enum ColorOperator {
                CountColors, CountColorsGreaterOrEqual, HasAnyOf, HasAllOf, Equals
            }

            private final ColorOperator op;
            private final byte color;

            public LeafColor(final ColorOperator operator, final byte thatColor) {
                op = operator;
                color = thatColor;
            }

            @Override
            public boolean isTrue(final CardRules subject) {
                switch (op) {
                case CountColors:
                    return subject.getColor().countColors() == color;
                case CountColorsGreaterOrEqual:
                    return subject.getColor().countColors() >= color;
                case Equals:
                    return subject.getColor().isEqual(color);
                case HasAllOf:
                    return subject.getColor().hasAllColors(color);
                case HasAnyOf:
                    return subject.getColor().hasAnyColor(color);
                default:
                    return false;
                }
            }
        }

        private static class LeafNumber extends Predicate<CardRules> {
            protected enum CardField {
                CMC, POWER, TOUGHNESS,
            }

            private final CardField field;
            private final ComparableOp operator;
            private final int operand;

            public LeafNumber(final CardField field, final ComparableOp op, final int what) {
                this.field = field;
                operand = what;
                operator = op;
            }

            @Override
            public boolean isTrue(final CardRules card) {
                int value;
                switch (field) {
                case CMC:
                    return op(card.getManaCost().getCMC(), operand);
                case POWER:
                    value = card.getIntPower();
                    return value >= 0 ? op(value, operand) : false;
                case TOUGHNESS:
                    value = card.getIntToughness();
                    return value >= 0 ? op(value, operand) : false;
                default:
                    return false;
                }
            }

            private boolean op(final int op1, final int op2) {
                switch (operator) {
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

        private static class PredicateCoreType extends Predicate<CardRules> {
            private final CardCoreType operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return shouldBeEqual == card.getType().typeContains(operand);
            }

            public PredicateCoreType(final CardCoreType type, final boolean wantEqual) {
                operand = type;
                shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateSuperType extends Predicate<CardRules> {
            private final CardSuperType operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return shouldBeEqual == card.getType().superTypeContains(operand);
            }

            public PredicateSuperType(final CardSuperType type, final boolean wantEqual) {
                operand = type;
                shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateLastesSetRarity extends Predicate<CardRules> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return card.getRarityFromLatestSet().equals(operand) == shouldBeEqual;
            }

            public PredicateLastesSetRarity(final CardRarity type, final boolean wantEqual) {
                operand = type;
                shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateExitsInSets extends Predicate<CardRules> {
            private final List<String> sets;

            public PredicateExitsInSets(final List<String> wantSets) {
                sets = wantSets; // maybe should make a copy here?
            }

            @Override
            public boolean isTrue(final CardRules subject) {
                for (String s : sets) {
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
            public static final Predicate<CardRules> IS_CREATURE = coreType(true, CardCoreType.Creature);

            /** The Constant isArtifact. */
            public static final Predicate<CardRules> IS_ARTIFACT = coreType(true, CardCoreType.Artifact);

            /** The Constant isLand. */
            public static final Predicate<CardRules> IS_LAND = coreType(true, CardCoreType.Land);

            /** The Constant isBasicLand. */
            public static final Predicate<CardRules> IS_BASIC_LAND = new Predicate<CardRules>() {
                @Override
                public boolean isTrue(final CardRules subject) {
                    return subject.getType().isBasicLand();
                }
            };

            /** The Constant isPlaneswalker. */
            public static final Predicate<CardRules> IS_PLANESWALKER = coreType(true, CardCoreType.Planeswalker);

            /** The Constant isInstant. */
            public static final Predicate<CardRules> IS_INSTANT = coreType(true, CardCoreType.Instant);

            /** The Constant isSorcery. */
            public static final Predicate<CardRules> IS_SORCERY = coreType(true, CardCoreType.Sorcery);

            /** The Constant isEnchantment. */
            public static final Predicate<CardRules> IS_ENCHANTMENT = coreType(true, CardCoreType.Enchantment);

            /** The Constant isNonLand. */
            public static final Predicate<CardRules> IS_NON_LAND = coreType(false, CardCoreType.Land);

            /** The Constant isNonCreatureSpell. */
            public static final Predicate<CardRules> IS_NON_CREATURE_SPELL = Predicate.compose(IS_CREATURE,
                    PredicatesOp.NOR, IS_LAND);

            /** The Constant isWhite. */
            public static final Predicate<CardRules> IS_WHITE = isColor(CardColor.WHITE);

            /** The Constant isBlue. */
            public static final Predicate<CardRules> IS_BLUE = isColor(CardColor.BLUE);

            /** The Constant isBlack. */
            public static final Predicate<CardRules> IS_BLACK = isColor(CardColor.BLACK);

            /** The Constant isRed. */
            public static final Predicate<CardRules> IS_RED = isColor(CardColor.RED);

            /** The Constant isGreen. */
            public static final Predicate<CardRules> IS_GREEN = isColor(CardColor.GREEN);

            /** The Constant isColorless. */
            public static final Predicate<CardRules> IS_COLORLESS = hasCntColors((byte) 0);

            /** The Constant isMulticolor. */
            public static final Predicate<CardRules> IS_MULTICOLOR = hasAtLeastCntColors((byte) 2);

            /** The Constant colors. */
            public static final List<Predicate<CardRules>> COLORS = new ArrayList<Predicate<CardRules>>();
            static {
                COLORS.add(IS_WHITE);
                COLORS.add(IS_BLUE);
                COLORS.add(IS_BLACK);
                COLORS.add(IS_RED);
                COLORS.add(IS_GREEN);
                COLORS.add(IS_COLORLESS);
            }

            /** The Constant constantTrue. */
            public static final Predicate<CardRules> CONSTANT_TRUE = Predicate.getTrue(CardRules.class);

            // Think twice before using these, since rarity is a prop of printed
            // card.
            /** The Constant isInLatestSetCommon. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_COMMON = rarityInCardsLatestSet(true,
                    CardRarity.Common);

            /** The Constant isInLatestSetUncommon. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_UNCOMMON = rarityInCardsLatestSet(true,
                    CardRarity.Uncommon);

            /** The Constant isInLatestSetRare. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_RARE = rarityInCardsLatestSet(true, CardRarity.Rare);

            /** The Constant isInLatestSetMythicRare. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_MYTHIC_RARE = rarityInCardsLatestSet(true,
                    CardRarity.MythicRare);

            /** The Constant isInLatestSetSpecial. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_SPECIAL = rarityInCardsLatestSet(true,
                    CardRarity.Special);
        }
    }
}
