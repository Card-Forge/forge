package forge.screens.quest;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.deck.DeckProxy;
import forge.deck.DeckgenUtil;
import forge.deck.FDeckChooser;
import forge.deck.FDeckViewer;
import forge.game.GameType;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gui.FThreads;
import forge.itemmanager.DeckManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FTextField;
import forge.util.ThreadUtil;

public class QuestDecksScreen extends FScreen {
    private static final float PADDING = FDeckChooser.PADDING;

    private final DeckManager lstDecks = add(new DeckManager(GameType.Quest));
    private final FButton btnNewDeck = add(new FButton(Forge.getLocalizer().getMessage("lblNewDeck")));
    private final FButton btnEditDeck = add(new FButton(Forge.getLocalizer().getMessage("btnEditDeck")));
    private final FButton btnViewDeck = add(new FButton(Forge.getLocalizer().getMessage("lblViewDeck")));
    private final FButton btnRandom = add(new FButton(Forge.getLocalizer().getMessage("lblRandomDeck")));

    private final FLabel lblInfo = add(new FLabel.Builder()
        .align(Align.center).font(FSkinFont.get(16))
        .text(Forge.getLocalizer().getMessage("lblBuildorselectadeck")).build());

    private final FEventHandler onDeckSelectionChanged = e -> {
        DeckProxy deck = lstDecks.getSelectedItem();
        if (deck != null) {
            FModel.getQuest().setCurrentDeck(deck.toString());
        }
        else {
            FModel.getQuest().setCurrentDeck(QPref.CURRENT_DECK.getDefault());
        }
        FModel.getQuest().save();
    };

    private boolean needRefreshOnActivate = true;
    public boolean commanderMode = false;

    public QuestDecksScreen() {
        super("", QuestMenu.getMenu());

        lstDecks.setup(ItemManagerConfig.QUEST_DECKS);
        lstDecks.setItemActivateHandler(e -> Forge.back());

        //must run in game thread to prevent blocking UI thread
        btnNewDeck.setCommand(e -> ThreadUtil.invokeInGameThread(() -> {
            if (!QuestUtil.checkActiveQuest(Forge.getLocalizer().getMessage("lblCreateaDeck"))) {
                return;
            }
            FThreads.invokeInEdtLater(() -> {
                QuestDeckEditor editor = new QuestDeckEditor(commanderMode);
                editor.setSaveHandler(e1 -> {
                    //ensure list is refreshed if new deck is saved
                    needRefreshOnActivate = true;
                });
                Forge.openScreen(editor);
            });
        }));
        btnEditDeck.setCommand(e -> editSelectedDeck());
        btnViewDeck.setCommand(e -> {
            final DeckProxy deck = lstDecks.getSelectedItem();
            if (deck != null) {
                FDeckViewer.show(deck.getDeck());
            }
        });
        btnRandom.setCommand(e -> DeckgenUtil.randomSelect(lstDecks));
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Decks\n(" + FModel.getQuest().getRank() + ")");

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
        final DeckProxy deck = hasQuest ? lstDecks.stringToItem(FModel.getQuest().getCurrentDeck()) : null;
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

        /*preload deck to cache*/
        ImageCache.preloadCache(deck.getDeck());

        needRefreshOnActivate = true;
        Forge.openScreen(new QuestDeckEditor(deck, commanderMode));
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING / 2;
        float w = width - 2 * PADDING;

        lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
        y += lblInfo.getHeight();

        float buttonWidth = (w - PADDING) / 2;
        float buttonHeight = FTextField.getDefaultHeight();
        float listHeight = height - 2 * buttonHeight - y - 3 * PADDING;

        lstDecks.setBounds(x, y, w, listHeight);
        y += listHeight + PADDING;
        btnNewDeck.setBounds(x, y, buttonWidth, buttonHeight);
        btnEditDeck.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
        y += buttonHeight + PADDING;
        btnViewDeck.setBounds(x, y, buttonWidth, buttonHeight);
        btnRandom.setBounds(x + buttonWidth + PADDING, y, buttonWidth, buttonHeight);
    }
}
