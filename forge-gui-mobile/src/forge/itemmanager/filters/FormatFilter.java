package forge.itemmanager.filters;

import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.item.InventoryItem;
import forge.itemmanager.ItemManager;
import forge.model.FModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public abstract class FormatFilter<T extends InventoryItem> extends ListLabelFilter<T> {
    protected boolean allowReprints = true;
    protected final Set<GameFormat> formats = new HashSet<GameFormat>();

    public FormatFilter(ItemManager<? super T> itemManager0) {
        super(itemManager0);
    }
    public FormatFilter(ItemManager<? super T> itemManager0, GameFormat format0) {
        super(itemManager0);
        this.formats.add(format0);
    }

    @Override
    protected String getTooltip() {
        Set<String> sets = new HashSet<String>();
        Set<String> bannedCards = new HashSet<String>();

        for (GameFormat format : this.formats) {
            List<String> formatSets = format.getAllowedSetCodes();
            if (formatSets != null) {
                sets.addAll(formatSets);
            }
            List<String> formatBannedCards = format.getBannedCardNames();
            if (formatBannedCards != null) {
                bannedCards.addAll(formatBannedCards);
            }
        }

        //use HTML tooltips so we can insert line breaks
        int lastLen = 0;
        int lineLen = 0;
        StringBuilder tooltip = new StringBuilder("<html>Sets:");
        if (sets.isEmpty()) {
            tooltip.append(" All");
        }
        else {
            CardEdition.Collection editions = FModel.getMagicDb().getEditions();

            for (String code : sets) {
                // don't let a single line get too long
                if (50 < lineLen) {
                    tooltip.append("<br>");
                    lastLen += lineLen;
                    lineLen = 0;
                }

                CardEdition edition = editions.get(code);
                tooltip.append(" ").append(edition.getName()).append(" (").append(code).append("),");
                lineLen = tooltip.length() - lastLen;
            }

            // chop off last comma
            tooltip.delete(tooltip.length() - 1, tooltip.length());

            if (this.allowReprints) {
                tooltip.append("<br><br>Allowing identical cards from other sets");
            }
        }

        if (!bannedCards.isEmpty()) {
            tooltip.append("<br><br>Banned:");
            lastLen += lineLen;
            lineLen = 0;

            for (String cardName : bannedCards) {
                // don't let a single line get too long
                if (50 < lineLen) {
                    tooltip.append("<br>");
                    lastLen += lineLen;
                    lineLen = 0;
                }

                tooltip.append(" ").append(cardName).append(";");
                lineLen = tooltip.length() - lastLen;
            }

            // chop off last semicolon
            tooltip.delete(tooltip.length() - 1, tooltip.length());
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    @Override
    public void reset() {
        this.formats.clear();
        this.updateLabel();
    }

    public static <T extends InventoryItem> boolean canAddFormat(GameFormat format, FormatFilter<T> existingFilter) {
        return existingFilter == null || !existingFilter.formats.contains(format);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean merge(ItemFilter<?> filter) {
        FormatFilter<T> formatFilter = (FormatFilter<T>)filter;
        this.formats.addAll(formatFilter.formats);
        this.allowReprints = formatFilter.allowReprints;
        return true;
    }

    @Override
    protected String getCaption() {
        return "Format";
    }

    @Override
    protected int getCount() {
        return this.formats.size();
    }

    @Override
    protected Iterable<String> getList() {
        Set<String> strings = new HashSet<String>();
        for (GameFormat f : this.formats) {
            strings.add(f.getName());
        }
        return strings;
    }
}
