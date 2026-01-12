package forge.itemmanager;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import forge.Graphics;
import forge.StaticData;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.item.PaperCard;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardFormatFilter;
import forge.itemmanager.filters.CardSearchFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.TextSearchFilter;
import forge.toolbox.FList;
import forge.toolbox.FList.CompactModeHandler;

/** 
 * ItemManager for cards
 */
public class CardManager extends ItemManager<PaperCard> {
    public CardManager(boolean wantUnique0) {
        super(PaperCard.class, wantUnique0);
    }

    @Override
    protected void addDefaultFilters() {
        addDefaultFilters(this);
    }

    @Override
    protected TextSearchFilter<PaperCard> createSearchFilter() {
        return createSearchFilter(this);
    }

    @Override
    protected AdvancedSearchFilter<PaperCard> createAdvancedSearchFilter() {
        return createAdvancedSearchFilter(this);
    }

    protected void onCardLongPress(int index, Entry<PaperCard, Integer> value, float x, float y) {
        CardZoom.show(model.getOrderedList(), index, CardManager.this);
    }

    /* Static overrides shared with SpellShopManager*/

    public static void addDefaultFilters(final ItemManager<? super PaperCard> itemManager) {
        itemManager.addFilter(new CardColorFilter(itemManager));
        itemManager.addFilter(new CardFormatFilter(itemManager));
        itemManager.addFilter(new CardTypeFilter(itemManager));
    }

    public static TextSearchFilter<PaperCard> createSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new CardSearchFilter(itemManager);
    }

    public static AdvancedSearchFilter<PaperCard> createAdvancedSearchFilter(final ItemManager<? super PaperCard> itemManager) {
        return new AdvancedSearchFilter<>(itemManager);
    }

    @Override
    protected Iterable<Entry<PaperCard, Integer>> getUnique(Iterable<Entry<PaperCard, Integer>> items) {
        //TO-maybe-DO: Share logic between this and identical method in desktop.
        ListMultimap<String, Entry<PaperCard, Integer>> entriesByName = Multimaps.newListMultimap(
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER), Lists::newArrayList);
        for (Entry<PaperCard, Integer> item : items) {
            final String cardName = item.getKey().getName();
            entriesByName.put(cardName, item);
        }

        // Now we're ready to go on with retrieving cards to be returned
        Map<PaperCard, Integer> cardsMap = new HashMap<>();
        for (String cardName : entriesByName.keySet()) {
            List<Entry<PaperCard, Integer>> entries = entriesByName.get(cardName);

            ListMultimap<CardEdition, Entry<PaperCard, Integer>> entriesByEdition = Multimaps.newListMultimap(new HashMap<>(), Lists::newArrayList);
            for (Entry<PaperCard, Integer> entry : entries) {
                CardEdition ed = StaticData.instance().getCardEdition(entry.getKey().getEdition());
                if (ed != null)
                    entriesByEdition.put(ed, entry);
            }
            if (entriesByEdition.isEmpty())
                continue;  // skip card

            // Try to retain only those editions accepted by the current Card Art Preference Policy
            Predicate<CardEdition> editionPredicate = ed -> StaticData.instance().getCardArtPreference().accept(ed);
            List<CardEdition> acceptedEditions = entriesByEdition.keySet().stream().filter(editionPredicate).collect(Collectors.toList());

            // If policy too strict, fall back to getting all editions.
            if (acceptedEditions.isEmpty())
                // Policy is too strict for current PaperCard in Entry. Remove any filter
                acceptedEditions.addAll(entriesByEdition.keySet());

            Entry<PaperCard, Integer> cardEntry = getCardEntryToAdd(entriesByEdition, acceptedEditions);
            if (cardEntry != null)
                cardsMap.put(cardEntry.getKey(), cardEntry.getValue());
        }
        return cardsMap.entrySet();
    }

    // Select the Card Art Entry to add, based on current Card Art Preference Order.
    // This method will prefer the entry currently having an image. If that's not the case,
    private Entry<PaperCard, Integer> getCardEntryToAdd(ListMultimap<CardEdition, Entry<PaperCard, Integer>> entriesByEdition,
                                                        List<CardEdition> acceptedEditions) {
        // Use standard sort + index, for better performance!
        Collections.sort(acceptedEditions);
        if (StaticData.instance().cardArtPreferenceIsLatest())
            Collections.reverse(acceptedEditions);
        Iterator<CardEdition> editionIterator = acceptedEditions.iterator();
        Entry<PaperCard, Integer> candidateEntry = null;
        Entry<PaperCard, Integer> firstCandidateEntryFound = null;
        while (editionIterator.hasNext() && candidateEntry == null){
            CardEdition cardEdition = editionIterator.next();
            // These are now the entries to add to Cards Map
            List<Entry<PaperCard, Integer>> cardEntries = entriesByEdition.get(cardEdition);
            Iterator<Entry<PaperCard, Integer>> entriesIterator = cardEntries.iterator();
            candidateEntry = entriesIterator.hasNext() ? entriesIterator.next() : null;
            if (candidateEntry != null && firstCandidateEntryFound == null)
                firstCandidateEntryFound = candidateEntry;  // save reference to the first candidate entry found!
            while ((candidateEntry == null || !candidateEntry.getKey().hasImage()) && entriesIterator.hasNext()) {
                candidateEntry = entriesIterator.next();
                if (firstCandidateEntryFound == null)
                    firstCandidateEntryFound = candidateEntry;
            }

            if (candidateEntry != null && !candidateEntry.getKey().hasImage())
                candidateEntry = null;  // resetting for next edition
        }
        return candidateEntry != null ? candidateEntry : firstCandidateEntryFound;
    }

    @Override
    public ItemRenderer getListItemRenderer(final CompactModeHandler compactModeHandler) {
        return new CardListItemRenderer(compactModeHandler);
    }

    protected class CardListItemRenderer extends ItemRenderer {
        private final CompactModeHandler compactModeHandler;

        public CardListItemRenderer(CompactModeHandler compactModeHandler) {
            this.compactModeHandler = compactModeHandler;
        }

        @Override
        public float getItemHeight() {
            return CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode());
        }

        @Override
        public void drawValue(Graphics g, Entry<PaperCard, Integer> value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, value.getKey(), isInfinite() ? 0 : value.getValue(), getItemSuffix(value), x, y, w, h, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean tap(Integer index, Entry<PaperCard, Integer> value, float x, float y, int count) {
            return CardRenderer.cardListItemTap(model.getOrderedList(), index, CardManager.this, x, y, count, compactModeHandler.isCompactMode());
        }

        @Override
        public boolean longPress(Integer index, Entry<PaperCard, Integer> value, float x, float y) {
            if (CardRenderer.cardListItemTap(model.getOrderedList(), index, CardManager.this, x, y, 1, compactModeHandler.isCompactMode())) {
                return true; //avoid calling onCardLongPress if user long presses on card art
            }
            onCardLongPress(index, value, x, y);
            return true;
        }

        @Override
        public boolean allowPressEffect(FList<Entry<PaperCard, Integer>> list, float x, float y) {
            //only allow press effect if right of card art
            return x > CardRenderer.getCardListItemHeight(compactModeHandler.isCompactMode()) * CardRenderer.CARD_ART_RATIO;
        }
    }
}
