package forge.screens.limited;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.deck.io.DeckPreferences;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.limited.SealedCardPoolGenerator;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FGroupBox;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FTextArea;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SGuiChoose;

public class LimitedScreen extends FScreen {
    private static final float PADDING = Utils.scale(10);

    private final FGroupBox grpDraft = add(new FGroupBox("Booster Draft") {
        @Override
        protected void layoutBox(float x, float y, float w, float h) {
            float buttonWidth = (w - PADDING) / 2;
            float buttonHeight = btnNewDraft.getAutoSizeBounds().height * 1.2f;
            float labelHeight = h - buttonHeight - PADDING;

            lblDraftDesc.setBounds(x, y, w, labelHeight);
            y += labelHeight + PADDING;
            btnNewDraft.setBounds(x, y, buttonWidth, buttonHeight);
            btnLoadDraft.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
        }
    });
    private final FTextArea lblDraftDesc = grpDraft.add(new FTextArea(false,
            "In Draft mode, three booster packs are rotated around eight players.\n\n" +
            "Build a deck from the cards you choose. The AI will do the same.\n\n" +
            "Then, play against any number of the AI opponents."));

    private final FButton btnNewDraft = grpDraft.add(new FButton("New Draft"));
    private final FButton btnLoadDraft = grpDraft.add(new FButton("Load Draft"));

    private final FGroupBox grpSealed = add(new FGroupBox("Sealed Deck") {
        @Override
        protected void layoutBox(float x, float y, float w, float h) {
            float buttonWidth = (w - PADDING) / 2;
            float buttonHeight = btnNewSealed.getAutoSizeBounds().height * 1.2f;
            float labelHeight = h - buttonHeight - PADDING;

            lblSealedDesc.setBounds(x, y, w, labelHeight);
            y += labelHeight + PADDING;
            btnNewSealed.setBounds(x, y, buttonWidth, buttonHeight);
            btnLoadSealed.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
        }
    });
    private final FTextArea lblSealedDesc = grpSealed.add(new FTextArea(false,
            "In Sealed mode, you build a deck from booster packs (maximum 10).\n\n" +
            "Build a deck from the cards you receive. A number of AI opponents will do the same.\n\n" +
            "Then, play against each of the AI opponents."));
    private final FButton btnNewSealed = grpSealed.add(new FButton("New Deck"));
    private final FButton btnLoadSealed = grpSealed.add(new FButton("Load Deck"));

    public LimitedScreen() {
        super("Draft / Sealed");

        lblDraftDesc.setFont(FSkinFont.get(12));
        lblDraftDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);
        btnNewDraft.setFont(FSkinFont.get(16));
        btnNewDraft.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
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
                                        Forge.openScreen(new DraftingProcessScreen(draft));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        btnLoadDraft.setFont(btnNewDraft.getFont());
        btnLoadDraft.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new LoadDraftScreen());
            }
        });
        lblSealedDesc.setFont(lblDraftDesc.getFont());
        lblSealedDesc.setTextColor(lblDraftDesc.getTextColor());
        btnNewSealed.setFont(btnNewDraft.getFont());
        btnNewSealed.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
                    @Override
                    public void run() {
                        final DeckGroup sealed = SealedCardPoolGenerator.generateSealedDeck(false);
                        if (sealed == null) { return; }

                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                DeckPreferences.setSealedDeck(sealed.getName());
                                Forge.openScreen(new FDeckEditor(EditorType.Sealed, sealed.getName(), false));
                                Forge.setBackScreen(new LoadSealedScreen()); //ensure pressing back goes to load sealed screen
                            }
                        });
                    }
                });
            }
        });
        btnLoadSealed.setFont(btnNewDraft.getFont());
        btnLoadSealed.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.openScreen(new LoadSealedScreen());
            }
        });
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float groupWidth = width - 2 * PADDING;
        float groupHeight = (height - startY - 3 * PADDING) / 2;
        grpDraft.setBounds(PADDING, startY + PADDING, groupWidth, groupHeight);
        grpSealed.setBounds(PADDING, startY + groupHeight + 2 * PADDING, groupWidth, groupHeight);
    }
}
