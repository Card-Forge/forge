package forge.itemmanager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

public class AdvancedSearch {
    private enum FilterOption {
        CARD_NAME("Name", PaperCard.class, FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_RULES_TEXT("Rules Text", PaperCard.class, FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_SET("Set", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListValueSelector<CardEdition>(FModel.getMagicDb().getSortedEditions(), CardEdition.FN_GET_CODE)),
        CARD_FORMAT("Format", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListValueSelector<GameFormat>((List<GameFormat>)FModel.getFormats().getOrderedList())),
        CARD_COLOR("Color", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListValueSelector<String>(MagicColor.Constant.COLORS_AND_COLORLESS)),
        CARD_TYPE("Type", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedCoreAndSuperTypes())),
        CARD_SUB_TYPE("Subtype", PaperCard.class, FilterOperator.MULTI_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedSubTypes())),
        CARD_CMC("CMC", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_GENERIC_COST("Generic Cost", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_POWER("Power", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_TOUGHNESS("Toughness", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_MANA_COST("Mana Cost", PaperCard.class, FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_RARITY("Rarity", PaperCard.class, FilterOperator.SINGLE_LIST_OPS, new CustomListValueSelector<CardRarity>(Arrays.asList(CardRarity.values())));

        private final String name;
        private final Class<? extends InventoryItem> type;
        private final FilterOperator[] operatorOptions;
        private final FilterValueSelector valueSelector;

        private FilterOption(String name0, Class<? extends InventoryItem> type0, FilterOperator[] operatorOptions0, FilterValueSelector valueSelector0) {
            name = name0;
            type = type0;
            operatorOptions = operatorOptions0;
            valueSelector = valueSelector0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum FilterOperator {
        //Numeric operators
        EQUALS("is equal to", "%1$s=%2$d", FilterValueCount.ONE),
        NOT_EQUALS("is not equal to", "%1$s<>%2$d", FilterValueCount.ONE),
        GREATER_THAN("is greater than", "%1$s>%2$d", FilterValueCount.ONE),
        LESS_THAN("is less than", "%1$s<%2$d", FilterValueCount.ONE),
        GT_OR_EQUAL("is greater than or equal to", "%1$s>=%2$d", FilterValueCount.ONE),
        LT_OR_EQUAL("is less than or equal to", "%1$s<=%2$d", FilterValueCount.ONE),
        BETWEEN_INCLUSIVE("is between (inclusive)", "%2$d<=%1$s<=%3$d", FilterValueCount.TWO),
        BETWEEN_EXCLUSIVE("is between (exclusive)", "%2$d<%1$s<%3$d", FilterValueCount.TWO),

        //String operators
        IS("is", "%1$s is '%2$s'", FilterValueCount.ONE),
        IS_NOT("is not", "%1$s is not '%2$s'", FilterValueCount.ONE),
        CONTAINS("contains", "%1$s contains '%2$s'", FilterValueCount.ONE),
        STARTS_WITH("starts with", "%1$s starts with '%2$s'", FilterValueCount.ONE),
        ENDS_WITH("ends with", "%1$s ends with '%2$s'", FilterValueCount.ONE),

        //Custom list operators
        IS_EXACTLY("is exactly", "%1$s is %2$s", FilterValueCount.MANY),
        IS_ANY("is any of", "%1$s is %2$s", FilterValueCount.MANY_OR),
        IS_ALL("is all of", "%1$s is %2$s", FilterValueCount.MANY_AND),
        IS_NONE("is none of", "%1$s is not %2$s", FilterValueCount.MANY_OR),
        INCLUDES_ANY("includes any of", "%1$s includes %2$s", FilterValueCount.MANY_OR),
        INCLUDES_ALL("includes all of", "%1$s includes %2$s", FilterValueCount.MANY_AND);

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
            IS_EXACTLY, IS_ANY, IS_ALL, IS_NONE, INCLUDES_ANY, INCLUDES_ALL
        };

        private final String caption, formatStr;
        private final FilterValueCount valueCount;

        private FilterOperator(String caption0, String formatStr0, FilterValueCount valueCount0) {
            caption = caption0;
            formatStr = formatStr0;
            valueCount = valueCount0;
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

    private static abstract class FilterValueSelector {
        public abstract <T extends InventoryItem> Filter<T> createFilter(String message, FilterOption option, FilterOperator operator);
    }

    private static class NumericValueSelector extends FilterValueSelector {
        private final int min, max;

        public NumericValueSelector(int min0, int max0) {
            min = min0;
            max = max0;
        }

        @Override
        public <T extends InventoryItem> Filter<T> createFilter(String message, FilterOption option, FilterOperator operator) {
            String msg = message;
            if (operator.valueCount == FilterValueCount.TWO) {
                msg += " (Lower Bound)";
            }
            Integer lowerBound = SGuiChoose.getInteger(msg, min, max);
            if (lowerBound == null) { return null; }

            final String caption;
            if (operator.valueCount == FilterValueCount.TWO) { //prompt for upper bound if needed
                msg = message + " (Upper Bound)";
                Integer upperBound = SGuiChoose.getInteger(msg, lowerBound, max);
                if (upperBound == null) { return null; }

                caption = String.format(operator.formatStr, option.name, lowerBound, upperBound);
            }
            else {
                caption = String.format(operator.formatStr, option.name, lowerBound);
            }

            return new Filter<T>(option, operator, caption, null);
        }
    }

    private static class StringValueSelector extends FilterValueSelector {
        public StringValueSelector() {
        }

        @Override
        public <T extends InventoryItem> Filter<T> createFilter(String message, FilterOption option, FilterOperator operator) {
            String value = SOptionPane.showInputDialog("", message);
            if (value == null) { return null; }

            final String caption = String.format(operator.formatStr, option.name, value);

            return new Filter<T>(option, operator, caption, null);
        }
    }

    private static class CustomListValueSelector<V> extends FilterValueSelector {
        private final Collection<V> choices;
        private final Function<V, String> toShortString, toLongString;

        public CustomListValueSelector(Collection<V> choices0) {
            this(choices0, null, null);
        }
        public CustomListValueSelector(Collection<V> choices0, Function<V, String> toShortString0) {
            this(choices0, toShortString0, null);
        }
        public CustomListValueSelector(Collection<V> choices0, Function<V, String> toShortString0, Function<V, String> toLongString0) {
            choices = choices0;
            toShortString = toShortString0;
            toLongString = toLongString0;
        }

        @Override
        public <T extends InventoryItem> Filter<T> createFilter(String message, FilterOption option, FilterOperator operator) {
            int max = choices.size();
            if (operator == FilterOperator.IS_EXACTLY && option.operatorOptions == FilterOperator.SINGLE_LIST_OPS) {
                max = 1;
            }
            List<V> values = SGuiChoose.getChoices(message, 0, max, choices);
            if (values == null || values.isEmpty()) {
                return null;
            }

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
            

            final String caption = String.format(operator.formatStr, option.name, valuesStr);

            return new Filter<T>(option, operator, caption, null);
        }

        private String formatValues(List<V> values, String delim, String finalDelim) {
            int valueCount = values.size();
            switch (valueCount) {
            case 1:
                return formatValue(values.get(0));
            case 2:
                return formatValue(values.get(0)) + finalDelim + " " + formatValue(values.get(1));
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

        private String formatValue(V value) {
            if (toShortString == null) {
                return value.toString();
            }
            return toShortString.apply(value);
        }
    }

    public static <T extends InventoryItem> Filter<T> getFilter(Class<? super T> type, Filter<T> editFilter) {
        //build list of filter options based on ItemManager type
        List<FilterOption> options = new ArrayList<FilterOption>();
        for (FilterOption opt : FilterOption.values()) {
            if (opt.type == type) {
                options.add(opt);
            }
        }

        final FilterOption defaultOption = editFilter == null ? null : editFilter.option;
        final FilterOption option = SGuiChoose.oneOrNone("Select a filter type", options, defaultOption, null);
        if (option == null) { return null; }

        final FilterOperator defaultOperator = option == defaultOption ? editFilter.operator : null;
        final FilterOperator operator = SGuiChoose.oneOrNone("Select an operator for " + option.name, option.operatorOptions, defaultOperator, null);
        if (operator == null) { return null; }

        final String message = option.name + " " + operator.caption + " ?";
        return option.valueSelector.createFilter(message, option, operator);
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
