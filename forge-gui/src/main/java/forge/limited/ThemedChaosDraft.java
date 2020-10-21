package forge.limited;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.card.CardEdition;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Themed chaos draft allows limiting the pool of available random boosters for a draft to a certain theme.
 */
public class ThemedChaosDraft implements Comparable<ThemedChaosDraft> {
    private final String tag;
    private final String label;
    private final int orderNumber;

    /**
     * @param tag Tag name used in edition files.
     * @param label Label used in user interface.
     * @param orderNumber Number used to order entries in user interface.
     */
    public ThemedChaosDraft(String tag, String label, int orderNumber) {
        this.tag = tag;
        this.label = label;
        this.orderNumber = orderNumber;
    }

    /**
     * @return theme tag
     */
    public String getTag() { return tag; }

    /**
     * @return theme label
     */
    public String getLabel() { return label; }

    /**
     * @return theme order number
     */
    public int getOrderNumber() { return orderNumber; }

    public Predicate<CardEdition> getEditionFilter() {
        if (!tag.equals("DEFAULT")) {
            System.out.println("Return themed filter for " + tag); // TODO remove
            return themedFilter;
        }
        System.out.println("Return default filter");  // TODO remove
        return DEFAULT_FILTER;
    }

    private final Predicate<CardEdition> themedFilter = new Predicate<CardEdition>() {
        @Override
        public boolean apply(final CardEdition cardEdition) {
            String[] themes = cardEdition.getChaosDraftThemes();
            for (String theme : themes) {
                if (tag.equals(theme)) return true;
            }
            return false;
        }
    };

    private static final Predicate<CardEdition> DEFAULT_FILTER = new Predicate<CardEdition>() {
        @Override
        public boolean apply(final CardEdition cardEdition) {
            boolean isExpansion = cardEdition.getType().equals(CardEdition.Type.EXPANSION);
            boolean isCoreSet = cardEdition.getType().equals(CardEdition.Type.CORE);
            boolean isReprintSet = cardEdition.getType().equals(CardEdition.Type.REPRINT);
            if (isExpansion || isCoreSet || isReprintSet) {
                // Only allow sets with 15 cards in booster packs
                if (cardEdition.hasBoosterTemplate()) {
                    final List<Pair<String, Integer>> slots = cardEdition.getBoosterTemplate().getSlots();
                    int boosterSize = 0;
                    for (Pair<String, Integer> slot : slots) {
                        boosterSize += slot.getRight();
                    }
                    return boosterSize == 15;
                }
            }
            return false;
        }
    };

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.label;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.tag == null) ? 0 : this.tag.hashCode());
        result = (prime * result) + ((this.label == null) ? 0 : this.label.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ThemedChaosDraft other) {
        int order = this.orderNumber - other.orderNumber;
        if (order != 0) return order;
        return this.label.compareTo(other.label);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final ThemedChaosDraft other = (ThemedChaosDraft) obj;
        if (!this.label.equals(other.label)) {
            return false;
        }
        if (!this.tag.equals(other.tag)) {
            return false;
        }
        return true;
    }

    public static final Function<ThemedChaosDraft, String> FN_GET_TAG = new Function<ThemedChaosDraft, String>() {
        @Override
        public String apply(ThemedChaosDraft themedChaosBooster) {
            return themedChaosBooster.getTag();
        }
    };

    public static class Reader extends StorageReaderFile<ThemedChaosDraft> {
        public Reader(String pathname) {
            super(pathname, ThemedChaosDraft.FN_GET_TAG);
        }

        @Override
        protected ThemedChaosDraft read(String line, int idx) {
            final String[] sParts = TextUtil.splitWithParenthesis(line, ',', 3);
            int orderNumber = Integer.parseInt(sParts[0].trim(), 10);
            String tag = sParts[1].trim();
            String label = sParts[2].trim();
            return new ThemedChaosDraft(tag, label, orderNumber);
        }
    }
}
