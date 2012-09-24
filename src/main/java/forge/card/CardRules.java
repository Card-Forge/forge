/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import forge.util.closures.Predicate;
import forge.util.closures.PredicateInteger.ComparableOp;
import forge.util.closures.Predicate.PredicatesOp;
import forge.util.closures.PredicateString;


/**
 * A collection of methods containing full
 * meta and gameplay properties of a card.
 * 
 * @author Forge
 * @version $Id: CardRules.java 9708 2011-08-09 19:34:12Z jendave $
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

    private final CardRules slavePart;

    private final boolean hasOtherFace;

    private List<String> originalScript;

    // Ctor and builders are needed here
    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return this.characteristics.getCardName();
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public CardType getType() {
        return this.characteristics.getCardType();
    }

    /**
     * Gets the mana cost.
     * 
     * @return the mana cost
     */
    public CardManaCost getManaCost() {
        return this.characteristics.getManaCost();
    }

    /**
     * Gets the color.
     * 
     * @return the color
     */
    public CardColor getColor() {
        return this.characteristics.getColor();
    }

    /**
     * Gets the rules.
     * 
     * @return the rules
     */
    public String[] getRules() {
        return this.characteristics.getCardRules();
    }

    /**
     * 
     * Gets Slave Part.
     * 
     * @return CardRules
     */
    public CardRules getSlavePart() {
        return this.slavePart;
    }

    /**
     * Gets the sets printed.
     * 
     * @return the sets printed
     */
    public Set<Entry<String, CardInSet>> getSetsPrinted() {
        return this.characteristics.getSetsData().entrySet();
    }

    /**
     * Gets the power.
     * 
     * @return the power
     */
    public String getPower() {
        return this.power;
    }

    /**
     * Gets the int power.
     * 
     * @return the int power
     */
    public int getIntPower() {
        return this.iPower;
    }

    /**
     * Gets the toughness.
     * 
     * @return the toughness
     */
    public String getToughness() {
        return this.toughness;
    }

    /**
     * Gets the int toughness.
     * 
     * @return the int toughness
     */
    public int getIntToughness() {
        return this.iToughness;
    }

    /**
     * Gets the loyalty.
     * 
     * @return the loyalty
     */
    public String getLoyalty() {
        return this.loyalty;
    }

    /**
     * Gets the rem ai decks.
     * 
     * @return the rem ai decks
     */
    public boolean getRemAIDecks() {
        return this.isRemovedFromAIDecks;
    }

    /**
     * Gets the rem random decks.
     * 
     * @return the rem random decks
     */
    public boolean getRemRandomDecks() {
        return this.isRemovedFromRandomDecks;
    }

    /**
     * Gets the p tor loyalty.
     * 
     * @return the p tor loyalty
     */
    public String getPTorLoyalty() {
        if (this.getType().isCreature()) {
            return this.power + "/" + this.toughness;
        }
        if (this.getType().isPlaneswalker()) {
            return this.loyalty;
        }
        return "";
    }

    /**
     * Checks if is alt state.
     * 
     * @return true, if is alt state
     */
    public boolean isAltState() {
        return this.isDoubleFaced() && (this.slavePart == null);
    }

    /**
     * Checks if is double faced.
     * 
     * @return true, if is double faced
     */
    public boolean isDoubleFaced() {
        return this.hasOtherFace;
    }

    /**
     * Instantiates a new card rules.
     * 
     * @param chars
     *            the chars
     * @param isDoubleFacedCard
     *            the is double faced card
     * @param otherPart
     *            the otherPart
     * @param removedFromRandomDecks
     *            the removed from random decks
     * @param removedFromAIDecks
     *            the removed from ai decks
     */
    public CardRules(final CardRuleCharacteristics chars, List<String> forgeScript, final boolean isDoubleFacedCard,
            final CardRules otherPart, final boolean removedFromRandomDecks, final boolean removedFromAIDecks) {
        this.characteristics = chars;
        this.slavePart = otherPart;
        this.hasOtherFace = isDoubleFacedCard;
        this.isRemovedFromAIDecks = removedFromAIDecks;
        this.isRemovedFromRandomDecks = removedFromRandomDecks;
        this.originalScript = forgeScript == null ? null : new ArrayList<String>(forgeScript);
        // System.out.println(cardName);

        if (this.getType().isCreature()) {
            final int slashPos = this.characteristics.getPtLine() == null ? -1 : this.characteristics.getPtLine()
                    .indexOf('/');
            if (slashPos == -1) {
                throw new RuntimeException(String.format("Creature '%s' has bad p/t stats", this.getName()));
            }
            this.power = this.characteristics.getPtLine().substring(0, slashPos);
            this.toughness = this.characteristics.getPtLine().substring(slashPos + 1,
                    this.characteristics.getPtLine().length());
            this.iPower = StringUtils.isNumeric(this.power) ? Integer.parseInt(this.power) : 0;
            this.iToughness = StringUtils.isNumeric(this.toughness) ? Integer.parseInt(this.toughness) : 0;
        } else if (this.getType().isPlaneswalker()) {
            this.loyalty = this.characteristics.getPtLine();
        }

        if (this.characteristics.getSetsData().isEmpty()) {
            this.characteristics.getSetsData().put("???", new CardInSet(CardRarity.Unknown, 1));
        }
        this.setsPrinted = this.characteristics.getSetsData();
    }

    /**
     * Rules contain.
     * 
     * @param text
     *            the text
     * @return true, if successful
     */
    public boolean rulesContain(final String text) {
        if (this.characteristics.getCardRules() == null) {
            return false;
        }
        for (final String r : this.characteristics.getCardRules()) {
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
        for (final String cs : this.setsPrinted.keySet()) {
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
    public CardInSet getEditionInfo(final String setCode) {
        final CardInSet result = this.setsPrinted.get(setCode);
        if (result != null) {
            return result;
        }
        throw new RuntimeException(String.format("Card '%s' was never printed in set '%s'", this.getName(), setCode));

    }

    /**
     * Gets the rarity from latest set.
     * 
     * @return the rarity from latest set
     */
    public CardRarity getRarityFromLatestSet() {
        final CardInSet cis = this.setsPrinted.get(this.getLatestSetPrinted());
        return cis.getRarity();
    }

    /**
     * Gets the ai status.
     * 
     * @return the ai status
     */
    public String getAiStatus() {
        return this.isRemovedFromAIDecks ? (this.isRemovedFromRandomDecks ? "AI ?" : "AI")
                : (this.isRemovedFromRandomDecks ? "?" : "");
    }

    /**
     * Gets the ai status comparable.
     * 
     * @return the ai status comparable
     */
    public Integer getAiStatusComparable() {
        if (this.isRemovedFromAIDecks && this.isRemovedFromRandomDecks) {
            return Integer.valueOf(3);
        } else if (this.isRemovedFromAIDecks) {
            return Integer.valueOf(4);
        } else if (this.isRemovedFromRandomDecks) {
            return Integer.valueOf(2);
        } else {
            return Integer.valueOf(1);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public Iterable<String> getCardScript() {
        return originalScript;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getPictureUrl() {
        return characteristics.getDlUrl();
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckHints() {
        return characteristics.getDeckHints();
    }

    /**
     * @return the deckHints
     */
    public DeckHints getDeckNeeds() {
        return characteristics.getDeckNeeds();
    }

    /**
     * @return the keywords
     */
    public List<String> getKeywords() {
        return characteristics.getKeywords();
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
                public boolean isTrue(final CardRules card) {
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
                return Predicates.coreType(isEqual, Enum.valueOf(CardCoreType.class, what));
            } catch (final Exception e) {
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
                return Predicates.superType(isEqual, Enum.valueOf(CardSuperType.class, what));
            } catch (final Exception e) {
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
            private final CardField field;

            @Override
            public boolean isTrue(final CardRules card) {
                boolean shouldContain;
                switch (this.field) {
                case NAME:
                    return this.op(card.getName(), this.operand);
                case SUBTYPE:
                    shouldContain = (this.getOperator() == StringOp.CONTAINS)
                            || (this.getOperator() == StringOp.EQUALS);
                    return shouldContain == card.getType().subTypeContains(this.operand);
                case RULES:
                    shouldContain = (this.getOperator() == StringOp.CONTAINS)
                            || (this.getOperator() == StringOp.EQUALS);
                    return shouldContain == card.rulesContain(this.operand);
                case JOINED_TYPE:
                    return this.op(card.getType().toString(), this.operand);
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
                this.op = operator;
                this.color = thatColor;
            }

            @Override
            public boolean isTrue(final CardRules subject) {
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

        private static class LeafNumber extends Predicate<CardRules> {
            protected enum CardField {
                CMC, POWER, TOUGHNESS,
            }

            private final CardField field;
            private final ComparableOp operator;
            private final int operand;

            public LeafNumber(final CardField field, final ComparableOp op, final int what) {
                this.field = field;
                this.operand = what;
                this.operator = op;
            }

            @Override
            public boolean isTrue(final CardRules card) {
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

        private static class PredicateCoreType extends Predicate<CardRules> {
            private final CardCoreType operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return this.shouldBeEqual == card.getType().typeContains(this.operand);
            }

            public PredicateCoreType(final CardCoreType type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateSuperType extends Predicate<CardRules> {
            private final CardSuperType operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return this.shouldBeEqual == card.getType().superTypeContains(this.operand);
            }

            public PredicateSuperType(final CardSuperType type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateLastesSetRarity extends Predicate<CardRules> {
            private final CardRarity operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) {
                return card.getRarityFromLatestSet().equals(this.operand) == this.shouldBeEqual;
            }

            public PredicateLastesSetRarity(final CardRarity type, final boolean wantEqual) {
                this.operand = type;
                this.shouldBeEqual = wantEqual;
            }
        }

        private static class PredicateExistsInSets extends Predicate<CardRules> {
            private final List<String> sets;

            public PredicateExistsInSets(final List<String> wantSets) {
                this.sets = wantSets; // maybe should make a copy here?
            }

            @Override
            public boolean isTrue(final CardRules subject) {
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
            public static final Predicate<CardRules> IS_CREATURE = Predicates.coreType(true, CardCoreType.Creature);

            /** The Constant isArtifact. */
            public static final Predicate<CardRules> IS_ARTIFACT = Predicates.coreType(true, CardCoreType.Artifact);

            /** The Constant isLand. */
            public static final Predicate<CardRules> IS_LAND = Predicates.coreType(true, CardCoreType.Land);

            /** The Constant isBasicLand. */
            public static final Predicate<CardRules> IS_BASIC_LAND = new Predicate<CardRules>() {
                @Override
                public boolean isTrue(final CardRules subject) {
                    return subject.getType().isBasicLand();
                }
            };

            /** The Constant isPlaneswalker. */
            public static final Predicate<CardRules> IS_PLANESWALKER = Predicates.coreType(true,
                    CardCoreType.Planeswalker);

            /** The Constant isInstant. */
            public static final Predicate<CardRules> IS_INSTANT = Predicates.coreType(true, CardCoreType.Instant);

            /** The Constant isSorcery. */
            public static final Predicate<CardRules> IS_SORCERY = Predicates.coreType(true, CardCoreType.Sorcery);

            /** The Constant isEnchantment. */
            public static final Predicate<CardRules> IS_ENCHANTMENT = Predicates.coreType(true,
                    CardCoreType.Enchantment);

            /** The Constant isNonLand. */
            public static final Predicate<CardRules> IS_NON_LAND = Predicates.coreType(false, CardCoreType.Land);

            /** The Constant isNonCreatureSpell. */
            public static final Predicate<CardRules> IS_NON_CREATURE_SPELL = Predicate.compose(Presets.IS_CREATURE,
                    PredicatesOp.NOR, Presets.IS_LAND);

            /**
             * 
             */
            @SuppressWarnings("unchecked")
            public static final Predicate<CardRules> IS_NONCREATURE_SPELL_FOR_GENERATOR = Predicate.or(Arrays.asList(
                    Presets.IS_SORCERY, Presets.IS_INSTANT, Presets.IS_PLANESWALKER, Presets.IS_ENCHANTMENT,
                    Predicate.compose(Presets.IS_ARTIFACT, PredicatesOp.GT, Presets.IS_CREATURE))
            );

            /** The Constant isWhite. */
            public static final Predicate<CardRules> IS_WHITE = Predicates.isColor(CardColor.WHITE);

            /** The Constant isBlue. */
            public static final Predicate<CardRules> IS_BLUE = Predicates.isColor(CardColor.BLUE);

            /** The Constant isBlack. */
            public static final Predicate<CardRules> IS_BLACK = Predicates.isColor(CardColor.BLACK);

            /** The Constant isRed. */
            public static final Predicate<CardRules> IS_RED = Predicates.isColor(CardColor.RED);

            /** The Constant isGreen. */
            public static final Predicate<CardRules> IS_GREEN = Predicates.isColor(CardColor.GREEN);

            /** The Constant isColorless. */
            public static final Predicate<CardRules> IS_COLORLESS = Predicates.hasCntColors((byte) 0);

            /** The Constant isMulticolor. */
            public static final Predicate<CardRules> IS_MULTICOLOR = Predicates.hasAtLeastCntColors((byte) 2);

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

            /** The Constant constantTrue. */
            public static final Predicate<CardRules> CONSTANT_TRUE = Predicate.getTrue(CardRules.class);

            // Think twice before using these, since rarity is a prop of printed
            // card.
            /** The Constant isInLatestSetCommon. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_COMMON = Predicates.rarityInCardsLatestSet(true,
                    CardRarity.Common);

            /** The Constant isInLatestSetUncommon. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_UNCOMMON = Predicates.rarityInCardsLatestSet(
                    true, CardRarity.Uncommon);

            /** The Constant isInLatestSetRare. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_RARE = Predicates.rarityInCardsLatestSet(true,
                    CardRarity.Rare);

            /** The Constant isInLatestSetMythicRare. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_MYTHIC_RARE = Predicates.rarityInCardsLatestSet(
                    true, CardRarity.MythicRare);

            /** The Constant isInLatestSetSpecial. */
            public static final Predicate<CardRules> IS_IN_LATEST_SET_SPECIAL = Predicates.rarityInCardsLatestSet(true,
                    CardRarity.Special);
        }
    }
}


