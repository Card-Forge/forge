package forge.screens.limited;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.FDeckEditor.EditorType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gui.FThreads;
import forge.gui.util.SGuiChoose;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class NewDraftScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            Forge.getLocalizer().getMessage("lblDraftText1") + "\n\n" +
                    Forge.getLocalizer().getMessage("lblDraftText2") + "\n\n" +
                    Forge.getLocalizer().getMessage("lblDraftText3")));

    public NewDraftScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.getInlineLabelColor());
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - PADDING;
        lblDesc.setBounds(x, y, w, h);
    }

    @Override
    protected void startMatch() {
        //must run in game thread to prevent blocking UI thread
        ThreadUtil.invokeInGameThread(() -> {
            final LimitedPoolType poolType = SGuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblChooseDraftFormat"), LimitedPoolType.values(true));
            if (poolType == null) { return; }

            final BoosterDraft draft = BoosterDraft.createDraft(poolType);
            if (draft == null) { return; }

            FThreads.invokeInEdtLater(() -> LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingNewDraft"), true, () -> Forge.openScreen(new DraftingProcessScreen(draft, EditorType.Draft, null))));
        });
    }
}
