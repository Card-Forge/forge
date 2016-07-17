package forge.screens.limited;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.FDeckEditor.EditorType;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SGuiChoose;

public class NewDraftScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            "In Draft mode, three booster packs are rotated around eight players.\n\n" +
            "Build a deck from the cards you choose. The AI will do the same.\n\n" +
            "Then, play against any number of the AI opponents."));

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
                final LimitedPoolType poolType = SGuiChoose.oneOrNone("Choose Draft Format", LimitedPoolType.values());
                if (poolType == null) { return; }

                final BoosterDraft draft = BoosterDraft.createDraft(poolType);
                if (draft == null) { return; }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        LoadingOverlay.show("Loading new draft...", new Runnable() {
                            @Override
                            public void run() {
                                Forge.openScreen(new DraftingProcessScreen(draft, EditorType.Draft));
                            }
                        });
                    }
                });
            }
        });
    }
}
