package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;

public class DraftDeckPage extends TabPage {
    private final BoosterDraft draft;
    private final CardManager lstDeck = add(new CardManager(false));

    protected DraftDeckPage(BoosterDraft draft0) {
        super("Main (0)", FSkinImage.DECKLIST);

        draft = draft0;

        lstDeck.setCaption("Main Deck");
        lstDeck.setAlwaysNonUnique(true);
        lstDeck.setup(ItemManagerConfig.DRAFT_POOL);
    }

    @Override
    protected void doLayout(float width, float height) {
        lstDeck.setBounds(0, 0, width, height);
    }
}
