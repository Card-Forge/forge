package forge.screens.planarconquest;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.CardColorFilter;
import forge.itemmanager.filters.CardRarityFilter;
import forge.itemmanager.filters.CardTypeFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.model.FModel;
import forge.planarconquest.ConquestController;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane;
import forge.screens.FScreen;

public class ConquestAEtherScreen extends FScreen {
    private final AEtherManager lstAEther = add(new AEtherManager());

    public ConquestAEtherScreen() {
        super("", ConquestMenu.getMenu());

        lstAEther.setup(ItemManagerConfig.CONQUEST_AETHER);
    }

    @Override
    public void onActivate() {
        ConquestController conquest = FModel.getConquest();
        ConquestData model = conquest.getModel();
        ConquestPlane plane = model.getCurrentPlane();

        setHeaderCaption(conquest.getName() + "\nPlane - " + plane.getName());

        CardPool pool = new CardPool();
        for (PaperCard card : plane.getCardPool().getAllCards()) {
            if (!model.hasUnlockedCard(card)) {
                pool.add(card);
            }
        }
        lstAEther.setPool(pool, true);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = ItemFilter.PADDING;
        float w = width - 2 * x;
        lstAEther.setBounds(x, startY, w, height - startY - ItemFilter.PADDING);
    }

    private static class AEtherManager extends CardManager {
        public AEtherManager() {
            super(false);
            setCaption("The AEther");
        }

        @Override
        protected void addDefaultFilters() {
            addFilter(new CardColorFilter(this));
            addFilter(new CardRarityFilter(this));
            addFilter(new CardTypeFilter(this));
        }
    }
}
