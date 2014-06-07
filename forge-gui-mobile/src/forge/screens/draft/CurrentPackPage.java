package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class CurrentPackPage extends TabPage {
    private final BoosterDraft draft;
    private final CardManager lstPack = add(new CardManager(false));

    protected CurrentPackPage(BoosterDraft draft0) {
        super("Pack 1", FSkinImage.PACK);

        draft = draft0;

        //hide filters and options panel so more of pack is visible by default
        lstPack.setHideViewOptions(1, true);
        lstPack.setAlwaysNonUnique(true);
        lstPack.setup(ItemManagerConfig.DRAFT_PACK);
        lstPack.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                draft.setChoice(lstPack.getSelectedItem());
            }
        });
    }

    @Override
    protected void doLayout(float width, float height) {
        lstPack.setBounds(0, 0, width, height);
    }
}
