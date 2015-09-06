package forge.itemmanager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.card.CardType.CoreType;
import forge.card.CardType.Supertype;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

public class AdvancedSearch {
    private enum FilterOption {
        NONE("(none)", null, null, null),
        CARD_NAME("Name", PaperCard.class, FilterOperator.STRING_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                return input.getName();
            }
        }),
        CARD_RULES_TEXT("Rules Text", PaperCard.class, FilterOperator.STRING_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                return input.getRules().getOracleText();
            }
        }),
        CARD_SET("Set", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<PaperCard, CardEdition>(FModel.getMagicDb().getSortedEditions(), CardEdition.FN_GET_CODE) {
            @Override
            protected CardEdition getItemValue(PaperCard input) {
                return FModel.getMagicDb().getEditions().get(input.getEdition());
            }
        }),
        CARD_FORMAT("Format", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<PaperCard, GameFormat>((List<GameFormat>)FModel.getFormats().getOrderedList()) {
            @Override
            protected GameFormat getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<GameFormat> getItemValues(PaperCard input) {
                return FModel.getFormats().getAllFormatsOfCard(input);
            }
        }),
        CARD_COLOR("Color", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new ColorEvaluator<PaperCard>() {
            @Override
            protected MagicColor.Color getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(PaperCard input) {
                return input.getRules().getColor().toEnumSet();
            }
        }),
        CARD_COLOR_IDENTITY("Color Identity", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new ColorEvaluator<PaperCard>() {
            @Override
            protected MagicColor.Color getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<MagicColor.Color> getItemValues(PaperCard input) {
                return input.getRules().getColorIdentity().toEnumSet();
            }
        }),
        CARD_TYPE("Type", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, String>(CardType.getSortedCoreAndSuperTypes()) {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                final CardType type = input.getRules().getType();
                final Set<String> types = new HashSet<String>();
                for (Supertype t : type.getSupertypes()) {
                    types.add(t.name());
                }
                for (CoreType t : type.getCoreTypes()) {
                    types.add(t.name());
                }
                return types;
            }
        }),
        CARD_SUB_TYPE("Subtype", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListEvaluator<PaperCard, String>(CardType.getSortedSubTypes()) {
            @Override
            protected String getItemValue(PaperCard input) {
                throw new RuntimeException("getItemValues should be called instead");
            }
            @Override
            protected Set<String> getItemValues(PaperCard input) {
                return (Set<String>)input.getRules().getType().getSubtypes();
            }
        }),
        CARD_CMC("CMC", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                return input.getRules().getManaCost().getCMC();
            }
        }),
        CARD_GENERIC_COST("Generic Cost", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                return input.getRules().getManaCost().getGenericCost();
            }
        }),
        CARD_POWER("Power", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                CardRules rules = input.getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntPower();
                }
                return null;
            }
        }),
        CARD_TOUGHNESS("Toughness", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericEvaluator<PaperCard>(0, 20) {
            @Override
            protected Integer getItemValue(PaperCard input) {
                CardRules rules = input.getRules();
                if (rules.getType().isCreature()) {
                    return rules.getIntToughness();
                }
                return null;
            }
        }),
        CARD_MANA_COST("Mana Cost", PaperCard.class, FilterOperator.STRING_OPS, new StringEvaluator<PaperCard>() {
            @Override
            protected String getItemValue(PaperCard input) {
                return input.getRules().getManaCost().toString();
            }
        }),
        CARD_RARITY("Rarity", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListEvaluator<PaperCard, CardRarity>(Arrays.asList(CardRarity.values()), CardRarity.FN_GET_LONG_NAME, CardRarity.FN_GET_LONG_NAME) {
            @Override
            protected CardRarity getItemValue(PaperCard input) {
                return input.getRarity();
            }
        });

        private final String name;
        private final Class<? extends InventoryItem> type;
        private final FilterOperator[] operatorOptions;
        private final FilterEvaluator<? extends InventoryItem, ?> evaluator;

        private FilterOption(String name0, Class<? extends InventoryItem> type0, FilterOperator[] operatorOptions0, FilterEvaluator<? extends InventoryItem, ?> evaluator0) {
            name = name0;
            type = type0;
            operatorOptions = operatorOptions0;
            evaluator = evaluator0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum FilterOperator {
        //Numeric operators
        EQUALS("is equal to", "%1$s = %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() == values.get(0).intValue();
                }
                return false;
            }
        }),
        NOT_EQUALS("is not equal to", "%1$s <> %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() != values.get(0).intValue();
                }
                return true;
            }
        }),
        GREATER_THAN("is greater than", "%1$s > %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() > values.get(0).intValue();
                }
                return false;
            }
        }),
        LESS_THAN("is less than", "%1$s < %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() < values.get(0).intValue();
                }
                return false;
            }
        }),
        GT_OR_EQUAL("is greater than or equal to", "%1$s >= %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() >= values.get(0).intValue();
                }
                return false;
            }
        }),
        LT_OR_EQUAL("is less than or equal to", "%1$s <= %2$d", FilterValueCount.ONE, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    return input.intValue() <= values.get(0).intValue();
                }
                return false;
            }
        }),
        BETWEEN_INCLUSIVE("is between (inclusive)", "%2$d <= %1$s <= %3$d", FilterValueCount.TWO, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    int inputValue = input.intValue();
                    return values.get(0).intValue() <= inputValue && inputValue <= values.get(1).intValue();
                }
                return false;
            }
        }),
        BETWEEN_EXCLUSIVE("is between (exclusive)", "%2$d < %1$s < %3$d", FilterValueCount.TWO, new OperatorEvaluator<Integer>() {
            @Override
            public boolean apply(Integer input, List<Integer> values) {
                if (input != null) {
                    int inputValue = input.intValue();
                    return values.get(0).intValue() < inputValue && inputValue < values.get(1).intValue();
                }
                return false;
            }
        }),

        //String operators
        IS("is", "%1$s is '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().equals(values.get(0).toLowerCase());
                }
                return false;
            }
        }),
        IS_NOT("is not", "%1$s is not '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return !input.toLowerCase().equals(values.get(0).toLowerCase());
                }
                return true;
            }
        }),
        CONTAINS("contains", "%1$s contains '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().indexOf(values.get(0).toLowerCase()) != -1;
                }
                return false;
            }
        }),
        STARTS_WITH("starts with", "%1$s starts with '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().startsWith(values.get(0).toLowerCase());
                }
                return false;
            }
        }),
        ENDS_WITH("ends with", "%1$s ends with '%2$s'", FilterValueCount.ONE, new OperatorEvaluator<String>() {
            @Override
            public boolean apply(String input, List<String> values) {
                if (input != null) {
                    return input.toLowerCase().endsWith(values.get(0).toLowerCase());
                }
                return false;
            }
        }),

        //Custom list operators
        IS_EXACTLY("is exactly", "%1$s is %2$s", FilterValueCount.MANY, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null && values.size() == 1) {
                    return input.equals(values.get(0));
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null && inputs.size() == values.size()) {
                    for (Object value : values) {
                        if (!inputs.contains(value)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }),
        IS_ANY("is any of", "%1$s is %2$s", FilterValueCount.MANY_OR, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null) {
                    for (Object value : values) {
                        if (input.equals(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (inputs.contains(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }),
        IS_ALL("is all of", "%1$s is %2$s", FilterValueCount.MANY_AND, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null && values.size() == 1) {
                    return input.equals(values.get(0));
                }
                return false;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (!inputs.contains(value)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }),
        IS_NONE("is none of", "%1$s is not %2$s", FilterValueCount.MANY_OR, new OperatorEvaluator<Object>() {
            @Override
            public boolean apply(Object input, List<Object> values) {
                if (input != null) {
                    for (Object value : values) {
                        if (input.equals(value)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            @Override
            public boolean apply(Set<Object> inputs, List<Object> values) {
                if (inputs != null) {
                    for (Object value : values) {
                        if (inputs.contains(value)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        });

        public static final FilterOperator[] NUMBER_OPS = new FilterOperator[] {
            EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GT_OR_EQUAL, LT_OR_EQUAL, BETWEEN_INCLUSIVE, BETWEEN_EXCLUSIVE
        };
        public static final FilterOperator[] STRING_OPS = new FilterOperator[] {
            IS, IS_NOT, CONTAINS, STARTS_WITH, ENDS_WITH
        };
        public static final FilterOperator[] SINGLE_LIST_OPS = new FilterOperator[] {
            IS_EXACTLY, IS_ANY, IS_NONE
        };
        public static final FilterOperator[] MULTI_LIST_OPS = new FilterOperator[] {
            IS_EXACTLY, IS_ANY, IS_ALL, IS_NONE
        };

        private final String caption, formatStr;
        private final FilterValueCount valueCount;
        private final OperatorEvaluator<?> evaluator;

        private FilterOperator(String caption0, String formatStr0, FilterValueCount valueCount0, OperatorEvaluator<?> evaluator0) {
            caption = caption0;
            formatStr = formatStr0;
            valueCount = valueCount0;
            evaluator = evaluator0;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    private enum FilterValueCount {
        ONE,
        TWO,
        MANY,
        MANY_OR,
        MANY_AND
    }

    private static abstract class OperatorEvaluator<V> {
        protected abstract boolean apply(V input, List<V> values);

        protected boolean apply(Set<V> inputs, List<V> values) {
            return false; //available for options that have multiple inputs
        }
    }

    private static abstract class FilterEvaluator<T extends InventoryItem, V> {
        @SuppressWarnings("unchecked")
        public final Filter<T> createFilter(String message, FilterOption option, FilterOperator operator) {
            final List<V> values = getValues(message, option, operator);
            if (values == null || values.isEmpty()) { return null; }

            String caption = getCaption(values, option, operator);

            final OperatorEvaluator<V> evaluator = (OperatorEvaluator<V>)operator.evaluator;
            Predicate<T> predicate;
            if (option.operatorOptions == FilterOperator.MULTI_LIST_OPS) {
                predicate = new Predicate<T>() {
                    @Override
                    public boolean apply(T input) {
                        return evaluator.apply(getItemValues(input), values);
                    }
                };
            }
            else {
                predicate = new Predicate<T>() {
                    @Override
                    public boolean apply(T input) {
                        return evaluator.apply(getItemValue(input), values);
                    }
                };
            }
            return new Filter<T>(option, operator, caption, predicate);
        }

        protected abstract List<V> getValues(String message, FilterOption option, FilterOperator operator);
        protected abstract String getCaption(List<V> values, FilterOption option, FilterOperator operator);
        protected abstract V getItemValue(T input);

        protected Set<V> getItemValues(T input) { //available for options that have multiple inputs
            return null;
        }
    }

    private static abstract class NumericEvaluator<T extends InventoryItem> extends FilterEvaluator<T, Integer> {
        private final int min, max;

        public NumericEvaluator(int min0, int max0) {
            min = min0;
            max = max0;
        }

        @Override
        protected List<Integer> getValues(String message, FilterOption option, FilterOperator operator) {
            String msg = message;
            if (operator.valueCount == FilterValueCount.TWO) {
                msg += " (Lower Bound)";
            }
            Integer lowerBound = SGuiChoose.getInteger(msg, min, max);
            if (lowerBound == null) { return null; }

            final List<Integer> values = new ArrayList<Integer>();
            values.add(lowerBound);

            if (operator.valueCount == FilterValueCount.TWO) { //prompt for upper bound if needed
                msg = message + " (Upper Bound)";
                Integer upperBound = SGuiChoose.getInteger(msg, lowerBound, max);
                if (upperBound == null) { return null; }

                values.add(upperBound);
            }
            return values;
        }

        @Override
        protected String getCaption(List<Integer> values, FilterOption option, FilterOperator operator) {
            if (operator.valueCount == FilterValueCount.TWO) {
                return String.format(operator.formatStr, option.name, values.get(0), values.get(1));
            }
            return String.format(operator.formatStr, option.name, values.get(0));
        }
    }

    private static abstract class StringEvaluator<T extends InventoryItem> extends FilterEvaluator<T, String> {
        public StringEvaluator() {
        }

        @Override
        protected List<String> getValues(String message, FilterOption option, FilterOperator operator) {
            String value = SOptionPane.showInputDialog("", message);
            if (value == null) { return null; }

            List<String> values = new ArrayList<String>();
            values.add(value);
            return values;
        }

        @Override
        protected String getCaption(List<String> values, FilterOption option, FilterOperator operator) {
            return String.format(operator.formatStr, option.name, values.get(0));
        }
    }

    private static abstract class CustomListEvaluator<T extends InventoryItem, V> extends FilterEvaluator<T, V> {
        private final Collection<V> choices;
        private final Function<V, String> toShortString, toLongString;

        public CustomListEvaluator(Collection<V> choices0) {
            this(choices0, null, null);
        }
        public CustomListEvaluator(Collection<V> choices0, Function<V, String> toShortString0) {
            this(choices0, toShortString0, null);
        }
        public CustomListEvaluator(Collection<V> choices0, Function<V, String> toShortString0, Function<V, String> toLongString0) {
            choices = choices0;
            toShortString = toShortString0;
            toLongString = toLongString0;
        }

        @Override
        protected List<V> getValues(String message, FilterOption option, FilterOperator operator) {
            int max = choices.size();
            if (operator == FilterOperator.IS_EXACTLY && option.operatorOptions == FilterOperator.SINGLE_LIST_OPS) {
                max = 1;
            }
            return SGuiChoose.getChoices(message, 0, max, choices, null, toLongString);
        }

        @Override
        protected String getCaption(List<V> values, FilterOption option, FilterOperator operator) {
            String valuesStr;
            switch (operator.valueCount) {
            case MANY:
                valuesStr = formatValues(values, " ", " ");
                break;
            case MANY_OR:
                valuesStr = formatValues(values, ", ", " or ");
                break;
            case MANY_AND:
            default:
                valuesStr = formatValues(values, ", ", " and ");
                break;
            }
            return String.format(operator.formatStr, option.name, valuesStr);
        }

        protected String formatValues(List<V> values, String delim, String finalDelim) {
            int valueCount = values.size();
            switch (valueCount) {
            case 1:
                return formatValue(values.get(0));
            case 2:
                return formatValue(values.get(0)) + finalDelim + formatValue(values.get(1));
            default:
                int lastValueIdx = valueCount - 1;
                String result = formatValue(values.get(0));
                for (int i = 1; i < lastValueIdx; i++) {
                    result += delim + formatValue(values.get(i));
                }
                result += delim.trim() + finalDelim + formatValue(values.get(lastValueIdx));
                return result;
            }
        }

        protected String formatValue(V value) {
            if (toShortString == null) {
                return value.toString();
            }
            return toShortString.apply(value);
        }
    }

    private static abstract class ColorEvaluator<T extends InventoryItem> extends CustomListEvaluator<T, MagicColor.Color> {
        public ColorEvaluator() {
            super(Arrays.asList(MagicColor.Color.values()), MagicColor.FN_GET_SYMBOL);
        }

        @Override
        protected String getCaption(List<MagicColor.Color> values, FilterOption option, FilterOperator operator) {
            if (operator == FilterOperator.IS_EXACTLY) {
                //handle special case for formatting colors with no spaces in between for is exactly operator
                return String.format(operator.formatStr, option.name, formatValues(values, "", ""));
            }
            return super.getCaption(values, option, operator);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends InventoryItem> Filter<T> getFilter(Class<? super T> type, Filter<T> editFilter) {
        //build list of filter options based on ItemManager type
        List<FilterOption> options = new ArrayList<FilterOption>();
        if (editFilter != null) {
            options.add(FilterOption.NONE); //provide option to clear existing filter
        }
        for (FilterOption opt : FilterOption.values()) {
            if (opt.type == type) {
                options.add(opt);
            }
        }

        final FilterOption defaultOption = editFilter == null ? null : editFilter.option;
        final FilterOption option = SGuiChoose.oneOrNone("Select a filter type", options, defaultOption, null);
        if (option == null) { return editFilter; }

        if (option == FilterOption.NONE) { return null; } //allow user to clear filter by selecting "(none)"

        final FilterOperator defaultOperator = option == defaultOption ? editFilter.operator : null;
        final FilterOperator operator = SGuiChoose.oneOrNone("Select an operator for " + option.name, option.operatorOptions, defaultOperator, null);
        if (operator == null) { return editFilter; }

        final String message = option.name + " " + operator.caption + " ?";
        Filter<T> filter = (Filter<T>)option.evaluator.createFilter(message, option, operator);
        if (filter == null) {
            filter = editFilter;
        }
        return filter;
    }

    public static class Filter<T extends InventoryItem> {
        private final FilterOption option;
        private final FilterOperator operator;
        private final String caption;
        private final Predicate<T> predicate;

        private Filter(FilterOption option0, FilterOperator operator0, String caption0, Predicate<T> predicate0) {
            option = option0;
            operator = operator0;
            caption = caption0;
            predicate = predicate0;
        }

        public Predicate<T> getPredicate() {
            return predicate;
        }

        @Override
        public String toString() {
            return caption;
        }
    }
}
