package forge.screens.quest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.FThreads;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor.EditorType;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.quest.IQuestTournamentView;
import forge.quest.QuestEventDraft;
import forge.quest.QuestTournamentController;
import forge.quest.QuestDraftUtils.Mode;
import forge.quest.data.QuestEventDraftContainer;
import forge.screens.limited.DraftingProcessScreen;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class QuestTournamentsScreen extends QuestLaunchScreen implements IQuestTournamentView {
    //Select Tournament panel
    private final SelectTournamentPanel pnlSelectTournament = add(new SelectTournamentPanel());

    private final FLabel lblCredits = pnlSelectTournament.add(new FLabel.Builder().icon(FSkinImage.QUEST_COINSTACK)
            .iconScaleFactor(0.75f).font(FSkinFont.get(16)).build());

    private final FLabel btnSpendToken = pnlSelectTournament.add(new FLabel.ButtonBuilder().text("Spend Token (0)").build());

    private final FLabel lblInfo = pnlSelectTournament.add(new FLabel.Builder().text("Select a tournament to join:")
            .align(HAlignment.CENTER).font(FSkinFont.get(16)).build());

    private final FLabel lblNoTournaments = pnlSelectTournament.add(new FLabel.Builder()
            .align(HAlignment.CENTER).text("There are no tournaments available at this time.").insets(Vector2.Zero)
            .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlTournaments = pnlSelectTournament.add(new QuestEventPanel.Container());

    //Prepare Deck panel
    private final PrepareDeckPanel pnlPrepareDeck = add(new PrepareDeckPanel());

    private final FButton btnEditDeck = pnlPrepareDeck.add(new FButton("Edit Deck"));
    private final FButton btnLeaveTournament = pnlPrepareDeck.add(new FButton("Leave Tournament"));
    private final CardManager deckViewer = pnlPrepareDeck.add(new CardManager(false));

    //Tournament Active panel
    private final TournamentActivePanel pnlTournamentActive = add(new TournamentActivePanel());

    //Results labels
    private static final FSkinFont RESULTS_FONT = FSkinFont.get(15);
    private static final Vector2 RESULTS_INSETS = new Vector2(2 * PADDING, 0);
    private final FLabel lblFirst = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblSecond = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblThird = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblFourth = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();

    private Mode mode;
    private final QuestTournamentController controller;

    public QuestTournamentsScreen() {
        super();
        controller = new QuestTournamentController(this);
        btnSpendToken.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
                    @Override
                    public void run() {
                        controller.spendToken();
                    }
                });
            }
        });
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editDeck(true);
            }
        });
        btnLeaveTournament.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
                    @Override
                    public void run() {
                        controller.endTournamentAndAwardPrizes();
                    }
                });
            }
        });
        deckViewer.setCaption("Main Deck");
        deckViewer.setup(ItemManagerConfig.QUEST_DRAFT_DECK_VIEWER);
        setMode(Mode.SELECT_TOURNAMENT);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        height -= startY;
        pnlSelectTournament.setBounds(0, startY, width, height);
        pnlPrepareDeck.setBounds(0, startY, width, height);
        pnlTournamentActive.setBounds(0, startY, width, height);
    }

    @Override
    protected String getGameType() {
        return "Tournaments";
    }

    @Override
    public void onUpdate() {
        controller.update();
        if (mode == Mode.PREPARE_DECK) {
            Deck deck = getDeck();
            if (deck != null) {
                deckViewer.setPool(deck.getMain());
            }
            else {
                deckViewer.setPool(new CardPool());
            }
        }
    }

    @Override
    protected void updateHeaderCaption() {
        if (mode == Mode.PREPARE_DECK) {
            setHeaderCaption(FModel.getQuest().getName() + " - " + getGameType() + "\nDraft - " + FModel.getQuest().getAchievements().getCurrentDraft().getTitle());
        }
        else {
            super.updateHeaderCaption();
        }
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode0) {
        if (mode == mode0) { return; }
        mode = mode0;
        pnlSelectTournament.setVisible(mode == Mode.SELECT_TOURNAMENT);
        pnlPrepareDeck.setVisible(mode == Mode.PREPARE_DECK);
        pnlTournamentActive.setVisible(mode == Mode.TOURNAMENT_ACTIVE);
        updateHeaderCaption();
    }

    @Override
    public void populate() {
        //not needed
    }

    @Override
    public void updateEventList(QuestEventDraftContainer events) {
        pnlTournaments.clear();

        if (events != null) {
            for (QuestEventDraft event : events) {
                pnlTournaments.add(new QuestEventPanel(event, pnlTournaments));
            }
        }

        pnlTournaments.revalidate();

        boolean hasTournaments = pnlTournaments.getChildCount() > 0;
        pnlTournaments.setVisible(hasTournaments);
        lblNoTournaments.setVisible(!hasTournaments);
    }

    @Override
    public void updateTournamentBoxLabel(String playerID, int iconID, int box, boolean first) {

    }

    @Override
    public void startDraft(BoosterDraft draft) {
        Forge.openScreen(new DraftingProcessScreen(draft, EditorType.QuestDraft, controller));
    }
    
    private Deck getDeck() {
        DeckGroup deckGroup = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
        if (deckGroup != null) {
            return deckGroup.getHumanDeck();
        }
        return null;
    }

    public void editDeck(boolean isExistingDeck) {
        Deck deck = getDeck();
        if (deck != null) {
            if (isExistingDeck) {
                Forge.openScreen(new QuestDraftDeckEditor(deck.getName()));
            }
            else {
                Forge.openScreen(new QuestDraftDeckEditor(deck));
            }
        }
    }

    @Override
    protected void startMatch() {
        FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
            @Override
            public void run() {
                switch (mode) {
                case SELECT_TOURNAMENT:
                    controller.startDraft();
                    break;
                case PREPARE_DECK:
                    controller.startTournament();
                    break;
                case TOURNAMENT_ACTIVE:
                    controller.startNextMatch();
                    break;
                default:
                    break;
                }
            }
        });
    }

    @Override
    public FLabel getLblCredits() {
        return lblCredits;
    }

    @Override
    public FLabel getLblFirst() {
        return lblFirst;
    }

    @Override
    public FLabel getLblSecond() {
        return lblSecond;
    }

    @Override
    public FLabel getLblThird() {
        return lblThird;
    }

    @Override
    public FLabel getLblFourth() {
        return lblFourth;
    }

    @Override
    public FLabel getBtnSpendToken() {
        return btnSpendToken;
    }

    @Override
    public FButton getBtnLeaveTournament() {
        return btnLeaveTournament;
    }

    private class SelectTournamentPanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            float gap = Utils.scale(2);
            float y = gap; //move credits label down a couple pixels so it looks better

            float halfWidth = width / 2;
            lblCredits.setBounds(0, y, halfWidth, lblCredits.getAutoSizeBounds().height);
            btnSpendToken.setBounds(halfWidth, y, halfWidth - gap, lblCredits.getHeight());
            y += lblCredits.getHeight() + gap;

            float x = PADDING;
            float w = width - 2 * PADDING;
            lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
            y += lblInfo.getHeight() + gap;
            lblNoTournaments.setBounds(x, y, w, lblNoTournaments.getAutoSizeBounds().height);
            pnlTournaments.setBounds(x, y, w, height - y);
        }
    }

    private class PrepareDeckPanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            float y = PADDING;
            float buttonWidth = (width - 3 * PADDING) / 2;
            btnEditDeck.setBounds(PADDING, y, buttonWidth, FTextField.getDefaultHeight());
            btnLeaveTournament.setBounds(btnEditDeck.getRight() + PADDING, y, buttonWidth, btnEditDeck.getHeight());
            y += btnEditDeck.getHeight() + PADDING - ItemFilter.PADDING;
            deckViewer.setBounds(0, y, width, height - y);
        }
    }

    private class TournamentActivePanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            
        }
    }
}
