package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;

public class CurrentPackPage extends TabPage {
    private final BoosterDraft draft;

    protected CurrentPackPage(BoosterDraft draft0) {
        super("Pack 1", FSkinImage.PACK);

        draft = draft0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }
}
