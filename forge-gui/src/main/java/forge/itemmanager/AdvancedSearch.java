package forge.itemmanager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
        CARD_EXPANSION("Expansion", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<CardEdition>(FModel.getMagicDb().getSortedEditions())),
        CARD_FORMAT("Format", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<GameFormat>((List<GameFormat>)FModel.getFormats().getOrderedList())),
        CARD_COLOR("Color", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(MagicColor.Constant.COLORS_AND_COLORLESS)),
        CARD_TYPE("Type", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedCoreAndSuperTypes())),
        CARD_SUB_TYPE("Subtype", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<String>(CardType.getSortedSubTypes())),
        CARD_CMC("CMC", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_GENERIC_COST("Generic Cost", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_POWER("Power", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_TOUGHNESS("Toughness", PaperCard.class, FilterOperator.NUMBER_OPS, new NumericValueSelector(0, 20)),
        CARD_MANA_COST("Mana Cost", PaperCard.class, FilterOperator.STRING_OPS, new StringValueSelector()),
        CARD_RARITY("Rarity", PaperCard.class, FilterOperator.CUSTOM_LIST_OPS, new CustomListValueSelector<CardRarity>(Arrays.asList(CardRarity.values())));

        private final String name;
        private final Class<? extends InventoryItem> type;
        private final FilterOperator[] operatorOptions;
        private final FilterValueSelector<?> valueSelector;

        private FilterOption(String name0, Class<? extends InventoryItem> type0, FilterOperator[] operatorOptions0, FilterValueSelector<?> valueSelector0) {
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
        IS_EXACTLY("is exactly", "%1$s is exactly %2$s", FilterValueCount.MANY),
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
        public static final FilterOperator[] CUSTOM_LIST_OPS = new FilterOperator[] {
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
    
    public static <T extends InventoryItem> Filter<T> getFilter(Class<? super T> type) {
        //build list of filter options based on ItemManager type
        List<FilterOption> options = new ArrayList<FilterOption>();
        for (FilterOption opt : FilterOption.values()) {
            if (opt.type == type) {
                options.add(opt);
            }
        }

        final FilterOption option = SGuiChoose.oneOrNone("Select a filter type", options);
        if (option == null) { return null; }

        final FilterOperator operator = SGuiChoose.oneOrNone("Select an operator for " + option.name, option.operatorOptions);
        if (operator == null) { return null; }

        final String message = option.name + " " + operator.caption + " ?";
        final List<?> values = option.valueSelector.getValues(message, operator.valueCount);
        if (values == null) { return null; }

        return new Filter<T>(option, operator, values);
    }

    public static class Filter<T extends InventoryItem> {
        private final FilterOption option;
        private final FilterOperator operator;
        private final List<?> values;

        private Filter(FilterOption option0, FilterOperator operator0, List<?> values0) {
            option = option0;
            operator = operator0;
            values = values0;
        }

        @Override
        public String toString() {
            switch (operator.valueCount) {
            case ONE:
                return String.format(operator.formatStr, option.name, values.get(0));
            case TWO:
                return String.format(operator.formatStr, option.name, values.get(0), values.get(1));
            case MANY:
                return String.format(operator.formatStr, option.name, StringUtils.join(values, ", "));
            case MANY_OR:
                return String.format(operator.formatStr, option.name, formatValues(", ", "or"));
            case MANY_AND:
                return String.format(operator.formatStr, option.name, formatValues(", ", "and"));
            }
            return "";
        }

        private String formatValues(String delim, String finalDelim) {
            switch (values.size()) {
            case 1:
                return values.get(0).toString();
            case 2:
                return values.get(0) + " " + finalDelim + " " + values.get(1);
            default:
                String result = StringUtils.join(values, delim);
                int index = result.lastIndexOf(delim) + delim.length();
                return result.substring(0, index) + finalDelim + " " + result.substring(index);
            }
        }
    }

    private enum FilterValueCount {
        ONE,
        TWO,
        MANY,
        MANY_OR,
        MANY_AND
    }

    private static abstract class FilterValueSelector<T> {
        public abstract List<T> getValues(String message, FilterValueCount count);
    }

    private static class NumericValueSelector extends FilterValueSelector<Integer> {
        private final int min, max;

        public NumericValueSelector(int min0, int max0) {
            min = min0;
            max = max0;
        }

        @Override
        public List<Integer> getValues(String message, FilterValueCount count) {
            String msg = message;
            if (count == FilterValueCount.TWO) {
                msg += " (Lower Bound)";
            }
            Integer lowerBound = SGuiChoose.getInteger(msg, min, max);
            if (lowerBound == null) { return null; }

            List<Integer> values = new ArrayList<Integer>();
            values.add(lowerBound);
            if (count == FilterValueCount.TWO) { //prompt for upper bound if needed
                msg = message + " (Upper Bound)";
                Integer upperBound = SGuiChoose.getInteger(msg, lowerBound, max);
                if (upperBound == null) { return null; }
                values.add(upperBound);
            }
            return values;
        }
    }

    private static class StringValueSelector extends FilterValueSelector<String> {
        public StringValueSelector() {
        }

        @Override
        public List<String> getValues(String message, FilterValueCount count) {
            String value = SOptionPane.showInputDialog("", message);
            if (value == null) { return null; }

            List<String> values = new ArrayList<String>();
            values.add(value);
            return values;
        }
    }

    private static class CustomListValueSelector<T> extends FilterValueSelector<T> {
        private final Collection<T> choices;

        public CustomListValueSelector(Collection<T> choices0) {
            choices = choices0;
        }

        @Override
        public List<T> getValues(String message, FilterValueCount count) {
            List<T> values = SGuiChoose.getChoices(message, 0, choices.size(), choices);
            if (values == null || values.size() == 0) {
                return null;
            }
            return values;
        }
    }
}
