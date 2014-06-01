package forge.screens.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.BoxedProductCardListViewer;
import forge.gui.CardListViewer;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.itemmanager.DeckManager;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEventDraft;
import forge.quest.data.QuestAchievements;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestDraftingProcess;
import forge.screens.deckeditor.controllers.CEditorQuestLimited;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.home.CHomeUI;
import forge.screens.home.quest.VSubmenuQuestDraft.Mode;
import forge.screens.home.sanctioned.CSubmenuDraft;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.JXButtonPanel;
import forge.util.storage.IStorage;

/** 
 * Controls the quest draft submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestDraft implements ICDoc {
    
    SINGLETON_INSTANCE;
    
    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("#,###");
    
    @SuppressWarnings("serial")
    @Override
    public void initialize() {

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        
        view.getBtnStartDraft().addActionListener(selectTournamentStart);
        view.getBtnStartTournament().addActionListener(prepareDeckStart);
        view.getBtnStartMatch().addActionListener(nextMatchStart);
        
        view.getBtnEditDeck().setCommand(
                new UiCommand() { @Override
                    public void run() { CSubmenuQuestDraft.this.editDeck(); } });
        
        view.getBtnLeaveTournament().setCommand(
                new UiCommand() { @Override
                    public void run() { CSubmenuQuestDraft.this.endTournamentAndAwardPrizes(); } });
        
        QuestAchievements achievements = FModel.getQuest().getAchievements();
        
        if (achievements == null) {
            view.setMode(Mode.EMPTY);
        } else if (achievements.getDraftEvents() == null || achievements.getDraftEvents().isEmpty()) {
            achievements.generateNewTournaments();
            if (achievements.getDraftEvents().isEmpty()) {
                view.setMode(Mode.EMPTY);
            } else {
                view.setMode(Mode.SELECT_TOURNAMENT);
            }
        } else if (FModel.getQuest().getDraftDecks() == null || !FModel.getQuest().getDraftDecks().contains(QuestEventDraft.DECK_NAME)) {
            view.setMode(Mode.SELECT_TOURNAMENT);
        } else if (!achievements.getCurrentDraft().isStarted()) {
            view.setMode(Mode.PREPARE_DECK);
        } else {
            view.setMode(Mode.TOURNAMENT_ACTIVE);
        }
        
    }
    
    @SuppressWarnings("unchecked")
    private void endTournamentAndAwardPrizes() {
        
        QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        
        if (!draft.isStarted()) {
            boolean shouldQuit = FOptionPane.showConfirmDialog("If you leave now, this tournament will be forever gone."
                    + "\nYou will keep the cards you drafted, but will receive no other prizes."
                    + "\n\nWould you still like to quit the tournament?", "Really Quit?", false);
            if (!shouldQuit) {
                return;
            }
            
        } else {
            
            if (draft.playerHasMatchesLeft()) {
                boolean shouldQuit = FOptionPane.showConfirmDialog("You have matches left to play!\nLeaving the tournament early will forfeit your potential future winnings."
                        + "\nYou will still receive winnings as if you conceded your next match and you will keep the cards you drafted."
                        + "\n\nWould you still like to quit the tournament?", "Really Quit?", false);
                if (!shouldQuit) {
                    return;
                }
            }
            
            String placement = QuestEventDraft.getPlacementString(draft.getPlayerPlacement());
            
            Object[] prizes = draft.getPrizes();
            
            if (prizes[0] != null && (int) prizes[0] > 0) {
                FOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + (int) prizes[0] + " credits!", "Credits Awarded", FSkin.getImage(FSkinProp.ICO_QUEST_COINSTACK));
                FModel.getQuest().getAssets().addCredits((int) prizes[0]);
            }
            
            if (prizes[2] != null) {
                
                List<PaperCard> individualCards = (ArrayList<PaperCard>) prizes[2];
                
                if (!individualCards.isEmpty()) {
                    final CardListViewer c = new CardListViewer("Tournament Reward", "For participating in the tournament, you have been awarded the following promotional card:", individualCards);
                    c.setVisible(true);
                    c.dispose();
                    FModel.getQuest().getCards().addAllCards(individualCards);
                }
                
            }
            
            if (prizes[1] != null) {
                
                List<BoosterPack> boosterPacks = (ArrayList<BoosterPack>) prizes[1];
    
                if (!boosterPacks.isEmpty()) {
                    
                    String packPlural = (boosterPacks.size() == 1) ? "" : "s";
                    
                    FOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + boosterPacks.size() + " booster pack" + packPlural + "!", "Booster Pack" + packPlural + " Awarded", FSkin.getImage(FSkinProp.ICO_CARD_IMAGE));
                    
                    if (FModel.getPreferences().getPrefBoolean(FPref.UI_OPEN_PACKS_INDIV) && boosterPacks.size() > 1) {
    
                        boolean skipTheRest = false;
                        List<PaperCard> remainingCards = new ArrayList<PaperCard>();
                        int totalPacks = boosterPacks.size();
                        int currentPack = 0;
                        
                        while (boosterPacks.size() > 0) {
                            
                            BoosterPack pack = boosterPacks.remove(0);
                            currentPack++;
                            
                            if (skipTheRest) {
                                remainingCards.addAll(pack.getCards());
                                continue;
                            }
                            
                            final BoxedProductCardListViewer c = new BoxedProductCardListViewer(pack.getName(), "You have found the following cards inside (Booster Pack " + currentPack + " of " + totalPacks + "):", pack.getCards());
                            c.setVisible(true);
                            c.dispose();
                            skipTheRest = c.skipTheRest();
                            FModel.getQuest().getCards().addAllCards(pack.getCards());
                            
                        }
                        
                        if (skipTheRest && !remainingCards.isEmpty()) {
                            final CardListViewer c = new CardListViewer("Tournament Reward", "You have found the following cards inside:", remainingCards);
                            c.setVisible(true);
                            c.dispose();
                            FModel.getQuest().getCards().addAllCards(remainingCards);
                        }
                        
                    } else {
                        
                        List<PaperCard> cards = new ArrayList<PaperCard>();
                        
                        while (boosterPacks.size() > 0) {
                            BoosterPack pack = boosterPacks.remove(0);
                            cards.addAll(pack.getCards());
                            continue;
                        }
                        
                        final CardListViewer c = new CardListViewer("Tournament Reward", "You have found the following cards inside:", cards);
                        c.setVisible(true);
                        c.dispose();
                        FModel.getQuest().getCards().addAllCards(cards);
                        
                    }
                }
                
            }
            
            if (draft.getPlayerPlacement() == 1) {
                FOptionPane.showMessageDialog("For placing " + placement + ", another tournament will be immediately available!");
                FModel.getQuest().getAchievements().addDraftToken();
            }
            
        }
        
        boolean saveDraft = FOptionPane.showConfirmDialog("Would you like to save this draft to the regular draft mode?", "Save Draft?");
        
        if (saveDraft) {
            
            String tournamentName = FModel.getQuest().getName() + " Tournament Deck " + new SimpleDateFormat("EEE d MMM yyyy HH-mm-ss").format(new Date());
            
            DeckGroup original = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
            DeckGroup output = new DeckGroup(tournamentName);
            for (Deck aiDeck : original.getAiDecks()) {
                output.addAiDeck(copyDeck(aiDeck));
            }
            output.setHumanDeck(copyDeck(original.getHumanDeck(), tournamentName));
            FModel.getDecks().getDraft().add(output);
            CSubmenuDraft.SINGLETON_INSTANCE.update();
            
        }
        
        String deckName = "Tournament Deck " + new SimpleDateFormat("EEE d MMM yyyy HH-mm-ss").format(new Date());
        
        Deck tournamentDeck = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME).getHumanDeck();
        Deck deck = new Deck(deckName);
        
        FModel.getQuest().getCards().addAllCards(deck.getAllCardsInASinglePool().toFlatList());
        
        if (tournamentDeck.get(DeckSection.Main).countAll() > 0) {
            deck.getOrCreate(DeckSection.Main).addAll(tournamentDeck.get(DeckSection.Main));
            FModel.getQuest().getMyDecks().add(deck);
        }
        
        FModel.getQuest().getDraftDecks().delete(QuestEventDraft.DECK_NAME);
        FModel.getQuest().getAchievements().endCurrentTournament();
        FModel.getQuest().save();
        
        VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        view.setMode(Mode.SELECT_TOURNAMENT);
        view.populate();
        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
        
    }
    
    private final ActionListener selectTournamentStart = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            CSubmenuQuestDraft.this.startDraft();
        }
    };
    
    private final ActionListener prepareDeckStart = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            CSubmenuQuestDraft.this.startTournament();
        }
    };
    
    private final ActionListener nextMatchStart = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            QuestDraftUtils.startNextMatch();
        }
    };

    private final KeyAdapter startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                VSubmenuQuestDraft.SINGLETON_INSTANCE.getBtnStartDraft().doClick();
            }
        }
    };
    
    private final MouseAdapter startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) {
                VSubmenuQuestDraft.SINGLETON_INSTANCE.getBtnStartDraft().doClick();
            }
        }
    };
    
    @Override
    public void update() {
        
        VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        
        if (FModel.getQuest().getAchievements() == null) {
            view.setMode(Mode.EMPTY);
            return;
        }
        
        QuestAchievements achievements = FModel.getQuest().getAchievements();
        achievements.generateNewTournaments();
        
        if (FModel.getQuest().getAchievements().getDraftEvents().isEmpty()) {
            view.setMode(Mode.EMPTY);
            return;
        }
        
        if ((FModel.getQuest().getDraftDecks() == null
            || !FModel.getQuest().getDraftDecks().contains(QuestEventDraft.DECK_NAME)
            || FModel.getQuest().getAchievements().getCurrentDraftIndex() == -1)) {
            view.setMode(Mode.SELECT_TOURNAMENT);
        } else if (!FModel.getQuest().getAchievements().getCurrentDraft().isStarted()) {
            view.setMode(Mode.PREPARE_DECK);
        } else {
            view.setMode(Mode.TOURNAMENT_ACTIVE);
        }
        
        QuestDraftUtils.update();
        
        switch (view.getMode()) {
        
            case SELECT_TOURNAMENT:
                updateSelectTournament();
                break;
        
            case PREPARE_DECK:
                updatePrepareDeck();
                break;
                
            case TOURNAMENT_ACTIVE:
                updateTournamentActive();
                break;
                
            default:
                break;
        
        }
        
    }
    
    private void updateSelectTournament() {
        
        VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;

        view.getLblCredits().setText("Available Credits: " + NUMBER_FORMATTER.format(FModel.getQuest().getAssets().getCredits()));

        FModel.getQuest().getAchievements().generateNewTournaments();

        view.getPnlTournaments().removeAll();
        JXButtonPanel grpPanel = new JXButtonPanel();
        
        boolean firstPanel = true;
        
        for (QuestEventDraft draft : FModel.getQuest().getAchievements().getDraftEvents()) {
            
            PnlDraftEvent draftPanel = new PnlDraftEvent(draft);
            final JRadioButton button = draftPanel.getRadioButton();
            
            if (firstPanel) {
                button.setSelected(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { button.requestFocusInWindow(); }
                });
                firstPanel = false;
            }
            
            grpPanel.add(draftPanel, button, "w 100%!, h 135px!, gapy 15px");
            
            button.addKeyListener(startOnEnter);
            button.addMouseListener(startOnDblClick);
            
        }
        
        view.getPnlTournaments().add(grpPanel, "w 100%!");
        
    }
    
    private void updatePrepareDeck() {
    }
    
    private void updateTournamentActive() {
        
        VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        
        if (FModel.getQuest().getAchievements().getCurrentDraft() == null) {
            return;
        }
        
        for (int i = 0; i < 15; i++) {
            
            String playerID = FModel.getQuest().getAchievements().getCurrentDraft().getStandings()[i];
            
            if (playerID.equals(QuestEventDraft.HUMAN)) {
                playerID = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            } else if (playerID.equals(QuestEventDraft.UNDETERMINED)) {
                playerID = "Undetermined";
            } else {
                playerID = FModel.getQuest().getAchievements().getCurrentDraft().getAINames()[Integer.parseInt(playerID) - 1];
            }
            
            view.getLblsStandings()[i].setText(playerID);
            
        }
        
    }

    public void setCompletedDraft(DeckGroup finishedDraft, String s) {
        
        List<Deck> aiDecks = new ArrayList<Deck>(finishedDraft.getAiDecks());
        finishedDraft.getAiDecks().clear();
        
        for (int i = 0; i < aiDecks.size(); i++) {
            Deck oldDeck = aiDecks.get(i);
            Deck namedDeck = new Deck("AI Deck " + i);
            namedDeck.putSection(DeckSection.Main, oldDeck.get(DeckSection.Main));
            namedDeck.putSection(DeckSection.Sideboard, oldDeck.get(DeckSection.Sideboard));
            finishedDraft.getAiDecks().add(namedDeck);
        }
        
        IStorage<DeckGroup> draft = FModel.getQuest().getDraftDecks();
        draft.add(finishedDraft);
        
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST_TOURNAMENT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuestLimited(FModel.getQuest()));
        
        FModel.getQuest().save();

        VSubmenuQuestDraft.SINGLETON_INSTANCE.setMode(Mode.PREPARE_DECK);
        VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();
        
    }
    
    private void editDeck() {
        VCurrentDeck.SINGLETON_INSTANCE.setItemManager(new DeckManager(GameType.Draft));
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST_TOURNAMENT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuestLimited(FModel.getQuest()));
        FModel.getQuest().save();
    }
    
    private void startDraft() {
        
        QuestEventDraft draftEvent = SSubmenuQuestUtil.getDraftEvent();
        
        long creditsAvailable = FModel.getQuest().getAssets().getCredits();
        if (creditsAvailable < draftEvent.getEntryFee()) {
            FOptionPane.showMessageDialog("You need " + NUMBER_FORMATTER.format(draftEvent.getEntryFee() - creditsAvailable) + " more credits to enter this tournament.", "Not Enough Credits");
            return;
        }
        
        boolean okayToEnter = FOptionPane.showConfirmDialog("This tournament costs " + draftEvent.getEntryFee() + " credits to enter.\nAre you sure you wish to enter?", "Enter Draft Tournament?");
        
        if (!okayToEnter) {
            return;
        }
        
        FModel.getQuest().getAchievements().setCurrentDraft(draftEvent);
        
        //TODO What happens when the draft is quit early
        
        FModel.getQuest().getAssets().subtractCredits(draftEvent.getEntryFee());
        
        BoosterDraft draft = BoosterDraft.createDraft(LimitedPoolType.Block, FModel.getBlocks().get(draftEvent.getBlock()), draftEvent.getBoosterConfiguration());

        final CEditorQuestDraftingProcess draftController = new CEditorQuestDraftingProcess();
        draftController.showGui(draft);

        draftController.setDraftQuest(CSubmenuQuestDraft.this);
        
        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
        
    }
    
    private void startTournament() {
        
        //TODO Refactor the deck getting to a single method
        String message = GameType.QuestDraft.getDecksFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
        if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            //TODO Pref for allowing non-conformant decks
            FOptionPane.showMessageDialog(message, "Deck Invalid");
            return;
        }
        
        boolean okayToStart = FOptionPane.showConfirmDialog("You will not be able to edit your deck once you start the tournament.\nAre you sure you wish to continue?", "Start Tournament?");
        
        if (!okayToStart) {
            return;
        }
        
        FModel.getQuest().getAchievements().getCurrentDraft().start();
        
        VSubmenuQuestDraft.SINGLETON_INSTANCE.setMode(Mode.TOURNAMENT_ACTIVE);
        VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();
        
        update();
        
    }
    
    private Deck copyDeck(final Deck deck) {
        
        Deck outputDeck = new Deck(deck.getName());
        
        outputDeck.putSection(DeckSection.Main, new CardPool(deck.get(DeckSection.Main)));
        outputDeck.putSection(DeckSection.Sideboard, new CardPool(deck.get(DeckSection.Sideboard)));
        
        return outputDeck;
        
    }
    
    private Deck copyDeck(final Deck deck, final String deckName) {
        
        Deck outputDeck = new Deck(deckName);
        
        outputDeck.putSection(DeckSection.Main, new CardPool(deck.get(DeckSection.Main)));
        outputDeck.putSection(DeckSection.Sideboard, new CardPool(deck.get(DeckSection.Sideboard)));
        
        return outputDeck;
        
    }

    @Override
    public UiCommand getCommandOnSelect() {
        final QuestController qc = FModel.getQuest();
        return new UiCommand() {
            private static final long serialVersionUID = 6153589785507038445L;
            @Override
            public void run() {
                if (qc.getAchievements() == null) {
                    CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_QUESTDATA);
                }
            }
        };
    }
    
}
