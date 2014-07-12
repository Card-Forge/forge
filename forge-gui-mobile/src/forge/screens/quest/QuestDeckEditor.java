package forge.screens.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;

import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.FDeckEditor;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;

public class QuestDeckEditor extends FDeckEditor {
    private Map<PaperCard, Integer> decksUsingMyCards;

    public QuestDeckEditor() {
        super(EditorType.Quest, "", false);
    }
    public QuestDeckEditor(DeckProxy editDeck) {
        super(EditorType.Quest, editDeck, true);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        decksUsingMyCards = countDecksForEachCard();
    }

    @Override
    protected Map<ColumnDef, ItemColumn> getColOverrides(ItemManagerConfig config) {
        Map<ColumnDef, ItemColumn> colOverrides = new HashMap<ColumnDef, ItemColumn>();
        switch (config) {
        case QUEST_EDITOR_POOL:
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            break;
        case QUEST_DECK_EDITOR:
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.NEW, FModel.getQuest().getCards().getFnNewCompare(), FModel.getQuest().getCards().getFnNewGet());
            ItemColumn.addColOverride(config, colOverrides, ColumnDef.DECKS,
                    new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
                        @Override
                        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
                            final Integer iValue = decksUsingMyCards.get(from.getKey());
                            return iValue == null ? Integer.valueOf(0) : iValue;
                        }
                    },
                    new Function<Entry<? extends InventoryItem, Integer>, Object>() {
                        @Override
                        public Object apply(final Entry<? extends InventoryItem, Integer> from) {
                            final Integer iValue = decksUsingMyCards.get(from.getKey());
                            return iValue == null ? "" : iValue.toString();
                        }
                    });
            break;
        default:
            colOverrides = null; //shouldn't happen
            break;
        }
        return colOverrides;
    }

    /**
     * Adds any card to the catalog and data pool.
     * 
     * @param card {@link forge.item.PaperCard}
     */
    public void addCheatCard(final PaperCard card, int qty) {
        getCatalogPage().addCard(card, qty);
        FModel.getQuest().getCards().getCardpool().add(card, qty);
    }

    // fills number of decks using each card
    private Map<PaperCard, Integer> countDecksForEachCard() {
        final Map<PaperCard, Integer> result = new HashMap<PaperCard, Integer>();
        for (final Deck deck : FModel.getQuest().getMyDecks()) {
            for (final Entry<PaperCard, Integer> e : deck.getMain()) {
                final PaperCard card = e.getKey();
                final Integer amount = result.get(card);
                result.put(card, Integer.valueOf(amount == null ? 1 : 1 + amount.intValue()));
            }
        }
        return result;
    }
}
