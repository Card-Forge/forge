package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;

public class DraftSideboardPage extends TabPage {
    private final BoosterDraft draft;

    protected DraftSideboardPage(BoosterDraft draft0) {
        super("Sideboard", FSkinImage.FLASHBACK);

        draft = draft0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
