package forge.itemmanager;

import forge.util.gui.SGuiChoose;

public class AdvancedSearch {

	private enum FilterOption {
        CARD_NAME("Name", FilterOperator.STRING_OPS, -1, -1),
        CARD_RULES_TEXT("Name", FilterOperator.STRING_OPS, -1, -1),
        CARD_TYPE("Name", FilterOperator.STRING_OPS, -1, -1),
        CARD_SUBTYPE("Name", FilterOperator.STRING_OPS, -1, -1),
        CARD_CMC("CMC", FilterOperator.NUMBER_OPS, 0, 20),
        CARD_GENERIC_COST("Generic Cost", FilterOperator.NUMBER_OPS, 0, 20),
        POWER("Power", FilterOperator.NUMBER_OPS, 0, 20),
        TOUGHNESS("Toughness", FilterOperator.NUMBER_OPS, 0, 20),
        TYPE("Type", FilterOperator.STRING_OPS, -1, -1),
        RULES_TEXT("Rules Text", FilterOperator.STRING_OPS, -1, -1),
        MANA_COST("Mana Cost", FilterOperator.STRING_OPS, -1, -1);

        private final String name;
        private final FilterOperator[] availableOps;
        private final int min, max;

        private FilterOption(String name0, FilterOperator[] availableOps0, int min0, int max0) {
            name = name0;
            availableOps = availableOps0;
            min = min0;
            max = max0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

	private enum FilterOperator {
		//Numeric operators
		EQUALS("is equal to", "{0}=={1}", 1),
	    NOT_EQUALS("is not equal to", "{0}!={1}", 1),
	    GREATER_THAN("is greater than", "{0}>{1}", 1),
	    LESS_THAN("is less than", "{0}<{1}", 1),
	    GT_OR_EQUAL("is greater than or equal to", "{0}>={1}", 1),
	    LT_OR_EQUAL("is less than or equal to", "{0}<={1}", 1),
	    BETWEEN_INCLUSIVE("is between (inclusive)", "{1}<={0}<={2}", 2),
	    BETWEEN_EXCLUSIVE("is between (exclusive)", "{1}<{0}<{2}", 2),

	    //String operators
		IS("is", "{0} is '{1}'", 1),
	    IS_NOT("is not", "{0} is not '{1}'", 1),
	    CONTAINS("contains", "{0} contains '{1}'", 1),
	    STARTS_WITH("starts with", "{0} starts with '{1}'", 1),
	    ENDS_WITH("ends with", "{0} ends with '{1}'", 1),

	    //Custom list operators
		IS_EXACTLY("is exactly", "{0} is exactly '{X}'", -1),
		IS_ANY("is any of", "{0} is '{X:or}'", -1),
		IS_ALL("is all of", "{0} is '{X:and}'", -1),
		IS_NONE("is none of", "{0} is not '{X:or}'", -1),
		INCLUDES_ANY("includes any of", "{0} includes '{X:or}'", -1),
		INCLUDES_ALL("includes all of", "{0} includes '{X:and}'", -1);

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
	    private final int valueCount;
	    
	    private FilterOperator(String caption0, String formatStr0, int valueCount0) {
	        caption = caption0;
	        formatStr = formatStr0;
	        valueCount = valueCount0;
	    }

	    @Override
	    public String toString() {
	        return caption;
	    }
	}

	private static abstract class FilterType<T> {
		public abstract T getValue(String message);
	}

	private static class NumberFilterType extends FilterType<Integer> {
		private final int min, max;

		public NumberFilterType(int min0, int max0) {
			min = min0;
			max = max0;
		}

		@Override
		public Integer getValue(String message) {
			return SGuiChoose.getInteger(message, min, max);
		}
	}
}
