package forge.screens.limited;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.FDeckEditor.EditorType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SGuiChoose;

public class NewDraftScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            Localizer.getInstance().getMessage("lblDraftText1") + "\n\n" +
                    Localizer.getInstance().getMessage("lblDraftText2") + "\n\n" +
                    Localizer.getInstance().getMessage("lblDraftText3")));

    public NewDraftScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);
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
        ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
            @Override
            public void run() {
                final LimitedPoolType poolType = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseDraftFormat"), LimitedPoolType.values());
                if (poolType == null) { return; }

                final BoosterDraft draft = BoosterDraft.createDraft(poolType);
                if (draft == null) { return; }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        LoadingOverlay.show(Localizer.getInstance().getMessage("lblLoadingNewDraft"), new Runnable() {
                            @Override
                            public void run() {
                                Forge.openScreen(new DraftingProcessScreen(draft, EditorType.Draft, null));
                            }
                        });
                    }
                });
            }
        });
    }
}
