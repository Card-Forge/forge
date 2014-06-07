package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.TabPageScreen.TabPage;

public class DraftSideboardPage extends TabPage<DraftingProcessScreen> {
    private final CardManager lstSideboard = add(new CardManager(false));

    protected DraftSideboardPage() {
        super("Side (0)", FSkinImage.FLASHBACK);

        lstSideboard.setCaption("Sideboard");
        lstSideboard.setAlwaysNonUnique(true);
        lstSideboard.setup(ItemManagerConfig.DRAFT_POOL);
    }

    public void refresh() {
        lstSideboard.setPool(parentScreen.getDeck().get(DeckSection.Sideboard));
        updateCaption();
    }

    public void addCard(PaperCard card) {
        lstSideboard.addItem(card, 1);
        updateCaption();
    }

    private void updateCaption() {
        caption = "Side (" + parentScreen.getDeck().get(DeckSection.Sideboard).countAll() + ")";
    }

    @Override
    protected void doLayout(float width, float height) {
        lstSideboard.setBounds(0, 0, width, height);
    }
}
