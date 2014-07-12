package forge.screens.quest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.DeckProxy;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestUtil;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class QuestDecksScreen extends FScreen {
    private static final float PADDING = FOptionPane.PADDING;

    private final DeckManager lstDecks = add(new DeckManager(GameType.Quest));
    private final FButton btnNewDeck = add(new FButton("New Deck"));
    private final FButton btnEditDeck = add(new FButton("Edit Deck"));

    private final FLabel lblInfo = add(new FLabel.Builder()
        .align(HAlignment.CENTER).font(FSkinFont.get(16))
        .text("Build or select a deck").build());

    private final FEventHandler onDeckSelectionChanged = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            DeckProxy deck = lstDecks.getSelectedItem();
            if (deck != null) {
                FModel.getQuestPreferences().setPref(QPref.CURRENT_DECK, deck.toString());
            }
            else {
                FModel.getQuestPreferences().setPref(QPref.CURRENT_DECK, QPref.CURRENT_DECK.getDefault());
            }
            FModel.getQuestPreferences().save();
        }
    };

    private boolean needRefreshOnActivate = true;

    public QuestDecksScreen() {
        super("Quest Decks", QuestMenu.getMenu());

        lstDecks.setup(ItemManagerConfig.QUEST_DECKS);
        lstDecks.setItemActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                Forge.back();
            }
        });

        btnNewDeck.setFont(FSkinFont.get(16));
        btnNewDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must run in game thread to prevent blocking UI thread
                    @Override
                    public void run() {
                        if (!QuestUtil.checkActiveQuest("Create a Deck.")) {
                            return;
                        }
                        QuestDeckEditor editor = new QuestDeckEditor();
                        editor.setSaveHandler(new FEventHandler() {
                            @Override
                            public void handleEvent(FEvent e) {
                                //ensure list is refreshed if new deck is saved
                                needRefreshOnActivate = true;
                            }
                        });
                        Forge.openScreen(editor);
                    }
                });
            }
        });
        btnEditDeck.setFont(btnNewDeck.getFont());
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editSelectedDeck();
            }
        });
    }

    @Override
    public void onActivate() {
        if (needRefreshOnActivate) {
            needRefreshOnActivate = false;
            refreshDecks();
        }
    }

    public void refreshDecks() {
        lstDecks.setSelectionChangedHandler(null); //set to null temporarily

        final QuestController qData = FModel.getQuest();
        boolean hasQuest = qData.getAssets() != null;
        // Retrieve and set all decks
        lstDecks.setPool(DeckProxy.getAllQuestDecks(hasQuest ? qData.getMyDecks() : null));
        lstDecks.setup(ItemManagerConfig.QUEST_DECKS);

        // Look through list for preferred deck from prefs
        final DeckProxy deck = hasQuest ? lstDecks.stringToItem(FModel.getQuestPreferences().getPref(QPref.CURRENT_DECK)) : null;
        if (deck != null) {
            lstDecks.setSelectedItem(deck);
        }
        else {
            lstDecks.setSelectedIndex(0);
            onDeckSelectionChanged.handleEvent(null); //update prefs immediately
        }

        lstDecks.setSelectionChangedHandler(onDeckSelectionChanged);
    }

    private void editSelectedDeck() {
        final DeckProxy deck = lstDecks.getSelectedItem();
        if (deck == null) { return; }

        needRefreshOnActivate = true;
        Forge.openScreen(new QuestDeckEditor(deck));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING / 2;
        float w = width - 2 * PADDING;

        lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
        y += lblInfo.getHeight();

        float buttonWidth = (w - FDeckChooser.PADDING) / 2;
        float buttonHeight = btnNewDeck.getAutoSizeBounds().height * 1.2f;
        float listHeight = height - buttonHeight - y - FDeckChooser.PADDING;

        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + FDeckChooser.PADDING;
        btnNewDeck.setBounds(x, y, buttonWidth, buttonHeight);
        btnEditDeck.setBounds(x + buttonWidth + FDeckChooser.PADDING, y, buttonWidth, buttonHeight);
    }
}
