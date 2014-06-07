package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.screens.TabPageScreen.TabPage;

public class DraftMainPage extends TabPage<DraftingProcessScreen> {
    private final CardManager lstMain = add(new CardManager(false));

    protected DraftMainPage() {
        super("Main (0)", FSkinImage.DECKLIST);

        lstMain.setCaption("Main Deck");
        lstMain.setAlwaysNonUnique(true);
        lstMain.setup(ItemManagerConfig.DRAFT_POOL);
    }

    public void refresh() {
        lstMain.setPool(parentScreen.getDeck().getMain());
        updateCaption();
    }

    public void addCard(PaperCard card) {
        lstMain.addItem(card, 1);
        updateCaption();
    }

    private void updateCaption() {
        caption = "Main (" + parentScreen.getDeck().getMain().countAll() + ")";
    }

    @Override
    protected void doLayout(float width, float height) {
        lstMain.setBounds(0, 0, width, height);
    }
}
