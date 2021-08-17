package forge.adventure.libgdxgui.screens.limited;

import forge.adventure.libgdxgui.Forge;
import forge.adventure.libgdxgui.assets.FSkinFont;
import forge.adventure.libgdxgui.deck.FDeckEditor.EditorType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gui.FThreads;
import forge.gui.util.SGuiChoose;
import forge.adventure.libgdxgui.screens.LaunchScreen;
import forge.adventure.libgdxgui.screens.LoadingOverlay;
import forge.adventure.libgdxgui.screens.home.NewGameMenu;
import forge.adventure.libgdxgui.toolbox.FLabel;
import forge.adventure.libgdxgui.toolbox.FTextArea;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.adventure.libgdxgui.util.Utils;

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
