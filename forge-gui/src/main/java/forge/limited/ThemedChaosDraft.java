package forge.limited;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.model.FModel;
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

    /**
     * @return Predicate to sort out editions not belonging to the chaos draft theme
     */
    public Predicate<CardEdition> getEditionFilter() {
        Predicate<CardEdition> filter;
        switch(tag) {
            case "DEFAULT":
                filter = DEFAULT_FILTER;
                break;
            case "MODERN":
            case "PIONEER":
            case "STANDARD":
                filter = getFormatFilter(tag);
                break;
            default:
                filter = themedFilter;
        }
        return filter;
    }

    /**
     * Filter to select editions by ChaosDraftThemes tag defined in edition files.
     * Tag must be defined in res/blockdata/chaosdraftthemes.txt
     */
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

    /**
     * @param formatName format to filter by, currently supported: MODERN, PIONEER, STANDARD
     * @return Filter to select editions belonging to a certain constructed format.
     */
    private Predicate<CardEdition> getFormatFilter(String formatName) {
        GameFormat.Collection formats = FModel.getFormats();
        GameFormat format;
        switch(formatName) {
            case "MODERN":
                format = formats.getModern();
                break;
            case "PIONEER":
                format = formats.getPioneer();
                break;
            case "STANDARD":
            default:
                format = formats.getStandard();
        }
        return new Predicate<CardEdition>() {
            @Override
            public boolean apply(final CardEdition cardEdition){
                return DEFAULT_FILTER.apply(cardEdition) && format.isSetLegal(cardEdition.getCode());
            }
        };
    }

    /**
     * Default filter that only allows actual sets that were printed as 15-card boosters
     */
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
        return (this.orderNumber != other.orderNumber)
                ? this.orderNumber - other.orderNumber
                : this.label.compareTo(other.label);
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
