package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;

public class DraftDeckPage extends TabPage {
    private final BoosterDraft draft;

    protected DraftDeckPage(BoosterDraft draft0) {
        super("Deck", FSkinImage.DECKLIST);

        draft = draft0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
