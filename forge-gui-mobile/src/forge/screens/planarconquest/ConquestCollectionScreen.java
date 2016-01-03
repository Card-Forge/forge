package forge.screens.planarconquest;

import com.google.common.base.Predicate;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ComboBoxFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;

public class ConquestCollectionScreen extends FScreen {
    private final CollectionManager lstCollection = add(new CollectionManager());

    public ConquestCollectionScreen() {
        super("", ConquestMenu.getMenu());

        ConquestData model = FModel.getConquest().getModel();
        ItemManagerConfig config = ItemManagerConfig.CONQUEST_COLLECTION;
        lstCollection.setup(config, model.getColOverrides(config));
    }

    @Override
    public void onActivate() {
        setHeaderCaption(FModel.getConquest().getModel().getName());
        refreshCards();
    }

    public void refreshCards() {
        CardPool pool = new CardPool();
        pool.add(FModel.getConquest().getModel().getUnlockedCards());
        lstCollection.setPool(pool, true);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float w = width - 2 * x;
        lstCollection.setBounds(x, startY, w, height - startY - ItemFilter.PADDING);
    }

    private static class CollectionManager extends CardManager {
        public CollectionManager() {
            super(false);
            setCaption("Collection");
        }

        @Override
        protected void addDefaultFilters() {
            addFilter(new CardColorFilter(this));
            addFilter(new CardOriginFilter(this));
            addFilter(new CardTypeFilter(this));
        }
    }

    private static class CardOriginFilter extends ComboBoxFilter<PaperCard, ConquestPlane> {
        public CardOriginFilter(ItemManager<? super PaperCard> itemManager0) {
            super("All Planes", ConquestPlane.values(), itemManager0);
        }

        @Override
        public ItemFilter<PaperCard> createCopy() {
            CardOriginFilter copy = new CardOriginFilter(itemManager);
            copy.filterValue = filterValue;
            return copy;
        }

        @Override
        protected Predicate<PaperCard> buildPredicate() {
            return new Predicate<PaperCard>() {
                @Override
                public boolean apply(PaperCard input) {
                    if (filterValue == null) {
                        return true;
                    }
                    return filterValue.getCardPool().contains(input);
                }
            };
        }
    }
}
