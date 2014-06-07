package forge.screens.draft;

import forge.assets.FSkinImage;
import forge.deck.CardPool;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;

public class DraftPackPage extends TabPage<DraftingProcessScreen> {
    private final CardManager lstPack = add(new CardManager(false));

    protected DraftPackPage() {
        super("Pack 1", FSkinImage.PACK);

        //hide filters and options panel so more of pack is visible by default
        lstPack.setHideViewOptions(1, true);
        lstPack.setAlwaysNonUnique(true);
        lstPack.setCaption("Cards");
        lstPack.setup(ItemManagerConfig.DRAFT_PACK);
        lstPack.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                parentScreen.getMainPage().addCard(lstPack.getSelectedItem());
                pickSelectedCard();
            }
        });
    }

    public void showChoices() {
        BoosterDraft draft = parentScreen.getDraft();
        CardPool pool = draft.nextChoice();
        int packNumber = draft.getCurrentBoosterIndex() + 1;
        caption = "Pack " + packNumber;
        lstPack.setPool(pool);
    }

    private void pickSelectedCard() {
        BoosterDraft draft = parentScreen.getDraft();
        draft.setChoice(lstPack.getSelectedItem());

        if (draft.hasNextChoice()) {
            showChoices();
        }
        else {
            draft.finishedDrafting();
            parentScreen.saveDraft();
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        lstPack.setBounds(0, 0, width, height);
    }
}
