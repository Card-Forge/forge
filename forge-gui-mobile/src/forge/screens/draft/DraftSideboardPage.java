package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;

public class DraftSideboardPage extends TabPage {
    private final BoosterDraft draft;
    private final CardManager lstSideboard = add(new CardManager(false));

    protected DraftSideboardPage(BoosterDraft draft0) {
        super("Sideboard", FSkinImage.FLASHBACK);

        draft = draft0;

        lstSideboard.setCaption("Sideboard");
        lstSideboard.setAlwaysNonUnique(true);
        lstSideboard.setup(ItemManagerConfig.DRAFT_POOL);
    }

    @Override
    protected void doLayout(float width, float height) {
        lstSideboard.setBounds(0, 0, width, height);
    }
}
