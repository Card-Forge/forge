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
 * <p>CardOracle class.</p>
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
    public String getName() { return characteristics.getCardName(); }
    public CardType getType() { return characteristics.getCardType(); }
    public CardManaCost getManaCost() { return characteristics.getManaCost(); }
    public CardColor getColor() { return characteristics.getColor(); }
    public String[] getRules() { return characteristics.getCardRules(); }
    public Set<Entry<String, CardInSet>> getSetsPrinted() { return characteristics.getSetsData().entrySet(); }

    public String getPower() { return power; }
    public int getIntPower() { return iPower; }
    public String getToughness() { return toughness; }
    public int getIntToughness() { return iToughness; }
    public String getLoyalty() { return loyalty; }
    public boolean getRemAIDecks() { return isRemovedFromAIDecks; }
    public boolean getRemRandomDecks() { return isRemovedFromRandomDecks; }

    public String getPTorLoyalty() {
        if (getType().isCreature()) { return power + "/" + toughness; }
        if (getType().isPlaneswalker()) { return loyalty; }
        return "";
    }
    
    private final boolean isAlt;
    public boolean isAltState() {
        return isAlt;
    }
    
    private final boolean isDFC;
    public boolean isDoubleFaced() {
        return isDFC;
    }

    public CardRules(final CardRuleCharacteristics chars,
            final boolean isDoubleFacedCard, final boolean isAlt0,
            final boolean removedFromRandomDecks, final boolean removedFromAIDecks)
    {
        characteristics = chars;
        isAlt = isAlt0;
        isDFC = isDoubleFacedCard;
        this.isRemovedFromAIDecks = removedFromAIDecks;
        this.isRemovedFromRandomDecks = removedFromRandomDecks;

        //System.out.println(cardName);

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

    public boolean rulesContain(final String text) {
        if (characteristics.getCardRules() == null) { return false; }
        for (String r : characteristics.getCardRules()) { if (StringUtils.containsIgnoreCase(r, text)) { return true; } }
        return false;
    }
    public String getLatestSetPrinted() {
        String lastSet = null;
        // TODO: Make a true release-date based sorting
        for (String cs : setsPrinted.keySet()) {
            lastSet = cs;
        }
        return lastSet;
    }
    public CardInSet getSetInfo(final String setCode) {
        CardInSet result = setsPrinted.get(setCode);
        if (result != null) { return result; }
        throw new RuntimeException(String.format("Card '%s' was never printed in set '%s'", getName(), setCode));

    }
    public CardRarity getRarityFromLatestSet() {
        CardInSet cis = setsPrinted.get(getLatestSetPrinted());
        return cis.getRarity();
    }

    public String getAiStatus() {
        return isRemovedFromAIDecks ? (isRemovedFromRandomDecks ? "AI ?" : "AI") : (isRemovedFromRandomDecks ? "?" : "");
    }
    public Integer getAiStatusComparable() {
        if (isRemovedFromAIDecks && isRemovedFromRandomDecks) { return Integer.valueOf(3); }
        else if (isRemovedFromAIDecks) { return Integer.valueOf(4); }
        else if (isRemovedFromRandomDecks) { return Integer.valueOf(2); }
        else { return Integer.valueOf(1); }
    }

    /**
     * Filtering conditions specific for CardRules class, defined here along with some presets.
     */
    public abstract static class Predicates {
        public static final Predicate<CardRules> isKeptInAiDecks = new Predicate<CardRules>() {
            @Override public boolean isTrue(CardRules card) { return !card.isRemovedFromAIDecks; } };
        public static final Predicate<CardRules> isKeptInRandomDecks = new Predicate<CardRules>() {
            @Override public boolean isTrue(CardRules card) { return !card.isRemovedFromRandomDecks; } };
            
        
        // Static builder methods - they choose concrete implementation by themselves
        public static Predicate<CardRules> cmc(final ComparableOp op, final int what)
        {
            return new LeafNumber(LeafNumber.CardField.CMC, op, what);
        }
        // Power
        // Toughness
        public static Predicate<CardRules> rules(final PredicateString.StringOp op, final String what) {
            return new LeafString(LeafString.CardField.RULES, op, what);
        }
        public static Predicate<CardRules> name(final PredicateString.StringOp op, final String what) {
            return new LeafString(LeafString.CardField.NAME, op, what);
        }
        public static Predicate<CardRules> subType(final String what) {
            return new LeafString(LeafString.CardField.SUBTYPE, PredicateString.StringOp.CONTAINS, what);
        }

        public static Predicate<CardRules> subType(final PredicateString.StringOp op, final String what) {
            return new LeafString(LeafString.CardField.SUBTYPE, op, what);
        }
        public static Predicate<CardRules> joinedType(final PredicateString.StringOp op, final String what) {
            return new LeafString(LeafString.CardField.JOINED_TYPE, op, what);
        }
        
        public static Predicate<CardRules> wasPrintedInSets(final List<String> setCodes) {
            return new PredicateExitsInSets(setCodes);
        }
        
        public static Predicate<CardRules> coreType(final boolean isEqual, final String what)
        {
            try { return coreType(isEqual, CardCoreType.valueOf(CardCoreType.class, what)); }
            catch (Exception e) { return Predicate.getFalse(CardRules.class); }
        }
        public static Predicate<CardRules> coreType(final boolean isEqual, final CardCoreType type)
        {
            return new PredicateCoreType(type, isEqual);
        }
        public static Predicate<CardRules> superType(final boolean isEqual, final String what)
        {
            try { return superType(isEqual, CardSuperType.valueOf(CardSuperType.class, what)); }
            catch (Exception e) { return Predicate.getFalse(CardRules.class); }
        }
        public static Predicate<CardRules> superType(final boolean isEqual, final CardSuperType type)
        {
            return new PredicateSuperType(type, isEqual);
        }
        public static Predicate<CardRules> rarityInCardsLatestSet(final boolean isEqual, final CardRarity value)
        {
            return new PredicateLastesSetRarity(value, isEqual);
        }
        public static Predicate<CardRules> hasColor(final byte thatColor) {
            return new LeafColor(LeafColor.ColorOperator.HasAllOf, thatColor);
        }
        public static Predicate<CardRules> isColor(final byte thatColor) {
            return new LeafColor(LeafColor.ColorOperator.HasAnyOf, thatColor);
        }
        public static Predicate<CardRules> hasCntColors(final byte cntColors) {
            return new LeafColor(LeafColor.ColorOperator.Equals, cntColors);
        }
        public static Predicate<CardRules> hasAtLeastCntColors(final byte cntColors) {
            return new LeafColor(LeafColor.ColorOperator.CountColorsGreaterOrEqual, cntColors);
        }

        private static class LeafString extends PredicateString<CardRules> {
            public enum CardField {
                RULES,
                NAME,
                SUBTYPE,
                JOINED_TYPE
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
                        shouldConatin = operator == StringOp.CONTAINS || operator == StringOp.EQUALS;
                        return shouldConatin == card.getType().subTypeContains(operand);
                    case RULES:
                        shouldConatin = operator == StringOp.CONTAINS || operator == StringOp.EQUALS;
                        return shouldConatin == card.rulesContain(operand);
                    case JOINED_TYPE:
                        return op(card.getType().toString(), operand);
                    default:
                        return false;
                }
            }

            public LeafString(final CardField field, final StringOp operator, final String operand)
            {
                super(operator);
                this.field = field;
                this.operand = operand;
            }
        }

        private static class LeafColor extends Predicate<CardRules> {
            public enum ColorOperator {
                CountColors,
                CountColorsGreaterOrEqual,
                HasAnyOf,
                HasAllOf,
                Equals
            }

            private final ColorOperator op;
            private final byte color;

            public LeafColor(final ColorOperator operator, final byte thatColor)
            {
                op = operator;
                color = thatColor;
            }

            @Override
            public boolean isTrue(final CardRules subject) {
                switch(op) {
                case CountColors: return subject.getColor().countColors() == color;
                case CountColorsGreaterOrEqual: return subject.getColor().countColors() >= color;
                case Equals: return subject.getColor().isEqual(color);
                case HasAllOf: return subject.getColor().hasAllColors(color);
                case HasAnyOf: return subject.getColor().hasAnyColor(color);
                default: return false;
                }
            }
        }

        private static class LeafNumber extends Predicate<CardRules> {
            protected enum CardField {
                CMC,
                POWER,
                TOUGHNESS,
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
                    case CMC: return op(card.getManaCost().getCMC(), operand);
                    case POWER: value = card.getIntPower(); return value >= 0 ? op(value, operand) : false;
                    case TOUGHNESS: value = card.getIntToughness(); return value >= 0 ? op(value, operand) : false;
                    default: return false;
                }
            }

            private boolean op(final int op1, final int op2) {
                switch (operator) {
                    case EQUALS: return op1 == op2;
                    case GREATER_THAN: return op1 > op2;
                    case GT_OR_EQUAL: return op1 >= op2;
                    case LESS_THAN: return op1 < op2;
                    case LT_OR_EQUAL: return op1 <= op2;
                    case NOT_EQUALS: return op1 != op2;
                    default: return false;
                }
            }
        }

        private static class PredicateCoreType extends Predicate<CardRules> {
            private final CardCoreType operand;
            private final boolean shouldBeEqual;

            @Override
            public boolean isTrue(final CardRules card) { return shouldBeEqual == card.getType().typeContains(operand); }

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
                    if (subject.setsPrinted.containsKey(s)) return true;
                }
                return false;
            }
        }

        public static class Presets {
            public static final Predicate<CardRules> isCreature = coreType(true, CardCoreType.Creature);
            public static final Predicate<CardRules> isArtifact = coreType(true, CardCoreType.Artifact);
            public static final Predicate<CardRules> isLand = coreType(true, CardCoreType.Land);
            public static final Predicate<CardRules> isBasicLand = new Predicate<CardRules>(){
                @Override public boolean isTrue(CardRules subject) { return subject.getType().isBasicLand(); } };
            public static final Predicate<CardRules> isPlaneswalker = coreType(true, CardCoreType.Planeswalker);
            public static final Predicate<CardRules> isInstant = coreType(true, CardCoreType.Instant);
            public static final Predicate<CardRules> isSorcery = coreType(true, CardCoreType.Sorcery);
            public static final Predicate<CardRules> isEnchantment = coreType(true, CardCoreType.Enchantment);

            public static final Predicate<CardRules> isNonLand = coreType(false, CardCoreType.Land);
            public static final Predicate<CardRules> isNonCreatureSpell = Predicate.compose(isCreature, PredicatesOp.NOR, isLand);

            public static final Predicate<CardRules> isWhite = isColor(CardColor.WHITE);
            public static final Predicate<CardRules> isBlue = isColor(CardColor.BLUE);
            public static final Predicate<CardRules> isBlack = isColor(CardColor.BLACK);
            public static final Predicate<CardRules> isRed = isColor(CardColor.RED);
            public static final Predicate<CardRules> isGreen = isColor(CardColor.GREEN);

            public static final Predicate<CardRules> isColorless = hasCntColors((byte) 0);
            public static final Predicate<CardRules> isMulticolor = hasAtLeastCntColors((byte) 2);

            public static final List<Predicate<CardRules>> colors = new ArrayList<Predicate<CardRules>>();
            static {
              colors.add(isWhite);
              colors.add(isBlue);
              colors.add(isBlack);
              colors.add(isRed);
              colors.add(isGreen);
              colors.add(isColorless);
            }
            
            public static final Predicate<CardRules> constantTrue = Predicate.getTrue(CardRules.class);

            // Think twice before using these, since rarity is a prop of printed card.
            public static final Predicate<CardRules> isInLatestSetCommon = rarityInCardsLatestSet(true, CardRarity.Common);
            public static final Predicate<CardRules> isInLatestSetUncommon = rarityInCardsLatestSet(true, CardRarity.Uncommon);
            public static final Predicate<CardRules> isInLatestSetRare = rarityInCardsLatestSet(true, CardRarity.Rare);
            public static final Predicate<CardRules> isInLatestSetMythicRare = rarityInCardsLatestSet(true, CardRarity.MythicRare);
            public static final Predicate<CardRules> isInLatestSetSpecial = rarityInCardsLatestSet(true, CardRarity.Special);
        }
    }
}
