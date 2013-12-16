package forge.gui.toolbox.itemmanager.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Singletons;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardFormatFilter extends ListLabelFilter<PaperCard> {
    protected boolean allowReprints = true;
    protected final Set<GameFormat> formats = new HashSet<GameFormat>();

    public CardFormatFilter(ItemManager<PaperCard> itemManager0) {
        super(itemManager0);
    }
    public CardFormatFilter(ItemManager<PaperCard> itemManager0, GameFormat format0) {
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
            CardEdition.Collection editions = Singletons.getMagicDb().getEditions();

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
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        CardFormatFilter copy = new CardFormatFilter(itemManager);
        copy.formats.addAll(this.formats);
        return copy;
    }

    public static boolean canAddFormat(GameFormat format, ItemFilter<PaperCard> existingFilter) {
        return existingFilter == null || !((CardFormatFilter)existingFilter).formats.contains(format);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean merge(ItemFilter filter) {
        CardFormatFilter cardFormatFilter = (CardFormatFilter)filter;
        this.formats.addAll(cardFormatFilter.formats);
        this.allowReprints = cardFormatFilter.allowReprints;
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

    @Override
    public final Predicate<PaperCard> buildPredicate() {
        List<Predicate<PaperCard>> predicates = new ArrayList<Predicate<PaperCard>>();
        for (GameFormat f : this.formats) {
            predicates.add(allowReprints ? f.getFilterRules() : f.getFilterPrinted());
        }
        return Predicates.or(predicates);
    }
}
